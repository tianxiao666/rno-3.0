package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedInputStream;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hive.ql.parse.HiveParser_IdentifiersParser.stringLiteralSequence_return;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

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
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;

public class G4NiParserJobRunnable extends DbParserBaseJobRunnable {

	private static Log log = LogFactory.getLog(G4NiParserJobRunnable.class);
	
	private static String insertTableSql = "";
	
	public G4NiParserJobRunnable() {
		super();
		super.setJobType("G4NIFILE");
	}
	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connection,
			Statement stmt) {

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
		long cityId = dataRec.getCityId();
		String fileName = dataRec.getFileName();
		filePath = FileInterpreter.makeFullPath(dataRec.getFullPath());
//		file = new File(filePath);
		file=FileTool.getFile(filePath);
		long dataId = dataRec.getDataCollectId();

		String msg = "";

		// 开始解析
		List<File> allMrFiles = new ArrayList<File>();// 将所有待处理的NI文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		Date date1 = new Date(), date2,cacheDate1;
		cacheDate1 = date1;//缓存第一次进入runable的时间
		if (fileName.endsWith(".zip") || fileName.endsWith("ZIP")
				|| fileName.endsWith("Zip")) {
			date1 = new Date();
			fromZip = true;
			// 压缩包
			log.info("上传的NI文件是一个压缩包。");

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
					allMrFiles.add(f);
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
			// super.setCachedInfo(token, msg);
			// clearResource(destDir, null);

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
			log.info("上传的NI是一个普通文件。");
			allMrFiles.add(file);
		}

		if (allMrFiles.isEmpty()) {
			msg = "未上传有效的NI文件！zip包里不能再包含有文件夹！";
			log.error(msg);
			// super.setCachedInfo(token, msg);
			// clearResource(destDir, null);
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("未上传有效的NI文件！注意zip包里不能再包含有文件夹！");
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

		// 读取数据库与文本文件格式的配置
		// dbFieldsToTitles = readDbToTitleCfgFromProfile();

		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;
		
		
		int totalFileCnt = allMrFiles.size();
		int i = 0;
		DateUtil dateUtil = new DateUtil();
		for (File f : allMrFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
//				System.out.println("tmpFileName========="+tmpFileName);
					//对文件进行处理：在更新该区域的lte小区的频点及PCI信息基础上进行邻区匹配及数据入库工作
					date1 = new Date();
					/*parseOk = parseMr(this, report, tmpFileName, f, connection,
							stmt, cityId);*/
					parseOk = true;
					//生成描述数据信息
					long seqId = -1;
					seqId = RnoHelper.getNextSeqValue("SEQ_RNO_4G_NI_DESC",
							stmt);
					String dataStr = dateUtil.format_yyyyMMdd(new Date());
					//(数据类型)(最小值+Math.random()*(最大值-最小值+1))
					int cnt = (int)(10000+Math.random()*(100000-10000+1));
					String presql = "insert into  "
							+ " RNO_4G_NI_DESC (NI_DESC_ID,MEA_TIME,DATA_TYPE,RECORD_CNT,CREATE_TIME,MOD_TIME,CITY_ID) values("+seqId+",to_date('"+dataStr+"','yyyy-mm-dd'),'NI'"
							+ ","+cnt+",sysdate,sysdate," + cityId + ")";
					Statement descState = connection.createStatement();
					descState.execute(presql);
					
					i++;
					date2 = new Date();
					report.setStage("文件处理总结");
					report.setReportType(1);
					report.setBegTime(date1);
					report.setEndTime(date2);
					if (parseOk) {
						report.setFinishState(DataParseStatus.Succeded.toString());
						report.setAttMsg("成功完成文件（" + i + "/" + totalFileCnt + "）:"
								+ tmpFileName);
						sucCnt++;
					} else {
						report.setFinishState(DataParseStatus.Failall.toString());
						report.setAttMsg("失败完成文件（" + i + "/" + totalFileCnt + "）:"
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
				report.setAttMsg("文件解析出错（" + i + "/" + totalFileCnt + "）:"
						+ tmpFileName);
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

	private boolean parseMr(JobRunnable jobWorker, JobReport report,
			String tmpFileName, File f, Connection connection, Statement stmt,
			long cityId) {
		//重新读取，因为其为公用变量，为了及时复位更新数据：在多个文件中存在此问题
		Map<String, DBFieldToTitle> dbFieldsToTitles  = null;
		
//		PreparedStatement insertTStmt = null;
		Statement insertTStmt = null;
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
			report.setSystemFields("检查文件编码", date1, date2,
					DataParseStatus.Failall.toString(), "文件：" + tmpFileName
							+ ":无法识别的文件编码！");
			addJobReport(report);
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
				/*
				 * String testlin=
				 * "08/30/2014,MZA28,G五华棉洋镇桥江-2,49,53,7,7,32,100%,19.3077,73.4231,101,16,450,0,16,16,16,16,16,16,16,0,0,16,0,0,0,0,0,0,0,0"
				 * ; if (line.equals(testlin)) { log.debug("读取行line:"+line); }
				 */
				if (line == null) {
					sps = new String[] {};
					break;
				}
				sps = line.split(",|\t");
			} while (sps == null || sps.length < 10);
			// 读取到标题头
			int fieldCnt = sps.length;
			log.debug("MR文件：" + tmpFileName + ",title 为：" + line + ",有"
					+ fieldCnt + "标题");

			//通过XML文件定义标题与EXCEL文件标题匹配，供13个XML文件
			//判断标题是否满足要求
			boolean isMeet = true;
			Map<Integer, String> pois = new HashMap<Integer, String>();
			String tabName = "";
			/*for (int i = 0; i < mrXmlFiles.length; i++) {
//				System.out.println("mrXmlFiles[i]="+mrXmlFiles[i]+"------tabName="+mrTabs[i]+"------file="+f.getName());
				dbFieldsToTitles = readDbToTitleCfgFromXml(mrXmlFiles[i]);
				isMeet = judeTitle(sps, dbFieldsToTitles,pois);
				//获取XML文件对应的表名
				tabName = mrTabs[i];
//				log.debug("mrXmlFiles[i]="+mrXmlFiles[i]+"------tabName="+mrTabs[i]+"------file="+f.getName());
				if(isMeet){
					break;
				}
			}*/
			if (!isMeet) {
				log.error("检查NI文件：" + tmpFileName + "的标题头有问题！" + msg);
				date2 = new Date();
				report.setFields("检查文件标题", date1, date2,
						DataParseStatus.Failall.toString(), "在预定义的XML文件中匹配不到与之相对应的标题信息" + msg);
				addJobReport(report);
				// connection.releaseSavepoint(savepoint);
				return false;
			}
			Map<String, Integer> mrDataNumMap=new HashMap<String, Integer>();//将某日期对应的记录数量迭加缓存起来
			DateUtil dateUtil = new DateUtil();//日期格式转换类
			String meaDate = "";//测量日期
			try {
				//获取statement对象实例
				//切换成HIVE的连接
//				connection  = jdbcTemplate.getDataSource().getConnection();
				stmt = connection.createStatement();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			log.info("NI文件数据读取开始");
			long sTime = System.currentTimeMillis();
			// 逐行读取数据(真正读取数据)
			int executeCnt = 0;
			boolean handleLineOk = false;
			long totalDataNum = 0;
			StringBuffer sqlBuffer = new StringBuffer();
			String sqlString = "";
			//设定一个字符串占位符%s
			insertTableSql = "insert into " + tabName + " partition (year='%s',month='%s') (";
			for (String d : dbFieldsToTitles.keySet()) {
				if (dbFieldsToTitles.get(d).index >= 0) {
					// 只对出现了的进行组sql
					insertTableSql += d + ",";
				}
			}
			insertTableSql += "cityid";
			insertTableSql += ") values";
			sqlBuffer.append(insertTableSql);
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
				//需要改造
				/*handleLineOk = handleHwMrrLine(sps, fieldCnt, pois,
						dbFieldsToTitles, insertTStmt,dateUtil,context);*/
				/*	构造hive ----------SQL语句	*/
				String ws = "";
				int index = 1;
				for (String d : dbFieldsToTitles.keySet()) {
					if (dbFieldsToTitles.get(d).index >= 0) {
						//获取日期时间
						if("meatime".equals(d)){
							meaDate = sps[dbFieldsToTitles.get(d).index];
							meaDate = dateUtil.format_yyyyMMdd(dateUtil.parseDateArbitrary(meaDate));
							if(mrDataNumMap.get(meaDate)==null){
								mrDataNumMap.put(meaDate, 1);
							}else{
								mrDataNumMap.put(meaDate, mrDataNumMap.get(meaDate)+1);
							}
						}
						// 只对出现了的进行组sql
//						System.out.println("sps line====="+sps[dbFieldsToTitles.get(d).index].trim());
//						ws += "'"+new String((sps[dbFieldsToTitles.get(d).index].trim()).getBytes(),"UTF-8")+"',";
						ws += "'"+sps[dbFieldsToTitles.get(d).index].trim()+"',";
					}
				}
				if (StringUtils.isBlank(ws)) {
					log.error("没有有效标题数据！");
					return false;
				}
				ws += "'"+cityId+"'";
				sqlBuffer.append("(" + ws + "),");
				log.debug("insertTableSql:"+insertTableSql);

//				if (handleLineOk == true) {
					executeCnt++;
//				}
				if (executeCnt > 10000) {
					log.info("读取10000行数据耗时："+(System.currentTimeMillis()-sTime)/1000+"s");
					sTime = System.currentTimeMillis();
					// 每5000行执行一次
					try {
						Calendar now = Calendar.getInstance(); 
						now.setTime(dateUtil.parseDateArbitrary(meaDate));
						sqlString = sqlBuffer.substring(0, sqlBuffer.length()-1);
						sqlString = String.format(sqlString, now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);
						sqlString = sqlString.replaceAll("\n", "");
//						System.out.println("sqlString.sql==================="+sqlString);
						stmt.execute(sqlString);
				        sqlBuffer.setLength(0);//清空该字符串
				        sqlBuffer.append(insertTableSql);//再将sql语句头部加上
					} catch (SQLException e) {
						e.printStackTrace();
					}
					log.info("存入HIVE耗时："+(System.currentTimeMillis()-sTime)/1000+"s");
					sTime = System.currentTimeMillis();
					executeCnt = 0;
				}
			} while (!StringUtils.isBlank(line));
			if (executeCnt > 0) {
				//当数量少的时候提交
				Calendar now = Calendar.getInstance(); 
				now.setTime(dateUtil.parseDateArbitrary(meaDate));
				sqlString = sqlBuffer.substring(0, sqlBuffer.length()-1);
				sqlString = String.format(sqlString, now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1);
				sqlString = sqlString.replaceAll("\n", "");
//				System.out.println("sqlString.sql"+executeCnt+"==================="+sqlString);
				stmt.execute(sqlString);
		        sqlBuffer.setLength(0);//清空该字符串
		        sqlBuffer.append(insertTableSql);//再将sql语句头部加上
			}
			log.debug("mr数据文件：" + tmpFileName + "共有：" + totalDataNum
					+ "行记录数据");
			// ----一下进行数据处理----//
			String attMsg = "文件：" + tmpFileName;
			long t1, t2;
			Date d1 = new Date();
//			ResultInfo resultInfo = new ResultInfo();
			// --报告---//
			Date d2 = new Date();

			// 生成描述信息
			d1 = new Date();
			
			Iterator<String> mrDate=mrDataNumMap.keySet().iterator();
			String recordeTime="";
			boolean flag=true;
			try {
				insertTableSql = "insert into rno_4g_NI_desc (meatime,datatype,recordcnt,createtime,modtime,cityid) values";
				while (mrDate.hasNext()) {
					recordeTime=mrDate.next();
					insertTableSql += "('"+recordeTime+"','"+tabName.substring(7)+"','"+mrDataNumMap.get(recordeTime)+"','"+dateUtil.format_yyyyMMddHHmmss(new Date())+"','"+dateUtil.format_yyyyMMddHHmmss(new Date())+"','"+cityId+"'),"; 
				}
				insertTableSql = insertTableSql.substring(0, insertTableSql.length()-1);
//				System.out.println("insertTableSql=======>>>>>"+insertTableSql);
				stmt.execute(insertTableSql);
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
				flag=false;
			}
			d2 = new Date();
			if (flag) {
				report.setSystemFields("生成NI描述信息", d1, d2,
						DataParseStatus.Succeded.toString(), attMsg);
				addJobReport(report);
			} else {
				/*report.setSystemFields("生成mr描述信息", d1, d2,
						DataParseStatus.Failall.toString(), attMsg + "--"
								+ resultInfo.getMsg());*/
				report.setSystemFields("生成NI描述信息", d1, d2,
						DataParseStatus.Failall.toString(), attMsg);
				addJobReport(report);
				return false;
			}

			long et = System.currentTimeMillis();
			log.info("退出对NI文件：" + tmpFileName + "的解析，耗时：" + (et - st)
					+ "ms");
			
			// 一个文件一个提交
//			connection.commit();
			
		} catch (Exception e) {
			e.printStackTrace();
			
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
	private boolean handleMrLine(String[] sps, int expectFieldCnt,
			Map<Integer, String> pois, Map<String, DBFieldToTitle> dbFtts,
			 DateUtil dateUtil,
			List<Put> ncsDataPuts, Map<String, Integer> mrDataNumMap,List<Put> cellMeatimePuts,long cityId) {
		// log.debug("handleHwNcsLine--sps="+sps+",expectFieldCnt="+expectFieldCnt+",pois="+pois+",dbFtts="+dbFtts);
		/*if (sps == null || sps.length != expectFieldCnt) {
			return false;
		}*/
		if (sps == null) {
			return false;
		}
		String dbField = "";
		DBFieldToTitle dt = null;
		String  meaDate = "", ncEarfcn = "", cellName="",cellId="",mmeId="";
		double	cellLon=0 ,cellLat=0;
		for (int i = 0; i < sps.length; i++) {
			dbField = pois.get(i);// 该位置对应的数据库字段 即xml中的name
			// log.debug(i+" -> dbField="+dbField);
			if (dbField == null) {
				continue;
			}

			dt = dbFtts.get(dbField);// 该数据库字段对应的配置信息
			if (dbField.equals("MEA_TIME")) {
				meaDate = sps[i];
			} else if (dbField.equals("CELL_ID")) {
				//186880-2
				cellId = sps[i].replace("-", "");
			} else if (dbField.equals("MME_ID")) {
				mmeId = sps[i];
			} 
		}
		//小区匹配：判断工参表中是否存在该小区，不存在则不录入库-----验证服务小区是否满足条件，不满足则不处理
		/*cellName=cfm.getMatchCell(enodebId+"_"+scEarfcn+"_"+scPci);
		if(cellName==null){
			return false;
		}*/
		if(cellId==null||"".equals(cellId)){
			//不存在则将其添加至某逗号相分割的字符串中
			return false;
		}
		//描述表记录数登记
		Date mDate= dateUtil.parseDateArbitrary(meaDate);
		//yyyy-mm-dd
		meaDate = dateUtil.format_yyyyMMdd(mDate);
		if(mrDataNumMap.get(meaDate)==null){
			mrDataNumMap.put(meaDate, 1);
		}else{
			mrDataNumMap.put(meaDate, mrDataNumMap.get(meaDate)+1);
		}
		
		
		// CELL MEATIME ROWKEY
		Calendar ca = Calendar.getInstance();
		ca.setTime(dateUtil.parseDateArbitrary(meaDate));
		Put cellMeatimePut = new Put(Bytes.toBytes(String.valueOf(cityId) + "_"
				+ "NI" + "_" + String.valueOf(ca.get(Calendar.YEAR)) + "_"
				+ cellId));//cellId   小区标识为服务小区建索引
		//yyyy-mm-dd hh:mm:ss
		cellMeatimePut.add(String.valueOf(ca.get(Calendar.MONTH) + 1)
				.getBytes(), Bytes.toBytes(String.valueOf(cityId)
				+ "_"
				+ String.valueOf(dateUtil.priorityAssignedParseDate(dateUtil.format_yyyyMMddHHmmss(mDate),
						DateFmt.SDF10.getIndex()).getTime()) + "_" + cellId+"_"+mmeId),
				null);
		// hbase rowkey MR
		//yyyy-mm-dd hh:mm:ss
		Put put = new Put(Bytes.toBytes(String.valueOf(cityId)
				+ "_"
				+ String.valueOf(dateUtil.priorityAssignedParseDate(dateUtil.format_yyyyMMddHHmmss(mDate),
						DateFmt.SDF10.getIndex()).getTime()) + "_" + cellId+"_"+mmeId));
		put.add("NIINFO".getBytes(), "CITY_ID".getBytes(),
				String.valueOf(cityId).getBytes());
		
		Iterator<DBFieldToTitle>  dbToTitles=dbFtts.values().iterator();
		DBFieldToTitle dbToTitle=null;
		while (dbToTitles.hasNext()) {
			dbToTitle=dbToTitles.next();
			try {
				if(sps.length-1<dbToTitle.index || "CELL_ID".equals(dbToTitle.dbField.toString())){
					continue;
				}
				
				put.add("NIINFO".getBytes(),dbToTitle.dbField.getBytes(),
						sps[dbToTitle.index].getBytes("utf-8"));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		
		try {
			//添加小区信息
			put.add("NIINFO".getBytes(),"CELL_ID".getBytes(),
					cellId.getBytes("utf-8"));
			
			//放入集合
			ncsDataPuts.add(put);
			cellMeatimePuts.add(cellMeatimePut);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			return false;
		}
		return true;

	}

	private JobStatus failWithStatus(String msg, Date date1,
			JobRunnable jobWorker, Statement stmt, JobReport report,
			JobStatus status, long dataId, DataParseStatus parseStatus,
			DataParseProgress progress) {
		// job报告
		Date date2 = new Date();
		report.setFinishState(parseStatus.toString());
		report.setStage(progress.toString());
		report.setBegTime(date1);
		report.setEndTime(date2);
		report.setAttMsg(msg);
		addJobReport(report);

		// 数据记录本身的状态
		String sql = "update rno_data_collect_rec set FILE_STATUS='"
				+ DataParseStatus.Failall.toString()
				+ "' where DATA_COLLECT_ID=" + dataId;
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

	private static Map<String, DBFieldToTitle> readDbToTitleCfgFromProfile() {
		InputStream is = null;
		Properties prop = null;
		try {
			// 需要实时读取
			is = new BufferedInputStream(new FileInputStream(
					G4NiParserJobRunnable.class.getResource(
							"hwNcsDbToTitles.properties").getPath()));
			prop = new Properties();
			prop.load(is);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		Enumeration<String> names = (Enumeration<String>) prop.propertyNames();
		while (names.hasMoreElements()) {
			String name = (String) names.nextElement();
			log.debug(name + "=" + prop.getProperty(name));
			if (StringUtils.isBlank(name) || name.indexOf(",") < 0) {
				continue;
			}
			String vs = prop.getProperty(name);
			if (StringUtils.isBlank(vs)) {
				log.error("跳过 vs==" + vs);
				continue;
			}
			String[] parts1 = name.split(",");
			if (parts1.length != 3) {
				System.out.println("---wrong:" + name);
				continue;
			}
			DBFieldToTitle dt = new DBFieldToTitle();

			if (StringUtils.equals(parts1[0], "1")) {
				// mandaroty
				dt.setMandatory(true);
			} else {
				dt.setMandatory(false);
			}
			// 字段类型
			dt.setDbType(parts1[1]);
			// 字段名称与匹配要求
			if (parts1[2].indexOf("-") > 0) {
				// log.debug("有||,parts1[2]=" + parts1[2]);
				String[] parts2 = parts1[2].split("-");
				// log.debug("parts2=" + parts2.length);
				if (parts2.length != 2) {
					continue;
				}
				dt.setDbField(parts2[0]);
				if (StringUtils.equals(parts2[1], "1")) {
					dt.setMatchType(1);
				} else {
					dt.setMatchType(0);
				}
			} else {
				// log.debug("没有||");
				dt.setDbField(parts1[2]);
				dt.setMatchType(0);
			}

			String[] v = vs.split(",");
			for (String vo : v) {
				dt.addTitle(vo);
			}
			dbCfgs.put(dt.dbField, dt);
			log.debug(dt);
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
			if (!StringUtils.isBlank(t)) {
				titles.add(t);
			}
		}

		@Override
		public String toString() {
			return "DBFieldToTitle [dbField=" + dbField + ", matchType="
					+ matchType + ", index=" + index + ", isMandatory="
					+ isMandatory + ", dbType=" + dbType + ", titles=" + titles
					+ "]";
		}

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
	public Map<String, DBFieldToTitle> readDbToTitleCfgFromXml(String xmlfils) {
		log.debug("读取xmlfils文件为==>>>>>>>>>>>>>>"+xmlfils);
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(
					G4NiParserJobRunnable.class.getResource(
							xmlfils).getPath()));
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
			// TODO: handle exception
			e.printStackTrace();
		}
		return dbCfgs;
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
	private boolean judeTitle(String[] sps,
			Map<String, DBFieldToTitle> dbFieldsToTitles,Map<Integer, String> pois) {
		// 判断标题的有效性
		
		int index = -1;
		// 是否发现某一标题
		boolean find = false;
		for (String sp : sps) {
			index++;
			find = false;
			for (DBFieldToTitle dt : dbFieldsToTitles.values()) {
				// log.debug("-----dt==" + dt);
				for (String dtf : dt.titles) {
					if (dt.matchType == 1) {
						if (StringUtils.equals(dtf, sp.trim())) {
							find = true;
							dt.setIndex(index);
							pois.put(index, dt.dbField);// 快速记录
							break;
						}
					} else if (dt.matchType == 0) {
						if (StringUtils.startsWith(sp.trim(), dtf)) {
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
				// 不符合要求
				return false;
			}
		}
		return true;
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

	public static void main(String[] args) {
				ApplicationContext ac = new ClassPathXmlApplicationContext("spring/datasource-appcontx.xml"); 
			   System.out.println( ac.getBean("jdbcTemplate"));
	}
	private File file=null;
	private String filePath=null;
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
