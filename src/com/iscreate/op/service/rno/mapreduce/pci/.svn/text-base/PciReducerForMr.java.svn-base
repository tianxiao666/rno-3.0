package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PciReducerForMr extends Reducer<Text, Text, LongWritable, Text> {

	private final static Logger logger = LoggerFactory.getLogger(PciReducerForMr.class);

	/** 输出的key **/
	final LongWritable outKey = new LongWritable();
	/** 输出的value **/
	final Text outVal = new Text();
	/** 拼装字符串 **/
	final StringBuilder sb = new StringBuilder("MR");
	/** 测量时间与当天的mixingsum **/
	final Map<String, Integer> meaTimeToMixingSum = new HashMap<String, Integer>();

	/** 邻区减主小区大于-6小于-3的测量报告数 RSRPtimes0- RSRPtimes1 权值 **/
	double rsrp0minus1weight = 0.8;
	/** 邻区减主小区大于-3的测量报告数 RSRPtimes1 权值 **/
	double rsrp1weight = 1.2;
	/** 最低测量次数 **/
	int minmeasuresum = 0;
	/** 最小关联度 **/
	double mincorrelation = 0;
	/** val数组 **/
	String[] indexs;
	/** 测量时间 **/
	String meaTime;
	/** 邻区列表 **/
	List<Ncell> ncells;
	/** 邻区 **/
	Ncell cell;

	// reduce 处理次数统计
	long inCounter = 0;

	// 处理行数统计
	long lineCounter = 0;

	// 输出行数统计
	long outCounter = 0;

	// 运行开始时间
	long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		// 初始化配置信息
		Configuration conf = context.getConfiguration();
		rsrp0minus1weight = conf.getDouble("rsrp0minus1weight", rsrp0minus1weight);
		rsrp1weight = conf.getDouble("rsrp1weight", rsrp1weight);
		minmeasuresum = conf.getInt("minmeasuresum", minmeasuresum);
		mincorrelation = conf.getDouble("mincorrelation", mincorrelation);
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
		// 小区ID
		int cellId = Integer.parseInt(key.toString());
		//清空测量时间
		meaTimeToMixingSum.clear();
		// 邻区数据
		ncells = new ArrayList<Ncell>();
		for (Text val : values) {
			if (++lineCounter % 100000 == 0) {
				System.out.println("reduce line counter = " + lineCounter);
			}
			indexs = val.toString().split(",");
			meaTime = indexs[1].intern();
			int mixingSum = Integer.parseInt(indexs[2]);
			// 测量时间与当天的mixingsum
			if (!meaTimeToMixingSum.containsKey(meaTime)) {
				meaTimeToMixingSum.put(meaTime, mixingSum);
			} else if (meaTimeToMixingSum.get(meaTime) < mixingSum) {
				meaTimeToMixingSum.put(meaTime, mixingSum);
			}
			// 获取某邻区的RSRPtimes1 可设置相对数值2(建议值：-3)
			// ncellid ,timesTotal,times0,times1
			cell = new Ncell(Integer.parseInt(indexs[0]), Integer.parseInt(indexs[3]), Integer.parseInt(indexs[4]),
					Integer.parseInt(indexs[5]));
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
				ncells.add(cell);
			}
		}
		//没有数据，下一条
		if(ncells.isEmpty()){
			System.out.println("Filter: cellId = " + cellId + ", no ncells");
			return;
		}
//		// 总的测量次数
//		int sumTimesTotals = 0;
//		// 计算关联度
//		for (Ncell ncell : ncells) {
//			sumTimesTotals += ncell.getTimesTotal();
//		}
//		// 过滤测量总数小于最小测量总数的
//		if (sumTimesTotals < minmeasuresum) {
//			System.out.println("Filter: cellId = " + cellId + ", sumTimesTotals=" + sumTimesTotals);
//			return;
//		}
		// 混频 新分母
		double totalMixingSum = 0;
		for (int d : meaTimeToMixingSum.values()) {
			totalMixingSum += d;
		}
		System.out.println("totalMixingSum=" + totalMixingSum);
		// 计算关联度
		sb.setLength(2);
		for (Ncell ncell : ncells) {
			double assocDegree = 0;
			if (totalMixingSum > 0) {
				System.out.println("times0="+ncell.getTimes0()+",times1="+ncell.getTimes1());
				assocDegree = (ncell.getTimes1() * rsrp1weight + (ncell.getTimes0() - ncell.getTimes1())
						* rsrp0minus1weight)
						/ totalMixingSum;
			}
			System.out.println("assocDegree="+assocDegree);
//			if (assocDegree > mincorrelation) {
//				if (++outCounter % 10000 == 0) {
//					System.out.println("reduce out counter = " + outCounter);
//				}
//				sb.append("#").append(ncell.getId()).append(",").append(assocDegree);
//			}
			sb.append("#").append(ncell.getId()).append(",").append(assocDegree);
		}

		System.out.println(sb.toString());
		if (sb.length() > 2) {
			outKey.set(cellId);
			outVal.set(sb.toString());
			context.write(outKey, outVal);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		logger.info("reduce in counter total = " + inCounter);
		logger.info("reduce line counter total = " + lineCounter);
		logger.info("reduce out counter total = " + outCounter);
		logger.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	/**
	 * 邻区对象
	 * 
	 * @author scott
	 */
	class Ncell {
		private int id; // 邻区标识
		private int timesTotal; // 测量次数
		private int times0; // RSRPtimes0
		private int times1; // RSRPtimes1

		public Ncell(int id, int timesTotal, int times0, int times1) {
			super();
			this.id = id;
			this.timesTotal = timesTotal;
			this.times0 = times0;
			this.times1 = times1;
		}

		public int getId() {
			return id;
		}

		public int getTimesTotal() {
			return timesTotal;
		}

		public int getTimes0() {
			return times0;
		}

		public int getTimes1() {
			return times1;
		}

		public void add(Ncell ncell) {
			if (null != ncell && this.equals(ncell)) {
				timesTotal += ncell.getTimesTotal();
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
			return "Ncell [id=" + id + ", timesTotal=" + timesTotal + ", times0=" + times0 + ", times1=" + times1 + "]";
		}

		private PciReducerForMr getOuterType() {
			return PciReducerForMr.this;
		}
	}
}