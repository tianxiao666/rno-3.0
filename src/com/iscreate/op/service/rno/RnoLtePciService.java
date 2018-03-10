package com.iscreate.op.service.rno;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.action.rno.model.MrJobCond;
import com.iscreate.op.pojo.rno.RnoLteInterMatrixTaskInfo;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;

/**
 * @author brightming
 * @version 1.0
 * @created 17-һ��-2014 11:02:42
 */
public interface RnoLtePciService {

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
	 * @return
	 * @author chao.xj
	 * @date 2015-3-27下午4:33:02
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> getDataRecordFromHbase(long cityId, String startTime, String endTime,
			String factory);

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
	public Map<String, Object> submitPciPlanAnalysisTask(String account, List<RnoThreshold> rnoThresholds,
			RnoLteInterferCalcTask.TaskInfo taskInfo);

	/**
	 * 查询pci自动规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public List<Map<String, Object>> queryPciPlanTaskByPage(Map<String, String> cond, Page newPage, String account);

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
	 * @title 分页查询4g干扰矩阵信息
	 * @param condition
	 * @param page
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午10:52:43
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> queryLteInterferMartixByPage(Map<String, String> condition, Page page);

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
	public List<String> queryTaksNameListByCityId(long cityId);

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
	public List<Map<String, Object>> checkLteInterMartixThisWeek(long areaId, String thisMonday, String today);

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
	public void addLteInterMartix(RnoLteInterMatrixTaskInfo taskInfo);

	/**
	 * 通过任务id和mapreduce任务id停止任务
	 * 
	 * @param mrJobCond
	 * @return
	 */
	public boolean stopJobByJobIdAndMrJobIdForAjaxAction(MrJobCond mrJobCond);

	/**
	 * 分页获取扫频数据
	 * 
	 * @param cond
	 * @return
	 * @author chen.c10
	 * @date 2016年3月24日
	 * @version RNO 3.0.1
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond);

	/**
	 * 分页获取扫频数据
	 * 
	 * @param cond
	 * @return
	 * @author chen.c10
	 * @date 2016年3月24日
	 * @version RNO 3.0.1
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond, Page page);
}