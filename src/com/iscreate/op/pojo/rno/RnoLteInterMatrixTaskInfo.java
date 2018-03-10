/**
 * 
 */
package com.iscreate.op.pojo.rno;

/**
 * 干扰矩阵任务消息类
 * 
 * @author chen.c10
 *
 */
public class RnoLteInterMatrixTaskInfo {
	private String account;
	private String taskName;
	private String taskDesc;
	private long cityId;
	private String cityName;
	private String begTime;
	private String endTime;
	private String dataType;
	private String dataDescription;
	private long recordNum;
	private double sameFreqCellCoefWeight;
	private double switchRatioWeight;
	private String jobType;
	private String sfFiles; // 扫频文件名串
	private String freqAdjType; // 频率调整方案类型
	private String d1Freq;// d1频点
	private String d2Freq;// d2频点

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskDesc() {
		return taskDesc;
	}

	public void setTaskDesc(String taskDesc) {
		this.taskDesc = taskDesc;
	}

	public long getCityId() {
		return cityId;
	}

	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public String getCityName() {
		return cityName;
	}

	public void setCityName(String cityName) {
		this.cityName = cityName;
	}

	public String getBegTime() {
		return begTime;
	}

	public void setBegTime(String begTime) {
		this.begTime = begTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getDataDescription() {
		return dataDescription;
	}

	public void setDataDescription(String dataDescription) {
		this.dataDescription = dataDescription;
	}

	public long getRecordNum() {
		return recordNum;
	}

	public void setRecordNum(long recordNum) {
		this.recordNum = recordNum;
	}

	public double getSameFreqCellCoefWeight() {
		return sameFreqCellCoefWeight;
	}

	public void setSameFreqCellCoefWeight(double sameFreqCellCoefWeight) {
		this.sameFreqCellCoefWeight = sameFreqCellCoefWeight;
	}

	public double getSwitchRatioWeight() {
		return switchRatioWeight;
	}

	public void setSwitchRatioWeight(double switchRatioWeight) {
		this.switchRatioWeight = switchRatioWeight;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getSfFiles() {
		return sfFiles;
	}

	public void setSfFiles(String sfFiles) {
		this.sfFiles = sfFiles;
	}

	public String getFreqAdjType() {
		return freqAdjType;
	}

	public void setFreqAdjType(String freqAdjType) {
		this.freqAdjType = freqAdjType;
	}

	public String getD1Freq() {
		return d1Freq;
	}

	public void setD1Freq(String d1Freq) {
		this.d1Freq = d1Freq;
	}

	public String getD2Freq() {
		return d2Freq;
	}

	public void setD2Freq(String d2Freq) {
		this.d2Freq = d2Freq;
	}
}
