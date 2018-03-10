package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.DataParseProgress;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;
import com.iscreate.plat.tools.LatLngHelper;


public class LteNewCellParserJobRunnable extends DbParserBaseJobRunnable {

	private static Log log = LogFactory
			.getLog(LteNewCellParserJobRunnable.class);

	public LteNewCellParserJobRunnable() {
		super();
		super.setJobType("LTENEWCELLFILE");
	}

	// 构建insert语句
	private static String lteNewCellTable = "RNO_LTE_NEW_CELL";

	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connection,
			Statement stmt) {
		// TODO Auto-generated method stub
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

			return status;
		}
		// 准备
		// long cityId = dataRec.getCityId();
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
				connection.commit();
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
			connection.commit();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;
		int totalFileCnt = allFiles.size();
		int i = 0;

		// LTE新站数据字段对应标题
		Map<String, DBFieldToTitle> lteNewCellDbFieldsToTitles = readDbToTitleCfgFromXml("NewCellToTitles.xml");

		for (File f : allFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
				date1 = new Date();
				parseOk = parseCsv(this, report, dataRec, tmpFileName, f,
						connection, stmt, baseDateStr,
						lteNewCellDbFieldsToTitles);
				i++;
				date2 = new Date();
				report.setStage("文件处理总结");
				report.setReportType(1);
				if (parseOk) {
					report.setFinishState(DataParseStatus.Succeded.toString());
				} else {
					report.setFinishState(DataParseStatus.Failall.toString());
				}
				report.setBegTime(date1);
				report.setEndTime(date2);
				if (parseOk) {
					List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
					list = lteNcellMatch(stmt);
					Date today = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat(
							"yyyyMMddHHmmssSSS");
					
					String p = LteNewCellParserJobRunnable.class.getResource("/").toString();
					String fPath = p.substring(5,p.indexOf("WEB-INF")) + "op/rno/download/" ;
					System.out.println(fPath);
					
					String filePath = "";
					String FileName = sdf.format(today) + "_LTE新站邻区数据.csv";
					filePath = fPath + FileName;
					
					File file = new File(filePath);
						boolean result = createCsvFile(file, list);
						if (result) {
							report.setAttMsg(FileName);
							
							sucCnt++;
						} else {
							report.setAttMsg("失败完成文件:"
									+ tmpFileName);
						}
					}else{
						report.setAttMsg("失败完成文件:"
								+ tmpFileName);
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
				report.setAttMsg("文件解析出错:"
						+ tmpFileName);
				addJobReport(report);
				log.error(msg);
			}finally{
				sql = "truncate table "+lteNewCellTable;
				try {
					stmt.executeUpdate(sql);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		if (sucCnt > 0) {
			status.setJobState(JobState.Finished);
			status.setUpdateTime(new Date());
		} else {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
		}

		if (sucCnt == allFiles.size()) {
			// 全部成功
			sql = "update rno_data_collect_rec set file_status='"
					+ DataParseStatus.Succeded.toString()
					+ "' where data_collect_id=" + dataRec.getDataCollectId();
		} else {
			if (sucCnt > 0) {
				sql = "update rno_data_collect_rec set file_status='"
						+ DataParseStatus.Failpartly.toString()
						+ "' where data_collect_id="
						+ dataRec.getDataCollectId();
			} else {
				sql = "update rno_data_collect_rec set file_status='"
						+ DataParseStatus.Failall.toString()
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

	/**
	 * 
	 * @title 从xml配置 文件中读取lte新站 数据库到excel的映射关系
	 * @param proFile
	 * @return
	 * @author li.tf
	 * @date 2015-10-13下午15:13:42
	 * @company 怡创科技
	 * @version 2.0
	 */
	public static Map<String, DBFieldToTitle> readDbToTitleCfgFromXml(
			String proFile) {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(
					LteNewCellParserJobRunnable.class.getResource(proFile)
							.getPath()));
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

	private boolean parseCsv(JobRunnable jobWorker, JobReport report,
			RnoDataCollectRec dataRec, String tmpFileName, File f,
			Connection connection, Statement stmt, String baseDateStr,
			Map<String, DBFieldToTitle> cellDbFieldsToTitles) {

		String spName = "everyStartlte" + System.currentTimeMillis();
		log.debug("f path:" + f.getAbsolutePath());
		Savepoint savepoint = null;
		try {
			savepoint = connection.setSavepoint(spName);
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		PreparedStatement insertCellTStmt = null;
		long st = System.currentTimeMillis();

		Date date1 = new Date();
		Date date2 = null;
		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		System.out.println(charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			date2 = new Date();
			report.setSystemFields("检查文件编码", date1, date2,
					DataParseStatus.Failall.toString(), "文件：" + tmpFileName
							+ ":无法识别的文件编码！");
			addJobReport(report);
			try {
				connection.releaseSavepoint(savepoint);
			} catch (SQLException e) {
				e.printStackTrace();
			}
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

		try {
			String[] sps = new String[1];
			do {
				line = reader.readLine();
				if (line == null) {
					sps = new String[] {};
					break;
				}
				sps = line.split(",");
			} while (sps == null || sps.length < 8);
			// 读取到标题头：标题头的全部数量
			int fieldCnt = sps.length;
			log.debug("文件：" + tmpFileName + ",title 为：" + line + ",有"
					+ fieldCnt + "标题");

			// 判断标题的有效性
			Map<Integer, String> cellPois = new HashMap<Integer, String>();
			// Map<Integer, String> ncellPois = new HashMap<Integer, String>();
			int index = -1;
			boolean find = false;
            int num = 0;
			// 专门记录邻区位置
			for (String sp : sps) {
				// log.debug("sp==" + sp);
				index++;
				find = false;
				for (DBFieldToTitle dt : cellDbFieldsToTitles.values()) {
					// log.debug("-----dt==" + dt);
					for (String dtf : dt.titles) {
						if (dt.matchType == 1) {
							if (StringUtils.equals(dtf, sp)) {
								find = true;
								dt.setIndex(index);
								cellPois.put(index, dt.dbField);// 快速记录标题位置
								break;
							}
						} else if (dt.matchType == 0) {
							// log.debug("--match type=0.dtf=" + dtf
							// + ",sp=" + sp);
							if (StringUtils.startsWith(sp, dtf)) {
								// log.debug("-----find " + sp + "->"
								// + dt);
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
				if ((dt.isMandatory && dt.index < 0) ||sps.length != cellDbFieldsToTitles.size()||num!=cellDbFieldsToTitles.size()) {
					msg += "[" + dt.dbField + "在文件中未找到对应的数据]\r\n";
				}
			}
			if (!StringUtils.isBlank(msg)) {
				log.error("检查文件：" + tmpFileName + "的标题头有问题！" + msg);
				date2 = new Date();
				report.setFields("检查文件标题", date1, date2,
						DataParseStatus.Failall.toString(), msg);
				addJobReport(report);
				try {
					connection.releaseSavepoint(savepoint);
				} catch (Exception e) {
					log.error("连接释放安全点出错！");
					return false;
				}
				return false;
			}
			// 拼装sql
			String insertCellTableSql = "insert into " + lteNewCellTable + " (";
			String ws = "";
			index = 1;
			int cellTotalMark = 0;// 记录问号的总数
			for (String d : cellDbFieldsToTitles.keySet()) {
				if (cellDbFieldsToTitles.get(d).index >= 0) {
					cellTotalMark++;
					// log.debug("dbfield:"+d+"----index:"+index+"-------totalMark:"+totalMark);
					// 只对出现了的进行组sql
					cellDbFieldsToTitles.get(d).sqlIndex = index++;// 在数据库中的位置
					if (index < 9) {
						insertCellTableSql += d + ",";
					} else {
						insertCellTableSql += d + ",CELL_ID";
					}
					ws += "?,";
				}
			}
			if (StringUtils.isBlank(ws)) {
				log.error("没有有效标题数据！");
				return false;
			}
			// 此处也要增加相应的位置索引
			// insertCellEnginTableSql = insertCellEnginTableSql.substring(0,
			// insertCellEnginTableSql.length()-1)+") values ( " +
			// ws.substring(0, ws.length()-1) + " )";
			insertCellTableSql = insertCellTableSql + ") values ( " + ws + "?)";
			log.debug("insertCellTableSql  sql=" + insertCellTableSql);

			try {
				insertCellTStmt = connection
						.prepareStatement(insertCellTableSql);
			} catch (Exception e) {
				msg = "准备lte新站数据插入的prepareStatement失败";
				log.error("准备lte新站数据插入的prepareStatement失败！sql="
						+ insertCellTableSql);
				e.printStackTrace();
				connection.releaseSavepoint(savepoint);
				return false;
			}

			// 逐行读取数据
			int executeCnt = 0;
			int addLineCnt = 0;
			boolean handleLineOkForCell = false;
			long totalDataNum = 0;
			DateUtil dateUtil = new DateUtil();

			long descId = 0;
			long length = 0;
			List<String> lteCellIdList = new ArrayList<String>();
			lteCellIdList.add("");
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

				handleLineOkForCell = handleLine(sps, fieldCnt, cellPois,
						cellDbFieldsToTitles, insertCellTStmt, dateUtil,
						baseDateStr, lteCellIdList);

				if (handleLineOkForCell == true) {
					executeCnt++;
					addLineCnt++;
					/* if(addLineCnt==1){ */
					descId = getNextSeqValue("SEQ_" + lteNewCellTable,
							connection);
					/* } */
					insertCellTStmt.setLong(cellTotalMark + 1, descId);
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
						insertCellTStmt.clearBatch();
						executeCnt = 0;
					} catch (SQLException e) {
						connection.rollback(savepoint);
						e.printStackTrace();
					}
				}
			} while (!StringUtils.isBlank(line));
			// 执行
			if (executeCnt > 0) {
				insertCellTStmt.executeBatch();
			}

			log.debug("lte新站数据文件：" + tmpFileName + "共有：" + totalDataNum
					+ "行记录数据,共入库=" + addLineCnt + "行记录");

		} catch (Exception e) {
			e.printStackTrace();
			try {
				connection.rollback(savepoint);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
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

	/**
	 * 
	 * @title 处理每一行数据
	 * @param sps
	 * @param expectFieldCnt
	 * @param pois
	 * @param dbFtts
	 * @param insertTempStatement
	 * @param dateUtil
	 * @param areaName
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2014-10-17下午3:44:05
	 * @company 怡创科技
	 * @version 1.2
	 */
	private boolean handleLine(String[] sps, int expectFieldCnt,
			Map<Integer, String> pois, Map<String, DBFieldToTitle> dbFtts,
			PreparedStatement insertStatement, DateUtil dateUtil,
			String baseDateStr, List<String> lteCellIdList) {
		// log.debug("handleHwNcsLine--sps="+sps+",expectFieldCnt="+expectFieldCnt+",pois="+pois+",dbFtts="+dbFtts);

		if (sps == null || sps.length != expectFieldCnt
				|| lteCellIdList.contains(sps[1])) {
			return false;
		}
		for(int m = 0; m<sps.length;m++){
			if(sps[m].equals("")||sps[m]==null){
				return false;
			}
		}
		lteCellIdList.add(sps[1]);
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
				if (!dateUtil.isSameDay(
						dateUtil.parseDateArbitrary(baseDateStr),
						dateUtil.parseDateArbitrary(sps[i]))) {
					return false;
				}
			}
			if (dt != null) {
				setIntoPreStmt(insertStatement, dt, sps[i], dateUtil);
			}
		}
		return true;
	}

	/**
	 * 
	 * @title 对预处理sql语句进行赋值处理
	 * @param pstmt
	 * @param dt
	 * @param val
	 * @param dateUtil
	 * @author chao.xj
	 * @date 2014-10-16下午6:14:37
	 * @company 怡创科技
	 * @version 1.2
	 */
	private void setIntoPreStmt(PreparedStatement pstmt, DBFieldToTitle dt,
			String val, DateUtil dateUtil) {
		String type = dt.getDbType();
		int index = dt.sqlIndex;
		if (index < 0) {
			log.error(dt + "在sql插入语句中，没有相应的位置！");
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
		} else if (StringUtils.equalsIgnoreCase("double", type)) {
			if (!StringUtils.isBlank(val)) {
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

	protected static long getNextSeqValue(String seq, Connection connection) {
		long descId = -1;
		String vsql = "select " + seq + ".NEXTVAL as id from dual";
		Statement pstmt = null;
		try {
			pstmt = connection.createStatement();
		} catch (SQLException e3) {
			e3.printStackTrace();
			return -1;
		}
		ResultSet rs = null;
		try {
			rs = pstmt.executeQuery(vsql);
			rs.next();
			descId = rs.getLong(1);
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			pstmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return descId;
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
				String str = "update RNO_DATA_COLLECT_REC set file_status='"
						+ prog + "' where job_id=" + jobStatus.getJobId();
				stmt.executeUpdate(str);
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}
	}

	private File file = null;
	private String filePath = null;

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
			if (!StringUtils.isBlank(t)) {
				titles.add(t);
			}
		}
	}

	protected static List<String> getLteCellId(Connection connection) {
		List<String> list = new ArrayList<String>();
		String lteCellId = null;
		String vsql = "select LABEL from cell";
		Statement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = connection.createStatement();
			rs = pstmt.executeQuery(vsql);
			while (rs.next()) {
				lteCellId = rs.getString(1);
				list.add(lteCellId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return list;
	}

	protected static boolean combineDb(Connection connection) {

		String insertCellTableSql = "MERGE INTO "
				+ "   RNO_LTE_NEW_CELL_TEMP t "
				+ "	USING(SELECT "
				+ "   RLNC.CELL_NAME AS CELL_NAME,"
				+ "   RLNC.BUSINESS_CELL_ID AS BUSINESS_CELL_ID,"
				+ "   RLNC.SAMEBAND_NCELLNUM AS SAMEBAND_NCELLNUM,"
				+ "   RLNC.NOBAND_NCELLNUM AS NOBAND_NCELLNUM,"
				+ "   RLNC.AZIMUTH AS AZIMUTH,"
				+ "   RLNC.LONGITUDE AS LONGITUDE,"
				+ "   RLNC.LATITUDE AS LATITUDE,"
				+ "   RLNC.COVER_TYPE AS COVER_TYPE FROM RNO_LTE_NEW_CELL RLNC"
				+ " UNION ALL"
				+ " SELECT"
				+ "   C.CELL_NAME AS CELL_NAME,"
				+ "   C.BUSINESS_CELL_ID AS BUSINESS_CELL_ID,"
				+ "   NULL AS SAMEBAND_NCELLNUM,"
				+ "   NULL AS NOBAND_NCELLNUM,"
				+ "   C.AZIMUTH AS AZIMUTH,"
				+ "   C.LONGITUDE AS LONGIITUDE,"
				+ "   C.LATITUDE AS LATITUDE,"
				+ "   C.COVER_TYPE AS COVER_TYPE FROM RNO_LTE_CELL C"
				+ "   ,RNO_LTE_NEW_CELL RLNC WHERE C.BUSINESS_CELL_ID != RLNC.BUSINESS_CELL_ID)A"
				+ "	ON (t.BUSINESS_CELL_ID = A.BUSINESS_CELL_ID)  "
				+ "	WHEN MATCHED THEN" + "	UPDATE SET "
				+ "   t.CELL_NAME = A.CELL_NAME," + "   t.AZIMUTH = A.AZIMUTH,"
				+ "   t.LONGITUDE = A.LONGITUDE,"
				+ "   t.LATITUDE = A.LATITUDE,"
				+ "   t.COVER_TYPE = A.COVER_TYPE" + "	WHEN NOT MATCHED THEN"
				+ "	INSERT (t.CELL_NAME,t.BUSINESS_CELL_ID,t.AZIMUTH,"
				+ "   t.LONGITUDE,t.LATITUDE,t.COVER_TYPE)"
				+ " VALUES(A.CELL_NAME,A.BUSINESS_CELL_ID,A.AZIMUTH,"
				+ "   A.LONGITUDE,A.LATITUDE,A.COVER_TYPE)";
		Statement pstmt = null;
		try {
			pstmt = connection.createStatement();
			pstmt.executeUpdate(insertCellTableSql);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {

			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		return true;
	}

	public List<Map<String, Object>> lteNcellMatch(Statement stmt) {

		List<Map<String, Object>> newCellData = getNewLteCellDataImpl(stmt);
		List<Map<String, Object>> cellData = lteNcellMatchImpl(stmt);
		/*
		 * Map<String, List<Map<String, Object>>> map = new TreeMap<String,
		 * List<Map<String, Object>>>();
		 */
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
      if(newCellData != null && newCellData.size()>0){
		if (cellData != null && cellData.size() > 0) {
			for (Map<String, Object> m : newCellData) {
				List<Map<String, Object>> comCoverCellData = new ArrayList<Map<String, Object>>();
				List<Map<String, Object>> uncomCoverCellData = new ArrayList<Map<String, Object>>();
				for (Map<String, Object> m1 : cellData) {
					if (!m.get("BUSINESS_CELL_ID").toString()
							.equals((m1).get("BUSINESS_CELL_ID").toString())) {

						int coverType = 0;
						int ncoverType = 0;
						// 两站距离
						double distance = LatLngHelper
								.Distance(Double.parseDouble(m.get("LONGITUDE")
										.toString()), Double.parseDouble(m.get(
										"LATITUDE").toString()), Double
										.parseDouble(m1.get("LONGITUDE")
												.toString()), Double
										.parseDouble(m1.get("LATITUDE")
												.toString()));
						// 两站南北方向距离
						double ydistance = LatLngHelper
								.Distance(Double.parseDouble(m.get("LONGITUDE")
										.toString()), Double.parseDouble(m.get(
										"LATITUDE").toString()), Double
										.parseDouble(m.get("LONGITUDE")
												.toString()), Double
										.parseDouble(m1.get("LATITUDE")
												.toString()));
						// 小区夹角
						double cellAngle = calAngle(ydistance, distance, m, m1);
						// 邻区夹角
						double ncellAngle = calAngle(ydistance, distance, m1, m);

						if (m.get("COVER_TYPE") == null
								|| m.get("COVER_TYPE").toString()
										.equals("室外覆盖")
								|| m.get("COVER_TYPE").toString().toUpperCase()
										.equals("OUTDOOR")) {
							coverType = 1;
						}

						if (m1.get("COVER_TYPE") == null
								|| m1.get("COVER_TYPE").toString()
										.equals("室外覆盖")
								|| m1.get("COVER_TYPE").toString()
										.toUpperCase().equals("OUTDOOR")) {
							ncoverType = 1;
						}

						if (coverType == 1 && ncoverType == 1) {

							if (distance <= 300) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								comCoverCellData.add(m1);
							}
							if (distance > 300 && distance <= 500) {
								if (cellAngle <= 60 || ncellAngle <= 60) {
									m1.put("CELL_BUSINESS_CELL_ID",
											m.get("BUSINESS_CELL_ID")
													.toString());
									m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
											.toString());
									m1.put("CELL_COVER_TYPE",
											m.get("COVER_TYPE").toString());
									m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
											.toString());
									m1.put("CELL_LATITUDE", m.get("LATITUDE")
											.toString());
									m1.put("DISTANCE", distance);
									m1.put("CELLANGLE", cellAngle);
									m1.put("NCELLANGLE", ncellAngle);
									comCoverCellData.add(m1);
								}
							}
							if (distance > 500 && distance <= 1000) {
								if (cellAngle <= 60 && ncellAngle <= 60) {
									m1.put("CELL_BUSINESS_CELL_ID",
											m.get("BUSINESS_CELL_ID")
													.toString());
									m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
											.toString());
									m1.put("CELL_COVER_TYPE",
											m.get("COVER_TYPE").toString());
									m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
											.toString());
									m1.put("CELL_LATITUDE", m.get("LATITUDE")
											.toString());
									m1.put("DISTANCE", distance);
									m1.put("CELLANGLE", cellAngle);
									m1.put("NCELLANGLE", ncellAngle);
									comCoverCellData.add(m1);
								}
							}
							if (distance > 1000 && distance <= 2000) {
								if (cellAngle <= 30 && ncellAngle <= 30) {
									m1.put("CELL_BUSINESS_CELL_ID",
											m.get("BUSINESS_CELL_ID")
													.toString());
									m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
											.toString());
									m1.put("CELL_COVER_TYPE",
											m.get("COVER_TYPE").toString());
									m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
											.toString());
									m1.put("CELL_LATITUDE", m.get("LATITUDE")
											.toString());
									m1.put("DISTANCE", distance);
									m1.put("CELLANGLE", cellAngle);
									m1.put("NCELLANGLE", ncellAngle);
									comCoverCellData.add(m1);
								}
							}
						}

						if (coverType == 0 && ncoverType == 0) {
							if (distance <= 100) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								comCoverCellData.add(m1);
							}
						}

						if (coverType == 0 && ncoverType == 1) {
							if (distance <= 200) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
							if (distance > 200 && distance <= 400
									&& ncellAngle <= 60) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
							if (distance > 400 && distance <= 600
									&& ncellAngle <= 30) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
						}

						if (coverType == 1 && ncoverType == 0) {
							if (distance <= 200) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
							if (distance > 200 && distance <= 400
									&& cellAngle <= 60) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
							if (distance > 400 && distance <= 600
									&& cellAngle <= 30) {
								m1.put("CELL_BUSINESS_CELL_ID",
										m.get("BUSINESS_CELL_ID").toString());
								m1.put("CELL_AZIMUTH", m.get("AZIMUTH")
										.toString());
								m1.put("CELL_COVER_TYPE", m.get("COVER_TYPE")
										.toString());
								m1.put("CELL_LONGITUDE", m.get("LONGITUDE")
										.toString());
								m1.put("CELL_LATITUDE", m.get("LATITUDE")
										.toString());
								m1.put("DISTANCE", distance);
								m1.put("CELLANGLE", cellAngle);
								m1.put("NCELLANGLE", ncellAngle);
								uncomCoverCellData.add(m1);
							}
						}
					}
				}

				if (comCoverCellData != null) {  
					List<Map<String, Object>> listMap1 = new ArrayList<Map<String, Object>>();
				     Set<Map> setMap = new HashSet<Map>();  
				     for(Map<String,Object> map1 : comCoverCellData){  
				         if(setMap.add(map1)){  
				             listMap1.add(map1);  
				         }  
				     } 
				     
				     Collections.sort(listMap1, new Comparator<Map<String,Object>>(){  
				         public int compare(Map<String,Object> o1,Map<String,Object> o2){ 
				        	 int result = 0;
				        Double d1 = Double.parseDouble(o1.get("DISTANCE").toString());
				        Double d2 = Double.parseDouble(o2.get("DISTANCE").toString());
				        Double d3 = Double.parseDouble(o1.get("CELLANGLE").toString())+Double.parseDouble(o1.get("NCELLANGLE").toString());
				        Double d4 = Double.parseDouble(o2.get("CELLANGLE").toString())+Double.parseDouble(o2.get("NCELLANGLE").toString());
				        result = d1.compareTo(d2);
				        if(result==0){
				        	result = d3.compareTo(d4);
				        	/*System.out.println(result);*/
				        }
				             return result;  
				         }  
				     });  
				     if(Integer.parseInt(m.get("SAMEBAND_NCELLNUM").toString())<listMap1.size()){
				    	 for(int z = 0; z<Integer.parseInt(m.get("SAMEBAND_NCELLNUM").toString());z++){
					    	 list.add(listMap1.get(z));
					     }
				     }else{
				    	 for(Map<String,Object> ma1 : listMap1){
					    	 list.add(ma1);
					     }
				     }
				}

				if (uncomCoverCellData != null) {
					List<Map<String, Object>> listMap2 = new ArrayList<Map<String, Object>>();
					Set<Map> setMap2 = new HashSet<Map>();  
				     for(Map<String,Object> map2 : uncomCoverCellData){  
				         if(setMap2.add(map2)){  
				             listMap2.add(map2);  
				         }  
				     } 
				     
				     Collections.sort(listMap2, new Comparator<Map<String,Object>>(){  
				         public int compare(Map<String,Object> o3,Map<String,Object> o4){ 
				        	 int result = 0;
				        Double d5 = Double.parseDouble(o3.get("DISTANCE").toString());
				        Double d6 = Double.parseDouble(o4.get("DISTANCE").toString());
				        Double d7 = Double.parseDouble(o3.get("CELLANGLE").toString())+Double.parseDouble(o3.get("NCELLANGLE").toString());
				        Double d8 = Double.parseDouble(o4.get("CELLANGLE").toString())+Double.parseDouble(o4.get("NCELLANGLE").toString());
				        result = d5.compareTo(d6);
				        if(result==0){
				        	result = d7.compareTo(d8);
				        	/*System.out.println(result);*/
				        }
				             return result;  
				         }  
				     });  
				     if(Integer.parseInt(m.get("NOBAND_NCELLNUM").toString())<listMap2.size()){
				    	 for(int z = 0; z<Integer.parseInt(m.get("NOBAND_NCELLNUM").toString());z++){
					    	 list.add(listMap2.get(z));
					     }
				     }else{
				    	 for(Map<String,Object> ma2 : listMap2){
					    	 list.add(ma2);
					     }
				     }
				 
				}

			}
		}
      }
		/* System.out.println("总结果："+list); */
		return list;
	}

	public List<Map<String, Object>> lteNcellMatchImpl(Statement stmt) {

		String sql = "SELECT DISTINCT * from(select"
				+ "  RLNC.CELL_NAME AS CELL_NAME,"
				+ "  RLNC.BUSINESS_CELL_ID AS BUSINESS_CELL_ID,"
				+ "  RLNC.AZIMUTH AS AZIMUTH,"
				+ "  RLNC.LONGITUDE AS LONGITUDE,"
				+ "  RLNC.LATITUDE AS LATITUDE,"
				+ "  RLNC.COVER_TYPE AS COVER_TYPE FROM RNO_LTE_NEW_CELL RLNC"
				+ "   UNION ALL"
				+ "  SELECT "
				+ "  C.CELL_NAME AS CELL_NAME,"
				+ "  C.BUSINESS_CELL_ID AS BUSINESS_CELL_ID,"
				+ "  C.AZIMUTH AS AZIMUTH,"
				+ "  C.LONGITUDE AS LONGIITUDE,"
				+ "  C.LATITUDE AS LATITUDE,"
				+ "  C.COVER_TYPE AS COVER_TYPE FROM RNO_LTE_CELL C "
				+ "  ,RNO_LTE_NEW_CELL RLNC WHERE C.BUSINESS_CELL_ID != RLNC.BUSINESS_CELL_ID" 
				+ "  AND C.LONGITUDE IS NOT NULL AND C.LATITUDE IS NOT NULL AND C.AZIMUTH IS NOT NULL) ORDER BY BUSINESS_CELL_ID";
		List<Map<String, Object>> recs = RnoHelper.commonQuery(stmt, sql);
		return recs;
	}

	public List<Map<String, Object>> getNewLteCellDataImpl(Statement stmt) {

		String sql = "SELECT BUSINESS_CELL_ID,AZIMUTH,SAMEBAND_NCELLNUM,NOBAND_NCELLNUM,LONGITUDE,LATITUDE,COVER_TYPE "
				+ "FROM RNO_LTE_NEW_CELL ORDER BY BUSINESS_CELL_ID";
		List<Map<String, Object>> recs = RnoHelper.commonQuery(stmt, sql);
		return recs;
	}

	public double calAngle(double ydistance, double distance,
			Map<String, Object> m1, Map<String, Object> m2) {
		double lineAngle = 0.0;
		if (Double.parseDouble(m1.get("LONGITUDE").toString()) <= Double
				.parseDouble(m2.get("LONGITUDE").toString())
				&& Double.parseDouble(m1.get("LATITUDE").toString()) < Double
						.parseDouble(m2.get("LATITUDE").toString())) {
			lineAngle = Math.acos(ydistance / distance) * 180 / Math.PI;
		}
		if (Double.parseDouble(m1.get("LONGITUDE").toString()) < Double
				.parseDouble(m2.get("LONGITUDE").toString())
				&& Double.parseDouble(m1.get("LATITUDE").toString()) > Double
						.parseDouble(m2.get("LATITUDE").toString())) {
			lineAngle = Math.asin(ydistance / distance) * 180 / Math.PI + 90.0;
		}
		if (Double.parseDouble(m1.get("LONGITUDE").toString()) > Double
				.parseDouble(m2.get("LONGITUDE").toString())
				&& Double.parseDouble(m1.get("LATITUDE").toString()) > Double
						.parseDouble(m2.get("LATITUDE").toString())) {
			lineAngle = Math.acos(ydistance / distance) * 180 / Math.PI + 180.0;
		}
		if (Double.parseDouble(m1.get("LONGITUDE").toString()) > Double
				.parseDouble(m2.get("LONGITUDE").toString())
				&& Double.parseDouble(m1.get("LATITUDE").toString()) < Double
						.parseDouble(m2.get("LATITUDE").toString())) {
			lineAngle = Math.asin(ydistance / distance) * 180 / Math.PI + 270.0;
		}
		double Angle = Math.abs(Double
				.parseDouble(m1.get("AZIMUTH").toString()) - lineAngle);
		if (Angle > 180) {
			Angle = 360.0 - Angle;
		}
		return Angle;
	}

	public boolean createCsvFile(File file, List<Map<String, Object>> dataList) {
		boolean isSucess = false;

		FileOutputStream out = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		try {
			out = new FileOutputStream(file);
			osw = new OutputStreamWriter(out,"GBK");
			bw = new BufferedWriter(osw);
			List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
			Set<Map> setMap2 = new HashSet<Map>(); 
			if (dataList != null && !dataList.isEmpty()) {
		     for(Map<String,Object> map2 : dataList){  
		         if(setMap2.add(map2)){  
		             listMap.add(map2);  
		         }  
		     } 
			
				int i = 1;
				for (Map<String, Object> data : listMap) {
					/* bw.append(data).append("\r"); */
					if (i == 1) {
						bw.write("CELLID" + "," + "覆盖类型" + "," + "方位角" + ","
								+ "经度" + "," + "纬度" + "," + "NCELLID" + ","
								+ "N覆盖类型" + "," + "N方位角" + "," + "N经度" + ","
								+ "N纬度" + "," + "邻区距离" + "," + "本小区夹角" + ","
								+ "邻小区夹角" + "\r");
					}
					bw.write(data.get("CELL_BUSINESS_CELL_ID").toString() + ","
							+ data.get("CELL_COVER_TYPE").toString() + ","
							+ data.get("CELL_AZIMUTH").toString() + ","
							+ data.get("CELL_LONGITUDE").toString() + ","
							+ data.get("CELL_LATITUDE").toString() + ","
							+ data.get("BUSINESS_CELL_ID").toString() + ","
							+ data.get("COVER_TYPE").toString() + ","
							+ data.get("AZIMUTH").toString() + ","
							+ data.get("LONGITUDE").toString() + ","
							+ data.get("LATITUDE").toString() + ","
							+ data.get("DISTANCE").toString() + ","
							+ data.get("CELLANGLE").toString() + ","
							+ data.get("NCELLANGLE").toString() + "\r");
					i++;
				}
			}else{
				bw.write("CELLID" + "," + "覆盖类型" + "," + "方位角" + ","
						+ "经度" + "," + "纬度" + "," + "NCELLID" + ","
						+ "N覆盖类型" + "," + "N方位角" + "," + "N经度" + ","
						+ "N纬度" + "," + "邻区距离" + "," + "本小区夹角" + ","
						+ "邻小区夹角" + "\r");
			}
			isSucess = true;
		} catch (Exception e) {
			isSucess = false;
		} finally {
			if (bw != null) {
				try {
					bw.close();
					bw = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (osw != null) {
				try {
					osw.close();
					osw = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (out != null) {
				try {
					out.close();
					out = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return isSucess;
	}

	public static void main(String[] args) {
		/*
		 * String sp = "邻区1,邻区2,邻区3";
		 * System.out.println(StringUtils.startsWith("邻区3", sp));
		 * System.out.println(StringUtils.equals("邻区2", "邻区2"));
		 * System.out.println(StringUtils.startsWith("邻区1", "邻区"));
		 */
		/*
		 * LteNewCellParserJobRunnable ln = new LteNewCellParserJobRunnable();
		 * String path = ln.returnPath(); System.out.println(path);
		 * System.out.println(System.getProperty("user.dir"));
		 */
		/*
		 * ServletContextEvent event = new ServletContextEvent(application);
		 * String ProjectPath =
		 * event.getServletContext().getRealPath("/template");
		 */
	/*	System.out.println(LteNewCellParserJobRunnable.class.getResource("/"));
		System.out.println(System.getProperty("user.dir"));
		SessionService ss = SessionService.getInstance();
		System.out.println(ss);
		HttpSession hs = ss.getSession();
		System.out.println(hs);
		System.out.println(SessionService.getInstance().getSession().getServletContext().getRealPath(""));*/
		/*ActionContext ac = ActionContext.getContext();
		//HttpServletRequest request =(HttpServletRequest)ac.get(ServletActionContext.HTTP_REQUEST);
		
		HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();  */
		String FileName =  "_LTE新站邻区数据.csv";
		String fn = null;
		try {
			fn = new String(FileName.getBytes("ISO-8859-1"), "gb2312");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(FileName);
		/* File file = new File("/op/rno/download/"); */
		/* System.out.println(file); */
	}
}
