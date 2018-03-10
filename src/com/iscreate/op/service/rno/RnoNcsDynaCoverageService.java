package com.iscreate.op.service.rno;

import java.util.*;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.pojo.rno.AreaRectangle;
import com.iscreate.op.pojo.rno.RnoLteAzimuthAssessTask;
import com.iscreate.op.pojo.rno.RnoMapLnglatRelaGps;

public interface RnoNcsDynaCoverageService {

	/**
	 * 获取画小区动态覆盖图所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	Map<String, List<Map<String, Object>>> getDynaCoverageDataByCityAndDate(
			String cell, long cityId, String startDate, String endDate,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	/**
	 * 获取画小区动态覆盖图(折线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年4月30日9:56:48
	 * @company 怡创科技
	 */
	Map<String, List<Map<String, Object>>> getDynaCoverageData2ByCityAndDate(
			String cell, long cityId, String startDate, String endDate,
			double imgCoeff,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	
	/**
	 * 分页获取2g小区方向角计算任务信息
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月4日14:04:38
	 * @company 怡创科技
	 */
	List<Map<String, Object>> query2GDirectionAngleTaskByPage(
			Map<String, String> cond, Page newPage);
	/**
	 * 查询对应条件的NCS数据记录数量
	 * 
	 * @param cond
	 * @return
	 * @author peng.jm
	 * @date 2014-8-16下午05:02:24
	 */
	long queryNcsDataCountByCond(Map<String, String> cond);
	
	/**
	 * 提交2g小区方向角计算任务信息
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年5月4日14:04:38
	 * @company 怡创科技
	 */
	void add2GDirectionAngleTask(Map<String, String> cond, String account);
	/**
	 * 获取画LTE小区动态覆盖图(贝塞尔曲线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	Map<String, List<Map<String, Object>>> get4GDynaCoverageDataByCityAndDate(
			String lteCellId, long cityId, String startDate, String endDate,double imgSizeCoeff,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	/**
	 * 获取画LTE小区动态覆盖图(折线)所需的数据
	 * @title 
	 * @return
	 * @author peng.jm
	 * @date 2015年3月10日10:59:56
	 * @company 怡创科技
	 */
	Map<String, List<Map<String, Object>>> get4GDynaCoverageData2ByCityAndDate(
			String lteCellId, long cityId, String startDate, String endDate,
			double imgCoeff,double imgSizeCoeff,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	/**
	 * 
	 * @title 获取画LTE小区动态覆盖 in干扰所需的数据
	 * @param lteCellId
	 * @param cityId
	 * @param startDate
	 * @param endDate
	 * @param standardPoints
	 * @return
	 * @author chao.xj
	 * @date 2015-6-12下午2:40:35
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String,String>> get4GDynaCoverageInInferDataByCityAndDate(
			String lteCellId, long cityId, String startDate, String endDate,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	/**
	 * 
	 * @title 获取画LTE小区动态覆盖 out干扰所需的数据
	 * @param lteCellId
	 * @param cityId
	 * @param startDate
	 * @param endDate
	 * @param standardPoints
	 * @return
	 * @author chao.xj
	 * @date 2015-6-12下午2:40:04
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String,String>> get4GDynaCoverageOutInferDataByCityAndDate(
			String lteCellId, long cityId, String startDate, String endDate,
			Map<AreaRectangle, List<RnoMapLnglatRelaGps>> standardPoints);
	/**
	 * 
	 * @title 提交LTE 方位角评估分析计算任务
	 * @param account
	 * @param path
	 * @param taskInfo
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午4:47:53
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, Object> submit4GAzimuthAssessTask(String account,
			final String path,final RnoLteAzimuthAssessTask taskInfo);
	/**
	 * 
	 * @title 分页查询LTE方位角评估分析任务信息
	 * @param condition
	 * @param page
	 * @param account
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午5:11:58
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> queryLteAzimuthAssessTaskByPage(
			Map<String, String> condition, Page page, String account);
	/**
	 * 
	 * @title 通过jobId查询对应的LTE方位角评估结果分析信息
	 * @param jobId
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午5:18:18
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> getLteAzimuthAssessTaskByJobId(long jobId);
}
