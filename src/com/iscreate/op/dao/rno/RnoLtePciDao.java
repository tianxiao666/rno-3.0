package com.iscreate.op.dao.rno;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask.TaskInfo;
import com.iscreate.op.pojo.rno.RnoLteInterMatrixTaskInfo;
import com.iscreate.op.pojo.rno.RnoThreshold;

/**
 * @author brightming
 * @version 1.0
 * @created 17-һ��-2014 11:19:16
 */
public interface RnoLtePciDao {

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
	public List<RnoThreshold> getThresholdsByModuleType(String moduleType);

	/**
	 * 
	 * @title 通过区域及起始时间和厂家,及数据类型从Hbase获取MR数据描述记录情况
	 * @param cityId
	 * @param startTime
	 * @param endTime
	 * @param factory
	 *            ERI,ZTE
	 * @param dataType
	 *            HO,MR
	 * @return
	 * @author chao.xj
	 * @date 2015-3-27下午4:33:02
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> getDataDescRecordFromHbase(long cityId, String startTime, String endTime,
			String factory, String dataType);

	/**
	 * 
	 * @title 通过jobId获取pci规划记录任务信息
	 * @param stmt
	 * @param jobId
	 * @return
	 * @author chao.xj
	 * @date 2015-3-30上午10:14:31
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, Object> queryPciPlanJobRecByJobId(Statement stmt, long jobId);

	/**
	 * 通过jobId获取PCI规划记录任务信息采用倒序排列
	 * 
	 * @param stmt
	 * @param jobId
	 * @return
	 * @author chen.c10
	 * @date 2015年10月26日 上午9:39:43
	 * @company 怡创科技
	 * @version V1.0
	 */
	public List<Map<String, Object>> queryPciPlanJobRecWithCreateTimeDescByJobId(Statement stmt, long jobId);

	/**
	 * 统计pci自动规划任务数量
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public long getPciAnalysisTaskCount(Map<String, String> cond, String account);

	/**
	 * 分页获取pci自动规划任务信息
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public List<Map<String, Object>> queryPciPlanTaskByPage(Map<String, String> cond, String account, int startIndex,
			int cnt);

	/**
	 * 
	 * @title 通过statement更新PCI规划的job状态
	 * @param stmt
	 * @param jobId
	 * @param workStatus
	 * @return
	 * @author chao.xj
	 * @date 2015-3-30上午10:40:40
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public boolean updatePciPlanWorkStatusByStmt(Statement stmt, long jobId, String workStatus);

	/**
	 * 
	 * @title 通过城市ID获取从基站标识到lte小区的映射集合
	 * @param stmt
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2015-3-31下午3:23:10
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, List<String>> getEnodebIdForCellsMap(Statement stmt, long cityId);

	/**
	 * 
	 * @param stmt
	 * @param cityId
	 * @return
	 * @author chen.c10
	 * @date 2016年3月29日
	 * @version RNO 3.0.1
	 */
	public Map<String, String> getCell2EnodebIdMap(Statement stmt, long cityId);

	/**
	 * 通过城市ID获取小区和经纬度的映射集合
	 * 
	 * @param stmt
	 * @param cityId
	 * @return
	 * @author chen.c10
	 * @date 2015年12月10日 下午4:38:56
	 * @company 怡创科技
	 * @version V1.0
	 */
	public Map<String, Object> getParameterForCellsMap(Statement stmt, long cityId);

	/**
	 * 更新mr任务的id
	 * @param stmt
	 * @param jobId
	 * @param mrJobId
	 * @param type
	 * @author chen.c10	
	 * @date 2016年4月22日
	 * @version RNO 3.0.1
	 */
	public void addMapReduceJobId(Statement stmt, long jobId, String mrJobId, String type);

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
	public Map<String, List<String>> getLteCellInfoByCellId(Statement stmt, long cityId);

	// 干扰矩阵

	/**
	 * 
	 * @title 获取符合条件的4g干扰矩阵数量
	 * @param condition
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午10:37:05
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public long getLteInterferMartixCount(Map<String, String> condition);

	/**
	 * 
	 * @title 分页获取符合条件的4g干扰矩阵的详情
	 * @param condition
	 * @param startIndex
	 * @param cnt
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午10:34:05
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> queryLteInterferMartixByPage(Map<String, String> condition, int startIndex, int cnt);

	/**
	 * 根据城市ID获取任务名列表
	 * 
	 * @param cityId
	 * @return
	 * @author chen.c10
	 * @date 2015年10月23日 上午11:25:55
	 * @company 怡创科技
	 * @version V1.0
	 */
	public List<Map<String, String>> getTaksNameListByCityId(long cityId);

	/**
	 * 
	 * @title 检查这周是否计算过4G MR干扰矩阵
	 * @param cityId
	 * @param thisMonday
	 * @param today
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15下午1:57:05
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> checkLteInterMartixThisWeek(long cityId, String thisMonday, String today);

	/**
	 * 
	 * @title 创建MR 4g 干扰矩阵计算任务
	 * @param interMartix
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15下午2:52:26
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public boolean createLteInterMartixRec(long jobId, RnoLteInterMatrixTaskInfo taskInfo);

	/**
	 * 分页查询扫频数据
	 * 
	 * @param cond
	 * @return
	 * @author chen.c10
	 * @date 2016年3月22日
	 * @version RNO 3.0.1
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond);

	/**
	 * 分页查询扫频数据
	 * 
	 * @param cond
	 * @return
	 * @author chen.c10
	 * @date 2016年3月22日
	 * @version RNO 3.0.1
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond, Page page);

	/**
	 * 保存pci计算任务信息
	 * 
	 * @param jobId
	 * @param rnoThresholds
	 * @param taskInfo
	 * @return
	 * @author chen.c10
	 * @date 2016年3月22日
	 * @version RNO 3.0.1
	 */
	public Map<String, Object> savePciPlanAnalysisTaskInfo(long jobId, List<RnoThreshold> rnoThresholds,
			TaskInfo taskInfo);

	/**
	 * 获取seq
	 * 
	 * @param seq
	 * @return
	 * @author chen.c10
	 * @date 2016年3月22日
	 * @version RNO 3.0.1
	 */
	public long getNextSeqValue(String seq);

	/**
	 * @param dcId
	 * @author chen.c10
	 * @date 2016年3月24日
	 * @version RNO 3.0.1
	 */
	public long getJobIdFromDataCollectById(long dcId);
}