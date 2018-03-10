package com.iscreate.op.service.rno.task.structana;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import com.iscreate.op.dao.rno.RnoStructureAnalysisDaoImplV2;
import com.iscreate.op.dao.rno.RnoStructureAnalysisDaoV2;
import com.iscreate.op.dao.rno.RnoStructureAnalysisHiveDao;
import com.iscreate.op.dao.rno.RnoStructureAnalysisHiveDaoImpl;
import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoLteStructAnaJobRec;
import com.iscreate.op.pojo.rno.RnoStructAnaJobRec;
import com.iscreate.op.pojo.rno.RnoStructureAnalysisTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.BaseJobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;

public class RnoLteStructAnaJobRunnable extends BaseJobRunnable {

	private static Log log = LogFactory.getLog(RnoLteStructAnaJobRunnable.class);
	private static String jobType = "RNO_LTE_STRUCT_ANA";
    private Connection connection;
    private Statement stmt;
	@Override
	public boolean isMyJob(JobProfile job) {
		if (job == null)
			return false;
		else {
			return jobType.equals(job.getJobType());
		}
	}

	@Override
	public JobStatus runJob(JobProfile job) {

		// 获取工作id
		long jobId = job.getJobId();
		// 初始化
		JobStatus status = new JobStatus(jobId);
		JobReport report = new JobReport(jobId);
//		JobWorker worker = super.getJobWorker();

		RnoStructureAnalysisHiveDao structureAnalysisHiveDao = new RnoStructureAnalysisHiveDaoImpl();

		// ---------准备数据库连接----------//
		String oriDs = DataSourceContextHolder.getDataSourceType();
		DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
		connection = DataSourceConn.initInstance().getConnection();
		DataSourceContextHolder.setDataSourceType(oriDs);
		try {
			connection.setAutoCommit(false);
		} catch (SQLException e2) {
			e2.printStackTrace();
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			//
			report.setStage("调整数据连接的属性");
			report.setBegTime(new Date());
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("调整数据连接的属性");
			addJobReport(report);
			//
//			status.setJobState(JobState.Failed);
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		stmt = null;
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("发生系统错误，无法创建数据库执行器！");
			// 保存报告信息
			report.setStage("创建数据库执行器");
			report.setBegTime(new Date());
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("无法创建数据库执行器");
			addJobReport(report);
			// 关闭连接
			try {
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		// 是否停止
		if (super.isStop()) {
			return clearWhenStop(job, connection, stmt);
		}

		String stage = "准备数据";
		Date begTime = new Date(), endTime;
		String finishState = "";
		String attMsg = "";
		// 获取任务的详细信息
		String sql = "";
		sql = "select JOB_ID,BEG_MEA_TIME,END_MEA_TIME,CITY_ID,DL_FILE_NAME,RESULT_DIR,FINISH_STATE,STATUS,CREATE_TIME,MOD_TIME from RNO_LTE_STRUCANA_JOB where job_id="
				+ jobId;
		List<Map<String, Object>> rawDatas = RnoHelper.commonQuery(stmt, sql);
		if (rawDatas == null || rawDatas.isEmpty()) {
			endTime = new Date();
			finishState = DataParseStatus.Failall.toString();
			attMsg = "不存在此LTE结构分析需要的数据！";
			report.setFields(stage, begTime, endTime, finishState, attMsg);
			addJobReport(report);

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());

			// 更新数据
			return status;
		}

		// 反射为对象
		RnoLteStructAnaJobRec jobRec = RnoHelper.commonInjection(
				RnoLteStructAnaJobRec.class, rawDatas.get(0),new DateUtil());
		log.debug("LTE结构分析的数据信息：" + jobRec);

		
		// 更新进度
//		status.setJobRunningStatus(JobRunningStatus.Running);
		status.setJobState(JobState.Running);
		status.setUpdateTime(new Date());
		updateProgress(status);

		sql = "update RNO_LTE_STRUCANA_JOB set FINISH_STATE='开始分析' where job_id="
				+ jobId;
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// 是否停止
		if (super.isStop()) {
			return clearWhenStop(job, connection, stmt);
		}

		// 开始分析
		ResultInfo resultInfo = new ResultInfo(false);
		try {
			begTime = new Date();
			//此处将解析HIVE数据仓库中的数据，需要获取HIVE JDBC的客户端连接 从ORACLE切换至HIVE
			ApplicationContext ac = new ClassPathXmlApplicationContext("spring/datasource-appcontx.xml");
			//获取hive jdbc模板实例
			JdbcTemplate jdbcTemplate = (JdbcTemplate)ac.getBean("jdbcTemplate");
			//切换成HIVE的连接
			Connection hiveConnection= jdbcTemplate.getDataSource().getConnection();
			resultInfo = structureAnalysisHiveDao.doLteStructAnalysis(this,
					hiveConnection, jobRec);
			endTime = new Date();
			// 报告
			stage = "任务总结";
			if (resultInfo.isFlag()) {
				// 任务状态
//				status.setJobRunningStatus(JobRunningStatus.Succeded);
				status.setJobState(JobState.Finished);
				status.setUpdateTime(new Date());
				
				finishState = DataParseStatus.Succeded.toString();
				attMsg = "LTE结构分析完成！";
			} else {
				// 任务状态
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				
				finishState = DataParseStatus.Failall.toString();
				attMsg = "LTE结构分析异常！" + resultInfo.getMsg();
			}
			report.setFields(stage, begTime, endTime, finishState, attMsg);
			addJobReport(report);
		} catch (Exception e) {
			log.error(jobId + "的LTE结构分析出错！");
			e.printStackTrace();
			endTime = new Date();
			// 状态
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			
			// 报告
			stage = "任务总结";
			finishState = DataParseStatus.Failall.toString();
			attMsg = "LTE结构分析异常！";
			report.setFields(stage, begTime, endTime, finishState, attMsg);
			addJobReport(report);
		}
		log.debug("LTE结构分析任务完成，result=" + resultInfo);
		status.setUpdateTime(new Date());
		
		// 更新结构分析表
		sql = "update RNO_LTE_STRUCANA_JOB set FINISH_STATE='分析完成' where job_id="
				+ jobId;
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// 删除临时表的小区
		freeTableSpace(stmt);

		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return status;
	}

	private void freeTableSpace(Statement stmt) {
		try {
			stmt.executeUpdate("truncate table rno_cell_city_t");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_2G_NCS_ANA_T");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_2G_MRR_ANA_T");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_NCS_WEIGHT_MID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_NCS_CLUSTER_CELL_RELA_MID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_NCS_CLUSTER_CELL_MID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_NCS_CLUSTER_MID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.executeUpdate("truncate table RNO_NCS_CELL_ANA_RESULT_MID");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @title 当要求stop时，做一些处理
	 * 更新LTE结构工作任务的状态，并中止该工作进程
	 * @param job
	 * @param connection
	 * @param stmt
	 * @return
	 * @author chao.xj
	 * @date 2015-10-29下午4:23:27
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private JobStatus clearWhenStop(JobProfile job, Connection connection,
			Statement stmt) {
		long jobId = job.getJobId();
		log.warn("本任务需要停止。" + job);
		try {
			stmt.executeUpdate("update RNO_LTE_STRUCANA_JOB set FINISH_STATE='被停止' where job_id="
					+ jobId);
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		try {
			connection.commit();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		try {
			stmt.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
		}
		try {
			connection.close();
		} catch (SQLException e) {
		}

		// 任务
		JobStatus status = new JobStatus(jobId);
//		status.setJobRunningStatus(JobRunningStatus.Stopped);
		status.setJobState(JobState.Killed);
		status.setUpdateTime(new Date());
		return status;
	}

	@Override
	public void releaseRes() {
		if(stmt!=null){
			try {
					stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(connection!=null){
			try {
					try{
						connection.setAutoCommit(true);
					}catch(Exception ee){
						
					connection.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		if(jobStatus==null){
			return;
		}
		if(stmt!=null){
			try {
				String prog=jobStatus.getProgress();
				if(prog==null){
					prog="";
				}
				prog=prog.trim();
				if("".equals(prog)){
					prog=jobStatus.getJobState().getCode();
				}
				stmt.executeUpdate("update RNO_LTE_STRUCANA_JOB set FINISH_STATE='"+prog+"',mod_time=to_date('"+new DateUtil().format_yyyyMMddHHmmss(jobStatus.getUpdateTime())+"','yyyy-mm-dd hh24:mi:ss') where job_id="+jobStatus.getJobId());
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
	}

}
