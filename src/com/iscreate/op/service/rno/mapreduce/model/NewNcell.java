package com.iscreate.op.service.rno.mapreduce.model;

/**
 * 邻区对象
 * 
 * @author scott
 */
public class NewNcell {
	private int id; // 邻区标识
	private int cellBcch;// 邻区频点
	private int pci; // 邻区PCI
	private MrData mrData;// mr数据
	private RsData rsData;// rs数据
	private HoData hoData;// mr数据
	private double assocDegree = 0.0;// 关联度
	private double mrAssocDegree = 0.0;// 关联度
	private double hoAssocDegree = 0.0;// 关联度
	private double sfAssocDegree = 0.0;// 关联度

	public NewNcell(int id) {
		this.id = id;
		this.cellBcch = -1;
		this.pci = -1;
	}

	public NewNcell(int id, int cellBcch, int pci) {
		super();
		this.id = id;
		this.cellBcch = cellBcch;
		this.pci = pci;
	}

	public void add(NewNcell other) {
		if (this.equals(other)) {
			if (other.hasMrData()) {
				addMrData(other.getMrData());
			}
			if (other.hasHoData()) {
				addHoData(other.getHoData());
			}
			if (other.hasRsData()) {
				addRsData(other.getRsData());
			}
		}
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCellBcch() {
		return cellBcch;
	}

	public void setCellBcch(int cellBcch) {
		this.cellBcch = cellBcch;
	}

	public int getPci() {
		return pci;
	}

	public void setPci(int pci) {
		this.pci = pci;
	}

	public MrData getMrData() {
		return mrData;
	}

	public void setMrData(MrData mrData) {
		this.mrData = mrData;
	}

	public RsData getRsData() {
		return rsData;
	}

	public void setRsData(RsData rsData) {
		this.rsData = rsData;
	}

	public HoData getHoData() {
		return hoData;
	}

	public void setHoData(HoData hoData) {
		this.hoData = hoData;
	}

	public double getAssocDegree() {
		return assocDegree;
	}

	public void setAssocDegree(double assocDegree) {
		this.assocDegree = assocDegree;
	}

	public double getMrAssocDegree() {
		return mrAssocDegree;
	}

	public void setMrAssocDegree(double mrAssocDegree) {
		this.mrAssocDegree = mrAssocDegree;
	}

	public double getHoAssocDegree() {
		return hoAssocDegree;
	}

	public void setHoAssocDegree(double hoAssocDegree) {
		this.hoAssocDegree = hoAssocDegree;
	}

	public double getSfAssocDegree() {
		return sfAssocDegree;
	}

	public void setSfAssocDegree(double sfAssocDegree) {
		this.sfAssocDegree = sfAssocDegree;
	}

	public boolean hasMrData() {
		if (mrData != null) {
			return true;
		}
		return false;
	}

	public boolean hasRsData() {
		if (rsData != null) {
			return true;
		}
		return false;
	}

	public boolean hasHoData() {
		if (hoData != null) {
			return true;
		}
		return false;
	}

	public void addMrData(MrData mrData) {
		addMrData(mrData.getTimesTotal(), mrData.getTimes0(), mrData.getTimes1());
	}

	public void addMrData(double timesTotal, double rSRPtimes0, double rSRPtimes1) {
		if (hasMrData()) {
			mrData.addData(timesTotal, rSRPtimes0, rSRPtimes1);
		} else {
			mrData = new MrData(timesTotal, rSRPtimes0, rSRPtimes1);
		}
	}

	public void addRsData(RsData rsData) {
		addRsData(rsData.getTimesTotal(), rsData.getTimes0(), rsData.getTimes1());
	}

	public void addRsData(double timesTotal, double rSRPtimes0, double rSRPtimes1) {
		if (hasRsData()) {
			rsData.addData(timesTotal, rSRPtimes0, rSRPtimes1);
		} else {
			rsData = new RsData(timesTotal, rSRPtimes0, rSRPtimes1);
		}
	}

	public void addHoData(HoData hoData) {
		addHoData(hoData.getTimesTotal());
	}

	public void addHoData(double timesTotal) {
		if (hasHoData()) {
			hoData.addData(timesTotal);
		} else {
			hoData = new HoData(timesTotal);
		}
	}

	public void calcMrAssocDegree(double totalMixingSum, double RSRP1weight, double RSRP0Minus1weight) {
		if (totalMixingSum != 0 && hasMrData()) {
			mrAssocDegree = mrData.getTimes1() / totalMixingSum * RSRP1weight
					+ (mrData.getTimes0() - mrData.getTimes1()) / totalMixingSum * RSRP0Minus1weight;
		} else {
			mrAssocDegree = 0;
		}
	}

	public void calcRsAssocDegree(double totalMixingSum, double RSRP1weight, double RSRP0Minus1weight) {
		if (totalMixingSum != 0 && hasRsData()) {
			sfAssocDegree = rsData.getTimes1() / totalMixingSum * RSRP1weight
					+ (rsData.getTimes0() - rsData.getTimes1()) / totalMixingSum * RSRP0Minus1weight;
		} else {
			sfAssocDegree = 0;
		}
	}

	public void calcHoAssocDegree(double timesTotal) {
		if (timesTotal != 0 && hasHoData()) {
			hoAssocDegree = hoData.getTimesTotal() / timesTotal;
		} else {
			hoAssocDegree = 0;
		}
	}

	private double calcMrOrRsAssocDegree() {
		return mrAssocDegree > sfAssocDegree ? mrAssocDegree : sfAssocDegree;
	}

	/**
	 * 计算该邻区关联度
	 * 
	 * @param samefreqcellcoefweight
	 * @param switchratioweight
	 * @author chen.c10
	 * @date 2016年3月25日
	 * @version RNO 3.0.1
	 */
	public void calcAssocDegree(double samefreqcellcoefweight, double switchratioweight) {
		calcAssocDegree(samefreqcellcoefweight, switchratioweight, calcMrOrRsAssocDegree());
	}

	private void calcAssocDegree(double samefreqcellcoefweight, double switchratioweight, double tmpMrOrRsAssocDegree) {
		// 两种数据都存在
		if (tmpMrOrRsAssocDegree > 0.0 && hasHoData()) {
			assocDegree = tmpMrOrRsAssocDegree * samefreqcellcoefweight + hoAssocDegree * switchratioweight;
		} // ho数据不存在
		else if (tmpMrOrRsAssocDegree > 0.0 && !hasHoData()) {
			assocDegree = tmpMrOrRsAssocDegree;
		} // mr数据不存在
		else if (tmpMrOrRsAssocDegree == 0.0 && hasHoData()) {
			assocDegree = hoAssocDegree * switchratioweight;
		}
	}

	public void clearMidData() {
		if (hasMrData()) {
			mrData = null;
		}
		if (hasHoData()) {
			hoData = null;
		}
		if (hasRsData()) {
			rsData = null;
		}
	}

	@Override
	public String toString() {
		return "NewNcell [id=" + id + ", cellBcch=" + cellBcch + ", pci=" + pci + ", assocDegree=" + assocDegree
				+ ", mrAssocDegree=" + mrAssocDegree + ", hoAssocDegree=" + hoAssocDegree + ", sfAssocDegree="
				+ sfAssocDegree + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
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
		if (!(obj instanceof NewNcell)) {
			return false;
		}
		NewNcell other = (NewNcell) obj;
		if (id != other.id) {
			return false;
		}
		return true;
	}
}