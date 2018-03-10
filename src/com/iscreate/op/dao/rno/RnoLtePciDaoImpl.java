package com.iscreate.op.dao.rno;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hibernate.transform.Transformers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.pojo.rno.RnoLteInterMatrixTaskInfo;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask.TaskInfo;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.HBTable;
import com.iscreate.op.service.rno.tool.HadoopUser;
import com.iscreate.op.service.rno.tool.RnoHelper;

/**
 * @author brightming
 * @version 1.0
 * @created 17-һ��-2014 11:20:37
 */
@Repository(value = "rnoLtePciDao")
public class RnoLtePciDaoImpl implements RnoLtePciDao {

	private static Log log = LogFactory.getLog(RnoLtePciDaoImpl.class);

	// ---注入----//
	@Autowired
	private HibernateTemplate hibernateTemplate;

	private DateUtil dateUtil = new DateUtil();

	public RnoLtePciDaoImpl() {

	}

	public void finalize() throws Throwable {

	}

	/**
	 * 获取seq
	 */
	public long getNextSeqValue(String seq) {
		final String sql = "select " + seq + ".NEXTVAL as id from dual";
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			@Override
			public Long doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				BigDecimal res = (BigDecimal) query.uniqueResult();
				long seqNum = res.longValue();
				log.debug("getNextSeqValue sql=" + sql + ",返回结果：" + seqNum);
				return seqNum;
			}
		});
	}

	@SuppressWarnings("deprecation")
	private String buildLtePciTaskWhere(Map<String, String> condition, String account) {
		String where = "";
		String val = "";
		long lv;

		if (condition.get("isMine") != null) {
			where += " job.creator='" + account + "'";
		}
		if (condition != null && condition.size() > 0) {
			// 改造结束
			for (String k : condition.keySet()) {
				val = condition.get(k);
				if (val == null || "".equals(val.trim())) {
					continue;
				}
				if ("cityId".equalsIgnoreCase(k)) {
					try {
						lv = Long.parseLong(val);
						if (lv > 0) {
							where += (where.length() == 0 ? " " : " and ") + "sjob.city_id = " + lv;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				} else if ("taskName".equalsIgnoreCase(k)) {
					where += (where.length() == 0 ? " " : " and ") + "job.job_name like '%" + val + "%'";
				} else if ("taskStatus".equalsIgnoreCase(k)) {
					if (!("ALL").equals(val)) {
						if (("LaunchedOrRunning").equals(val)) {
							where += (where.length() == 0 ? " " : " and ") + "(job.job_running_status = 'Launched' "
									+ " or job.job_running_status ='Running')";
						} else {
							where += (where.length() == 0 ? " " : " and ") + "job.job_running_status = '" + val + "'";
						}
					}
				} else if ("meaTime".equalsIgnoreCase(k)) {
					Date dt = RnoHelper.parseDateArbitrary(val);
					if (dt != null) {
						where += (where.length() == 0 ? " " : " and ") + "  sjob.beg_mea_time <= to_date('" + val
								+ "','yyyy-MM-dd') " + "	and sjob.end_mea_time >= to_date('" + val + "','yyyy-MM-dd') ";
					} else {
						log.warn("传入无效的时间字符：" + val);
					}
				} else if ("startSubmitTime".equalsIgnoreCase(k)) {
					Date dt = dateUtil.to_yyyyMMddHHmmssDate(val);
					if (dt != null) {
						where += (where.length() == 0 ? " " : " and ") + " job.create_time >= to_date('" + val
								+ "','yyyy-MM-dd HH24:mi:ss') ";
					} else {
						log.warn("传入无效的时间字符：" + val);
					}
				} else if ("endSubmitTime".equalsIgnoreCase(k)) {
					Date dt = dateUtil.to_yyyyMMddHHmmssDate(val);
					if (dt != null) {
						where += (where.length() == 0 ? " " : " and ") + " job.create_time <= to_date('" + val
								+ "','yyyy-MM-dd HH24:mi:ss') ";
					} else {
						log.warn("传入无效的时间字符：" + val);
					}
				}
			}
		}
		where = " where " + where + " order by sjob.create_time desc nulls last";
		return where;
	}

	/**
	 * 
	 * @title 构建4G干扰矩阵的查询条件
	 * @param condition
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午10:39:54
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private String buildLteInterferMartixWhere(Map<String, String> condition) {
		String where = "";
		String v = "";
		long lv;
		where += " where sjob.STATUS = 'Y' ";

		if (condition != null && condition.size() > 0) {

			// 改造结束
			for (String k : condition.keySet()) {
				v = condition.get(k);
				if (v == null || "".equals(v.trim())) {
					continue;
				}
				if ("cityId".equalsIgnoreCase(k)) {
					try {
						lv = Long.parseLong(v);
						if (lv > 0) {
							where += (where.length() == 0 ? " " : " and ") + "sjob.CITY_ID=" + lv;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if ("interMartixType".equalsIgnoreCase(k)) {
					if (!"ALL".equals(v)) {
						where += (where.length() == 0 ? " " : " and ") + "sjob.TYPE = '" + v + "'";
					}
				} else if ("begTime".equalsIgnoreCase(k)) {
					Date dt = dateUtil.to_yyyyMMddHHmmssDate(v);
					if (dt != null) {
						where += (where.length() == 0 ? " " : " and ") + " sjob.CREATE_DATE>=to_date('" + v
								+ "','yyyy-MM-dd HH24:mi:ss') ";
					} else {
						log.warn("传入无效的时间字符：" + v);
					}
				} else if ("endTime".equalsIgnoreCase(k)) {
					Date dt = dateUtil.to_yyyyMMddHHmmssDate(v);
					if (dt != null) {
						where += (where.length() == 0 ? " " : " and ") + " sjob.CREATE_DATE<=to_date('" + v
								+ "','yyyy-MM-dd HH24:mi:ss') ";
					} else {
						log.warn("传入无效的时间字符：" + v);
					}
				}
			}
		}
		return where;
	}

	// pci 计算

	/**
	 * 
	 * @title 通过模块类型获取阈值门限对象集合
	 * @param moduleType
	 * @return
	 * @author chao.xj
	 * @date 2014-8-31上午11:54:03
	 * @company 怡创科技
	 * @version 1.2
	 */
	@SuppressWarnings("unchecked")
	public List<RnoThreshold> getThresholdsByModuleType(final String moduleType) {
		log.debug("进入方法：getThresholdsByModuleType。moduleType=" + moduleType);
		return hibernateTemplate.executeFind(new HibernateCallback<List<RnoThreshold>>() {
			public List<RnoThreshold> doInHibernate(Session arg0) throws HibernateException, SQLException {
				String sql = "select * from rno_threshold where module_type='" + moduleType + "' order by order_num";
				log.debug("getThresholdsByModuleType ,sql=" + sql);
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String, Object>> rows = query.list();
				List<RnoThreshold> result = new ArrayList<RnoThreshold>();
				if (rows != null && rows.size() > 0) {
					RnoThreshold rec;
					DateUtil dateUtil = new DateUtil();
					for (Map<String, Object> row : rows) {
						rec = RnoHelper.commonInjection(RnoThreshold.class, row, dateUtil);
						if (rec != null) {
							result.add(rec);
						}
					}
				}
				log.debug("退出方法：getThresholdsByModuleType。result.size=" + result.size());
				return result;
			}
		});
	}

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
			String factory, String dataType) {
		log.debug("进入方法：getDataDescRecordFromHbase。cityId=" + cityId + ",startTime=" + startTime + ",endTime="
				+ endTime + ",factory=" + factory + ",dataType=" + dataType);
		DateUtil dateUtil = new DateUtil();
		long sMill = dateUtil.parseDateArbitrary(startTime).getTime();
		String startRow = cityId + "_" + sMill + "_#";
		long eMill = dateUtil.parseDateArbitrary(endTime).getTime();
		String stopRow = cityId + "_" + eMill + "_~";

		String tabName = "";
		if (dataType.equals("HO")) {
			tabName = "RNO_4G_HO_DESC";
		} else if (dataType.equals("MR")) {
			tabName = "RNO_4G_MR_DESC";
		} else {
			return Collections.emptyList();
		}
		org.apache.hadoop.hbase.client.Connection conn = null;
		Table table = null;
		List<Map<String, Object>> mapList = new LinkedList<Map<String, Object>>();
		ResultScanner scanner = null;
		try {
			conn = ConnectionFactory.createConnection(HBaseConfiguration.create(new Configuration()));
			table = conn.getTable(TableName.valueOf(HBTable.valueOf(tabName)));
			if (table == null) {
				return Collections.emptyList();
			}
			// 获取筛选对象
			Scan scan = new Scan();
			// 起止
			scan.setStartRow(Bytes.toBytes(startRow));
			scan.setStopRow(Bytes.toBytes(stopRow));
			// 列
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MEA_TIME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("RECORD_COUNT"));
			// 过滤
			SingleColumnValueFilter filter1 = new SingleColumnValueFilter(Bytes.toBytes("DESCINFO"),
					Bytes.toBytes("FACTORY"), CompareOp.EQUAL, Bytes.toBytes(factory));
			filter1.setFilterIfMissing(true);
			scan.setFilter(filter1);

			// 缓存1000条数据
			scan.setCaching(1000);
			scan.setCacheBlocks(false);

			scanner = table.getScanner(scan);
			for (Result r : scanner) {
				Map<byte[], byte[]> fmap = new LinkedHashMap<byte[], byte[]>();
				fmap.putAll(r.getFamilyMap(Bytes.toBytes("DESCINFO")));

				Map<String, Object> rmap = new LinkedHashMap<String, Object>();
				for (byte[] key : fmap.keySet()) {
					byte[] value = fmap.get(key);
					rmap.put(Bytes.toString(key), Bytes.toString(value));
				}
				mapList.add(rmap);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			if (table != null) {
				try {
					table.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		log.debug("退出方法：getDataDescRecordFromHbase。mapList.size=" + mapList.size());
		return mapList;
	}

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
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map<String, Object> queryPciPlanJobRecByJobId(Statement stmt, long jobId) {
		log.debug("进入方法queryPciPlanJobRecByJobId。jobId=" + jobId);

		String sql = "select * from rno_lte_pci_job where JOB_ID =" + jobId + "order by CREATE_TIME desc";

		log.debug("queryPciPlanJobRecByJobId : sql=" + sql);

		ResultSet rs = null;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		Clob clob = null;
		String optimizeCells = "";

		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Map map = new HashMap();
				map.put("CITY_ID", rs.getString("CITY_ID"));
				map.put("BEG_MEA_TIME", rs.getString("BEG_MEA_TIME"));
				map.put("END_MEA_TIME", rs.getString("END_MEA_TIME"));
				map.put("RESULT_DIR", rs.getString("RESULT_DIR"));
				map.put("PLAN_TYPE", rs.getString("PLAN_TYPE"));
				map.put("CONVER_TYPE", rs.getString("CONVER_TYPE"));
				map.put("RELA_NUM_TYPE", rs.getString("RELA_NUM_TYPE"));
				map.put("IS_CHECK_NCELL", rs.getString("IS_CHECK_NCELL"));
				map.put("RD_FILE_NAME", rs.getString("RD_FILE_NAME"));
				map.put("DATA_FILE_PATH", rs.getString("DATA_FILE_PATH"));
				map.put("IS_EXPORT_ASSOTABLE", rs.getString("IS_EXPORT_ASSOTABLE"));
				map.put("IS_EXPORT_MIDPLAN", rs.getString("IS_EXPORT_MIDPLAN"));
				map.put("IS_EXPORT_NCCHECKPLAN", rs.getString("IS_EXPORT_NCCHECKPLAN"));
				map.put("SF_FILE_NAMES", rs.getString("SF_FILE_NAMES"));
				map.put("FREQ_ADJ_TYPE", rs.getString("FREQ_ADJ_TYPE"));
				map.put("D1FREQ", rs.getString("D1FREQ"));
				map.put("D2FREQ", rs.getString("D2FREQ"));
				map.put("MATRIX_DATA_COLLECT_ID", rs.getLong("MATRIX_DATA_COLLECT_ID"));
				map.put("FLOW_DATA_COLLECT_ID", rs.getLong("FLOW_DATA_COLLECT_ID"));
				// 获取Clob数据
				clob = rs.getClob("OPTIMIZE_CELLS");
				// clob 获取字符串从1开始
				optimizeCells = clob == null ? "" : clob.getSubString(1, (int) clob.length());
				map.put("OPTIMIZE_CELLS", optimizeCells);
				list.add(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		log.debug("退出方法：queryPciPlanJobRecByJobId。返回：" + list.size() + "个记录");
		return list.get(0);
	}

	/**
	 * 通过jobId获取PCI规划记录任务信息采用倒序排列
	 * 
	 * @param stmt
	 * @param jobId
	 * @return
	 * @see com.iscreate.op.dao.rno.RnoStructureQueryDao#queryPciPlanJobRecWithCreateTimeDescByJobId(java.sql.Statement,
	 *      long)
	 * @author chen.c10
	 * @date 2015年10月26日 上午9:40:32
	 * @company 怡创科技
	 * @version V1.0
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<Map<String, Object>> queryPciPlanJobRecWithCreateTimeDescByJobId(Statement stmt, long jobId) {
		log.debug("进入方法queryPciPlanJobRecWithCreateTimeDescByJobId。jobId=" + jobId);

		String sql = "select city_id,beg_mea_time,end_mea_time,result_dir,PLAN_TYPE,CONVER_TYPE,RELA_NUM_TYPE,OPTIMIZE_CELLS,rd_file_name,IS_CHECK_NCELL,DATA_FILE_PATH"
				+ ",IS_EXPORT_ASSOTABLE,IS_EXPORT_MIDPLAN,IS_EXPORT_NCCHECKPLAN from rno_lte_pci_job where JOB_ID ="
				+ jobId + "order by create_time desc";

		log.debug("queryPciPlanJobRecWithCreateTimeDescByJobId : sql=" + sql);

		ResultSet rs = null;
		List list = new ArrayList();
		Clob clob = null;
		Reader inStream = null;
		String optimizeCells = "";

		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Map map = new HashMap();
				map.put("CITY_ID", rs.getString("CITY_ID"));
				map.put("BEG_MEA_TIME", rs.getString("BEG_MEA_TIME"));
				map.put("END_MEA_TIME", rs.getString("END_MEA_TIME"));
				map.put("RESULT_DIR", rs.getString("RESULT_DIR"));
				map.put("PLAN_TYPE", rs.getString("PLAN_TYPE"));
				map.put("CONVER_TYPE", rs.getString("CONVER_TYPE"));
				map.put("RELA_NUM_TYPE", rs.getString("RELA_NUM_TYPE"));
				map.put("IS_CHECK_NCELL", rs.getString("IS_CHECK_NCELL"));
				map.put("RD_FILE_NAME", rs.getString("RD_FILE_NAME"));
				map.put("DATA_FILE_PATH", rs.getString("DATA_FILE_PATH"));
				map.put("IS_EXPORT_ASSOTABLE", rs.getString("IS_EXPORT_ASSOTABLE"));
				map.put("IS_EXPORT_MIDPLAN", rs.getString("IS_EXPORT_MIDPLAN"));
				map.put("IS_EXPORT_NCCHECKPLAN", rs.getString("IS_EXPORT_NCCHECKPLAN"));
				// 获取Clob数据
				clob = rs.getClob("OPTIMIZE_CELLS");
				inStream = clob.getCharacterStream();
				char[] c = new char[(int) clob.length()];
				inStream.read(c);
				optimizeCells = new String(c);
				map.put("OPTIMIZE_CELLS", optimizeCells);

				list.add(map);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				inStream.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		log.debug("退出方法：queryPciPlanJobRecWithCreateTimeDescByJobId。返回：" + list.size() + "个记录");
		return list;
	}

	/**
	 * 统计pci自动规划任务数量
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public long getPciAnalysisTaskCount(Map<String, String> cond, String account) {
		log.debug("进入方法：getPciAnalysisTaskCount。condition=" + cond + ",account=" + account);

		String where = buildLtePciTaskWhere(cond, account);
		final String sql = "select count(*) " + "  from rno_lte_pci_job sjob "
				+ " left join rno_job job on job.job_id = sjob.job_id  " + where;
		log.debug("获取PCI规划任务的总数getPciAnalysisTaskCount : sql=" + sql);
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			@Override
			public Long doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				BigDecimal res = (BigDecimal) query.uniqueResult();
				long cnt = 0;
				if (res != null)
					cnt = res.longValue();
				log.debug("获取PCI规划任务的总数,返回结果：" + cnt);
				return cnt;
			}
		});
	}

	/**
	 * 分页获取pci自动规划任务信息
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public List<Map<String, Object>> queryPciPlanTaskByPage(Map<String, String> cond, String account, int startIndex,
			int cnt) {
		log.debug("进入方法：queryPciPlanTaskByPage。condition=" + cond + ",account=" + account + ",startIndex=" + startIndex
				+ ",cnt=" + cnt);

		String where = buildLtePciTaskWhere(cond, account);
		final String sql = "select *      " + "   from (select t1.*,rownum as rn    "
				+ "	             from ( select sjob.job_id as job_id,   "
				+ "	                       job.job_name as job_name,   "
				+ "	                       job.job_running_status as job_running_status,   "
				+ "	                       to_char(sjob.beg_mea_time,'yyyy-MM-dd HH24:mi:ss') as beg_mea_time,   "
				+ "	                       to_char(sjob.end_mea_time,'yyyy-MM-dd HH24:mi:ss') as end_mea_time,   "
				+ "	                       sjob.dl_file_name as dl_file_name,   "
				+ "	                       sjob.rd_file_name as rd_file_name,   "
				+ "	                       sjob.result_dir as result_dir,   "
				+ "	                       sjob.mr_job_id as mr_job_id,   "
				+ "	                       to_char(job.launch_time,'yyyy-MM-dd HH24:mi:ss') as launch_time,   "
				+ "                         to_char(job.complete_time,'yyyy-MM-dd HH24:mi:ss') as complete_time  "
				+ "                      from rno_lte_pci_job sjob   "
				+ "                      left join rno_job job on job.job_id = sjob.job_id   " + where + ") t1   "
				+ "             where rownum <= " + (cnt + startIndex) + " ) t2   " + "      where t2.rn > "
				+ startIndex;

		log.debug("分页获取pci自动规划任务信息queryPciPlanTaskByPage : sql="
				+ RnoHelper.replaceTabsBlanksBreaksReturnsToOneBlank(sql));
		List<Map<String, Object>> res = hibernateTemplate.execute(new HibernateCallback<List<Map<String, Object>>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}
		});
		if (res != null && res.size() > 0) {
			String cityName = "";
			for (String k : cond.keySet()) {
				if ("cityId".equalsIgnoreCase(k)) {
					Map<String, Object> area = AuthDsDataDaoImpl.getAreaData(Long.parseLong(cond.get(k).toString()));
					if (area != null) {
						cityName = (String) area.get("NAME");
					}
				}
			}
			for (Map<String, Object> one : res) {
				one.put("CITY_NAME", cityName);
			}
		}
		log.debug("退出方法：queryPciPlanTaskByPage。返回：" + res.size() + "个记录");
		return res;
	}

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
	public boolean updatePciPlanWorkStatusByStmt(Statement stmt, long jobId, String workStatus) {
		final String sql = "update rno_lte_pci_job t set t.finish_state='" + workStatus + "' " + " where t.job_id="
				+ jobId;
		log.debug("updatePciPlanWorkStatusByStmt的sql:" + sql);
		boolean result = false;
		try {
			int rows = stmt.executeUpdate(sql);
			if (rows > 0) {
				result = true;
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return result;
	}

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
	public Map<String, List<String>> getEnodebIdForCellsMap(Statement stmt, long cityId) {
		Map<String, List<String>> enodebToCells = new HashMap<String, List<String>>();
		String sql = "select t1.business_cell_id cell," + "t2.business_enodeb_id ENODEB" + " from rno_lte_cell t1"
				+ " inner join rno_lte_enodeb t2" + " on t1.enodeb_id = t2.enodeb_id" + " where t1.area_id = " + cityId;
		List<Map<String, Object>> lteCells = RnoHelper.commonQuery(stmt, sql);
		if (lteCells == null || lteCells.size() == 0) {
			return null;
		}
		String enodebId = "", cellId = "";
		for (Map<String, Object> map : lteCells) {
			enodebId = map.get("ENODEB").toString().intern();
			cellId = map.get("CELL").toString().intern();

			if (!enodebToCells.containsKey(enodebId)) {
				enodebToCells.put(enodebId.intern(), new ArrayList<String>());
			}
			if (!enodebToCells.get(enodebId).contains(cellId)) {
				enodebToCells.get(enodebId).add(cellId.intern());
			}
		}
		return enodebToCells;
	}

	/**
	 * 
	 * @param stmt
	 * @param cityId
	 * @return
	 * @author chen.c10
	 * @date 2016年3月29日
	 * @version RNO 3.0.1
	 */
	public Map<String, String> getCell2EnodebIdMap(Statement stmt, long cityId) {

		Map<String, String> cell2Enodeb = new HashMap<String, String>();
		String sql = "select t1.Business_Cell_Id CELL," + " t2.Business_Enodeb_Id ENODEB" + " from Rno_Lte_Cell t1"
				+ " inner join Rno_Lte_Enodeb t2" + " on t1.Enodeb_Id = t2.Enodeb_Id" + " where t1.Area_Id = " + cityId;
		List<Map<String, Object>> lteCells = RnoHelper.commonQuery(stmt, sql);
		if (lteCells == null || lteCells.size() == 0) {
			return null;
		}
		for (Map<String, Object> map : lteCells) {
			cell2Enodeb.put(map.get("CELL").toString().intern(), map.get("ENODEB").toString().intern());
		}
		return cell2Enodeb;
	}

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
	public Map<String, Object> getParameterForCellsMap(Statement stmt, long cityId) {
		Map<String, Object> cellToParameter = new HashMap<String, Object>();
		List<String> cellList = new ArrayList<String>();
		Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();
		Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();
		Map<String, Integer> cellToEarfcn = new HashMap<String, Integer>();
		String sql = "select t.Business_Cell_Id cid,t.Longitude lng,t.Latitude lat,t.Pci,t.Earfcn from RNO_LTE_CELL t where t.Latitude is not null and t.Longitude is not null and t.Pci is not null and t.Earfcn is not null and t.Area_Id="
				+ cityId;
		List<Map<String, Object>> lteCells = RnoHelper.commonQuery(stmt, sql);
		if (lteCells == null || lteCells.isEmpty()) {
			return null;
		}
		String cid = "";
		for (Map<String, Object> map : lteCells) {
			try {
				cid = map.get("CID").toString().intern();
				if (!cellList.contains(cid)) {
					cellList.add(cid.intern());
					cellToOriPci.put(cid.intern(), Integer.parseInt(map.get("PCI").toString()));
					cellToLonLat.put(
							cid.intern(),
							new double[] { Double.parseDouble(map.get("LNG").toString()),
									Double.parseDouble(map.get("LAT").toString()) });
					cellToEarfcn.put(cid.intern(), Integer.parseInt(map.get("EARFCN").toString()));
				}
			} catch (Exception e) {
				// e.printStackTrace();
			}
		}
		cellToParameter.put("cellList", cellList);
		cellToParameter.put("cellToOriPci", cellToOriPci);
		cellToParameter.put("cellToLonLat", cellToLonLat);
		cellToParameter.put("cellToEarfcn", cellToEarfcn);
		return cellToParameter;
	}

	public void addMapReduceJobId(Statement stmt, long jobId, String mrJobId, String type) {
		String table = "rno_lte_pci_job";
		if ("MARTIX".equals(type.toUpperCase())) {
			table = "Rno_4g_Inter_Martix_Rec";
		}
		String sql = "update " + table + " set MR_JOB_ID='" + mrJobId + "' WHERE JOB_ID=" + jobId;
		try {
			stmt.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		log.debug("进入方法体getLteCellInfoByCellId。cityId=" + cityId);
		Map<String, List<String>> cellToCells = new HashMap<String, List<String>>();
		String sql = "select t1.business_cell_id cell,			" + "      t1.cell_name name,                 "
				+ "      t1.pci,                            " + "      t1.earfcn,                         "
				+ "      t2.business_enodeb_id ENODEB_ID    " + " from rno_lte_cell t1                    "
				+ " left join rno_lte_enodeb t2             " + "   on t1.enodeb_id = t2.enodeb_id        "
				+ "where t1.area_id =                     " + cityId;
		List<Map<String, Object>> lteCells = RnoHelper.commonQuery(stmt, sql);
		if (lteCells == null || lteCells.size() == 0) {
			return null;
		}
		String cellId = "";
		String pci = "";
		String cellName = "";
		String earfcn = "";
		for (Map<String, Object> map : lteCells) {
			// enodebId=Integer.parseInt(map.get("ENODEB_ID").toString());
			cellId = map.get("CELL").toString();
			pci = map.get("PCI").toString();
			cellName = map.get("NAME").toString();
			earfcn = map.get("EARFCN").toString();
			cellToCells.put(cellId, Arrays.asList(cellName, pci, earfcn));
		}
		log.debug("退出方法getLteCellInfoByCellId。cellToCells.size=" + cellToCells.size());
		return cellToCells;
	}

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
	public long getLteInterferMartixCount(Map<String, String> condition) {
		String where = buildLteInterferMartixWhere(condition);
		final String sql = "select count(MARTIX_DESC_ID) from RNO_4G_INTER_MARTIX_REC sjob" + where;
		log.debug("get4GInterferMartixCount : sql=" + sql);
		// System.out.println("getNcsDescriptorCount : sql="+sql);
		return hibernateTemplate.execute(new HibernateCallback<Long>() {

			@Override
			public Long doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				BigDecimal res = (BigDecimal) query.uniqueResult();
				long cnt = res.longValue();
				log.debug("get4GInterferMartixCount sql=" + sql + ",返回结果：" + cnt);
				return cnt;
			}
		});
	}

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
	public List<Map<String, Object>> queryLteInterferMartixByPage(Map<String, String> condition, int startIndex, int cnt) {
		log.debug("进入方法：queryLteInterferMartixByPage。condition=" + condition + ",startIndex=" + startIndex + ",cnt="
				+ cnt);
		if (condition == null || condition.isEmpty()) {
			log.debug("未传入查询条件");
		}
		if (startIndex < 0 || cnt < 0) {
			return Collections.emptyList();
		}

		String where = buildLteInterferMartixWhere(condition);

		final String sql = "select t2.city_name," + "t2.job_id," + "t2.mr_job_id," + "t2.task_name," + "t2.type,"
				+ "t2.Record_Num," + "t2.data_description," + "t2.SAMEFREQCELLCOEFWEIGHT," + "t2.SWITCHRATIOWEIGHT,"
				+ "t2.RELA_NUM_TYPE," + "t2.job_running_status," + "t2.job_type," + "t2.start_mea_date,"
				+ "t2.end_mea_date," + "t2.create_date," + "t2.launch_time," + "t2.complete_time"
				+ " from (select t1.*, rownum as rn" + " from (select sjob.job_id as job_id,"
				+ "sjob.mr_job_id as mr_job_id," + "sjob.Record_Num as RECORD_NUM," + "sjob.TYPE as TYPE,"
				+ "sjob.TASK_NAME as TASK_NAME," + "sjob.Data_Description as DATA_DESCRIPTION,"
				+ "sjob.SAMEFREQCELLCOEFWEIGHT as SAMEFREQCELLCOEFWEIGHT,"
				+ "sjob.Switchratioweight as SWITCHRATIOWEIGHT," + "sjob.RELA_NUM_TYPE as RELA_NUM_TYPE,"
				+ "job.job_running_status as job_running_status," + "job.Job_Type as Job_Type,"
				+ "sa.Name as city_name," + "to_char(sjob.start_mea_date, 'yyyy-MM-dd HH24:mi:ss') as start_mea_date,"
				+ "to_char(sjob.end_mea_date, 'yyyy-MM-dd HH24:mi:ss') as end_mea_date,"
				+ "to_char(sjob.create_date, 'yyyy-MM-dd HH24:mi:ss') as create_date,"
				+ "to_char(job.launch_time, 'yyyy-MM-dd HH24:mi:ss') as launch_time,"
				+ "to_char(job.complete_time, 'yyyy-MM-dd HH24:mi:ss') as complete_time"
				+ " from RNO_4G_INTER_MARTIX_REC sjob" + " left join rno_job job" + " on job.job_id = sjob.job_id"
				+ " left join Sys_Area sa" + " on sa.area_id = sjob.city_id " + where
				+ " order by sjob.create_date desc) t1) t2" + " where t2.rn <= " + (cnt + startIndex) + " and t2.rn > "
				+ startIndex;
		log.debug("分页获取干扰矩阵的sql：" + sql);
		List<Map<String, Object>> res = hibernateTemplate.execute(new HibernateCallback<List<Map<String, Object>>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				// query.addEntity(RnoNcsDescriptor.class);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}

		});

		log.debug("退出方法：query4GInterferMartixByPage。返回：" + res.size() + "个记录");
		return res;
	}

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
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getTaksNameListByCityId(final long cityId) {
		log.debug("进入方法getTaksNameListByCityId。cityId = " + cityId);
		return hibernateTemplate.executeFind(new HibernateCallback<List<Map<String, String>>>() {
			public List<Map<String, String>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				List<Map<String, String>> result = new ArrayList<Map<String, String>>();
				String sql = "select Task_Name from RNO_4G_INTER_MARTIX_REC where city_Id = " + cityId;
				log.debug("getTaksNameListByCityId.sql=" + sql);
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				result = (List<Map<String, String>>) query.list();
				log.debug("退出方法getTaksNameListByCityId。result = " + result);
				return result;
			}
		});
	}

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
	public List<Map<String, Object>> checkLteInterMartixThisWeek(long cityId, String thisMonday, String today) {
		log.debug("进入方法check4GInterMartixThisWeek。areaId=" + cityId + ",thisMonday=" + thisMonday + ",today=" + today);
		final String sql = " select  to_char(t.create_date, 'yyyy-MM-dd HH24:mi:ss') as CREATE_DATE "
				+ " from RNO_4G_INTER_MARTIX_REC  t " + " where t.CITY_ID = " + cityId
				+ " and t.CREATE_DATE <= to_date('" + today + "','yyyy-MM-dd HH24:mi:ss') "
				+ " and t.CREATE_DATE >= to_date('" + thisMonday + "','yyyy-MM-dd HH24:mi:ss') "
				+ " and t.WORK_STATUS = '计算完成' and t.STATUS = 'Y' " + " order by t.create_date desc";

		log.debug("检查这周是否计算过 4G MR干扰矩阵: sql=" + sql);
		return hibernateTemplate.execute(new HibernateCallback<List<Map<String, Object>>>() {
			@SuppressWarnings("unchecked")
			@Override
			public List<Map<String, Object>> doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				return query.list();
			}
		});
	}

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
	public boolean createLteInterMartixRec(long jobId, RnoLteInterMatrixTaskInfo taskInfo) {
		log.debug("进入方法createMr4GInterMartixRec。taskInfo=" + taskInfo);
		// 创建日期
		Calendar cal = Calendar.getInstance();
		String createDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
		// 结果文件路径
		String filePath = "hdfs:///" + HadoopUser.homeOfUser() + "rno_data/4gmatrix/" + cal.get(Calendar.YEAR) + "/"
				+ (cal.get(Calendar.MONTH) + 1) + "/" + jobId;
		String fileName = UUID.randomUUID().toString().replaceAll("-", "");

		final String sql = "insert into RNO_4G_INTER_MARTIX_REC (" + "MARTIX_DESC_ID," + "CITY_ID," + "CREATE_DATE,"
				+ "START_MEA_DATE," + "END_MEA_DATE," + "RECORD_NUM," + "TYPE," + "WORK_STATUS," + "FILE_PATH,"
				+ "JOB_ID," + "STATUS," + "FILE_NAME," + "TASK_NAME," + "DATA_DESCRIPTION, "
				+ "SAMEFREQCELLCOEFWEIGHT, " + "SWITCHRATIOWEIGHT," + "SF_FILES) " + " values("
				+ getNextSeqValue("SEQ_4G_INTER_MARTIX_REC")
				+ ","
				+ taskInfo.getCityId()
				+ ", to_date('"
				+ createDate
				+ "', 'yyyy-MM-dd HH24:mi:ss')"
				+ ", to_date('"
				+ taskInfo.getBegTime()
				+ "', 'yyyy-MM-dd HH24:mi:ss')"
				+ ", to_date('"
				+ taskInfo.getEndTime()
				+ "', 'yyyy-MM-dd HH24:mi:ss')"
				+ ", "
				+ taskInfo.getRecordNum()
				+ ",'"
				+ taskInfo.getDataType()
				+ "','"
				+ "排队中"
				+ "','"
				+ filePath
				+ "',"
				+ jobId
				+ ",'"
				+ "Y"
				+ "','"
				+ fileName
				+ "','"
				+ taskInfo.getTaskName()
				+ "','"
				+ taskInfo.getDataDescription()
				+ "',"
				+ taskInfo.getSameFreqCellCoefWeight()
				+ ","
				+ taskInfo.getSwitchRatioWeight() + ",'" + taskInfo.getSfFiles() + "')";

		log.debug("createMr4GInterMartixRec的sql:" + sql);
		return hibernateTemplate.execute(new HibernateCallback<Boolean>() {
			public Boolean doInHibernate(Session arg0) throws HibernateException, SQLException {
				SQLQuery query = arg0.createSQLQuery(sql);
				int res = query.executeUpdate();
				log.debug("退出createMr4GInterMartixRec,受影响行数=" + res);
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
	 * 分页查询扫频数据
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond) {
		log.debug("进入方法：querySfDataFromHbaseByPage。cond=" + cond);
		String tableName, startRow, stopRow;

		tableName = cond.buildTable();
		startRow = cond.buildStartRow();
		stopRow = cond.buildStopRow();

		org.apache.hadoop.hbase.client.Connection conn = null;
		Table table = null;
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
		ResultScanner scanner = null;
		try {
			conn = ConnectionFactory.createConnection(HBaseConfiguration.create(new Configuration()));
			table = conn.getTable(TableName.valueOf(HBTable.valueOf(tableName)));
			if (table == null) {
				return Collections.emptyList();
			}
			// 获取筛选对象
			Scan scan = new Scan();
			// 设置row范围
			scan.setStartRow(Bytes.toBytes(startRow));
			scan.setStopRow(Bytes.toBytes(stopRow));

			// 设置需要的column
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CITY_ID"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FILE_NAME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MEA_TIME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("RECORD_COUNT"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FACTORY"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CREATE_TIME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MOD_TIME"));

			// 给筛选对象放入过滤器(true标识分页,具体方法在下面)

			// 缓存1000条数据
			scan.setCaching(1000);
			scan.setCacheBlocks(false);

			// 获取结果集
			scanner = table.getScanner(scan);
			// 遍历扫描器对象， 并将需要查询出来的数据row key取出
			for (Result r : scanner) {
				Map<byte[], byte[]> fmap = new LinkedHashMap<byte[], byte[]>();
				fmap.putAll(r.getFamilyMap(Bytes.toBytes("DESCINFO")));

				Map<String, String> rmap = new LinkedHashMap<String, String>();
				for (byte[] key : fmap.keySet()) {
					byte[] value = fmap.get(key);
					rmap.put(Bytes.toString(key), Bytes.toString(value));
				}
				mapList.add(rmap);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			if (table != null) {
				try {
					table.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		log.debug("退出方法：querySfDataFromHbaseByPage。mapList.size=" + mapList.size());
		return mapList;
	}

	/**
	 * 分页查询扫频数据
	 */
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond, Page page) {
		log.debug("进入方法：querySfDataFromHbaseByPage。cond=" + cond + ",page=" + page);
		String tableName, startRow, stopRow;

		tableName = cond.buildTable();
		startRow = cond.buildStartRow();
		stopRow = cond.buildStopRow();

		org.apache.hadoop.hbase.client.Connection conn = null;
		Table table = null;
		List<Map<String, String>> mapList = new LinkedList<Map<String, String>>();
		ResultScanner scanner = null;
		try {
			// 获取最大返回结果数量
			if (page.getPageSize() == 0)
				page.setPageSize(25);
			if (page.getCurrentPage() == 0)
				page.setCurrentPage(1);
			// 计算起始页和结束页
			Integer firstPage = (page.getCurrentPage() - 1) * page.getPageSize();
			Integer endPage = firstPage + page.getPageSize();

			conn = ConnectionFactory.createConnection(HBaseConfiguration.create(new Configuration()));
			table = conn.getTable(TableName.valueOf(HBTable.valueOf(tableName)));
			if (table == null) {
				return Collections.emptyList();
			}
			// 获取筛选对象
			Scan scan = new Scan();

			// 设置row范围
			scan.setStartRow(Bytes.toBytes(startRow));
			scan.setStopRow(Bytes.toBytes(stopRow));

			// 设置需要的column
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CITY_ID"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FILE_NAME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MEA_TIME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("RECORD_COUNT"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FACTORY"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CREATE_TIME"));
			scan.addColumn(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MOD_TIME"));

			// 给筛选对象放入过滤器(true标识分页,具体方法在下面)

			// 缓存1000条数据
			scan.setCaching(1000);
			scan.setCacheBlocks(false);
			scanner = table.getScanner(scan);
			int i = 0;
			int totCnt = page.getTotalCnt();
			// 遍历扫描器对象， 并将需要查询出来的数据row key取出
			for (Result r : scanner) {
				if (i >= firstPage && i < endPage) {
					Map<byte[], byte[]> fmap = new LinkedHashMap<byte[], byte[]>();
					fmap.putAll(r.getFamilyMap(Bytes.toBytes("DESCINFO")));

					Map<String, String> rmap = new LinkedHashMap<String, String>();
					for (byte[] key : fmap.keySet()) {
						byte[] value = fmap.get(key);
						rmap.put(Bytes.toString(key), Bytes.toString(value));
					}
					mapList.add(rmap);
				}
				i++;
				if (totCnt > 0 && i >= endPage) {
					break;
				}
			}
			if (totCnt < 0) {
				page.setTotalCnt(i);
			}
			if (scanner != null) {
				scanner.close();
			}
			if (table != null) {
				table.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
			if (table != null) {
				try {
					table.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		log.debug("退出方法：querySfDataFromHbaseByPage。mapList.size=" + mapList.size());
		return mapList;
	}

	public Map<String, Object> savePciPlanAnalysisTaskInfo(long tmpJobId, final List<RnoThreshold> rnoThresholds,
			final TaskInfo taskInfo) {
		log.debug("进入方法：savePciPlanAnalysisTaskInfo。tmpJobId=" + tmpJobId + ",rnoThresholds=" + rnoThresholds
				+ ",taskInfo=" + taskInfo);
		Map<String, Object> result = new HashMap<String, Object>();
		if (tmpJobId <= 0) {
			log.error("创建jobId失败！");
			result.put("flag", false);
			result.put("desc", "提交任务失败！");
			return result;
		}
		// 判断是不是干扰矩阵导入模式
		if (tmpJobId == taskInfo.getMatrixDataCollectId()) {
			tmpJobId = getJobIdFromDataCollectById(tmpJobId);
		}
		final long jobId = tmpJobId;
		result = hibernateTemplate.execute(new HibernateCallback<Map<String, Object>>() {
			public Map<String, Object> doInHibernate(Session session) throws HibernateException, SQLException {
				final Map<String, Object> result = new HashMap<String, Object>();
				Transaction ts = session.beginTransaction();
				session.doWork(new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						PreparedStatement pstmt = null;
						// 下载文件名
						String dlFileName = "";
						if (!(taskInfo.getIsExportAssoTable().equals("NO")
								&& taskInfo.getIsExportMidPlan().equals("NO") && taskInfo.getIsExportNcCheckPlan()
								.equals("NO"))) {
							dlFileName = jobId + "_PCI优化.zip";
						} else {
							dlFileName = jobId + "_PCI优化方案.xlsx";
						}
						// 读取文件名
						String rdFileName = jobId + "_pci_data";
						// 创建日期
						Calendar cal = Calendar.getInstance();
						cal = Calendar.getInstance();
						String createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
						// 文件保存路径
						String resultDir = "hdfs:///" + HadoopUser.homeOfUser() + "rno_data/pci/"
								+ cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1) + "/" + jobId;
						String dataFilePath = resultDir + "/" + jobId + "_pci_cal_data";
						String finishState = "排队中";
						// 更新日期
						String modTime = createTime;

						if (taskInfo.isUseFlow()) {
							String sql = "UPDATE RNO_DATA_COLLECT_REC SET JOB_ID = ? WHERE DATA_COLLECT_ID = ?";
							try {
								pstmt = connection.prepareStatement(sql);
								pstmt.setString(1, String.valueOf(jobId));
								pstmt.setLong(2, taskInfo.getFlowDataCollectId());
								pstmt.addBatch();
								pstmt.executeBatch();
							} catch (SQLException e) {
								e.printStackTrace();
								log.error("jobId=" + jobId + "，保存PCI规划流量失败！");
								result.put("flag", false);
								result.put("desc", "提交任务失败！");
								return;
							}
						}

						String thresholdSql = "insert into rno_lte_pci_job_param (JOB_ID," + "PARAM_TYPE,"
								+ "PARAM_CODE," + "PARAM_VAL) values(" + jobId + ",?," + "?," + "?)";
						try {
							pstmt = connection.prepareStatement(thresholdSql);
							// 保存对应的门限值
							String paramVal;
							String paramCode;
							for (RnoThreshold rnoThreshold : rnoThresholds) {
								paramCode = rnoThreshold.getCode();
								paramVal = rnoThreshold.getDefaultVal();
								if (!StringUtils.isBlank(paramCode) && !StringUtils.isBlank(paramVal)) {
									pstmt.setString(1, "PCI_THRESHOLD");
									pstmt.setString(2, paramCode);
									pstmt.setString(3, paramVal);
									pstmt.addBatch();
								}
							}
							// 执行
							pstmt.executeBatch();
						} catch (SQLException e) {
							e.printStackTrace();
							log.error("jobId=" + jobId + "，保存PCI规划参数失败！");
							result.put("flag", false);
							result.put("desc", "提交任务失败！");
							return;
						}
						// 保存PCI任务
						String begMeaTime = taskInfo.getStartTime();
						String endMeaTime = taskInfo.getEndTime();
						long cityId = taskInfo.getCityId();
						String optimizeCells = taskInfo.getLteCells();
						String planType = taskInfo.getPlanType();
						String converType = taskInfo.getConverType();
						String relaNumType = taskInfo.getRelaNumerType();
						String isCheckNCell = taskInfo.getIsCheckNCell();
						String isExportAssoTable = taskInfo.getIsExportAssoTable();
						String isExportMidPlan = taskInfo.getIsExportMidPlan();
						String isExportNcCheckPlan = taskInfo.getIsExportNcCheckPlan();
						String sfFiles = taskInfo.getSfFiles();
						String freqAdjType = taskInfo.getFreqAdjType();
						String d1Freq = taskInfo.getD1Freq();
						String d2Freq = taskInfo.getD2Freq();
						long matrixDcId = taskInfo.getMatrixDataCollectId();
						long flowDcId = taskInfo.getFlowDataCollectId();
						double ks = taskInfo.getKs();

						String insertSql = "insert into rno_lte_pci_job	("
								+ "JOB_ID,BEG_MEA_TIME,END_MEA_TIME,CITY_ID,"
								+ "DL_FILE_NAME,RD_FILE_NAME,RESULT_DIR,FINISH_STATE,STATUS,CREATE_TIME,"
								+ "MOD_TIME,OPTIMIZE_CELLS,PLAN_TYPE,CONVER_TYPE,RELA_NUM_TYPE,"
								+ "IS_CHECK_NCELL,DATA_FILE_PATH,IS_EXPORT_ASSOTABLE,IS_EXPORT_MIDPLAN,"
								+ "IS_EXPORT_NCCHECKPLAN,SF_FILE_NAMES,FREQ_ADJ_TYPE,D1FREQ,D2FREQ,"
								+ "Matrix_Data_Collect_Id,Flow_Data_Collect_Id,KS_CORR_VAL) " + "values ("
								+ jobId
								+ ",to_date('"
								+ begMeaTime
								+ "', 'yyyy-MM-dd HH24:mi:ss'),"
								+ "to_date('"
								+ endMeaTime
								+ "', 'yyyy-MM-dd HH24:mi:ss'),"
								+ cityId
								+ ",'"
								+ dlFileName
								+ "','"
								+ rdFileName
								+ "','"
								+ resultDir
								+ "',"
								+ "'"
								+ finishState
								+ "','N',"
								+ "to_date('"
								+ createTime
								+ "', 'yyyy-MM-dd HH24:mi:ss'),"
								+ "to_date('"
								+ modTime
								+ "', 'yyyy-MM-dd HH24:mi:ss'),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
						try {
							pstmt = connection.prepareStatement(insertSql);
							// 保存参数
							// BufferedReader reader = new BufferedReader(new StringReader(optimizeCells));

							// pstmt.setClob(1, clob);
							pstmt.setString(1, optimizeCells);
							pstmt.setString(2, planType);
							pstmt.setString(3, converType);
							pstmt.setString(4, relaNumType);
							pstmt.setString(5, isCheckNCell);
							pstmt.setString(6, dataFilePath);
							pstmt.setString(7, isExportAssoTable);
							pstmt.setString(8, isExportMidPlan);
							pstmt.setString(9, isExportNcCheckPlan);
							pstmt.setString(10, sfFiles);
							pstmt.setString(11, freqAdjType);
							pstmt.setString(12, d1Freq);
							pstmt.setString(13, d2Freq);
							pstmt.setLong(14, matrixDcId);
							pstmt.setLong(15, flowDcId);
							pstmt.setDouble(16, ks);
							pstmt.addBatch();
							// 执行
							pstmt.executeBatch();
						} catch (SQLException e) {
							e.printStackTrace();
							log.error("jobId=" + jobId + "，保存PCI规划任务失败！");
							result.put("flag", false);
							result.put("desc", "提交任务失败！");
							return;
						}
						log.debug("jobId=" + jobId + "，保存PCI规划任务成功！");
						result.put("flag", true);
						result.put("desc", "提交任务成功！");
					}
				});
				if ((Boolean) result.get("flag")) {
					ts.commit();
				} else {
					ts.rollback();
				}
				session.flush();
				return result;
			}
		});
		log.debug("退出方法：savePciPlanAnalysisTaskInfo。result=" + result);
		return result;
	}

	public long getJobIdFromDataCollectById(long dcId) {
		final String sql = "select t.Job_Id from Rno_Data_Collect_Rec t where t.Data_Collect_Id = " + dcId;
		return hibernateTemplate.execute(new HibernateCallback<Long>() {
			public Long doInHibernate(Session session) throws HibernateException, SQLException {
				SQLQuery query = session.createSQLQuery(sql);
				return ((BigDecimal) query.uniqueResult()).longValue();
			}
		});
	}
}