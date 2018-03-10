package com.iscreate.op.pojo.system;

import java.util.Date;

/**
 * SysRoleRelaPermission entity. @author MyEclipse Persistence Tools
 */

public class SysRoleRelaPermission implements java.io.Serializable {

	// Fields

	/**
	 * 
	 */
	private static final long serialVersionUID = -2661129031255830750L;
	private Long role_id;
	private Long permission_id;
	private String status;

	private Date create_time;
	private Date mod_time;

	// Constructors

	/** default constructor */
	public SysRoleRelaPermission() {
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((permission_id == null) ? 0 : permission_id.hashCode());
		result = prime * result + ((role_id == null) ? 0 : role_id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SysRoleRelaPermission other = (SysRoleRelaPermission) obj;
		if (permission_id == null) {
			if (other.permission_id != null)
				return false;
		} else if (!permission_id.equals(other.permission_id))
			return false;
		if (role_id == null) {
			if (other.role_id != null)
				return false;
		} else if (!role_id.equals(other.role_id))
			return false;
		return true;
	}

	public Long getRole_id() {
		return role_id;
	}

	public void setRole_id(Long role_id) {
		this.role_id = role_id;
	}

	public Long getPermission_id() {
		return permission_id;
	}

	public void setPermission_id(Long permission_id) {
		this.permission_id = permission_id;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getCreate_time() {
		return create_time;
	}

	public void setCreate_time(Date create_time) {
		this.create_time = create_time;
	}

	public Date getMod_time() {
		return mod_time;
	}

	public void setMod_time(Date mod_time) {
		this.mod_time = mod_time;
	}

	

	// Property accessors

	

}