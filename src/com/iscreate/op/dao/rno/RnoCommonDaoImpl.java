package com.iscreate.op.dao.rno;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.stereotype.Repository;

import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoMapLnglatRelaGps;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.RnoHelper;

@Repository(value = "rnoCommonDao")
@Scope("prototype")
public class RnoCommonDaoImpl implements RnoCommonDao {

	private static final Logger logger = LoggerFactory.getLogger(RnoCellDaoImpl.class);
	// ---注入----//
	@Autowired
	private HibernateTemplate hibernateTemplate;

	/**
	 * 
	 * 通过区域ID获取指定区域的地图经纬度纠偏集合
	 * 
	 * @author chao.xj
	 * @date 2013-10-24下午02:10:37
	 */

	@SuppressWarnings("unchecked")
	public List<RnoMapLnglatRelaGps> getSpecialAreaRnoMapLnglatRelaGpsList(long areaid, String mapType) {

		logger.info("进入方法：getSpecialAreaRnoMapLnglatRelaGpsList。areaId=" + areaid);
		// String fieldName = "RESOURCE_USE_RATE";// "TRAFFIC_PER_LINE";

		// final String
		// sql="SELECT DISTINCT(bottomleft),GPS,BAIDU,OFFSET,AREAID,TOPLEFT,TOPRIGHT,BOTTOMRIGHT,MAP_LNGLAT_RELA_GPS_ID from RNO_MAP_LNGLAT_RELA_GPS where areaid='"+areaid+"' ORDER BY bottomleft";
		final String sql = "SELECT * from RNO_MAP_LNGLAT_RELA_GPS WHERE AREAID='" + areaid + "' and MAPTYPE='"
				+ mapType + "' ORDER BY BOTTOMLEFT";
		// System.out.println(sql);
		return hibernateTemplate.executeFind(new HibernateCallback<List<RnoMapLnglatRelaGps>>() {
			public List<RnoMapLnglatRelaGps> doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				// query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				query.addEntity(RnoMapLnglatRelaGps.class);
				List<RnoMapLnglatRelaGps> list = query.list();
				/*
				 * for (int i = 0; i < list.size(); i++) {
				 * System.out.println
				 * (((RnoMapLnglatRelaGps)list.get(i)).getGps()); }
				 */
				return list;
			}
		});

	}

	/**
	 * 获取指定类型的数据
	 * 
	 * @param class1
	 * @param idField
	 * @param areaIdField
	 * @param cfids
	 * @param areaIds
	 *            如果areaIds为空，则表示查询全部
	 * @author brightming 2013-11-27 下午2:50:11
	 */
	@SuppressWarnings("rawtypes")
	public List getObjectByIdsInArea(final Class<?> class1, final String idField, final String areaIdField,
			final List<Long> configIds, final List<Long> areaIds) {
		if (configIds == null || configIds.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		try {
			return hibernateTemplate.executeFind(new HibernateCallback<List>() {

				@SuppressWarnings("unused")
				public List doInHibernate(Session arg0) throws HibernateException, SQLException {

					Criteria criteria = arg0.createCriteria(class1);
					List<Long> cids = configIds;
					if (cids == null) {
						cids = new ArrayList<Long>();
					}
					List<Long> as = areaIds;
					if (as == null) {
						as = new ArrayList<Long>();
					}
					if (areaIds != null && areaIds.size() > 0) {
						criteria.add(Restrictions.and(Restrictions.in(idField, cids), Restrictions.in(areaIdField, as)));
					} else {
						criteria.add(Restrictions.in(idField, cids));
					}
					return criteria.list();
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
			return Collections.EMPTY_LIST;
		}
	}

	/**
	 * 获取城市下所有的bsc
	 * 
	 * @param cityId
	 * @return
	 * @author brightming 2014-2-10 下午3:57:14
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getAllBscsInCity(final long cityId) {
		logger.info("进入方法：getAllBscsInCity. cityId=" + cityId);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				List<Long> subAreas = AuthDsDataDaoImpl.getSubAreaIdsByCityId(cityId);
				String areaStrs = "";
				for (Long id : subAreas) {
					areaStrs += id + ",";
				}
				if (areaStrs.length() > 0) {
					areaStrs = areaStrs.substring(0, areaStrs.length() - 1);
				}
				if (areaStrs.length() > 0) {
					String sql = "select bsc_id,engname from rno_bsc where bsc_id in (select bsc_id from RNO_BSC_RELA_AREA where area_id in ("
							+ areaStrs + "))";
					SQLQuery query = arg0.createSQLQuery(sql);
					query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					return query.list();
				} else {
					return Collections.EMPTY_LIST;
				}
			}
		});
	}

	/**
	 * 获取指定bsc下的所有小区
	 * 
	 * @param bsc
	 * @return
	 * @author brightming 2014-2-10 下午4:04:41
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getAllCellsInBsc(final long bscId) {
		logger.info("进入方法：getAllCellsInBsc. bscId=" + bscId);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {

				String sql = "select LABEL,name from CELL where bsc_id=" + bscId;
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}
		});
	}

	/**
	 * 模糊匹配小区
	 * 
	 * @param cityId
	 * @param cellWord
	 * @return
	 * @author brightming 2014-2-10 下午3:55:04
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> findCellWithPartlyWords(final long cityId, final String cellWord) {
		logger.info("进入方法：findCellWithPartlyWords。cityId=" + cityId + ",cellWord=" + cellWord);
		if (cellWord == null || "".equals(cellWord.trim())) {
			logger.warn("传入的搜索小区词是空的！");
			return Collections.emptyList();
		}
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				List<Long> subAreas = AuthDsDataDaoImpl.getSubAreaIdsByCityId(cityId);
				String areaStrs = "";
				for (Long id : subAreas) {
					areaStrs += id + ",";
				}
				if (areaStrs.length() > 0) {
					areaStrs = areaStrs.substring(0, areaStrs.length() - 1);
				}
				if (areaStrs.length() > 0) {
					String sql = "select mid1.*,rno_bsc.engname from (select label,name,bsc_id from cell where area_id in ( "
							+ areaStrs
							+ " ) and label like '%"
							+ cellWord.toUpperCase()
							+ "%')mid1 "
							+ "inner join rno_bsc on mid1.bsc_id=rno_bsc.bsc_id";
					SQLQuery query = arg0.createSQLQuery(sql);
					query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
					return query.list();
				} else {
					return Collections.EMPTY_LIST;
				}
			}
		});
	}

	/**
	 * 获取指定城市下的bsc和小区
	 * 
	 * @param cityId
	 * @return key:engname为bsc名称 key:label为cell名称 key:name为cell中文名称
	 * 
	 * @author brightming 2014-2-11 下午12:09:05
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getBscCellInCity(final long cityId) {
		logger.info("进入方法：getBscCellInCity. cityId=" + cityId);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				String areaIdStrs = AuthDsDataDaoImpl.getSubAreaAndSelfIdListStrByParentId(cityId);
				String sql = "select mid1.label,mid1.name, bsc.engname,bsc.manufacturers  from "
						+ " ( select distinct(label),name,bsc_id from cell " + " where area_id in (" + areaIdStrs
						+ "))mid1 "
						+ " left join (select bsc_id,engname,manufacturers from rno_bsc where status='N') bsc"
						+ " on mid1.bsc_id=bsc.bsc_id " + " order by bsc.engname,mid1.label";
				SQLQuery query = arg0.createSQLQuery(sql);
				logger.info("获取指定城市下的bsc和小区,sql=" + sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}
		});
	}

	/**
	 * 保存数据上传的记录
	 * 
	 * @param dataRec
	 * @return
	 * @author brightming 2014-8-21 下午3:51:58
	 */
	public long saveDataUploadRec(final RnoDataCollectRec dataRec) {
		logger.debug("进入方法saveDataUploadRec。dataRec=" + dataRec);
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			@Override
			public Long doInHibernate(Session session) throws HibernateException, SQLException {
				Connection conn = SessionFactoryUtils.getDataSource(session.getSessionFactory()).getConnection();
				long dataId = RnoHelper.getNextSeqValue("SEQ_RNO_DATA_COLLECT_ID", conn);
				try {
					conn.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
				DateUtil dateUtil = new DateUtil();
				String sql = "insert into RNO_DATA_COLLECT_REC(DATA_COLLECT_ID,UPLOAD_TIME,BUSINESS_TIME,FILE_NAME,ORI_FILE_NAME,ACCOUNT,CITY_ID,BUSINESS_DATA_TYPE,FILE_SIZE,FULL_PATH,FILE_STATUS,JOB_ID,JOB_UUID,IS_TO_HBASE,ID_FOR_CELL) "
						+ "values("
						+ dataId
						+ ",to_date('"
						+ dateUtil.format_yyyyMMddHHmmss(dataRec.getUploadTime())
						+ "','yyyy-mm-dd HH24:mi:ss'),to_date('"
						+ dateUtil.format_yyyyMMddHHmmss(dataRec.getBusinessTime())
						+ "','yyyy-mm-dd HH24:mi:ss'),"
						+ "'"
						+ dataRec.getFileName()
						+ "','"
						+ dataRec.getOriFileName()
						+ "','"
						+ dataRec.getAccount()
						+ "',"
						+ dataRec.getCityId()
						+ ","
						+ dataRec.getBusinessDataType()
						+ ","
						+ dataRec.getFileSize()
						+ ",'"
						+ dataRec.getFullPath()
						+ "','"
						+ dataRec.getFileStatus()
						+ "',"
						+ dataRec.getJobId()
						+ ",'"
						+ dataRec.getJobUuid()
						+ "','"
						+ dataRec.getIsToHbase()
						+ "','"
						+ dataRec.getIdForCell() + "')";
				logger.debug("saveDataUploadRec保存数据上传记录的sql=" + sql);
				SQLQuery query = session.createSQLQuery(sql);
				query.executeUpdate();
				logger.debug("退出方法saveDataUploadRec。dataId=" + dataId);
				return dataId;
			}
		});
	}

	/**
	 * 保存用户的配置
	 * 
	 * @author peng.jm
	 * @date 2014-9-25下午02:51:15
	 */
	public boolean saveUserConfig(final String account, final long cityId) {
		logger.info("进入saveUserConfig。 account=" + account + ", cityId=" + cityId);
		return hibernateTemplate.execute(new HibernateCallback<Boolean>() {
			public Boolean doInHibernate(Session arg0) throws HibernateException, SQLException {
				String sql = " MERGE INTO RNO_USER_CONFIG t " + "	USING (SELECT '" + account
						+ "' ACCOUNT FROM dual) t2  " + "	  ON ( t.ACCOUNT = t2.ACCOUNT)  " + "	WHEN MATCHED THEN   "
						+ "	  UPDATE SET t.ATTEN_CITY_ID =" + cityId + "	WHEN NOT MATCHED THEN   "
						+ "	  INSERT (ACCOUNT,ATTEN_CITY_ID) " + " VALUES('" + account + "'," + cityId + ") ";
				logger.debug("saveUserConfig的sql=" + sql);
				SQLQuery query = arg0.createSQLQuery(sql);
				int num = query.executeUpdate();
				logger.debug("saveUserConfig的受影响行数，num=" + num);
				if (num > 0) {
					return true;
				} else {
					return false;
				}
			}
		});
	}

	/**
	 * 获取用户配置的默认城市id
	 * 
	 * @param account
	 * @return
	 * @author peng.jm
	 * @date 2014-9-25下午04:12:21
	 */
	public long getUserConfigAreaId(final String account) {
		logger.info("进入saveUserConfig。 account=" + account);
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			public Long doInHibernate(Session arg0) throws HibernateException, SQLException {
				String sql = "select atten_city_id from rno_user_config where account='" + account + "'";
				logger.debug("getUserConfigAreaId的sql=" + sql);
				SQLQuery query = arg0.createSQLQuery(sql);
				BigDecimal res = (BigDecimal) query.uniqueResult();
				long result = -1;
				if (res != null) {
					result = res.longValue();
				}
				logger.debug("getUserConfigAreaId的结果为，cityId=" + result);
				return result;
			}
		});
	}

	/**
	 * 通过areaStr获取所有BSC
	 * 
	 * @param areaStr
	 * @return
	 * @author peng.jm
	 * @date 2014-10-22下午02:04:29
	 */
	public List<Map<String, Object>> getAllBscByAreaStr(final String areaStr) {
		logger.info("进入getAllBscByAreaStr。 areaStr=" + areaStr);
		return hibernateTemplate.execute(new HibernateCallback<List<Map<String, Object>>>() {
			@SuppressWarnings("unchecked")
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				String sql = "select * from rno_bsc" + " where status = 'N' "
						+ "and bsc_id in (select bsc_id from rno_bsc_rela_area" + " where area_id in(" + areaStr
						+ ")) order by engname";
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.iscreate.op.dao.rno.RnoCommonDao#updateJobNameById(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean updateJobNameById(String jobName, long jobId) {
		logger.debug("进入updateJobNameById。 jobName=" + jobName + ",jobId=" + jobId);
		final String sql = "update rno_job  set job_name='" + jobName + "' " + " where job_id=" + jobId;
		logger.debug("updateJobNameById的sql:" + sql);
		return hibernateTemplate.execute(new HibernateCallback<Boolean>() {
			public Boolean doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				int res = query.executeUpdate();
				logger.debug("退出updateJobNameById,受影响行数=" + res);
				boolean result = false;
				if (res > 0) {
					result = true;
				} else {
					result = false;
				}
				return result;
			}
		});
	}

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
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getLteCellInCity(final long cityId) {
		logger.info("进入方法：getLteCellInCity. cityId=" + cityId);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				String areaIdStrs = AuthDsDataDaoImpl.getSubAreaAndSelfIdListStrByParentId(cityId);
				String sql = "select t1.business_cell_id label,		   " + "       t1.cell_name name,                    "
						+ "       t2.enodeb_name engname,               "
						+ "       '1' manufacturers                     "
						+ "  from (select distinct (business_cell_id),  "
						+ "                        enodeb_id,           "
						+ "                        cell_name,           "
						+ "                        '1' manufacturers    "
						+ "          from rno_lte_cell                  " + "         where area_id in (" + areaIdStrs
						+ ")) t1" + "  left join (select enodeb_id, enodeb_name   "
						+ "               from rno_lte_enodeb           " + "              where area_id in ("
						+ areaIdStrs + ")) t2" + "    on t1.enodeb_id = t2.enodeb_id           "
						+ " order by t2.enodeb_name, t1.business_cell_id";

				sql = "select distinct (business_cell_id) label," + "                'TAC_'||tac engname,             "
						+ "                cell_name name,          " + "                '1' manufacturers        "
						+ "  from rno_lte_cell                      " + " where area_id in (" + areaIdStrs
						+ ") and tac !=0      ";
				SQLQuery query = arg0.createSQLQuery(sql);
				logger.info("获取指定城市下的lte enodeb和小区,sql=" + sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String, Object>> cells = query.list();
				return cells;
			}
		});
	}

	/**
	 * 获取指定区域集合的所有上级区域
	 * 
	 * @param area_ids
	 * @param area_level
	 * @return
	 * @author brightming 2013-10-17 上午11:39:22
	 */
	@SuppressWarnings("unchecked")
	public List<Area> getSpecialUpperAreas(long[] area_ids, String area_level) {
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		for (int i = 0; i < area_ids.length; i++) {
			buf.append(area_ids[i] + ",");
		}
		buf.deleteCharAt(buf.length() - 1);
		buf.append(")");
		final String sql = "select * "
				+ "     from (select distinct area_id,"
				+ "                  name,"
				+ "                  area_level,"
				+ "                  path,"
				+ "                  longitude,"
				+ "                  latitude,"
				+ "                  parent_id "
				+ "             from sys_area a "
				+ "            start with a.area_id in "
				+ buf.toString()
				+ "          connect by a.area_id=prior a.parent_id) tmp "
				+ "    where tmp.area_level='" + area_level + "'";
		logger.debug("getSpecialUpperAreas。 sql=" + sql);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Area>>() {
			public List<Area> doInHibernate(Session session) throws HibernateException, SQLException {
				SQLQuery query = session.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String, Object>> find = query.list();
				List<Area> result = new ArrayList<Area>();
				if (find != null) {
					Area area;
					for (Map<String, Object> one : find) {
						area = Area.fromMap(one);
						if (area != null) {
							result.add(area);
						}
					}
				}
				logger.debug("退出getSpecialUpperAreas。 返回result：" + result);
				return result;
			}
		});
	}

	/**
	 * 获取用户可访问的指定级别的区域
	 * 
	 * @param accountId
	 * @param areaLevel
	 * @return
	 * @author brightming 2013-10-17 上午11:49:35
	 */
	@SuppressWarnings("unchecked")
	public List<Area> getSpecialLevalAreaByAccount(String accountId, String areaLevel) {
		final String sql = "select area_id, name, area_level, path, longitude, latitude, parent_id "
				+ "           from (select distinct area_id, name, area_level, path, longitude, latitude, parent_id " 
				+ "                   from sys_area a"
				+ "                  start with a.area_id in" 
				+ "                            (select Distinct sarea.Area_Id"
				+ "                               from SYS_ACCOUNT       sa,"
				+ "                                    SYS_USER_RELA_ORG suro,"
				+ "                                    SYS_ORG_RELA_AREA sora,"
				+ "                                    SYS_AREA          sarea"
				+ "                              where sa.org_user_id = suro.org_user_id"
				+ "                                and suro.org_id = sora.org_id"
				+ "                                and sarea.area_id = sora.area_id"
				+ "                                and suro.Status = 'A'" 
				+ "                                and sa.account = '" + accountId + "')" 
				+ "                connect by a.area_id = prior a.parent_id) tmp"
				+ "         where tmp.area_level = '" + areaLevel + "'"
				+ "         order by tmp.area_id";
		logger.debug("getSpecialLevalAreaByAccount。 sql=" + sql);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Area>>() {
			public List<Area> doInHibernate(Session session) throws HibernateException, SQLException {
				SQLQuery query = session.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String, Object>> find = query.list();
				List<Area> result = new ArrayList<Area>();
				if (find != null) {
					Area area;
					for (Map<String, Object> one : find) {
						area = Area.fromMap(one);
						if (area != null) {
							result.add(area);
						}
					}
				}
				logger.debug("退出getSpecialSubAreasByAccountAndParentArea。 返回result：" + result);
				return result;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public List<Area> getSpecialSubAreasByAccountAndParentArea(String account, long parentAreaId, String subAreaLevel) {
		final String sql = "select area_id, name, area_level, path, longitude, latitude, parent_id "
				+ "           from (select distinct area_id, name, area_level, path, longitude, latitude, parent_id " 
				+ "                   from sys_area a"
				+ "                  start with a.area_id in" 
				+ "                           (select Distinct sarea.Area_Id"
				+ "                              from SYS_ACCOUNT       sa,"
				+ "                                   SYS_USER_RELA_ORG suro,"
				+ "                                   SYS_ORG_RELA_AREA sora,"
				+ "                                   SYS_AREA          sarea"
				+ "                             where sa.org_user_id = suro.org_user_id"
				+ "                               and suro.org_id = sora.org_id"
				+ "                               and sarea.area_id = sora.area_id"
				+ "                               and suro.Status = 'A'" 
				+ "                               and sa.account = '" + account + "')" 
				+ "                connect by a.area_id = prior a.parent_id) tmp" 
				+ "         where tmp.parent_id = " + parentAreaId 
				+ "           and tmp.area_level = '" + subAreaLevel + "'"
				+ "         order by tmp.area_id";
		logger.debug("getSpecialSubAreasByAccountAndParentArea。 sql=" + sql);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Area>>() {
			public List<Area> doInHibernate(Session session) throws HibernateException, SQLException {
				SQLQuery query = session.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String, Object>> find = query.list();
				List<Area> result = new ArrayList<Area>();
				if (find != null) {
					Area area;
					for (Map<String, Object> one : find) {
						area = Area.fromMap(one);
						if (area != null) {
							result.add(area);
						}
					}
				}
				logger.debug("退出getSpecialSubAreasByAccountAndParentArea。 返回result：" + result);
				return result;
			}
		});
	}

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
	public long getParentIdByCityId(long cityId) {
		final String sql = "select parent_id from sys_area where area_id=" + cityId;
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			public Long doInHibernate(Session session) throws HibernateException, SQLException {
				SQLQuery query = session.createSQLQuery(sql);
				Object find = query.uniqueResult();
				Long result = 0l;
				if (find != null) {
					result = Long.parseLong(find.toString());
				}
				return result;
			}
		});
	}
}
