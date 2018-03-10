package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PciReducerForSf extends Reducer<Text, Text, LongWritable, Text> {
	private static Logger logger = LoggerFactory.getLogger(PciReducerForSf.class);
	/** 邻区减主小区大于-6小于-3的测量报告数 RSRPtimes0- RSRPtimes1 权值 **/
	private double rsrp0minus1weight = 0.8;
	/** 邻区减主小区大于-3的测量报告数 RSRPtimes1 权值 **/
	private double rsrp1weight = 1.2;
	/** 最小关联度 **/
	private double mincorrelation = 0;
	/** key数组 **/
	private String[] keyArr;
	/** val数组 **/
	private String[] indexs;
	/** 小区ID **/
	private int cellId;
	/** 测量时间 **/
	private String meaTime;
	/** 邻区列表 **/
	private List<Ncell> ncells;
	/** 邻区 **/
	private Ncell cell;
	/** 输出的key **/
	private LongWritable outKey = new LongWritable();
	/** 输出的value **/
	private Text outVal = new Text();
	/** 拼装字符串 **/
	private StringBuilder sb = new StringBuilder();
	/** 文件保存路径 **/
	private String dataFilePath;
	/** 扫频小区列表 **/
	private List<String> abCellList = new ArrayList<String>();

	// reduce 处理次数统计
	private long inCounter = 0;

	// 处理行数统计
	private long lineCounter = 0;

	// 输出行数统计
	private long outCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		// 初始化配置信息
		Configuration conf = context.getConfiguration();
		rsrp0minus1weight = conf.getDouble("rsrp0minus1weight", rsrp0minus1weight);
		rsrp1weight = conf.getDouble("rsrp1weight", rsrp1weight);
		mincorrelation = conf.getDouble("mincorrelation", mincorrelation);
		dataFilePath = conf.get("DATA_FILE_PATH", "");
	}

	/**
	 * key 格式为 cellId,meaTime,mixingSum <br/>
	 * 如：1868801,2015-6-23,5233 <br/>
	 * values 格式为 ncellId,timestotal,times0,times1 的集合 <br/>
	 * 如：1868803,120,53,22
	 */
	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		if (++inCounter % 1000 == 0) {
			System.out.println("reduce in counter = " + inCounter);
			System.out.println("reduce time =  " + (System.currentTimeMillis() - startTimeMillis));
		}
		keyArr = key.toString().split(",");
		if (keyArr.length != 3) {
			return;
		}
		cellId = Integer.parseInt(keyArr[0]);
		// 保存主小区到DT小区表
		if (!abCellList.contains(keyArr[0])) {
			abCellList.add(keyArr[0]);
		}
		meaTime = keyArr[1].intern();
		double mixingSum = Double.parseDouble(keyArr[2]);
		// 测量时间与当天的mixingsum
		Map<String, Double> meaTimeToMixingSum = new HashMap<String, Double>();
		if (!meaTimeToMixingSum.containsKey(meaTime)) {
			meaTimeToMixingSum.put(meaTime, mixingSum);
		} else if (meaTimeToMixingSum.get(meaTime) < mixingSum) {
			meaTimeToMixingSum.put(meaTime, mixingSum);
		}
		// 邻区数据
		ncells = new ArrayList<Ncell>();
		for (Text val : values) {
			if (++lineCounter % 100000 == 0) {
				System.out.println("reduce line counter = " + lineCounter);
			}
			indexs = val.toString().split(",");
			// 过滤
			if (indexs[0].equals("")) {
				continue;
			}

			// 获取某邻区的RSRPtimes1 可设置相对数值2(建议值：-3)
			// ncellid ,timesTotal,times0,times1
			cell = new Ncell(Integer.parseInt(indexs[0]), Double.parseDouble(indexs[1]), Double.parseDouble(indexs[2]));
			if (ncells.contains(cell)) {
				for (Ncell n : ncells) {
					// 判断是否包含当前邻区
					if (n.equals(cell)) {
						// 如果包含则增加测量值
						n.add(cell);
						break;
					}
				}
			} else {
				// 保存邻小区到DT小区表
				if (!abCellList.contains(indexs[0])) {
					abCellList.add(indexs[0]);
				}
				ncells.add(cell);
			}
		}
		// 没有数据，下一条
		if (ncells.isEmpty()) {
			return;
		}
		// 计算关联度
		double totalMixingSum = 0.0;
		for (double d : meaTimeToMixingSum.values()) {
			totalMixingSum += d;
		}
		// 计算关联度
		sb.setLength(0);
		// SF数据头
		sb.append("SF");
		for (Ncell ncell : ncells) {
			double assocDegree = 0;
			if (totalMixingSum != 0) {
				assocDegree = ncell.getTimes1() / totalMixingSum * rsrp1weight
						+ (ncell.getTimes0() - ncell.getTimes1()) / totalMixingSum * rsrp0minus1weight;
			}
			if (assocDegree > mincorrelation) {
				if (++outCounter % 10000 == 0) {
					System.out.println("reduce out counter = " + outCounter);
				}
				sb.append("#").append(ncell.getId()).append(",").append(assocDegree);
			}
		}
		outKey.set(cellId);
		outVal.set(sb.toString());
		context.write(outKey, outVal);
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		savePciDataListToHdfs(context.getConfiguration());
		super.cleanup(context);
		logger.info("reduce in counter total = " + inCounter);
		logger.info("reduce line counter total = " + lineCounter);
		logger.info("reduce out counter total = " + outCounter);
		logger.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	/**
	 * 把Pci计算的数据写到HDFS
	 */
	public void savePciDataListToHdfs(Configuration conf) {
		FileSystem fs = null;
		ObjectOutputStream objos = null;
		try {
			fs = FileSystem.get(conf);
			// 创建流
			if (fs != null) {
				objos = new ObjectOutputStream(fs.create(new Path(URI.create(dataFilePath)), true));
				// 根据规则写入数据
				if (objos != null) {
					logger.info("abCellList = " + abCellList);
					objos.writeObject(abCellList);
					logger.info("Export PciCalcData.dat Success.");
				}
			}
		} catch (IOException e) {
			if (fs == null) {
				logger.error("PciReducer过程：打开hdfs文件系统出错！");
			} else if (objos == null) {
				logger.error("PciReducer过程：创建输出数据文件流出错！");
			} else {
				logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			}
			e.printStackTrace();
		} finally {
			if (objos != null) {
				try {
					objos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fs != null) {
				try {
					fs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 邻区对象
	 * 
	 * @author scott
	 */
	class Ncell {
		private int id; // 邻区标识
		private double times0; // RSRPtimes0
		private double times1; // RSRPtimes1

		public Ncell(int id, double times0, double times1) {
			super();
			this.id = id;
			this.times0 = times0;
			this.times1 = times1;
		}

		public int getId() {
			return id;
		}

		public double getTimes0() {
			return times0;
		}

		public double getTimes1() {
			return times1;
		}

		public void add(Ncell ncell) {
			if (null != ncell && this.equals(ncell)) {
				times0 += ncell.getTimes0();
				times1 += ncell.getTimes1();
			}
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + id;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Ncell)) {
				return false;
			}
			Ncell other = (Ncell) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (id != other.id) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "Ncell [id=" + id + ", times0=" + times0 + ", times1=" + times1 + "]";
		}

		private PciReducerForSf getOuterType() {
			return PciReducerForSf.this;
		}
	}
}