package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

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

public class G4MroMrsParserJobRunnable extends DbParserBaseJobRunnable {
	private static final Log log = LogFactory.getLog(G4MroMrsParserJobRunnable.class);

	private File file = null;
	private String filePath = null;
	private String tablesCfgFile = "g4MroMrsDbToTitles.xml";

	public G4MroMrsParserJobRunnable() {
		super();
		super.setJobType("G4MROMRSFILE");
	}

	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connectionTmp, Statement stmtTmp) {

		long jobId = job.getJobId();
		JobStatus status = new JobStatus(jobId);
		JobReport report = new JobReport(jobId);

		// 获取job相关的信息
		String sql = "select * from RNO_DATA_COLLECT_REC where JOB_ID=" + jobId;
		List<Map<String, Object>> recs = RnoHelper.commonQuery(stmt, sql);
		RnoDataCollectRec dataRec = null;
		if (recs != null && recs.size() > 0) {
			dataRec = RnoHelper.commonInjection(RnoDataCollectRec.class, recs.get(0), new DateUtil());
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

			return status;
		}

		// 准备
		long cityId = dataRec.getCityId();
		String fileName = dataRec.getFileName();
		filePath = FileInterpreter.makeFullPath(dataRec.getFullPath());
		file = FileTool.getFile(filePath);

		String msg = "";

		// 开始解析
		List<File> allMrFiles = new ArrayList<File>();// 将所有待处理的MROMRS文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		Date date1 = new Date(), date2;
		if (fileName.endsWith(".zip") || fileName.endsWith("ZIP") || fileName.endsWith("Zip")) {
			date1 = new Date();
			fromZip = true;
			// 压缩包
			log.info("上传的MROMRS文件是一个压缩包。");

			// 进行解压
			String path = file.getParentFile().getPath();
			destDir = path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "/";
			boolean unzipOk = false;
			try {
				unzipOk = ZipFileHandler.unZip(FileInterpreter.makeFullPath(file.getAbsolutePath()), destDir);
			} catch (Exception e) {
				msg = "压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg);
				e.printStackTrace();
				// super.setCachedInfo(token, msg);
				// clearResource(destDir, null);

				// job报告
				date2 = new Date();
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setStage(DataParseProgress.Decompress.toString());
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setAttMsg("压缩包解析失败！请确认压缩包文件是否被破坏！");
				addJobReport(report);

				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					connection.commit();
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
				// super.setCachedInfo(token, msg);
				// clearResource(destDir, null);

				// job报告
				date2 = new Date();
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setStage(DataParseProgress.Decompress.toString());
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setAttMsg("解压失败 ！仅支持zip格式的压缩包！");
				addJobReport(report);

				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					connection.commit();
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
					allMrFiles.add(f);
				}
			}

			// ---报告----//
			date2 = new Date();
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setFinishState(DataParseStatus.Succeded.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setAttMsg("解压文件：" + fileName + ",大小：" + RnoHelper.getPropSizeExpression(dataRec.getFileSize()));
			addJobReport(report);
		} else if (fileName.endsWith(".rar")) {
			msg = "请用zip格式压缩文件！";
			log.error(msg);
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("解压失败 ！请用zip格式压缩文件！");
			addJobReport(report);

			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
				connection.commit();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(date2);
			return status;

		} else {
			log.info("上传的MROMRS是一个普通文件。");
			allMrFiles.add(file);
		}

		if (allMrFiles.isEmpty()) {
			msg = "未上传有效的MROMRS文件！zip包里不能再包含有文件夹！";
			log.error(msg);
			// super.setCachedInfo(token, msg);
			// clearResource(destDir, null);
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("未上传有效的MROMRS文件！注意zip包里不能再包含有文件夹！");
			addJobReport(report);

			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
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
		sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Parsing.toString()
				+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
		try {
			stmt.executeUpdate(sql);
			connection.commit();
		} catch (Exception e2) {
			e2.printStackTrace();
		}

		// 读取数据库与文本文件格式的配置
		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;

		int totalFileCnt = allMrFiles.size();
		int i = 0;
		// 获取jdbc模板实例
		ApplicationContext ac = new ClassPathXmlApplicationContext("spring/datasource-appcontx.xml");
		JdbcTemplate jdbcTemplate = (JdbcTemplate) ac.getBean("jdbcTemplate");
		Connection hiveConn = null;
		try {
			// 切换成HIVE的连接
			hiveConn = jdbcTemplate.getDataSource().getConnection();
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (File f : allMrFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
				// System.out.println("tmpFileName========="+tmpFileName);
				// 对文件进行处理：在更新该区域的lte小区的频点及PCI信息基础上进行邻区匹配及数据入库工作
				date1 = new Date();
				parseOk = parseMr(report, tmpFileName, f, hiveConn, cityId);
				i++;
				date2 = new Date();
				report.setStage("文件处理总结");
				report.setReportType(1);
				report.setBegTime(date1);
				report.setEndTime(date2);
				if (parseOk) {
					report.setFinishState(DataParseStatus.Succeded.toString());
					report.setAttMsg("成功完成文件（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
					sucCnt++;
				} else {
					report.setFinishState(DataParseStatus.Failall.toString());
					report.setAttMsg("失败完成文件（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
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
				report.setAttMsg("文件解析出错（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
				addJobReport(report);
				log.error(msg);
			}
		}

		if (sucCnt > 0) {
			// status.setJobRunningStatus(JobRunningStatus.Succeded);
			status.setJobState(JobState.Finished);
			status.setUpdateTime(new Date());
		} else {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
		}

		if (sucCnt == allMrFiles.size()) {
			// 全部成功
			sql = "update rno_data_collect_rec set file_status='" + DataParseStatus.Succeded.toString()
					+ "' where data_collect_id=" + dataRec.getDataCollectId();
		} else {
			if (sucCnt > 0) {
				sql = "update rno_data_collect_rec set file_status='" + DataParseStatus.Failpartly.toString()
						+ "' where data_collect_id=" + dataRec.getDataCollectId();
			} else {
				sql = "update rno_data_collect_rec set file_status='" + DataParseStatus.Failall.toString()
						+ "' where data_collect_id=" + dataRec.getDataCollectId();
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

	private boolean parseMr(JobReport report, String tmpFileName, File f, Connection hiveConn, long cityId) {
		// 重新读取，因为其为公用变量，为了及时复位更新数据：在多个文件中存在此问题
		long st = System.currentTimeMillis();
		String msg = "";
		String stage = "";
		Date date1 = new Date();
		DateUtil dateUtil = new DateUtil();// 日期格式转换类
		Statement stmt = null;
		String charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		stage = "检查文件编码";
		if (charset == null) {
			msg = "文件：" + tmpFileName + ":无法识别的文件编码！";
			log.error(msg);
			report.setSystemFields(stage, date1, new Date(), DataParseStatus.Failall.toString(), msg);
			addJobReport(report);
			return false;
		}

		BufferedReader reader = null;
		date1 = new Date();
		try {
			// 读取文件
			stage = "读取文件";
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
			// 获取statement对象实例
			stage = "获取statement对象实例";
			stmt = hiveConn.createStatement();
			String[] sps = new String[1];
			String line = "";
			stage = "读取标题头";
			do {
				line = reader.readLine();
				if (line == null) {
					sps = new String[] {};
					break;
				}
				sps = line.split(",|\t");
			} while (sps == null || sps.length < 10);
			// 读取到标题头
			int fieldCnt = sps.length;
			log.debug("MR文件：" + tmpFileName + ",title 为：" + line + ",有" + fieldCnt + "标题");

			// 通过XML文件定义标题与EXCEL文件标题匹配，供13个XML文件
			// 判断标题是否满足要求
			stage = "检查文件标题";
			List<Table> tables = readTableCfgFromXml(tablesCfgFile);
			Table table = judeTitle(sps, tables);
			if (table == null) {
				msg = "检查MROMRS文件：" + tmpFileName + ",在预定义的XML文件中匹配不到与之相对应的标题信息:" + line;
				log.error(msg);
				report.setFields(stage, date1, new Date(), DataParseStatus.Failall.toString(), msg);
				addJobReport(report);
				return false;
			}
			Map<String, Integer> mrDataNumMap = new HashMap<String, Integer>();// 将某日期对应的记录数量迭加缓存起来
			String meaDate = "";// 测量日期

			long sTime = System.currentTimeMillis();
			// 逐行读取数据(真正读取数据)
			int executeCnt = 0;
			long totalDataNum = 0;
			// 设定一个字符串占位符%s
			// 新版本 hive insert into 格式为 insert into table [partition ()] values () [,()]
			// values 的结构需要与表结构一一对应
			String insertTableSql = "insert into " + table.getName() + " partition (year='%s',month='%s') values";
			StringBuffer sqlBuffer = new StringBuffer(insertTableSql);
			stage = "逐行解析文件";
			do {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				sps = line.split(",");
				if (sps.length != fieldCnt) {
					continue;
				}
				totalDataNum++;
				/* 构造hive ----------SQL语句 */
				String ws = "";
				for (ColumnToTitle c : table.getFields()) {
					if ("cityid".equals(c.getName())) {
						ws += "'" + cityId + "',";
					}
					if (c.getIndex() >= 0) {
						if ("meatime".equals(c.getName())) {
							meaDate = sps[c.getIndex()];
							meaDate = dateUtil.format_yyyyMMdd(dateUtil.parseDateArbitrary(meaDate));
							if (mrDataNumMap.get(meaDate) == null) {
								mrDataNumMap.put(meaDate, 1);
							} else {
								mrDataNumMap.put(meaDate, mrDataNumMap.get(meaDate) + 1);
							}
						}
						// 只对出现了的进行组sql
						ws += "'" + sps[c.getIndex()].trim() + "',";
					}
				}
				if (StringUtils.isBlank(ws)) {
					continue;
				}
				sqlBuffer.append("(" + ws.substring(0, ws.length() - 1) + "),");

				executeCnt++;
				if (executeCnt > 10000) {
					log.info("读取10000行数据耗时：" + (System.currentTimeMillis() - sTime) / 1000 + "s");
					sTime = System.currentTimeMillis();
					// 每5000行执行一次
					submitToHive(sqlBuffer, dateUtil.parseDateArbitrary(meaDate), stmt);
					sqlBuffer.append(insertTableSql);// 再将sql语句头部加上
					log.info("存入HIVE耗时：" + (System.currentTimeMillis() - sTime) / 1000 + "s");
					sTime = System.currentTimeMillis();
					executeCnt = 0;
				}
			} while (!StringUtils.isBlank(line));
			if (executeCnt > 0) {
				log.info("读取" + executeCnt + "行数据耗时：" + (System.currentTimeMillis() - sTime) / 1000 + "s");
				sTime = System.currentTimeMillis();
				// 每5000行执行一次
				submitToHive(sqlBuffer, dateUtil.parseDateArbitrary(meaDate), stmt);
				log.info("存入HIVE耗时：" + (System.currentTimeMillis() - sTime) / 1000 + "s");
				sTime = System.currentTimeMillis();
				executeCnt = 0;
			}
			msg = "mr数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据";
			log.debug(msg);
			report.setSystemFields(stage, date1, new Date(), DataParseStatus.Succeded.toString(), msg);
			// ----一下进行数据处理----//
			stage = "生成MROMRS描述信息";
			msg = "文件：" + tmpFileName;
			date1 = new Date();
			// 生成描述信息
			// cityid,meatime,datatype,recordcnt,createtime,modtime
			String descSql = "insert into rno_4g_mromrs_desc values";
			for (Entry<String, Integer> entry : mrDataNumMap.entrySet()) {
				String createime = dateUtil.format_yyyyMMddHHmmss(new Date());
				descSql += "('" + cityId + "','" + entry.getKey() + "','" + table.getName().substring(7) + "','"
						+ entry.getValue() + "','" + createime + "','" + createime + "'),";
			}
			stmt.execute(descSql.substring(0, descSql.length() - 1));
			report.setSystemFields(stage, date1, new Date(), DataParseStatus.Succeded.toString(), msg);
			addJobReport(report);
			log.info("退出对MROMRS文件：" + tmpFileName + "的解析，耗时：" + (System.currentTimeMillis() - st) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
			report.setSystemFields(stage, date1, new Date(), DataParseStatus.Failall.toString(), msg);
			addJobReport(report);
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
				}
			}
		}
		return true;
	}

	private void submitToHive(StringBuffer sqlBuffer, Date meaDate, Statement stmt) {
		Calendar now = Calendar.getInstance();
		now.setTime(meaDate);
		String sqlString = sqlBuffer.substring(0, sqlBuffer.length() - 1);
		sqlString = String.format(sqlString, now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1);
		sqlString = sqlString.replaceAll("\n", "");
		// System.out.println("sqlString.sql===================" + sqlString);
		try {
			stmt.execute(sqlString);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		sqlBuffer.setLength(0);// 清空该字符串
	}

	/**
	 * 
	 * @title 判断标题是否满足要求
	 * @param sps
	 * @param dbFieldsToTitles
	 * @return
	 * @author chao.xj
	 * @date 2015-10-19上午10:46:39
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private Table judeTitle(String[] sps, List<Table> tables) {
		// 判断标题的有效性
		for (Table table : tables) {
			findTitle(sps, table);
			if (table.isMeet()) {
				return table;
			}
		}
		return null;
	}

	private void findTitle(String[] sps, Table table) {
		int index = -1;
		// 是否发现某一标题
		for (String sp : sps) {
			index++;
			boolean find = false;
			for (ColumnToTitle dt : table.getFields()) {
				for (String dtf : dt.getTitles()) {
					if (dt.getMatchType() == 1) {
						if (StringUtils.equals(dtf, sp.trim())) {
							find = true;
							dt.setIndex(index);
							break;
						}
					} else if (dt.getMatchType() == 0) {
						if (StringUtils.startsWith(sp.trim(), dtf)) {
							find = true;
							dt.setIndex(index);
							break;
						}
					}
				}
				if (find) {
					break;
				}
			}
		}
	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		if (jobStatus == null) {
			return;
		}
		if (stmt != null) {
			try {
				String prog = jobStatus.getProgress();
				if (prog == null) {
					prog = "";
				}
				prog = prog.trim();
				if ("".equals(prog)) {
					prog = jobStatus.getJobState().getCode();
				}
				String str = "update RNO_DATA_COLLECT_REC set file_status='" + prog + "' where job_id="
						+ jobStatus.getJobId();
				stmt.executeUpdate(str);
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	@Override
	public void releaseRes() {
		try {
			if (file != null && file.exists()) {
				file.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		FileTool.deleteLocOrHdfsFileOrDir(filePath);
		super.releaseRes();
	}

	/**
	 * 
	 * @title 从xml配置 文件中读取 MR 数据库到excel的映射关系
	 * @param xmlfils
	 * @return
	 * @author chao.xj
	 * @date 2015-10-19上午10:07:18
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private List<Table> readTableCfgFromXml(String xmlfile) {
		InputStream in = null;
		try {
			in = new FileInputStream(new File(G4MroMrsParserJobRunnable.class.getResource(xmlfile).getPath()));
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			XMLReader reader = parser.getXMLReader();
			TableHandler handler = new TableHandler();
			reader.setContentHandler(handler);
			reader.parse(new InputSource(in));
			return handler.getTables();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private class TableHandler extends DefaultHandler {
		private List<Table> tables = new ArrayList<Table>();
		private Table table = null;
		private boolean tName = false;
		private ColumnToTitle field = null;
		private boolean fName = false;
		private boolean fType = false;
		private boolean fEssential = false;
		private boolean fMatch = false;
		private boolean fExceltitle = false;

		public List<Table> getTables() {
			return tables;
		}

		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("table")) {
				table = new Table();
			} else if (qName.equalsIgnoreCase("field")) {
				field = new ColumnToTitle();
			} else if (qName.equalsIgnoreCase("name")) {
				if (field == null) {
					tName = true;
				} else {
					fName = true;
				}
			} else if (qName.equalsIgnoreCase("type")) {
				fType = true;
			} else if (qName.equalsIgnoreCase("essential")) {
				fEssential = true;
			} else if (qName.equalsIgnoreCase("match")) {
				fMatch = true;
			} else if (qName.equalsIgnoreCase("exceltitle")) {
				fExceltitle = true;
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (tName) {
				table.setName(new String(ch, start, length));
				tName = false;// 解析完后，必须关闭掉；因为解析到下一个元素Author时characters还会被执行，如果不关闭掉会首先执行进来。
			} else if (fName) {
				field.setName(new String(ch, start, length));
				fName = false;
			} else if (fType) {
				field.setDbType(new String(ch, start, length));
				fType = false;
			} else if (fEssential) {
				field.setMandatory("1".equals(new String(ch, start, length)));
				fEssential = false;
			} else if (fMatch) {
				field.setMatchType(Integer.parseInt(new String(ch, start, length)));
				fMatch = false;
			} else if (fExceltitle) {
				field.setTitles(Arrays.asList(new String(ch, start, length).split(",")));
				fExceltitle = false;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (qName.equalsIgnoreCase("table")) {
				tables.add(table);
				field = null;
			} else if (qName.equalsIgnoreCase("field")) {
				table.addField(field);
			}
		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
		}

	}

	private class ColumnToTitle {
		private String name;
		private String dbType;// 类型，Number，String,Date
		private boolean isMandatory = true;// 是否强制要求出现
		private int matchType;// 0：模糊匹配，1：精确匹配
		private List<String> titles = new ArrayList<String>();

		private int index = -1;// 在文件中出现的位置，从0开始
		private int sqlIndex = -1;// 在sql语句中的位置

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setDbType(String dbType) {
			this.dbType = dbType;
		}

		public void setMandatory(boolean isMandatory) {
			this.isMandatory = isMandatory;
		}

		public int getMatchType() {
			return matchType;
		}

		public void setMatchType(int matchType) {
			this.matchType = matchType;
		}

		public List<String> getTitles() {
			return titles;
		}

		public void setTitles(List<String> titles) {
			this.titles = titles;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		@Override
		public String toString() {
			return "ColumnToTitle [name=" + name + ", dbType=" + dbType + ", isMandatory=" + isMandatory
					+ ", matchType=" + matchType + ", titles=" + titles + ", index=" + index + ", sqlIndex=" + sqlIndex
					+ "]";
		}

		public boolean isFind() {
			if (isMandatory && index < 0) {
				// 不符合要求
				return false;
			}
			return true;
		}
	}

	private class Table {
		private String name;
		private final List<ColumnToTitle> fields = new ArrayList<ColumnToTitle>();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<ColumnToTitle> getFields() {
			return fields;
		}

		public void addField(ColumnToTitle field) {
			fields.add(field);
		}

		@Override
		public String toString() {
			return "Table [name=" + name + ", fields=" + fields + "]";
		}

		public boolean isMeet() {
			for (ColumnToTitle field : fields) {
				if (!field.isFind()) {
					return false;
				}
			}
			return true;
		}
	}
}
