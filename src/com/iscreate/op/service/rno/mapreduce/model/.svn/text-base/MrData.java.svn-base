package com.iscreate.op.service.rno.mapreduce.model;

/**用来保存mr数据**/
public class MrData {
	private double timesTotal; // 测量次数
	private double times0; // RSRPtimes0
	private double times1; // RSRPtimes1
	private double assocDegree = 0.0;// 关联度
	public double getTimesTotal() {
		return timesTotal;
	}
	public void setTimesTotal(double timesTotal) {
		this.timesTotal = timesTotal;
	}
	
	public double getTimes0() {
		return times0;
	}
	public void setTimes0(double times0) {
		this.times0 = times0;
	}
	public double getTimes1() {
		return times1;
	}
	public void setTimes1(double times1) {
		this.times1 = times1;
	}
	public double getAssocDegree() {
		return assocDegree;
	}
	public void setAssocDegree(double assocDegree) {
		this.assocDegree = assocDegree;
	}
	public MrData(double timesTotal, double rSRPtimes0, double rSRPtimes1) {
		super();
		this.timesTotal = timesTotal;
		this.times0 = rSRPtimes0;
		this.times1 = rSRPtimes1;
	}
	public void addData(double timesTotal, double rSRPtimes0, double rSRPtimes1){
		this.timesTotal += timesTotal;
		this.times0 += rSRPtimes0;
		this.times1 += rSRPtimes1;
	}
	@Override
	public String toString() {
		return "MrData [timesTotal=" + timesTotal + ", times0=" + times0 + ", times1=" + times1 + ", assocDegree="
				+ assocDegree + "]";
	};
}
