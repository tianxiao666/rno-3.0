package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 汇总各种数据的关联度
 * @author chen.c10
 *
 */
public class PciReducerSum extends Reducer<LongWritable, Text, NullWritable, Text> {

	private static Logger logger = LoggerFactory.getLogger(PciReducerSum.class);

	private enum CALC_TYPE {
		PCI, MATRIX
	}
	private final static String MR_TAG = "MR#";
	private final static String HO_TAG = "HO#";
	private final static String SF_TAG = "SF#";
	private final static NullWritable NULLKEY = NullWritable.get();
	/** 类型 **/
	private CALC_TYPE type;
	/** 输出的value **/
	final Text outputVal = new Text();
	/** 权值 **/
	double samefreqcellcoefweight = 0.8;
	/** 切换比例权值 **/
	double switchratioweight = 0.2;
	/** 小区ID **/
	int cellId;
	/** 邻区字符串 **/
	String valStr;
	/** 邻区数组 **/
	String[] indexs;
	/** 邻区数组 **/
	String[] valArr;
	/** 拼装字符串 **/
	StringBuilder sb = new StringBuilder();
	/** 邻区列表 **/
	List<Ncell> ncells;

	// reduce 处理次数统计
	long inCounter = 0;

	// reduce 处理次数统计
	long outCounter = 0;

	// 处理行数统计
	long lineCounter = 0;

	/** MR 处理行统计 **/
	long mrLineCounter = 0;

	/** HO 处理行统计 **/
	long hoLineCounter = 0;

	/** SF 处理行统计 **/
	long sfLineCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		// 初始化配置信息
		Configuration conf = context.getConfiguration();
		samefreqcellcoefweight = conf.getDouble("samefreqcellcoefweight", samefreqcellcoefweight);
		switchratioweight = conf.getDouble("switchratioweight", switchratioweight);
		if ("MATRIX".equals(conf.get("CALC_TYPE"))) {
			type = CALC_TYPE.MATRIX;
		} else {
			type = CALC_TYPE.PCI;
		}
	}

	/**
	 * key 为 cellId，如 1868801
	 * values 格式为 ncellId,timestotal,time1,enodebId,cellPci,ncellPci,cellBcch,ncellBcch 的集合
	 * 如：1868803,120,53,186880,462,335,38100,38100
	 */
	@Override
	protected void reduce(LongWritable key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		if (++inCounter % 1000 == 0) {
			System.out.println("reduce in counter = " + inCounter + "  ,reduce time =  "
					+ (System.currentTimeMillis() - startTimeMillis));
		}
		// 邻区数据
		ncells = new ArrayList<Ncell>();
		// 主小区ID
		cellId = Integer.parseInt(key.toString());
		// 按照类型分别处理
		for (Text val : values) {
			lineCounter++;
			valStr = val.toString();
			if (valStr.startsWith(MR_TAG)) {
				indexs = valStr.replace(MR_TAG, "").split("#");
				for (String nVal : indexs) {
					valArr = nVal.split(",");
					if (++mrLineCounter % 100000 == 0) {
						System.out.println("reduce mr line counter = " + mrLineCounter);
					}
					getNcellById(Integer.parseInt(valArr[0])).setMrAssocDegree(Double.parseDouble(valArr[1]));
				}
			} else if (valStr.startsWith(HO_TAG)) {
				indexs = valStr.replace(HO_TAG, "").split("#");
				for (String nVal : indexs) {
					valArr = nVal.split(",");
					if (++hoLineCounter % 100000 == 0) {
						System.out.println("reduce ho line counter = " + hoLineCounter);
					}
					getNcellById(Integer.parseInt(valArr[0])).setHoAssocDegree(Double.parseDouble(valArr[1]));
				}
			} else if (valStr.startsWith(SF_TAG)) {
				indexs = valStr.replace(SF_TAG, "").split("#");
				for (String nVal : indexs) {
					valArr = nVal.split(",");
					if (++sfLineCounter % 100000 == 0) {
						System.out.println("reduce sf line counter = " + sfLineCounter);
					}
					getNcellById(Integer.parseInt(valArr[0])).setSfAssocDegree(Double.parseDouble(valArr[1]));
				}
			} else {
				System.out.println("cellId=" + cellId + ",其中一条数据不合法,valStr="+valStr);
			}
		}
		// 无数据，直接下一条
		if (ncells.isEmpty()) {
			return;
		}
		// 输出部分
		switch (type) {
		case MATRIX:
			// 计算关联度
			for (Ncell ncell : ncells) {
				ncell.calcAssocDegree(samefreqcellcoefweight, switchratioweight);
				sb.setLength(0);
				sb.append(cellId).append("#");
				sb.append(ncell.getId()).append("#");
				sb.append(ncell.getAssocDegree()).append("#");
				sb.append(ncell.getMrAssocDegree()).append("#");
				sb.append(ncell.getHoAssocDegree()).append("#");
				sb.append(ncell.getSfAssocDegree());
				outputVal.set(sb.toString());
				if (++outCounter % 100000 == 0) {
					System.out.println("reduce out counter = " + outCounter);
				}
				context.write(NULLKEY, outputVal);
			}
			break;

		default:
			//总关联度 
			double totalAssocDegree = 0;
			for (Ncell ncell : ncells) {
				ncell.calcAssocDegree(samefreqcellcoefweight, switchratioweight);
				totalAssocDegree += ncell.getAssocDegree();
			}
			sb.setLength(0);
			sb.append(cellId).append(",");
			sb.append(totalAssocDegree).append(",");
			for (Ncell ncell : ncells) {
				sb.append(ncell.getId()).append("=").append(ncell.getAssocDegree()).append("#");
			}
			sb.substring(0, sb.length() - 1);
			outputVal.set(sb.toString());
			if (++outCounter % 100000 == 0) {
				System.out.println("reduce out counter = " + outCounter);
			}
			context.write(NULLKEY, outputVal);
			break;
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		logger.info("reduce in counter total = " + inCounter);
		logger.info("reduce out counter total = " + outCounter);
		logger.info("reduce line counter total = " + lineCounter);
		logger.info("reduce mr line counter total = " + mrLineCounter);
		logger.info("reduce ho line counter total = " + hoLineCounter);
		logger.info("reduce sf line counter total = " + sfLineCounter);
		logger.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	public Ncell getNcellById(int ncellId) {
		for (Ncell oldNcell : ncells) {
			if (oldNcell.getId() == ncellId) {
				return oldNcell;
			}
		}
		Ncell ncell = new Ncell(ncellId);
		ncells.add(ncell);
		return ncell;
	}

	class Ncell {
		private int id; // 邻区标识
		private double assocDegree = 0.0;// 关联度
		private double mrAssocDegree = 0.0;// 关联度
		private double hoAssocDegree = 0.0;// 关联度
		private double sfAssocDegree = 0.0;// 关联度

		public Ncell(int id) {
			super();
			this.id = id;
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

		private PciReducerSum getOuterType() {
			return PciReducerSum.this;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public double getAssocDegree() {
			return assocDegree;
		}

		public double getMrAssocDegree() {
			return mrAssocDegree;
		}

		public void setMrAssocDegree(double mrAssocDegree) {
			this.mrAssocDegree = mrAssocDegree;
		}

		public double getHoAssocDegree() {
			return hoAssocDegree;
		}

		public void setHoAssocDegree(double hoAssocDegree) {
			this.hoAssocDegree = hoAssocDegree;
		}

		public double getSfAssocDegree() {
			return sfAssocDegree;
		}

		public void setSfAssocDegree(double sfAssocDegree) {
			this.sfAssocDegree = sfAssocDegree;
		}

		/**
		 * 计算该邻区关联度
		 * 
		 * @param samefreqcellcoefweight
		 * @param switchratioweight
		 * @author chen.c10
		 * @date 2016年3月25日
		 * @version RNO 3.0.1
		 */
		public void calcAssocDegree(double samefreqcellcoefweight, double switchratioweight) {
			assocDegree = (mrAssocDegree > sfAssocDegree ? mrAssocDegree : sfAssocDegree) * samefreqcellcoefweight
					+ hoAssocDegree * switchratioweight;
		}
	}
}