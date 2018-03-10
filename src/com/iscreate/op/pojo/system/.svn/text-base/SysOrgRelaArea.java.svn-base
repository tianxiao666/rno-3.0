package com.iscreate.op.pojo.system;

/**
 * SysOrgRelaArea entity. @author MyEclipse Persistence Tools
 */

public class SysOrgRelaArea implements java.io.Serializable {

	// Fields

	/**
	 * 
	 */
	private static final long serialVersionUID = -287998116242710012L;
	private Long orgId;
	private Long areaId;
	private String description;

	// Constructors

	/** default constructor */
	public SysOrgRelaArea() {
	}

	

	// Property accessors

	

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}



	public Long getOrgId() {
		return orgId;
	}



	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}



	public Long getAreaId() {
		return areaId;
	}



	public void setAreaId(Long areaId) {
		this.areaId = areaId;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((areaId == null) ? 0 : areaId.hashCode());
		result = prime * result + ((orgId == null) ? 0 : orgId.hashCode());
		return result;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SysOrgRelaArea)) {
			return false;
		}
		SysOrgRelaArea other = (SysOrgRelaArea) obj;
		if (areaId == null) {
			if (other.areaId != null) {
				return false;
			}
		} else if (!areaId.equals(other.areaId)) {
			return false;
		}
		if (orgId == null) {
			if (other.orgId != null) {
				return false;
			}
		} else if (!orgId.equals(other.orgId)) {
			return false;
		}
		return true;
	}

}