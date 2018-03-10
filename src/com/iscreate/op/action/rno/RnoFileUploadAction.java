package com.iscreate.op.action.rno;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iscreate.op.action.rno.model.DataUploadQueryCond;
import com.iscreate.op.action.rno.upload.DataUploadStatus;
import com.iscreate.op.action.rno.upload.FileAcceptStatus;
import com.iscreate.op.action.rno.upload.accept.FileAcceptable;
import com.iscreate.op.action.rno.upload.submit.DataUploadJobSubmit;
import com.iscreate.op.constant.RnoConstant;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoFileUploadService;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HadoopUser;
import com.iscreate.op.service.rno.tool.HttpTools;
import com.iscreate.op.service.rno.tool.RnoHelper;

@Controller
@Scope("prototype")
public class RnoFileUploadAction extends RnoCommonAction {
	private static final Logger logger = LoggerFactory.getLogger(RnoFileUploadAction.class);

	@Resource
	private RnoFileUploadService rnoFileUploadService;
	// -----文件接收判断器----由spring 注入//
	@Resource
	private Map<String, FileAcceptable> fileAcceptables;
	// -----任务提交工具----由spring 注入//
	@Resource
	private Map<String, DataUploadJobSubmit> dataUploadJobSubmits;

	// ---页面变量----//
	// ---------页面变量----------------//
	private long fileSize;
	private File file;// 文件输入流
	// 上传文件名
	private String fileFileName;
	// 上传文件类型
	private String fileCode;// 文件编码
	private String token;// 文件解析任务对应的token
	private long cityId;// 城市id

	private String meaTime;// 数据所属日期

	private DataUploadQueryCond uploadCond;// 上传记录查询条件

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public String getFileFileName() {
		return fileFileName;
	}

	public void setFileFileName(String fileFileName) {
		this.fileFileName = fileFileName;
	}

	public String getFileCode() {
		return fileCode;
	}

	public void setFileCode(String fileCode) {
		this.fileCode = fileCode;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public long getCityId() {
		return cityId;
	}

	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public String getMeaTime() {
		return meaTime;
	}

	public void setMeaTime(String meaTime) {
		this.meaTime = meaTime;
	}

	public Map<String, FileAcceptable> getFileAcceptables() {
		return fileAcceptables;
	}

	public void setUploadCond(DataUploadQueryCond uploadCond) {
		this.uploadCond = uploadCond;
	}

	/**
	 * 询问当前是否允许上传文件 上一个文件上传没有完成，下一个不可以开始
	 * 
	 * @author brightming 2014-8-20 下午3:41:00
	 */
	@SuppressWarnings("unchecked")
	public void ifAllowedUploadFileAjaxAction() {
		Map<String, DataUploadStatus> statuses = ((Map<String, DataUploadStatus>) SessionService.getInstance()
				.getValueByKey(RnoConstant.SessionConstant.uploadStatusSessionKey));
		if (statuses == null || statuses.isEmpty()) {
			logger.debug("允许上传操作");
			writeToClient("{'flag':true}");
		} else {
			DataUploadStatus st;
			for (String k : statuses.keySet()) {
				st = statuses.get(k);
				if (st.getReadedBytes() != st.getTotalBytes()) {
					logger.debug("当前不允许上传操作，上一次上传未完成。");
					writeToClient("{'flag':false}");
					return;
				}
			}
		}

		logger.debug("所有文件都已经上传完成，允许上传操作");
		writeToClient("{'flag':true}");
	}

	/**
	 * 是否可以接收该文件
	 * 
	 * @author brightming 2014-8-20 下午3:45:42
	 */
	public void ifFileAcceptableAjaxAction() {
		logger.debug("判断文件是否可以接收：filesize=" + fileSize + ",fileCode=" + fileCode + ",attachParams=" + attachParams);
		// 通过fileCode寻找相应的判断器
		FileAcceptable accept = fileAcceptables.get(fileCode);

		if (accept == null) {
			logger.warn("fileCode=" + fileCode + " 没有相应的文件接收判断类，使用默认的基础判断类");
			accept = fileAcceptables.get("default");
		}
		FileAcceptStatus status = accept.isAcceptable(fileSize, fileCode, attachParams);

		if (status.isFlag()) {
			// 分配一个查询进度的token
			String token = "uploadtoken";// hard code
											// UUID.randomUUID().toString().replaceAll("-",
											// "");
			status.setToken(token);// 上传文件时，带上这个token，给进度更新器使用
		}
		String res = gson.toJson(status);
		logger.debug("判断文件是否可以接收：filesize=" + fileSize + ",fileCode=" + fileCode + ",attachParams=" + attachParams
				+ ",的结果：" + res);
		writeToClient(res);
	}

	/**
	 * 查询上传进度
	 * 
	 * @author brightming 2014-8-20 下午4:07:10
	 */
	@SuppressWarnings("unchecked")
	public void queryUploadFileProgressAction() {
		Map<String, DataUploadStatus> statuses = ((Map<String, DataUploadStatus>) SessionService.getInstance()
				.getValueByKey(RnoConstant.SessionConstant.uploadStatusSessionKey));
		// log.debug("all progress:" + statuses);
		if (statuses == null) {
			logger.error("token=" + token + "对应的数据没有上传！");
			DataUploadStatus st = new DataUploadStatus();
			st.setCurrentItem(0);
			st.setReadedBytes(0);
			st.setTotalBytes(1);
			writeToClient(gson.toJson(st));
			// log.debug("token:" + token + "对应的文件上传进度：" + gson.toJson(st));
			return;
		}
		DataUploadStatus st = statuses.get(token);
		String res = gson.toJson(st);
		logger.debug("token:" + token + "对应的文件上传进度：" + res);
		writeToClient(res);
	}

	/**
	 * 批量上传
	 * 
	 * @author brightming 2014-8-20 下午5:50:03
	 */
	@SuppressWarnings("deprecation")
	public void batchUploadFileAjaxAction() {
		logger.debug("进入方法batchUploadFileAjaxAction。fileCode=" + fileCode + ",cityId=" + cityId + ",meaTime=" + meaTime);
		HttpServletRequest request = ServletActionContext.getRequest();
		if (request.getAttribute("fileTooLarge") != null) {
			logger.error("file too large!" + request.getAttribute("fileTooLarge"));
			writeToClient("{'flag':false,'msg':'文件大小超过系统允许值:"
					+ RnoHelper.getPropSizeExpression((Long) request.getAttribute("allowedFileSize")) + "！'}");
			return;
		}
		// 存储到指定位置
		boolean valid = true;
		String msg = "";
		if (file == null) {
			logger.error("未上传文件！");
			msg += "未上传文件！";
			valid = false;
		}
		if (fileCode == null || fileCode.trim().isEmpty()) {
			logger.error("未带文件类型编码！");
			msg += "文件类型不明！";
			valid = false;
		}
		if (cityId == 0) {
			logger.error("未指定城市！");
			msg += "未指定城市！";
			valid = false;
		}
		if (meaTime == null || meaTime.trim().isEmpty()) {
			logger.error("没有指定数据归属日期！");
			msg += "没有指定数据归属日期！";
			valid = false;
		}

		if (valid) {
			String path = "op/rno/upload";
			Date now = new Date();
			String day = sdf_SIMPLE.format(now);
			path += "/" + day + "/" + file.getName();
			// System.out.println("with file path==" + path);
			File anotherFile = new File(path);
			if (!anotherFile.getParentFile().exists()) {
				anotherFile.getParentFile().mkdirs();
			}

			// FileTool.copy(file, anotherFile);
			String hdfs = "hdfs:///" + HadoopUser.homeOfUser() + path;

			long fileLen = file.length();
			String account = (String) SessionService.getInstance().getValueByKey("userId");
			RnoDataCollectRec dataRec = new RnoDataCollectRec();
			dataRec.setAccount(account);
			dataRec.setBusinessDataType(RnoConstant.BusinessDataType.getByCode(fileCode).getType());
			dataRec.setBusinessTime(RnoHelper.parseDateArbitrary(meaTime));
			dataRec.setCityId(cityId);
			dataRec.setFileName(fileFileName);
			dataRec.setFileSize(fileLen);
			dataRec.setFileStatus("等待解析");
			dataRec.setFullPath(hdfs);
			dataRec.setOriFileName(fileFileName);
			dataRec.setUploadTime(now);
			if (idForCell != null) {
				dataRec.setIdForCell(idForCell);
			}
			if (isToHbase != null) {
				dataRec.setIsToHbase(isToHbase);
			}
			// 执行拷贝
			FileTool.copyFromLocalFile(file.getAbsolutePath(), hdfs, true);

			// 通过fileCode寻找相应的任务提交工具
			DataUploadJobSubmit submit = dataUploadJobSubmits.get(fileCode);
			if (submit == null) {
				logger.warn("fileCode=" + fileCode + " 没有相应的任务提交工具类，使用默认的基础提交类");
				submit = dataUploadJobSubmits.get("default");
			}
			submit.setFileCode(fileCode);
			dataRec = submit.submitDataUploadJob(dataRec);

			if (dataRec == null) {
				logger.error("数据上传后，提交任务失败！");
				writeToClient("{'flag':false,'msg':'提交数据解析任务失败'}");
			} else {
				String result = gson.toJson(dataRec);
				logger.debug("数据上传结果：" + dataRec);
				writeToClient("{'flag':true,'msg':" + result + "}");
			}
		} else {
			writeToClient("{'flag':false,'msg':'" + msg + "'}");
		}
	}

	/**
	 * 分页查询数据上传记录
	 * 
	 * @author brightming 2014-8-22 上午9:20:36
	 */
	public void queryUploadRecAjaxAction() {

		uploadCond = new DataUploadQueryCond();
		uploadCond.setBegUploadDate((String) attachParams.get("begUploadDate"));
		uploadCond.setEndUploadDate((String) attachParams.get("endUploadDate"));
		uploadCond.setFileStatus((String) attachParams.get("fileStatus"));
		uploadCond.setCityId(Long.parseLong((String) attachParams.get("cityId")));
		uploadCond.setBusinessDataType(RnoConstant.BusinessDataType.getByCode(fileCode).getType());
		logger.debug("queryUploadRecAjaxAction.page=" + page + ",attmap=" + attachParams + ",queryCond=" + uploadCond);

		int cnt = page.getTotalCnt();
		if (cnt < 0) {
			cnt = (int) rnoFileUploadService.queryUploadDataCnt(uploadCond);
		}

		Page newPage = page.copy();
		newPage.setTotalCnt(cnt);

		List<RnoDataCollectRec> dataRecs = rnoFileUploadService.queryUploadDataByPage(uploadCond, newPage);
		logger.debug("RnoDataCollectRec size:{}", dataRecs == null ? 0 : dataRecs.size());
		String result1 = gson.toJson(dataRecs);

		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'data':" + result1 + "}";
		logger.debug("退出queryUploadRecAjaxAction。输出：{}", result);
		writeToClient(result);
	}

	/**
	 * 查询指定job的报告
	 * 
	 * @author brightming 2014-8-22 下午4:22:20
	 */
	public void queryJobReportAjaxAction() {
		logger.debug("queryJobReportAjaxAction.jobId=" + attachParams.get("jobId"));

		Page newPage = page.copy();
		String jobIdStr = (String) attachParams.get("jobId");
		if (jobIdStr == null || jobIdStr.trim().isEmpty()) {
			logger.error("未传入一个有效的jobid！无法查看其报告！");
			newPage.setTotalCnt(0);
			newPage.setTotalPageCnt(0);
			String pstr = gson.toJson(newPage);
			String result = "{'page':" + pstr + ",'data':[]}";
			logger.debug("退出queryJobReportAjaxAction。输出：" + result);
			HttpTools.writeToClient(result);
		}

		Long jobId = Long.parseLong(jobIdStr);
		int cnt = 0;
		if (page.getTotalCnt() < 0) {
			cnt = rnoFileUploadService.queryJobReportCnt(jobId);
			newPage.setTotalCnt(cnt);
		}
		List<Map<String, Object>> reportRecs = rnoFileUploadService.queryJobReportByPage(jobId, newPage);
		logger.debug("reportRecs size:{}", reportRecs == null ? 0 : reportRecs.size());
		String result1 = gson.toJson(reportRecs);

		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'data':" + result1 + "}";
		logger.debug("退出queryJobReportAjaxAction。输出：{}", result);
		writeToClient(result);
	}
}
