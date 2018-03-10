/**
 * 
 */
package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

/**
 * @author chen.c10
 *
 */
public class PciMapperForHo extends TableMapper<LongWritable, Text> {

	// 对hbase表中可能用的column初始化
	final static byte[] FAMILYMAP = Bytes.toBytes("HOINFO");
	final static byte[] ENODEBID = Bytes.toBytes("ENODEBID");
	final static byte[] EUTRANCELLRELATION = Bytes.toBytes("EUTRANCELLRELATION");
	final static byte[] BUS_CELL_ID = Bytes.toBytes("BUS_CELL_ID");
	final static byte[] NCELL = Bytes.toBytes("NCELL");
	final static byte[] CELLID = Bytes.toBytes("CELLID");
	final static byte[] PMHOEXEATTLTEINTRAF = Bytes.toBytes("PMHOEXEATTLTEINTRAF");

	private String tableName = "";

	private NavigableMap<byte[], byte[]> navi = null;

	private StringBuilder sb = new StringBuilder();

	LongWritable outKey = new LongWritable();
	Text outVal = new Text();

	/** 基站标识 **/
	private String enodebId = "";

	/** 小区标识 **/
	private String cellId = "";

	/** 邻区标识 **/
	private String ncellId = "";

	/** 邻区字段 **/
	private String[] ncellArr = new String[5];

	/** 行统计 **/
	private long inCounter = 0;

	/** MR行统计 **/
	private long outCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		tableName = new String(((TableSplit) context.getInputSplit()).getTableName());
		System.out.println("tableName=" + tableName);
	}

	@Override
	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		if (!tableName.endsWith("HO")) {
			return;
		}
		if (++inCounter % 100000 == 0) {
			System.out.println("map in counter = " + inCounter);
		}

		// HO 数据（中兴或爱立信的）
		navi = value.getFamilyMap(FAMILYMAP);
		enodebId = Bytes.toString(navi.get(ENODEBID));
		// 针对不同的厂家处理
		if (("RNO_4G_ERI_HO").equals(tableName)) {
			// 邻区ID
			ncellArr = Bytes.toString(navi.get(EUTRANCELLRELATION)).split("-");
			ncellId = ncellArr[1] + ncellArr[2];
			// 小区ID
			cellId = Bytes.toString(navi.get(BUS_CELL_ID));
		} else if (("RNO_4G_ZTE_HO").equals(tableName)) {
			// 邻区ID
			ncellArr = Bytes.toString(navi.get(NCELL)).split(":");
			ncellId = ncellArr[3] + ncellArr[4];
			// 小区ID
			cellId = enodebId + Bytes.toString(navi.get(CELLID));
		} else {
			return;
		}
		// 过滤非同频数据
		if (null != cellId && !"".equals(cellId) && !"".equals(ncellId) && !cellId.equals(ncellId)) {
			// 装入text
			outKey.set(Long.parseLong(cellId));
			// 清空
			sb.setLength(0);
			// 拼装value
			sb.append(ncellId).append(",");
			sb.append(Bytes.toString(navi.get(PMHOEXEATTLTEINTRAF)));
			// 装入value
			outVal.set(sb.toString());

			if (++outCounter % 100000 == 0) {
				System.out.println("map out counter = " + outCounter);
			}
			context.write(outKey, outVal);
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		System.out.println("map in counter total = " + inCounter);
		System.out.println("map out counter total = " + outCounter);
		System.out.println("map spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}
}
