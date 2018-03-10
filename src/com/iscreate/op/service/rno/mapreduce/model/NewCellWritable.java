package com.iscreate.op.service.rno.mapreduce.model;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.WritableComparable;

/**
 * 小区对象
 * @author chen.c10
 *
 */
public class NewCellWritable implements WritableComparable<NewCellWritable> {

	// 最终输出前几种数据
	private String id;
	private int pci;
	private int cellBcch;
	private double totalAssocDegree;
	private Map<String, Double> cellsAssocDegree;

	public NewCellWritable() {
		super();
	}

	public NewCellWritable(NewCell newCell) {
		set(newCell);
	}

	public void set(NewCell newCell) {
		this.id = newCell.getId();
		this.pci = newCell.getPci();
		this.cellBcch = newCell.getCellBcch();
		this.totalAssocDegree = newCell.getTotalAssocDegree();
		this.cellsAssocDegree = newCell.getCellsAssocDegree();
	}

	public NewCell get() {
		NewCell newCell = new NewCell(id);
		newCell.setPci(pci);
		newCell.setCellBcch(cellBcch);
		newCell.setTotalAssocDegree(totalAssocDegree);
		newCell.setCellsAssocDegree(cellsAssocDegree);
		return newCell;
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

	public double getCellBcch() {
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

	/* (non-Javadoc)
	 * @see org.apache.hadoop.io.Writable#readFields(java.io.DataInput)
	 */
	@Override
	public void readFields(DataInput in) throws IOException {
		this.id = in.readUTF();
		this.pci = in.readInt();
		this.cellBcch = in.readInt();
		this.totalAssocDegree = in.readDouble();
		int cellsAssocDegreeLen = in.readInt();
		for (int i = 0; i < cellsAssocDegreeLen; i++) {
			this.cellsAssocDegree.put(in.readUTF(), in.readDouble());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.io.Writable#write(java.io.DataOutput)
	 */
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(id);
		out.writeInt(pci);
		out.writeInt(cellBcch);
		out.writeDouble(totalAssocDegree);
		out.writeInt(cellsAssocDegree.size());
		for (String cell : cellsAssocDegree.keySet()) {
			out.writeUTF(cell);
			out.writeDouble(cellsAssocDegree.get(cell));
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(NewCellWritable o) {
		int oId = Integer.parseInt(o.getId());
		int myId = Integer.parseInt(id);
		return myId-oId;
	}

	@Override
	public String toString() {
		return "NewCellWritable [id=" + id + ", pci=" + pci + ", cellBcch=" + cellBcch + ", totalAssocDegree="
				+ totalAssocDegree + ", cellsAssocDegree=" + cellsAssocDegree + "]";
	}

	public void readString(String str) {
		if(!str.trim().startsWith("NewCellWritable")){
			return;
		}
		int begIdx = 0, endIdx = 0;
		begIdx = str.indexOf("[") + 1;
		endIdx = str.lastIndexOf("]");
		String body = str.substring(begIdx, endIdx);
		String flag = "id=";
		begIdx = body.indexOf(flag) + flag.length();
		endIdx = body.indexOf(",", begIdx);
		id = body.substring(begIdx, endIdx);
		flag = "pci=";
		begIdx = body.indexOf(flag) + flag.length();
		endIdx = body.indexOf(",", begIdx);
		pci = Integer.parseInt(body.substring(begIdx, endIdx));
		flag = "cellBcch=";
		begIdx = body.indexOf(flag) + flag.length();
		endIdx = body.indexOf(",", begIdx);
		cellBcch = Integer.parseInt(body.substring(begIdx, endIdx));
		flag = "totalAssocDegree=";
		begIdx = body.indexOf(flag) + flag.length();
		endIdx = body.indexOf(",", begIdx);
		totalAssocDegree = Double.parseDouble(body.substring(begIdx, endIdx));

		cellsAssocDegree = new HashMap<String, Double>();
		flag = "cellsAssocDegree={";
		begIdx = body.indexOf(flag) + flag.length();
		endIdx = body.indexOf("}", begIdx);
		for (String cell : body.substring(begIdx, endIdx).split(",")) {
			if (!cell.isEmpty()) {
				if (cell.split("=").length == 2) {
					cellsAssocDegree.put(cell.split("=")[0], Double.parseDouble(cell.split("=")[1]));
				}
			}
		}
	}

	public static NewCellWritable read(String str) {
		NewCellWritable cw = new NewCellWritable();
		cw.readString(str);
		return cw;
	}
	public static void main(String[] args) {
		String str = "NewCellWritable [id=100, pci=200, cellBcch=39.0, totalAssocDegree=1500.0, sameStationOtherCells=[], notSameStationCells=[56151,465464,46465,46455], cellsAssocDegree={}]";
/*		NewCell c = new NewCell("100");
		NewCellWritable cw = new NewCellWritable(c);
		System.out.println(cw);*/
		NewCellWritable cw = new NewCellWritable();
		cw.readString(str);
		System.out.println(cw);
		System.out.println(NewCellWritable.read(str));
	}
}
