package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
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

public class NewPciConfig {
	private static Logger logger = LoggerFactory.getLogger(NewPciConfig.class);

	private Configuration conf;

	/** 距离限制，单位米 **/
	private double dislimit = 5000.0;

	/** 权值 **/
	private double samefreqcellcoefweight = 0.8;

	/** 切换比例权值 **/
	private double switchratioweight = 0.2;

	private double rsrp0minus1weight = 0.8; // 邻区减主小区大于-6小于-3的测量报告数 RSRPtimes0- RSRPtimes1 权值
	private double rsrp1weight = 1.2; // 邻区减主小区大于-3的测量报告数 RSRPtimes1 权值

	private String jobId = "";
	private String filePath = "";
	private String fileName = "";

	private String dataFilePath = "";
	/** 合并后最小的测量总数 **/
	private long minmeasuresum = 0;

	/** 合并后最小的关联度（百分比） **/
	private double mincorrelation = 0.0;

	private List<String> cellList = new ArrayList<String>();

	private Map<String, double[]> cellToLonLat = new HashMap<String, double[]>();

	private Map<String, Integer> cellToOriPci = new HashMap<String, Integer>();

	private Map<String, Integer> cellToEarfcn = new HashMap<String, Integer>();

	/** ab小区列表 **/
	private List<String> abCellList = new ArrayList<String>();

	/** 小区与原PCI的映射 **/
	private Map<String, Integer> cellToOriPciFromData = new HashMap<String, Integer>();

	/** 小区到经纬度的映射，不重复 **/
	private Map<String, double[]> cellToLonLatFromData = new HashMap<String, double[]>();

	/** 小区与频率的映射 **/
	private Map<String, Integer> cellToEarfcnFromData = new HashMap<String, Integer>();

	public NewPciConfig(@SuppressWarnings("rawtypes") Context context) {
		Configuration conf = context.getConfiguration();
		this.conf = conf;
		this.jobId = context.getJobID().toString();

		// 方案保存的文件路径
		this.filePath = conf.get("RESULT_DIR", "");
		this.fileName = conf.get("RD_FILE_NAME", "");
		this.dataFilePath = conf.get("DATA_FILE_PATH", "");

		// 获取工参
		String cellListStr = conf.get("cellList", "");
		if (!cellListStr.isEmpty()) {
			this.cellList = string2Map2(cellListStr);
		}
		// 获取权值
		this.samefreqcellcoefweight = conf.getDouble("samefreqcellcoefweight", samefreqcellcoefweight);
		this.switchratioweight = conf.getDouble("switchratioweight", switchratioweight);
		this.rsrp0minus1weight = conf.getDouble("rsrp0minus1weight", rsrp0minus1weight);
		this.rsrp1weight = conf.getDouble("rsrp1weight", rsrp1weight);
		// 距离限制
		this.dislimit = conf.getDouble("dislimit", dislimit);
		// 获取“合并后最小测量总数”和“合并后小于关联度（百分比）”的参数
		this.minmeasuresum = conf.getLong("minmeasuresum", minmeasuresum);
		this.mincorrelation = conf.getDouble("mincorrelation", mincorrelation) * 0.01;
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

	public List<String> getCellList() {
		return cellList;
	}

	public Map<String, double[]> getCellToLonLat() {
		return cellToLonLat;
	}

	public Map<String, Integer> getCellToOriPci() {
		return cellToOriPci;
	}

	public Map<String, Integer> getCellToEarfcn() {
		return cellToEarfcn;
	}

	public double getRsrp0minus1weight() {
		return rsrp0minus1weight;
	}

	public double getRsrp1weight() {
		return rsrp1weight;
	}

	public List<String> getAbCellList() {
		return abCellList;
	}

	public Map<String, Integer> getCellToOriPciFromData() {
		return cellToOriPciFromData;
	}

	public Map<String, double[]> getCellToLonLatFromData() {
		return cellToLonLatFromData;
	}

	public Map<String, Integer> getCellToEarfcnFromData() {
		return cellToEarfcnFromData;
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
			fs = FileSystem.get(this.conf);
		} catch (IOException e1) {
			logger.error("savePciDataListToHdfs过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}
		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(this.dataFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("savePciDataListToHdfs过程：保存文件时，删除原有文件出错！");
			e2.printStackTrace();
		}
		// 创建新文件
		Path filePathObj = new Path(URI.create(this.dataFilePath));
		// 创建流
		OutputStream dataOs = null;
		try {
			dataOs = fs.create(filePathObj, true);
		} catch (IOException e1) {
			logger.error("创建文件流失败！");
			e1.printStackTrace();
		}
		ObjectOutputStream objos = null;
		try {
			objos = new ObjectOutputStream(dataOs);
			objos.writeObject(cellToOriPci);
			objos.writeObject(cellToLonLat);
			objos.writeObject(cellToEarfcn);
			objos.writeObject(abCellList);
			objos.close();
			logger.info("Export PciCalcData.dat Success.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
