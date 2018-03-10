package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

public class PciMapper extends TableMapper<Text, Text> {

	private Configuration conf = null;

	/** 关联度分子字段 **/
	private String numeratorField = "";

	/** 关联度分子的值 **/
	private String numerator = "";

	/** 基站标识 **/
	private String enodebId = "";

	/** 小区标识 **/
	private String cellId = "";

	/** 小区频点 **/
	private String cellBcch = "";

	/** 小区 PCI **/
	private String cellPci = "";

	/** 邻区标识 **/
	private String ncellId = "";

	/** 邻区频点 **/
	private String ncellBcch = "";

	/** 邻区 PCI **/
	private String ncellPci = "";

	/** 小区 经度 **/
	private String cellLon = "";

	/** 小区 纬度 **/
	private String cellLat = "";

	/** 邻区 经度 **/
	private String ncellLon = "";

	/** 邻区 纬度 **/
	private String ncellLat = "";

	/** 测量次数 **/
	private String timestotal = "";

	/** 混频 **/
	private String mixingSum = "";

	/** 测量时间 **/
	private String meaTime = "";

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

	// 运行开始时间
	private long startTimeMillis = 0;

	// 距离
	private String dis = "";

	/** 距离限制 **/
	private double disLimit = 0;

	/** 邻区字段 **/
	String[] ncellArr = new String[5];

	/** 城市ID **/
	private String cityId = "";

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// 获取配置信息
		conf = context.getConfiguration();

		if (conf != null) {
			numeratorField = conf.get("numeratorField");

			if (numeratorField == null || "".equals(numeratorField)) {
				numeratorField = "RSRPTIMES1";
			} else {
				numeratorField = numeratorField.toUpperCase();
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

				timestotal = new String(navi.get("TIMESTOTAL".getBytes()));
				numerator = new String(navi.get(numeratorField.getBytes()));
				mixingSum = new String(navi.get("MIXINGSUM".getBytes()));
				meaTime = new String(navi.get("MEA_TIME".getBytes()));

				// 以 cellId,cellPci,enodebId,cellBcch,cellLon,cellLat 作为 key,因为这是一个小区的固有属性
				String strKey = cellId + "," + enodebId + "," + cellPci + "," + cellBcch + "," + cellLon + ","
						+ cellLat;
				String strValue = ncellId + "," + ncellPci + "," + ncellBcch + "," + ncellLon + "," + ncellLat + ","
						+ timestotal + "," + numerator + "," + mixingSum + "," + meaTime + "," + dis + ",MR";
				if (row.compareTo("91_1460563200000_6775831_6775833".getBytes())==0) {
					System.out.println(strKey);
					System.out.println(strValue);
				}
				sameFreqCounter++;
				sameFreqMrCounter++;
				System.out.println("strKey="+strKey);
				System.out.println("strValue="+strValue);
				context.write(new Text(strKey), new Text(strValue));
			}else {
				System.out.println("filter cellId ="+cellId);
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
			// 针对不同的厂家处理
			if (("RNO_4G_ERI_HO").equals(tableName.toString())) {
				// 邻区ID
				ncellArr = new String(navi.get(Bytes.toBytes("EUTRANCELLRELATION"))).split("-");
				ncellId = ncellArr[1] + ncellArr[2];
				// 小区ID
				cellId = new String(navi.get(Bytes.toBytes("BUS_CELL_ID")));
			} else if (("RNO_4G_ZTE_HO").equals(tableName.toString())) {
				// 邻区ID
				ncellArr = new String(navi.get(Bytes.toBytes("NCELL"))).split(":");
				ncellId = ncellArr[3] + ncellArr[4];
				// 小区ID
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
				timestotal = new String(navi.get(Bytes.toBytes("PMHOEXEATTLTEINTRAF")));

				String strKey = cellId + "," + enodebId + "," + cellPci + "," + cellBcch + "," + cellLon + ","
						+ cellLat;
				String strValue = ncellId + "," + ncellPci + "," + ncellBcch + "," + ncellLon + "," + ncellLat + ","
						+ timestotal + "," + meaTime + "," + cityId + "," + dis + ",HO";
				sameFreqCounter++;
				sameFreqHoCounter++;
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