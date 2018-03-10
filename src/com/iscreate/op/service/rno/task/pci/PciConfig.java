package com.iscreate.op.service.rno.task.pci;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class PciConfig {
	// private static Logger logger = LoggerFactory.getLogger(PciConfig.class);
	private Configuration conf;
	private double m3r = 0;
	private double m6r = 0;
	private double m30r = 0;
	private double topRate = 0; // top n%
	private double defInterRate = 0; // 给定的干扰差值比例m
	private double defVariance = 0; // 给定的方差值
	private int divideNumber = 10;
	private double ks = -1.0;

	private String planType = "ONE"; // 评估方案，默认1
	private String schemeType = "ONE"; // 收敛方案，默认1
	private String isCheckNCell = "YES"; // 邻区核查，默认进行
	private String isExportAssoTable = "NO";// 关联表输出，默认不输出
	private String isExportMidPlan = "NO";// 中间方案输出，默认不输出
	private String isExportNcCheckPlan = "NO";// 邻区核查方案输出，默认不输出
	/** TODO 是否合并从数据中读取的工参,暂时不合并 **/
	boolean isMerge = false;

	boolean isFilter = true;

	private String jobId = "";
	private String filePath = "";
	private String fileName = "";
	private String dataFilePath = "";
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

	private Map<String, String> enodebToCells;

	/** 小区与同站其他小区列的映射，同站其他小区已按关联度从大到小排列 **/
	Map<String, List<String>> cellToSameStationOtherCells = new HashMap<String, List<String>>();

	/** 小区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 **/
	Map<String, List<String>> cellToNotSameStationCells = new HashMap<String, List<String>>();

	/** 小区与邻区关联度的映射（包含了同站其他小区） **/
	Map<String, Map<String, Double>> cellToNcellAssocDegree = new HashMap<String, Map<String, Double>>();

	/** 小区与小区总关联度的映射 **/
	Map<String, Double> cellToTotalAssocDegree = new HashMap<String, Double>();

	/** 小区与原PCI的映射 **/
	Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();

	/** 小区到经纬度的映射，不重复 **/
	Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();

	/** 小区与频率的映射 **/
	Map<String, String> cellToEarfcn = new HashMap<String, String>();

	@SuppressWarnings("unchecked")
	public PciConfig(Configuration conf, long jobId, String isExportAssoTable, String isExportMidPlan,
			String isExportNcCheckPlan, String ks) {
		this.conf = conf;
		this.jobId = String.valueOf(jobId);

		// 方案保存的文件路径
		this.filePath = conf.get("RESULT_DIR");
		this.fileName = conf.get("RD_FILE_NAME");
		this.dataFilePath = conf.get("DATA_FILE_PATH");

		/*
		 * // 方案保存的文件路径
		 * this.filePath = filePath;
		 * this.fileName = rdFileName;
		 * this.dataFilePath = dataFilePath;
		 */
		// 获取需要优化的小区字符串（变小区），格式为“A,B,C....”
		String[] strCells = conf.get("OPTIMIZE_CELLS").split(",");
		this.cellsNeedtoAssignList = new ArrayList<String>();
		for (String cellId : strCells) {
			this.cellsNeedtoAssignList.add(cellId.trim().intern());
		}

		// 获取各个mod的值
		this.m3r = Double.parseDouble(conf.get("cellm3rinterfercoef"));
		this.m6r = Double.parseDouble(conf.get("cellm6rinterfercoef"));
		this.m30r = Double.parseDouble(conf.get("cellm30rinterfercoef"));

		// 获取 top n% 百分比
		this.topRate = Double.parseDouble(conf.get("topncelllist")) * 0.01;

		// 干扰差值比例(m%)
		this.defInterRate = Double.parseDouble(conf.get("convermethod1targetval")) * 0.01;

		// 方差值(m%)
		this.defVariance = Double.parseDouble(conf.get("convermethod2targetval")) * 0.01;

		// 等分数
		this.divideNumber = Integer.parseInt(conf.get("convermethod2scoren"));

		// 评估方案
		this.planType = conf.get("PLAN_TYPE");

		// 收敛方案
		this.schemeType = conf.get("CONVER_TYPE");

		// 邻区核查
		this.isCheckNCell = conf.get("IS_CHECK_NCELL");

		/*
		 * // 获取需要优化的小区字符串（变小区），格式为“A,B,C....”
		 * String[] strCells = optimizeCells.split(",");
		 * this.cellsNeedtoAssignList = new ArrayList<String>();
		 * for (String cellId : strCells) {
		 * this.cellsNeedtoAssignList.add(cellId.trim().intern());
		 * }
		 * 
		 * // 获取各个mod的值
		 * this.m3r = Double.parseDouble(cellm3rinterfercoef);
		 * this.m6r = Double.parseDouble(cellm6rinterfercoef);
		 * this.m30r = Double.parseDouble(cellm30rinterfercoef);
		 * 
		 * // 获取 top n% 百分比
		 * this.topRate = Double.parseDouble(topncelllist) * 0.01;
		 * 
		 * // 干扰差值比例(m%)
		 * this.defInterRate = Double.parseDouble(convermethod1targetval) * 0.01;
		 * 
		 * // 方差值(m%)
		 * this.defVariance = Double.parseDouble(convermethod2targetval) * 0.01;
		 * 
		 * // 等分数
		 * this.divideNumber = Integer.parseInt(convermethod2scoren);
		 * 
		 * // 评估方案
		 * this.planType = planType;
		 * 
		 * // 收敛方案
		 * this.schemeType = converType;
		 * 
		 * // 邻区核查
		 * this.isCheckNCell = isCheckNCell;
		 */

		// 关联表输出
		this.isExportAssoTable = isExportAssoTable;

		// 中间方案输出
		this.isExportMidPlan = isExportMidPlan;

		// 邻区核查方案输出
		this.isExportNcCheckPlan = isExportNcCheckPlan;

		// 流量权值系数
		try {
			this.ks = Double.parseDouble(ks);
		} catch (Exception e) {
			this.ks = -1.0;
		}
		/*
		 * // 同站同频点小区集合（从数据库工参表取到的数据，只和所选地市有关，和测量数据的多少无关）
		 * String strEnodebToCells = nodebs;
		 * if (strEnodebToCells != null && strEnodebToCells.length() > 0) {
		 * System.out.println("strEnodebToCells length=" + strEnodebToCells.length());
		 * this.enodebToCells = string2Map(strEnodebToCells);
		 * }
		 * 
		 * // 获取权值
		 * this.samefreqcellcoefweight = Double.parseDouble(samefreqcellcoefweight);
		 * this.switchratioweight = Double.parseDouble(switchratioweight);
		 * 
		 * // 获取“合并后最小测量总数”和“合并后小于关联度（百分比）”的参数
		 * this.minmeasuresum = Long.parseLong(minmeasuresum);
		 * this.mincorrelation = Double.parseDouble(mincorrelation) * 0.01;
		 * // 距离限制
		 * if (dislimit != null && "".equals(dislimit)) {
		 * this.dislimit = Double.parseDouble(dislimit);
		 * }
		 */
		// 距离限制
		if (conf.get("dislimit") != null && "".equals(conf.get("dislimit"))) {
			this.dislimit = Double.parseDouble(conf.get("dislimit"));
		}
		String strCellToLngLat = conf.get("cellToLngLat");
		if (strCellToLngLat != null && strCellToLngLat.length() > 0) {
			System.out.println("strCellToLngLat length=" + strCellToLngLat.length());
			Map<String, Object> result = string2Map2(strCellToLngLat);
			this.cellToLonLat = (Map<String, double[]>) result.get("lnglat");
			this.cellToOriPci = (Map<String, Integer>) result.get("pci");
			this.cellToEarfcn = (Map<String, String>) result.get("earfcn");
		}
	}

	public Configuration getConf() {
		return conf;
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

	public String getJobId() {
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

	public Map<String, String> getEnodebToCells() {
		return enodebToCells;
	}

	public int getDivideNumber() {
		return divideNumber;
	}

	public String getDataFilePath() {
		return dataFilePath;
	}

	public String getIsCheckNCell() {
		return isCheckNCell;
	}

	public Double getDislimit() {
		return dislimit;
	}

	public String getIsExportAssoTable() {
		return isExportAssoTable;
	}

	public String getIsExportMidPlan() {
		return isExportMidPlan;
	}

	public String getIsExportNcCheckPlan() {
		return isExportNcCheckPlan;
	}

	public Double getKs() {
		return ks;
	}

	public Map<String, List<String>> getCellToSameStationOtherCells() {
		return cellToSameStationOtherCells;
	}

	public Map<String, List<String>> getCellToNotSameStationCells() {
		return cellToNotSameStationCells;
	}

	public Map<String, Map<String, Double>> getCellToNcellAssocDegree() {
		return cellToNcellAssocDegree;
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

	public Map<String, String> getCellToEarfcn() {
		return cellToEarfcn;
	}

	public String getReturnInfo() {
		return returnInfo;
	}

	public void setReturnInfo(String returnInfo) {
		this.returnInfo = returnInfo;
	}

	/**
	 * 从hdfs文件中读取数据
	 */
	@SuppressWarnings("unchecked")
	public void readDataFromFile() {
		Path hFilePath = new Path(URI.create(dataFilePath));
		FileSystem hdfs = null;
		ObjectInputStream objis = null;
		try {
			hdfs = FileSystem.get(conf);
			objis = new ObjectInputStream(hdfs.open(hFilePath));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		/** 小区与原PCI的映射 **/
		Map<String, Integer> cellToOriPciFromData = new HashMap<String, Integer>();

		/** 小区到经纬度的映射，不重复 **/
		Map<String, double[]> cellToLonLatFromData = new HashMap<String, double[]>();

		/** 小区与频率的映射 **/
		Map<String, String> cellToEarfcnFromData = new HashMap<String, String>();
		if (objis != null) {
			try {
				// obj方式必须保证顺序一致
				this.cellToNcellAssocDegree = (Map<String, Map<String, Double>>) objis.readObject();
				this.cellToNotSameStationCells = (Map<String, List<String>>) objis.readObject();
				this.cellToTotalAssocDegree = (Map<String, Double>) objis.readObject();
				this.cellToSameStationOtherCells = (Map<String, List<String>>) objis.readObject();
				cellToOriPciFromData = (Map<String, Integer>) objis.readObject();
				cellToLonLatFromData = (Map<String, double[]>) objis.readObject();
				cellToEarfcnFromData = (Map<String, String>) objis.readObject();

				if (this.isMerge) {
					if (this.cellToOriPci != null && cellToOriPciFromData != null) {
						this.cellToOriPci.putAll(cellToOriPciFromData);
					}
					if (this.cellToLonLat != null && cellToLonLatFromData != null) {
						this.cellToLonLat.putAll(cellToLonLatFromData);
					}
					if (this.cellToEarfcn != null && cellToEarfcnFromData != null) {
						this.cellToEarfcn.putAll(cellToEarfcnFromData);
					}
				}
				if (this.isFilter) {
					Map<String, Double> tmp = new HashMap<String, Double>();
					for (String cellId : cellToTotalAssocDegree.keySet()) {
						if (!cellToOriPci.containsKey(cellId)) {
							tmp.put(cellId, cellToTotalAssocDegree.get(cellId));
						}
					}
					for (String cellId : tmp.keySet()) {
						cellToNcellAssocDegree.remove(cellId);
						cellToNotSameStationCells.remove(cellId);
						cellToTotalAssocDegree.remove(cellId);
						cellToSameStationOtherCells.remove(cellId);
					}
				}
				System.out.println("cellToNcellAssocDegree.size()=" + cellToNcellAssocDegree.size());
				System.out.println("cellToNotSameStationCells.size()=" + cellToNotSameStationCells.size());
				System.out.println("cellToTotalAssocDegree.size()=" + cellToTotalAssocDegree.size());
				System.out.println("cellToSameStationOtherCells.size()=" + cellToSameStationOtherCells.size());
				System.out.println("cellToOriPci.size()=" + cellToOriPci.size());
				System.out.println("cellToLonLat.size()=" + cellToLonLat.size());
				System.out.println("cellToEarfcn.size()=" + cellToEarfcn.size());

				objis.close();
				hdfs.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param resFilePath Pci计算返回信息文件的hdfs路径
	 * @param rdFileName Pci计算返回信息文件的名称
	 * @return
	 */
	/*
	 * public String readReturnInfoFromHdfs() {
	 * 
	 * // Pci规划数据源文件的名称,源数据文件有两个，一个是current后缀，一个是backup后缀
	 * String currentFileName = filePath + ".resultInfo";
	 * 
	 * File currentFile = FileTool.getFile(filePath + "/" + currentFileName);
	 * 
	 * // 两个源文件不存在，不能提供下载
	 * if (currentFile == null) {
	 * logger.info("Pci规划计算的返回信息文件不存在！currentFileName=" + filePath + "/" + currentFileName);
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
	 * logger.info("获取Pci规划结果源文件中，读取数据出错");
	 * result = "error";
	 * }
	 * logger.info("获取Pci规划结果源文件中，读取数据出错");
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
	private Map<String, Object> string2Map2(String cells) {
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();
		Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();
		Map<String, String> cellToEarfcn = new HashMap<String, String>();
		String keyarr[] = null;
		String valarr[] = null;
		String valarr2[] = null;
		String lnglat[] = null;
		keyarr = cells.split("\\|");
		for (int i = 0; i < keyarr.length; i++) {
			valarr = keyarr[i].split("=");
			valarr2 = valarr[1].split("#");
			lnglat = valarr2[0].split("_");
			cellToLonLat.put(valarr[0], new double[] { Double.parseDouble(lnglat[0]), Double.parseDouble(lnglat[1]) });
			cellToOriPci.put(valarr[0], Integer.parseInt(valarr2[1]));
			cellToEarfcn.put(valarr[0], valarr2[2]);
		}
		result.put("lnglat", cellToLonLat);
		result.put("pci", cellToOriPci);
		result.put("earfcn", cellToEarfcn);
		return result;
	}

	@Override
	public String toString() {
		return "m3r = " + m3r + ", m6r = " + m6r + ", m30r = " + m30r + ", topRate = " + topRate + ", defInterRate = "
				+ defInterRate + ", defVariance = " + defVariance + ", planType = " + planType + ", schemeType = "
				+ schemeType + ", samefreqcellcoefweight = " + samefreqcellcoefweight + ", switchratioweight = "
				+ switchratioweight + ", mincorrelation = " + mincorrelation + ", minmeasuresum = " + minmeasuresum
				+ ",isCheckNCell = " + isCheckNCell + ", dislimit = " + dislimit;
	}

}
