package com.iscreate.op.service.rno.mapreduce.model;

/**
 * 邻区对象
 * 
 * @author scott
 */
public class Ncell {
	private String id; // 邻区标识
	private double cellBcch;// 邻区频点
	private int pci; // 邻区PCI
	private double lon;
	private double lat;
	private double timesTotal; // 测量次数
	private double numerator; // 关联度分子
	private double assocDegree;// 关联度
	private double dis;

	public Ncell(String id) {
		this.id = id;
		this.cellBcch = 0.0;
		this.pci = -1;
		this.lon = 1000.0;
		this.lat = 1000.0;
		this.timesTotal = 0;
		this.numerator = 0;
		this.assocDegree = 0.0;
		this.dis = 0.0;
	}

	public Ncell(String id, double cellBcch, int pci, double lon, double lat, double timesTotal, double numerator,
			double dis) {
		super();
		this.id = id;
		this.cellBcch = cellBcch;
		this.pci = pci;
		this.lon = lon;
		this.lat = lat;
		this.timesTotal = timesTotal;
		this.numerator = numerator;
		this.dis = 0.0;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getCellBcch() {
		return cellBcch;
	}

	public void setCellBcch(double cellBcch) {
		this.cellBcch = cellBcch;
	}

	public int getPci() {
		return pci;
	}

	public void setPci(int pci) {
		this.pci = pci;
	}

	public double getLon() {
		return lon;
	}

	public double getLat() {
		return lat;
	}

	public void setLon(double lon) {
		this.lon = lon;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public double getTimesTotal() {
		return timesTotal;
	}

	public void setTimesTotal(double timesTotal) {
		this.timesTotal = timesTotal;
	}

	public double getNumerator() {
		return numerator;
	}

	public void setNumerator(double numerator) {
		this.numerator = numerator;
	}

	public double getAssocDegree() {
		return assocDegree;
	}

	public void setAssocDegree(double assocDegree) {
		this.assocDegree = assocDegree;
	}

	public double getDis() {
		return dis;
	}

	public void setDis(double dis) {
		this.dis = dis;
	}

	@Override
	public String toString() {
		return "Ncell [id=" + id + ", cellBcch=" + cellBcch + ", pci=" + pci + ", lon=" + lon + ", lat=" + lat
				+ ", timesTotal=" + timesTotal + ", numerator=" + numerator + ", assocDegree=" + assocDegree + ", dis="
				+ dis + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Ncell)) {
			return false;
		}
		Ncell other = (Ncell) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}
}