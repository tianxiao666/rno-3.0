/**
 * 
 */
package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

/**
 * @author chen.c10
 *
 */
public class PciMapperForMr extends TableMapper<Text, Text> {

	// 对hbase表中可能用的column初始化
	final static byte[] FAMILYMAP = Bytes.toBytes("MRINFO");
	final static byte[] CELL_ID = Bytes.toBytes("CELL_ID");
	final static byte[] NCELL_ID = Bytes.toBytes("NCELL_ID");
	final static byte[] DISTANCE = Bytes.toBytes("DISTANCE");
	final static byte[] TIMESTOTAL = Bytes.toBytes("TIMESTOTAL");
	final static byte[] RSRPTIMES0 = Bytes.toBytes("RSRPTIMES0");
	final static byte[] RSRPTIMES1 = Bytes.toBytes("RSRPTIMES1");
	final static byte[] MIXINGSUM = Bytes.toBytes("MIXINGSUM");
	final static byte[] MEA_TIME = Bytes.toBytes("MEA_TIME");

	private NavigableMap<byte[], byte[]> navi = null;

	private StringBuilder sb = new StringBuilder();
	
	private Text outKey = new Text();
	private Text outVal = new Text();

	/** 小区标识 **/
	private String cellId = "";

	/** 邻区标识 **/
	private String ncellId = "";

	/** 行统计 **/
	private long inCounter = 0;

	/** MR行统计 **/
	private long outCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	/** 距离限制 **/
	private double disLimit = 5000;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		// 获取配置信息
		disLimit = context.getConfiguration().getDouble("dislimit", disLimit);
		startTimeMillis = System.currentTimeMillis();
	}

	@Override
	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		if (++inCounter % 100000 == 0) {
			System.out.println("map in counter = " + inCounter);
		}

		// MR 数据（中兴或爱立信的）
		navi = value.getFamilyMap(FAMILYMAP);
		cellId = Bytes.toString(navi.get(CELL_ID));
		ncellId = Bytes.toString(navi.get(NCELL_ID));
		// 过滤非同频数据
//		if (null != cellId && !"".equals(cellId) && !"".equals(ncellId) && !cellId.equals(ncellId)
//				&& Double.parseDouble(Bytes.toString(navi.get(DISTANCE))) * 1000 < disLimit) {
		if (StringUtils.isNotBlank(cellId) && StringUtils.isNotBlank(ncellId) && !StringUtils.equals(cellId, ncellId)) {
			//装入text
			outKey.set(cellId);
			// 清空
			sb.setLength(0);
			// 拼装value
			sb.append(ncellId).append(",");
			sb.append(Bytes.toString(navi.get(MEA_TIME))).append(",");
			sb.append(Bytes.toString(navi.get(MIXINGSUM))).append(",");
			sb.append(Bytes.toString(navi.get(TIMESTOTAL))).append(",");
			sb.append(Bytes.toString(navi.get(RSRPTIMES0))).append(",");
			sb.append(Bytes.toString(navi.get(RSRPTIMES1)));
			//装入value
			outVal.set(sb.toString());
			System.out.println(sb.toString());

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
