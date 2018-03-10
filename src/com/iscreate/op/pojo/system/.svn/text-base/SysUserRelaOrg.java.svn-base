package com.iscreate.op.pojo.system;

import java.util.Date;

/**
 * SysUserRelaOrg entity. @author MyEclipse Persistence Tools
 */

public class SysUserRelaOrg implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6465678899303457493L;

	// Fields
	private Long orgId;
	private Long orgUserId;
	private String description;
	private Date createtime;
	private Date updatetime;
	private String status;

	// Constructors

	public String getStatus() {
		return status;
	}



	public void setStatus(String status) {
		this.status = status;
	}



	/** default constructor */
	public SysUserRelaOrg() {
	}

	

	// Property accessors

	

	

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getCreatetime() {
		return this.createtime;
	}

	public void setCreatetime(Date createtime) {
		this.createtime = createtime;
	}

	public Date getUpdatetime() {
		return this.updatetime;
	}

	public void setUpdatetime(Date updatetime) {
		this.updatetime = updatetime;
	}



	public Long getOrgId() {
		return orgId;
	}



	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}



	public Long getOrgUserId() {
		return orgUserId;
	}



	public void setOrgUserId(Long orgUserId) {
		this.orgUserId = orgUserId;
	}



	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orgId == null) ? 0 : orgId.hashCode());
		result = prime * result + ((orgUserId == null) ? 0 : orgUserId.hashCode());
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
		if (!(obj instanceof SysUserRelaOrg)) {
			return false;
		}
		SysUserRelaOrg other = (SysUserRelaOrg) obj;
		if (orgId == null) {
			if (other.orgId != null) {
				return false;
			}
		} else if (!orgId.equals(other.orgId)) {
			return false;
		}
		if (orgUserId == null) {
			if (other.orgUserId != null) {
				return false;
			}
		} else if (!orgUserId.equals(other.orgUserId)) {
			return false;
		}
		return true;
	}
}