package com.iscreate.op.pojo.system;

import java.io.Serializable;
import java.util.Date;

public class SysUserRelaRole  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long orgUserId;
	private Long roleId;
	private String description;
	private Date createtime;
	private Date updatetime;
	private String status;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Long getOrgUserId() {
		return orgUserId;
	}
	public void setOrgUserId(Long orgUserId) {
		this.orgUserId = orgUserId;
	}
	public Long getRoleId() {
		return roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public Date getCreatetime() {
		return createtime;
	}
	public void setCreatetime(Date createtime) {
		this.createtime = createtime;
	}
	public Date getUpdatetime() {
		return updatetime;
	}
	public void setUpdatetime(Date updatetime) {
		this.updatetime = updatetime;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((orgUserId == null) ? 0 : orgUserId.hashCode());
		result = prime * result + ((roleId == null) ? 0 : roleId.hashCode());
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
		if (!(obj instanceof SysUserRelaRole)) {
			return false;
		}
		SysUserRelaRole other = (SysUserRelaRole) obj;
		if (orgUserId == null) {
			if (other.orgUserId != null) {
				return false;
			}
		} else if (!orgUserId.equals(other.orgUserId)) {
			return false;
		}
		if (roleId == null) {
			if (other.roleId != null) {
				return false;
			}
		} else if (!roleId.equals(other.roleId)) {
			return false;
		}
		return true;
	}
	
	

}
