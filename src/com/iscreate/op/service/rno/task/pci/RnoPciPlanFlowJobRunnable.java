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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.iscreate.op.dao.rno.RnoStructureQueryDaoImpl;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.BaseJobRunnable;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.mapreduce.IscreateHadoopBaseJob;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapper;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducer;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HBTable;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;

public class RnoPciPlanFlowJobRunnable extends BaseJobRunnable {

	private static Log log = LogFactory.getLog(RnoPciPlanFlowJobRunnable.class);
	private static String jobType = "RNO_PCI_PLAN_FLOW";
	private static String rnoCellFlowTable = "RNO_CELL_FLOW";

	private static List<String> mrTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// MR数据
			add(HBTable.valueOf("RNO_4G_ERI_MR"));
			add(HBTable.valueOf("RNO_4G_ZTE_MR"));
		}
	};
	private static List<String> hoTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// HO数据
			add(HBTable.valueOf("RNO_4G_ERI_HO"));
			add(HBTable.valueOf("RNO_4G_ZTE_HO"));
		}
	};

	RnoStructureQueryDaoImpl structureQueryDao = null;
	private Connection connection;
	private Statement stmt;
	private long jobId = 0;

	@Override
	public boolean isMyJob(JobProfile job) {
		if (job == null)
			return false;
		else {
			return jobType.equals(job.getJobType());
		}
	}

	@Override
	public JobStatus runJob(JobProfile job) throws InterruptedException {
		// 获取工作id
		jobId = job.getJobId();

		// 初始化 Job 信息
		JobStatus status = new JobStatus(jobId);
		JobReport report = new JobReport(jobId);
		structureQueryDao = new RnoStructureQueryDaoImpl();

		// 创建数据库连接
		log.debug("PCI 干扰计算的 job 准备数据库连接。");
		DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
		connection = DataSourceConn.initInstance().getConnection();
		Date startTime = new Date();
		stmt = null;

		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			log.error("发生系统错误，无法创建数据库执行器！");

			// 保存报告信息
			report.setStage("创建数据库执行器");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("无法创建数据库执行器");
			addJobReport(report);

			// 关闭连接
			try {
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		// 获取job相关的信息
		String sql1 = "select * from RNO_DATA_COLLECT_REC where JOB_UUID=(select JOB_UUID from RNO_DATA_COLLECT_REC where JOB_ID="
				+ jobId + ")" + "and JOB_ID IS NULL";
		List<Map<String, Object>> recs1 = RnoHelper.commonQuery(stmt, sql1);
		RnoDataCollectRec dataRec1 = null;
		if (recs1 != null && recs1.size() > 0) {
			dataRec1 = RnoHelper.commonInjection(RnoDataCollectRec.class, recs1.get(0), new DateUtil());
		}
		// log.debug("jobId=" + jobId + ",对应的dataRec=" + dataRec);
		if (dataRec1 == null) {
			log.error("转换RnoDataCollectRec失败！");
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}
		// 准备
		// long cityId = dataRec.getCityId();
		Date baseDate = dataRec1.getBusinessTime();
		DateUtil dateUtil2 = new DateUtil();
		String baseDateStr = dateUtil2.format_yyyyMMdd(baseDate);
		String fileName = dataRec1.getFileName();
		String filepath = FileInterpreter.makeFullPath(dataRec1.getFullPath());
		// file = new File(filePath);
		File file = FileTool.getFile(filepath);
		// long dataId = dataRec.getDataCollectId();

		String msg1 = "";
		// 开始解析
		List<File> allFiles = new ArrayList<File>();// 将所有待处理的文件放置在这个列表里
		boolean fromZip = false;
		String destDir = "";
		if (fileName.endsWith(".zip") || fileName.endsWith("ZIP") || fileName.endsWith("Zip")) {
			fromZip = true;
			// 压缩包
			log.info("上传的文件是一个压缩包。");

			// 进行解压
			String path = file.getParentFile().getPath();
			destDir = path + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "/";

			//
			boolean unzipOk = false;
			try {
				unzipOk = ZipFileHandler.unZip(FileInterpreter.makeFullPath(file.getAbsolutePath()), destDir);
			} catch (Exception e) {
				msg1 = "压缩包解析失败！请确认压缩包文件是否被破坏！";
				log.error(msg1);
				e.printStackTrace();
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;

			}
			if (!unzipOk) {
				// job status
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;

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
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;

		} else {
			log.info("上传的是一个普通文件。");
			allFiles.add(file);
		}
		if (allFiles.isEmpty()) {
			// job status
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		String tmpFileName = fileName;
		int sucCnt = 0;
		boolean parseOk = false;

		// LTE新站数据字段对应标题
		Map<String, DBFieldToTitle> cellFlowDbFieldsToTitles = readDbToTitleCfgFromXml("cellFlowToTitles.xml");

		for (File f : allFiles) {
			try {
				// 每一个文件的解析都应该是独立的
				if (fromZip) {
					tmpFileName = f.getName();
				}
				parseOk = parseCsv(this, report, dataRec1, tmpFileName, f, connection, stmt, baseDateStr,
						cellFlowDbFieldsToTitles);
				if (parseOk) {
					sucCnt++;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (sucCnt == 0) {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		String sql2 = "select cell_id,cell_dflow,cell_uflow from RNO_CELL_FLOW";
		report.setBegTime(new Date());
		Map<String, Float> cellFlow = RnoHelper.commonQueryToMap(stmt, sql2);

		String sq = "select cell_id,is_dt_cell from RNO_CELL_FLOW";
		Map<String, String> isDtCellMap = RnoHelper.commonQueryToMap1(stmt, sq);

		if (cellFlow == null || isDtCellMap == null) {
			log.error("查找相应的小区流量数据失败！");
			// 失败了
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());

			report.setStage("获取小区流量数据");
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("未找到小区流量数据！");
			addJobReport(report);
			try {
				stmt.close();
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return status;
		}
		try {
			String sql3 = "truncate table RNO_CELL_FLOW";
			stmt.executeUpdate(sql3);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		@SuppressWarnings("static-access")
		HttpSession session1 = SessionService.getInstance().getSession();
		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) session1.getAttribute("MYTASKINFO");
		String ks = taskobj.getTaskInfo().getKs() + "";
		session1.removeAttribute("MYTASKINFO");

		// 通过 jobId 获取干扰矩阵计算记录信息(rno_lte_pci_job表），包括变小区的 CLOB 信息
		List<Map<String, Object>> pciPlanRec = structureQueryDao.queryPciPlanJobRecByJobId(stmt, jobId);

		if (pciPlanRec.size() <= 0) {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		long cityId = Long.parseLong(pciPlanRec.get(0).get("CITY_ID").toString());
		String startMeaDate = pciPlanRec.get(0).get("BEG_MEA_TIME").toString();
		String endMeaDate = pciPlanRec.get(0).get("END_MEA_TIME").toString();
		String filePath = pciPlanRec.get(0).get("RESULT_DIR").toString();
		String planType = pciPlanRec.get(0).get("PLAN_TYPE").toString();
		String converType = pciPlanRec.get(0).get("CONVER_TYPE").toString();
		String relaNumType = pciPlanRec.get(0).get("RELA_NUM_TYPE").toString();
		String isCheckNCell = pciPlanRec.get(0).get("IS_CHECK_NCELL").toString();
		String optimizeCells = pciPlanRec.get(0).get("OPTIMIZE_CELLS").toString();
		String rdFileName = pciPlanRec.get(0).get("RD_FILE_NAME").toString();
		String dataFilePath = pciPlanRec.get(0).get("DATA_FILE_PATH").toString();
		String isExportAssoTable = pciPlanRec.get(0).get("IS_EXPORT_ASSOTABLE").toString();
		String isExportMidPlan = pciPlanRec.get(0).get("IS_EXPORT_MIDPLAN").toString();
		String isExportNcCheckPlan = pciPlanRec.get(0).get("IS_EXPORT_NCCHECKPLAN").toString();

		System.out.println("======>>>测量数据开始时间和结束时间：" + startMeaDate + ", " + endMeaDate);

		DateUtil dateUtil = new DateUtil();
		long startMeaMillis = dateUtil.parseDateArbitrary(startMeaDate).getTime();
		long endMeaMillis = dateUtil.parseDateArbitrary(endMeaDate).getTime();

		// 获取指定城市的全部同站小区，以 ENODEBID_EARFCN 为 key，多个小区CELL以#连接，一般一个ENODEBID_EARFCN的同站小区是三个。
		// 这是从 RNO_LTE_ENODEB 表中获取的，或者说是从公参表中获取的
		// @author chao.xj 2015-5-6 下午2:48:25
		// 同站小区判断条件变更由 enodeb->enodeb+earfcn
		Map<String, String> enodebToCells = structureQueryDao.getEnodebIdForCellsMap(stmt, cityId);

		if (enodebToCells == null) {
			report.setStage("通过城市ID获取从基站标识到lte小区的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");
			addJobReport(report);

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		Map<String, String> cellToLngLat = structureQueryDao.getLngLatForCellsMap(stmt, cityId);

		if (cellToLngLat == null || cellToLngLat.size() == 0) {
			report.setStage("通过城市ID获取小区到经纬度的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");
			addJobReport(report);

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			try {
				stmt.close();
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return status;
		}

		/************ Hadoop PCI干扰计算 start ************/
		log.debug("准备PCI规划干扰计算的mapreduce任务...");

		Configuration conf = new YarnConfiguration();

		String nodebs = map2String(enodebToCells);

		// 设置同站小区的公参数据
		conf.set("enodebToCells", nodebs);

		String cellToLngLatStr = map2String(cellToLngLat);

		conf.set("cellToLngLat", cellToLngLatStr);

		// 设置“关联度分子字段名”
		conf.set("numeratorField", relaNumType);

		// 设置其他数据
		conf.set("RESULT_DIR", filePath);
		conf.set("RD_FILE_NAME", rdFileName);
		conf.set("PLAN_TYPE", planType);
		conf.set("CONVER_TYPE", converType);
		conf.set("IS_CHECK_NCELL", isCheckNCell);
		conf.set("OPTIMIZE_CELLS", optimizeCells);
		conf.set("DATA_FILE_PATH", dataFilePath);
		conf.set("IS_EXPORT_ASSOTABLE", isExportAssoTable);
		conf.set("IS_EXPORT_MIDPLAN", isExportMidPlan);
		conf.set("IS_EXPORT_NCCHECKPLAN", isExportNcCheckPlan);

		// 获取页面自定义的阈值门限值
		List<RnoThreshold> rnoThresholds = new ArrayList<RnoThreshold>();
		String sql = "select job_id, param_type, param_code, param_val from rno_lte_pci_job_param where job_id = "
				+ jobId;
		List<Map<String, Object>> rawDatas = RnoHelper.commonQuery(stmt, sql);

		if (rawDatas == null || rawDatas.size() == 0) {
			// 取默认门限值
			sql = "select " + jobId
					+ " as job_id, 'STRUCTANA' as param_type, CODE as param_code, DEFAULT_VAL as param_val"
					+ " from RNO_THRESHOLD where module_type = 'LTEINTERFERCALC'";
			rawDatas = RnoHelper.commonQuery(stmt, sql);
		}

		for (int i = 0; i < rawDatas.size(); i++) {
			Map<String, Object> map = rawDatas.get(i);
			String code = map.get("PARAM_CODE").toString();
			String val = map.get("PARAM_VAL").toString();

			RnoThreshold rnoThreshold = new RnoThreshold();
			rnoThreshold.setCode(code);
			rnoThreshold.setDefaultVal(val);
			rnoThresholds.add(rnoThreshold);
		}

		String samefreqcellcoefweight = "0.8"; // 权值
		String switchratioweight = "0.2"; // 切换比例权值

		String cellm3rinterfercoef = "1";
		String cellm6rinterfercoef = "0.8";
		String cellm30rinterfercoef = "0.1";

		String beforenstrongcelltab = "6";
		String topncelllist = "10";

		String increasetopncelllist = "5";
		String convermethod1targetval = "5";
		String convermethod2targetval = "5";
		String convermethod2scoren = "10";

		String mincorrelation = "2";
		String minmeasuresum = "500";

		String dislimit = "5000";

		if (rnoThresholds != null) {
			for (RnoThreshold rnoThreshold : rnoThresholds) {
				String code = rnoThreshold.getCode();
				String val = rnoThreshold.getDefaultVal();
				if (code.equals("SAMEFREQCELLCOEFWEIGHT".toUpperCase())) {
					samefreqcellcoefweight = val;
				}
				if (code.equals("SWITCHRATIOWEIGHT".toUpperCase())) {
					switchratioweight = val;
				}
				if (code.equals("CELLM3RINTERFERCOEF".toUpperCase())) {
					cellm3rinterfercoef = val;
				}
				if (code.equals("CELLM6RINTERFERCOEF".toUpperCase())) {
					cellm6rinterfercoef = val;
				}
				if (code.equals("CELLM30RINTERFERCOEF".toUpperCase())) {
					cellm30rinterfercoef = val;
				}

				if (code.equals("BEFORENSTRONGCELLTAB".toUpperCase())) {
					beforenstrongcelltab = val;
				}
				if (code.equals("TOPNCELLLIST".toUpperCase())) {
					topncelllist = val;
				}
				if (code.equals("INCREASETOPNCELLLIST".toUpperCase())) {
					increasetopncelllist = val;
				}
				if (code.equals("CONVERMETHOD1TARGETVAL".toUpperCase())) {
					convermethod1targetval = val;
				}
				if (code.equals("CONVERMETHOD2TARGETVAL".toUpperCase())) {
					convermethod2targetval = val;
				}
				if (code.equals("CONVERMETHOD2SCOREN".toUpperCase())) {
					convermethod2scoren = val;
				}
				if (code.equals("MINCORRELATION".toUpperCase())) {
					mincorrelation = val;
				}
				if (code.equals("MINMEASURESUM".toUpperCase())) {
					minmeasuresum = val;
				}
				if (code.equals("DISLIMIT".toUpperCase())) {
					dislimit = val;
				}
			}
		}

		conf.set("samefreqcellcoefweight", samefreqcellcoefweight);
		conf.set("switchratioweight", switchratioweight);
		conf.set("cellm3rinterfercoef", cellm3rinterfercoef);
		conf.set("cellm6rinterfercoef", cellm6rinterfercoef);
		conf.set("cellm30rinterfercoef", cellm30rinterfercoef);
		conf.set("beforenstrongcelltab", beforenstrongcelltab);
		conf.set("topncelllist", topncelllist);
		conf.set("increasetopncelllist", increasetopncelllist);
		conf.set("convermethod1targetval", convermethod1targetval);
		conf.set("convermethod2targetval", convermethod2targetval);
		conf.set("convermethod2scoren", convermethod2scoren);
		conf.set("mincorrelation", mincorrelation);
		conf.set("minmeasuresum", minmeasuresum);
		conf.set("dislimit", dislimit);
		conf.set("ks", ks);

		PciConfig config = new PciConfig(conf, jobId, isExportAssoTable, isExportMidPlan, isExportNcCheckPlan, ks);

		log.info("门限值：" + "samefreqcellcoefweight=" + samefreqcellcoefweight + ",switchratioweight="
				+ switchratioweight + ",cellm3rinterfercoef=" + cellm3rinterfercoef + ",cellm6rinterfercoef="
				+ cellm6rinterfercoef + ",cellm30rinterfercoef=" + cellm30rinterfercoef + ",beforenstrongcelltab="
				+ beforenstrongcelltab + ",topncelllist=" + topncelllist + ",increasetopncelllist="
				+ increasetopncelllist + ",convermethod1targetval=" + convermethod1targetval
				+ ",convermethod2targetval=" + convermethod2targetval + ",convermethod2scoren=" + convermethod2scoren
				+ ",mincorrelation=" + mincorrelation + ",minmeasuresum=" + minmeasuresum + ",dislimit=" + dislimit);

		// 更新PCI规划干扰计算状态
		// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "正在计算");
		updateOwnProgress("正在计算");

		// 实例化一个 MapReduce 的 Job
		Job pciJob = null;
		try {
			pciJob = Job.getInstance(conf, "PciCalc_Worker");
		} catch (IOException e) {
			e.printStackTrace();
			log.error("创建hadoop集群用于pci规划干扰计算的job失败！");
			// 保存报告信息
			report.setStage("创建hadoop pci规划干扰计算job");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("创建hadoop pci规划干扰计算job失败");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		Path path = new Path(filePath + "/out");
		try {
			// 先在 HDFS 中删除已经存在的文件，因为 HDFS 文件系统是不能修改的。
			FileSystem fs = FileSystem.get(conf);
			fs.delete(path, true);
		} catch (IOException e1) {
			e1.printStackTrace();
			log.error("设置hadoop集群用于 pci规划干扰计算job的输出路径失败！");
			// 保存报告信息
			report.setStage("设置hadoop pci规划干扰计算job输出路径");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("设置hadoop pci规划干扰计算job输出路径失败");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		System.out.println("组装rowkey：cityId=" + cityId + ", 开始日期毫秒表示=" + startMeaMillis + ", 结束日期毫秒表示=" + endMeaMillis);

		// 在 Hadoop 中设置结果输出路径
		FileOutputFormat.setOutputPath(pciJob, path);

		String cellArr[] = null;
		if (optimizeCells != null && !"".equals(optimizeCells.trim())) {
			cellArr = optimizeCells.split(",");
			if (cellArr.length == 0) {
				log.error("变PCI小区字符串逗号分割后的长度为０,不满足基本需求！");
				// 保存报告信息
				report.setStage("变PCI小区字符串逗号分割后的长度为０,不满足基本需求！");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("变PCI小区字符串逗号分割后的长度为０,不满足基本需求！");
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		} else {
			log.error("变PCI小区字符串为NULL！");
			// 保存报告信息
			report.setStage("变PCI小区字符串为NULL！");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("变PCI小区字符串为NULL！");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		pciJob.setJarByClass(IscreateHadoopBaseJob.class);

		System.out.println("======>>>运行模式：" + conf.get("mapred.job.tracker"));

		pciJob.setMapperClass(PciMapper.class);
		pciJob.setReducerClass(PciReducer.class);
		pciJob.setOutputKeyClass(Text.class);
		pciJob.setOutputValueClass(Text.class);

		// TODO mr数据总是采用，ho默认启用，后续添加开关
		boolean isUseHO = true;
		// 数据源表名
		List<String> tableNames = new ArrayList<String>();

		tableNames.addAll(mrTableNames);
		if (isUseHO) {
			tableNames.addAll(hoTableNames);
		}
		// 拼装scan
		List<Scan> scans = getScans(cityId, startMeaMillis, endMeaMillis, tableNames);
		try {
			// 向 Hadoop 提交 Map 任务
			TableMapReduceUtil.initTableMapperJob(scans, PciMapper.class, Text.class, Text.class, pciJob);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("初始化 pci规划干扰计算job的Mapper失败！");
			// 保存报告信息
			report.setStage("初始化 pci规划干扰计算job的Mapper");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("初始化 pci规划干扰计算job的Mapper失败");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		System.out.println("注册集群用于 pci规划干扰计算的job...");
		String mrJobId = "";
		HttpSession session = SessionService.getSession();

		System.out.println("======>>>HttpSession 为: " + session);

		try {
			// 向 Hadoop 提交 Reduce 任务
			pciJob.submit();

			int progMonitorPollIntervalMillis = Job.getProgressPollInterval(conf);

			JobID pciJobId = pciJob.getJobID();
			mrJobId = pciJob.getJobID().toString();

			// 获取 reduce 的 jobid 存储入 lte_pci_job 表，以备后面获取停止该 job
			// 通过主进程 jobId 更新 lte_pci_job 表中 MR_JOB_ID（MapReduce 的 jobId）
			structureQueryDao.addMapReduceJobId(stmt, jobId, mrJobId);

			// 通过 mrjobID 存储job对象，
			log.debug("+++>>>当前 mrJobId 为:" + mrJobId + "-----当前 mrJob 为: " + pciJob + " 的信息!");

			session.setAttribute(mrJobId, pciJob);

			String msg = "";
			if (pciJob.getJobState().getValue() == 4) {
				log.info("+++>>>jobId=" + pciJobId + ",名称=" + job.getJobName() + "准备中...");
			}
			log.info("+++>>>jobId=" + pciJobId + ",名称=" + job.getJobName() + "准备中... 状态为="
					+ pciJob.getJobState().getValue());
			while (!pciJob.isComplete()) {
				if (pciJob.getJobState().getValue() == 1) {
					log.info("+++>>>jobId=" + pciJobId + ",名称=" + job.getJobName() + "执行中..." + System.lineSeparator()
							+ "+++>>>Map: " + org.apache.hadoop.util.StringUtils.formatPercent(pciJob.mapProgress(), 0)
							+ " Reduce: "
							+ org.apache.hadoop.util.StringUtils.formatPercent(pciJob.reduceProgress(), 0));
				}
				Thread.sleep(progMonitorPollIntervalMillis);
			}
			boolean calcResFlag = true;
			if (pciJob.isSuccessful()) {
				// 读取结果
				config.readDataFromFile();
				// 开始计算
				PciCalc pciCalc = new PciCalc(config);

				pciCalc.setCellToFlow(cellFlow);
				pciCalc.setCellToIsDTCell(isDtCellMap);
				try {
					pciCalc.execCalc();
				} catch (Exception e) {
					calcResFlag = false;
					e.printStackTrace();
				}
				if (calcResFlag) {
					log.info("+++>>>集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName() + ", 结果：任务成功！");
					log.info("PCI 规划干扰计算完成。");
					// 保存报告信息
					report.setStage("MapReduce计算结束，成功");
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("成功");

					// 从 HDFS 中获取返回信息
					// String strReturn = readReturnInfoFromHdfs(filePath, rdFileName);
					// String strReturn = config.readReturnInfoFromHdfs();
					String strReturn = config.getReturnInfo();

					if (!strReturn.equals("fail")) {
						report.setAttMsg("集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName()
								+ ", 结果：任务成功！<br>返回信息：" + strReturn);
					} else {
						report.setAttMsg("集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName()
								+ ", 结果：任务成功！<br>读取返回信息失败！");
					}
					addJobReport(report);

					// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算完成");
					updateOwnProgress("计算完成");
				} else {

					log.info("+++>>>集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName() + ", 结果：任务失败！");
					log.info("PCI 规划干扰计算完成。");
					// 保存报告信息
					report.setStage("MapReduce计算结束，失败");
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("失败");

					// 从 HDFS 中获取返回信息
					// String strReturn = readReturnInfoFromHdfs(filePath, rdFileName);
					// String strReturn = config.readReturnInfoFromHdfs();
					String strReturn = config.getReturnInfo();

					if (!strReturn.equals("fail")) {
						report.setAttMsg("集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName()
								+ ", 结果：任务失败！<br>返回信息：" + strReturn);
					} else {
						report.setAttMsg("集群 jobId = " + pciJobId + ", 名称 = " + job.getJobName()
								+ ", 结果：任务失败！<br>读取返回信息失败！");
					}
					addJobReport(report);

					// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算完成");
					updateOwnProgress("计算完成");
				}
			} else {
				msg = "+++>>>集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！";
				if (pciJob.getJobState().getValue() == 3) {
					msg = "+++>>>集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！原因：计算失败。";
				} else if (pciJob.getJobState().getValue() == 5) {
					msg = "+++>>>集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！原因：被关闭。";
				}
				log.info(msg);
				log.info("PCI 规划干扰计算失败");
				// 保存报告信息
				report.setStage("MapReduce计算结束，未成功完成");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg(msg);
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			report.setStage("提交MapReduce任务");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("提交MapReduce任务失败，未找到类");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		} catch (IOException e) {
			e.printStackTrace();
			report.setStage("提交MapReduce任务");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("提交MapReduce任务失败，IO错误");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		} catch (InterruptedException e) {
			e.printStackTrace();
			report.setStage("提交MapReduce任务");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("提交MapReduce任务失败，线程被中断");
			addJobReport(report);

			// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		} finally {
			try {
				// 完成或失败后予以删除该对象
				session.removeAttribute(mrJobId);
			} catch (Exception e2) {
				e2.printStackTrace();
				report.setStage("删除 MRJOB ");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("通过集群 jobId = " + mrJobId + ", 删除 MRJOB 失败");
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		}
		// 在 Hadoop 集群中 完成 PCI 规划干扰计算，并在 HDFS 中保存结果文件。

		try {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			stmt.close();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}

		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		status.setJobState(JobState.Finished);
		status.setUpdateTime(new Date());
		return status;
	}

	/**
	 * 
	 * @param resFilePath Pci计算返回信息文件的hdfs路径
	 * @param rdFileName Pci计算返回信息文件的名称
	 * @return
	 */
	/*
	 * private String readReturnInfoFromHdfs(String resFilePath, String rdFileName) {
	 * 
	 * // Pci规划数据源文件的名称,源数据文件有两个，一个是current后缀，一个是backup后缀
	 * String currentFileName = rdFileName + ".info";
	 * 
	 * File currentFile = FileTool.getFile(resFilePath + "/" + currentFileName);
	 * 
	 * // 两个源文件不存在，不能提供下载
	 * if (currentFile == null) {
	 * log.info("Pci规划计算的返回信息文件不存在！currentFileName=" + resFilePath + "/" + currentFileName);
	 * return "fail";
	 * }
	 * 
	 * FileInputStream fsCur = null;
	 * DataInputStream disCur = null;
	 * String isFinished = "";
	 * String result = "";
	 * try {
	 * // 当前数据文件存在，则选择current文件
	 * fsCur = new FileInputStream(currentFile);
	 * disCur = new DataInputStream(fsCur);
	 * result = disCur.readUTF();
	 * isFinished = disCur.readUTF();
	 * } catch (Exception e) {
	 * e.printStackTrace();
	 * try {
	 * if (disCur != null) {
	 * disCur.close();
	 * }
	 * if (fsCur != null) {
	 * fsCur.close();
	 * }
	 * } catch (IOException e1) {
	 * e1.printStackTrace();
	 * log.info("获取Pci规划结果源文件中，读取数据出错");
	 * result = "error";
	 * }
	 * log.info("获取Pci规划结果源文件中，读取数据出错");
	 * result = "error";
	 * }
	 * if (result.equals("error")) {
	 * return "fail";
	 * }
	 * if (!isFinished.equals("finished")) {
	 * return "fail";
	 * }
	 * return result;
	 * }
	 */

	@Override
	public void releaseRes() {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		if (stmt != null && structureQueryDao != null) {
			// 更新 PCI 规划干扰计算表的进度
			structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, jobStatus.getJobState().getCode());
		}
	}

	public void updateOwnProgress(String jobStatus) {
		if (stmt != null && structureQueryDao != null) {
			// 更新pci规划干扰计算表的进度
			structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, jobStatus);
		}
	}

	/**
	 * 把基站小区映射转换成以分隔符连接的字符串，用于MR计算
	 * 
	 * @param enodebToCells
	 *            基站小区映射 Map
	 * @return 字符串
	 */
	public static String map2String(Map<String, String> enodebToCells) {
		String str = "";
		for (String key : enodebToCells.keySet()) {
			str += key + "=" + enodebToCells.get(key) + "|";
		}
		return str.substring(0, str.length() - 1);
	}

	private static List<Scan> getScans(long cityId, long startMeaMillis, long endMeaMillis, List<String> tableNames) {
		List<Scan> scans = new ArrayList<Scan>();
		for (String tableName : tableNames) {
			Scan scan = getScan(cityId, startMeaMillis, endMeaMillis, tableName);
			scans.add(scan);
		}
		return scans;
	}

	private static Scan getScan(long cityId, long startMeaMillis, long endMeaMillis, String tableName) {
		Scan scan = new Scan();
		// 加入这两句速度快很多
		scan.setCacheBlocks(false);
		scan.setCaching(500);
		scan.setStartRow(Bytes.toBytes(cityId + "_" + startMeaMillis + "_#"));
		scan.setStopRow(Bytes.toBytes(cityId + "_" + endMeaMillis + "_~"));
		scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes(tableName));
		// Filter filter=new SingleColumnValueFilter("NCSINFO".getBytes(), "DISTANCE".getBytes(),
		// CompareOp.LESS_OR_EQUAL, "3".getBytes());
		return scan;
	}

	/*	*//**
	 * 从hdfs文件中读取数据
	 */
	/*
	 * private void readDataFromFile(String filePath, PciCalc pciCalc) {
	 *//** 小区与同站其他小区列的映射，同站其他小区已按关联度从大到小排列 **/
	/*
	 * Map<String, List<String>> cellToSameStationOtherCells = new HashMap<String, List<String>>();
	 *//** 小区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 **/
	/*
	 * Map<String, List<String>> cellToNotSameStationCells = new HashMap<String, List<String>>();
	 *//** 小区与邻区关联度的映射（包含了同站其他小区） **/
	/*
	 * Map<String, Map<String, Double>> cellToNcellAssocDegree = new HashMap<String, Map<String, Double>>();
	 *//** 小区与小区总关联度的映射 **/
	/*
	 * Map<String, Double> cellToTotalAssocDegree = new HashMap<String, Double>();
	 *//** 小区与原PCI的映射 **/
	/*
	 * Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();
	 * 
	 * log.debug(filePath);
	 * BufferedReader reader = null;
	 * String line1, line2, line3, line4, line5 = "";
	 * File file = FileTool.getFile(filePath);
	 * String charset = null;
	 * charset = FileTool.getFileEncode(file.getAbsolutePath());
	 * try {
	 * reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
	 * } catch (UnsupportedEncodingException e) {
	 * e.printStackTrace();
	 * } catch (FileNotFoundException e) {
	 * e.printStackTrace();
	 * }
	 * 
	 * try {
	 * line1 = reader.readLine();
	 * if (line1 != null && line1.length() > 0) {
	 * String[] sps1 = line1.split(";");
	 * for (int a = 0; a < sps1.length; a++) {
	 * String[] keyvalue1 = sps1[a].split(":");
	 * cellToOriPci.put(keyvalue1[0], Integer.parseInt(keyvalue1[1].equals("*") ? "0" : keyvalue1[1]));
	 * }
	 * }
	 * 
	 * line2 = reader.readLine();
	 * if (line2 != null && line2.length() > 0) {
	 * String[] sps2 = line2.split(";");
	 * for (int b = 0; b < sps2.length; b++) {
	 * List<String> list = new ArrayList<String>();
	 * String[] keyvalue2 = sps2[b].split(":");
	 * String[] str1 = null;
	 * if (!keyvalue2[1].equals("*")) {
	 * str1 = keyvalue2[1].split("#");
	 * for (int c = 0; c < str1.length; c++) {
	 * list.add(str1[c]);
	 * }
	 * }
	 * cellToSameStationOtherCells.put(keyvalue2[0], list);
	 * }
	 * }
	 * 
	 * line3 = reader.readLine();
	 * if (line3 != null && line3.length() > 0) {
	 * String[] sps3 = line3.split(";");
	 * for (int d = 0; d < sps3.length; d++) {
	 * Map<String, Double> map = new HashMap<String, Double>();
	 * String[] keyvalue3 = sps3[d].split(":");
	 * if (!keyvalue3[1].equals("*")) {
	 * String[] listString = keyvalue3[1].split("#");
	 * for (int i = 0; i < listString.length; i++) {
	 * String[] str1 = listString[i].split("/");
	 * String str = "";
	 * if (str1.length == 1) {
	 * str = "0.0";
	 * } else if (str1.length == 2) {
	 * if (str1[1].length() > 12) {
	 * str = str1[1].substring(0, str1[1].lastIndexOf(".") + 10);
	 * } else {
	 * str = str1[1];
	 * }
	 * }
	 * map.put(str1[0], Double.parseDouble(str));
	 * }
	 * }
	 * cellToNcellAssocDegree.put(keyvalue3[0], map);
	 * }
	 * }
	 * 
	 * line4 = reader.readLine();
	 * if (line4 != null && line4.length() > 0) {
	 * String[] sps4 = line4.split(";");
	 * for (int f = 0; f < sps4.length; f++) {
	 * List<String> list = new ArrayList<String>();
	 * String[] keyvalue4 = sps4[f].split(":");
	 * String[] str1 = null;
	 * if (!keyvalue4[1].equals("*")) {
	 * str1 = keyvalue4[1].split("#");
	 * for (int g = 0; g < str1.length; g++) {
	 * list.add(str1[g]);
	 * }
	 * }
	 * cellToNotSameStationCells.put(keyvalue4[0], list);
	 * }
	 * }
	 * 
	 * line5 = reader.readLine();
	 * if (line5 != null && line5.length() > 0) {
	 * String[] sps5 = line5.split(";");
	 * for (int h = 0; h < sps5.length; h++) {
	 * String[] keyvalue5 = sps5[h].split(":");
	 * String str = "";
	 * if (!keyvalue5[1].equals("*")) {
	 * if (keyvalue5[1].length() > 12) {
	 * str = keyvalue5[1].substring(0, keyvalue5[1].lastIndexOf(".") + 10);
	 * } else {
	 * str = keyvalue5[1];
	 * }
	 * } else {
	 * str = "0.0";
	 * }
	 * cellToTotalAssocDegree.put(keyvalue5[0], Double.parseDouble(str));
	 * }
	 * }
	 * 
	 * } catch (IOException e) {
	 * e.printStackTrace();
	 * }
	 * pciCalc.setCellToNcellAssocDegree(cellToNcellAssocDegree);
	 * pciCalc.setCellToNotSameStationCells(cellToNotSameStationCells);
	 * pciCalc.setCellToOriPci(cellToOriPci);
	 * pciCalc.setCellToSameStationOtherCells(cellToSameStationOtherCells);
	 * pciCalc.setCellToTotalAssocDegree(cellToTotalAssocDegree);
	 * 
	 * }
	 */

	public static Map<String, DBFieldToTitle> readDbToTitleCfgFromXml(String proFile) {
		Map<String, DBFieldToTitle> dbCfgs = new TreeMap<String, DBFieldToTitle>();
		try {
			InputStream in = new FileInputStream(new File(RnoPciPlanImportFlowJobRunnable.class.getResource(proFile)
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

	private boolean parseCsv(JobRunnable jobWorker, JobReport report, RnoDataCollectRec dataRec, String tmpFileName,
			File f, Connection connection, Statement stmt, String baseDateStr,
			Map<String, DBFieldToTitle> cellDbFieldsToTitles) {

		// String spName = "everyStartlte" + System.currentTimeMillis();
		log.debug("f path:" + f.getAbsolutePath());
		/*
		 * Savepoint savepoint = null;
		 * try {
		 * savepoint = connection.setSavepoint(spName);
		 * } catch (SQLException e2) {
		 * e2.printStackTrace();
		 * }
		 */
		PreparedStatement insertCellTStmt = null;
		// long st = System.currentTimeMillis();

		// 读取文件
		BufferedReader reader = null;
		String charset = null;
		charset = FileTool.getFileEncode(f.getAbsolutePath());
		log.debug(tmpFileName + " 文件编码：" + charset);
		System.out.println(charset);
		if (charset == null) {
			log.error("文件：" + tmpFileName + ":无法识别的文件编码！");
			/*
			 * date2 = new Date();
			 * report.setSystemFields("检查文件编码", date1, date2,
			 * DataParseStatus.Failall.toString(), "文件：" + tmpFileName
			 * + ":无法识别的文件编码！");
			 * addJobReport(report);
			 * try {
			 * connection.releaseSavepoint(savepoint);
			 * } catch (SQLException e) {
			 * e.printStackTrace();
			 * }
			 */
			return false;
		}
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), charset));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String msg = "";
		String line = "";
		if (reader != null) {
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
					if ((dt.isMandatory && dt.index < 0) || sps.length != cellDbFieldsToTitles.size()
							|| num != cellDbFieldsToTitles.size()) {
						msg += "[" + dt.dbField + "在文件中未找到对应的数据]\r\n";
					}
				}
				if (!StringUtils.isBlank(msg)) {
					log.error("检查文件：" + tmpFileName + "的标题头有问题！" + msg);
					/*
					 * date2 = new Date();
					 * report.setFields("检查文件标题", date1, date2,
					 * DataParseStatus.Failall.toString(), msg);
					 * addJobReport(report);
					 * try {
					 * connection.releaseSavepoint(savepoint);
					 * } catch (Exception e) {
					 * log.error("连接释放安全点出错！");
					 * return false;
					 * }
					 */
					return false;
				}
				// 拼装sql
				String insertCellTableSql = "insert into " + rnoCellFlowTable + " (";
				String ws = "";
				index = 1;
				int cellTotalMark = 0;// 记录问号的总数
				for (String d : cellDbFieldsToTitles.keySet()) {
					if (cellDbFieldsToTitles.get(d).index >= 0) {
						cellTotalMark++;
						// 只对出现了的进行组sql
						cellDbFieldsToTitles.get(d).sqlIndex = index++;// 在数据库中的位置
						insertCellTableSql += d + ",";
						ws += "?,";
					}
				}
				if (StringUtils.isBlank(ws)) {
					log.error("没有有效标题数据！");
					return false;
				}
				insertCellTableSql = insertCellTableSql + "DATAID) values ( " + ws + "?)";
				log.debug("insertCellTableSql  sql=" + insertCellTableSql);

				try {
					insertCellTStmt = connection.prepareStatement(insertCellTableSql);
				} catch (Exception e) {
					msg = "准备流量数据插入的prepareStatement失败";
					log.error("准备流量数据插入的prepareStatement失败！sql=" + insertCellTableSql);
					e.printStackTrace();
					// connection.releaseSavepoint(savepoint);
					return false;
				}

				// 逐行读取数据
				int executeCnt = 0;
				int addLineCnt = 0;
				boolean handleLineOkForCell = false;
				long totalDataNum = 0;
				DateUtil dateUtil1 = new DateUtil();

				long descId = 0;
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

					handleLineOkForCell = handleLine(sps, fieldCnt, cellPois, cellDbFieldsToTitles, insertCellTStmt,
							dateUtil1, baseDateStr);

					if (handleLineOkForCell == true) {
						executeCnt++;
						addLineCnt++;
						/* if(addLineCnt==1){ */
						descId = getNextSeqValue("SEQ_" + rnoCellFlowTable, connection);
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
							// connection.rollback(savepoint);
							e.printStackTrace();
						}
					}
				} while (!StringUtils.isBlank(line));
				// 执行
				if (executeCnt > 0) {
					insertCellTStmt.executeBatch();
				}

				log.debug("流量数据文件：" + tmpFileName + "共有：" + totalDataNum + "行记录数据,共入库=" + addLineCnt + "行记录");

			} catch (Exception e) {
				e.printStackTrace();
				/*
				 * try {
				 * connection.rollback(savepoint);
				 * } catch (SQLException e1) {
				 * e1.printStackTrace();
				 * }
				 */
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
		}
		return true;
	}

	private boolean handleLine(String[] sps, int expectFieldCnt, Map<Integer, String> pois,
			Map<String, DBFieldToTitle> dbFtts, PreparedStatement insertStatement, DateUtil dateUtil, String baseDateStr) {

		/*
		 * for(int m = 0; m<sps.length;m++){
		 * if(sps[m].equals("")||sps[m]==null){
		 * return false;
		 * }
		 * }
		 */
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
			if (dt != null) {
				setIntoPreStmt(insertStatement, dt, sps[i], dateUtil);
			}
		}
		return true;
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

	private void setIntoPreStmt(PreparedStatement pstmt, DBFieldToTitle dt, String val, DateUtil dateUtil) {
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
}
