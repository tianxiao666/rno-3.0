package com.iscreate.op.service.rno.mapreduce.matrix;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

public class G4PciMatrixMapper extends TableMapper<Text, Text> {

	/** 城市ID **/
	private String cityId = "";

	/** 测量时间 **/
	private String meaTime = "";

	/** 基站ID **/
	private String enodebId = "";

	/** 小区ID **/
	private String cellId = "";

	/** 小区PCI **/
	private String cellPci = "";

	/** 小区频点 **/
	private String cellBcch = "";

	/** 小区 经度 **/
	private String cellLon = "";

	/** 小区 纬度 **/
	private String cellLat = "";

	/** 邻区ID **/
	private String ncellId = "";

	/** 邻区PCI **/
	private String ncellPci = "";

	/** 邻区频点 **/
	private String ncellBcch = "";

	/** 邻区 经度 **/
	private String ncellLon = "";

	/** 邻区 纬度 **/
	private String ncellLat = "";

	/** 分子 **/
	private String numerator = "RSRPTIMES1";

	/** 分子 **/
	private String time1 = "";

	/** 混频 **/
	private String mixingSum = "";

	/** 距离 **/
	private String dis = "";

	Configuration conf = null;

	String[] ncellArr = new String[5];

	/** 距离限制 **/
	private double disLimit = 0;

	/** 行统计 **/
	private long counter = 0;

	/** MR行统计 **/
	private long mrCounter = 0;

	/** HO行统计 **/
	private long hoCounter = 0;

	/** 过滤后的同频行数统计 **/
	private long sameFreqCounter = 0;

	/** 过滤后的同频MR行数统计 **/
	private long sameFreqMrCounter = 0;

	/** 过滤后的同频HO行数统计 **/
	private long sameFreqHoCounter = 0;

	/** 运行时间 **/
	private long startTimeMillis = 0;

	/** 保存字符串 **/
	StringBuffer sbValue = new StringBuffer();

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// 获取配置信息
		conf = context.getConfiguration();

		if (conf != null) {
			if (conf.get("numerator") != null && !"".equals(conf.get("numerator"))) {
				numerator = conf.get("numerator").toUpperCase();
			}
			if (conf.get("dislimit") == null || "".equals(conf.get("dislimit"))) {
				disLimit = 5000;
			} else {
				disLimit = Double.parseDouble(conf.get("dislimit"));
			}
		}
		System.out.println("PCIMapper 执行setup中..");
		super.setup(context);
		System.out.println("map counter = " + counter);
		startTimeMillis = System.currentTimeMillis();
	}

	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		if (++counter % 10000 == 0) {
			System.out.println("map counter = " + counter);
		}

		TableSplit split = (TableSplit) context.getInputSplit();
		String tableName = split.getTable().toString();
		NavigableMap<byte[], byte[]> navi = null;

		if (tableName.endsWith("MR")) {
			if (++mrCounter % 10000 == 0) {
				System.out.println("map mr counter = " + mrCounter);
			}
			// MR 数据（中兴或爱立信的）
			navi = value.getFamilyMap(Bytes.toBytes("MRINFO"));
			enodebId = new String(navi.get("ENODEB_ID".getBytes()));
			cellBcch = new String(navi.get("CELL_BCCH".getBytes()));
			ncellBcch = new String(navi.get("NCELL_BCCH".getBytes()));
			ncellId = new String(navi.get("NCELL_ID".getBytes()));
			cellId = new String(navi.get("CELL_ID".getBytes()));
			dis = new String(navi.get("DISTANCE".getBytes()));
			// 过滤非同频数据
			if (!"".equals(cellId) && !"".equals(ncellId) && !"".equals(ncellBcch) && cellBcch.equals(ncellBcch)
					&& Double.parseDouble(dis) * 1000 < disLimit) {
				cellPci = new String(navi.get("CELL_PCI".getBytes()));
				cellLon = new String(navi.get("CELL_LON".getBytes()));
				cellLat = new String(navi.get("CELL_LAT".getBytes()));

				ncellPci = new String(navi.get("NCELL_PCI".getBytes()));
				ncellLon = new String(navi.get("NCELL_LON".getBytes()));
				ncellLat = new String(navi.get("NCELL_LAT".getBytes()));

				cityId = new String(navi.get("CITY_ID".getBytes()));

				meaTime = new String(navi.get("MEA_TIME".getBytes()));
				time1 = new String(navi.get(numerator.getBytes()));
				mixingSum = new String(navi.get("MIXINGSUM".getBytes()));

				String strKey = cellId + "," + enodebId + "," + cellPci + "," + cellBcch + "," + cellLon + ","
						+ cellLat;
				if (strKey.split(",").length < 6) {
					System.out.println("cellId=" + cellId + "数据不全。strKey=" + strKey);
					return;
				}
				sameFreqCounter++;
				sameFreqMrCounter++;
				String strValue = ncellId + "," + ncellPci + "," + ncellBcch + "," + ncellLon + "," + ncellLat + ","
						+ meaTime + "," + time1 + "," + mixingSum + "," + dis + "," + cityId + ",MR";
				context.write(new Text(strKey), new Text(strValue));
			}
		} else if (tableName.endsWith("HO")) {
			if (++hoCounter % 10000 == 0) {
				System.out.println("map ho counter = " + hoCounter);
			}
			navi = value.getFamilyMap(Bytes.toBytes("HOINFO"));
			enodebId = new String(navi.get("ENODEBID".getBytes()));
			cellBcch = new String(navi.get(Bytes.toBytes("CELL_BCCH")));
			ncellBcch = new String(navi.get(Bytes.toBytes("NCELL_BCCH")));
			dis = new String(navi.get("DISTANCE".getBytes()));
			//针对不同的厂家处理
			if (("RNO_4G_ERI_HO").equals(tableName)) {
				//邻区ID
				ncellArr = new String(navi.get(Bytes.toBytes("EUTRANCELLRELATION"))).split("-");
				ncellId = ncellArr[1] + ncellArr[2];
				//小区ID
				cellId = new String(navi.get(Bytes.toBytes("BUS_CELL_ID")));
			} else if (("RNO_4G_ZTE_HO").equals(tableName)) {
				//邻区ID
				ncellArr = new String(navi.get(Bytes.toBytes("NCELL"))).split(":");
				ncellId = ncellArr[3] + ncellArr[4];
				//小区ID
				cellId = enodebId + new String(navi.get(Bytes.toBytes("CELLID")));
			} else {
				return;
			}

			if (!"".equals(cellId) && !"".equals(ncellId) && !"".equals(ncellBcch) && cellBcch.equals(ncellBcch)) {
				cellPci = new String(navi.get(Bytes.toBytes("CELL_PCI")));
				cellLon = new String(navi.get("CELL_LON".getBytes()));
				cellLat = new String(navi.get("CELL_LAT".getBytes()));

				ncellPci = new String(navi.get(Bytes.toBytes("NCELL_PCI")));
				ncellLon = new String(navi.get("NCELL_LON".getBytes()));
				ncellLat = new String(navi.get("NCELL_LAT".getBytes()));

				cityId = new String(navi.get(Bytes.toBytes("CITY_ID")));
				meaTime = new String(navi.get(Bytes.toBytes("MEA_TIME")));
				time1 = new String(navi.get(Bytes.toBytes("PMHOPREPATTLTEINTRAF")));

				String strKey = cellId + "," + enodebId + "," + cellPci + "," + cellBcch + "," + cellLon + ","
						+ cellLat;
				if (strKey.split(",").length < 6) {
					System.out.println("cellId=" + cellId + "数据不全。strKey=" + strKey);
					return;
				}
				sameFreqCounter++;
				sameFreqHoCounter++;
				String strValue = ncellId + "," + ncellPci + "," + ncellBcch + "," + ncellLon + "," + ncellLat
						+ "," + meaTime + "," + time1 + "," + dis + "," + cityId + ",HO";
				context.write(new Text(strKey), new Text(strValue));
			}
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		System.out.println("map counter = " + counter);
		System.out.println("map mr counter = " + mrCounter);
		System.out.println("map ho counter = " + hoCounter);
		System.out.println("same freq total = " + sameFreqCounter);
		System.out.println("same freq mr total = " + sameFreqMrCounter);
		System.out.println("same freq ho total = " + sameFreqHoCounter);
		System.out.println("map spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}
}