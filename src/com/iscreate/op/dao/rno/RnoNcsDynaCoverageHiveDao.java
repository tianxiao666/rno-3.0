package com.iscreate.op.dao.rno;

import java.sql.Connection;

import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoLteAzimuthAssessJobRec;
import com.iscreate.op.pojo.rno.RnoLteStructAnaJobRec;
import com.iscreate.op.service.rno.task.angle.RnoLteAzimuthAssessJobRunnable;
import com.iscreate.op.service.rno.task.structana.RnoLteStructAnaJobRunnable;

public interface RnoNcsDynaCoverageHiveDao {

	/**
	 * 
	 * @title LTE方位角评估分析入口
	 * @param rnoLteAzimuthAssessJobRunnable
	 * @param connection
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午5:45:53
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public ResultInfo doLteAzimuthAssess(RnoLteAzimuthAssessJobRunnable rnoLteAzimuthAssessJobRunnable,
			Connection connection, RnoLteAzimuthAssessJobRec jobRec);
}
