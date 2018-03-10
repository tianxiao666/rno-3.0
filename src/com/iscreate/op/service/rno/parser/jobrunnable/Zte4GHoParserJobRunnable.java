package com.iscreate.op.service.rno.parser.jobrunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import com.iscreate.op.dao.rno.AuthDsDataDaoImpl;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.DataParseProgress;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.parser.HoParserContext;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;
import com.iscreate.plat.tools.LatLngHelper;

public class Zte4GHoParserJobRunnable extends DbParserBaseJobRunnable {

	private static Log log = LogFactory.getLog(Zte4GHoParserJobRunnable.class);

	//private RnoStructAnaV2 rnoStructAnaV2 = new RnoStructAnaV2Impl();
	//private ExcelServiceImpl excelService = new ExcelServiceImpl();
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");

	//必须要有的标题头
	private static List<String> expectTitles = Arrays.asList("开始时间", "结束时间",
			"查询粒度","小区","eNodeB", "邻区关系", "异频切换出请求次数",
			"异频切换出成功次数", "同频切换出请求次数","同频切换出成功次数");

	//必须的标题头所在的列
	private Map<String,Integer> expectTitlesToColumn = new HashMap<String, Integer>();
	
	private static Map<String, String> excelTitlesToFields = new HashMap<String, String>() {
		private static final long serialVersionUID = 1L;
		{
			put("MEA_TIME","开始时间");
		};
		{
			put("END_TIME","结束时间");
		};
		{
			put("DURATION","查询粒度");
		};
		{
			put("CELLID","小区");
		};
		{
			put("ENODEBID","eNodeB");
		};
		{
			put("NCELL","邻区关系");
		};
		{
			put("PMHOPREPATTLTEINTERF","异频切换出请求次数");
		};
		{
			put("PMHOPREPSUCCLTEINTERF","异频切换出成功次数");
		};
		{
			put("PMHOPREPATTLTEINTRAF","同频切换出请求次数");
		};
		{
			put("PMHOPREPSUCCLTEINTRAF","同频切换出成功次数");
		};
	};
	
	private Map<String,String> cellIdToPci = new HashMap<String, String>();
	private Map<String,String> cellIdToBcch = new HashMap<String, String>();
	private Map<String,String> cellIdToLon = new HashMap<String, String>();
	private Map<String,String> cellIdToLat = new HashMap<String, String>();
	
	public Zte4GHoParserJobRunnable() {
		super();
		super.setJobType("LTE_ZTE_HO_FILE");
	}

	
	@Override
	public JobStatus runJobInternal(JobProfile job, Connection connection,
			Statement stmt) {
		
		long jobId = job.getJobId();
		JobStatus status = new JobStatus(jobId);
		JobReport report = new JobReport(jobId);

		//正在运行
//		status.setJobRunningStatus(JobRunningStatus.Running);
		status.setJobState(JobState.Running);
		status.setUpdateTime(new Date());
		
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
		String areaIdStr = AuthDsDataDaoImpl.getSubAreaAndSelfIdListStrByParentId(cityId);

		Date tmpdate = new Date();
		//获取小区与小区id映射
		String lteCellSql = "select business_cell_id, pci,earfcn, longitude, latitude  from rno_lte_cell  where area_id in("+areaIdStr+")";
		List<Map<String, Object>> cellIdToCellIdList = RnoHelper.commonQuery(stmt, lteCellSql);
		
		//统计
		int cCnt = 0;
		String cellList = "";
		
		String cellId ="";
		String pci = "";
		String bcch = "";
		String lon = "";
		String lat = "";
		
		for (Map<String, Object> one : cellIdToCellIdList) {
			try {
				cellId = one.get("BUSINESS_CELL_ID").toString();
				pci = one.get("PCI").toString();
				bcch = one.get("EARFCN").toString();
				lon = one.get("LONGITUDE").toString();
				lat = one.get("LATITUDE").toString();
			} catch (Exception e) {
				cCnt++;
				cellList+=one.get("BUSINESS_CELL_ID").toString()+",";
				//log.warn(one.get("BUSINESS_CELL_ID").toString()+" 的工参不完整！");
			}			
			cellIdToPci.put(cellId, pci);
			cellIdToBcch.put(cellId, bcch);
			cellIdToLon.put(cellId, lon);
			cellIdToLat.put(cellId, lat);
		}

		// 报告
		if(cCnt==0){
			report.setFinishState(DataParseStatus.Succeded.toString());
		}else if (cCnt==cellIdToCellIdList.size()) {
			report.setFinishState(DataParseStatus.Failall.toString());
		}else {
			report.setFinishState(DataParseStatus.Failpartly.toString());
		}
		report.setStage("获取工参");
		report.setBegTime(tmpdate);
		report.setEndTime(new Date());
		report.setReportType(1);
		report.setAttMsg("有 "+cCnt+" 个小区的工参不完整，小区列表为："+(cellList.length()>0?cellList.substring(0,cellList.length()-1):""));
		addJobReport(report);
		
		String fileName = dataRec.getFileName();
		filePath = FileInterpreter.makeFullPath(dataRec.getFullPath());
//		File file = new File(filePath);
		file = FileTool.getFile(filePath);

		String msg = "";
		
		//开始解析
		List<File> allZteHoFiles = new ArrayList<File>();// 将所有待处理的文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		Date date1 = new Date(), date2;
		if (fileName.endsWith(".zip")
				||fileName.endsWith("ZIP")
				|| fileName.endsWith("Zip")
				) {
			fromZip = true;
			// 压缩包
			log.info("上传的中兴切换文件是一个压缩包。");

			// 进行解压
			String path = file.getParentFile().getPath();
			destDir = path + "/"
					+ UUID.randomUUID().toString().replaceAll("-", "") + "/";

			//
			boolean unzipOk =false;
			try{
				unzipOk=ZipFileHandler.unZip(file.getAbsolutePath(), destDir);
			}catch(Exception e){
				msg="压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg);
				clearResource(destDir, null);
				
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
				clearResource(destDir, null);
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
			//判断文件数量，不要超过规定数量
//			if(files.length>50){
//				msg="一次上传的mrr文件数量:["+files.length+"]超过了规定的最大数量：50";
//				log.error(msg);
//				setTokenInfo(token, msg);
//				super.setCachedInfo(token, msg);
//				clearResource(destDir, null);
//				areaLockManager.unlockAreasForMrr(affectAreaIds);
//				return false;
//			}
			for (File f : files) {
				// 只要文件，不要目录
				if (f.isFile() && !f.isHidden()) {
					allZteHoFiles.add(f);
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
		}else if(fileName.endsWith(".rar")){
			msg = "请用zip格式压缩文件！";
			log.error(msg);
			clearResource(destDir, null);
			
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
		}
		else {
			log.info("上传的数据是一个普通文件。");
			allZteHoFiles.add(file);
		}

		if (allZteHoFiles.isEmpty()) {
			msg = "未上传有效的zte切换文件！zip包里不能再包含有文件夹！";
			log.error(msg);
			clearResource(destDir, null);
			// job报告
			date2 = new Date();
			report.setFinishState(DataParseStatus.Failall.toString());
			report.setStage(DataParseProgress.Decompress.toString());
			report.setBegTime(date1);
			report.setEndTime(date2);
			report.setAttMsg("未上传有效的zte切换文件！注意zip包里不能再包含有文件夹！");
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
		
		//开始分析
		HoParserContext context = new HoParserContext();
		
		//获取中兴切换的HBase描述表、数据表
		try {
			Configuration conf = new Configuration();
			conf = HBaseConfiguration.create(conf);
			context.addHtable("descTable","RNO_4G_HO_DESC",conf);
			context.addHtable("dataTable","RNO_4G_ZTE_HO",conf);

		} catch (IOException e1) {
			e1.printStackTrace();
			msg = "中兴切换数据导入时，获取HTable出错！";
			log.error(msg);
			clearResource(destDir, context);
			return failWithStatus("获取HBase表时出错", new Date(),
					this, stmt, report, status,
					dataRec.getDataCollectId(), DataParseStatus.Failall,
					DataParseProgress.Prepare);
		}
		//建立每个表对应的put队列
		context.addPuts("descPuts");
		context.addPuts("dataPuts");
		
		// 2、设置cityId
		context.setCityId(cityId);

		String tmpFileName = fileName;
		int sucCnt = 0;
		HoResultInfo resInfo = new HoResultInfo();
		int totalFileCnt=allZteHoFiles.size();
		int i=0;
		
		for (File f : allZteHoFiles) {

				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
				date1 = new Date();
				i++;
				//文件解析
				try {
					resInfo = parseHo(this, report, stmt, tmpFileName, f, context,
							connection);
				} catch (Exception e) {
					e.printStackTrace();
					resInfo.setFlag(false);
					date2 = new Date();
					log.error(tmpFileName + "文件解析出错！");
					
					report.setReportType(1);
					report.setStage("文件处理总结");
					report.setBegTime(date1);
					report.setEndTime(date2);
					report.setFinishState(DataParseStatus.Failall.toString());
					report.setAttMsg("文件解析出错（"+i+"/"+totalFileCnt+"）:" + tmpFileName);
					addJobReport(report);
					continue;
				}
	
				date2 = new Date();
				report.setReportType(1);
				report.setStage("文件处理总结");
				if(resInfo.isFlag()){
					report.setFinishState(DataParseStatus.Succeded.toString());
				}else{
					report.setFinishState(DataParseStatus.Failall.toString());
				}
				report.setBegTime(date1);
				report.setEndTime(date2);
				if (resInfo.isFlag()) {
					report.setAttMsg("成功文件（"+i+"/"+totalFileCnt+"）:" + tmpFileName + ","+resInfo.getMsg());
					sucCnt++;
				}else{
					report.setAttMsg("失败文件（"+i+"/"+totalFileCnt+"）:" + tmpFileName+ ","+resInfo.getMsg());
				}
				addJobReport(report);
		}
		
		if (sucCnt > 0) {
			status.setJobState(JobState.Finished);
			status.setUpdateTime(new Date());
		} else {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
		}

		if (sucCnt == allZteHoFiles.size()) {
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
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		clearResource(destDir, context);
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return status;
	}

	/**
	 * 解析一个中兴切换数据文件，返回结果
	 * @param jobWorker
	 * @param report
	 * @param statement
	 * @param fileName
	 * @param file
	 * @param context
	 * @param connection
	 * @return
	 */
	private HoResultInfo parseHo(JobRunnable jobWorker, JobReport report,
			Statement statement, String fileName, File file,
			HoParserContext context, Connection connection){
		
		HoResultInfo resInfo = new HoResultInfo();
		long cityId = context.getCityId();
		
		
		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(file.getAbsolutePath());
		log.debug(fileName + " 文件编码：" + charset);
		if (charset == null) {
			log.error("文件：" + fileName + ":无法识别的文件编码！");
			resInfo.setFlag(false);
			resInfo.setMsg("文件：" + fileName + ":无法识别的文件编码！");
			return resInfo;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), charset));
		} catch (Exception e) {
			resInfo.setFlag(false);
			resInfo.setMsg("文件读取出错");
			return resInfo;
		}
		
		long t1 = System.currentTimeMillis();
		long t2 = 0l;
		log.info("----------开始处理中兴切换数据:");
		String line = "";
		String msg = "";
		int totalDataNum = 0;
		int n=0;
		
		int nullCellFilterCnt = 0;
		int nullNcellFilterCnt = 0;
		int emFilterCnt = 0;
		List<String> nullCellFilterList = new ArrayList<String>();
		List<String> nullNcellFilterList = new ArrayList<String>();
		List<String> emFilterList = new ArrayList<String>();
		
		try{
			String[] sps = new String[1];
			do {
				line = reader.readLine();
				if(line==null){
					sps=new String[]{};
					break;
				}
				sps = line.split(",|\t");
			} while (sps == null || sps.length < 10);
			// 读取到标题头长度
			int fieldCnt = sps.length;
			
			// 获取必须标题在文件的位置, 判断标题是否齐全
			for (String expectTitle : expectTitles) {
				boolean isExisted = false;
				for (int i = 0; i < sps.length; i++) {
					if(sps[i].equals(expectTitle)) {
						expectTitlesToColumn.put(expectTitle, i);
						isExisted = true;
					}
				}
				if(!isExisted) {
					msg += "["+expectTitle+"]";
				}
			}

			if(expectTitlesToColumn.size() != expectTitles.size()) {
				resInfo.setFlag(false);
				resInfo.setMsg("文件缺少以下列："+msg);
				return resInfo;
			}
			
			// 逐行读取数据(真正读取数据)
			long sTime =System.currentTimeMillis();
			int executeCnt = 0;

			do {
				line = reader.readLine();
				if (line == null) {
					break;
				}
				sps = line.split(",|\t");
				if (sps.length != fieldCnt || sps == null) {
					continue;
				}
				totalDataNum++;
				//System.out.println(totalDataNum);
				
				//将一行数据存进put
				//数据格式为2015/3/10 0:00，用dateUtil不能解析
				String sMeaTime = context.getDateUtil().format_yyyyMMddHHmmss(
						sdf.parse(sps[expectTitlesToColumn
								.get((excelTitlesToFields.get("MEA_TIME")))]));
				String eMeaTime = context.getDateUtil().format_yyyyMMddHHmmss(
						sdf.parse(sps[expectTitlesToColumn
					            .get((excelTitlesToFields.get("END_TIME")))]));
				// 缓存进context
				context.setMeaDate(sMeaTime);
				long meaMillis = context.getDateUtil().parseDateArbitrary(sMeaTime).getTime();
				String eNodebId = sps[expectTitlesToColumn
					                  .get((excelTitlesToFields.get("ENODEBID")))];
				String cellId = sps[expectTitlesToColumn
					                  .get((excelTitlesToFields.get("CELLID")))];
				String ncell = sps[expectTitlesToColumn
					                  .get((excelTitlesToFields.get("NCELL")))];	
				//rowkey
				String rowkey = cityId+"_"+meaMillis+"_"+eNodebId+"_"+cellId+"_"+ncell;
				Put put = new Put(Bytes.toBytes(rowkey));
				//cityId
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CITY_ID"), 
						Bytes.toBytes(cityId+""));
				//测量时间
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("MEA_TIME"), 
						Bytes.toBytes(sMeaTime));
				//结束时间
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("END_TIME"), 
						Bytes.toBytes(eMeaTime));
				//查询粒度
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("DURATION"), 
						Bytes.toBytes(sps[expectTitlesToColumn
						                  .get((excelTitlesToFields.get("DURATION")))]));
				//小区标识
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CELLID"), 
						Bytes.toBytes(cellId));
				//基站标识
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("ENODEBID"), 
						Bytes.toBytes(eNodebId));
				//邻区
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("NCELL"), 
						Bytes.toBytes(ncell));
				//小区PCI
				if(cellIdToPci.get(eNodebId+cellId) == null) {
					nullCellFilterCnt++;
					if(!nullCellFilterList.contains(eNodebId+cellId)){
					nullCellFilterList.add(eNodebId+cellId);
					}
					//在小区工参匹配不出小区名
					log.debug("在小区工参匹配不出小区名："+eNodebId+cellId+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CELL_PCI"), 
						Bytes.toBytes(cellIdToPci.get(eNodebId+cellId)));
				//CELL_BCCH
				if(cellIdToBcch.get(eNodebId+cellId) == null) {
					//在小区工参匹配不出小区名
					log.debug("在小区工参匹配不出小区名："+eNodebId+cellId+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CELL_BCCH"), 
						Bytes.toBytes(cellIdToBcch.get(eNodebId+cellId)));
				//CELL_LON
				if(cellIdToLon.get(eNodebId+cellId) == null) {
					log.debug("在小区工参匹配不出小区名："+eNodebId+cellId+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CELL_LON"), 
						Bytes.toBytes(cellIdToLon.get(eNodebId+cellId)));
				//CELL_LAT
				if(cellIdToLat.get(eNodebId+cellId) == null) {
					log.debug("在小区工参匹配不出小区名："+eNodebId+cellId+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("CELL_LAT"), 
						Bytes.toBytes(cellIdToLat.get(eNodebId+cellId)));
				
				//0:460:00:169373:2
				String[] ncellIdStr = ncell.split(":");
				//邻区PCI
				if(cellIdToPci.get(ncellIdStr[3]+ncellIdStr[4]) == null) {
					nullNcellFilterCnt++;
					if(!nullNcellFilterList.contains(ncellIdStr[3]+ncellIdStr[4])){
						nullNcellFilterList.add(ncellIdStr[3]+ncellIdStr[4]);
					}
					//在小区工参匹配不出小区名
					log.debug("在小区工参匹配不出邻区名："+ncellIdStr[3]+ncellIdStr[4]+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("NCELL_PCI"), 
						Bytes.toBytes(cellIdToPci.get(ncellIdStr[3]+ncellIdStr[4])));
				//NCELL_BCCH
				if(cellIdToBcch.get(ncellIdStr[3]+ncellIdStr[4]) == null) {
					//在小区工参匹配不出小区名
					log.debug("在小区工参匹配不出邻区名："+ncellIdStr[3]+ncellIdStr[4]+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("NCELL_BCCH"), 
						Bytes.toBytes(cellIdToBcch.get(ncellIdStr[3]+ncellIdStr[4])));
				//NCELL_LON
				if(cellIdToLon.get(ncellIdStr[3]+ncellIdStr[4]) == null) {
					log.debug("在小区工参匹配不出邻区名："+ncellIdStr[3]+ncellIdStr[4]+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("NCELL_LON"), 
						Bytes.toBytes(cellIdToLon.get(ncellIdStr[3]+ncellIdStr[4])));
				//NCELL_LAT
				if(cellIdToLat.get(ncellIdStr[3]+ncellIdStr[4]) == null) {
					log.debug("在小区工参匹配不出邻区名："+ncellIdStr[3]+ncellIdStr[4]+"，该行数据不入库");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("NCELL_LAT"), 
						Bytes.toBytes(cellIdToLat.get(ncellIdStr[3]+ncellIdStr[4])));
				//DISTANCE
				double dis = -1.0;
				double cellLon = 1000.0;
				double cellLat = 1000.0;
				double ncellLon = 1000.0;
				double ncellLat = 1000.0;
				try {
					cellLon = Double.parseDouble(cellIdToLon.get(eNodebId+cellId));
					cellLat = Double.parseDouble(cellIdToLat.get(eNodebId+cellId));
				} catch (Exception e) {
					emFilterCnt++;
					if(!emFilterList.contains(eNodebId+cellId)){
						emFilterList.add(eNodebId+cellId);
					}
					log.info(eNodebId+cellId + ",的坐标为空！");
					continue;
				}
				try {
					ncellLon = Double.parseDouble(cellIdToLon.get(ncellIdStr[3]+ncellIdStr[4]));
					ncellLat = Double.parseDouble(cellIdToLat.get(ncellIdStr[3]+ncellIdStr[4]));
				} catch (Exception e) {
					emFilterCnt++;
					if(!emFilterList.contains(eNodebId+cellId)){
						emFilterList.add(ncellIdStr[3]+ncellIdStr[4]);
					}
					log.info(ncellIdStr[3]+ncellIdStr[4]+",的坐标为空！");
					continue;
				}
				if(cellLon<1000.0&&cellLat<1000.0&&ncellLon<1000.0&&ncellLat<1000.0){
				dis = LatLngHelper.Distance(cellLon,cellLat,ncellLon,ncellLat);
				}else{
					emFilterCnt++;
					log.info(eNodebId+cellId + " 或 " + ncellIdStr[3]+ncellIdStr[4]+",的数据有误！");
					continue;
				}
				put.add(Bytes.toBytes("HOINFO"),Bytes.toBytes("DISTANCE"), 
						String.valueOf(dis/1000).getBytes("utf-8"));
				//异频切换出请求次数
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("PMHOPREPATTLTEINTERF"), 
						Bytes.toBytes(sps[expectTitlesToColumn
						                  .get((excelTitlesToFields.get("PMHOPREPATTLTEINTERF")))]));
				//异频切换出成功次数
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("PMHOPREPSUCCLTEINTERF"), 
						Bytes.toBytes(sps[expectTitlesToColumn
						                  .get((excelTitlesToFields.get("PMHOPREPSUCCLTEINTERF")))]));
				//同频切换出请求次数
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("PMHOPREPATTLTEINTRAF"), 
						Bytes.toBytes(sps[expectTitlesToColumn
						                  .get((excelTitlesToFields.get("PMHOPREPATTLTEINTRAF")))]));
				//同频切换出成功次数
				put.add(Bytes.toBytes("HOINFO"), Bytes.toBytes("PMHOPREPSUCCLTEINTRAF"), 
						Bytes.toBytes(sps[expectTitlesToColumn
						                  .get((excelTitlesToFields.get("PMHOPREPSUCCLTEINTRAF")))]));
				//放进put队列
				if(context.getPuts("dataPuts") != null) {
					context.getPuts("dataPuts").add(put);
				} else {
					log.error("dataPuts队列为null！不能加入put对象");
				}

				executeCnt++;
				
				// 每5000行执行一次提交
				if (executeCnt > 5000) {
					log.info("读取5000行数据耗时："+(System.currentTimeMillis()-sTime)/1000+"s");
					sTime = System.currentTimeMillis();
					
					//存入htable
					if(context.getHtable("dataTable") != null 
							&& context.getPuts("dataPuts") != null 
							&& context.getPuts("dataPuts").size() > 0) {
						log.info("Puts.size()="+context.getPuts("dataPuts").size());
						context.getHtable("dataTable").put(context.getPuts("dataPuts"));
					} else {
						log.info("这5000条数据中中兴切换数据为空，或者没找到对应context对象中的HTable！");
					}
					log.info("存入Hbase的表耗时："+(System.currentTimeMillis()-sTime)/1000+"s");
					sTime = System.currentTimeMillis();
					
					//清除put队列
					context.clearPuts();
					context.tableFlushCommits();
					
					executeCnt = 0;
				}
				n++;
			} while (!StringUtils.isBlank(line));
			
			//将剩余不足5000条的数据存入htable
			if (executeCnt > 0) {
				if(context.getHtable("dataTable") != null 
						&& context.getPuts("dataPuts") != null 
						&& context.getPuts("dataPuts").size() > 0) {
					log.info("Puts.size()="+context.getPuts("dataPuts").size());
					context.getHtable("dataTable").put(context.getPuts("dataPuts"));
				} else {
					log.info("这批数据中中兴切换数据为空，或者没找到对应context对象中的HTable！");
				}
				log.info("存入Hbase的表耗时："+(System.currentTimeMillis()-sTime)/1000+"s");
				//清除put队列
				context.clearPuts();
				context.tableFlushCommits();
			}
			
			if(n<=0) {
				resInfo.setFlag(false);
				resInfo.setMsg("文件数据导入失败，需要对应的小区工参");
				return resInfo;
			}
			
			//将描述信息存进Hbase表
			if(context.getMeaDate() == null) {
				resInfo.setFlag(false);
				resInfo.setMsg("文件解析出错");
				return resInfo;
			}
			long meaMillis = context.getDateUtil().parseDateArbitrary(context.getMeaDate()).getTime();
			//记录成功的行数
			//int recordNum = totalDataNum;
			int recordNum = n;
			String createTime = context.getDateUtil().format_yyyyMMddHHmmss(new Date());
			String modTime = context.getDateUtil().format_yyyyMMddHHmmss(new Date());

			String rowkey = cityId + "_" + meaMillis + "_ZTE";
			Put put = new Put(Bytes.toBytes(rowkey));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CITY_ID"), 
					Bytes.toBytes(cityId+""));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MEA_TIME"), 
					Bytes.toBytes(context.getMeaDate()));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("RECORD_COUNT"), 
					Bytes.toBytes(recordNum+""));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FACTORY"), 
					Bytes.toBytes("ZTE"));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("CREATE_TIME"), 
					Bytes.toBytes(createTime));
			put.add(Bytes.toBytes("DESCINFO"), Bytes.toBytes("MOD_TIME"), 
					Bytes.toBytes(modTime));
			//放进put队列
			if(context.getPuts("descPuts") != null) {
				context.getPuts("descPuts").add(put);
			} else {
				log.error("descPuts队列为null！不能加入put对象");
			}
			//提交到table
			if(context.getHtable("descTable") != null 
					&& context.getPuts("descPuts") != null 
					&& context.getPuts("descPuts").size() > 0) {
				log.info("Puts.size()="+context.getPuts("descPuts").size());
				context.getHtable("descTable").put(context.getPuts("descPuts"));
			} else {
				log.info("描述数据为空，或者没找到对应context对象中的HTable！");
			}
			
			t2 = System.currentTimeMillis();
			log.info("----------中兴切换数据处理完毕。耗时：" + (t2 - t1) + "ms");
			
		} catch (Exception e) {
			e.printStackTrace();
			resInfo.setFlag(false);
			resInfo.setMsg("文件解析出错");
			return resInfo;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
				resInfo.setFlag(false);
				resInfo.setMsg("文件解析出错");
				return resInfo;
			}
		}
		
		//文件解析和保存成功
		resInfo.setFlag(true);
/*		resInfo.setMsg("[文件数据共："+totalDataNum+"行，成功："+n+"行。其中主小区匹配不到工参:"+nullCellFilterCnt+"行，邻区匹配不到工参:"+nullNcellFilterCnt+"行，工参不全："+emFilterCnt+"行]<br/>"
						+ "匹配失败主小区列表："+nullCellFilterList.toString()+"<br/>"
						+ "匹配失败邻区列表："+nullNcellFilterList.toString()+"<br/>"
						+ "工参不全列表："+emFilterList.toString());*/
		resInfo.setMsg("[文件数据共："+totalDataNum+"行，成功："+n+"行。其中主小区匹配不到工参:"+nullCellFilterCnt+"行，邻区匹配不到工参:"+nullNcellFilterCnt+"行，工参不全："+emFilterCnt+"行]");
		Map<String,String> bigMsg = new HashMap<String, String>();
		bigMsg.put("匹配失败主小区列表", nullCellFilterList.toString());
		bigMsg.put("匹配失败邻区列表", nullNcellFilterList.toString());
		bigMsg.put("工参不全列表", emFilterList.toString());
		return resInfo;
	}

	
	/**
	 * 清理资源
	 * @param destDir
	 * @param context
	 * @author peng.jm
	 * @date 2014-7-18上午11:31:00
	 */
	private void clearResource(String destDir, HoParserContext context) {
		FileTool.deleteDir(destDir);
		if (context != null) {
			context.closeHTables();
		}
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
	
	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		if(jobStatus==null){
			return;
		}
		if(stmt!=null){
			try {
				String prog=jobStatus.getProgress();
				if(prog==null){
					prog="";
				}
				prog=prog.trim();
				if("".equals(prog)){
					prog=jobStatus.getJobState().getCode();
				}
				String str="update RNO_DATA_COLLECT_REC set file_status='"+prog+"' where job_id="+jobStatus.getJobId();
				stmt.executeUpdate(str);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
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
	
	class HoResultInfo{
		boolean flag = false;
		String msg = "";
		Map<String,String> bigMsg = new HashMap<String, String>(); 
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
