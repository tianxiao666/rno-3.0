package com.iscreate.op.action.rno;

public class LteCellQueryCondition {
	private long provinceId = -1;
	private long cityId = -1;
	private String enodebName = null;
	private String cellName = null;
	private int pci = -1;

	public long getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(long provinceId) {
		this.provinceId = provinceId;
	}

	public long getCityId() {
		return cityId;
	}

	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public String getEnodebName() {
		return enodebName;
	}

	public void setEnodebName(String enodebName) {
		this.enodebName = enodebName;
	}

	public String getCellName() {
		return cellName;
	}

	public void setCellName(String cellName) {
		this.cellName = cellName;
	}

	public int getPci() {
		return pci;
	}

	public void setPci(int pci) {
		this.pci = pci;
	}

	/**
	 * 构建WHERE,enodebName的部分需要单独处理
	 * 
	 * @return
	 * @author brightming 2014-5-19 下午1:48:33
	 */
	public String buildCellQueryCond(String prx) {
		String where = "";

		if (pci != -1) {
			where += prx + ".PCI=" + pci;
		}

		// ----默认加上status-------//
		where += (where.length() == 0 ? " " : " AND ") + " STATUS='N' ";

		if (cellName != null) {
			cellName = cellName.trim();
			if (!"".equals(cellName)) {
				where += (where.length() == 0 ? " " : " and ") + prx + ".CELL_NAME like '%" + cellName + "%'";
			}
		}

		if (cityId != -1) {
			where += (where.length() == 0 ? " " : " and ") + prx + ".AREA_ID=" + cityId;
		} else {
			if (provinceId != -1) {
				where += (where.length() == 0 ? " " : " and ") + prx
						+ ".AREA_ID in ( SELECT AREA_ID FROM SYS_AREA WHERE PARENT_ID=" + provinceId + ")";
			}
		}

		if (enodebName != null) {
			enodebName = enodebName.trim();
			if (!"".equals(enodebName)) {
				where += (where.length() == 0 ? " " : " and ") + prx
						+ ".ENODEB_ID IN (SELECT ENODEB_ID FROM RNO_LTE_ENODEB WHERE ENODEB_NAME LIKE '%" + enodebName
						+ "%')";
			}
		}
		return where;
	}

	@Override
	public String toString() {
		return "LteCellQueryCondition [provinceId=" + provinceId + ", cityId=" + cityId + ", enodebName=" + enodebName
				+ ", cellName=" + cellName + ", pci=" + pci + "]";
	}
}
