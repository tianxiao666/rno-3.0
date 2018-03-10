package com.iscreate.op.service.rno.mapreduce.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iscreate.op.service.rno.mapreduce.model.Ncell;

/**
 * 小区对象
 * 
 * @author scott
 */
public class Cell {
	private String id;
	private int pci;
	private double cellBcch;
	private double lon;
	private double lat;
	private double timesTotals;
	private double totalAssocDegree;
	private Map<String, Double> meaTimeToMixingSum;// 小区标识到邻区集合，一对多的关系

	private List<String> sameStationOtherCells;
	private List<String> notSameStationCells;
	private Map<String, Double> nssCellsAssocDegree;
	private List<Ncell> ncells;
	private List<Ncell> mrNcells;
	private List<Ncell> hoNcells;

	public Cell(String id) {
		this.id = id;
		this.pci = -1;
		this.cellBcch = 0;
		this.lon = 1000.0;
		this.lat = 1000.0;
		this.timesTotals = 0;
		this.totalAssocDegree = 0.0;
		this.meaTimeToMixingSum = new HashMap<String, Double>();

		this.sameStationOtherCells = new ArrayList<String>();
		this.notSameStationCells = new ArrayList<String>();
		this.nssCellsAssocDegree = new HashMap<String, Double>();
		this.ncells = new ArrayList<Ncell>();
		this.mrNcells = new ArrayList<Ncell>();
		this.hoNcells = new ArrayList<Ncell>();
	}

	public void putMeaTimeToMixingSum(String meaTime, double mixingSum) {
		if (!meaTimeToMixingSum.containsKey(meaTime)) {
			meaTimeToMixingSum.put(meaTime, mixingSum);
		}else if (meaTimeToMixingSum.get(meaTime)<mixingSum) {
			meaTimeToMixingSum.put(meaTime, mixingSum);
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

	public double getCellBcch() {
		return cellBcch;
	}

	public void setCellBcch(double cellBcch) {
		this.cellBcch = cellBcch;
	}

	public double getTimesTotals() {
		return timesTotals;
	}

	public void setTimesTotals(double timesTotals) {
		this.timesTotals = timesTotals;
	}

	public double getTotalAssocDegree() {
		return totalAssocDegree;
	}

	public void setTotalAssocDegree(double totalAssocDegree) {
		this.totalAssocDegree = totalAssocDegree;
	}

	public List<String> getSameStationOtherCells() {
		return sameStationOtherCells;
	}

	public void setSameStationOtherCells(List<String> sameStationOtherCells) {
		this.sameStationOtherCells = sameStationOtherCells;
	}

	public List<String> getNotSameStationCells() {
		return notSameStationCells;
	}

	public void setNotSameStationCells(List<String> notSameStationCells) {
		this.notSameStationCells = notSameStationCells;
	}

	public Map<String, Double> getNssCellsAssocDegree() {
		return nssCellsAssocDegree;
	}

	public void setNssCellsAssocDegree(Map<String, Double> nssCellsAssocDegree) {
		this.nssCellsAssocDegree = nssCellsAssocDegree;
	}

	public List<Ncell> getNcells() {
		return ncells;
	}

	public void setNcells(List<Ncell> ncells) {
		this.ncells = ncells;
	}

	public List<Ncell> getMrNcells() {
		return mrNcells;
	}

	public void setMrNcells(List<Ncell> mrNcells) {
		this.mrNcells = mrNcells;
	}

	public List<Ncell> getHoNcells() {
		return hoNcells;
	}

	public void setHoNcells(List<Ncell> hoNcells) {
		this.hoNcells = hoNcells;
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
		if (!(obj instanceof Cell)) {
			return false;
		}
		Cell other = (Cell) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Cell [id=" + id + ", pci=" + pci + ", cellBcch=" + cellBcch + ", lon=" + lon + ", lat=" + lat
				+ ", totalAssocDegree=" + totalAssocDegree + ", sameStationOtherCells=" + sameStationOtherCells
				+ ", notSameStationCells=" + notSameStationCells + "]";
	}
	
}
