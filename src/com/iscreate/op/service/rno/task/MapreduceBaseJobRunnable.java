package com.iscreate.op.service.rno.task;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.BaseJobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;

public abstract class MapreduceBaseJobRunnable extends BaseJobRunnable {
	private static Log log = LogFactory.getLog(MapreduceBaseJobRunnable.class);
	protected Connection connection;
	protected Statement stmt;
	private Connection updateStatusConnection;
	protected Statement updateStatusStmt;
	protected JobProfile job = null;
	protected JobStatus status = null;
	protected JobReport report = null;
	protected long jobId = -1;
	protected Date startTime = null;

	public MapreduceBaseJobRunnable() {
		startTime = new Date();
		DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
		connection = DataSourceConn.initInstance().getConnection();
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("发生系统错误，无法创建数据库执行器！");

			// 保存报告信息
			report.setStage("创建数据库执行器");
			report.setBegTime(startTime);
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
			return;
		}
		updateStatusConnection = DataSourceConn.initInstance().getConnection();
		try {
			updateStatusStmt = updateStatusConnection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("发生系统错误，无法创建数据库执行器！");

			// 保存报告信息
			report.setStage("创建数据库执行器");
			report.setBegTime(startTime);
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
			return;
		}
	}

	@Override
	public JobStatus runJob(JobProfile job) {
		this.job = job;
		jobId = job.getJobId();
		if (jobId > 0) {
			status = new JobStatus(jobId);
			report = new JobReport(jobId);
		} else {
			status = new JobStatus(jobId);
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			status.setProgress("失败！任务id无效！");
			return status;
		}
		if (connection != null && updateStatusConnection != null) {
			try {
				connection.setAutoCommit(false);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		} else {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			status.setProgress("失败！数据库连接失败！");
			return status;
		}
		try {
			if (status != null && report != null) {
				if (stmt != null && updateStatusStmt != null) {
					runJobInternal();
				} else {
					status.setJobState(JobState.Failed);
					status.setUpdateTime(new Date());
					status.setProgress("失败！获取stmt失败！");
					return status;
				}
			} else {
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				status.setProgress("失败！生成任务信息失败！");
				return status;
			}
		} catch (Exception e) {
			e.printStackTrace();
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			status.setProgress("失败！");
		}
		if (JobState.isEndWithSucceded(status.getJobState())) {
			try {
				connection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else {
			try {
				// connection.rollback();
				connection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (updateStatusConnection != null) {
			try {
				updateStatusConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return status;
	}

	@Override
	public void releaseRes() {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (updateStatusStmt != null) {
			try {
				updateStatusStmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (updateStatusConnection != null) {
			try {
				updateStatusConnection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public abstract JobStatus runJobInternal();
}
