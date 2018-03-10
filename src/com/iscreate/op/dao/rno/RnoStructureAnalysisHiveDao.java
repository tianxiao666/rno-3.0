package com.iscreate.op.dao.rno;

import java.sql.Connection;

import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoLteStructAnaJobRec;
import com.iscreate.op.service.rno.task.structana.RnoLteStructAnaJobRunnable;

public interface RnoStructureAnalysisHiveDao {
	/**
	 * 
	 * @title LTE结构分析入口
	 * @param worker
	 * @param connection
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-10-29下午4:04:15
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public ResultInfo doLteStructAnalysis(RnoLteStructAnaJobRunnable worker,
			Connection connection, RnoLteStructAnaJobRec jobRec);
}
