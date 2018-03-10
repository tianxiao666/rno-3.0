package com.iscreate.op.service.rno.mapreduce.model;

/**保存ho数据**/
public class HoData {
	private double timesTotal; // 测量次数
	private double assocDegree = 0.0;// 关联度
	public double getTimesTotal() {
		return timesTotal;
	}
	public void setTimesTotal(double timesTotal) {
		this.timesTotal = timesTotal;
	}
	public double getAssocDegree() {
		return assocDegree;
	}
	public void setAssocDegree(double assocDegree) {
		this.assocDegree = assocDegree;
	}
	public HoData(double timesTotal) {
		super();
		this.timesTotal = timesTotal;
	}
	public void addData(double timesTotal){
		this.timesTotal += timesTotal;
	}
}
