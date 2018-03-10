package com.iscreate.op.service.rno.task.pci;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import oracle.jdbc.OracleConnection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobID;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.springframework.jdbc.support.nativejdbc.C3P0NativeJdbcExtractor;

import com.iscreate.op.dao.rno.RnoStructureQueryDao;
import com.iscreate.op.dao.rno.RnoStructureQueryDaoImpl;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.BaseJobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.mapreduce.IscreateHadoopBaseJob;
import com.iscreate.op.service.rno.mapreduce.pci.PciImportMapper;
import com.iscreate.op.service.rno.mapreduce.pci.PciImportReducer;
import com.iscreate.op.service.rno.parser.DataParseStatus;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.HadoopUser;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;

public class RnoPciPlanImportJobRunnable extends BaseJobRunnable {
	private static Log log = LogFactory.getLog(RnoPciPlanImportJobRunnable.class);
	private static String jobType = "RNO_PCI_PLAN_IMPORT";
	RnoStructureQueryDao structureQueryDao = null;
	private Connection connection;
	private Statement stmt;
	private long jobId = 0;

	@Override
	public boolean isMyJob(JobProfile job) {
		if (job == null) {
			return false;
		} else {
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
			try {
				stmt.close();
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			return status;
		}

		// 准备
		// String fileName = dataRec.getFileName();
		// hdfs:///op/rno/upload/2015-05-20/upload__4a5d259a_14d6f35d892__8000_00000004.tmp
		String pciOriPath = FileInterpreter.makeFullPath(dataRec.getFullPath());

		// =============================>>>>>>>>>>>>>>>>>>>>>>>保存job与参数信息开始<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<=================
		// 获取sessioin
		/*
		 * SessionService.getInstance().saveSession("session",
		 * ServletActionContext.getRequest() .getSession());在某个位置先贮存
		 */
		@SuppressWarnings("static-access")
		HttpSession session = SessionService.getInstance().getSession();
		// 在判断器里保存
		String lteCells = session.getAttribute("pciCell").toString();
		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) session.getAttribute("MRTASKINFO");
		// 保存需要优化的小区列
		taskobj.getTaskInfo().setLteCells(lteCells);
		if (taskobj != null) {
			List<RnoThreshold> rnoThresholds = taskobj.getRnoThresholds();
			RnoLteInterferCalcTask.TaskInfo taskInfo = taskobj.getTaskInfo(); // 任务信息
			// 更新任务名，由于是导入任务被默认分配了导入的任务名
			job.setJobName(taskobj.getTaskInfo().getTaskName());
			// 下载文件名
			String dlFileName = "";
			if (!(taskInfo.getIsExportAssoTable().equals("NO") && taskInfo.getIsExportMidPlan().equals("NO") && taskInfo
					.getIsExportNcCheckPlan().equals("NO"))) {
				dlFileName = jobId + "_PCI优化.zip";
			} else {
				dlFileName = jobId + "_PCI优化方案.xlsx";
			}
			// 读取文件名
			String rdFileName = jobId + "_pci_data";
			// 创建日期
			Calendar cal = Calendar.getInstance();
			String createTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
			// 文件保存路径
			String resultDir = "hdfs:///"+HadoopUser.homeOfUser()+"rno_data/pci/" + cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.MONTH) + 1);
			String dataFilePath = "hdfs:///"+HadoopUser.homeOfUser()+"rno_data/pci/" + cal.get(Calendar.YEAR) + "/"
					+ (cal.get(Calendar.MONTH) + 1) + "/" + jobId + "_pci_cal_data";
			String finishState = "排队中";
			// 更新日期
			String modTime = createTime;

			// 保存对应的门限值
			String paramVal;
			String paramCode;
			PreparedStatement pstmt = null;
			String threshHoldSql = "insert into rno_lte_pci_job_param (JOB_ID," + "PARAM_TYPE," + "PARAM_CODE,"
					+ "PARAM_VAL) values(" + jobId + ",?," + "?," + "?)";
			try {
				pstmt = connection.prepareStatement(threshHoldSql);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			try {
				for (RnoThreshold rnoThreshold : rnoThresholds) {
					paramCode = rnoThreshold.getCode();
					paramVal = rnoThreshold.getDefaultVal();

					if (!StringUtils.isBlank(paramCode) && !StringUtils.isBlank(paramVal)) {
						try {
							pstmt.setString(1, "PCI_THRESHOLD");
							pstmt.setString(2, paramCode);
							pstmt.setString(3, paramVal);
							pstmt.addBatch();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}

				// 执行
				try {
					pstmt.executeBatch();
				} catch (SQLException e) {
					e.printStackTrace();
					log.error("jobId=" + jobId + "，保存PCI规划参数失败！");
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

				String insertSql = "insert into rno_lte_pci_job	(JOB_ID," + "BEG_MEA_TIME," + "END_MEA_TIME,"
						+ "CITY_ID," + "DL_FILE_NAME," + "RD_FILE_NAME," + "RESULT_DIR," + "FINISH_STATE," + "STATUS,"
						+ "CREATE_TIME,"
						+ "MOD_TIME,OPTIMIZE_CELLS,PLAN_TYPE,CONVER_TYPE,RELA_NUM_TYPE,DATA_FILE_PATH,IS_CHECK_NCELL,"
						+ "IS_EXPORT_ASSOTABLE,IS_EXPORT_MIDPLAN,IS_EXPORT_NCCHECKPLAN)" + "	    values											"
						+ "		  ("
						+ jobId
						+ ",											"
						+ "		   to_date('"
						+ begMeaTime
						+ "', 'yyyy-MM-dd HH24:mi:ss'),	"
						+ "		   to_date('"
						+ endMeaTime
						+ "', 'yyyy-MM-dd HH24:mi:ss'),	"
						+ "		   "
						+ cityId
						+ ",											"
						+ "		   '"
						+ dlFileName
						+ "',											"
						+ "		   '"
						+ rdFileName
						+ "',											"
						+ "		   '"
						+ resultDir
						+ "',											"
						+ "		   '"
						+ finishState
						+ "',											"
						+ "		   'N',												"
						+ "		   to_date('"
						+ createTime
						+ "', 'yyyy-MM-dd HH24:mi:ss'),"
						+ "		   to_date('"
						+ modTime
						+ "', 'yyyy-MM-dd HH24:mi:ss'),?,?,?,?,?,?,?,?,?)";

				pstmt = connection.prepareStatement(insertSql);

				// 将conn转为oracle的conn
				C3P0NativeJdbcExtractor c3p0NativeJdbcExtractor = new C3P0NativeJdbcExtractor();
				OracleConnection conn = (OracleConnection) c3p0NativeJdbcExtractor.getNativeConnection(connection);
				// 获取clob对象
				oracle.sql.CLOB clob = oracle.sql.CLOB.createTemporary(conn, false, oracle.sql.CLOB.DURATION_SESSION);
				clob.open(oracle.sql.CLOB.MODE_READWRITE);
				clob.setString(3, optimizeCells);

				pstmt.setClob(1, clob);
				pstmt.setString(2, planType);
				pstmt.setString(3, converType);
				pstmt.setString(4, relaNumType);
				pstmt.setString(5, dataFilePath);
				pstmt.setString(6, isCheckNCell);
				pstmt.setString(7, isExportAssoTable);
				pstmt.setString(8, isExportMidPlan);
				pstmt.setString(9, isExportNcCheckPlan);
				pstmt.addBatch();

				// 执行
				try {
					pstmt.executeBatch();
				} catch (SQLException e) {
					e.printStackTrace();
					log.error("jobId=" + jobId + "，保存PCI规划任务失败！");
				}
			} catch (Exception ee) {
				ee.printStackTrace();
				log.error("jobId=" + jobId + "，保存PCI规划任务失败！");
			} finally {
				try {
					if (pstmt != null)
						pstmt.clearBatch();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				try {
					if (pstmt != null)
						pstmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			// 清空session
			session.removeAttribute("MRTASKINFO");
		}
		// =============================>>>>>>>>>>>>>>>>>>>>>>>保存job与参数信息结束<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<=================

		// 更正job的名称，因为采用导入文件形式启动的runnable，所以任务名被默认了
		sql = "update rno_job  set job_name='" + job.getJobName() + "' " + " where job_id=" + job.getJobId();
		try {
			stmt.executeUpdate(sql);
		} catch (SQLException e3) {
			e3.printStackTrace();
		}

		// 通过jobId获取干扰矩阵计算记录信息
		List<Map<String, Object>> pciPlanRec = structureQueryDao.queryPciPlanJobRecWithCreateTimeDescByJobId(stmt,
				jobId);

		if (pciPlanRec.size() <= 0) {
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

		long cityId = Long.parseLong(pciPlanRec.get(0).get("CITY_ID").toString());
		String startMeaDate = pciPlanRec.get(0).get("BEG_MEA_TIME").toString();
		String endMeaDate = pciPlanRec.get(0).get("END_MEA_TIME").toString();
		String filePath = pciPlanRec.get(0).get("RESULT_DIR").toString();
		String planType = pciPlanRec.get(0).get("PLAN_TYPE").toString();
		String converType = pciPlanRec.get(0).get("CONVER_TYPE").toString();
		// String relaNumType = pciPlanRec.get(0).get("RELA_NUM_TYPE").toString();
		String isCheckNCell = pciPlanRec.get(0).get("IS_CHECK_NCELL").toString();
		String optimizeCells = pciPlanRec.get(0).get("OPTIMIZE_CELLS").toString();
		String rdFileName = pciPlanRec.get(0).get("RD_FILE_NAME").toString();
		String dataFilePath = pciPlanRec.get(0).get("DATA_FILE_PATH").toString();
		String isExportAssoTable = pciPlanRec.get(0).get("IS_EXPORT_ASSOTABLE").toString();
		String isExportMidPlan = pciPlanRec.get(0).get("IS_EXPORT_MIDPLAN").toString();
		String isExportNcCheckPlan = pciPlanRec.get(0).get("IS_EXPORT_NCCHECKPLAN").toString();

		DateUtil dateUtil = new DateUtil();
		long startMeaMillis = dateUtil.parseDateArbitrary(startMeaDate).getTime();
		long endMeaMillis = dateUtil.parseDateArbitrary(endMeaDate).getTime();

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
			try {
				stmt.close();
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
		// System.setProperty("HADOOP_USER_NAME", "rnohbase");

		Configuration conf = new YarnConfiguration();
		// hbase所用到的表
		/*
		 * conf.set("4GERIMRTAB","RNO_4G_ERI_MR");
		 * conf.set("4GZTEMRTAB","RNO_4G_ZTE_MR");
		 * conf.set("4GERIHOTAB","RNO_4G_ERI_HO");
		 * conf.set("4GZTEHOTAB","RNO_4G_ZTE_HO");
		 */

		String cellToLngLatStr = map2String(cellToLngLat);

		conf.set("cellToLngLat", cellToLngLatStr);

		conf.set("RESULT_DIR", filePath);
		conf.set("RD_FILE_NAME", rdFileName);
		conf.set("PLAN_TYPE", planType);
		conf.set("CONVER_TYPE", converType);
		conf.set("OPTIMIZE_CELLS", optimizeCells);
		conf.set("IS_CHECK_NCELL", isCheckNCell);
		conf.set("DATA_FILE_PATH", dataFilePath);
		conf.set("IS_EXPORT_ASSOTABLE", isExportAssoTable);
		conf.set("IS_EXPORT_MIDPLAN", isExportMidPlan);
		conf.set("IS_EXPORT_NCCHECKPLAN", isExportNcCheckPlan);

		// 生成文件路径
		/*
		 * String idxHdfsPath = filePath + File.separator + "ncs_res_" +
		 * matrixRecId + RnoConstant.ReportConstant.INTERMATRIXIDXSUFFIX; String
		 * dataHdfsPath = filePath + File.separator + "ncs_res_" + matrixRecId +
		 * RnoConstant.ReportConstant.INTERMATRIXDATASUFFIX; conf.set("idxPath",
		 * idxHdfsPath); conf.set("dataPath", dataHdfsPath);
		 */

		// 先获取页面自定义的阈值门限
		List<RnoThreshold> rnoThresholds = new ArrayList<RnoThreshold>();
		sql = "select  job_id,param_type,param_code,param_val from rno_lte_pci_job_param where job_id=" + jobId;
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
		String ks = "-1";

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
			try {
				stmt.close();
				connection.close();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			return status;
		}
		/*
		 * pciJob.setJarByClass(IscreateHadoopBaseJob.class);
		 * pciJob.setReducerClass(PciReducer.class);
		 * pciJob.setOutputKeyClass(Text.class);
		 * pciJob.setOutputValueClass(Text.class);
		 */

		Path path = new Path(filePath + "/out");
		try {
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
			try {
				stmt.close();
				connection.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return status;
		}
		log.debug("组装rowkey：cityId=" + cityId + ",开始日期毫秒表示=" + startMeaMillis + ",结束日期毫秒表示=" + endMeaMillis);
		// 结果输出路径
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
		pciJob.setMapperClass(PciImportMapper.class);
		pciJob.setReducerClass(PciImportReducer.class);
		pciJob.setOutputKeyClass(Text.class);
		pciJob.setOutputValueClass(Text.class);
		try {
			FileInputFormat.addInputPath(pciJob, new Path(pciOriPath));
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
		log.debug("注册集群用于 pci规划干扰计算的job...");
		String mrJobId = "";
		try {
			pciJob.submit();
			int progMonitorPollIntervalMillis = Job.getProgressPollInterval(conf);
			JobID pciJobId = pciJob.getJobID();
			mrJobId = pciJob.getJobID().toString();
			// 鑾峰彇reduce鐨刯obid瀛樺偍鍏te pci job鐨勪互澶囧悗闈㈣幏鍙栧仠姝㈣job
			// 閫氳繃涓昏繘绋婭D鏇存柊reduce鐨刯ob_id
			structureQueryDao.addMapReduceJobId(stmt, jobId, mrJobId);
			// 通过mrjobID存储job对象，
			log.debug("当前mrJobId为:" + mrJobId + "-----当前mrJob为:" + pciJob + "的信息!");

			session.setAttribute(mrJobId, pciJob);
			while (!pciJob.isComplete()) {
				log.info("jobId=" + pciJobId + ",名称=" + job.getJobName() + "执行中...");
				log.info(" map " + org.apache.hadoop.util.StringUtils.formatPercent(pciJob.mapProgress(), 0)
						+ " reduce " + org.apache.hadoop.util.StringUtils.formatPercent(pciJob.reduceProgress(), 0));

				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Parsing.toString()
						+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					// connection.commit();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				Thread.sleep(progMonitorPollIntervalMillis);
			}
			// job.waitForCompletion(true);
			boolean calcResFlag = true;
			if (pciJob.isSuccessful()) {
				// 读取结果
				config.readDataFromFile();
				// 开始计算
				PciCalc pciCalc = new PciCalc(config);
				try {
					pciCalc.execCalc();
				} catch (Exception e) {
					calcResFlag = false;
					e.printStackTrace();
				}
				if (calcResFlag) {
					log.info("集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务成功！");
					log.info("pci规划干扰计算完成");
					// 保存报告信息
					report.setStage("MapReduce计算结束，成功");
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("成功");
					report.setAttMsg("集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务成功！");

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

					// 数据记录本身的状态
					sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Succeded.toString()
							+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
					try {
						stmt.executeUpdate(sql);
						// connection.commit();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				} else {

					log.info("集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！");
					log.info("pci规划干扰计算完成");
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

					// 数据记录本身的状态
					sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
							+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
					try {
						stmt.executeUpdate(sql);
						// connection.commit();
					} catch (Exception e2) {
						e2.printStackTrace();
					}
				}
			} else {
				log.info("集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！");
				log.info("pci规划干扰计算失败");
				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					// connection.commit();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				// 保存报告信息
				report.setStage("MapReduce计算结束，未成功完成");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("集群jobId=" + pciJobId + ",名称=" + job.getJobName() + ",结果：任务失败！");
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
				// connection.commit();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
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
		} catch (IOException e) {
			e.printStackTrace();
			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
				// connection.commit();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
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
			// 数据记录本身的状态
			sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
					+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
			try {
				stmt.executeUpdate(sql);
				// connection.commit();
			} catch (Exception e2) {
				e2.printStackTrace();
			}
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
				// 数据记录本身的状态
				sql = "update rno_data_collect_rec set FILE_STATUS='" + DataParseStatus.Failall.toString()
						+ "' where DATA_COLLECT_ID=" + dataRec.getDataCollectId();
				try {
					stmt.executeUpdate(sql);
					// connection.commit();
				} catch (Exception e3) {
					e3.printStackTrace();
				}
				report.setStage("删除 MRJOB ");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("通过集群jobId=" + mrJobId + ",删除MRJOB失败");
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

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
		}
		/************ Hadoop pci规划干扰计算，并保存结果文件 end ************/

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
			// 更新pci规划干扰计算表的进度
			structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, jobStatus.getJobState().getCode());
		}
	}

	public void updateOwnProgress(String jobStatus) {
		if (stmt != null && structureQueryDao != null) {
			// 更新pci规划干扰计算表的进度
			structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, jobStatus);
		}
	}

	public static String map2String(Map<String, String> enodebToCells) {
		String str = "";
		for (String key : enodebToCells.keySet()) {
			str += key + "=" + enodebToCells.get(key) + "|";
		}
		return str.substring(0, str.length() - 1);
	}

	/*	*//**
	 * 从hdfs文件中读取数据
	 * 
	 * @param pciCalc
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

}
