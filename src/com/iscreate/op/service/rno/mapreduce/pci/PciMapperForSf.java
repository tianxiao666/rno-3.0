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
import org.apache.hadoop.io.Text;

/**
 * @author chen.c10
 *
 */
public class PciMapperForSf extends TableMapper<Text, Text> {

	// 对hbase表中可能用的column初始化
	final static byte[] FAMILY_MAP = Bytes.toBytes("SFINFO");
	final static byte[] CELL_ID = Bytes.toBytes("CELL_ID");
	final static byte[] NCELL_ID = Bytes.toBytes("NCELL_ID");
	final static byte[] RSRPTIMES0 = Bytes.toBytes("RSRPTIMES0");
	final static byte[] RSRPTIMES1 = Bytes.toBytes("RSRPTIMES1");
	final static byte[] MIXINGSUM = Bytes.toBytes("MIXINGSUM");
	final static byte[] MEA_TIME = Bytes.toBytes("MEA_TIME");

	private String tableName = "";
	private String[] keyArr;
	private NavigableMap<byte[], byte[]> navi = null;

	private StringBuilder sb = new StringBuilder();
	
	private Text outKey = new Text();
	private Text outVal = new Text();

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
		tableName = new String(((TableSplit) context.getInputSplit()).getTableName());
		System.out.println("tableName=" + tableName);
	}

	@Override
	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		if(!tableName.endsWith("SF")){
			return;
		}
		if (++inCounter % 100000 == 0) {
			System.out.println("map in counter = " + inCounter);
		}

		// like 88_1458489600000_fn*5*fn_1728671_1736006
		// cityID mea_time fileID cellID ncellID
		keyArr = Bytes.toString(row.get()).split("_");

		if (null != keyArr && keyArr.length == 5 && !keyArr[3].isEmpty()) {
			// MR 数据（中兴或爱立信的）
			navi = value.getFamilyMap(FAMILY_MAP);
			// 清空
			sb.setLength(0);
			// 拼装key
			sb.append(keyArr[3]).append(",");
			sb.append(Bytes.toString(navi.get(MEA_TIME))).append(",");
			sb.append(Bytes.toString(navi.get(MIXINGSUM)));
			// 装入text
			outKey.set(sb.toString());
			// 清空
			sb.setLength(0);
			// 拼装value
			sb.append(keyArr[4]).append(",");
			sb.append(Bytes.toString(navi.get(RSRPTIMES0))).append(",");
			sb.append(Bytes.toString(navi.get(RSRPTIMES1)));
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
