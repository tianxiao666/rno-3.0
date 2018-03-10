package com.iscreate.op.service.rno;

import java.io.IOException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Cluster;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.action.rno.model.MrJobCond;
import com.iscreate.op.dao.rno.Rno4GPciDao;
import com.iscreate.op.dao.rno.RnoLtePciDao;
import com.iscreate.op.pojo.rno.RnoLteInterMatrixTaskInfo;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.client.api.JobClient;
import com.iscreate.op.service.rno.job.client.api.JobClientDelegate;
import com.iscreate.op.service.rno.job.server.JobAddCallback;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.RnoHelper;

/*import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;*/

/**
 * @author brightming
 * @version 1.0
 * @created 17-һ��-2014 11:02:57
 */
@Service(value = "rnoLtePciService")
public class RnoLtePciServiceImpl implements RnoLtePciService {

	private static Log log = LogFactory.getLog(RnoLtePciServiceImpl.class);

	// ---注入----//
	@Autowired
	private RnoLtePciDao rnoLtePciDao;
	@Autowired
	private Rno4GPciDao rno4gPciDao;

	public RnoLtePciServiceImpl() {

	}

	public void finalize() throws Throwable {

	}

	// pci计算

	/**
	 * 
	 * @title 通过模块类型获取阈值门限对象集合
	 * @param moduleType
	 * @return
	 * @author chao.xj
	 * @date 2014-8-28上午10:52:09
	 * @company 怡创科技
	 * @version 1.2
	 */
	public List<RnoThreshold> getThresholdsByModuleType(String moduleType) {
		return rnoLtePciDao.getThresholdsByModuleType(moduleType);
	}

	/**
	 * 
	 * @title 通过区域及起始时间和厂家,及数据类型从Hbase获取MR数据描述记录情况
	 * @param cityId
	 * @param startTime
	 * @param endTime
	 * @param factory
	 *            ERI,ZTE
	 * @return
	 * @author chao.xj
	 * @date 2015-3-27下午4:33:02
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> getDataRecordFromHbase(long cityId, String startTime, String endTime,
			String factory) {

		DateUtil dateUtil = new DateUtil();

		if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
			log.error("getDataRecordFromHbase未指明时间范围！");
			return Collections.emptyList();
		}
		Date startDate = dateUtil.to_yyyyMMddDate(startTime);
		Date endDate = dateUtil.to_yyyyMMddDate(endTime);

		if (startDate == null || endDate == null) {
			log.error("getDataRecordFromHbase指定的时间有错！startTime=" + startTime + ",endTime=" + endTime);
			return Collections.emptyList();
		}
		List<Date> dateList = RnoHelper.findDates(startDate, endDate);
		// MR
		List<Map<String, Object>> mrRecordNumList = rnoLtePciDao.getDataDescRecordFromHbase(cityId, startTime, endTime,
				factory, "MR");
		// HO
		List<Map<String, Object>> hoRecordNumList = rnoLtePciDao.getDataDescRecordFromHbase(cityId, startTime, endTime,
				factory, "HO");
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		Map<String, Object> map;
		String date = "";
		// long recordNum;
		String meaTime = "";

		// Calendar now=Calendar.getInstance();

		for (Date d : dateList) {
			date = dateUtil.format_yyyyMMdd(d);
			map = new HashMap<String, Object>();
			map.put("DATETIME", date);
			map.put("MR_RECORD_NUM", "--");
			map.put("HO_RECORD_NUM", "--");
			for (Map<String, Object> one : mrRecordNumList) {
				meaTime = one.get("MEA_TIME").toString();
				meaTime = dateUtil.format_yyyyMMdd(dateUtil.parseDateArbitrary(meaTime));
				if (meaTime.equals(date)) {
					map.put("MR_RECORD_NUM", Long.parseLong(one.get("RECORD_COUNT").toString()));
				}
			}
			for (Map<String, Object> one : hoRecordNumList) {
				meaTime = one.get("MEA_TIME").toString();
				meaTime = dateUtil.format_yyyyMMdd(dateUtil.to_yyyyMMddDate(meaTime));
				if (meaTime.equals(date)) {
					map.put("HO_RECORD_NUM", Long.parseLong(one.get("RECORD_COUNT").toString()));
				}
			}
			result.add(map);
		}
		return result;
	}

	/**
	 * 
	 * @title 通过区域ID获取LTE小区标识对应的小区信息
	 * @param stmt
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2015-4-9下午2:34:39
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, List<String>> getLteCellInfoByCellId(Statement stmt, long cityId) {
		return rnoLtePciDao.getLteCellInfoByCellId(stmt, cityId);
	}

	/**
	 * 提交PCI规划任务
	 * 
	 * @param account
	 * @param threshold
	 * @param taskInfo
	 * @return
	 * @author peng.jm
	 * @date 2015年3月30日11:36:44
	 */
	@Override
	public Map<String, Object> submitPciPlanAnalysisTask(String account, final List<RnoThreshold> rnoThresholds,
			final RnoLteInterferCalcTask.TaskInfo taskInfo) {
		log.debug("进入submitPciPlanAnalysisTask account=" + account + ",rnoThresholds=" + rnoThresholds + ",taskInfo="
				+ taskInfo);
		Map<String, Object> result = new HashMap<String, Object>();

		if (taskInfo.getMatrixDataCollectId() == 0) {// hbase数据源
			// 创建job
			JobProfile job = new JobProfile();
			job.setAccount(account);
			job.setJobName(taskInfo.getTaskName());
			job.setJobType("RNO_PCI_PLAN_NEW");
			job.setSubmitTime(new Date());
			job.setDescription(taskInfo.getTaskDesc());
			// jobClient.submitJob(job);
			JobClient jobClient = JobClientDelegate.getJobClient();
			result = jobClient.submitJob(job, new JobAddCallback<Map<String, Object>>() {
				@Override
				public Map<String, Object> doWhenJobAdded(JobProfile job) {
					return rnoLtePciDao.savePciPlanAnalysisTaskInfo(job.getJobId(), rnoThresholds, taskInfo);
				}
			});
		} else {// 干扰矩阵文件
			result = rnoLtePciDao.savePciPlanAnalysisTaskInfo(taskInfo.getMatrixDataCollectId(), rnoThresholds,
					taskInfo);
		}
		if ((Boolean) result.get("flag")) {
			SessionService.saveSession("session", ServletActionContext.getRequest().getSession());
		}
		log.debug("退出submitPciPlanAnalysisTask result=" + result);
		return result;
	}

	/**
	 * 查询pci自动规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	@Override
	public List<Map<String, Object>> queryPciPlanTaskByPage(Map<String, String> cond, Page page, String account) {
		log.debug("进入方法：queryPciPlanTaskByPage。condition=" + cond + ",page=" + page);
		if (page == null) {
			log.warn("方法：queryPciPlanTaskByPage的参数：page 是空！无法查询!");
			return Collections.emptyList();
		}
		long totalCnt = page.getTotalCnt();
		if (totalCnt < 0) {
			totalCnt = rnoLtePciDao.getPciAnalysisTaskCount(cond, account);
			page.setTotalCnt((int) totalCnt);
		}
		int startIndex = page.calculateStart();
		int cnt = page.getPageSize();
		List<Map<String, Object>> res = rnoLtePciDao.queryPciPlanTaskByPage(cond, account, startIndex, cnt);
		return res;
	}

	// 干扰矩阵

	/**
	 * 
	 * @title 分页查询4g干扰矩阵信息
	 * @param condition
	 * @param page
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午10:52:43
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> queryLteInterferMartixByPage(Map<String, String> condition, Page page) {
		log.debug("进入方法：queryLteInterferMartixByPage。condition=" + condition + ",page=" + page);
		if (page == null) {
			log.warn("方法：queryLteInterferMartixByPage的参数：page 是空！无法查询!");
			return Collections.emptyList();
		}
		long totalCnt = page.getTotalCnt();
		if (totalCnt < 0) {
			totalCnt = rnoLtePciDao.getLteInterferMartixCount(condition);
			page.setTotalCnt((int) totalCnt);
		}
		int startIndex = page.calculateStart();
		int cnt = page.getPageSize();
		List<Map<String, Object>> res = rnoLtePciDao.queryLteInterferMartixByPage(condition, startIndex, cnt);
		return res;
	}

	/**
	 * 获取任务名列表
	 * 
	 * @param attachParams
	 * @return
	 * @author chen.c10
	 * @date 2015年10月23日 上午11:11:08
	 * @company 怡创科技
	 * @version V1.0
	 */
	public List<String> queryTaksNameListByCityId(long cityId) {
		List<Map<String, String>> taskNameList = new ArrayList<Map<String, String>>();
		List<String> result = new ArrayList<String>();
		taskNameList = rnoLtePciDao.getTaksNameListByCityId(cityId);
		for (Map<String, String> map : taskNameList) {
			result.add(map.get("TASK_NAME").trim());
		}
		return result;
	}

	/**
	 * 
	 * @title 检查这周是否计算过4g pci干扰矩阵
	 * @param areaId
	 * @param thisMonday
	 * @param today
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15下午1:59:46
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> checkLteInterMartixThisWeek(long areaId, String thisMonday, String today) {
		return rnoLtePciDao.checkLteInterMartixThisWeek(areaId, thisMonday, today);
	}

	/**
	 * 
	 * @title 新增4g pci 干扰矩阵
	 * @param condition
	 * @param account
	 * @author chao.xj
	 * @date 2015-4-15下午2:58:38
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void addLteInterMartix(final RnoLteInterMatrixTaskInfo taskInfo) {
		log.debug("进入方法：addLteInterMartix。taskInfo=" + taskInfo);
		// 数据量
		long mrRecordNum = rno4gPciDao.queryMrDataRecordsCount(taskInfo.getCityId(), taskInfo.getBegTime(),
				taskInfo.getEndTime());
		long hoRecordNum = rno4gPciDao.queryHoDataRecordsCount(taskInfo.getCityId(), taskInfo.getBegTime(),
				taskInfo.getEndTime());
		String dataType = taskInfo.getDataType();
		String dataDescription = "";
		long recordNum = 0;
		if (dataType.equals("MR")) {
			recordNum = mrRecordNum;
			dataDescription = "MR:" + mrRecordNum;
		} else if (dataType.equals("HO")) {
			recordNum = hoRecordNum;
			dataDescription = "HO:" + hoRecordNum;
		} else {
			recordNum = mrRecordNum + hoRecordNum;
			dataDescription = "MR:" + mrRecordNum + ";HO:" + hoRecordNum;
		}
		taskInfo.setDataDescription(dataDescription);
		taskInfo.setRecordNum(recordNum);

		JobProfile job = new JobProfile();
		job.setAccount(taskInfo.getAccount());
		job.setJobName("计算4gpci干扰矩阵新算法");
		job.setJobType(taskInfo.getJobType());
		job.setSubmitTime(new Date());
		job.setDescription("4g pci干扰矩阵job 新算法");

		JobClientDelegate.getJobClient().submitJob(job, new JobAddCallback<Boolean>() {
			@Override
			public Boolean doWhenJobAdded(JobProfile job) {
				// 获取jobId
				long jobId = job.getJobId();
				// 在数据库创建干扰矩阵计算任务
				boolean flag = rnoLtePciDao.createLteInterMartixRec(jobId, taskInfo);
				if (!flag) {
					log.error("jobId=" + jobId + " 的4g pci 干扰矩阵任务创建失败");
					return false;
				}
				return true;
			}
		});

		log.debug("退出方法：addLteInterMartix。job=" + job);
	}

	@Override
	public boolean stopJobByJobIdAndMrJobIdForAjaxAction(MrJobCond mrJobCond) {
		log.debug("进入方法：stopJobByJobIdAndMrJobIdForAjaxAction。cond=" + mrJobCond);
		long jobId = mrJobCond.getJobId();
		String mrJobId = mrJobCond.getMrJobId();
		String runType = mrJobCond.getRunType();
		String account = mrJobCond.getAccount();

		boolean flag = true;

		// 停止runnable的job
		JobClientDelegate.getJobClient().killJob(new JobProfile(jobId), account, "用户主动停止");
		/*
		 * if (!JobState.isInEndState(JobClientDelegate.getJobClient().killJob(new JobProfile(jobId), account,
		 * "用户主动停止").getJobState())) {
		 * //flag = false;
		 * }
		 */

		// 停止Hadoop的mapreduce的job
		if ("hadoop".equals(runType)) {
			try {
				Job mrJob = new Cluster(new Configuration()).getJob(JobID.forName(mrJobId));
				if (mrJob != null) {
					log.debug("通过mrJobId从session获取的job是:" + mrJob);
					mrJob.killJob();
					log.debug(mrJob + "该mr工作状态为：" + mrJob.getJobState());
				}
			} catch (IOException e) {
				log.error(mrJobId + "该mrjob停止失败!");
				flag = false;
				e.printStackTrace();
			} catch (InterruptedException e) {
				log.error(mrJobId + "该mrjob停止失败!");
				flag = false;
				e.printStackTrace();
			}
		}
		log.debug("退出方法：stopJobByJobIdAndMrJobIdForAjaxAction。flag=" + flag);
		return flag;
	}

	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond) {
		return rnoLtePciDao.querySfDataFromHbaseByPage(cond);
	}

	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond, Page page) {
		return rnoLtePciDao.querySfDataFromHbaseByPage(cond, page);
	}
}