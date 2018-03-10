package com.iscreate.op.service.rno.mapreduce.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小区对象
 * @author chen.c10
 *
 */
public class NewCell{

	// 最终输出前几种数据
	private String id;
	private int pci;
	private int cellBcch;
	private double totalAssocDegree;
	private Map<String, Double> cellsAssocDegree;

	// 用于暂时保留
	private Map<String, Double> meaTimeToMixingSum;
	private List<NewNcell> ncells;

	public NewCell(String id) {
		this.id = id;
		this.pci = -1;
		this.cellBcch = 0;
		this.totalAssocDegree = 0.0;
		this.cellsAssocDegree = new HashMap<String, Double>();

		this.meaTimeToMixingSum = new HashMap<String, Double>();
		this.ncells = new ArrayList<NewNcell>();
	}

	public void addNcell(NewNcell ncell) {
		if (!ncells.contains(ncell)) {
			ncells.add(ncell);
		} else {
			for (NewNcell oldNcell : ncells) {
				if (oldNcell.equals(ncell)) {
					oldNcell.add(ncell);
				}
			}
		}
	}

	public void putMeaTimeToMixingSum(String meaTime, double mixingSum) {
		if (!meaTimeToMixingSum.containsKey(meaTime)) {
			meaTimeToMixingSum.put(meaTime.intern(), mixingSum);
		} else if (meaTimeToMixingSum.get(meaTime) < mixingSum) {
			meaTimeToMixingSum.put(meaTime.intern(), mixingSum);
		}
	}

	public double getMixingSum() {
		double mixingSum = 0.0;
		for (double d : meaTimeToMixingSum.values()) {
			mixingSum += d;
		}
		return mixingSum;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getPci() {
		return pci;
	}

	public void setPci(int pci) {
		this.pci = pci;
	}

	public int getCellBcch() {
		return cellBcch;
	}

	public void setCellBcch(int cellBcch) {
		this.cellBcch = cellBcch;
	}

	public double getTotalAssocDegree() {
		return totalAssocDegree;
	}

	public void setTotalAssocDegree(double totalAssocDegree) {
		this.totalAssocDegree = totalAssocDegree;
	}

	public Map<String, Double> getCellsAssocDegree() {
		return cellsAssocDegree;
	}

	public void setCellsAssocDegree(Map<String, Double> cellsAssocDegree) {
		this.cellsAssocDegree = cellsAssocDegree;
	}

	public List<NewNcell> getNcells() {
		return ncells;
	}

	public void setNcells(List<NewNcell> ncells) {
		this.ncells = ncells;
	}

	@Override
	public String toString() {
		return "NewCell [id=" + id + ", pci=" + pci + ", cellBcch=" + cellBcch + ", totalAssocDegree="
				+ totalAssocDegree + ", cellsAssocDegree=" + cellsAssocDegree + ", meaTimeToMixingSum="
				+ meaTimeToMixingSum + ", ncells=" + ncells + "]";
	}

	private boolean calcMrAssocDegree(double RSRP1weight, double RSRP0Minus1weight, double minmeasuresum) {

		// 混频 新分母
		double totalMixingSum = getMixingSum();
		// 总的测量次数
		double sumTimesTotals = 0;

		// 计算最小测量总数
		for (NewNcell ncell : ncells) {
			if (ncell.hasMrData()) {
				sumTimesTotals += ncell.getMrData().getTimesTotal();
			}
		}
		// 过滤测量总数小于最小测量总数的
		if (sumTimesTotals < minmeasuresum) {
			// System.out.println("Filter: cellId = " + cellId + ", sumTimesTotals=" + sumTimesTotals);
			// mr关联度全部置0
			for (NewNcell ncell : ncells) {
				ncell.setMrAssocDegree(0);
			}
		} else {
			// 计算mr关联度
			for (NewNcell ncell : ncells) {
				ncell.calcMrAssocDegree(totalMixingSum, RSRP1weight, RSRP0Minus1weight);
			}
		}
		return true;
	}

	private boolean calcRsAssocDegree(double RSRP1weight, double RSRP0Minus1weight) {

		// 总的测量次数
		double sumTimesTotals = 0;

		// 计算关联度
		for (NewNcell ncell : ncells) {
			if (ncell.hasRsData()) {
				sumTimesTotals += ncell.getRsData().getTimesTotal();
			}
		}
		for (NewNcell ncell : ncells) {
			ncell.calcRsAssocDegree(sumTimesTotals, RSRP1weight, RSRP0Minus1weight);
		}
		return true;
	}

	private boolean calcHoAssocDegree() {
		double timesTotal = 0;

		for (NewNcell ncell : ncells) {
			if (ncell.hasHoData()) {
				timesTotal += ncell.getHoData().getTimesTotal();
			}
		}
		for (NewNcell ncell : ncells) {
			ncell.calcHoAssocDegree(timesTotal);
		}
		return true;
	}

	public boolean calcAssocDegree(double RSRP1weight, double RSRP0Minus1weight, double minmeasuresum,
			double samefreqcellcoefweight, double switchratioweight) {
		calcMrAssocDegree(RSRP1weight, RSRP0Minus1weight, minmeasuresum);
		calcRsAssocDegree(RSRP1weight, RSRP0Minus1weight);
		calcHoAssocDegree();
		for (NewNcell ncell : ncells) {
			ncell.calcAssocDegree(samefreqcellcoefweight, switchratioweight);
		}
		return true;
	}

	public boolean calcTotalAssocDegree(double mincorrelation, Map<String, Integer> cellToOriPciFromData, Map<String, Integer> cellToEarfcnFromData) {
		List<NewNcell> willFilteredNcell = new ArrayList<NewNcell>();

		for (NewNcell ncell : ncells) {
			String ncellId = String.valueOf(ncell.getId());
			// 如果列表中不存在，则更新MR中工参
			if (!cellToOriPciFromData.containsKey(ncellId)) {
				cellToOriPciFromData.put(ncellId.intern(), ncell.getPci());
			}
			// 前面已经过滤过，所有邻区都是主小区的同频小区，直接用主小区频点
			if (!cellToEarfcnFromData.containsKey(ncellId)) {
				cellToEarfcnFromData.put(ncellId.intern(), cellBcch);
			}
			//过滤关联度小于最小关联度的邻区
			double assocDegree = ncell.getAssocDegree();
			//ncell.clearMidData();
			if (assocDegree < mincorrelation) {
				// 暂存关联度小于“最小关联度”的邻区
				willFilteredNcell.add(ncell);
			} else {
				// 总关联度不累加小于“最小关联度”的值
				totalAssocDegree += assocDegree;
			}
		}

		// 过滤关联度小于“最小关联度”的邻区
		for (NewNcell ncell : willFilteredNcell) {
			System.out.println("Filter: cellId = " + this.id + ", ncell = " + ncell);
			ncells.remove(ncell);
		}
		for (NewNcell ncell : ncells) {
			cellsAssocDegree.put(String.valueOf(ncell.getId()).intern(), ncell.getAssocDegree());
		}
		return true;
	}

	public void clear() {
		this.meaTimeToMixingSum = null;
		this.ncells = null;
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
		if (!(obj instanceof NewCell)) {
			return false;
		}
		NewCell other = (NewCell) obj;
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
