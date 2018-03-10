package com.iscreate.op.action.rno;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iscreate.op.constant.RnoConstant;
import com.iscreate.op.pojo.rno.AreaRectangle;
import com.iscreate.op.pojo.rno.RnoLteAzimuthAssessTask;
import com.iscreate.op.pojo.rno.RnoMapLnglatRelaGps;
import com.iscreate.op.pojo.rno.RnoStructureAnalysisTask;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoNcsDynaCoverageService;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.HttpTools;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;

public class RnoNcsDynaCoverageAction extends RnoCommonAction {
	private static Log log = LogFactory.getLog(RnoNcsDynaCoverageAction.class);
	private static Gson gson = new GsonBuilder().create();// 线程安全

	//注入
	private RnoNcsDynaCoverageService rnoNcsDynaCoverageService;
	
	//动态覆盖图
	private String preFiveDayTime;
	private long cityId;
	private String cell;
	private String startDate;
	private String endDate;
	private double imgCoeff;//限制图２
	private String lteCellId;
	private double imgSizeCoeff;//限制图１，２

	//现网小区计算方向角
	private Map<String, String> cond;
	private String lastMonday;
	private String lastSunday;
	private long jobId;
	private String error;
	private InputStream exportInputStream;// 导出结果文件的输入流
	//LTE 方位角评估 任务信息对象
	private RnoLteAzimuthAssessTask azimuthAssessTask;
	private InputStream reportInputStream;// 导出分析报告的输入流
	
	public InputStream getReportInputStream() {
		return reportInputStream;
	}
	public void setReportInputStream(InputStream reportInputStream) {
		this.reportInputStream = reportInputStream;
	}
	public RnoLteAzimuthAssessTask getAzimuthAssessTask() {
		return azimuthAssessTask;
	}
	public void setAzimuthAssessTask(RnoLteAzimuthAssessTask azimuthAssessTask) {
		this.azimuthAssessTask = azimuthAssessTask;
	}
	public String getLteCellId() {
		return lteCellId;
	}
	public void setLteCellId(String lteCellId) {
		this.lteCellId = lteCellId;
	}
	public InputStream getExportInputStream() {
		return exportInputStream;
	}
	public void setExportInputStream(InputStream exportInputStream) {
		this.exportInputStream = exportInputStream;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	public long getJobId() {
		return jobId;
	}
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	public String getLastMonday() {
		return lastMonday;
	}
	public void setLastMonday(String lastMonday) {
		this.lastMonday = lastMonday;
	}
	public String getLastSunday() {
		return lastSunday;
	}
	public void setLastSunday(String lastSunday) {
		this.lastSunday = lastSunday;
	}
	public Map<String, String> getCond() {
		return cond;
	}
	public void setCond(Map<String, String> cond) {
		this.cond = cond;
	}
	public double getImgCoeff() {
		return imgCoeff;
	}
	public void setImgCoeff(double imgCoeff) {
		this.imgCoeff = imgCoeff;
	}
	public long getCityId() {
		return cityId;
	}
	public void setCityId(long cityId) {
		this.cityId = cityId;
	}
	public String getCell() {
		return cell;
	}
	public void setCell(String cell) {
		this.cell = cell;
	}
	public String getStartDate() {
		return startDate;
	}
	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}
	public String getEndDate() {
		return endDate;
	}
	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}
	public String getPreFiveDayTime() {
		return preFiveDayTime;
	}
	public void setPreFiveDayTime(String preFiveDayTime) {
		this.preFiveDayTime = preFiveDayTime;
	}
	public RnoNcsDynaCoverageService getRnoNcsDynaCoverageService() {
		return rnoNcsDynaCoverageService;
	}
	public void setRnoNcsDynaCoverageService(
			RnoNcsDynaCoverageService rnoNcsDynaCoverageService) {
		this.rnoNcsDynaCoverageService = rnoNcsDynaCoverageService;
	}
	public double getImgSizeCoeff() {
		return imgSizeCoeff;
	}
	public void setImgSizeCoeff(double imgSizeCoeff) {
		this.imgSizeCoeff = imgSizeCoeff;
	}
	/**
	 * 初始化小区动态覆盖页面
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015-3-9 下午4:23:27
	 * @company 怡创科技
	 */
	public String initCellDynaCoveragePageAction() {
		initAreaList();
		Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE,-5);
	    preFiveDayTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		return "success";
	}
	
	public String init2GCellDirectionAngleTaskManageAction() {
		initAreaList();
//		Calendar cal = Calendar.getInstance();
//	    cal.add(Calendar.DATE,-5);
//	    preFiveDayTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		return "success";
	}
	
	public String initDirectionAngleTaskAddForAjaxAction() {
		
		initAreaList();// 加载区域相关信息
		
		Calendar cal = Calendar.getInstance();
		//n为推迟的周数，1本周，-1向前推迟一周，2下周，依次类推
		int n = -1;
		cal.add(Calendar.DATE, n*7);
		cal.set(Calendar.DAY_OF_WEEK,Calendar.MONDAY);
		lastMonday = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		//System.out.println(monday);
		  
		n = 1;
		cal.add(Calendar.DATE, n*7);
		cal.set(Calendar.DAY_OF_WEEK,Calendar.SUNDAY);
		lastSunday = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(cal.getTime());
		//System.out.println(sunday);
		
		return "success";
	}
	
	/**
	 * 获取画小区动态覆盖图所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	public void getDynaCoverageDataForAction() {
		log.debug("获取画小区动态覆盖图(曲线)所需的数据, cell=" + cell + ", cityId=" + cityId
				+ ", startDate=" + startDate + ", endDate=" + endDate + ", imgCoeff="+imgCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		
		Map<String, List<Map<String, Object>>> res = rnoNcsDynaCoverageService
				.getDynaCoverageDataByCityAndDate(cell, cityId, startDate, endDate,standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
	}
	
	/**
	 * 获取画小区动态覆盖图(折线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年4月30日9:55:32
	 * @company 怡创科技
	 */
	public void getDynaCoverageData2ForAction() {
		log.debug("获取画小区动态覆盖图(折线)所需的数据, cell=" + cell + ", cityId=" + cityId
				+ ", startDate=" + startDate + ", endDate=" + endDate + ", imgCoeff="+imgCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		
		Map<String, List<Map<String, Object>>> res = rnoNcsDynaCoverageService
				.getDynaCoverageData2ByCityAndDate(cell, cityId, startDate, endDate,imgCoeff,standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
	}
	

	/**
	 * 初始化4G小区动态覆盖页面
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月7日16:12:06
	 * @company 怡创科技
	 */
	public String init4GCellDynaCoveragePageAction() {
		initAreaList();
		Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE,-5);
	    preFiveDayTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		return "success";
	}
	
	/**
	 * 获取画LTE小区动态覆盖图(贝塞尔曲线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	public void get4GDynaCoverageDataForAction() {
		log.debug("获取画LTE小区动态覆盖图(贝塞尔曲线)所需的数据, lteCellId=" + lteCellId
				+ ", cityId=" + cityId + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", imgCoeff=" + imgCoeff+",imgSizeCoeff="+imgSizeCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		
		Map<String, List<Map<String, Object>>> res = rnoNcsDynaCoverageService
				.get4GDynaCoverageDataByCityAndDate(lteCellId, cityId, startDate, endDate,imgSizeCoeff,standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
		
	}
	/**
	 * 获取画LTE小区动态覆盖图(折线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	public void get4GDynaCoverageData2ForAction() {
		log.debug("获取画LTE小区动态覆盖图(折线)所需的数据, lteCellId=" + lteCellId
				+ ", cityId=" + cityId + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", imgCoeff=" + imgCoeff+",imgSizeCoeff="+imgSizeCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		
		Map<String, List<Map<String, Object>>> res = rnoNcsDynaCoverageService
				.get4GDynaCoverageData2ByCityAndDate(lteCellId, cityId, startDate, endDate,imgCoeff,imgSizeCoeff,standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
	}
	
	
	/**
	 * 分页获取2g小区方向角计算任务信息
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月4日14:04:38
	 * @company 怡创科技
	 */
	public void query2GDirectionAngleTaskForAjaxAction() {
		log.info("进入方法query2GDirectionAngleTaskForAjaxAction. page=" + page
				+ ",cond=" + cond);
			
			Map<String, Object> result = new HashMap<String, Object>();
			if (page == null) {
				log.error("方法query2GDirectionAngleTaskForAjaxAction的page参数为空！");
				HttpTools.writeToClient(gson.toJson(result));
				return;
			}

			Page newPage = page.copy();
			List<Map<String, Object>> res = rnoNcsDynaCoverageService
					.query2GDirectionAngleTaskByPage(cond, newPage);

			log.info("计算以后，page=" + newPage);
			int totalCnt = newPage.getTotalCnt();
			newPage.setTotalPageCnt(totalCnt / newPage.getPageSize()
					+ (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
			newPage.setForcedStartIndex(-1);

			result.put("page", newPage);
			result.put("data", res);

			HttpTools.writeToClient(gson.toJson(result));
	}
	
	/**
	 * 提交2g小区方向角计算任务信息
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月4日14:04:38
	 * @company 怡创科技
	 */
	public void add2GDirectionAngleTaskForAjaxAction() {
		log.info("进入方法addInterMartixByNcsForAjaxAction。cond=" + cond);
		if (cond == null || cond.isEmpty()) {
			log.info("未传入查询条件");
		}

		boolean isNcsExist = false;
		//判断日期范围是否存在NCS数据
		long ncsCnt = rnoNcsDynaCoverageService.queryNcsDataCountByCond(cond);
		if(ncsCnt > 0) {
			isNcsExist = true;
		}
		
		if(isNcsExist) {
			//提交2g小区方向角计算任务
			String account = (String) SessionService.getInstance().getValueByKey("userId");
			rnoNcsDynaCoverageService.add2GDirectionAngleTask(cond, account);
		}

		Map<String, Object> result = new HashMap<String, Object>();
		result.put("isNcsExist", isNcsExist);
		HttpTools.writeToClient(gson.toJson(result));
	}
	
	
	/**
	 * 下载2g小区方向角计算结果文件
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月5日16:06:07
	 * @company 怡创科技
	 */
	public String downloadDirAngleFileAction() {
		log.info("下载2g小区方向角计算结果文件， jobId=" + jobId);
		// 获取任务的信息。
		Connection connection = DataSourceConn.initInstance().getConnection();
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String sql = "select * from rno_2g_dirangle_rec where job_id="+jobId;
		List<Map<String,Object>> taskInfo = RnoHelper.commonQuery(stmt, sql);
		try {
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return "fail";
		}
		if(taskInfo.size() <= 0) {
			log.debug("下载2g小区方向角计算结果文件中，查询不到jobId="+jobId+"的任务信息");
			return "fail";
		}
		String filePath = taskInfo.get(0).get("FILE_PATH").toString();
		String dlFileName = jobId + "_2G方向角.csv";
		String dlFileRealPath = filePath+"/"+dlFileName;
		
		try {
			setFileName(new String(dlFileName.getBytes(),"iso-8859-1"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			return "fail";
		}
		File f = new File(dlFileRealPath);
		if (!f.exists()) {
			log.error("下载的2g小区方向角计算结果文件不存在！jobId="+jobId);
			error="下载的2g小区方向角计算结果文件不存在！,文件路径："+dlFileRealPath;
			return "fail";
		} else {
			try {
				exportInputStream = new FileInputStream(dlFileRealPath);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				error=exportInputStream+"获取文件流异常,文件路径："+dlFileRealPath;
				return "fail";
			}
		}
		
		return "success";
	}
	/**
	 * 
	 * @title 获取画LTE小区动态覆盖In干扰连线所需的数据
	 * @author chao.xj
	 * @date 2015-6-12上午11:33:55
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void get4GDynaCoverageInInferDataForAction() {
		log.debug("获取画LTE小区动态覆盖图(折线)所需的数据, lteCellId=" + lteCellId
				+ ", cityId=" + cityId + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", imgCoeff=" + imgCoeff+",imgSizeCoeff="+imgSizeCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		List<Map<String, String>> res = rnoNcsDynaCoverageService
				.get4GDynaCoverageInInferDataByCityAndDate(lteCellId, cityId, startDate, endDate, standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
	}
	/**
	 * 
	 * @title 获取画LTE小区动态覆盖out干扰连线所需的数据
	 * @author chao.xj
	 * @date 2015-6-12上午11:33:55
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void get4GDynaCoverageOutInferDataForAction() {
		log.debug("获取画LTE小区动态覆盖图(折线)所需的数据, lteCellId=" + lteCellId
				+ ", cityId=" + cityId + ", startDate=" + startDate
				+ ", endDate=" + endDate + ", imgCoeff=" + imgCoeff+",imgSizeCoeff="+imgSizeCoeff);
		
		// 基准点
		Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints = null;
		standardPoints = rnoCommonService
				.getSpecialAreaRnoMapLnglatRelaGpsMapList(cityId,
						RnoConstant.DBConstant.MAPTYPE_BAIDU);
		
		List<Map<String, String>> res = rnoNcsDynaCoverageService
				.get4GDynaCoverageOutInferDataByCityAndDate(lteCellId, cityId, startDate, endDate, standardPoints);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
	}
	/**
	 * 
	 * @title 初始化4G方位角评估分析页面
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午3:31:18
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String init4GAzimuthAssessmentPageAction() {
		initAreaList();// 加载区域相关信息
		return "success";
	}
	/**
	 * 
	 * @title 在创建4G方位角评估新任务消息跳转页面
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午3:37:53
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String add4GAzimuthAssessmentTaskInfoPageForAjaxAction() {
		initAreaList();// 加载区域相关信息
		Calendar cal = Calendar.getInstance();
	    cal.add(Calendar.DATE,-5);
	    preFiveDayTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		return "goTaskInfoPage";
	}
	/**
	 * 
	 * @title 提交LTE 方位角评估分析计算任务
	 * @author chao.xj
	 * @date 2015-11-16下午4:31:00
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void submit4GAzimuthAssessTaskAction() {
		log.info("进入：submit4GAzimuthAssessTaskAction azimuthAssessTask="+azimuthAssessTask);
		String account = (String) SessionService.getInstance().getValueByKey(
					"userId");
		String path = ServletActionContext.getServletContext().getRealPath(
				"/op/rno/ana_result/");
		log.info("提交者=" + account + ",文件存储=" + path);
		
		Map<String, Object> res =null;
		
		res = rnoNcsDynaCoverageService.submit4GAzimuthAssessTask(account, path, azimuthAssessTask);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);	
	}
	/**
	 * 
	 * @title 分页查询LTE方位角评估分析计算任务信息
	 * @author chao.xj
	 * @date 2015-11-16下午5:23:12
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void queryLteAzimuthAssessTaskByPageForAjaxAction() {
		log.info("进入：queryLteAzimuthAssessTaskByPageForAjaxAction。cond=" + cond
				+ ",page=" + page);
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		if (page == null) {
			log.error("方法queryLteAzimuthAssessTaskByPageForAjaxAction的page参数为空！");
			HttpTools.writeToClient("[]");
			return;
		}

		Page newPage = page.copy();
		List<Map<String, Object>> structAnaTasks = rnoNcsDynaCoverageService
				.queryLteAzimuthAssessTaskByPage(cond, newPage, account);
		String res = gson.toJson(structAnaTasks);

		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize()
				+ (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'data':" + res + "}";
		log.info("退出queryLteAzimuthAssessTaskByPageForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}
	/**
	 * 
	 * @title 下载lte方位角评估分析结果文件
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午5:15:18
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String downloadLteAzimuthAssessFileAction() {
		log.info("下载LTE方位角评估分析结果文件。任务jobId=" + jobId);
		// 获取任务的信息。
		List<Map<String,Object>> res = rnoNcsDynaCoverageService.getLteAzimuthAssessTaskByJobId(jobId);
		if(res.size() <= 0) {
			log.error("找不到对应jobId="+jobId+"的任务！");
			error = "<h>不存在指定的分析任务!无法导出其分析报告！</h>";
			return "fail";
		}
		String resultDir = "";
		String dlFileName = "";
		if(res.get(0).get("RESULT_DIR") != null) {
			resultDir = res.get(0).get("RESULT_DIR").toString();
		} 
		if(res.get(0).get("DL_FILE_NAME") != null) {
			dlFileName = res.get(0).get("DL_FILE_NAME").toString();
		} 
		
		log.info("结果文件所在路径：" + resultDir + ", 文件名：" + dlFileName);
		try {
			super.setFileName(new String(dlFileName.getBytes(),"iso-8859-1"));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		}

		// 判断文件是否存在
		String realPath = resultDir + dlFileName;
		File f = new File(realPath);
		if (!f.exists()) {
			log.error("任务jobId[" + jobId + "]对应的结果文件：" + dlFileName + "不存在！");
			error = "所需要的任务结果文件不存在！是否重新计算？"
					+ "<form action='' type='post'>"
					+ "<input type='hidden' name='jobId' value='" + jobId
					+ "'/>" + "<input type='submit' value='重新计算' />"
					+ "</form>";
			return "fail";
		}
		try {
			reportInputStream=new FileInputStream(realPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return "fail";
		}
		return "success";
	}
	public static void main(String[] args) {
		Map<String, List<Map<String, Object>>> maps=new HashMap<String, List<Map<String, Object>>>();
		List<Map<String, Object>> list=new ArrayList<Map<String,Object>>();
		Map<String, Object> map=new HashMap<String, Object>();
		map.put("aa", String.valueOf(4.0d));
		list.add(map);
		maps.put("aa", list);
		String res=gson.toJson(maps);
		System.out.println(res);
	}
}
