package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class PciImportMapper extends Mapper<Object, Text, Text, Text> {

	Configuration conf = null;

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

	/** 关联度 **/
	private String assocDegree = "";

	/** 同站小区 **/
	private String sameStationCells = "";

	/** 行统计 **/
	private long counter = 0;

	/** 开始运行时间 **/
	private long startTimeMillis = 0;

	/** 缓存key **/
	private StringBuffer sbKey = new StringBuffer();

	/** 缓存value **/
	private StringBuffer sbValue = new StringBuffer();

	/** 过滤后的同频行数统计 **/
	private long sameFreqCounter = 0;

	/** 分割value **/
	private String[] vals = {};

	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		conf = context.getConfiguration();
		System.out.println("进入到mapper");
		super.setup(context);
	}

	protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		// line=cell+"#"+ncellId+"#"+cosi+"#"+cellPci+"#"+ncellPci;
		// CELL##NCELL,COSI;CELL2,CELL3;CELLPCI;NCELLPCI
		// 读入格式
		// ex:小区标识 , 邻区标识,关联度,小区频点,小区PCI,邻区频点,邻区PCI,同站cells
		// 修正格式
		// line=cell+"##"+ncellId+","+cosi+";"+cells+";"+cellPci+";"+ncellPci;
		// ncellId+","+cosi+";"+cells+";"+cellPci+";"+ncellPci;
		if (++counter % 10000 == 0) {
			System.out.println("map counter = " + counter);
		}
		vals = value.toString().split(",|\t");
		// 判断输入字段是否完整，小区ID是否是数字来过滤数据
		if (vals.length == 13 && isNumeric(getStringNoBlank(vals[0])) && isNumeric(getStringNoBlank(vals[1]))) {
			ncellId = Pattern.compile("[^0-9]").matcher(vals[1]).replaceAll("");
			cellId = Pattern.compile("[^0-9]").matcher(vals[0]).replaceAll("");
			if (!"".equals(ncellId) && !"".equals(cellId)) {
				ncellBcch = vals[5];
				cellBcch = vals[3];
				if (cellBcch.equals(ncellBcch) && !"".equals(ncellBcch) && !"".equals(ncellId)) {
					cellPci = vals[4];
					ncellPci = vals[6];
					assocDegree = vals[2];
					sameStationCells = vals[12].replace("_", ",");
					cellLon = vals[7];
					cellLat = vals[8];
					ncellLon = vals[9];
					ncellLat = vals[10];

					sameFreqCounter++;
					sbKey.setLength(0);
					sbValue.setLength(0);
					// cellId+","+cellPci+","+cellBcch+","+cellLon+","+cellLat";"+sameStationCells 数据构成
					sbKey.append(cellId).append(",").append(cellPci).append(",").append(cellBcch).append(",")
							.append(cellLon).append(",").append(cellLat).append(";").append(sameStationCells);
					// ncellId+","+ncellPci+","+ncellBcch+","+ncellLon+","+ncellLon +","+assocDegree 数据构成
					sbValue.append(ncellId).append(",").append(ncellPci).append(",").append(ncellBcch).append(",")
							.append(ncellLon).append(",").append(ncellLat).append(",").append(assocDegree);
					context.write(new Text(sbKey.toString()), new Text(sbValue.toString()));
				}
			}
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		System.out.println("map counter total = " + counter);
		System.out.println("same freq total = " + sameFreqCounter);
		System.out.println("map spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	/**
	 * 
	 * @title 判断是否为数字
	 * @param str
	 * @return
	 * @author chao.xj
	 * @date 2015-5-20下午3:51:12
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public boolean isNumeric(String str) {
		// Pattern pattern = Pattern.compile("[0-9]*");
		Pattern pattern = Pattern.compile("[0-9]+");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param str
	 * @return
	 * @author chen.c10
	 * @date 2015年12月21日 下午6:40:11
	 * @company 怡创科技
	 * @version V1.0
	 */
	public static String getStringNoBlank(String str) {
		if (str != null && !"".equals(str)) {
			Pattern p = Pattern.compile("\\s*|\t|\r|\n");
			Matcher m = p.matcher(str);
			String strNoBlank = m.replaceAll("");
			return strNoBlank;
		} else {
			return str;
		}
	}
	public static void main(String[] args) {
		String str = "  ";
		String[] arr = str.split(",");
		System.out.println(arr[0]+"_");
	}
}
