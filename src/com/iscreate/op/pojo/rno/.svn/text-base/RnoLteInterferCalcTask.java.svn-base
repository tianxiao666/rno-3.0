package com.iscreate.op.pojo.rno;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.vo.Threshold;

public class RnoLteInterferCalcTask {
	
	private TaskInfo taskInfo=null;
	private Threshold thresholdDefault=null;//默认参数值
	private Threshold threshold=null;
	private HoInfo hoInfo=null;
	private MrInfo mrInfo=null;

	private List<Map<String,Object>> eriInfo;
	private List<Map<String,Object>> zteInfo;
	private List<RnoThreshold> rnoThresholds;
//	private List<List<RnoThreshold>> groupThresholds;
	private Map<Long,List<RnoThreshold>> groupThresholds;
	private String flowInfo;
	private String jobuuidInfo;
	private List<Map<String, String>> sfFileInfo = null;
	
	public List<Map<String, String>> getSfFileInfo() {
		return sfFileInfo;
	}
	public void setSfFileInfo(List<Map<String, String>> sfFileInfo) {
		this.sfFileInfo = sfFileInfo;
	}
	public Map<Long, List<RnoThreshold>> getGroupThresholds() {
		return groupThresholds;
	}
	public void setGroupThresholds(Map<Long, List<RnoThreshold>> groupThresholds) {
		this.groupThresholds = groupThresholds;
	}
	public List<RnoThreshold> getRnoThresholds() {
		return rnoThresholds;
	}
	public void setRnoThresholds(List<RnoThreshold> rnoThresholds) {
		this.rnoThresholds = rnoThresholds;
	}
	public List<Map<String, Object>> getEriInfo() {
		if(eriInfo==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(eriInfo==null){
					eriInfo=new ArrayList<Map<String,Object>>();
				}
			}
		}
		//如果不为null，直接返回instance
		return eriInfo;
	}
	public void setEriInfo(List<Map<String, Object>> eriInfo) {
		this.eriInfo = eriInfo;
	}
	public void setZteInfo(List<Map<String, Object>> zteInfo) {
		this.zteInfo = zteInfo;
	}
	public List<Map<String, Object>> getZteInfo() {
		if(zteInfo==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(zteInfo==null){
					zteInfo=new ArrayList<Map<String,Object>>();
				}
			}
		}
		//如果不为null，直接返回instance
		return zteInfo;
	}
	public Threshold getThresholdDefault() {
		if(thresholdDefault==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(thresholdDefault==null){
					thresholdDefault=new Threshold();
				}
			}
		}
		//如果不为null，直接返回instance
		return thresholdDefault;
	}
	public void setThresholdDefault(Threshold thresholdDefault) {
		this.thresholdDefault = thresholdDefault;
	}
	public Threshold getThreshold() {
		if(threshold==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(threshold==null){
					threshold=new Threshold();
				}
			}
		}
		//如果不为null，直接返回instance
		return threshold;
	}
	public void setThreshold(Threshold threshold) {
		this.threshold = threshold;
	}
	public TaskInfo getTaskInfo() {
		if(taskInfo==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(taskInfo==null){
					taskInfo=new TaskInfo();
				}
			}
		}
		//如果不为null，直接返回instance
		return taskInfo;
	}
	public void setTaskInfo(TaskInfo taskInfo) {
		this.taskInfo = taskInfo;
	}
	public void setHoInfo(HoInfo hoInfo) {
		this.hoInfo = hoInfo;
	}
	public HoInfo getHoInfo() {
			if (hoInfo==null) {
				//如果为null，加锁初始化
				synchronized (this) {
					if (hoInfo==null) {
						hoInfo=new HoInfo();
					}
				}
			}
			//如果不为null，直接返回instance
			return hoInfo;
		
	}
	public void setMrInfo(MrInfo mrInfo) {
		this.mrInfo = mrInfo;
	}
	public MrInfo getMrInfo() {
		if (mrInfo==null) {
			//如果为null，加锁初始化
			synchronized (this) {
				if (mrInfo==null) {
					mrInfo=new MrInfo();
				}
			}
		}
		//如果不为null，直接返回instance
		return mrInfo;
	}
	public String getFlowInfo() {
		if(flowInfo==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(flowInfo==null){
					flowInfo=new String();
				}
			}
		}
		//如果不为null，直接返回instance
		return flowInfo;
	}
	public void setFlowInfo(String flowInfo) {
		this.flowInfo = flowInfo;
	}
	public String getjobuuidInfo() {
		if(jobuuidInfo==null){
			//如果为null，加锁初始化
			synchronized(this){
				if(jobuuidInfo==null){
					jobuuidInfo=new String();
				}
			}
		}
		//如果不为null，直接返回instance
		return jobuuidInfo;
	}
	public void setjobuuidInfo(String jobuuidInfo) {
		this.jobuuidInfo = jobuuidInfo;
	}
	
	/**
	 * 任务消息内部类
	 * @author chao.xj
	 *
	 */
	public static class TaskInfo {
		private String taskName;
		private String taskDesc;
		private long provinceId;
		private long cityId;
		private String cityName;
		private String provinceName;
		private String startTime;
		private String endTime;
		private String planType;
		private String converType;//收敛方式类型
		private String relaNumerType;//关联度分子
		private String lteCells;
		private String isCheckNCell;
		private String isExportAssoTable;
		private String isExportMidPlan;
		private String isExportNcCheckPlan;
		private boolean isUseFlow;
		private double ks;
		private String sfFiles; //扫频文件名串
		private String freqAdjType; //频率调整方案类型
		private String d1Freq;//d1频点
		private String d2Freq;//d2频点
		private String sourceType;
		private long matrixDataCollectId; //干扰矩阵文件ID
		private long flowDataCollectId; //流量文件ID
		private TaskInfo(){
			
		}
		public long getFlowDataCollectId() {
			return flowDataCollectId;
		}
		public void setFlowDataCollectId(long flowDataCollectId) {
			this.flowDataCollectId = flowDataCollectId;
		}
		public long getMatrixDataCollectId() {
			return matrixDataCollectId;
		}
		public void setMatrixDataCollectId(long matrixDataCollectId) {
			this.matrixDataCollectId = matrixDataCollectId;
		}
		public String getSourceType() {
			return sourceType;
		}
		public void setSourceType(String sourceType) {
			this.sourceType = sourceType;
		}
		public boolean isUseFlow() {
			return isUseFlow;
		}
		public void setUseFlow(boolean isUseFlow) {
			this.isUseFlow = isUseFlow;
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
		public String getFreqAdjType() {
			return freqAdjType;
		}
		public void setFreqAdjType(String freqAdjType) {
			this.freqAdjType = freqAdjType;
		}
		public String getSfFiles() {
			return sfFiles;
		}


		public void setSfFiles(String sfFiles) {
			this.sfFiles = sfFiles;
		}


		public double getKs() {
			return ks;
		}


		public void setKs(double ks) {
			this.ks = ks;
		}


		public String getIsExportAssoTable() {
			return isExportAssoTable;
		}


		public void setIsExportAssoTable(String isExportAssoTable) {
			this.isExportAssoTable = isExportAssoTable;
		}


		public String getIsExportMidPlan() {
			return isExportMidPlan;
		}


		public void setIsExportMidPlan(String isExportMidPlan) {
			this.isExportMidPlan = isExportMidPlan;
		}


		public String getIsExportNcCheckPlan() {
			return isExportNcCheckPlan;
		}


		public void setIsExportNcCheckPlan(String isExportNcCheckPlan) {
			this.isExportNcCheckPlan = isExportNcCheckPlan;
		}


		public String getIsCheckNCell() {
			return isCheckNCell;
		}


		public void setIsCheckNCell(String isCheckNCell) {
			this.isCheckNCell = isCheckNCell;
		}


		public String getLteCells() {
			return lteCells;
		}


		public void setLteCells(String lteCells) {
			this.lteCells = lteCells;
		}


		public String getCityName() {
			return cityName;
		}

		public void setCityName(String cityName) {
			this.cityName = cityName;
		}

		public String getProvinceName() {
			return provinceName;
		}

		public void setProvinceName(String provinceName) {
			this.provinceName = provinceName;
		}

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

		public String getStartTime() {
			return startTime;
		}

		public void setStartTime(String startTime) {
			this.startTime = startTime;
		}

		public String getEndTime() {
			return endTime;
		}

		public void setEndTime(String endTime) {
			this.endTime = endTime;
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
		
		public String getPlanType() {
			return planType;
		}

		public void setPlanType(String planType) {
			this.planType = planType;
		}

		public String getConverType() {
			return converType;
		}

		public void setConverType(String converType) {
			this.converType = converType;
		}
		public String getRelaNumerType() {
			return relaNumerType;
		}

		public void setRelaNumerType(String relaNumerType) {
			this.relaNumerType = relaNumerType;
		}
		@Override
		public String toString() {
			return "TaskInfo [taskName=" + taskName + ", taskDesc=" + taskDesc + ", provinceId=" + provinceId
					+ ", cityId=" + cityId + ", cityName=" + cityName + ", provinceName=" + provinceName
					+ ", startTime=" + startTime + ", endTime=" + endTime + ", planType=" + planType + ", converType="
					+ converType + ", relaNumerType=" + relaNumerType + ", lteCells.len=" + lteCells.length()
					+ ", isCheckNCell=" + isCheckNCell + ", isExportAssoTable=" + isExportAssoTable
					+ ", isExportMidPlan=" + isExportMidPlan + ", isExportNcCheckPlan=" + isExportNcCheckPlan
					+ ", isUseFlow=" + isUseFlow + ", ks=" + ks + ", sfFiles=" + sfFiles + ", freqAdjType="
					+ freqAdjType + ", d1Freq=" + d1Freq + ", d2Freq=" + d2Freq + ", sourceType=" + sourceType
					+ ", matrixDataCollectId=" + matrixDataCollectId + "]";
		}
	}
	/**
	 * LTE信息内部类 HO
	 * @author chao.xj
	 *
	 */
	public static class HoInfo {
		private int recordNum;
		private HoInfo(){
			
		}
		public int getRecordNum() {
			return recordNum;
		}
		public void setRecordNum(int recordNum) {
			this.recordNum = recordNum;
		}
	}
	/**
	 * mr信息内部类
	 * @author chao.xj
	 *
	 */
	public static class MrInfo {
		private int recordNum;

		private MrInfo(){
			
		}
		public int getRecordNum() {
			return recordNum;
		}
		public void setRecordNum(int recordNum) {
			this.recordNum = recordNum;
		}
	
	}
	
}
