package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.io.Text;

public class G4PciAzimuthMapper extends TableMapper<Text, Text> {
	private static final byte[] MrInfo_BYTE = "MRINFO".getBytes();
	private static final byte[] EnodebId_BYTE = "ENODEB_ID".getBytes();
	private static final byte[] CellId_BYTE = "CELL_ID".getBytes();
	private static final byte[] CityId_BYTE = "CITY_ID".getBytes();
	private static final byte[] MeaTime_BYTE = "MEA_TIME".getBytes();
	private static final byte[] CellLon_BYTE = "CELL_LON".getBytes();
	private static final byte[] CellLat_BYTE = "CELL_LAT".getBytes();
	private static final byte[] CellBcch_BYTE = "CELL_BCCH".getBytes();
	private static final byte[] NcellId_BYTE = "NCELL_ID".getBytes();
	private static final byte[] Ncell_BYTE = "NCELL".getBytes();
	private static final byte[] NcellLon_BYTE = "NCELL_LON".getBytes();
	private static final byte[] NcellLat_BYTE = "NCELL_LAT".getBytes();
	private static final byte[] NcellBcch_BYTE = "NCELL_BCCH".getBytes();
	private static final byte[] TimesTotal_BYTE = "TIMESTOTAL".getBytes();
	private static final byte[] Distance_BYTE = "DISTANCE".getBytes();
	private static final byte[] Mixingsum_BYTE = "MIXINGSUM".getBytes();
	private static final String EBand = "e|E|E频段";
	private String enodebId = "";
	private String cityId = "";
	private String meaTime = "";
	private String cellId = "";
	private String cellLon = "";
	private String cellLat = "";
	private String ncellId = "";
	private String ncell = "";
	private String ncellLon = "";
	private String ncellLat = "";
	private String timestotal = "";
	private double distance;
	/** 最大距离限制，单位米 **/
	private double maxDisLimit = 5000;
	/** 最小距离限制，单位米，用来排除同站小区，暂时默认100 **/
	private double minDisLimit = 100;
	private String cellBcch = "";
	private String ncellBcch = "";
	private String mixingSum = "";// 混频
	private String numerator = "";
	private String time1 = "";
	private String bandType;
	private Map<String, String> cellIdToBandTypes = new HashMap<String, String>();
	private NavigableMap<byte[], byte[]> navi = null;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// 获取配置信息
		Configuration conf = context.getConfiguration();
		// System.out.println("setup 中的conf能获得吗?="+conf);
		if (conf != null) {
			numerator = conf.get("numerator");
			numerator = numerator.toUpperCase();
			String arr[] = conf.getStrings("cellIdToBandType");
			if (arr != null && arr.length != 0) {
				cellIdToBandTypes = string2Map(arr[0]);
			}
			if (conf.get("dislimit") == null || "".equals(conf.get("dislimit"))) {
				maxDisLimit = 5000;
			} else {
				maxDisLimit = Double.parseDouble(conf.get("dislimit"));
			}
		}
		System.out.println("PCIMapper 执行setup中..改变了....");
		super.setup(context);
	}

	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		navi = value.getFamilyMap(MrInfo_BYTE);
		cellId = new String(navi.get(CellId_BYTE));
		bandType = cellIdToBandTypes.get(cellId);
		if (bandType == null || Pattern.matches(EBand, bandType)) {
			System.out.println("小区cellId=" + cellId + ",匹配不到工参，过滤。。");
			return;
		}
		enodebId = new String(navi.get(EnodebId_BYTE));
		cityId = new String(navi.get(CityId_BYTE));
		meaTime = new String(navi.get(MeaTime_BYTE));
		cellLon = new String(navi.get(CellLon_BYTE));
		cellLat = new String(navi.get(CellLat_BYTE));
		cellBcch = new String(navi.get(CellBcch_BYTE));
		ncellId = new String(navi.get(NcellId_BYTE));
		ncell = new String(navi.get(Ncell_BYTE));
		ncellLon = new String(navi.get(NcellLon_BYTE));
		ncellLat = new String(navi.get(NcellLat_BYTE));
		ncellBcch = new String(navi.get(NcellBcch_BYTE));
		timestotal = new String(navi.get(TimesTotal_BYTE));
		distance = Double.parseDouble(new String(navi.get(Distance_BYTE))) * 1000;
		mixingSum = new String(navi.get(Mixingsum_BYTE));
		time1 = new String(navi.get(numerator.getBytes()));
		/*
		 * LTE剔除条件
		 * 1. 排除E频段（E频段就是室分，不参与计算）
		 * 2. 近距离邻区不考虑，暂定为距离小于100米的邻区不参与计算
		 */
		// 同频累加
		if (!"".equals(ncellId) && !"".equals(ncellBcch) && cellBcch.equals(ncellBcch) && distance > minDisLimit
				&& distance < maxDisLimit) {
			// 添加字段混频项,并且从以小区为主服务小区变更为以邻区为主服务小区
			context.write(new Text(ncellId), new Text(cellId + "," + timestotal + "," + time1 + "," + enodebId + ","
					+ cityId + "," + meaTime + "," + ncellLon + "," + ncellLat + "," + cellLon + "," + cellLat + ","
					+ ncell + "," + mixingSum + ",MR"));
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
	}

	public static Map<String, String> string2Map(String enodebs) {
		Map<String, String> enodebToCells = new HashMap<String, String>();
		String keyarr[] = null;
		String valarr[] = null;
		keyarr = enodebs.split("\\|");
		for (int i = 0; i < keyarr.length; i++) {
			valarr = keyarr[i].split("=");
			enodebToCells.put(valarr[0], valarr[1].replace("#", ","));
		}
		return enodebToCells;
	}
}
