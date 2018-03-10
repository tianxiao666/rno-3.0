package com.iscreate.hive.udf;

import org.apache.hadoop.hive.ql.exec.UDF;
/**
 * 经纬度
 * @author chao.xj
 *
 */
public class LngLatHelper extends UDF {

	// 地球的平均半径，单位为米
	private final static double R = 6371000;
	public double evaluate(double startlong, double startlat, double endlong, double endlat) {
		double a, b;
	      startlat = startlat * Math.PI / 180.0;
	      endlat = endlat * Math.PI / 180.0;
	      a = startlat - endlat;
	      b = (startlong - endlong) * Math.PI / 180.0;
	      double d;
	      double sa2, sb2;
	      sa2 = Math.sin(a / 2.0);
	      sb2 = Math.sin(b / 2.0);
	      d = 2 * R * Math.asin(Math.sqrt(sa2 * sa2 + Math.cos(startlat) * Math.cos(endlat) * sb2 * sb2));
	      return d;
	}
	public String evaluate(String str) {
		return str;
	}
	
}
