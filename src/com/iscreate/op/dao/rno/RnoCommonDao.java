package com.iscreate.op.dao.rno;

import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoMapLnglatRelaGps;

public interface RnoCommonDao {

	/**
	 * 
	 * 通过区域ID获取指定区域的地图经纬度纠偏集合
	 * 
	 * @author chao.xj
	 * @date 2013-10-24下午02:10:37
	 */
	public List<RnoMapLnglatRelaGps> getSpecialAreaRnoMapLnglatRelaGpsList(long areaid, String mapType);

	/**
	 * 获取指定类型的数据
	 * 
	 * @param class1
	 * @param idField
	 * @param areaIdField
	 * @param cfids
	 * @param areaIds
	 * @author brightming
	 *         2013-11-27 下午2:50:11
	 */
	@SuppressWarnings("rawtypes")
	public List getObjectByIdsInArea(Class<?> class1, String idField, String areaIdField, List<Long> cfids,
			List<Long> areaIds);

	/**
	 * 获取城市下所有的bsc
	 * 
	 * @param cityId
	 * @return
	 * @author brightming
	 *         2014-2-10 下午3:57:14
	 */
	public List<Map<String, Object>> getAllBscsInCity(long cityId);

	/**
	 * 获取指定bsc下的所有小区
	 * 
	 * @param bsc
	 * @return
	 * @author brightming
	 *         2014-2-10 下午4:04:41
	 */
	public List<Map<String, Object>> getAllCellsInBsc(long bscId);

	/**
	 * 模糊匹配小区
	 * 
	 * @param cityId
	 * @param cellWord
	 * @return
	 * @author brightming
	 *         2014-2-10 下午3:55:04
	 */
	public List<Map<String, Object>> findCellWithPartlyWords(long cityId, String cellWord);

	/**
	 * 获取指定城市下的bsc和小区
	 * 
	 * @param cityId
	 * @return
	 *         key:engname为bsc名称
	 *         key:label为cell名称
	 *         key:name为cell中文名称
	 * @author brightming
	 *         2014-2-11 下午12:09:05
	 */
	public List<Map<String, Object>> getBscCellInCity(long cityId);

	/**
	 * 保存数据上传的记录
	 * 
	 * @param dataRec
	 * @return
	 * @author brightming
	 *         2014-8-21 下午3:51:58
	 */
	public long saveDataUploadRec(RnoDataCollectRec dataRec);

	/**
	 * 保存用户的配置
	 * 
	 * @author peng.jm
	 * @date 2014-9-25下午02:51:15
	 */
	public boolean saveUserConfig(String account, long cityId);

	/**
	 * 获取用户配置的默认城市id
	 * 
	 * @param account
	 * @return
	 * @author peng.jm
	 * @date 2014-9-25下午04:12:21
	 */
	public long getUserConfigAreaId(String account);

	/**
	 * 通过areaStr获取所有BSC
	 * 
	 * @param areaStr
	 * @return
	 * @author peng.jm
	 * @date 2014-10-22下午02:04:29
	 */
	public List<Map<String, Object>> getAllBscByAreaStr(String areaStr);

	/**
	 * @param jobName
	 * @param jobId
	 * @author chen.c10
	 * @date 2016年3月21日
	 * @version RNO 3.0.1
	 */
	public boolean updateJobNameById(String jobName, long jobId);

	/**
	 * 
	 * @title 获取指定城市下的LTE enodeb和小区
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2016年4月7日下午12:00:40
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> getLteCellInCity(final long cityId);

	/**
	 * 获取指定区域集合的所有上级区域
	 * 
	 * @param area_ids
	 * @param area_level
	 * @return
	 * @author brightming 2013-10-17 上午11:39:22
	 */
	public List<Area> getSpecialUpperAreas(long[] area_ids, String area_level);

	/**
	 * 获取用户可访问的指定级别的区域
	 * 
	 * @param accountId
	 * @param areaLevel
	 * @return
	 * @author brightming 2013-10-17 上午11:49:35
	 */
	public List<Area> getSpecialLevalAreaByAccount(String accountId, String areaLevel);

	/**
	 * 根据父区域，获取用户可访问的指定级别的子区域
	 * 
	 * @param account
	 * @param parentAreaId
	 * @param subAreaLevel
	 * @return
	 * @author brightming 2013-10-17 上午11:46:56
	 */
	public List<Area> getSpecialSubAreasByAccountAndParentArea(String account, long parentAreaId, String subAreaLevel);

	/**
	 * 
	 * @title 通过城市ID获取父ID即省
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2015-6-8上午10:41:39
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public long getParentIdByCityId(long cityId);
}
