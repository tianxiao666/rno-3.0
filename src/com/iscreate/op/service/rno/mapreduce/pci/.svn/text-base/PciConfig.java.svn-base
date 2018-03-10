package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PciConfig {
	private static Logger logger = LoggerFactory.getLogger(PciConfig.class);

	private Configuration conf;

	/** 距离限制，单位米 **/
	private double dislimit = 5000.0;

	/** 权值 **/
	private double samefreqcellcoefweight = 0.8;

	/** 切换比例权值 **/
	private double switchratioweight = 0.2;

	private String jobId = "";
	private String filePath = "";
	private String fileName = "";

	private String dataFilePath = "";
	/** 合并后最小的测量总数 **/
	private long minmeasuresum = 0;

	/** 合并后最小的关联度（百分比） **/
	private double mincorrelation = 0.0;

	private List<String> cellList = new ArrayList<String>();

	private Map<String, String> enodebToCells = new HashMap<String, String>();

	private Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();

	private Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();

	private Map<String, String> cellToEarfcn = new HashMap<String, String>();

	/** 小区与同站其他小区列的映射，同站其他小区已按关联度从大到小排列 **/
	private Map<String, List<String>> cellToSameStationOtherCells = new HashMap<String, List<String>>();

	/** 小区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 **/
	private Map<String, List<String>> cellToNotSameStationCells = new HashMap<String, List<String>>();

	/** 小区与邻区关联度的映射（包含了同站其他小区） **/
	private Map<String, Map<String, Double>> cellToNcellAssocDegree = new HashMap<String, Map<String, Double>>();

	/** 小区与小区总关联度的映射 **/
	private Map<String, Double> cellToTotalAssocDegree = new HashMap<String, Double>();
	
	/** 小区与原PCI的映射 **/
	Map<String, Integer> cellToOriPciFromData = new HashMap<String, Integer>();

	/** 小区到经纬度的映射，不重复 **/
	Map<String, double[]> cellToLonLatFromData = new HashMap<String, double[]>();

	/** 小区与频率的映射 **/
	Map<String, String> cellToEarfcnFromData = new HashMap<String, String>();

	public PciConfig(@SuppressWarnings("rawtypes") Context context) {
		Configuration conf = context.getConfiguration();
		this.conf = conf;
		this.jobId = context.getJobID().toString();

		// 方案保存的文件路径
		this.filePath = conf.get("RESULT_DIR","");
		this.fileName = conf.get("RD_FILE_NAME","");
		this.dataFilePath = conf.get("DATA_FILE_PATH","");

		// 同站同频点小区集合（从数据库工参表取到的数据，只和所选地市有关，和测量数据的多少无关）
		String strEnodebToCells = conf.get("enodebToCells","");
		System.out.println("strEnodebToCells="+strEnodebToCells);
		if (strEnodebToCells != null && strEnodebToCells.length() > 0) {
			this.enodebToCells = string2Map(strEnodebToCells);
		}

		//获取工参
		String cellListStr = conf.get("cellList","");
		if (!cellListStr.isEmpty()) {
			this.cellList = string2Map2(cellListStr);
		}
		// 获取权值
		this.samefreqcellcoefweight = Double.parseDouble(conf.get("samefreqcellcoefweight","0.8"));
		this.switchratioweight = Double.parseDouble(conf.get("switchratioweight","0.2"));
		// 距离限制
		this.dislimit = Double.parseDouble(conf.get("dislimit","5000.0"));
		// 获取“合并后最小测量总数”和“合并后小于关联度（百分比）”的参数
		this.minmeasuresum = Long.parseLong(conf.get("minmeasuresum","0"));
		this.mincorrelation = Double.parseDouble(conf.get("mincorrelation","0.0")) * 0.01;
	}

	public Configuration getConf() {
		return conf;
	}

	public double getDislimit() {
		return dislimit;
	}

	public double getSamefreqcellcoefweight() {
		return samefreqcellcoefweight;
	}

	public double getSwitchratioweight() {
		return switchratioweight;
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

	public String getDataFilePath() {
		return dataFilePath;
	}

	public long getMinmeasuresum() {
		return minmeasuresum;
	}

	public double getMincorrelation() {
		return mincorrelation;
	}

	public Map<String, String> getEnodebToCells() {
		return enodebToCells;
	}

	public List<String> getCellList() {
		return cellList;
	}

	public Map<String, double[]> getCellToLonLat() {
		return cellToLonLat;
	}

	public Map<String, Integer> getCellToOriPci() {
		return cellToOriPci;
	}

	public Map<String, String> getCellToEarfcn() {
		return cellToEarfcn;
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

	public Map<String, Integer> getCellToOriPciFromData() {
		return cellToOriPciFromData;
	}

	public Map<String, double[]> getCellToLonLatFromData() {
		return cellToLonLatFromData;
	}

	public Map<String, String> getCellToEarfcnFromData() {
		return cellToEarfcnFromData;
	}

	private Map<String, String> string2Map(String enodebs) {
		Map<String, String> enodebToCells = new HashMap<String, String>();
		if (!enodebs.isEmpty()) {
			String keyarr[] = null;
			String valarr[] = null;
			keyarr = enodebs.split("\\|");
			for (int i = 0; i < keyarr.length; i++) {
				valarr = keyarr[i].split("=");
				enodebToCells.put(valarr[0].intern(), valarr[1].replace("#", ","));
			}
		}
		return enodebToCells;
	}

	private List<String> string2Map2(String cells) {
		if (cells == null || cells.isEmpty()) {
			return new ArrayList<String>();
		}
		return new ArrayList<String>(Arrays.asList(cells.split("#")));
	}

	public void updateOwnParam() {
		// 用MR的部分工参更新数据库中的工参
		if (cellToOriPci != null) {
			cellToOriPci.putAll(cellToOriPciFromData);
		}
		// 经纬度不需要更新
		if (cellToLonLat != null) {
			cellToLonLat.putAll(cellToLonLatFromData);
		}
		if (cellToEarfcn != null) {
			cellToEarfcn.putAll(cellToEarfcnFromData);
		}
	}
	/**
	 * 把Pci计算的数据写到HDFS
	 */
	public void savePciDataListToHdfs() {
		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("savePciDataListToHdfs过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}
		ObjectOutputStream objos = null;
		try {
			objos = new ObjectOutputStream(fs.create(new Path(URI.create(dataFilePath)), true));

			objos.writeObject(cellToNcellAssocDegree);
			objos.writeObject(cellToNotSameStationCells);
			objos.writeObject(cellToTotalAssocDegree);
			objos.writeObject(cellToSameStationOtherCells);
			objos.writeObject(cellToOriPci);
			objos.writeObject(cellToLonLat);
			objos.writeObject(cellToEarfcn);

			objos.close();
			fs.close();
			logger.info("Export PciCalcData.dat Success.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 把Pci计算的数据写入到临时文件中，用于分析和调试
	 */
	public void writeObject() {
		try {
			FileOutputStream outStream = new FileOutputStream("/tmp/PciCalc-" + getFileName() + ".dat");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);

			objectOutputStream.writeObject(cellToNcellAssocDegree);
			objectOutputStream.writeObject(cellToNotSameStationCells);
			objectOutputStream.writeObject(cellToTotalAssocDegree);
			objectOutputStream.writeObject(cellToSameStationOtherCells);
			objectOutputStream.writeObject(cellToOriPci);
			objectOutputStream.writeObject(cellToLonLat);
			objectOutputStream.writeObject(cellToEarfcn);

			outStream.close();
			logger.info("Export PciCalcData.dat Success.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
