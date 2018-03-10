package com.iscreate.op.action.rno.model;

public class MrJobCond {
	long jobId;
	String mrJobId;
	String account;
	String runType;
	/**
	 * @return the jobId
	 */
	public long getJobId() {
		return jobId;
	}
	/**
	 * @param jobId the jobId to set
	 */
	public void setJobId(long jobId) {
		this.jobId = jobId;
	}
	/**
	 * @return the mrJobId
	 */
	public String getMrJobId() {
		return mrJobId;
	}
	/**
	 * @param mrJobId the mrJobId to set
	 */
	public void setMrJobId(String mrJobId) {
		this.mrJobId = mrJobId;
	}
	/**
	 * @return the account
	 */
	public String getAccount() {
		return account;
	}
	/**
	 * @param account the account to set
	 */
	public void setAccount(String account) {
		this.account = account;
	}
	/**
	 * @return the runType
	 */
	public String getRunType() {
		return runType;
	}
	/**
	 * @param runType the runType to set
	 */
	public void setRunType(String runType) {
		this.runType = runType;
	}
	public MrJobCond(long jobId, String mrJobId, String account, String runType) {
		super();
		this.jobId = jobId;
		this.mrJobId = mrJobId;
		this.account = account;
		this.runType = runType;
	}
}
