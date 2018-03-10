package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PciReducerForHo extends Reducer<LongWritable, Text, LongWritable, Text> {

	private static Logger logger = LoggerFactory.getLogger(PciReducerForHo.class);

	/** 最小关联度 **/
	double mincorrelation = 0;
	/** val数组 **/
	String[] indexs;
	/** 邻区列表 **/
	List<Ncell> ncells;
	/** 邻区 **/
	Ncell cell;
	/** 输出的value **/
	Text outVal = new Text();
	/** 拼装字符串 **/
	StringBuilder sb = new StringBuilder();

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
		mincorrelation = context.getConfiguration().getDouble("mincorrelation", mincorrelation);
	}

	/**
	 * key 格式为 cellId <br/>
	 * 如：1868801 <br/>
	 * values 格式为 ncellId,times 的集合 <br/>
	 * 如：1868803,120
	 */
	@Override
	protected void reduce(LongWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		if (++inCounter % 1000 == 0) {
			System.out.println("reduce in counter = " + inCounter);
			System.out.println("reduce time =  " + (System.currentTimeMillis() - startTimeMillis));
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
			// ncellid ,times
			cell = new Ncell(Integer.parseInt(indexs[0]), Double.parseDouble(indexs[1]));
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
		// 没有数据，下一条
		if (ncells.isEmpty()) {
			return;
		}
		// 总的测量次数
		double sumTimes = 0;
		// 计算关联度
		for (Ncell ncell : ncells) {
			sumTimes += ncell.getTimes();
		}
		// 计算关联度
		sb.setLength(0);
		sb.append("HO");
		for (Ncell ncell : ncells) {
			double assocDegree = 0;
			if (sumTimes != 0) {
				assocDegree = ncell.getTimes() / sumTimes;
			}
			if (assocDegree > mincorrelation) {
				if (++outCounter % 10000 == 0) {
					System.out.println("reduce out counter = " + outCounter);
				}
				sb.append("#").append(ncell.getId()).append(",").append(assocDegree);
			}
		}
		outVal.set(sb.toString());
		context.write(key, outVal);
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
		private double times;

		public Ncell(int id, double times) {
			super();
			this.id = id;
			this.times = times;
		}

		public int getId() {
			return id;
		}

		public double getTimes() {
			return times;
		}

		public void add(Ncell ncell) {
			if (null != ncell && this.equals(ncell)) {
				times += ncell.getTimes();
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
			return "Ncell [id=" + id + ", times=" + times + "]";
		}

		private PciReducerForHo getOuterType() {
			return PciReducerForHo.this;
		}
	}
}