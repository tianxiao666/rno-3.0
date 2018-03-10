package com.iscreate.op.service.rno.task.pci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.URI;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

import com.iscreate.op.dao.rno.RnoLtePciDao;
import com.iscreate.op.dao.rno.RnoLtePciDaoImpl;
import com.iscreate.op.pojo.rno.RnoDataCollectRec;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.client.JobRunnable;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.parser.jobmanager.FileInterpreter;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.HBTable;
import com.iscreate.op.service.rno.tool.RnoHelper;

public class NewPciConfig {
	private Configuration conf;
	private double m3r = 1;
	private double m6r = 0.8;
	private double m30r = 0.1;
	private double topRate = 0.1; // top n%
	private double defInterRate = 0.05; // 给定的干扰差值比例m
	private double defVariance = 0.05; // 给定的方差值
	private int divideNumber = 10;
	private double dtKs = -1.0;

	private String planType = "ONE"; // 评估方案，默认1
	private String schemeType = "ONE"; // 收敛方案，默认1
	private boolean isCheckNCell; // 邻区核查，默认进行
	private boolean isExportAssoTable;// 关联表输出，默认不输出
	private boolean isExportMidPlan;// 中间方案输出，默认不输出
	private boolean isExportNcCheckPlan;// 邻区核查方案输出，默认不输出

	private boolean isUseSf = false;

	private String d1D2Type = "";

	private String d1Freq = "";

	private String d2Freq = "";

	private long jobId = -1;
	private String filePath = "";
	private String fileName = "";
	private String dataFilePath = "";
	private String redOutPath = ""; // reduce缺省输出目录
	// 保存返回信息
	private String returnInfo = "";

	/** 距离限制，单位米 **/
	private double dislimit = 5000.0;

	/** 权值 **/
	private double samefreqcellcoefweight = 0.8;

	/** 切换比例权值 **/
	private double switchratioweight = 0.2;

	/** 合并后最小的测量总数 **/
	private long minmeasuresum = 0;

	/** 合并后最小的关联度（百分比） **/
	private double mincorrelation = 0.0;

	private List<String> cellsNeedtoAssignList;

	private Map<String, List<String>> enodebToCells;

	private Map<String, String> cell2Enodeb;

	/**
	 * 小区与同站其他小区列的映射，同站其他小区已按关联度从大到小排列 <br>
	 * 比如key为1 <br>
	 * 干扰矩阵为 <br>
	 * 1->2 <br>
	 * 1->3 <br>
	 * 同站
	 **/
	private Map<String, List<String>> cellToSameStationOtherCells = new HashMap<String, List<String>>();

	/**
	 * 小区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 <br>
	 * 比如key为1 、 <br>
	 * 干扰矩阵为 <br>
	 * 1->12 <br>
	 * 1->23 <br>
	 * 非同站
	 **/
	private Map<String, List<String>> cellToNotSameStationCells = new HashMap<String, List<String>>();

	/**
	 * 邻区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 <br>
	 * 比如key为1 <br>
	 * 干扰矩阵为 <br>
	 * 12->1 <br>
	 * 23->1 <br>
	 * 非同站
	 **/
	private Map<String, List<String>> ncellToNotSameStationCells = new HashMap<String, List<String>>();

	/** 小区与邻区关联度的映射（包含了同站其他小区） 以主小区为key **/
	private Map<String, Map<String, Double>> cellToNcellAssocDegree = new HashMap<String, Map<String, Double>>();

	/** 小区与邻区关联度的映射 以邻小区为key **/
	private Map<String, Map<String, Double>> ncellToCellAssocDegree = new HashMap<String, Map<String, Double>>();

	/** 小区与小区总关联度的映射 **/
	private Map<String, Double> cellToTotalAssocDegree = new HashMap<String, Double>();

	/** 小区与原PCI的映射 **/
	private Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();

	/** 小区到经纬度的映射，不重复 **/
	private Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();

	/** 小区与频率的映射 **/
	private Map<String, Integer> cellToEarfcn = new HashMap<String, Integer>();

	/** d1小区列表 **/
	private List<String> abCellList = new ArrayList<String>();

	private List<String> fileNameList = new ArrayList<String>();
	/** 文件名与ID的映射 **/
	private Map<String, String> fn2Id = new HashMap<String, String>();
	/** 小区到KS的映射 **/
	private Map<String, Double> cellToKs = new HashMap<String, Double>();

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

	private static List<String> sfTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// SF数据
			add(HBTable.valueOf("RNO_4G_SF"));
		}
	};
	// 拼装scan
	private List<Scan> mrScans = new ArrayList<Scan>();
	private List<Scan> hoScans = new ArrayList<Scan>();
	private List<Scan> sfScans = new ArrayList<Scan>();
	// 干扰矩阵上传文件路径
	private String matrixDfPath;

	public NewPciConfig(long jobId) {
		this.jobId = jobId;
	}

	@SuppressWarnings("unchecked")
	public boolean buildPciTaskConf(JobRunnable jobWorker, JobStatus status, JobReport report, Connection connection,
			Statement updateStatusStmt, Date startTime) {
		RnoLtePciDao rnoLtePciDao = new RnoLtePciDaoImpl();

		// 通过 jobId 获取干扰矩阵计算记录信息(rno_lte_pci_job表），包括变小区的 CLOB 信息
		String sql = "select * from rno_lte_pci_job where JOB_ID =" + jobId + "order by CREATE_TIME desc";

		List<Map<String, Object>> rec = RnoHelper.commonQuery(updateStatusStmt, sql);

		if (rec == null || rec.isEmpty()) {
			report.setStage("获取任务配置信息");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("任务配置信息不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}

		// List<Map<String, Object>> pciPlanRec = rnoLtePciDao.queryPciPlanJobRecByJobId(updateStatusStmt, jobId);

		Map<String, Object> pciPlanRec = rec.get(0);

		if (pciPlanRec.isEmpty()) {
			report.setStage("获取任务配置信息");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("任务配置信息不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}

		String optimizeCells = null;
		try {
			Clob clob = (Clob) pciPlanRec.get("OPTIMIZE_CELLS");
			optimizeCells = clob == null ? "" : clob.getSubString(1, (int) clob.length());
		} catch (SQLException e) {
			e.printStackTrace();
			report.setStage("获取任务配置信息");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("任务配置信息不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}

		String cellArr[] = null;
		if (optimizeCells != null && !"".equals(optimizeCells.trim())) {
			cellArr = optimizeCells.split(",");
			if (cellArr.length == 0) {
				// 保存报告信息
				report.setStage("变PCI小区字符串逗号分割后的长度为０,不满足基本需求！");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("变PCI小区字符串逗号分割后的长度为０,不满足基本需求！");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return false;
			}
		} else {
			// 保存报告信息
			report.setStage("变PCI小区字符串为NULL！");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("变PCI小区字符串为NULL！");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}
		this.cellsNeedtoAssignList = Arrays.asList(cellArr);

		long cityId = Long.parseLong(pciPlanRec.get("CITY_ID").toString());
		// 获取指定城市的全部同站小区，以 ENODEBID_EARFCN 为 key，多个小区CELL以#连接，一般一个ENODEBID_EARFCN的同站小区是三个。
		// 这是从 RNO_LTE_ENODEB 表中获取的，或者说是从公参表中获取的
		// @author chao.xj 2015-5-6 下午2:48:25
		// 同站小区判断条件变更由 enodeb->enodeb+earfcn
		this.enodebToCells = rnoLtePciDao.getEnodebIdForCellsMap(updateStatusStmt, cityId);

		if (enodebToCells == null) {
			report.setStage("通过城市ID获取从基站标识到lte小区的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}
		this.cell2Enodeb = rnoLtePciDao.getCell2EnodebIdMap(updateStatusStmt, cityId);

		if (cell2Enodeb == null) {
			report.setStage("通过城市ID获取lte小区到基站的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}

		Map<String, Object> cellToParameter = rnoLtePciDao.getParameterForCellsMap(updateStatusStmt, cityId);

		if (cellToParameter == null || cellToParameter.isEmpty()) {
			report.setStage("通过城市ID获取小区到经纬度的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return false;
		}
		this.cellToLonLat = (Map<String, double[]>) cellToParameter.get("cellToLonLat");
		this.cellToOriPci = (Map<String, Integer>) cellToParameter.get("cellToOriPci");
		this.cellToEarfcn = (Map<String, Integer>) cellToParameter.get("cellToEarfcn");

		String startMeaDate = pciPlanRec.get("BEG_MEA_TIME").toString();
		String endMeaDate = pciPlanRec.get("END_MEA_TIME").toString();

		// 干扰矩阵和流量文件上传ID
		long matrixDcId = pciPlanRec.get("MATRIX_DATA_COLLECT_ID") == null ? 0 : Long.parseLong(pciPlanRec.get(
				"MATRIX_DATA_COLLECT_ID").toString());

		long flowDcId = pciPlanRec.get("FLOW_DATA_COLLECT_ID") == null ? 0 : Long.parseLong(pciPlanRec.get(
				"FLOW_DATA_COLLECT_ID").toString());

		this.filePath = pciPlanRec.get("RESULT_DIR").toString();
		this.redOutPath = filePath + "/out";
		this.fileName = pciPlanRec.get("RD_FILE_NAME").toString();
		this.dataFilePath = pciPlanRec.get("DATA_FILE_PATH").toString();
		this.planType = pciPlanRec.get("PLAN_TYPE").toString();
		this.schemeType = pciPlanRec.get("CONVER_TYPE").toString();
		this.isCheckNCell = "YES".equals(pciPlanRec.get("IS_CHECK_NCELL").toString().toUpperCase());
		this.isExportAssoTable = "YES".equals(pciPlanRec.get("IS_EXPORT_ASSOTABLE").toString().toUpperCase());
		this.isExportMidPlan = "YES".equals(pciPlanRec.get("IS_EXPORT_MIDPLAN").toString().toUpperCase());
		this.isExportNcCheckPlan = "YES".equals(pciPlanRec.get("IS_EXPORT_NCCHECKPLAN").toString().toUpperCase());
		this.fileNameList = Arrays.asList(null == pciPlanRec.get("SF_FILE_NAMES") ? new String[0] : pciPlanRec
				.get("SF_FILE_NAMES").toString().trim().split(","));
		// System.out.println("fileNameList=" + fileNameList);
		if (!fileNameList.isEmpty() && !fileNameList.get(0).isEmpty()) {
			String inStr = "";
			for (String fn : fileNameList) {
				inStr += "'" + fn + "',";
			}
			inStr = inStr.substring(0, inStr.length() - 1);
			inStr = "(" + inStr + ")";
			sql = "select file_name,rec_id from rno_lte_sf_file_rec where file_name in " + inStr + " and area_id="
					+ cityId + " order by mod_time";
			ResultSet rs;
			try {
				rs = updateStatusStmt.executeQuery(sql);
				while (rs.next()) {
					fn2Id.put(rs.getString(1), "fn*" + rs.getLong(2) + "*fn");
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			isUseSf = true;
			this.d1D2Type = null == pciPlanRec.get("FREQ_ADJ_TYPE") ? "" : pciPlanRec.get("FREQ_ADJ_TYPE").toString();
			if (!d1D2Type.isEmpty()) {
				this.d1Freq = null == pciPlanRec.get("D1FREQ") ? "" : pciPlanRec.get("D1FREQ").toString();
				this.d2Freq = null == pciPlanRec.get("D2FREQ") ? "" : pciPlanRec.get("D2FREQ").toString();
			}
		}
		// System.out.println("isUseSf=" + isUseSf);

		/************ Hadoop PCI干扰计算 start ************/

		this.conf = new YarnConfiguration();
		conf.set("CALC_TYPE", "PCI");
		conf.setLong("jobId", jobId);

		// 工参中的小区列表，用于匹配数据
		conf.set("cellList", map2String((List<String>) cellToParameter.get("cellList")));

		// 设置其他数据
		conf.set("D1D2_TYPE", d1D2Type);
		conf.set("D1Freq", d1Freq);
		conf.set("D2Freq", d2Freq);
		// 方案保存的文件路径
		conf.set("RESULT_DIR", filePath);
		conf.set("RD_FILE_NAME", fileName);
		conf.set("DATA_FILE_PATH", dataFilePath);

		// 获取页面自定义的阈值门限值
		List<RnoThreshold> rnoThresholds = new ArrayList<RnoThreshold>();
		sql = "select job_id, param_type, param_code, param_val from rno_lte_pci_job_param where job_id = " + jobId;
		List<Map<String, Object>> rawDatas = RnoHelper.commonQuery(updateStatusStmt, sql);

		if (rawDatas == null || rawDatas.size() == 0) {
			// 取默认门限值
			sql = "select " + jobId
					+ " as job_id, 'STRUCTANA' as param_type, CODE as param_code, DEFAULT_VAL as param_val"
					+ " from RNO_THRESHOLD where module_type = 'LTEINTERFERCALC'";
			rawDatas = RnoHelper.commonQuery(updateStatusStmt, sql);
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

		double samefreqcellcoefweight = 0.8; // 权值
		double switchratioweight = 0.2; // 切换比例权值
		double mincorrelation = 2;
		int minmeasuresum = 500;

		if (rnoThresholds != null) {
			for (RnoThreshold rnoThreshold : rnoThresholds) {
				String code = rnoThreshold.getCode();
				String val = rnoThreshold.getDefaultVal();
				if (code.equals("SAMEFREQCELLCOEFWEIGHT".toUpperCase())) {
					samefreqcellcoefweight = Double.parseDouble(val);
				}
				if (code.equals("SWITCHRATIOWEIGHT".toUpperCase())) {
					switchratioweight = Double.parseDouble(val);
				}
				if (code.equals("CELLM3RINTERFERCOEF".toUpperCase())) {
					this.m3r = Double.parseDouble(val);
				}
				if (code.equals("CELLM6RINTERFERCOEF".toUpperCase())) {
					this.m6r = Double.parseDouble(val);
				}
				if (code.equals("CELLM30RINTERFERCOEF".toUpperCase())) {
					this.m30r = Double.parseDouble(val);
				}
				if (code.equals("TOPNCELLLIST".toUpperCase())) {
					this.topRate = Double.parseDouble(val) * 0.01;
				}
				if (code.equals("CONVERMETHOD1TARGETVAL".toUpperCase())) {
					this.defInterRate = Double.parseDouble(val) * 0.01;
				}
				if (code.equals("CONVERMETHOD2TARGETVAL".toUpperCase())) {
					this.defVariance = Double.parseDouble(val) * 0.01;
				}
				if (code.equals("CONVERMETHOD2SCOREN".toUpperCase())) {
					this.divideNumber = Integer.parseInt(val);
				}
				if (code.equals("MINCORRELATION".toUpperCase())) {
					mincorrelation = Double.parseDouble(val);
				}
				if (code.equals("MINMEASURESUM".toUpperCase())) {
					minmeasuresum = Integer.parseInt(val);
				}
				if (code.equals("DISLIMIT".toUpperCase())) {
					this.dislimit = Double.parseDouble(val);
				}
			}
		}

		conf.setDouble("samefreqcellcoefweight", samefreqcellcoefweight);
		conf.setDouble("switchratioweight", switchratioweight);
		conf.setDouble("mincorrelation", mincorrelation);
		conf.setInt("minmeasuresum", minmeasuresum);
		conf.setDouble("dislimit", dislimit);

		System.out.println("门限值：" + "samefreqcellcoefweight=" + samefreqcellcoefweight + ",switchratioweight="
				+ switchratioweight + ",cellm3rinterfercoef=" + m3r + ",cellm6rinterfercoef=" + m6r
				+ ",cellm30rinterfercoef=" + m30r + ",topncelllist=" + topRate + ",convermethod1targetval="
				+ defInterRate + ",convermethod2targetval=" + defVariance + ",convermethod2scoren=" + divideNumber
				+ ",mincorrelation=" + mincorrelation + ",minmeasuresum=" + minmeasuresum + ",dislimit=" + dislimit);

		// 如果流量文件ID大于0 获取流量文件信息；
		if (flowDcId > 0) {
			// 获取修正值
			this.dtKs = pciPlanRec.get("KS_CORR_VAL") == null ? -1 : Double.parseDouble(pciPlanRec.get("KS_CORR_VAL")
					.toString());
			if (!(dtKs > 0)) {
				dtKs = -1;
			}
			// 获取job相关的信息
			sql = "select * from RNO_DATA_COLLECT_REC where Data_Collect_Id=" + flowDcId;
			List<Map<String, Object>> recs = RnoHelper.commonQuery(updateStatusStmt, sql);
			if (recs != null && recs.size() > 0) {
				// 流量文件路径
				RnoDataCollectRec flowDataRec = RnoHelper.commonInjection(RnoDataCollectRec.class, recs.get(0),
						new DateUtil());
				if (flowDataRec == null) {
					status.setJobState(JobState.Failed);
					status.setUpdateTime(new Date());
					// 报告
					report.setFinishState("失败");
					report.setStage("获取流量文件");
					report.setBegTime(new Date());
					report.setReportType(1);
					report.setAttMsg("转换RnoDataCollectRec失败！");
					return false;
				}
				LteFlowFileParser parser = new LteFlowFileParser(jobWorker, status, report, cellToKs, connection, dtKs);
				if (!parser.parse(flowDataRec)) {
					status.setJobState(JobState.Failed);
					status.setUpdateTime(new Date());
					// 报告
					report.setFinishState("失败");
					report.setStage("获取流量文件");
					report.setBegTime(new Date());
					report.setReportType(1);
					report.setAttMsg("解析流量文件失败！");
					return false;
				}
			} else {
				// 失败了
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());

				// 报告
				report.setFinishState("失败");
				report.setStage("获取流量文件");
				report.setBegTime(new Date());
				report.setReportType(1);
				report.setAttMsg("未找到流量文件！");
				return false;
			}
		}
		// 获取干扰矩阵导入计算信息
		if (matrixDcId > 0) {
			// 获取job相关的信息
			sql = "select Full_Path from RNO_DATA_COLLECT_REC where Data_Collect_Id=" + matrixDcId;
			List<Map<String, Object>> recs = RnoHelper.commonQuery(updateStatusStmt, sql);
			if (recs == null || recs.size() <= 0) {
				// 失败了
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());

				// 报告
				report.setFinishState("失败");
				report.setStage("获取干扰矩阵文件");
				report.setBegTime(new Date());
				report.setReportType(1);
				report.setAttMsg("未找到干扰矩阵文件！");
				return false;
			}
			this.matrixDfPath = FileInterpreter.makeFullPath(recs.get(0).get("FULL_PATH").toString());
		} else {
			// 数据源表名
			// System.out.println("tableNames=" + tableNames);
			// System.out.println("======>>>测量数据开始时间和结束时间：" + startMeaDate + ", " + endMeaDate);
			DateUtil dateUtil = new DateUtil();
			long startMeaMillis = dateUtil.parseDateArbitrary(startMeaDate).getTime();
			long endMeaMillis = dateUtil.parseDateArbitrary(endMeaDate).getTime();
			// 拼装scan
			mrScans = getScans(cityId, startMeaMillis, endMeaMillis, mrTableNames);
			hoScans = getScans(cityId, startMeaMillis, endMeaMillis, hoTableNames);
			sfScans = getScans(cityId, startMeaMillis, endMeaMillis, sfTableNames);
			// System.out.println("scans=" + scans);
		}
		return true;
	}

	public Configuration getConf() {
		return conf;
	}

	public List<Scan> getMrScans() {
		return mrScans;
	}

	public List<Scan> getHoScans() {
		return hoScans;
	}

	public List<Scan> getSfScans() {
		return sfScans;
	}

	public double getM3r() {
		return m3r;
	}

	public double getM6r() {
		return m6r;
	}

	public double getM30r() {
		return m30r;
	}

	public double getTopRate() {
		return topRate;
	}

	public double getDefInterRate() {
		return defInterRate;
	}

	public double getDefVariance() {
		return defVariance;
	}

	public String getPlanType() {
		return planType;
	}

	public String getSchemeType() {
		return schemeType;
	}

	public Long getJobId() {
		return jobId;
	}

	public String getFilePath() {
		return filePath;
	}

	public String getFileName() {
		return fileName;
	}

	public Double getSamefreqcellcoefweight() {
		return samefreqcellcoefweight;
	}

	public Double getSwitchratioweight() {
		return switchratioweight;
	}

	public long getMinmeasuresum() {
		return minmeasuresum;
	}

	public double getMincorrelation() {
		return mincorrelation;
	}

	public List<String> getCellsNeedtoAssignList() {
		return cellsNeedtoAssignList;
	}

	public Map<String, List<String>> getEnodebToCells() {
		return enodebToCells;
	}

	public int getDivideNumber() {
		return divideNumber;
	}

	public Double getDislimit() {
		return dislimit;
	}

	public Double getDtKs() {
		return dtKs;
	}

	public Map<String, List<String>> getCellToSameStationOtherCells() {
		return cellToSameStationOtherCells;
	}

	public Map<String, List<String>> getCellToNotSameStationCells() {
		return cellToNotSameStationCells;
	}

	public Map<String, List<String>> getNcellToNotSameStationCells() {
		return ncellToNotSameStationCells;
	}

	public Map<String, Map<String, Double>> getCellToNcellAssocDegree() {
		return cellToNcellAssocDegree;
	}

	public Map<String, Map<String, Double>> getNcellToCellAssocDegree() {
		return ncellToCellAssocDegree;
	}

	public Map<String, Double> getCellToTotalAssocDegree() {
		return cellToTotalAssocDegree;
	}

	public Map<String, Integer> getCellToOriPci() {
		return cellToOriPci;
	}

	public Map<String, double[]> getCellToLonLat() {
		return cellToLonLat;
	}

	public Map<String, Integer> getCellToEarfcn() {
		return cellToEarfcn;
	}

	public String getReturnInfo() {
		return returnInfo;
	}

	public void setReturnInfo(String returnInfo) {
		this.returnInfo = returnInfo;
	}

	public String getD1D2Type() {
		return d1D2Type;
	}

	public List<String> getAbCellList() {
		return abCellList;
	}

	public String getD1Freq() {
		return d1Freq;
	}

	public String getD2Freq() {
		return d2Freq;
	}

	public boolean isCheckNCell() {
		return isCheckNCell;
	}

	public boolean isExportAssoTable() {
		return isExportAssoTable;
	}

	public boolean isExportMidPlan() {
		return isExportMidPlan;
	}

	public boolean isExportNcCheckPlan() {
		return isExportNcCheckPlan;
	}

	public String getMatrixDfPath() {
		return matrixDfPath;
	}

	public Map<String, Double> getCellToKs() {
		return cellToKs;
	}

	public String getRedOutPath() {
		return redOutPath;
	}

	public Map<String, String> getCell2Enodeb() {
		return cell2Enodeb;
	}

	/**
	 * 从hdfs文件中读取数据
	 */
	@SuppressWarnings("unchecked")
	public void readDataFromFile() {
		FileSystem hdfs = null;
		BufferedReader br = null;
		ObjectInputStream objis = null;
		String[] lineArr;
		String cellId;
		Map<String, Double> cellsAssocDegree = new HashMap<String, Double>();
		try {
			hdfs = FileSystem.get(conf);
			for (Path path : FileUtil.stat2Paths(hdfs.listStatus(new Path(URI.create(redOutPath))))) {
				if (path.getName().startsWith("part-")) {
					br = new BufferedReader(new InputStreamReader(hdfs.open(path), "UTF-8"));
					String line;
					while (null != (line = br.readLine())) {
						lineArr = line.split(",");
						cellId = lineArr[0].intern();
						cellToTotalAssocDegree.put(cellId.intern(), Double.parseDouble(lineArr[1]));
						if (!cellToNcellAssocDegree.containsKey(cellId)) {
							cellToNcellAssocDegree.put(cellId.intern(), new HashMap<String, Double>());
						}
						cellsAssocDegree.clear();
						for (String ss : lineArr[2].split("#")) {
							cellsAssocDegree.put(ss.split("=")[0].intern(), Double.parseDouble(ss.split("=")[1]));
						}
						cellToNcellAssocDegree.get(cellId).putAll(cellsAssocDegree);
						for (String ncell : cellsAssocDegree.keySet()) {
							if (!ncellToCellAssocDegree.containsKey(ncell)) {
								ncellToCellAssocDegree.put(ncell.intern(), new HashMap<String, Double>());
							}
							ncellToCellAssocDegree.get(ncell).put(cellId.intern(), cellsAssocDegree.get(ncell));
						}
					}
					br.close();
				}
			}
			cellToTotalAssocDegree = sortMapByValue(cellToTotalAssocDegree);

			List<String> sstCells;
			List<String> nsstCells;
			// 邻区关联度从大到小排序
			for (String cell : cellToNcellAssocDegree.keySet()) {
				cellToNcellAssocDegree.put(cell.intern(), sortMapByValue(cellToNcellAssocDegree.get(cell)));

				sstCells = cellToSameStationOtherCells.get(cell);
				if (sstCells == null) {
					sstCells = new ArrayList<String>();
					for (String cellTmp : enodebToCells.get(cell2Enodeb.get(cell))) {
						if (!cellTmp.equals(cell) && cellToEarfcn.get(cell).equals(cellToEarfcn.get(cellTmp))) {
							sstCells.add(cellTmp.intern());
						}
					}
					cellToSameStationOtherCells.put(cell.intern(), sstCells);
				}

				if (!cellToNotSameStationCells.containsKey(cell)) {
					nsstCells = new ArrayList<String>();
					for (String ncell : cellToNcellAssocDegree.get(cell).keySet()) {
						if (!sstCells.contains(ncell) && cellToEarfcn.get(cell).equals(cellToEarfcn.get(ncell))) {
							nsstCells.add(ncell.intern());
						}
					}
					cellToNotSameStationCells.put(cell.intern(), nsstCells);
				}
			}
			// 求以邻区为key的反向映射
			for (String ncell : ncellToCellAssocDegree.keySet()) {
				ncellToCellAssocDegree.put(ncell.intern(), sortMapByValue(ncellToCellAssocDegree.get(ncell)));

				sstCells = cellToSameStationOtherCells.get(ncell);
				if (sstCells == null) {
					sstCells = new ArrayList<String>();
					for (String cellTmp : enodebToCells.get(cell2Enodeb.get(ncell))) {
						if (!cellTmp.equals(ncell) && cellToEarfcn.get(ncell).equals(cellToEarfcn.get(cellTmp))) {
							sstCells.add(cellTmp.intern());
						}
					}
					cellToSameStationOtherCells.put(ncell.intern(), sstCells);
				}
				if (!ncellToNotSameStationCells.containsKey(ncell)) {
					nsstCells = new ArrayList<String>();
					for (String cell : ncellToCellAssocDegree.get(ncell).keySet()) {
						if (!sstCells.contains(cell) && cellToEarfcn.get(ncell).equals(cellToEarfcn.get(cell))) {
							nsstCells.add(cell.intern());
						}
					}
					ncellToNotSameStationCells.put(ncell.intern(), nsstCells);
				}
			}
			System.out.println("cellToNcellAssocDegree.size()=" + cellToNcellAssocDegree.size());
			System.out.println("ncellToCellAssocDegree.size()=" + ncellToCellAssocDegree.size());
			System.out.println("cellToTotalAssocDegree.size()=" + cellToTotalAssocDegree.size());
			System.out.println("cellToSameStationOtherCells.size()=" + cellToSameStationOtherCells.size());
			System.out.println("cellToNotSameStationCells.size()=" + cellToNotSameStationCells.size());
			System.out.println("ncellToNotSameStationCells.size()=" + ncellToNotSameStationCells.size());
			System.out.println("cellToOriPci.size()=" + cellToOriPci.size());
			System.out.println("cellToLonLat.size()=" + cellToLonLat.size());
			System.out.println("cellToEarfcn.size()=" + cellToEarfcn.size());

			if (!d1D2Type.isEmpty()) {
				objis = new ObjectInputStream(hdfs.open(new Path(URI.create(dataFilePath))));
				abCellList = (List<String>) objis.readObject();
				System.out.println("abCellList.size()=" + abCellList.size());
				objis.close();
			}

			hdfs.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (objis != null) {
				try {
					objis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (hdfs != null) {
				try {
					hdfs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public String toString() {
		return "NewPciConfig [conf=" + conf + ", isCheckNCell=" + isCheckNCell + ", isExportAssoTable="
				+ isExportAssoTable + ", isExportMidPlan=" + isExportMidPlan + ", isExportNcCheckPlan="
				+ isExportNcCheckPlan + ", isUseSf=" + isUseSf + ", jobId=" + jobId + "]";
	}

	/**
	 * 拼装configuration传递的数据
	 * 
	 * @param cellList
	 * @return
	 */
	public static String map2String(List<String> cellList) {
		StringBuffer sb = new StringBuffer();
		for (String key : cellList) {
			sb.append(key).append("#");
		}
		return sb.substring(0, sb.length() - 1);
	}

	private List<Scan> getScans(long cityId, long startMeaMillis, long endMeaMillis, List<String> tableNames) {
		List<Scan> scans = new ArrayList<Scan>();
		for (String tableName : tableNames) {
			Scan scan = getScan(cityId, startMeaMillis, endMeaMillis, tableName);
			scans.add(scan);
		}
		return scans;
	}

	private Scan getScan(long cityId, long startMeaMillis, long endMeaMillis, String tableName) {
		Scan scan = new Scan();
		// 加入这两句速度快很多
		scan.setCacheBlocks(false);
		scan.setCaching(1000);
		scan.setStartRow(Bytes.toBytes(cityId + "_" + startMeaMillis + "_#"));
		scan.setStopRow(Bytes.toBytes(cityId + "_" + endMeaMillis + "_~"));
		scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes(tableName));
		if (isUseSf && tableName.endsWith("SF")) {
			List<Filter> filters = new ArrayList<Filter>();
			for (String fileName : fileNameList) {
				filters.add(new RowFilter(CompareOp.EQUAL, new SubstringComparator(fn2Id.get(fileName))));
			}
			scan.setFilter(new FilterList(Operator.MUST_PASS_ONE, filters));
		}
		return scan;
	}

	private Map<String, Double> sortMapByValue(Map<String, Double> unsortMap) {
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		// sort list based on comparator
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return ((Comparable<Double>) o2.getValue()).compareTo(o1.getValue());
			}
		});
		// put sorted list into map again
		// LinkedHashMap make sure order in which keys were inserted
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
			sortedMap.put(entry.getKey().intern(), entry.getValue());
		}
		return sortedMap;
	}
}
