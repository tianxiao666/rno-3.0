package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.iscreate.op.dao.system.SysAreaDao;
import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.DataParseProgress;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;

public class RnoLteCellParser extends DbParserBaseJobRunnable {

	private static Log log = LogFactory.getLog(RnoLteCellParser.class);

	public RnoLteCellParser() {
		super();
		super.setJobType("LTECELLFILE");
	}

	// ---spring 注入----//
	// private SysAreaDao sysAreaDao;
	// private AreaLockManager areaLockManager;
	// private static Gson gson = new GsonBuilder().create();// 线程安全

	// 导入的3中模式
	// private static String OVERWRITE_MODE = "overwrite";
	// private static String APPEND_MODE = "append";
	// private static String DELETE_MODE = "delete";
	private File file = null;
	private String filePath = null;
	private boolean isLocalCellId;
	/*
	 * public void setSysAreaDao(SysAreaDao sysAreaDao) { this.sysAreaDao = sysAreaDao; }
	 */

	/*
	 * public void setAreaLockManager(AreaLockManager areaLockManager) { this.areaLockManager = areaLockManager; }
	 */

	// // 只存在于中间表，不存在目标表的字段
	// private static List<String> fieldsNotExistInTargetTab = Arrays.asList(
	// "CELL_ID", "AREA_NAME");

	// 构建insert语句
	private static String midTable = "RNO_LTE_IMPORT_MID";
	private static String insertInoMidTableSql = "";
	// areaId在sql序列中的位置记录
	Map<String, Integer> areaSqlPosition = new HashMap<String, Integer>();
	String excelAreaIds = "";// excel出现的区域对应的id
	Set<Long> excelAreaIdSet = new HashSet<Long>();
	// 区域名称到id的映射
	Map<String, Long> areaNameToIds = new HashMap<String, Long>();
	// BUSINESS_CELL_ID=BUSINESS_ENODEB_ID||LOCAL_CELLID
	Map<String, String> businessCellIds = new HashMap<String, String>();
	// 成功与失败的数量
	int failCnt = 0, sucCnt = 0, fc = 0, sc = 0, commonIdCount = 0;
	long cityId = 0, ai = 0;
	String cityName = "";
	StringBuilder bufMsg = new StringBuilder();
	String msgStr = "";
	boolean isSelectedCity = true;
	List<String> bci = new ArrayList<String>();

	/**
	 * 
	 * @param token
	 * @param file
	 * @param needPersist
	 * @param update
	 * @param oldConfigId
	 * @param areaId
	 * @param autoload
	 * @param attachParams
	 * @return
	 * @author brightming 2014-5-16 上午10:06:26
	 */
	protected boolean parseDataInternal(JobReport report,
			RnoDataCollectRec dataRec, String tmpFileName, File f,
			Connection connection, long cityId, Statement stmt,
			String baseDateStr, Map<String, DBFieldToTitle> dbFieldsToTitles) {
		// 导入模式
		// String importMode = OVERWRITE_MODE;

		// super.setCachedInfo(token, "正在分析格式有效性...");
		Date date = null;
		long begTime = System.currentTimeMillis();
		long t1, t2;
		// 当前的areaid的含义是省 的id
		long provinceId = ai;
		long area_ids[] = new long[] { provinceId };
		// List<Map<String, Object>> allAreas = sysAreaDao.getSubAreasInSpecAreaLevel(new long[] { provinceId }, "市");
		StringBuilder buf = new StringBuilder();
		buf.append("(");
		for (int i = 0; i < area_ids.length; i++) {
			buf.append(area_ids[i] + ",");
		}
		buf.deleteCharAt(buf.length() - 1);
		buf.append(")");
		String sqlStr = "select a.* from sys_area a start with a.area_id in " + buf.toString()
				+ " connect by prior a.area_id=a.parent_id and area_level='市'";
		// System.out.println("sql:"+sql);
		List<Map<String, Object>> allAreas = RnoHelper.commonQuery(stmt, sqlStr);
		// List<Map<String, Object>> allAreas = new ArrayList<Map<String,Object>>();

		if (allAreas == null || allAreas.isEmpty()) {
			log.error("province=" + provinceId + " 下没有市设置！");
			// super.setCachedInfo(token, RnoConstant.TimeConstant.TokenTime,"指定的导入省不存在任何市区域，请联系管理员！");
			return false;
		}
		log.debug("allAreas=" + allAreas);
		// System.out.println("allAreas=" + allAreas);
		long oneid = -1;
		for (Map<String, Object> one : allAreas) {
			oneid = /* ((BigDecimal) one.get("AREA_ID")).longValue() */Long.parseLong(one.get("AREA_ID").toString());
			if (oneid == provinceId) {
				continue;
			}
			areaNameToIds.put(one.get("NAME").toString(), oneid);
		}
		log.debug("areaNameToIds=" + areaNameToIds);

		// 获取标题情况
		// StringBuilder checkMsg = new StringBuilder();

		// 准备数据库插入statement
		/*
		 * PreparedStatement PStatement = null; log.debug("插入中间表的sql语句为：" + insertInoMidTableSql); try { PStatement =
		 * connection.prepareStatement(insertInoMidTableSql); } catch (Exception e) {
		 * log.error("准备插入中间表的preparedStatement时出错！insertInoMidTableSql=" + insertInoMidTableSql); setCachedInfo(token,
		 * RnoConstant.TimeConstant.TokenTime, "系统出错!code=301"); return false; }
		 */

		// super.setCachedInfo(token, "正在逐行分析数据...");

		// --------逐行分析excel csv数据，转换为导入中间表的sql---//

		// int index;
		// boolean excelLineOk;
		String msg;
		// long oneAreaId;
		String excelAreaIds = "";// excel出现的区域对应的id
		boolean parseOk = false;
		// report.setBegTime(new Date());
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			msg = "准备处理数据时出错！code=303";
			log.error(msg);
			bufMsg.append("准备处理数据时出错 <br/>");
			// super.setCachedInfo(token, bufMsg.toString());
			// 报告
			/*
			 * report.setFinishState("失败"); report.setStage("准备处理数据时"); report.setEndTime(new Date());
			 * report.setReportType(1); report.setAttMsg(msg); addJobReport(report);
			 */
			return false;
		}
		date = new Date();
		// report.setBegTime(new Date());
		parseOk = parseLteCell(file, connection, stmt, dbFieldsToTitles);
		if (parseOk) {
			msg = "解析文件成功!";
			// bufMsg.append(msg + "<br/>");
			// setCachedInfo(token, bufMsg.toString());
		} else {
			msg = "解析文件出现错误!";
			// bufMsg.append(msg + "<br/>");
			if (!isSelectedCity) {
				msg = "选择的城市与文件所属城市不符！";
				bufMsg.append(msg + "<br/>");
			}
			return false;
			// setCachedInfo(token, bufMsg.toString());
		}
		if (!excelAreaIdSet.isEmpty()) {
			for (Long id : excelAreaIdSet) {
				excelAreaIds += id + ",";
			}
		}
		if (!"".equals(excelAreaIds)) {
			excelAreaIds = excelAreaIds.substring(0, excelAreaIds.length() - 1);
		}

		// super.setCachedInfo(token, "正在执行数据导入...");
		log.info("lte导入，准备阶段：sucCnt=" + sucCnt + ",failCnt=" + failCnt);
		System.out.println("lte导入，准备阶段：sucCnt=" + sucCnt + ",failCnt=" + failCnt);
		// --------处理中间表-----//
		if (sucCnt == 0) {
			log.warn("导入的excel表的数据统统无效!");
			// super.setCachedInfo(token, bufMsg.toString());
			bufMsg.append("导入的excel表的数据统统无效 <br/>");
			return false;
		}

		String sql = "";

		int affectCnt = 0;
		sql = "delete from RNO_LTE_CELL c where c.area_id =" + cityId;
		// System.out.println("update sql:" + sql);
		log.debug("准备删除工参表数据：sql=" + sql);
		t1 = System.currentTimeMillis();
		try {
			affectCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.info("准备删除工参表数据执行成功，影响：" + affectCnt + ",耗时：" + (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("准备删除工参表数据执行失败！sql=" + sql);
			msg = "准备删除工参表数据时出错！";
			log.error(msg);
			bufMsg.append("准备删除工参表数据时出错" + "<br/>");
			// super.setCachedInfo(token, bufMsg.toString());
			try {
				stmt.close();
			} catch (SQLException e1) {
			}
			return false;
		}

		sql = "delete from RNO_LTE_ENODEB  where area_id =" + cityId;
		// System.out.println("update sql:" + sql);
		log.debug("准备删除基站表数据：sql=" + sql);
		t1 = System.currentTimeMillis();
		try {
			affectCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.info("准备删除基站表数据执行成功，影响：" + affectCnt + ",耗时：" + (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("准备删除基站表数据执行失败！sql=" + sql);
			msg = "准备删除基站表数据时出错！";
			log.error(msg);
			bufMsg.append("准备准备删除基站表数据时出错" + "<br/>");
			// super.setCachedInfo(token, bufMsg.toString());
			try {
				stmt.close();
			} catch (SQLException e1) {
			}
			return false;
		}

		Map<String, String> tempMap = new HashMap<String, String>();
		sql = "select lte_cell_id from RNO_LTE_CELL where area_id=" + cityId;
		List<Map<String, Object>> bidList = RnoHelper.commonQuery(stmt, sql);
		for (int i = 0; i < bidList.size(); i++) {
			tempMap.put(bidList.get(i).get("lte_cell_id").toString(), bidList.get(i).get("lte_cell_id").toString());
		}
		// System.out.println("tempMap:"+tempMap);
		if (tempMap != null && !tempMap.isEmpty()) {
			StringBuffer busid = new StringBuffer();
			int idcount = 0;
			for (String id : tempMap.values()) {
				idcount++;
				if (idcount < 1000) {
					busid.append("'" + id + "',");
				} else {
					busid.delete(busid.toString().length() - 1, busid.toString().length());
					busid.append(")or c.lte_cell_id in('" + id + "',");
					idcount = 0;
				}
			}

			sql = "delete from RNO_LTE_CELL_MAP_SHAPE  where lte_cell_id in ("
					+ busid.toString().substring(0, busid.toString().length() - 1) + ")";
			// System.out.println("update sql:" + sql);
			log.debug("准备删除形状表数据：sql=" + sql);
			t1 = System.currentTimeMillis();
			try {
				affectCnt = stmt.executeUpdate(sql);
				t2 = System.currentTimeMillis();
				log.info("准备删除形状表数据执行成功，影响：" + affectCnt + ",耗时：" + (t2 - t1) + "ms");
			} catch (SQLException e) {
				e.printStackTrace();
				log.error("准备删除形状表数据执行失败！sql=" + sql);
				msg = "准备删除形状表数据时出错！";
				log.error(msg);
				bufMsg.append("准备准备删除形状表数据时出错" + "<br/>");
				// super.setCachedInfo(token, bufMsg.toString());
				try {
					stmt.close();
				} catch (SQLException e1) {
				}
				return false;
			}
		}
		// ---打印
		// sql="select BUSINESS_CELL_ID,BUSINESS_ENODEB_ID,LOCAL_CELLID,CELL_ID,ENODEB_STATUS,CELL_STATUS from "+midTable;
		// List<Map<String,Object>> rest=RnoHelper.commonQuery(stmt, sql);
		// log.debug("临时表的BUSINESS_CELL_ID情况：----");
		// int i=0;
		// for(Map<String,Object> one:rest){
		// msg="";
		// for(String k:one.keySet()){
		// msg+=k+"="+one.get(k)+",";
		// }
		// log.debug((i++)+"---:"+msg);
		// }
		// log.debug("临时表的BUSINESS_CELL_ID情况输出结束。");
		//

		// --------操作系统表-----//
		// report.setBegTime(new Date());
		boolean ok = importOverwrite(stmt);
		sc = sucCnt;
		fc = failCnt;
		log.debug("导入数据操作的执行结果为：" + ok);
		long endTime = System.currentTimeMillis();
		t2 = endTime - begTime;// 耗时
		String res = "";
		if (ok) {
			res = "导入数据成功！耗时：" + (t2 / 1000) + "s。成功处理：" + sucCnt + "条记录，异常数据：" + failCnt + "条。<br/>";

			System.out.println(res);
			if (failCnt > 0) {
				res += "详情如下：" + bufMsg.toString();
			}
			bufMsg = new StringBuilder();
			bufMsg.append(res + msgStr);
		} else {
			res = "导入操作在数据处理时失败！耗时：" + (t2 / 1000) + "s。有效数据数量：" + sucCnt + "条，无效数据数量：" + failCnt + "。<br/>";
			System.out.println(res);
			if (failCnt > 0) {
				res += "无效数据详情如下：" + bufMsg.toString();
			}
			bufMsg = new StringBuilder();
			bufMsg.append(res + msgStr);
		}
		sucCnt = 0;
		failCnt = 0;
		businessCellIds.clear();
		// super.setCachedInfo(token, res);
		log.debug("导入的最终结果：" + res);
		/*
		 * try { PStatement.clearBatch(); } catch (SQLException e) { } try { PStatement.close(); } catch (SQLException
		 * e) { }
		 */
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return true;
	}

	/**
	 * 新增模式导入
	 * 
	 * @param stmt
	 * @return
	 * @author brightming 2014-5-16 下午4:04:41
	 */
	private boolean importAppend(Statement stmt) {
		log.debug("进入方法：--------importAppend");
		long begTime = System.currentTimeMillis();
		long t1 = System.currentTimeMillis();
		// 将enodeb_status=2的enodeb信息插入到enodeb表
		String enodebFields = "BUSINESS_ENODEB_ID,SUBNET_ID,MANAGENET_ID,ENODEB_NAME,AREA_ID,SITE_STYLE,LONGITUDE,LATITUDE,BUILD_TYPE,STATION_CFG,FRAME_CFG,SPECIAL_FRAME_CFG,MME,MCC,MNC,COVER_AREA,BUILD_PHASE,TDS_SITE_NAME,G2_SITE_NAME,SHARE_TYPE,STATE,OPERATION_TIME,CLUSTER_NUM,BELONGING_OMC,IS_HIGH_SPEED,IS_VIP,COVER_AREA_TYPE,ADDRESS,REMARK";
		String sql = "insert into RNO_LTE_ENODEB (ENODEB_ID," + enodebFields
				+ ",STATUS) select SEQ_LTE_ENODEB_ID.NEXTVAL," + enodebFields
				+ ",'N' from ( SELECT " + enodebFields
				+ ",ROW_NUMBER() OVER(partition by business_enodeb_id order by business_enodeb_id desc) RN FROM "
				+ midTable + " where enodeb_status=2) WHERE RN=1";
		log.debug("开始执行将临时表enodeb_status=2的enodeb信息插入到enodeb表，sql=" + sql);
		long t2;
		int affCnt = 0;
		try {
			affCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.debug("将临时表enodeb_status=2的enodeb信息插入到enodeb表执行完成，影响：" + affCnt + ",耗时："
					+ (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("将临时表enodeb_status=2的enodeb信息插入到enodeb表出错！sql=" + sql);
			return false;
		}

		// 更新enodeb_id
		sql = "merge into "
				+ midTable
				+ " tar using (select enodeb_id,business_enodeb_id from RNO_LTE_ENODEB WHERE AREA_ID IN (SELECT DISTINCT(AREA_ID) FROM "
				+ midTable
				+ ")) src on (tar.business_enodeb_id=src.business_enodeb_id) when matched then update set tar.enodeb_id=src.enodeb_id";
		log.debug("更新临时表" + midTable + " 的enodeb 的数据库id，sql=" + sql);
		t1 = System.currentTimeMillis();
		try {
			affCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.debug("更新临时表" + midTable + " 的enodeb 的数据库id执行完成，影响：" + affCnt + ",耗时："
					+ (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("更新临时表" + midTable + " 的enodeb 的数据库id执行出错！sql=" + sql);
			return false;
		}

		if (isLocalCellId) {
			sql = "update " + midTable
					+ " set BUSINESS_CELL_ID=BUSINESS_ENODEB_ID||LOCAL_CELLID";
		} else {
			sql = "update " + midTable
					+ " set BUSINESS_CELL_ID=BUSINESS_ENODEB_ID||CELL_ID";
		}
		log.debug("准备更新lte小区导入临时表的business_cell_id,sql=" + sql);
		try {
			t1 = System.currentTimeMillis();
			affCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.info("更新临时表小区的business_cell_id的sql执行成功，影响：" + affCnt + ",耗时:" + (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("更新临时表小区的business_cell_id的sql执行失败！sql=" + sql);
			return false;
		}

		// 将cell_status=2的小区信息填入到lte cell表
		String cellFields = "BUSINESS_CELL_ID,AREA_ID,CELL_NAME,LONGITUDE,LATITUDE,BAND_TYPE,COVER_TYPE,LOCAL_CELLID,SECTOR_ID,PCI,TAC,TAL,CELL_RADIUS,BAND,EARFCN,GROUND_HEIGHT,AZIMUTH,DOWNTILT,M_DOWNTILT,E_DOWNTILT,RRUNUM,RRUVER,ANTENNA_TYPE,INTEGRATED,PDCCH,PA,PB,COVER_RANGE,RSPOWER,ENODEB_ID,STATION_SPACE";
		sql = "insert into RNO_LTE_CELL(LTE_CELL_ID," + cellFields
				+ ",STATUS) SELECT SEQ_LTE_CELL_ID.NEXTVAL," + cellFields
				+ ",'N' FROM " + midTable + " where cell_status=2";
		log.debug("准备将临时表" + midTable + "的新出现的lte 小区插入到系统表：RNO_LTE_CELL中，sql="
				+ sql);
		t1 = System.currentTimeMillis();
		try {
			affCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.debug("将临时表" + midTable
					+ "的新出现的lte 小区插入到系统表：RNO_LTE_CELL中执行完成，影响：" + affCnt + ",耗时：" + (t2 - t1)
					+ "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("将临时表" + midTable
					+ "的新出现的lte 小区插入到系统表：RNO_LTE_CELL中时执行出错！sql=" + sql);

			sql = "select BUSINESS_CELL_ID,BUSINESS_ENODEB_ID,LOCAL_CELLID,CELL_ID from " + midTable;
			List<Map<String, Object>> res = RnoHelper.commonQuery(stmt, sql);
			log.debug("临时表的BUSINESS_CELL_ID情况：----");
			int i = 0;
			String msg;
			for (Map<String, Object> one : res) {
				msg = "";
				for (String k : one.keySet()) {
					msg += k + "=" + one.get(k) + ",";
				}
				log.debug((i++) + "---:" + msg);
			}
			log.debug("临时表的BUSINESS_CELL_ID情况输出结束。");
			return false;
		}

		// ----地图上展示的经纬度数据---//
		// 默认为三角形，ge地图数据//
		sql = "insert into RNO_LTE_CELL_MAP_SHAPE(CELL_MAP_SHAPE_ID,LTE_CELL_ID,MAP_TYPE,SHAPE_TYPE,SHAPE_DATA) select SEQ_CELL_MAP_SHAPE_ID.NEXTVAL,LTE_CELL_ID,'E','T',FUN_RNO_CREATE_TRINGLE_SHAPE(LONGITUDE,LATITUDE,AZIMUTH) FROM RNO_LTE_CELL WHERE BUSINESS_CELL_ID IN (SELECT BUSINESS_CELL_ID FROM "
				+ midTable + " WHERE CELL_STATUS=2)";
		log.debug("补充新增小区的经纬度形状数据，sql=" + sql);
		try {
			affCnt = stmt.executeUpdate(sql);
			t2 = System.currentTimeMillis();
			log.debug("补充新增小区的经纬度形状数据执行完成，影响：" + affCnt + ",耗时：" + (t2 - t1) + "ms");
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("补充新增小区的经纬度形状数据时执行出错！sql=" + sql);
			return false;
		}
		long endTime = System.currentTimeMillis();
		log.debug("方法importAppend总共耗时：" + (endTime - begTime) + "ms");
		return true;
	}

	/**
	 * 覆盖模式导入
	 * 
	 * @param stmt
	 * @return
	 * @author brightming 2014-5-16 下午4:04:26
	 */
	private boolean importOverwrite(Statement stmt) {
		log.debug("进入方法：importOverwrite");
		long begTime = System.currentTimeMillis(), endTime;
		log.debug("调用importAppend方法");
		boolean ok = importAppend(stmt);
		if (!ok) {
			log.error("处理新增数据时出错！");
			bufMsg.append("处理新增数据时出错 <br/>");
			return false;
		}

		endTime = System.currentTimeMillis();
		log.debug("执行方法：importOverwrite耗时：" + (endTime - begTime) + "ms");
		return true;
	}

	/**
	 * 删除模式导入
	 * 
	 * @param stmt
	 * @return
	 * @author brightming 2014-5-16 下午4:04:59
	 */
	/*
	 * private boolean importDelete(Statement stmt) { log.debug("进入方法：importDelete");
	 * 
	 * long begTime = System.currentTimeMillis(), endTime; long t1, t2;
	 * 
	 * String sql = ""; String business_cell_id=""; int i =1; for(String v : bci){ if(i==1){ business_cell_id += v; i++;
	 * }else{ business_cell_id +="," + v; } } sql = "select business_cell_id from "+ midTable +
	 * " mid where cell.area_id=mid.area_id"; // --找到并删除在RNO_LTE_CELL表中存在，而在临时表中不存在的小区--// sql =
	 * "select cell.lte_cell_id from rno_lte_cell cell where cell.business_cell_id not in (" + sql + ")"; sql =
	 * "select cell.lte_cell_id from rno_lte_cell cell where cell.business_cell_id not in (" + business_cell_id + ")";
	 * // 更新状态 sql = "update RNO_LTE_CELL SET STATUS='D' WHERE LTE_CELL_ID IN (" + sql + ")";
	 * log.debug("删除在RNO_LTE_CELL表中存在而在临时表不存在的小区数据，sql=" + sql); t1 = System.currentTimeMillis(); try {
	 * stmt.executeUpdate(sql); t2 = System.currentTimeMillis(); log.debug("删除在RNO_LTE_CELL表中存在而在临时表不存在的小区数据执行完成，耗时：" +
	 * (t2 - t1) + "ms"); } catch (Exception e) { e.printStackTrace();
	 * log.error("删除在RNO_LTE_CELL表中存在而在临时表不存在的小区数据执行出错！sql=" + sql); return false; }
	 * 
	 * // --找到并删除在RNO_LTE_ENODEB表中存在，而在临时表中不存在的ENODEB--// sql =
	 * "select ENODEB.ENODEB_ID from rno_lte_ENODEB ENODEB where ENODEB.business_ENODEB_id not in (select DISTINCT(business_ENODEB_id) from "
	 * + midTable + " mid where ENODEB.area_id=mid.area_id )"; // 更新状态 sql =
	 * "update RNO_LTE_ENODEB SET STATUS='D' WHERE ENODEB_ID IN (" + sql + ")";
	 * log.debug("删除在RNO_LTE_ENODEB表中存在而在临时表不存在的ENODEB数据，sql=" + sql); t1 = System.currentTimeMillis(); try {
	 * stmt.executeUpdate(sql); t2 = System.currentTimeMillis();
	 * log.debug("删除在RNO_LTE_ENODEB表中存在而在临时表不存在的ENODEB数据执行完成，耗时：" + (t2 - t1) + "ms"); } catch (Exception e) {
	 * e.printStackTrace(); log.error("删除在RNO_LTE_ENODEB表中存在而在临时表不存在的ENODEB数据执行出错！sql=" + sql); return false; }
	 * 
	 * // 调用覆盖模式处理 log.debug("调用覆盖模式处理。"); boolean ok = importOverwrite(stmt); endTime = System.currentTimeMillis();
	 * log.debug("方法importDelete执行完成，结果：" + ok + ",耗时：" + (endTime - begTime) + "ms"); return ok; }
	 */

	/**
	 * 
	 * @title 解析LTE小区数据
	 * @param token
	 * @param f
	 * @param connection
	 * @param stmt
	 * @param cityId
	 * @param dbFieldsToTitles
	 * @return
	 * @author chao.xj
	 * @date 2015-4-3下午2:45:26
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private boolean parseLteCell(File f, Connection connection, Statement stmt,
			Map<String, DBFieldToTitle> dbFieldsToTitles) {

		PreparedStatement insertTStmt = null;
		// long st = System.currentTimeMillis();

		Date date1 = new Date();
		Date date2 = null;

		String tmpFileName = f.getName();
		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			bufMsg.append("文件：" + tmpFileName + ":无法识别的文件编码！<br/>");
			date2 = new Date();
			// setCachedInfo(token, RnoConstant.TimeConstant.TokenTime, "文件：" + tmpFileName + ":无法识别的文件编码！");
			return false;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(f), charset));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String msg = "";
		String line = "";
		date1 = new Date();
		date2 = null;
		// 忽视掉哪些不具备一定长度的标题
		try {
			String[] sps = new String[1];
			do {
				line = reader.readLine();

				if (line == null) {
					sps = new String[] {};
					break;
				}
				sps = line.split(",|\t");
			} while (sps == null || sps.length < 10);
			// 再读取下一行才是真正意义上的标题头
			line = reader.readLine();
			log.debug("真正的标题头信息:" + line);
			if (line != null) {
				sps = line.split(",|\t");
			}
			// 读取到标题头
			int fieldCnt = sps.length;
			log.debug("lte cell文件：" + tmpFileName + ",title 为：" + line + ",有"
					+ fieldCnt + "标题");

			// 判断标题的有效性
			Map<Integer, String> pois = new HashMap<Integer, String>();
			int index = -1;
			boolean find = false;
			for (String sp : sps) {
				index++;
				find = false;
				for (DBFieldToTitle dt : dbFieldsToTitles.values()) {
					// log.debug("-----dt==" + dt);
					for (String dtf : dt.titles) {
						if (dt.matchType == 1) {
							if (StringUtils.equals(dtf, sp)) {
								// log.debug("-----find " + sp + "->"
								// + dt);
								find = true;
								dt.setIndex(index);
								pois.put(index, dt.dbField);// 快速记录
								break;
							}
						} else if (dt.matchType == 0) {
							if (StringUtils.startsWith(sp, dtf)) {
								// log.debug("-----find " + sp + "->"
								// + dt);
								find = true;
								dt.setIndex(index);
								pois.put(index, dt.dbField);// 快速记录
								break;
							}
						}
					}

					if (find) {
						break;
					}
				}
			}

			// 判断标题头合法性，及各数据库字段对应的位置
			for (DBFieldToTitle dt : dbFieldsToTitles.values()) {
				if (dt.isMandatory && dt.index < 0) {
					// msg += "[" + dt.dbField + "]";
					msg += dt.getTitles();
				}
			}
			if (!StringUtils.isBlank(msg)) {
				log.error("检查lte cell文件：" + tmpFileName + "的标题头有问题！" + msg);
				bufMsg.append("检查到工参文件的标题头有问题！<br/>");
				date2 = new Date();
				bufMsg.append("在文件中缺失以下标题:" + msg + "<br/>");
				// setCachedInfo(token, "在文件中缺失以下标题:" + msg+"</br>");
				// setCachedInfo(token, RnoConstant.TimeConstant.TokenTime, "在文件中缺失以下标题:" + msg);
				// connection.releaseSavepoint(savepoint);
				return false;
			}
			// 拼装sql
			insertInoMidTableSql = "insert into " + midTable + " (";
			String ws = "";
			index = 1;
			int poi = 1;

			for (String d : dbFieldsToTitles.keySet()) {
				if (dbFieldsToTitles.get(d).index >= 0) {
					// 只对出现了的进行组sql
					dbFieldsToTitles.get(d).sqlIndex = index++;// 在数据库中的位置
					insertInoMidTableSql += d + ",";
					ws += "?,";
					poi++;
				}
			}
			areaSqlPosition.put("AREA_ID", poi++);
			insertInoMidTableSql += "AREA_ID,";
			insertInoMidTableSql += "ENODEB_STATUS,";
			insertInoMidTableSql += "CELL_STATUS";

			ws += "?,2,2";// Area_id的,以及默认的enodeb、cell存在状态为2
			if (StringUtils.isBlank(ws)) {
				log.error("没有有效标题数据！");
				bufMsg.append("没有有效标题数据！<br/>");
				return false;
			}
			insertInoMidTableSql += ") values ( " + ws + " )";
			log.debug("lte cell插入临时表的sql=" + insertInoMidTableSql);
			try {
				insertTStmt = connection
						.prepareStatement(insertInoMidTableSql);
			} catch (Exception e) {
				msg = "准备let cell插入的prepareStatement失败";
				log.error("准备lte cell入的prepareStatement失败！sql="
						+ insertInoMidTableSql);
				bufMsg.append("准备插入数据时失败<br/>");
				e.printStackTrace();
				// connection.releaseSavepoint(savepoint);
				return false;
			}

			// 逐行读取数据
			int executeCnt = 0;
			int count = 2;
			boolean handleLineOk = false;
			long totalDataNum = 0;
			DateUtil dateUtil = new DateUtil();
			do {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				sps = line.split(",|\t");
				/*
				 * if (sps.length != fieldCnt) { continue; }
				 */
				totalDataNum++;

				handleLineOk = handleLteCellLine(sps, fieldCnt, pois,
						dbFieldsToTitles, insertTStmt, dateUtil);
				count++;
				if (handleLineOk == true) {
					executeCnt++;
					sucCnt++;
				} else {
					if (!isSelectedCity) {
						return false;
					}
					failCnt++;
					System.out.println("文件第" + count + "行导入失败！");

				}
				if (executeCnt > 5000) {
					// 每5000行执行一次
					try {
						insertTStmt.executeBatch();
						insertTStmt.clearBatch();

						executeCnt = 0;
					} catch (SQLException e) {
						e.printStackTrace();
						// connection.rollback(savepoint);
						// 清除数据，关闭资源，返回失败
						try {
							stmt.executeUpdate("truncate table " + midTable);
						} catch (Exception e3) {
							e3.printStackTrace();
						}
						insertTStmt.close();
						connection.commit();
						return false;
					}
				}
			} while (!StringUtils.isBlank(line));
			// 执行
			if (executeCnt > 0) {
				insertTStmt.executeBatch();
				// 当数量少的时候提交
			}
			if (commonIdCount > 0) {
				msgStr = "有" + commonIdCount + "条数据CellId重复。";
			}
			log.debug("lte cell数据文件：" + tmpFileName + "共有：" + totalDataNum
					+ "行记录数据");
			System.out.println("lte cell数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据");
			// ----一下进行数据处理----//
			String attMsg = "文件：" + tmpFileName;
			long t1, t2;
			Date d1 = new Date();

			ResultInfo resultInfo = null;
			Date d2 = new Date();

			// 一个文件一个提交
			// connection.commit();
		} catch (Exception e) {
			e.printStackTrace();
			// try {
			// connection.rollback(savepoint);
			// } catch (SQLException e1) {
			// e1.printStackTrace();
			// }
			try {
				insertTStmt.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			/*
			 * try { stmt.executeUpdate("truncate table "+midTable); } catch (Exception e3) { e3.printStackTrace(); }
			 */

			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (insertTStmt != null) {
				try {
					insertTStmt.close();
				} catch (SQLException e) {
				}
			}
		}

		return true;
	}

	/**
	 * 
	 * @title 处理每一行数据
	 * @param sps
	 * @param expectFieldCnt
	 * @param pois
	 * @param dbFtts
	 * @param insertTempStatement
	 * @param dateUtil
	 * @return
	 * @author chao.xj
	 * @date 2015-4-3下午2:45:12
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private boolean handleLteCellLine(String[] sps, int expectFieldCnt,
			Map<Integer, String> pois, Map<String, DBFieldToTitle> dbFtts,
			PreparedStatement insertTempStatement, DateUtil dateUtil) {
		// log.debug("handleHwNcsLine--sps="+sps+",expectFieldCnt="+expectFieldCnt+",pois="+pois+",dbFtts="+dbFtts);
		long cityId = 0;

		if (sps == null) {
			return false;
		}
		String dbField = "";
		DBFieldToTitle dt = null;
		String areaName = "";
		String eNodeBID = "", localCellID = "", cellId = "", businessCellId = "";
		long areaId = 0;
		for (int i = 0; i < expectFieldCnt; i++) {
			dbField = pois.get(i);// 该位置对应的数据库字段
			// log.debug(i+" -> dbField="+dbField);
			if (dbField == null) {
				continue;
			}

			dt = dbFtts.get(dbField);// 该数据库字段对应的配置信息
			if (dbField.equals("AREA_NAME")) {
				areaName = sps[i];
				if (!sps[i].equals(cityName)) {
					isSelectedCity = false;
					return false;
				}
				if (areaNameToIds.get(areaName) != null) {
					areaId = areaNameToIds.get(areaName);
				} else {
					return false;
				}
				// 添加系统中存在的区域集合
				excelAreaIdSet.add(areaId);
				try {
					insertTempStatement.setLong(areaSqlPosition.get("AREA_ID"), areaId);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (dbField.equals("BUSINESS_ENODEB_ID")) {
				// eNodeBID
				eNodeBID = sps[i];
			}
			if (dbField.equals("LOCAL_CELLID")) {
				// Local CellID
				localCellID = sps[i];
			}
			if (dt.getTit().equals("CellID")) {
				// CellID
				cellId = sps[i];
			}
			if (dt != null) {
				if (sps.length - 1 < i) {
					setIntoPreStmt(insertTempStatement, dt, null, dateUtil);
				} else {
					setIntoPreStmt(insertTempStatement, dt, sps[i], dateUtil);
				}
			}
		}
		if (isLocalCellId) {
			businessCellId = eNodeBID + localCellID;
		} else {
			businessCellId = eNodeBID + cellId;
		}
		// bci.add(businessCellId);
		if (businessCellIds.get(businessCellId) != null) {
			commonIdCount++;
			return false;
		} else {
			businessCellIds.put(businessCellId, businessCellId);
		}
		try {
			insertTempStatement.addBatch();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	private void setIntoPreStmt(PreparedStatement pstmt, DBFieldToTitle dt,
			String val, DateUtil dateUtil) {
		String type = dt.getDbType();
		int index = dt.sqlIndex;
		if (index < 0) {
			log.error(dt + "在sql插入语句中，没有相应的位置！");
			bufMsg.append(dt + "在插入语句中，没有相应的位置<br/>");
			return;
		}
		if (StringUtils.equalsIgnoreCase("Long", type)) {
			if (!StringUtils.isBlank(val) && StringUtils.isNumeric(val)) {
				try {
					pstmt.setLong(index, Long.parseLong(val));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					pstmt.setNull(index, java.sql.Types.BIGINT);
				} catch (SQLException e) {
				}
			}
		} else if (StringUtils.equalsIgnoreCase("Date", type)) {
			if (!StringUtils.isBlank(val)) {
				try {
					Date date = dateUtil.parseDateArbitrary(val);
					if (date != null) {
						pstmt.setTimestamp(index,
								new java.sql.Timestamp(date.getTime()));
					} else {
						pstmt.setNull(index, java.sql.Types.DATE);
					}
				} catch (SQLException e) {
					e.printStackTrace();
					log.error("hwncs parse date fail:date str=" + val);
				}
			} else {
				try {
					pstmt.setNull(index, java.sql.Types.DATE);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}

		} else if (StringUtils.equalsIgnoreCase("String", type)) {
			// log.debug("val:"+val);
			if (!StringUtils.isBlank(val)) {
				try {
					pstmt.setString(index, val);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} else {
				try {
					pstmt.setNull(index, Types.VARCHAR);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else if (StringUtils.equalsIgnoreCase("Float", type)) {
			if (!StringUtils.isBlank(val)) {
				Float f = null;
				try {
					f = Float.parseFloat(val);
				} catch (Exception e) {

				}
				if (f != null) {
					try {
						pstmt.setFloat(index, f);
						return;
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				pstmt.setNull(index, Types.FLOAT);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} else if (StringUtils.startsWithIgnoreCase("int", type)) {
			if (!StringUtils.isBlank(val) && StringUtils.isNumeric(val)) {
				try {
					pstmt.setInt(index, Integer.parseInt(val));
				} catch (NumberFormatException e) {
				} catch (SQLException e) {
				}
			} else {
				try {
					pstmt.setNull(index, Types.INTEGER);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} else if (StringUtils.startsWithIgnoreCase("Double", type)) {
			if (!StringUtils.isBlank(val) && isNumeric(val)) {
				try {
					pstmt.setDouble(index, Double.parseDouble(val));
				} catch (NumberFormatException e) {
				} catch (SQLException e) {
				}
			} else {
				try {
					pstmt.setNull(index, Types.DOUBLE);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @title 从xml配置 文件中读取lte cell 数据库到excel的映射关系
	 * @return
	 * @author chao.xj
	 * @date 2015-4-2上午11:26:44
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, DBFieldToTitle> readDbToTitleCfgFromXml() {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(
					RnoLteCellParser.class.getResource(
							"lteCellDbToTitles.xml").getPath()));
			SAXReader reader = new SAXReader();
			Document doc = reader.read(in);
			Element root = doc.getRootElement();
			for (Object o : root.elements()) {
				Element e = (Element) o;
				DBFieldToTitle dt = new DBFieldToTitle();
				for (Object obj : e.elements()) {
					Element e1 = (Element) obj;
					String key = e1.getName();
					String val = e1.getTextTrim();
					if ("name".equals(key)) {
						dt.setDbField(val);
					}
					if ("type".equals(key)) {
						dt.setDbType(val);
					}
					if ("essential".equals(key)) {
						if (StringUtils.equals(val, "1")) {
							// mandaroty
							dt.setMandatory(true);
						} else {
							dt.setMandatory(false);
						}
					}
					if ("match".equals(key)) {
						dt.setMatchType(Integer.parseInt(val));
					}
					if ("exceltitle".equals(key)) {
						dt.setTit(val);
						String[] v = val.split(",");
						for (String vo : v) {
							dt.addTitle(vo);
						}
					}

				}
				dbCfgs.put(dt.dbField, dt);
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return dbCfgs;
	}

	public static class DBFieldToTitle {
		String dbField;
		int matchType;// 0：模糊匹配，1：精确匹配
		int index = -1;// 在文件中出现的位置，从0开始
		boolean isMandatory = true;// 是否强制要求出现
		private String dbType;// 类型，Number，String,Date
		List<String> titles = new ArrayList<String>();
		String tit;

		int sqlIndex = -1;// 在sql语句中的位置

		public String getDbField() {
			return dbField;
		}

		public void setDbField(String dbField) {
			this.dbField = dbField;
		}

		public int getMatchType() {
			return matchType;
		}

		public void setMatchType(int matchType) {
			this.matchType = matchType;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public List<String> getTitles() {
			return titles;
		}

		public void setTitles(List<String> titles) {
			this.titles = titles;
		}

		public boolean isMandatory() {
			return isMandatory;
		}

		public void setMandatory(boolean isMandatory) {
			this.isMandatory = isMandatory;
		}

		public String getDbType() {
			return dbType;
		}

		public void setDbType(String dbType) {
			this.dbType = dbType;
		}

		public void addTitle(String t) {
			if (!StringUtils.isBlank(t)) {
				titles.add(t);
			}
		}

		public String getTit() {
			return tit;
		}

		public void setTit(String tit) {
			this.tit = tit;
		}

		@Override
		public String toString() {
			return "DBFieldToTitle [dbField=" + dbField + ", matchType="
					+ matchType + ", index=" + index + ", isMandatory="
					+ isMandatory + ", dbType=" + dbType + ", titles=" + titles
					+ "]";
		}

	}

	public boolean isNumeric(String num) {
		try {
			Double.parseDouble(num);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private void printTmpTable1(String sql, Statement stmt) {
		// TODO Auto-generated method stub
		List<Map<String, Object>> res = RnoHelper.commonQuery(stmt, sql);
		if (res != null && res.size() > 0) {
			log.debug("\r\n---数据如下：");
			for (Map<String, Object> one : res) {
				log.debug(one);
			}
			log.debug("---数据展示完毕\r\n");
		} else {
			log.debug("---数据为空！");
		}
	}

	@Override
	public JobStatus runJobInternal(JobProfile job, Connection conn, Statement stmt) {
		log.debug("进入RnoLteCellParser方法：parseDataInternal。 ");
		long jobId = job.getJobId();
		JobStatus status = new JobStatus(jobId);
		JobReport report = new JobReport(jobId);

		// 获取job相关的信息
		String sql = "select * from RNO_DATA_COLLECT_REC where JOB_ID=" + jobId;
		List<Map<String, Object>> recs = RnoHelper.commonQuery(stmt, sql);
		RnoDataCollectRec dataRec = null;
		if (recs != null && recs.size() > 0) {
			dataRec = RnoHelper.commonInjection(RnoDataCollectRec.class,
					recs.get(0), new DateUtil());
		}

		// log.debug("jobId=" + jobId + ",对应的dataRec=" + dataRec);
		if (dataRec == null) {
			log.error("转换RnoDataCollectRec失败！");
			// 失败了
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());

			// 报告
			report.setFinishState("失败");
			report.setStage("获取上传文件");
			report.setBegTime(new Date());
			report.setReportType(1);
			report.setAttMsg("未找到上传文件！");
			addJobReport(report);
			bufMsg.append("未找到上传文件！");
			return status;
		}
		// 准备
		cityId = dataRec.getCityId();
		isLocalCellId = dataRec.getIdForCell().equals("1") ? true : false;
		System.out.println("当前工参标识使用ENODEBID+" + (isLocalCellId ? "localCellId" : "cellId") + "组合方式");
		sql = "select parent_id,name from sys_area where area_id=" + cityId;
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				ai = rs.getLong("PARENT_ID");
				cityName = rs.getString("NAME");
			}
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		Date baseDate = dataRec.getBusinessTime();
		DateUtil dateUtil = new DateUtil();
		String baseDateStr = dateUtil.format_yyyyMMdd(baseDate);
		String fileName = dataRec.getFileName();
		filePath = FileInterpreter.makeFullPath(dataRec.getFullPath());
		// file = new File(filePath);
		file = FileTool.getFile(filePath);
		long dataId = dataRec.getDataCollectId();

		String msg = "";
		// 开始解析
		List<File> allFiles = new ArrayList<File>();// 将所有待处理的文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		Date date1 = new Date(), date2;
		if (fileName.endsWith(".zip") || fileName.endsWith("ZIP")
				|| fileName.endsWith("Zip")) {
			date1 = new Date();
			fromZip = true;
			// 压缩包
			log.info("上传的文件是一个压缩包。");

			// 进行解压
			String path = file.getParentFile().getPath();
			destDir = path + "/"
					+ UUID.randomUUID().toString().replaceAll("-", "") + "/";

			//
			boolean unzipOk = false;
			try {
				unzipOk = ZipFileHandler.unZip(
						FileInterpreter.makeFullPath(file.getAbsolutePath()),
						destDir);
			} catch (Exception e) {
				msg = "压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg);
				bufMsg.append(msg + "<br/>");
				e.printStackTrace();

				// job报告
				date2 = new Date();
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setStage(DataParseProgress.Decompress.toString());
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setAttMsg("压缩包解析失败！请确认压缩包文件是否被破坏！");
				addJobReport(report);

				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='"
						+ DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID="
						+ dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					conn.commit();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				// job status
				status.setJobState(JobState.Failed);
				status.setUpdateTime(date2);
				return status;

			}
			if (!unzipOk) {
				msg = "解压失败 ！仅支持zip格式的压缩包！ ";
				log.error(msg);
				bufMsg.append(msg + "<br/>");
				// job报告
				date2 = new Date();
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setStage(DataParseProgress.Decompress.toString());
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setAttMsg("解压失败 ！仅支持zip格式的压缩包！");
				addJobReport(report);

				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='"
						+ DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID="
						+ dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					conn.commit();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				// job status
				status.setJobState(JobState.Failed);
				status.setUpdateTime(date2);
				return status;

			}
			file = new File(destDir);
			File[] files = file.listFiles();
			for (File f : files) {
				// 只要文件，不要目录
				if (f.isFile() && !f.isHidden()) {
					allFiles.add(f);
				}
			}

			// ---报告----//
			date2 = new Date();
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setFinishState(DataParseStatus.Succeded.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setAttMsg("解压文件：" + fileName + ",大小："
					+ RnoHelper.getPropSizeExpression(dataRec.getFileSize()));
			addJobReport(report);
		} else if (fileName.endsWith(".rar")) {
			msg = "请用zip格式压缩文件！";
			log.error(msg);
			bufMsg.append(msg + "<br/>");
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("解压失败 ！请用zip格式压缩文件！");
			addJobReport(report);

			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='"
					+ DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
				conn.commit();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(date2);
			return status;

		} else {
			log.info("上传的是一个普通文件。");
			allFiles.add(file);
		}
		if (allFiles.isEmpty()) {
			msg = "未上传有效的文件！zip包里不能再包含有文件夹！";
			log.error(msg);
			bufMsg.append(msg + "<br/>");
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("未上传有效的文件！注意zip包里不能再包含有文件夹！");
			addJobReport(report);

			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='"
					+ DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(date2);
			return status;
		}

		// 正在解析
		sql = "update rno_data_collect_rec set FILE_STATUS='"
				+ DataParseStatus.Parsing.toString()
				+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
		try {
			stmt.executeUpdate(sql);
			conn.commit();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;
		int totalFileCnt = allFiles.size();
		int i = 0;

		// GSM新站数据字段对应标题
		Map<String, DBFieldToTitle> cellDbFieldsToTitles = readDbToTitleCfgFromXml();

		for (File f : allFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
				date1 = new Date();
				parseOk = parseDataInternal(report, dataRec, tmpFileName, f, conn, cityId, stmt, baseDateStr,
						cellDbFieldsToTitles);
				i++;
				date2 = new Date();
				report.setStage("文件处理总结");
				report.setReportType(1);
				if (parseOk) {
					if (sc == 0) {
						report.setFinishState(DataParseStatus.Failall.toString());
					} else if (sc == (sc + fc)) {
						report.setFinishState(DataParseStatus.Succeded.toString());
					} else {
						report.setFinishState(DataParseStatus.Suc.toString());
					}
				} else {
					report.setFinishState(DataParseStatus.Failall.toString());
				}
				report.setBegTime(date1);
				report.setEndTime(date2);
				if (parseOk) {
					report.setAttMsg("成功完成文件（" + i + "/" + totalFileCnt + "）:"
							+ tmpFileName + "<br/>" + bufMsg.toString());
					sucCnt++;
				} else {
					report.setAttMsg("失败完成文件（" + i + "/" + totalFileCnt + "）:"
							+ tmpFileName + "<br/>" + bufMsg.toString());
				}
				addJobReport(report);
			} catch (Exception e) {
				e.printStackTrace();
				date2 = new Date();
				msg = tmpFileName + "文件解析出错！";
				report.setStage("文件处理总结");
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setReportType(1);
				report.setAttMsg("文件解析出错（" + i + "/" + totalFileCnt + "）:"
						+ tmpFileName + "<br/>" + bufMsg.toString());
				addJobReport(report);
				log.error(msg);
			}
		}

		if (sucCnt > 0) {
			status.setJobState(JobState.Finished);
			status.setUpdateTime(new Date());
		} else {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
		}

		if (sc > 0 && sc == (fc + sc)) {
			// 全部成功
			sql = "update rno_data_collect_rec set file_status='"
					+ DataParseStatus.Succeded.toString()
					+ "' where data_collect_id=" + dataRec.getDataCollectId();
		} else {
			if (sc == 0) {
				sql = "update rno_data_collect_rec set file_status='"
						+ DataParseStatus.Failall.toString()
						+ "' where data_collect_id="
						+ dataRec.getDataCollectId();
			} else {
				sql = "update rno_data_collect_rec set file_status='"
						+ DataParseStatus.Suc.toString()
						+ "' where data_collect_id="
						+ dataRec.getDataCollectId();
			}
		}
		log.debug("更新结果状态 rno_data_collect_rec  sql:" + sql);
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return status;

	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		// TODO Auto-generated method stub

	}
}
