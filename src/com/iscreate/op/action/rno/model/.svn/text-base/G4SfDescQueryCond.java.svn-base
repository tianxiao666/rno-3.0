package com.iscreate.op.action.rno.model;

import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FirstKeyOnlyFilter;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.util.Bytes;

import com.iscreate.op.service.rno.tool.DateUtil;


/**
 * 查询SF扫频测量描述新的查询条件
 * @author li.tf
 *
 */
public class G4SfDescQueryCond {
	private long cityId;
	private String factory;
	private String meaBegTime;
	private String meaEndTime;
	private final static String tableName = "RNO_4G_SF_DESC";

	public long getCityId() {
		return cityId;
	}
	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public String getMeaBegTime() {
		return meaBegTime;
	}
	public void setMeaBegTime(String meaBegTime) {
		this.meaBegTime = meaBegTime;
	}
	public String getMeaEndTime() {
		return meaEndTime;
	}
	public void setMeaEndTime(String meaEndTime) {
		this.meaEndTime = meaEndTime;
	}
	public String getFactory() {
		return factory;
	}
	public void setFactory(String factory) {
		this.factory = factory;
	}
	
	public String buildStartRow() {
		DateUtil dateUtil=new DateUtil();
		long sMill = dateUtil.parseDateArbitrary(this.meaBegTime).getTime();
		String startRow = cityId+"_"+sMill+"_#";
		return startRow;
	}
	public String buildStopRow() {
		DateUtil dateUtil=new DateUtil();
		long eMill = dateUtil.parseDateArbitrary(this.meaEndTime).getTime();
		String stopRow = cityId+"_"+eMill+"_~";
		return stopRow;
	}
	public String buildTable() {
		return tableName.toUpperCase();
	}
	@Override
	public String toString() {
		return "G4SfDescQueryCond [cityId=" + cityId + ", factory=" + factory + ", meaBegTime=" + meaBegTime
				+ ", meaEndTime=" + meaEndTime + "]";
	}
	/**
	 * 
	 * @title 封装过滤查询条件
	 * @param isPage
	 * @param cond
	 * @return
	 * @author chen.c10
	 * @date 2016-3-7 上午12:08:55
	 */
	public FilterList getSfDescFilters(boolean isPage) {
		FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);
		Filter filter1 = null;
		if (!"ALL".equals(factory)) {
			filter1 = new SingleColumnValueFilter(Bytes.toBytes("DESCINFO"), Bytes.toBytes("FACTORY"), CompareOp.EQUAL,
					Bytes.toBytes(factory));
			((SingleColumnValueFilter) filter1).setFilterIfMissing(true);
			filterList.addFilter(filter1);
		} else {
			if (isPage) {
				filterList.addFilter(new FirstKeyOnlyFilter());
			}
		}
		return filterList;
	}
}
