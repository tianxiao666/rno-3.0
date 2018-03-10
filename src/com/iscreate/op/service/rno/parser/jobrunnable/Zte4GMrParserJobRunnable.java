package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.iscreate.op.dao.rno.RnoMrAdjCellMatch;
import com.iscreate.op.dao.rno.RnoMrAdjCellMatchImpl;
import com.iscreate.op.dao.rno.RnoMrAdjCellMatchImpl.CellForMatch;
import com.iscreate.op.dao.rno.RnoMrAdjCellMatchImpl.CellLonLat;
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
import com.iscreate.op.service.rno.tool.DateUtil.DateFmt;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HBTable;
import com.iscreate.op.service.rno.tool.HadoopXml;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;
import com.iscreate.plat.tools.LatLngHelper;

public class Zte4GMrParserJobRunnable extends DbParserBaseJobRunnable {

	private static Log log = LogFactory.getLog(Zte4GMrParserJobRunnable.class);
	private RnoMrAdjCellMatch rnoMrAdjCellMatch = new RnoMrAdjCellMatchImpl();
	private StringBuffer cellBuffer = new StringBuffer();

	public Zte4GMrParserJobRunnable() {
		super();
		super.setJobType("ZTE4GMRFILE");
	}

	// 表及相关语句
	private static String insertTempTableSql = "";
	private static String LteCellTempTable = "RNO_LTE_IMPORT_MID";

	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connection, Statement stmt) {

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
		List<File> allMrFiles = new ArrayList<File>();// 将所有待处理的MR文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		Date date1 = new Date(), date2, cacheDate1;
		cacheDate1 = date1;// 缓存第一次进入runable的时间
		if (fileName.toLowerCase().endsWith(".zip")) {
			date1 = new Date();
			fromZip = true;
			// 压缩包
			log.info("上传的MR文件是一个压缩包。");

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
			log.info("上传的MR是一个普通文件。");
			allMrFiles.add(file);
		}

		if (allMrFiles.isEmpty()) {
			msg = "未上传有效的mr文件！zip包里不能再包含有文件夹！";
			log.error(msg);

			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("未上传有效的MR文件！注意zip包里不能再包含有文件夹！");
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

		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;
		boolean preParseOk = false;

		int totalFileCnt = allMrFiles.size();
		int i = 0;
		// 是否还需要验证标题头
		Map<String, DBFieldToTitle> dbFieldsToTitles = readDbToTitleCfgFromXml();
		for (File f : allMrFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}

				// 首先对文件进行预处理：插入临时表，更新该区域的LTE小区的频点及PCI信息
				date1 = new Date();
				preParseOk = preParseMr(this, report, tmpFileName, f, connection, stmt, cityId, dbFieldsToTitles);
				date2 = new Date();
				report.setStage("文件预处理总结");
				report.setReportType(1);
				report.setBegTime(date1);
				report.setEndTime(date2);
				if (preParseOk) {
					report.setFinishState(DataParseStatus.Succeded.toString());
					report.setAttMsg("成功完成文件（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
				} else {
					report.setFinishState(DataParseStatus.Failall.toString());
					report.setAttMsg("失败完成文件（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
				}
				addJobReport(report);

				if (preParseOk) {
					// 再次对文件进行处理：在更新该区域的LTE小区的频点及PCI信息基础上进行邻区匹配及数据入库工作
					date1 = new Date();
					log.debug("开始解释 MR 数据。");
					parseOk = parseMr(this, report, tmpFileName, f, connection, stmt, cityId, dbFieldsToTitles);
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
				}

			} catch (Exception e) {
				e.printStackTrace();
				date2 = new Date();
				msg = tmpFileName + "文件解析出错！";
				report.setStage("文件处理总结");
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setBegTime(date1);
				report.setEndTime(date2);
				report.setReportType(1);
				report.setAttMsg("文件解析出错（" + i + "/" + totalFileCnt + "）:" + tmpFileName);
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
		log.debug("更新结果状态 rno_data_collect_rec, sql:" + sql);
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		if (cellBuffer.length() > 0) {
			date2 = new Date();
			report.setStage("小区匹配汇总");
			report.setReportType(1);

			report.setBegTime(cacheDate1);
			report.setEndTime(date2);
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setAttMsg("在工参中无法匹配到的小区（数量 /" + cellBuffer.substring(0, cellBuffer.length() - 1).split(",").length
					+ "）:" + cellBuffer.substring(0, cellBuffer.length() - 1));
			addJobReport(report);
			cellBuffer.setLength(0);// 清空
		}

		// 清除邻区匹配数据
		rnoMrAdjCellMatch.clearMatchCellContext();
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * 对MR文件进行预处理：插入临时表，更新该区域的LTE小区的频点及PCI信息
	 * 
	 * @param jobWorker
	 * @param report
	 * @param tmpFileName
	 * @param f
	 * @param connection
	 * @param stmt
	 * @param cityId
	 * @param dbFieldsToTitles
	 * @return
	 */
	private boolean preParseMr(JobRunnable jobWorker, JobReport report, String tmpFileName, File f,
			Connection connection, Statement stmt, long cityId, Map<String, DBFieldToTitle> dbFieldsToTitles) {

		PreparedStatement insertTStmt = null;
		long st = System.currentTimeMillis();
		String spName = "everyStartZte" + System.currentTimeMillis();
		Savepoint savepoint = null;
		try {
			savepoint = connection.setSavepoint(spName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		Date date1 = new Date();
		Date date2 = null;
		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			date2 = new Date();
			report.setSystemFields("检查文件编码", date1, date2, DataParseStatus.Failall.toString(), "文件：" + tmpFileName
					+ ":无法识别的文件编码！");
			addJobReport(report);
			return false;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
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
				sps = line.split(",|\t");
			} while (sps == null || sps.length < 10);

			// 读取到标题头
			int fieldCnt = sps.length;
			log.debug("中兴MR文件：" + tmpFileName + ", 标题行为：" + line + ", 有" + fieldCnt + "个字段。");

			// 判断标题的有效性
			Map<Integer, String> pois = new HashMap<Integer, String>();
			int index = -1;
			boolean find = false;
			for (String sp : sps) {
				index++;
				find = false;
				for (DBFieldToTitle dt : dbFieldsToTitles.values()) {
					for (String dtf : dt.titles) {
						if (dt.matchType == 1) {
							if (StringUtils.equals(dtf, sp)) {
								find = true;
								dt.setIndex(index);
								pois.put(index, dt.dbField);// 快速记录
								break;
							}
						} else if (dt.matchType == 0) {
							if (StringUtils.startsWith(sp, dtf)) {
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
					msg += "[" + dt.dbField + "]";
				}
			}
			if (!StringUtils.isBlank(msg)) {
				log.error("检查MR文件：" + tmpFileName + "的标题头有问题！" + msg);
				date2 = new Date();
				report.setFields("检查文件标题", date1, date2, DataParseStatus.Failall.toString(), "在文件中缺失以下标题:" + msg);
				addJobReport(report);
				return false;
			}

			// 逐行读取数据
			int executeCnt = 0;
			boolean handleLineOk = false;
			long totalDataNum = 0;
			DateUtil dateUtil = new DateUtil();

			// 拼装sql
			insertTempTableSql = "insert into " + LteCellTempTable
					+ " (business_cell_id,pci,EARFCN,BUSINESS_ENODEB_ID) values(?,?,?,?)";
			log.debug("insertTempTableSql:" + insertTempTableSql);

			try {
				insertTStmt = connection.prepareStatement(insertTempTableSql);
			} catch (Exception e) {
				log.error("中兴MR数据prepareStatement处理失败！sql=" + insertTempTableSql);
				e.printStackTrace();
				connection.releaseSavepoint(savepoint);
				return false;
			}

			// 缓存excel表中已经出现过的小区ID
			Map<String, String> cacheExcelCellId = new HashMap<String, String>();
			do {
				line = reader.readLine();

				if (line == null) {
					break;
				}
				sps = line.split(",|\t");
				if (sps.length != fieldCnt) {
					continue;
				}
				totalDataNum++;

				handleLineOk = preHandleMrLine(sps, fieldCnt, pois, dbFieldsToTitles, dateUtil, cityId,
						cacheExcelCellId, insertTStmt);

				if (handleLineOk == true) {
					executeCnt++;
				}
				if (executeCnt > 1000) {
					// 每1000行执行一次
					try {
						insertTStmt.executeBatch();
						insertTStmt.clearBatch();
						executeCnt = 0;
					} catch (Exception e) {
						e.printStackTrace();
						return false;
					}
				}
			} while (!StringUtils.isBlank(line));

			// 提交到文件结尾剩余的行
			if (executeCnt > 0) {
				try {
					insertTStmt.executeBatch();
					insertTStmt.clearBatch();
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			}

			log.debug("MR数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据");

			Date d1 = new Date();
			Date d2 = null;

			// 通过业务小区ID更新系统LTE小区表的频点及PCI
			String upLteCellTabSql = "merge into rno_lte_cell p using RNO_LTE_IMPORT_MID np on (p.business_cell_id = np.business_cell_id) when matched then update set p.pci = np.pci,p.EARFCN = np.EARFCN";
			boolean flag = false;
			try {
				insertTStmt.execute(upLteCellTabSql);
				flag = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			d2 = new Date();
			if (flag) {
				report.setStage("完成更新系统LTE小区表");
				report.setReportType(1);
				report.setBegTime(d1);
				report.setEndTime(d2);
				report.setFinishState(DataParseStatus.Succeded.toString());
				report.setAttMsg("成功完成文件:" + tmpFileName);
				addJobReport(report);
			} else {
				report.setStage("完成更新系统LTE小区表");
				report.setReportType(1);
				report.setBegTime(d1);
				report.setEndTime(d2);
				report.setFinishState(DataParseStatus.Failall.toString());
				report.setAttMsg("失败完成文件:" + tmpFileName);
				addJobReport(report);
				return false;
			}

			long et = System.currentTimeMillis();
			log.info("退出对MR文件：" + tmpFileName + "的解析，耗时：" + (et - st) + "ms");

		} catch (Exception e) {
			e.printStackTrace();
			try {
				connection.rollback(savepoint);
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			try {
				insertTStmt.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (insertTStmt != null) {
				try {
					insertTStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}

		return true;
	}

	private boolean parseMr(JobRunnable jobWorker, JobReport report, String tmpFileName, File f, Connection connection,
			Statement stmt, long cityId, Map<String, DBFieldToTitle> dbFieldsToTitles) {
		// 重新读取，因为其为公用变量，为了及时复位更新数据：在多个文件中存在此问题
		dbFieldsToTitles = readDbToTitleCfgFromXml();
		PreparedStatement insertTStmt = null;
		long st = System.currentTimeMillis();

		Date date1 = new Date();
		Date date2 = null;
		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			date2 = new Date();
			report.setSystemFields("检查文件编码", date1, date2, DataParseStatus.Failall.toString(), "文件：" + tmpFileName
					+ ":无法识别的文件编码！");
			addJobReport(report);
			return false;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String msg = "";
		String line = "";
		date1 = new Date();
		date2 = null;
		HTable mrTable = null;
		HTable mrDescTable = null;
		// 创建 hbase 小区索引表
		HTable cellMeaTable = null;
		// 创建 hbase 邻区索引表
		HTable ncellMeaTable = null;
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
			// 读取到标题头
			int fieldCnt = sps.length;
			log.debug("中兴MR文件：" + tmpFileName + ",标题行为为：" + line + ", 有" + fieldCnt + "个字段。");

			// 判断标题的有效性
			Map<Integer, String> pois = new HashMap<Integer, String>();
			int index = -1;
			boolean find = false;
			for (String sp : sps) {
				index++;
				find = false;
				for (DBFieldToTitle dt : dbFieldsToTitles.values()) {
					for (String dtf : dt.titles) {
						if (dt.matchType == 1) {
							if (StringUtils.equals(dtf, sp)) {
								find = true;
								dt.setIndex(index);
								pois.put(index, dt.dbField);// 快速记录
								break;
							}
						} else if (dt.matchType == 0) {
							if (StringUtils.startsWith(sp, dtf)) {
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
					msg += "[" + dt.dbField + "]";
				}
			}
			if (!StringUtils.isBlank(msg)) {
				log.error("检查MR文件：" + tmpFileName + "的标题头有问题！" + msg);
				date2 = new Date();
				report.setFields("检查文件标题", date1, date2, DataParseStatus.Failall.toString(), "在文件中缺失以下标题:" + msg);
				addJobReport(report);
				return false;
			}

			List<Put> mrDataPuts = null;
			List<Put> mrDataDescPuts = null;

			List<Put> cellMeatimePuts = null;
			List<Put> ncellMeatimePuts = null;
			Map<String, Integer> mrDataNumMap = new HashMap<String, Integer>();// 将某日期对应的记录数量迭加缓存起来
			try {
				// hbase MR数据
				mrDataPuts = new ArrayList<Put>();
				mrDataDescPuts = new ArrayList<Put>();

				// hbase cell索引数据
				cellMeatimePuts = new ArrayList<Put>();
				// hbase ncell索引数据
				ncellMeatimePuts = new ArrayList<Put>();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			// 逐行读取数据
			int executeCnt = 0;
			// 缓存成功数量
			int cacheSuCnt = 0;
			boolean handleLineOk = false;
			long totalDataNum = 0;
			DateUtil dateUtil = new DateUtil();

			mrTable = new HTable(HadoopXml.getHbaseConfig(), HBTable.valueOf("RNO_4G_ZTE_MR"));
			mrDescTable = new HTable(HadoopXml.getHbaseConfig(), HBTable.valueOf("RNO_4G_MR_DESC"));
			mrTable.setAutoFlushTo(false);
			mrTable.setWriteBufferSize(10 * 1024 * 1024);
			mrDescTable.setAutoFlushTo(false);

			cellMeaTable = new HTable(HadoopXml.getHbaseConfig(), HBTable.valueOf("CELL_MEATIME"));
			cellMeaTable.setAutoFlushTo(false);

			ncellMeaTable = new HTable(HadoopXml.getHbaseConfig(), HBTable.valueOf("NCELL_MEATIME"));
			ncellMeaTable.setAutoFlushTo(false);
			// 获取邻区匹配所需要信息
			CellForMatch cellForMatch = rnoMrAdjCellMatch.getMatchCellContext(stmt, cityId);
			// 缓存excel表中已经出现过的小区ID
			Map<String, String> cacheExcelCellId = new HashMap<String, String>();

			log.debug("处理文件：" + tmpFileName + "，开始循环处理每一行。");
			do {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				sps = line.split(",|\t");

				if (totalDataNum % 10000 == 0) {
					log.debug("处理文件：" + tmpFileName + "，处理行：" + totalDataNum);
				}

				totalDataNum++;

				handleLineOk = handleMrLine(sps, fieldCnt, pois, cellForMatch, dbFieldsToTitles, dateUtil, mrDataPuts,
						mrDataNumMap, cellMeatimePuts, ncellMeatimePuts, cityId, cacheExcelCellId);

				if (handleLineOk == true) {
					executeCnt++;
					cacheSuCnt++;
				}
				if (executeCnt > 10000) {
					// 每10000行执行一次
					try {
						mrTable.put(mrDataPuts);
						mrDataPuts.clear();
						mrTable.flushCommits();

						cellMeaTable.put(cellMeatimePuts);
						cellMeatimePuts.clear();
						cellMeaTable.flushCommits();

						ncellMeaTable.put(ncellMeatimePuts);
						ncellMeatimePuts.clear();
						ncellMeaTable.flushCommits();

						executeCnt = 0;
					} catch (Exception e) {
						e.printStackTrace();
						mrDescTable.close();
						cellMeaTable.close();
						ncellMeaTable.close();
						return false;
					}
				}
			} while (!StringUtils.isBlank(line));

			// 执行
			if (executeCnt > 0) {
				// 当数量少的时候提交
				mrTable.put(mrDataPuts);
				mrDataPuts.clear();
				mrTable.flushCommits();

				cellMeaTable.put(cellMeatimePuts);
				cellMeatimePuts.clear();
				cellMeaTable.flushCommits();

				ncellMeaTable.put(ncellMeatimePuts);
				ncellMeatimePuts.clear();
				ncellMeaTable.flushCommits();
			}
			if (cacheSuCnt == 0) {
				return false;
			}
			log.debug("MR数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据");
			// ----一下进行数据处理----//
			String attMsg = "文件：" + tmpFileName;
			Date d1 = new Date();
			// --报告---//
			Date d2 = new Date();

			// 生成描述信息
			d1 = new Date();

			Iterator<String> mrDate = mrDataNumMap.keySet().iterator();
			String recordeTime = "";
			boolean flag = true;
			try {
				while (mrDate.hasNext()) {
					recordeTime = mrDate.next();
					Put put = new Put(Bytes.toBytes(String.valueOf(cityId)
							+ "_"
							+ String.valueOf(dateUtil.priorityAssignedParseDate(recordeTime, DateFmt.SDF10.getIndex())
									.getTime()) + "_" + "ZTE"));
					try {
						put.addColumn("DESCINFO".getBytes(), "CITY_ID".getBytes(),
								String.valueOf(cityId).getBytes("utf-8"));
						put.addColumn("DESCINFO".getBytes(), "MEA_TIME".getBytes(), recordeTime.getBytes("utf-8"));
						put.addColumn("DESCINFO".getBytes(), "RECORD_COUNT".getBytes(),
								String.valueOf(mrDataNumMap.get(recordeTime)).getBytes("utf-8"));
						put.addColumn("DESCINFO".getBytes(), "FACTORY".getBytes(), "ZTE".getBytes("utf-8"));
						put.addColumn("DESCINFO".getBytes(), "CREATE_TIME".getBytes(),
								dateUtil.format_yyyyMMddHHmmss(new Date()).getBytes("utf-8"));
						put.addColumn("DESCINFO".getBytes(), "MOD_TIME".getBytes(),
								dateUtil.format_yyyyMMddHHmmss(new Date()).getBytes("utf-8"));

						mrDataDescPuts.add(put);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				// 提交描述表数据
				mrDescTable.put(mrDataDescPuts);
				mrDataDescPuts.clear();
				mrDescTable.flushCommits();
				// 清空
				mrDataNumMap.clear();
			} catch (Exception e) {
				e.printStackTrace();
				flag = false;
			}
			d2 = new Date();
			if (flag) {
				report.setSystemFields("生成MR描述信息", d1, d2, DataParseStatus.Succeded.toString(), attMsg);
				addJobReport(report);
			} else {
				report.setSystemFields("生成MR描述信息", d1, d2, DataParseStatus.Failall.toString(), attMsg);
				addJobReport(report);
				return false;
			}

			long et = System.currentTimeMillis();
			log.info("退出对MR文件：" + tmpFileName + "的解析，耗时：" + (et - st) + "ms");

			// 一个文件一个提交
			connection.commit();

		} catch (Exception e) {
			e.printStackTrace();
			try {
				insertTStmt.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			try {
				connection.rollback();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return false;
		} finally {

			try {
				mrTable.close();
				mrDescTable.close();
				cellMeaTable.close();
				ncellMeaTable.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (insertTStmt != null) {
				try {
					insertTStmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
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
	 * @param cfm
	 * @param dbFtts
	 * @param dateUtil
	 * @param ncsDataPuts
	 * @param mrDataNumMap
	 * @param cityId
	 * @return
	 * @author chao.xj
	 * @date 2015-3-19下午2:17:07
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private boolean handleMrLine(String[] sps, int expectFieldCnt, Map<Integer, String> pois, CellForMatch cfm,
			Map<String, DBFieldToTitle> dbFtts, DateUtil dateUtil, List<Put> ncsDataPuts,
			Map<String, Integer> mrDataNumMap, List<Put> cellMeatimePuts, List<Put> ncellMeatimePuts, long cityId,
			Map<String, String> cacheExcelCellId) {
		if (sps == null) {
			return false;
		}
		String dbField = "";
		String enodebId = "", scEarfcn = "", scPci = "", meaDate = "", ncEarfcn = "", ncPci = "", cellName = "", ncEnodebId = "", cellId = "", ncellId = "", excelCellId = "";
		double cellLon = 0, cellLat = 0;
		for (int i = 0; i < sps.length; i++) {
			dbField = pois.get(i);// 该位置对应的数据库字段 即xml中的name
			if (dbField == null) {
				continue;
			}

			if (dbField.equals("ENODEB_ID")) {
				enodebId = sps[i];
			} else if (dbField.equals("CELL_BCCH")) {
				scEarfcn = sps[i];
			} else if (dbField.equals("CELL_PCI")) {
				scPci = sps[i];
			} else if (dbField.equals("NCELL_BCCH")) {
				ncEarfcn = sps[i];
			} else if (dbField.equals("NCELL_PCI")) {
				ncPci = sps[i];
			} else if (dbField.equals("MEA_TIME")) {
				meaDate = sps[i];
			} else if (dbField.equals("CELL_ID")) {
				excelCellId = sps[i];
			}
		}

		String cellKey = enodebId + "_" + scEarfcn + "_" + scPci;
		String ncellBcchPciKey = ncEarfcn + "_" + ncPci;
		CellLonLat cellInfo = null;
		cellInfo = cfm.getMatchCell(cellKey);
		// 系统工参表中不存在该小区则忽视
		if (cellInfo == null) {
			// 不存在则将其添加至某逗号相分割的字符串中
			if (cacheExcelCellId.get(excelCellId.trim()) == null) {
				cacheExcelCellId.put(excelCellId.trim(), excelCellId.trim());
				cellBuffer.append(excelCellId.trim() + ",");
			}
			return false;
		}
		cellName = cellInfo.getName();
		cellId = cellInfo.getCell();
		// 描述表记录数登记
		if (mrDataNumMap.get(meaDate) == null) {
			mrDataNumMap.put(meaDate, 1);
		} else {
			mrDataNumMap.put(meaDate, mrDataNumMap.get(meaDate) + 1);
		}
		// ---邻区匹配----//
		// 邻区信息
		String ncell = "";
		StringBuffer ncells = new StringBuffer();
		double dis = 1000000;
		// 邻区属性
		double ncellLon = 0;
		double ncellLat = 0;

		// 进行匹配
		// 主小区存在:此key为ncearfcn+ncpci:同频同PCI下存在若干个小区
		List<CellLonLat> clls = cfm.getBcchBsicCells(ncellBcchPciKey);
		System.out.println("ncellBcchPciKey=" + ncellBcchPciKey+",clls="+clls);
		if (clls == null || clls.isEmpty() || (clls.size() == 1 && cellId.equals(clls.get(0).getCell()))) {
			System.out.println("过滤");
			// pci与arfcn无组合
			ncell = "NotF_" + ncEarfcn + "_" + ncPci;
			// ncells = "";
			ncells.delete(0, ncells.length());
			// 小区属性值
			scEarfcn = String.valueOf(cellInfo.getBcch());
			cellLon = cellInfo.getLon();
			cellLat = cellInfo.getLat();
		} else {
			// 同频同PCI的邻区只有一个的情况
			if (clls.size() == 1) {
				ncell = clls.get(0).getName();
				// 算距离米为单位
				dis = LatLngHelper.Distance(cellInfo.getLon(), cellInfo.getLat(), clls.get(0).getLon(), clls.get(0)
						.getLat());
				dis = dis / 1000;// 转换为千米或公里
				/*
				 * ncells = ncell + "(" + clls.get(0).getName() + ",["
				 * + dis + "km]";
				 */
				ncells.append(ncell);
				ncells.append("(");
				ncells.append(",[");
				ncells.append(dis);
				ncells.append("km]");

				ncEnodebId = String.valueOf(clls.get(0).getEnodebId());
				ncEarfcn = String.valueOf(clls.get(0).getBcch());
				ncPci = String.valueOf(clls.get(0).getPci());
				ncellId = clls.get(0).getCell();
			} else {
				// 同频同PCI的邻区有若干个的情况
				// 计算所有的距离 treemap可以按序排列
				Map<Double, CellLonLat> disInfo = new TreeMap<Double, CellLonLat>();
				double lon = cellInfo.getLon();
				double lat = cellInfo.getLat();
				for (CellLonLat nc : clls) {
					if (!cellId.equals(nc.getCell())) {
						dis = LatLngHelper.Distance(lon, lat, nc.getLon(), nc.getLat()) / 1000;
						disInfo.put(dis, nc);
					}
					// System.out.println("disInfo:"+disInfo);
				}
				Set<Entry<Double, CellLonLat>> entry = disInfo.entrySet();
				int num = 0;
				for (Iterator<Entry<Double, CellLonLat>> it = entry.iterator(); it.hasNext();) {
					Entry<Double, CellLonLat> entry2 = it.next();
					if (num == 0) {
						ncell = entry2.getValue().getName();
						ncEnodebId = String.valueOf(entry2.getValue().getEnodebId());
						ncEarfcn = String.valueOf(entry2.getValue().getBcch());
						ncPci = String.valueOf(entry2.getValue().getPci());
						ncellId = entry2.getValue().getCell();
						dis = entry2.getKey();
					}
					ncells.append(entry2.getValue().getCell());
					ncells.append("(");
					ncells.append(entry2.getValue().getName());
					ncells.append(",[");
					ncells.append(entry2.getKey());
					ncells.append("]),");
					num++; // 不加永远是最大一个（升序）
				}
				ncells.deleteCharAt(ncells.length() - 1);
			}
			// 小区属性值
			cellLon = cellInfo.getLon();
			cellLat = cellInfo.getLat();

			// 邻区属性值
			CellLonLat ncellInfo = cfm.getMatchCell(ncEnodebId + "_" + ncEarfcn + "_" + ncPci);
			ncellLon = ncellInfo.getLon();
			ncellLat = ncellInfo.getLat();
			ncellId = ncellInfo.getCell();
		}

		// CELL MEATIME ROWKEY
		Calendar ca = Calendar.getInstance();
		ca.setTime(dateUtil.parseDateArbitrary(meaDate));
		Put cellMeatimePut = new Put(Bytes.toBytes(String.valueOf(cityId) + "_" + "MR" + "_"
				+ String.valueOf(ca.get(Calendar.YEAR)) + "_" + cellId));// cellId 小区标识为服务小区建索引

		cellMeatimePut.addColumn(
				String.valueOf(ca.get(Calendar.MONTH) + 1).getBytes(),
				Bytes.toBytes(String.valueOf(cityId)
						+ "_"
						+ String.valueOf(dateUtil.priorityAssignedParseDate(meaDate, DateFmt.SDF10.getIndex())
								.getTime()) + "_" + cellId + "_" + ncellId), null);
		// ncell meatime rowkey
		Put ncellMeatimePut = new Put(Bytes.toBytes(String.valueOf(cityId) + "_" + "MR" + "_"
				+ String.valueOf(ca.get(Calendar.YEAR)) + "_" + ncellId));// ncellId 邻区标识为服务小区建索引
		ncellMeatimePut.addColumn(
				String.valueOf(ca.get(Calendar.MONTH) + 1).getBytes(),
				Bytes.toBytes(String.valueOf(cityId)
						+ "_"
						+ String.valueOf(dateUtil.priorityAssignedParseDate(meaDate, DateFmt.SDF10.getIndex())
								.getTime()) + "_" + cellId + "_" + ncellId), null);

		Put put = new Put(Bytes.toBytes(String.valueOf(cityId) + "_"
				+ String.valueOf(dateUtil.priorityAssignedParseDate(meaDate, DateFmt.SDF10.getIndex()).getTime()) + "_"
				+ cellId + "_" + ncellId));

		put.addColumn("MRINFO".getBytes(), "CITY_ID".getBytes(), String.valueOf(cityId).getBytes());
		Iterator<DBFieldToTitle> dbToTitles = dbFtts.values().iterator();
		DBFieldToTitle dbToTitle = null;
		while (dbToTitles.hasNext()) {
			dbToTitle = dbToTitles.next();
			try {
				if (sps.length - 1 < dbToTitle.index) {
					continue;
				}

				put.addColumn("MRINFO".getBytes(), dbToTitle.dbField.getBytes(), sps[dbToTitle.index].getBytes("utf-8"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			// 添加匹配的小区信息
			put.addColumn("MRINFO".getBytes(), "CELL_ID".getBytes(), cellId.getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "CELL_NAME".getBytes(), cellName.getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "CELL_LON".getBytes(), String.valueOf(cellLon).getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "CELL_LAT".getBytes(), String.valueOf(cellLat).getBytes("utf-8"));
			// 添加匹配的邻区信息
			put.addColumn("MRINFO".getBytes(), "NCELL_ID".getBytes(), ncellId.getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "NCELL".getBytes(), ncell.getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "NCELL_LON".getBytes(), String.valueOf(ncellLon).getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "NCELL_LAT".getBytes(), String.valueOf(ncellLat).getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "NCELLS".getBytes(), String.valueOf(ncells).getBytes("utf-8"));
			put.addColumn("MRINFO".getBytes(), "DISTANCE".getBytes(), String.valueOf(dis).getBytes("utf-8"));
			// 放入集合
			ncsDataPuts.add(put);
			cellMeatimePuts.add(cellMeatimePut);
			ncellMeatimePuts.add(ncellMeatimePut);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;

	}

	private boolean preHandleMrLine(String[] sps, int expectFieldCnt, Map<Integer, String> pois,
			Map<String, DBFieldToTitle> dbFtts, DateUtil dateUtil, long cityId, Map<String, String> cacheExcelCellId,
			PreparedStatement insertTempStatement) {
		boolean flag = false;
		if (sps == null) {
			return flag;
		}
		String dbField = "";
		String enodebId = "", scEarfcn = "", scPci = "", excelCellId = "";
		for (int i = 0; i < sps.length; i++) {
			dbField = pois.get(i);// 该位置对应的数据库字段 即xml中的name
			if (dbField == null) {
				continue;
			}

			if (dbField.equals("ENODEB_ID")) {
				enodebId = sps[i];
			} else if (dbField.equals("CELL_BCCH")) {
				scEarfcn = sps[i];
			} else if (dbField.equals("CELL_PCI")) {
				scPci = sps[i];
			} else if (dbField.equals("CELL_ID")) {
				excelCellId = sps[i];
			}
		}
		if (cacheExcelCellId.get(excelCellId.trim()) == null) {

			try {
				insertTempStatement.setString(1, excelCellId.trim());
				insertTempStatement.setString(2, scPci);
				insertTempStatement.setString(3, scEarfcn);
				insertTempStatement.setString(4, enodebId);

				insertTempStatement.addBatch();

				cacheExcelCellId.put(excelCellId.trim(), excelCellId.trim());
				flag = true;
			} catch (SQLException e) {
				flag = false;
				e.printStackTrace();
			}
		} else {
			flag = false;
		}
		return flag;
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

		@Override
		public String toString() {
			return "DBFieldToTitle [dbField=" + dbField + ", matchType=" + matchType + ", index=" + index
					+ ", isMandatory=" + isMandatory + ", dbType=" + dbType + ", titles=" + titles + "]";
		}

	}

	/**
	 * @title 从xml配置文件中读取中兴MR数据库到excel的映射关系
	 * @return
	 * @author chao.xj
	 * @date 2015-3-17上午9:54:44
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public Map<String, DBFieldToTitle> readDbToTitleCfgFromXml() {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(Zte4GMrParserJobRunnable.class.getResource(
					"zte4GMrDbToTitles.xml").getPath()));
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

	public static void main(String[] args) {

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
}
