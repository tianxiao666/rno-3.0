package com.iscreate.op.service.rno.task.pci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.ZipFileHandler;

/**
 * @author chen.c10
 *
 */
public strictfp class LteFlowFileParser {
	private final static Log log = LogFactory.getLog(LteFlowFileParser.class);
	private final static String FlowTable = "RNO_CELL_FLOW";
	private JobRunnable jobWorker;
	private JobStatus status;
	private JobReport report;
	/** 小区到KS的映射 **/
	private Map<String, Double> cellToKs;
	private Connection connection;
	private double dtKs = 0;
	private DateUtil dateUtil = new DateUtil();

	public LteFlowFileParser(JobRunnable jobWorker, JobStatus status, JobReport report, Map<String, Double> cellToKs,
			Connection connection, double dtKs) {
		super();
		this.jobWorker = jobWorker;
		this.status = status;
		this.report = report;
		this.cellToKs = cellToKs;
		this.connection = connection;
		this.dtKs = dtKs;
	}

	/**
	 * 
	 * @author chen.c10
	 * @date 2016年4月7日
	 * @version RNO 3.0.1
	 */
	boolean parse(RnoDataCollectRec dataRec) {
		// 准备
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		Date baseDate = dataRec.getBusinessTime();
		String baseDateStr = dateUtil.format_yyyyMMdd(baseDate);
		String fileName = dataRec.getFileName();
		String filepath = FileInterpreter.makeFullPath(dataRec.getFullPath());
		File file = FileTool.getFile(filepath);

		String msg = "";
		// 开始解析
		List<File> allFiles = new ArrayList<File>();// 将所有待处理的文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		if (fileName.endsWith(".zip") || fileName.endsWith("ZIP") || fileName.endsWith("Zip")) {
			fromZip = true;
			// 压缩包
			// log.info("上传的文件是一个压缩包。");

			// 进行解压
			String path = file.getParentFile().getPath();
			destDir = path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "/";

			try {
				if (!ZipFileHandler.unZip(FileInterpreter.makeFullPath(file.getAbsolutePath()), destDir)) {
					msg = "压缩包解析失败！请确认压缩包文件是否被破坏！";
					log.error(msg);
					status.setJobState(JobState.Failed);
					status.setUpdateTime(new Date());
					// 报告
					report.setStage("解压流量文件");
					report.setEndTime(new Date());
					report.setFinishState("失败");
					report.setAttMsg(msg);
					return false;
				}
			} catch (Exception e) {
				msg = "压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg);
				e.printStackTrace();
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				// 报告
				report.setStage("解压流量文件");
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg(msg);
				return false;
			}
			file = new File(destDir);
			File[] files = file.listFiles();
			if (files != null) {
				for (File f : files) {
					// 只要文件，不要目录
					if (f.isFile() && !f.isHidden()) {
						allFiles.add(f);
					}
				}
			}
		} else if (fileName.endsWith(".rar")) {
			msg = "流量文件格式错误！";
			log.error(msg);
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			// 报告
			report.setStage("解压流量文件");
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg(msg);
			return false;
		} else {
			// log.info("上传的是一个普通文件。");
			allFiles.add(file);
		}
		if (allFiles.isEmpty()) {
			msg = "未获取到有效的流量文件！";
			log.error(msg);
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			// 报告
			report.setStage("解析流量文件");
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg(msg);
			return false;
		}

		String tmpFileName = fileName;
		int sucCnt = 0;

		// LTE新站数据字段对应标题
		Map<String, DBFieldToTitle> cellFlowDbFieldsToTitles = readDbToTitleCfgFromXml("cellFlowToTitles.xml");

		for (File f : allFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}

				if (parseCsv(jobWorker, report, dataRec, tmpFileName, f, baseDateStr, cellFlowDbFieldsToTitles)) {
					sucCnt++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (sucCnt == 0) {
			msg = "流量文件解析全部失败！";
			log.error(msg);
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			// 报告
			report.setStage("解析流量文件");
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg(msg);
			return false;
		}

		report.setBegTime(new Date());

		String sql = "select cell_id,cell_dflow,cell_uflow from RNO_CELL_FLOW";
		Map<String, Float> cellToFlow = commonQueryToMap(stmt, sql);

		sql = "select cell_id,is_dt_cell from RNO_CELL_FLOW";
		Map<String, String> isDtCellMap = commonQueryToMap1(stmt, sql);

		if (cellToFlow == null || isDtCellMap == null) {
			msg = "查找相应的小区流量数据失败！";
			log.error(msg);
			// 失败了
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());

			// 报告
			report.setStage("获取小区流量数据");
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg(msg);
			return false;
		}

		// 计算全网平均业务量
		double perFlow = 0;
		for (double flow : cellToFlow.values()) {
			perFlow += flow < 100 ? 100 : flow;
		}
		perFlow = perFlow / cellToFlow.size();

		for (String cellId : cellToFlow.keySet()) {
			double ks = 1;
			double flow = cellToFlow.get(cellId) < 100 ? 100 : cellToFlow.get(cellId);
			if (isDtCellMap.get(cellId).equals("Y")) {
				ks = 1 + dtKs * flow / perFlow;
			} else {
				ks = flow / perFlow;
			}
			cellToKs.put(cellId.intern(), ks);
		}
		try {
			stmt.executeUpdate("truncate table RNO_CELL_FLOW");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public static Map<String, Float> commonQueryToMap(Statement stmt, String sql) {
		ResultSet rs = null;
		Map<String, Float> one = new HashMap<String, Float>();
		Set<String> set = new HashSet<String>();
		String str;
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				str = rs.getString(1);
				if (set.add(rs.getString(1))) {
					one.put(str,
							Float.parseFloat(rs.getString(2) == null ? "0" : rs.getString(2))
									+ Float.parseFloat(rs.getString(3) == null ? "0" : rs.getString(3)));
				} else {
					one.put(str,
							Float.parseFloat(rs.getString(2) == null ? "0" : rs.getString(2))
									+ Float.parseFloat(rs.getString(3) == null ? "0" : rs.getString(3)) + one.get(str));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return one;
	}

	public static Map<String, String> commonQueryToMap1(Statement stmt, String sql) {
		ResultSet rs = null;
		Map<String, String> one = new HashMap<String, String>();
		Set<String> set = new HashSet<String>();
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				if (set.add(rs.getString(1))) {
					one.put(rs.getString(1), rs.getString(2).equals("是") ? "Y" : "N");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return one;
	}

	private boolean parseCsv(JobRunnable jobWorker, JobReport report, RnoDataCollectRec dataRec, String tmpFileName,
			File f, String baseDateStr, Map<String, DBFieldToTitle> cellDbFieldsToTitles) {
		PreparedStatement insertCellTStmt = null;

		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		// log.debug(tmpFileName + " 文件编码：" + charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			return false;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
		} catch (Exception e) {
			log.error("文件：" + tmpFileName + ":读取失败！");
			e.printStackTrace();
			return false;
		}

		String msg = "";
		String line = "";
		try {
			String[] sps = new String[1];
			do {
				line = reader.readLine();
				if (line == null) {
					sps = new String[] {};
					break;
				}
				sps = line.split(",");
			} while (sps == null || sps.length < 2);
			// 读取到标题头：标题头的全部数量
			int fieldCnt = sps.length;
			log.debug("文件：" + tmpFileName + ",title 为：" + line + ",有" + fieldCnt + "标题");

			// 判断标题的有效性
			Map<Integer, String> cellPois = new HashMap<Integer, String>();
			int index = -1;
			boolean find = false;
			int num = 0;
			// 专门记录邻区位置
			for (String sp : sps) {
				index++;
				find = false;
				for (DBFieldToTitle dt : cellDbFieldsToTitles.values()) {
					for (String dtf : dt.titles) {
						if (dt.matchType == 1) {
							if (org.apache.commons.lang.StringUtils.equals(dtf, sp)) {
								find = true;
								dt.setIndex(index);
								cellPois.put(index, dt.dbField);// 快速记录标题位置
								break;
							}
						} else if (dt.matchType == 0) {
							if (org.apache.commons.lang.StringUtils.startsWith(sp, dtf)) {
								find = true;
								dt.setIndex(index);
								cellPois.put(index, dt.dbField);// 快速记录
								break;
							}
						}
					}
					if (find) {
						num++;
						break;
					}
				}
			}

			// 判断标题头合法性，及各数据库字段对应的位置
			for (DBFieldToTitle dt : cellDbFieldsToTitles.values()) {
				if ((dt.isMandatory && dt.index < 0) || sps.length != cellDbFieldsToTitles.size()
						|| num != cellDbFieldsToTitles.size()) {
					msg += "[" + dt.dbField + "在文件中未找到对应的数据]\r\n";
				}
			}
			if (!org.apache.commons.lang.StringUtils.isBlank(msg)) {
				log.error("检查文件：" + tmpFileName + "的标题头有问题！" + msg);
				return false;
			}
			String param = "";
			String ws = "";
			index = 1;
			for (String d : cellDbFieldsToTitles.keySet()) {
				if (cellDbFieldsToTitles.get(d).index >= 0) {
					// 只对出现了的进行组sql
					cellDbFieldsToTitles.get(d).sqlIndex = index++;// 在数据库中的位置
					param += "," + d;
					ws += ",?";
				}
			}
			if (org.apache.commons.lang.StringUtils.isBlank(ws)) {
				log.error("没有有效标题数据！");
				return false;
			}
			// 拼装sql
			String insertCellTableSql = "insert into " + FlowTable + " (DATAID" + param + ") values (SEQ_" + FlowTable
					+ ".NEXTVAL" + ws + ")";
			log.debug("insertCellTableSql=" + insertCellTableSql);
			try {
				insertCellTStmt = connection.prepareStatement(insertCellTableSql);
			} catch (Exception e) {
				msg = "准备流量数据插入的prepareStatement失败";
				log.error("准备流量数据插入的prepareStatement失败！sql=" + insertCellTableSql);
				e.printStackTrace();
				return false;
			}

			// 逐行读取数据
			int executeCnt = 0;
			int addLineCnt = 0;
			long totalDataNum = 0;

			long length = 0;

			do {
				line = reader.readLine();

				if (line == null) {
					break;
				}
				sps = line.split(",");
				length = sps.length;
				if (length != fieldCnt) {
					continue;
				}
				totalDataNum++;

				if (handleLine(jobWorker, sps, fieldCnt, cellPois, cellDbFieldsToTitles, insertCellTStmt, dateUtil,
						baseDateStr)) {
					executeCnt++;
					addLineCnt++;
					try {
						insertCellTStmt.addBatch();
					} catch (SQLException e) {
						addLineCnt = 0;
						e.printStackTrace();
					}
				}

				if (executeCnt > 5000) {
					// 每5000行执行一次
					try {
						insertCellTStmt.executeBatch();
						if (!connection.getAutoCommit()) {
							connection.commit();
						}
						insertCellTStmt.clearBatch();
						executeCnt = 0;
					} catch (SQLException e) {
						// connection.rollback(savepoint);
						e.printStackTrace();
					}
				}
			} while (!org.apache.commons.lang.StringUtils.isBlank(line));
			// 执行
			if (executeCnt > 0) {
				insertCellTStmt.executeBatch();
				if (!connection.getAutoCommit()) {
					connection.commit();
				}
				insertCellTStmt.clearBatch();
			}
			insertCellTStmt.closeOnCompletion();

			log.debug("流量数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据,共入库" + addLineCnt + "行记录");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (insertCellTStmt != null) {
				try {
					insertCellTStmt.close();
				} catch (SQLException e) {
				}
			}
		}
		return true;
	}

	private boolean handleLine(JobRunnable jobWorker, String[] sps, int expectFieldCnt, Map<Integer, String> pois,
			Map<String, DBFieldToTitle> dbFtts, PreparedStatement insertStatement, DateUtil dateUtil, String baseDateStr) {
		String dbField = "";
		DBFieldToTitle dt = null;
		for (int i = 0; i < expectFieldCnt; i++) {
			dbField = pois.get(i);// 该位置对应的数据库字段
			// log.debug(i+" -> dbField="+dbField);
			if (dbField == null) {
				continue;
			}
			dt = dbFtts.get(dbField);// 该数据库字段对应的配置信息
			if ("MEA_DATE".equals(dbField)) {
				// 判断选择日期与文件录入日期是否相同，不相同不录入
				// log.debug("选择日期："+baseDateStr+"－－－－－－－－－－－－－文件日期："+sps[i]);
				if (!dateUtil.isSameDay(dateUtil.parseDateArbitrary(baseDateStr), dateUtil.parseDateArbitrary(sps[i]))) {
					return false;
				}
			}
			if ("CELL_ID".equals(dbField)) {
				sps[i] = sps[i].replace("-", "").substring(5);
			}
			if (dt != null) {
				setIntoPreStmt(insertStatement, dt, sps[i], dateUtil);
			}
		}
		return true;
	}

	private void setIntoPreStmt(PreparedStatement pstmt, DBFieldToTitle dt, String val, DateUtil dateUtil) {
		String type = dt.getDbType();
		int index = dt.sqlIndex;
		if (index < 0) {
			log.error(dt + "在sql插入语句中，没有相应的位置！");
			return;
		}
		if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("Long", type)) {
			if (!org.apache.commons.lang.StringUtils.isBlank(val) && org.apache.commons.lang.StringUtils.isNumeric(val)) {
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
		} else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("Date", type)) {
			if (!org.apache.commons.lang.StringUtils.isBlank(val)) {
				try {
					Date date = dateUtil.parseDateArbitrary(val);
					if (date != null) {
						pstmt.setTimestamp(index, new java.sql.Timestamp(date.getTime()));
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

		} else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("String", type)) {
			// log.debug("val:"+val);
			if (!org.apache.commons.lang.StringUtils.isBlank(val)) {
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
		} else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("Float", type)) {
			if (!org.apache.commons.lang.StringUtils.isBlank(val)) {
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
		} else if (org.apache.commons.lang.StringUtils.startsWithIgnoreCase("int", type)) {
			if (!org.apache.commons.lang.StringUtils.isBlank(val) && org.apache.commons.lang.StringUtils.isNumeric(val)) {
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
		} else if (org.apache.commons.lang.StringUtils.equalsIgnoreCase("double", type)) {
			if (!org.apache.commons.lang.StringUtils.isBlank(val)) {
				Double d = null;
				try {
					d = Double.parseDouble(val);
				} catch (Exception e) {

				}
				if (d != null) {
					try {
						pstmt.setDouble(index, d);
						return;
					} catch (NumberFormatException e) {
						e.printStackTrace();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				pstmt.setNull(index, Types.DOUBLE);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public static Map<String, DBFieldToTitle> readDbToTitleCfgFromXml(String proFile) {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(LteFlowFileParser.class.getResource(proFile).getPath()));
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
						if (org.apache.commons.lang.StringUtils.equals(val, "1")) {
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
						String[] v = val.split(",");
						for (String vo : v) {
							dt.addTitle(vo);
						}
					}

				}
				dbCfgs.put(dt.dbField, dt);
			}
		} catch (Exception e) {
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
			if (!org.apache.commons.lang.StringUtils.isBlank(t)) {
				titles.add(t);
			}
		}
	}
}
