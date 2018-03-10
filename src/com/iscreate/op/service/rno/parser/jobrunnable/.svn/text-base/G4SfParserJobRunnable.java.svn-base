package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.DataParseProgress;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.parser.SfParserContext;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HBTable;
import com.iscreate.op.service.rno.tool.HadoopXml;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;
import com.iscreate.plat.tools.LatLngHelper;

public class G4SfParserJobRunnable extends DbParserBaseJobRunnable {
	private static Logger log = LoggerFactory.getLogger(G4SfParserJobRunnable.class);
	private final static SimpleDateFormat ROWKEY_SDF = new SimpleDateFormat("yyyy-MM-dd");
	private final static DateUtil dateUtil = new DateUtil();
	private final static String descTable = HBTable.valueOf("RNO_4G_SF_DESC");
	private final static String dataTable = HBTable.valueOf("RNO_4G_SF");
	private final static byte[] DATA_FAMILY_NAME = "SFINFO".getBytes();
	private final static String FILE_NAME_TABLE = "rno_lte_sf_file_rec";

	private MatchCell matchCell = null;
	private String factory = "ALL";
	private Date begTime = new Date(), endTime;
	/** 一次提交条数 **/
	private int executeNum = 10000;
	private StringBuilder sb = new StringBuilder();

	long cityId = -1;
	File file, workFile;
	String filePath, workFileName;
	long jobId = -1;
	JobStatus status;
	JobReport report;

	public G4SfParserJobRunnable() {
		super();
		super.setJobType("LTE_SF_FILE");
	}

	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connection111, Statement stmt111) {
		log.debug("进入任务：G4SfParserJobRunnable。job=" + job);
		jobId = job.getJobId();
		status = new JobStatus(jobId);
		report = new JobReport(jobId);

		begTime = new Date();
		// 获取job相关的信息
		String sql = "select * from RNO_DATA_COLLECT_REC where JOB_ID=" + jobId;
		List<Map<String, Object>> recs = RnoHelper.commonQuery(updateStatusStmt, sql);
		RnoDataCollectRec dataRec = null;
		if (recs != null && recs.size() > 0) {
			dataRec = RnoHelper.commonInjection(RnoDataCollectRec.class, recs.get(0), dateUtil);
		}

		String msg = "";

		if (dataRec == null) {
			msg = "获取上传文件失败！";
			log.error(msg);
			endTime = new Date();
			// job报告
			report.setSystemFields(DataParseProgress.GetFile.toString(), begTime, endTime,
					DataParseStatus.Failall.toString(), msg);
			addJobReport(report);

			// 数据记录本身的状态
			updateOwnProgress(DataParseStatus.Failall.toString());
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(endTime);
			return status;
		}

		// 准备
		cityId = dataRec.getCityId();
		String fileName = dataRec.getFileName();
		filePath = FileInterpreter.makeFullPath(dataRec.getFullPath());
		file = FileTool.getFile(filePath);
		// 开始解压
		List<File> allFiles = new ArrayList<File>();// 将所有待处理的SF文件放置在这个列表里
		String destDir = "";
		boolean fromZip = false;
		begTime = new Date();
		if (fileName.toLowerCase().endsWith(".zip")) {
			// 压缩包
			log.info("上传的SF文件是一个压缩包。");

			// 进行解压
			destDir = file.getParentFile().getPath() + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "/";

			boolean unzipOk = false;
			try {
				unzipOk = ZipFileHandler.unZip(FileInterpreter.makeFullPath(file.getAbsolutePath()), destDir);
			} catch (Exception e) {
				msg = "压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg);
				endTime = new Date();
				// job报告
				report.setSystemFields(DataParseProgress.Decompress.toString(), begTime, endTime,
						DataParseStatus.Failall.toString(), msg);
				addJobReport(report);

				// 数据记录本身的状态
				updateOwnProgress(DataParseStatus.Failall.toString());
				// job status
				status.setJobState(JobState.Failed);
				status.setUpdateTime(endTime);
				return status;
			}
			if (!unzipOk) {
				msg = "解压失败 ！仅支持zip格式的压缩包！ ";
				log.error(msg);
				endTime = new Date();
				// job报告
				report.setSystemFields(DataParseProgress.Decompress.toString(), begTime, endTime,
						DataParseStatus.Failall.toString(), msg);
				addJobReport(report);

				// 数据记录本身的状态
				updateOwnProgress(DataParseStatus.Failall.toString());
				// job status
				status.setJobState(JobState.Failed);
				status.setUpdateTime(endTime);
				return status;
			}
			file = new File(destDir);
			for (File f : file.listFiles()) {
				// 只要文件，不要目录
				if (f.isFile() && !f.isHidden()) {
					allFiles.add(f);
				}
			}
			fromZip = true;

			// ---报告----//
			msg = "解压文件：" + fileName + ",大小：" + RnoHelper.getPropSizeExpression(dataRec.getFileSize());
			log.debug(msg);
			// job报告
			report.setFields(DataParseProgress.Decompress.toString(), begTime, new Date(),
					DataParseStatus.Succeded.toString(), msg);
			addJobReport(report);
		} else if (fileName.endsWith(".rar")) {
			msg = "请用zip格式压缩文件！";
			log.error(msg);
			endTime = new Date();
			// job报告
			report.setSystemFields(DataParseProgress.Decompress.toString(), begTime, endTime,
					DataParseStatus.Failall.toString(), msg);
			addJobReport(report);

			// 数据记录本身的状态
			updateOwnProgress(DataParseStatus.Failall.toString());
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(endTime);
			return status;
		} else {
			log.debug("上传的SF是一个普通文件。");
			allFiles.add(file);
		}
		if (allFiles.isEmpty()) {
			msg = "未上传有效的mr文件！zip包里不能再包含有文件夹！";
			log.error(msg);
			endTime = new Date();
			// job报告
			report.setSystemFields(DataParseProgress.Decompress.toString(), begTime, endTime,
					DataParseStatus.Failall.toString(), msg);
			addJobReport(report);

			// 数据记录本身的状态
			updateOwnProgress(DataParseStatus.Failall.toString());
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(endTime);
			return status;
		}
		begTime = new Date();
		// 开始分析
		SfParserContext context = new SfParserContext();

		// 获取中兴切换的HBase描述表、数据表
		try {
			Configuration conf = HadoopXml.getHbaseConfig();
			context.buildConn(conf);
			context.addBufferedMutator("descTable", descTable);
			context.addBufferedMutator("dataTable", dataTable);
		} catch (IOException e1) {
			e1.printStackTrace();
			msg = "扫频数据导入时，获取HTable出错！";
			log.error(msg);
			if (context != null) {
				context.clear();
			}
			endTime = new Date();
			// job报告
			report.setSystemFields(DataParseProgress.Prepare.toString(), begTime, endTime,
					DataParseStatus.Failall.toString(), msg);
			addJobReport(report);

			// 数据记录本身的状态
			updateOwnProgress(DataParseStatus.Failall.toString());
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(endTime);
			return status;
		}
		// 建立每个表对应的put队列
		context.addPuts("descPuts");
		context.addPuts("dataPuts");
		// 2、设置cityId
		context.setCityId(cityId);
		// 是否还需要验证标题头
		context.setDbFieldsToTitles(readDbToTitleCfgFromXml());

		matchCell = new MatchCell(stmt, cityId);

		// 正在解析
		// job报告
		msg = "开始解析！";
		Date cacheBegTime = new Date();
		// job报告
		report.setFields(DataParseProgress.Decode.toString(), cacheBegTime, new Date(),
				DataParseStatus.Parsing.toString(), msg);
		addJobReport(report);
		// 数据记录本身的状态
		updateOwnProgress(DataParseStatus.Parsing.toString());
		// 状态
		status.setJobState(JobState.Running);
		status.setUpdateTime(new Date());
		updateProgress(status);

		int sucCnt = 0;
		ResultInfo info = new ResultInfo();
		int totalFileCnt = allFiles.size();
		int i = 0;
		for (File f : allFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					workFileName = f.getName();
					workFile = f;
				} else {
					workFileName = fileName;
					workFile = file;
				}

				// 再次对文件进行处理：在更新该区域的LTE小区的频点及PCI信息基础上进行邻区匹配及数据入库工作
				info = parse(this, context);
				i++;
				report.setStage(DataParseProgress.Report.toString());
				report.setReportType(1);
				report.setBegTime(begTime);
				report.setEndTime(new Date());
				if (info.isFlag()) {
					msg = "完成文件:成功（" + i + "/" + totalFileCnt + "）:" + workFileName;
					report.setFinishState(DataParseStatus.Succeded.toString());
					report.setAttMsg(msg);
					log.debug(msg);
					sucCnt++;
				} else {
					msg = "完成文件:失败（" + i + "/" + totalFileCnt + "）:" + workFileName + ",原因：" + info.getMsg();
					report.setFinishState(DataParseStatus.Failall.toString());
					report.setAttMsg(msg);
					log.debug(msg);
				}
				addJobReport(report);
			} catch (Exception e) {
				msg = "文件解析出错（" + i + "/" + totalFileCnt + "）:" + workFileName;
				// job报告
				report.setFields(DataParseProgress.Report.toString(), begTime, new Date(),
						DataParseStatus.Failall.toString(), msg);
				addJobReport(report);
				log.error(msg);
			}
		}
		// 清理hbase连接
		context.clear();
		// 记录状态
		msg = "全部文件解析结束！";
		String finishState = "";
		status.setUpdateTime(new Date());
		if (sucCnt == 0) {
			status.setJobState(JobState.Failed);
			finishState = DataParseStatus.Failall.toString();
			updateOwnProgress(DataParseStatus.Failall.toString());
		} else {
			status.setJobState(JobState.Finished);
			if (sucCnt == allFiles.size()) {
				// 全部成功
				finishState = DataParseStatus.Succeded.toString();
				updateOwnProgress(DataParseStatus.Succeded.toString());
			} else {
				finishState = DataParseStatus.Failpartly.toString();
				updateOwnProgress(DataParseStatus.Failpartly.toString());
			}
		}
		// job报告
		report.setFields(DataParseProgress.Decode.toString(), cacheBegTime, new Date(), finishState, msg);
		addJobReport(report);
		log.debug("退出任务：G4SfParserJobRunnable。status=" + status);
		return status;
	}

	private ResultInfo parse(JobRunnable jobWorker, SfParserContext context) {
		log.debug("开始解释 SF 数据。" + workFileName);
		begTime = new Date();
		Date date1 = new Date();
		long st = date1.getTime();
		ResultInfo info = new ResultInfo();
		// 每个文件清理缓存
		context.clearMidCache();
		// 保存文件名
		long fileId = saveFileName();
		if (fileId <= 0) {
			log.error("保存：" + workFileName + ":的文件名失败！");
			info.setFlag(false);
			info.setMsg("保存：" + workFileName + ":的文件名失败！");
			report.setSystemFields("保存文件名", date1, new Date(), DataParseStatus.Failall.toString(), "保存：" + workFileName
					+ ":的文件名失败！");
			addJobReport(report);
			return info;
		}
		context.setFileId(fileId);

		// 所用文件共用table
		BufferedMutator dataTable = context.getBufferedMutator("dataTable");
		BufferedMutator descTable = context.getBufferedMutator("descTable");

		// 读取文件
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(workFile), CSVFormat.DEFAULT.withHeader());
			Map<String, Integer> headerMap = parser.getHeaderMap();
			// 检查标题头
			if (!checkTitles(info, headerMap, context)) {
				return info;
			}
			// 逐行读取数据
			int executeCnt = 0;
			// 缓存成功数量
			int cacheSuCnt = 0;
			// 缓存总数
			long totalDataNum = 0;
			// 标题头映射
			final String meaTimeStr = context.getForParser().get("MEA_TIME");
			final String cellIdStr = context.getForParser().get("CELL_ID");
			final String ncellBcchStr = context.getForParser().get("NCELL_BCCH");
			final String ncellPciStr = context.getForParser().get("NCELL_PCI");

			final Map<String, String> saveMap = context.getForSave();
			// 参数
			final List<Put> dataPuts= context.getPuts("dataPuts");
			final Map<String, Integer> dataNumMap = context.getDataNumMap();

			String meaTime;
			int cellId,ncellId;
			Put put;

			log.debug("处理文件：" + workFileName + "，开始循环处理每一行。");
			for (CSVRecord record : parser) {
				if (++totalDataNum % 10000 == 0) {
					log.debug("处理文件：" + workFileName + "，处理行：" + totalDataNum);
				}
				try {
					meaTime = record.get(meaTimeStr);
					// 支持2016-2-2 2016/2/2 两种格式
					meaTime = meaTime.replace("/", "-");
					cellId = Integer.parseInt(record.get(cellIdStr));
					ncellId = matchCell.matchingCell(cellId, Integer.parseInt(record.get(ncellBcchStr)),
							Integer.parseInt(record.get(ncellPciStr)));
					if (ncellId > 0) {
						sb.setLength(0);
						// 在文件名ID首尾加入fn* *fn方便通过filter在rowkey中过滤文件名，不会与cellid等混淆
						sb.append(context.getCityId()).append("_")
								.append(ROWKEY_SDF.parse(meaTime).getTime()).append("_")
								.append("fn*").append(context.getFileId()).append("*fn").append("_").append(cellId)
								.append("_").append(ncellId);
						put = new Put(Bytes.toBytes(sb.toString()));
						for (String title : saveMap.keySet()) {
							put.addColumn(DATA_FAMILY_NAME, title.getBytes(),
									record.get(saveMap.get(title)).getBytes());
						}

						// 放入集合
						dataPuts.add(put);
						if (dataNumMap.containsKey(meaTime)) {
							dataNumMap.put(meaTime.intern(), dataNumMap.get(meaTime) + 1);
						} else {
							dataNumMap.put(meaTime.intern(), 1);
						}
						executeCnt++;
						cacheSuCnt++;
					}
				} catch (Exception e) {
					e.printStackTrace();
					log.info("处理第{}行失败",record.getRecordNumber());
				}
				if (executeCnt > executeNum) {
					// 每10000行执行一次
					try {
						dataTable.mutate(dataPuts);
						dataTable.flush();
						dataPuts.clear();
						executeCnt = 0;
					} catch (Exception e) {
						e.printStackTrace();
						dataTable.close();
						descTable.close();
						info.setFlag(false);
						info.setMsg("文件：" + workFileName + ",入库失败！");
						return info;
					}
				}
			}

			// 执行
			if (executeCnt > 0) {
				// 当数量少的时候提交
				dataTable.mutate(dataPuts);
				dataTable.flush();
				dataPuts.clear();
			}

			if (cacheSuCnt == 0) {
				info.setFlag(false);
				info.setMsg("文件：" + workFileName + ",无有效数据！");
				return info;
			}
			log.debug("SF数据文件：" + workFileName + "共有：" + totalDataNum + "行记录数据，入库：" + cacheSuCnt + "行");
			// ----一下进行数据处理----//
			String attMsg = "文件：" + workFileName;

			// 生成描述信息
			date1 = new Date();
			// 描述表记录数登记
			for (String recordeTime : context.getDataNumMap().keySet()) {
				put = new Put(Bytes.toBytes(String.valueOf(cityId) + "_" + ROWKEY_SDF.parse(recordeTime).getTime()
						+ "_" + workFileName));
				put.addColumn("DESCINFO".getBytes(), "CITY_ID".getBytes(), String.valueOf(cityId).getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "MEA_TIME".getBytes(), recordeTime.getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "FILE_NAME".getBytes(), workFileName.getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "RECORD_COUNT".getBytes(),
						String.valueOf(context.getDataNumMap().get(recordeTime)).getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "FACTORY".getBytes(), factory.getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "CREATE_TIME".getBytes(),
						dateUtil.format_yyyyMMddHHmmss(new Date()).getBytes("utf-8"));
				put.addColumn("DESCINFO".getBytes(), "MOD_TIME".getBytes(), dateUtil.format_yyyyMMddHHmmss(new Date())
						.getBytes("utf-8"));
				context.getPuts("descPuts").add(put);
			}
			try {
				// 提交描述表数据
				descTable.mutate(context.getPuts("descPuts"));
				descTable.flush();
				context.getPuts("descPuts").clear();
				context.clearCache();
			} catch (Exception e) {
				report.setSystemFields("生成SF描述信息", date1, new Date(), DataParseStatus.Failall.toString(), attMsg);
				addJobReport(report);
				info.setFlag(false);
				info.setMsg("文件：" + workFileName + ",生成描述信息失败！");
				return info;
			}
			report.setSystemFields("生成SF描述信息", date1, new Date(), DataParseStatus.Succeded.toString(), attMsg);
			addJobReport(report);
			log.debug("退出对SF文件：" + workFileName + "的解析，耗时：" + (System.currentTimeMillis() - st) + "ms");
		} catch (Exception e) {
			e.printStackTrace();
			info.setFlag(false);
			info.setMsg("文件：" + workFileName + ",出错！");
			return info;
		} finally {
			if (parser != null) {
				try {
					parser.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		info.setFlag(true);
		return info;
	}
	/**
	 * 保存文件名
	 * 
	 * @param info
	 * @return
	 * @author chen.c10
	 * @date 2016年4月28日
	 * @version RNO 3.0.1
	 */
	private long saveFileName() {
		long fileId = -1;
		try {
			String sql = "select rec_id from " + FILE_NAME_TABLE + " where file_name = '" + workFileName
					+ "' and area_id=" + cityId + " order by mod_time";
			log.debug("查询文件名sql:{}", sql);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				fileId = rs.getLong(1);
			}
			rs.close();
			if (fileId < 0) {
				sql = "select SEQ_" + FILE_NAME_TABLE + ".nextval id from dual";
				log.debug("获取序列sql:{}", sql);
				rs = stmt.executeQuery(sql);
				while (rs.next()) {
					fileId = rs.getLong(1);
				}
				rs.close();
				if (fileId > 0) {
					sql = "insert into " + FILE_NAME_TABLE + " (rec_id, area_id, file_name, job_id) VALUES (" + fileId
							+ "," + cityId + ",'" + workFileName + "'," + jobId + ")";
					log.debug("插入文件名sql:{}", sql);
					if (stmt.executeUpdate(sql) == 0) {
						fileId = -1;
					}
				}
			} else {
				sql = "update " + FILE_NAME_TABLE + " set mod_time=sysdate, job_id=" + jobId + " where rec_id ="
						+ fileId;
				log.debug("更新文件名信息sql:{}", sql);
				if (stmt.executeUpdate(sql) == 0) {
					fileId = -1;
				}
			}
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		return fileId;
	}

	/**
	 * 检查标题头
	 * 
	 * @param info
	 * @param reader
	 * @param pois
	 * @param dbFieldsToTitles
	 * @return
	 * @author chen.c10
	 * @date 2016年4月28日
	 * @version RNO 3.0.1
	 */
	private boolean checkTitles(ResultInfo info, Map<String, Integer> headerMap, SfParserContext context) {
		Date date = new Date();
		// 读取到标题头
		log.debug("扫频文件：" + workFileName + ",标题行为为：" + headerMap.keySet() + ", 有" + headerMap.size() + "个字段。");
		// 判断标题的有效性
		boolean find = false;
		for (String sp : headerMap.keySet()) {
			find = false;
			for (DBFieldToTitle dt : context.getDbFieldsToTitles().values()) {
				for (String dtf : dt.titles) {
					if (dt.matchType == 1) {
						if (StringUtils.equals(dtf, sp)) {
							find = true;
							dt.setBingoName(sp);
							break;
						}
					} else if (dt.matchType == 0) {
						if (StringUtils.startsWith(sp, dtf)) {
							find = true;
							dt.setBingoName(sp);
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
		String msg = "";
		for (DBFieldToTitle dt : context.getDbFieldsToTitles().values()) {
			if (dt.isMandatory && dt.bingoName == null) {
				msg += "[" + dt.dbField + "]";
			}
		}
		if (!StringUtils.isBlank(msg)) {
			log.error("检查SF文件：" + workFileName + "的标题头有问题！" + msg);
			info.setFlag(false);
			info.setMsg("检查SF文件：" + workFileName + "的标题头有问题！" + msg);
			report.setFields("检查文件标题", date, new Date(), DataParseStatus.Failall.toString(), "在文件中缺失以下标题:" + msg);
			addJobReport(report);
			return false;
		}
		// 文件标题头合法，将标题头包装好，用于后面的解析和入库
		Map<String, String> forParser = new HashMap<String, String>();
		forParser.put("MEA_TIME", context.getDbFieldsToTitles().get("MEA_TIME").bingoName);
		forParser.put("CELL_ID", context.getDbFieldsToTitles().get("CELL_ID").bingoName);
		forParser.put("NCELL_BCCH", context.getDbFieldsToTitles().get("NCELL_BCCH").bingoName);
		forParser.put("NCELL_PCI", context.getDbFieldsToTitles().get("NCELL_PCI").bingoName);
		context.setForParser(forParser);
		Map<String, String> forSave = new HashMap<String, String>();
		for (DBFieldToTitle dt : context.getDbFieldsToTitles().values()) {
			if (dt.isSave) {
				forSave.put(dt.dbField, dt.bingoName);
			}
		}
		context.setForSave(forSave);
		return true;
	}

	public static class DBFieldToTitle {
		String dbField;
		int matchType;// 0：模糊匹配，1：精确匹配
		String bingoName;// 在文件中出现的位置，从0开始
		boolean isMandatory = true;// 是否强制要求出现
		boolean isSave = true;// 是否需要入库
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

		public String getBingoName() {
			return bingoName;
		}

		public void setBingoName(String bingoName) {
			this.bingoName = bingoName;
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

		public boolean isSave() {
			return isSave;
		}

		public void setSave(boolean isSave) {
			this.isSave = isSave;
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
			return "DBFieldToTitle [dbField=" + dbField + ", matchType=" + matchType + ", bingoName=" + bingoName
					+ ", isMandatory=" + isMandatory + ", isSave=" + isSave + ", dbType=" + dbType + ", titles="
					+ titles + ", sqlIndex=" + sqlIndex + "]";
		}
	}

	/**
	 * @title 从xml配置文件中读取中兴SF数据库到excel的映射关系
	 * @return
	 * @author chao.xj
	 * @date 2015-3-17上午9:54:44
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public static Map<String, DBFieldToTitle> readDbToTitleCfgFromXml() {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(G4SfParserJobRunnable.class.getResource("g4SfDbToTitles.xml")
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
					if ("save".equals(key)) {
						if (StringUtils.equals(val, "1")) {
							// save
							dt.setSave(true);
						} else {
							dt.setSave(false);
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
		if (updateStatusStmt != null) {
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
				updateStatusStmt.executeUpdate(str);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public void updateOwnProgress(String status) {
		if (status != null && updateStatusStmt != null) {
			try {
				updateStatusStmt.executeUpdate("update rno_data_collect_rec set FILE_STATUS='" + status
						+ "' where JOB_ID=" + jobId);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void releaseRes() {
		try {
			if (file != null) {
				FileTool.delete(file);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		FileTool.deleteLocOrHdfsFileOrDir(filePath);
		super.releaseRes();
	}

	class MatchCell {
		/** 频点与小区列表的映射 **/
		private Map<Integer, List<Integer>> earfcnToCellList = new HashMap<Integer, List<Integer>>();
		/** PCI与小区列表的映射 **/
		private Map<Integer, List<Integer>> pciToCellList = new HashMap<Integer, List<Integer>>();
		/** 小区与经纬度的映射 **/
		private Map<Integer, double[]> cellToLonLat = new HashMap<Integer, double[]>();
		/** 小区与基站的映射 **/
		private Map<Integer, Integer> cellToEnodeb = new HashMap<Integer, Integer>();
		/** 小区与邻区的映射 **/
		private Map<Integer, Map<String, Integer>> cell2Ncell = new HashMap<Integer, Map<String, Integer>>();

		private List<Integer> potentialCells = new ArrayList<Integer>();

		/**
		 * @return the cellToEnodeb
		 */
		public Map<Integer, Integer> getCellToEnodeb() {
			return cellToEnodeb;
		}

		public MatchCell(Statement stmt, long cityId) {
			String sql = "select business_cell_id cellid, longitude lon, latitude lat, earfcn, pci ,enodeb_id enodeb from Rno_Lte_Cell where area_id ="
					+ cityId;
			List<Map<String, Object>> recs = RnoHelper.commonQuery(stmt, sql);
			int earfcn;
			int pci;
			int cellId;
			int enodeb;
			double[] lonLat = null;
			for (Map<String, Object> rec : recs) {
				try {
					enodeb = Integer.parseInt(rec.get("ENODEB").toString());
					earfcn = Integer.parseInt(rec.get("EARFCN").toString());
					pci = Integer.parseInt(rec.get("PCI").toString());
					cellId = Integer.parseInt(rec.get("CELLID").toString());
					lonLat = new double[] { Double.parseDouble(rec.get("LON").toString()),
							Double.parseDouble(rec.get("LAT").toString()) };
					if (!earfcnToCellList.containsKey(earfcn)) {
						earfcnToCellList.put(earfcn, new ArrayList<Integer>());
					}
					earfcnToCellList.get(earfcn).add(cellId);

					if (!pciToCellList.containsKey(pci)) {
						pciToCellList.put(pci, new ArrayList<Integer>());
					}
					pciToCellList.get(pci).add(cellId);

					cellToLonLat.put(cellId, lonLat);
					cellToEnodeb.put(cellId, enodeb);
				} catch (Exception e) {
				}
			}
		}

		private int matchingCell(int cellId, int earfcn, int pci) {
			if (!cellToLonLat.containsKey(cellId)) {
				return -1;
			}
			if (cell2Ncell.containsKey(cellId)) {
				if (cell2Ncell.get(cellId).containsKey(earfcn + "_" + pci)) {
					return cell2Ncell.get(cellId).get(earfcn + "_" + pci);
				}
			} else {
				cell2Ncell.put(cellId, new HashMap<String, Integer>());
			}
			potentialCells.clear();
			int minDisNcell = -1;
			// 匹配邻区列表
			if (earfcnToCellList.containsKey(earfcn) && pciToCellList.containsKey(pci)) {
				for (int c : pciToCellList.get(pci)) {
					if (earfcnToCellList.get(earfcn).contains(c)) {
						potentialCells.add(c);
					}
				}
			}
			// 邻区列表中最近的小区
			double minDis = Double.MAX_VALUE;
			for (int c : potentialCells) {
				if (cellToLonLat.containsKey(c)) {
					double dis = LatLngHelper.Distance(cellToLonLat.get(cellId), cellToLonLat.get(c));
					if (dis < minDis) {
						minDis = dis;
						minDisNcell = c;
					}
				}
			}
			// 存入缓存
			cell2Ncell.get(cellId).put((earfcn + "_" + pci).intern(), minDisNcell);
			return minDisNcell;
		}
	}

	class ResultInfo {
		boolean flag = false;
		String msg = "";
		Map<String, String> bigMsg = new HashMap<String, String>();

		public boolean isFlag() {
			return flag;
		}

		public void setFlag(boolean flag) {
			this.flag = flag;
		}

		public String getMsg() {
			return msg;
		}

		public void setMsg(String msg) {
			this.msg = msg;
		}

		public Map<String, String> getBigMsg() {
			return bigMsg;
		}

		public void setBigMsg(Map<String, String> bigMsg) {
			this.bigMsg = bigMsg;
		}
	}
}
