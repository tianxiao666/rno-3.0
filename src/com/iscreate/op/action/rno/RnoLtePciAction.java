package com.iscreate.op.action.rno;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.struts2.ServletActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.action.rno.model.MrJobCond;
import com.iscreate.op.pojo.rno.RnoLteInterMatrixTaskInfo;
import com.iscreate.op.pojo.rno.RnoLteInterferCalcTask;
import com.iscreate.op.pojo.rno.RnoThreshold;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.Rno4GPciService;
import com.iscreate.op.service.rno.RnoLtePciService;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HttpTools;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;
import com.iscreate.plat.tools.ZipCommand;

@Controller
@Scope("prototype")
public class RnoLtePciAction extends RnoCommonAction {

	private static Log log = LogFactory.getLog(RnoLtePciAction.class);

	// 注入
	@Autowired
	private Rno4GPciService rno4gPciService;
	@Autowired
	private RnoLtePciService rnoLtePciService;

	private Map<String, String> cond;

	private MrJobCond mrJobCond;

	private long cityId, provinceId;
	private String cityName, provinceName;
	private String taskBeginTime, taskEndTime;

	private String error;

	private String taskName, taskDescription, taskInfoType, meaStartTime, meaEndTime;

	private Map<String, String> threshold;// 阈值门限

	private long jobId;

	// PCI规划
	private String planType;
	private String converType;
	private String cosi;
	private String isCheckNCell;
	private String lteCells;
	private String mrJobId;
	private InputStream exportInputStream;// 导出pci结果文件的输入流
	private String isWithFlow;
	private String isExportAssoTable;
	private String isExportMidPlan;
	private String isExportNcCheckPlan;
	private String ks;

	// 干扰矩阵
	private String lastMonday, lastSunday;

	public Map<String, String> getCond() {
		return cond;
	}

	public void setCond(Map<String, String> cond) {
		this.cond = cond;
	}

	public long getCityId() {
		return cityId;
	}

	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public long getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(long provinceId) {
		this.provinceId = provinceId;
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

	public String getTaskBeginTime() {
		return taskBeginTime;
	}

	public void setTaskBeginTime(String taskBeginTime) {
		this.taskBeginTime = taskBeginTime;
	}

	public String getTaskEndTime() {
		return taskEndTime;
	}

	public void setTaskEndTime(String taskEndTime) {
		this.taskEndTime = taskEndTime;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskDescription() {
		return taskDescription;
	}

	public void setTaskDescription(String taskDescription) {
		this.taskDescription = taskDescription;
	}

	public String getTaskInfoType() {
		return taskInfoType;
	}

	public void setTaskInfoType(String taskInfoType) {
		this.taskInfoType = taskInfoType;
	}

	public String getMeaStartTime() {
		return meaStartTime;
	}

	public void setMeaStartTime(String meaStartTime) {
		this.meaStartTime = meaStartTime;
	}

	public String getMeaEndTime() {
		return meaEndTime;
	}

	public void setMeaEndTime(String meaEndTime) {
		this.meaEndTime = meaEndTime;
	}

	public Map<String, String> getThreshold() {
		return threshold;
	}

	public void setThreshold(Map<String, String> threshold) {
		this.threshold = threshold;
	}

	public long getJobId() {
		return jobId;
	}

	public void setJobId(long jobId) {
		this.jobId = jobId;
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

	public String getCosi() {
		return cosi;
	}

	public void setCosi(String cosi) {
		this.cosi = cosi;
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

	public String getMrJobId() {
		return mrJobId;
	}

	public void setMrJobId(String mrJobId) {
		this.mrJobId = mrJobId;
	}

	public InputStream getExportInputStream() {
		return exportInputStream;
	}

	public void setExportInputStream(InputStream exportInputStream) {
		this.exportInputStream = exportInputStream;
	}

	public String getIsWithFlow() {
		return isWithFlow;
	}

	public void setIsWithFlow(String isWithFlow) {
		this.isWithFlow = isWithFlow;
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

	public String getKs() {
		return ks;
	}

	public void setKs(String ks) {
		this.ks = ks;
	}

	public String getLastMonday() {
		return lastMonday;
	}

	public void setLastMonday(String lastMonday) {
		this.lastMonday = lastMonday;
	}

	public String getLastSunday() {
		return lastSunday;
	}

	public void setLastSunday(String lastSunday) {
		this.lastSunday = lastSunday;
	}

	/** ############lte pci 计算新算法 部分 开始################### **/
	/**
	 * 
	 * @title 初始化lte干扰计算页面
	 * @return
	 * @author chao.xj
	 * @date 2015-3-26下午1:51:10
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String initNewLteInterferCalcPageAction() {
		// 取消任务时
		// 清空session
		// SessionService.getInstance().rmvValueByKey("NCSTASKINFO");
		initAreaList();// 加载区域相关信息
		return "success";
	}

	/**
	 * 
	 * @title 在创建lte干扰计算新任务消息过程中主导页面跳转
	 * @return
	 * @author chao.xj
	 * @date 2015-3-26下午2:29:35
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction() {
		log.debug("进入方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
		if (taskInfoType != null && "taskInfoForward".equals(taskInfoType)) {
			// 加载区域相关信息
			initAreaList();

			RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
					"MRTASKINFO");
			// taskobj.setThresholdDefault(thresholdObj);
			List<RnoThreshold> rnoThresholds;
			if (taskobj.getRnoThresholds() == null) {
				rnoThresholds = rnoLtePciService.getThresholdsByModuleType("LTEINTERFERCALC");
				taskobj.setRnoThresholds(rnoThresholds);
			} else {
				rnoThresholds = taskobj.getRnoThresholds();
			}
			// 获取不重复的分组条件
			HashSet<String> conditionGroups = new HashSet<String>();
			for (RnoThreshold one : rnoThresholds) {
				conditionGroups.add(one.getConditionGroup());
			}
			// List<List<RnoThreshold>> groupRnoThresholds = new
			// ArrayList<List<RnoThreshold>>();
			Map<Long, List<RnoThreshold>> groupRnoThresholds = new TreeMap<Long, List<RnoThreshold>>();
			List<RnoThreshold> groupRnoThreshold;
			long orderNum = 0;
			// 循环分组条件，加入参数对象
			for (String cond : conditionGroups) {
				groupRnoThreshold = new ArrayList<RnoThreshold>();
				for (RnoThreshold rnoThreshold : rnoThresholds) {
					if (cond.equals(rnoThreshold.getConditionGroup())) {
						groupRnoThreshold.add(rnoThreshold);
						orderNum = rnoThreshold.getOrderNum();
					}
				}
				groupRnoThresholds.put(orderNum, groupRnoThreshold);
			}

			taskobj.setGroupThresholds(groupRnoThresholds); // 用于分组显示
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "taskInfoForward";

		} else if (taskInfoType != null && "paramInfoBack".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "paramInfoBack";

		} else if (taskInfoType != null && "paramInfoForward".equals(taskInfoType)) {

			RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
					"MRTASKINFO");
			long cityId = taskobj.getTaskInfo().getCityId();
			String startTime = taskobj.getTaskInfo().getStartTime();
			String endTime = taskobj.getTaskInfo().getEndTime();
			// 查询爱立信和中兴在任务的数据详情
			List<Map<String, Object>> eriInfo = rnoLtePciService.getDataRecordFromHbase(cityId, startTime, endTime,
					"ERI");
			List<Map<String, Object>> zteInfo = rnoLtePciService.getDataRecordFromHbase(cityId, startTime, endTime,
					"ZTE");
			// 保存进session
			taskobj.setEriInfo(eriInfo);
			taskobj.setZteInfo(zteInfo);
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "paramInfoForward";

		} else if (taskInfoType != null && "importFlowFileBack".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "importFlowFileBack";
		} else if (taskInfoType != null && "importFlowFileForward".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
					"MRTASKINFO");
			/*
			 * RnoLteInterferCalcTask taskobj = new RnoLteInterferCalcTask();
			 * SessionService.getInstance().setValueByKey("ISWITHFLOW", taskobj);
			 */
			if (isWithFlow != null && "y".equals(isWithFlow)) {
				taskobj.setFlowInfo("y");
			} else if (isWithFlow != null && "n".equals(isWithFlow)) {
				taskobj.setFlowInfo("n");
			}
			// taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey("MRTASKINFO");
			G4SfDescQueryCond cond = new G4SfDescQueryCond();
			cond.setCityId(taskobj.getTaskInfo().getCityId());
			cond.setMeaBegTime(taskobj.getTaskInfo().getStartTime());
			cond.setMeaEndTime(taskobj.getTaskInfo().getEndTime());
			cond.setFactory("ALL");
			List<Map<String, String>> list = rnoLtePciService.querySfDataFromHbaseByPage(cond);
			taskobj.setSfFileInfo(list);
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "importFlowFileForward";
		} else if (taskInfoType != null && "sweepBack".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			// SessionService.getInstance().rmvValueByKey("ISWITHFLOW");
			RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
					"MRTASKINFO");
			taskobj.setFlowInfo("n");
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "sweepBack";
		} else if (taskInfoType != null && "sweepForward".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "sweepForward";
		} else if (taskInfoType != null && "overviewInfoBack".equals(taskInfoType)) {
			initAreaList();// 加载区域相关信息
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "overviewInfoBack";
		} else {
			// 先清空该 key下的属性值
			SessionService.getInstance().rmvValueByKey("MRTASKINFO");
			// SessionService.getInstance().rmvValueByKey("ISWITHFLOW");
			RnoLteInterferCalcTask taskobj = new RnoLteInterferCalcTask();
			SessionService.getInstance().setValueByKey("MRTASKINFO", taskobj);

			initAreaList();// 加载区域相关信息

			Calendar calbeg = Calendar.getInstance();
			Calendar calend = Calendar.getInstance();

			// n为调整的天数，0为今天，1为明天，2为后天，-1为昨天，-2为前天，依次类推

			int n = -5; // 开始时间默认设置为5天前
			calbeg.add(Calendar.DATE, n);
			taskBeginTime = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(calbeg.getTime());
			n = -1; // 结束时间默认为昨天
			calend.add(Calendar.DATE, n);
			taskEndTime = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(calend.getTime());
			log.debug("离开方法: stepByStepOperateNewLteInterCalcTaskInfoPageForAjaxAction taskInfoType=" + taskInfoType);
			return "goTaskInfoPage";
		}

	}

	/**
	 * 
	 * @title 在创建新任务消息过程中主导存储LTE干扰计算任务对象信息（下一步操作和提交任务操作）
	 * @author chao.xj
	 * @date 2015-3-26下午3:02:57
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void storageNewLteInterferCalcTaskObjInfoForAjaxAction() {
		log.debug("进入方法: storageNewLteInterferCalcTaskObjInfoForAjaxAction。");
		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
				"MRTASKINFO");
		String result = "";
		if (taskobj != null && "taskInfoForward".equals(taskInfoType)) {
			try {
				// 仅仅是存储任务消息内容
				RnoLteInterferCalcTask.TaskInfo ti = taskobj.getTaskInfo();
				ti.setTaskName(taskName);
				ti.setTaskDesc(taskDescription);
				ti.setStartTime(meaStartTime);
				ti.setEndTime(meaEndTime);
				ti.setProvinceId(provinceId);
				ti.setCityId(cityId);
				ti.setCityName(cityName.trim());
				ti.setProvinceName(provinceName.trim());
				ti.setPlanType(planType);
				ti.setConverType(converType);
				ti.setRelaNumerType(cosi);
				ti.setIsCheckNCell(isCheckNCell);
				ti.setIsExportAssoTable(isExportAssoTable);
				ti.setIsExportMidPlan(isExportMidPlan);
				ti.setIsExportNcCheckPlan(isExportNcCheckPlan);
				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		} else if (taskobj != null && "paramInfoBack".equals(taskInfoType)) {
			try {
				List<RnoThreshold> rnoThresholds = taskobj.getRnoThresholds();
				for (RnoThreshold rnoThreshold : rnoThresholds) {
					String code = rnoThreshold.getCode();
					for (String key : threshold.keySet()) {
						if (key.toUpperCase().equals(code)) {
							rnoThreshold.setDefaultVal(threshold.get(key));
							// System.out.println(key+" : "+threshold.get(key));
						}
					}
				}
				// 获取不重复的分组条件
				HashSet<String> conditionGroups = new HashSet<String>();
				for (RnoThreshold one : rnoThresholds) {
					conditionGroups.add(one.getConditionGroup());
				}
				// List<List<RnoThreshold>> groupRnoThresholds = new ArrayList<List<RnoThreshold>>();
				Map<Long, List<RnoThreshold>> groupRnoThresholds = new TreeMap<Long, List<RnoThreshold>>();
				long orderNum = 0;
				List<RnoThreshold> groupRnoThreshold;
				// 循环分组条件，加入参数对象
				for (String cond : conditionGroups) {
					groupRnoThreshold = new ArrayList<RnoThreshold>();
					for (RnoThreshold rnoThreshold : rnoThresholds) {
						if (cond.equals(rnoThreshold.getConditionGroup())) {
							groupRnoThreshold.add(rnoThreshold);
							orderNum = rnoThreshold.getOrderNum();
						}
					}
					// groupRnoThresholds.add(groupRnoThreshold);
					groupRnoThresholds.put(orderNum, groupRnoThreshold);
				}
				taskobj.setGroupThresholds(groupRnoThresholds);

				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		} else if (taskobj != null && "paramInfoForward".equals(taskInfoType)) {
			try {
				List<RnoThreshold> rnoThresholds = taskobj.getRnoThresholds();
				log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>rnoThresholds size=" + rnoThresholds.size());
				for (RnoThreshold rnoThreshold : rnoThresholds) {
					String code = rnoThreshold.getCode();
					for (String key : threshold.keySet()) {
						if (key.toUpperCase().equals(code)) {
							rnoThreshold.setDefaultVal(threshold.get(key));
							// System.out.println(key+" : "+threshold.get(key));
						}
					}
				}
				// 获取不重复的分组条件
				HashSet<String> conditionGroups = new HashSet<String>();
				for (RnoThreshold one : rnoThresholds) {
					conditionGroups.add(one.getConditionGroup());
				}
				// List<List<RnoThreshold>> groupRnoThresholds = new ArrayList<List<RnoThreshold>>();
				Map<Long, List<RnoThreshold>> groupRnoThresholds = new TreeMap<Long, List<RnoThreshold>>();
				long orderNum = 0;
				List<RnoThreshold> groupRnoThreshold;
				// 循环分组条件，加入参数对象
				for (String cond : conditionGroups) {
					groupRnoThreshold = new ArrayList<RnoThreshold>();
					for (RnoThreshold rnoThreshold : rnoThresholds) {
						if (cond.equals(rnoThreshold.getConditionGroup())) {
							groupRnoThreshold.add(rnoThreshold);
							orderNum = rnoThreshold.getOrderNum();
						}
					}
					// groupRnoThresholds.add(groupRnoThreshold);
					groupRnoThresholds.put(orderNum, groupRnoThreshold);
				}
				taskobj.setGroupThresholds(groupRnoThresholds);
				taskobj.setRnoThresholds(rnoThresholds);
				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		} else if (taskobj != null && "importFlowFileForward".equals(taskInfoType)) {
			try {
				/*
				 * RnoLteInterferCalcTask myTaskobj = new RnoLteInterferCalcTask();
				 * RnoLteInterferCalcTask.TaskInfo mti = myTaskobj.getTaskInfo();
				 * mti.setKs(ks);
				 * SessionService.getInstance().setValueByKey("MYTASKINFO", myTaskobj);
				 */
				taskobj.getTaskInfo().setKs(Double.parseDouble(ks));
				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		} else if (taskobj != null && "sweepForward".equals(taskInfoType)) {
			try {
				RnoLteInterferCalcTask.TaskInfo ti = taskobj.getTaskInfo();
				if (cond.get("filenames") != null && !"".equals(cond.get("filenames"))
						&& !"undefined".equals(cond.get("filenames").toLowerCase())) {
					ti.setSfFiles(cond.get("filenames"));
					if (cond.get("freqAdjType") != null && !"".equals(cond.get("freqAdjType"))
							&& !"undefined".equals(cond.get("freqAdjType").toLowerCase())) {
						ti.setFreqAdjType(cond.get("freqAdjType"));
						ti.setD1Freq(cond.get("d1Freq"));
						ti.setD2Freq(cond.get("d2Freq"));
					} else {
						ti.setFreqAdjType("");
					}
				} else {
					ti.setSfFiles("");
				}
				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		} else if (taskobj != null && "overViewBack".equals(taskInfoType)) {
			try {
				// 缓存需要优化的小区列
				taskobj.getTaskInfo().setLteCells(lteCells);

				result = "{'state':'true'}";
			} catch (Exception e) {
				result = "{'state':'false'}";
				e.printStackTrace();
			}
			HttpTools.writeToClient(result);
		}
	}

	/**
	 * 提交pci自动规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月28日15:09:34
	 */
	public void submitNewPciPlanAnalysisTaskAction() {
		log.debug("进入：submitNewPciPlanAnalysisTaskAction");
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		log.debug("提交者=" + account);

		RnoLteInterferCalcTask taskobj = (RnoLteInterferCalcTask) SessionService.getInstance().getValueByKey(
				"MRTASKINFO");

		// 保存需要优化的小区列
		taskobj.getTaskInfo().setLteCells(lteCells);

		Map<String, Object> res = null;
		if (taskobj != null) {
			// 取出session里保存的任务所需信息
			// Threshold threshold = taskobj.getThreshold(); //门限值
			List<RnoThreshold> rnoThresholds = taskobj.getRnoThresholds();
			RnoLteInterferCalcTask.TaskInfo taskInfo = taskobj.getTaskInfo(); // 任务信息
			if (cond.get("calType") != null && "y".equals(cond.get("calType").trim().toLowerCase())) {
				taskInfo.setUseFlow(true);
			} else {
				taskInfo.setUseFlow(false);
			}
			res = rnoLtePciService.submitPciPlanAnalysisTask(account, rnoThresholds, taskInfo);
			// 清空session
			SessionService.getInstance().rmvValueByKey("MRTASKINFO");
		}
		String result = gson.toJson(res);
		log.debug("退出submitNewPciPlanAnalysisTaskAction result=" + result);
		HttpTools.writeToClient(result);
	}

	/**
	 * 查询pci自动规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public void queryNewPciPlanAnalysisTaskByPageForAjaxAction() {
		log.debug("进入：queryNewPciPlanAnalysisTaskByPageForAjaxAction。cond=" + cond + ",page=" + page);
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		if (page == null) {
			log.error("方法queryNewPciPlanAnalysisTaskByPageForAjaxAction的page参数为空！");
			HttpTools.writeToClient("[]");
			return;
		}

		Page newPage = page.copy();
		List<Map<String, Object>> pciTasks = rnoLtePciService.queryPciPlanTaskByPage(cond, newPage, account);
		String res = gson.toJson(pciTasks);

		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'data':" + res + "}";
		log.debug("退出queryNewPciPlanAnalysisTaskByPageForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}

	/**
	 * 终止Pci规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public void stopNewPciJobByJobIdAndMrJobIdForAjaxAction() {
		log.debug("进入方法：stopNewPciJobByJobIdAndMrJobIdForAjaxAction。jobId=" + jobId + ", mrJobId=" + mrJobId);
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		mrJobCond = new MrJobCond(jobId, mrJobId, account, "");
		Map<String, Object> res = new HashMap<String, Object>();
		boolean flag = rnoLtePciService.stopJobByJobIdAndMrJobIdForAjaxAction(mrJobCond);
		res.put("flag", flag);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
		log.debug("退出方法：stopNewPciJobByJobIdAndMrJobIdForAjaxAction。result=" + result);
	}

	/**
	 * 下载pci自动规划结果文件
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */

	public String downloadNewPciFileAction() {
		log.debug("进入方法downloadNewPciFileAction。 jobId=" + jobId);
		// 获取任务的信息。
		Connection connection = DataSourceConn.initInstance().getConnection();
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
			error = "任务：" + jobId + "获取数据连接失败";
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
			}
			return "fail";
		}
		String sql = "select * from rno_lte_pci_job where job_id=" + jobId;
		List<Map<String, Object>> pciTaskInfo = RnoHelper.commonQuery(stmt, sql);

		if (pciTaskInfo.size() <= 0) {
			error = "不存在该" + jobId + "任务信息";
			if (stmt != null) {
				try {
					stmt.close();
					connection.close();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
			}
			return "fail";
		}
		// 区域ID
		long cityId = Long.parseLong(pciTaskInfo.get(0).get("CITY_ID").toString());
		Map<String, List<String>> cellIdToCessInfo = rnoLtePciService.getLteCellInfoByCellId(stmt, cityId);

		if (cellIdToCessInfo.size() <= 0) {
			error = "该区域" + cityId + "不存在系统小区信息";
			if (stmt != null) {
				try {
					stmt.close();
					connection.close();
				} catch (SQLException e2) {
					e2.printStackTrace();
				}
			}
			return "fail";
		}
		try {
			stmt.close();
			connection.close();
		} catch (SQLException e2) {
			e2.printStackTrace();
		}

		// 获取pci规划任务中的待优化小区字段
		Clob cellsClob = (Clob) pciTaskInfo.get(0).get("OPTIMIZE_CELLS");
		Map<String, String> cellsMap = new HashMap<String, String>();
		try {
			String[] cellStrs = cellsClob.getSubString(1, (int) cellsClob.length()).trim().split(",");
			for (String c : cellStrs) {
				if (!"".equals(c.trim())) {
					cellsMap.put(c, "");
				}
			}
		} catch (SQLException e3) {
			e3.printStackTrace();
		}

		List<String> sourceFileList = new ArrayList<String>();
		String result = "success";
		// Pci规划数据源文件的hdfs路径
		String resFilePath = pciTaskInfo.get(0).get("RESULT_DIR").toString();

		// 下载的Pci规划文件全路径
		Calendar calendar = new GregorianCalendar();
		Date createDate = new DateUtil().parseDateArbitrary(pciTaskInfo.get(0).get("CREATE_TIME").toString());
		calendar.setTime(createDate);
		String path = ServletActionContext.getServletContext().getRealPath("/op/rno/ana_result/");
		String dlFileRealdir = path + "/" + calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.MONTH) + 1)
				+ "/" + jobId + "/";
		// 下载的Pci规划文件名称
		String dlFileName = "";
		// 下载的Pci规划文件全路径
		String dlFileRealPath = "";
		if ("success".equals(result) && pciTaskInfo.get(0).get("IS_EXPORT_ASSOTABLE").toString().equals("YES")) {
			// System.out.println("进来关联表生成方法");
			// 下载的Pci规划文件名称
			dlFileName = jobId + "_PCI优化关联度排序表.xlsx";
			dlFileRealPath = dlFileRealdir + dlFileName;
			// System.out.println("sourceFileList:"+sourceFileList);
			File realdir = new File(dlFileRealdir);
			if (!realdir.exists()) {
				realdir.mkdirs();
			}
			File realfile = new File(dlFileRealPath);
			if (!realfile.exists()) {
				try {
					realfile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			String assoTableFileName = jobId + "_Asso_Table.current";
			File assoTableFile = FileTool.getFile(resFilePath + "/" + assoTableFileName);
			if (assoTableFile == null) {
				log.info("Pci规划计算的关联度表源数据文件不存在！assoTableFileName=" + resFilePath + "/" + assoTableFileName);
				error = "Pci规划计算的关联度表源数据文件不存在！assoTableFileName=" + resFilePath + "/" + assoTableFileName;
				return "fail";
			}
			List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
			Map<String, Object> one = null;
			DataInputStream dis = null;
			try {
				if (assoTableFile != null) {
					// System.out.println("assoTableFile不为空");
					// 当前数据文件存在
					dis = new DataInputStream(new BufferedInputStream(new FileInputStream(assoTableFile)));
					int dataNum = dis.readInt();
					for (int i = 0; i < dataNum; i++) {
						one = new HashMap<String, Object>();
						one.put("cellId", dis.readUTF());
						one.put("asso", dis.readDouble());
						res.add(one);
					}
					if (dis.readUTF().equals("finished")) {
						// 可以生成excel结果文件
						if (createAssoTableToExcel(dlFileRealPath, res)) {
							sourceFileList.add(dlFileRealPath);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (dis != null) {
						dis.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					log.info("获取Pci规划结果源文件中，读取数据出错");
					error = "获取Pci规划结果源文件中，读取数据出错";
					result = "error";
				}
				log.info("获取Pci规划结果源文件中，读取数据出错");
				error = "获取Pci规划结果源文件中，读取数据出错";
				result = "error";
			}
		}
		if ("success".equals(result) && pciTaskInfo.get(0).get("IS_EXPORT_MIDPLAN").toString().equals("YES")) {
			// System.out.println("进来中间方案生成方法");
			// 下载的Pci规划文件名称
			dlFileName = jobId + "_PCI优化中间方案个数.txt";
			// 下载的Pci规划文件全路径
			dlFileRealPath = dlFileRealdir + dlFileName;
			File realdir = new File(dlFileRealdir);
			if (!realdir.exists()) {
				realdir.mkdirs();
			}
			File realfile = new File(dlFileRealPath);
			if (!realfile.exists()) {
				try {
					realfile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			String midPlanCountFileName = jobId + "_MidPlanCount.current";
			File midPlanCountFile = FileTool.getFile(resFilePath + "/" + midPlanCountFileName);
			if (midPlanCountFile == null) {
				log.info("Pci规划计算的中间方案个数源数据文件不存在！midPlanCountFileName=" + resFilePath + "/" + midPlanCountFileName);
				error = "Pci规划计算的中间方案个数源数据文件不存在！midPlanCountFileName=" + resFilePath + "/" + midPlanCountFileName;
				return "fail";
			}
			int DataNum = 0;
			DataInputStream dis = null;
			try {
				if (midPlanCountFile != null) {
					// System.out.println("midPlanCountFile不为空");
					// 当前数据文件存在
					dis = new DataInputStream(new BufferedInputStream(new FileInputStream(midPlanCountFile)));
					DataNum = dis.readInt();
					// System.out.println("DataNum:"+DataNum);
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (dis != null) {
						dis.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					log.info("获取Pci规划结果源文件中，读取数据出错");
					error = "获取Pci规划结果源文件中，读取数据出错";
					result = "error";
				}
				log.info("获取Pci规划结果源文件中，读取数据出错");
				error = "获取Pci规划结果源文件中，读取数据出错";
				result = "error";
			}
			if ("success".equals(result)) {
				for (int n = 1; n <= DataNum; n++) {
					// 下载的Pci规划文件名称
					dlFileName = jobId + "_PCI优化中间方案_" + n + ".xlsx";
					// 下载的Pci规划文件全路径
					dlFileRealPath = dlFileRealdir + dlFileName;

					// System.out.println("sourceFileList:"+sourceFileList);
					File realdir1 = new File(dlFileRealdir);
					if (!realdir1.exists()) {
						realdir1.mkdirs();
					}
					File realfile1 = new File(dlFileRealPath);
					if (!realfile1.exists()) {
						try {
							realfile1.createNewFile();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					String midPlanFileName = jobId + "_Mid_Plan_" + n + ".current";

					File midPlanFile = FileTool.getFile(resFilePath + "/" + midPlanFileName);

					// 两个源文件不存在，不能提供下载
					if (midPlanFile == null) {
						log.info("Pci规划计算的源数据current文件不存在！currentFileName=" + resFilePath + "/" + midPlanFileName);
						error = "Pci规划计算的源数据backup文件不存在！backupFileName=" + resFilePath + "/" + midPlanFileName;
						return "fail";
					}

					DataInputStream dis1 = null;
					List<Map<String, Object>> res1 = new ArrayList<Map<String, Object>>();
					List<Map<String, Object>> res2 = new ArrayList<Map<String, Object>>();
					Map<String, Object> one1 = null;
					Map<String, Object> one2 = null;
					String isFinished = "";

					String cellId = "";
					List<String> cells = null;
					try {
						if (midPlanFile != null) {
							// System.out.println("midPlanFile不为空");
							// 当前数据文件存在，则选择current文件
							dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(midPlanFile)));
							dis1.readDouble();
							int totDataNum = dis1.readInt();
							int topListNum = dis1.readInt();
							for (int i = 0; i < totDataNum; i++) {
								one1 = new HashMap<String, Object>();
								cellId = dis1.readUTF();

								cells = cellIdToCessInfo.get(cellId);
								if (cells == null) {
									log.info("小区找不到对应工参数据：cellId = " + cellId);
									one1.put("oldPci", -1);
									one1.put("oldEarfcn", -1);
									one1.put("cellName", "未知小区");
								} else {
									// 小区名，pci,频点
									one1.put("cellName", cells.get(0));
									one1.put("oldPci", Integer.parseInt(cells.get(1)));
									one1.put("oldEarfcn", Integer.parseInt(cells.get(2)));
								}
								one1.put("cellId", cellId);
								int newEarfcn = dis1.readInt();
								if (newEarfcn == -1) {
									if (cells == null) {
										one1.put("newEarfcn", "找不到对应工参数据");
									} else {
										one1.put("newEarfcn", "找不到对应MR数据");
									}
								} else {
									one1.put("newEarfcn", newEarfcn);
								}
								int newPci = dis1.readInt();
								if (newPci == -1) {
									if (cells == null) {
										one1.put("newPci", "找不到对应工参数据");
									} else {
										one1.put("newPci", "找不到对应MR数据");
									}
								} else {
									one1.put("newPci", newPci);
								}
								one1.put("oriInterVal", dis1.readDouble());
								one1.put("interVal", dis1.readDouble());

								if (cellsMap.containsKey(cellId)) {
									one1.put("remark", "修改小区");
								} else {
									one1.put("remark", "MR其他小区");
								}
								res1.add(one1);
							}
							for (int j = 0; j < topListNum; j++) {
								one2 = new HashMap<String, Object>();
								one2.put("cell", dis1.readUTF());
								one2.put("inter", dis1.readDouble());
								res2.add(one2);
							}
							isFinished = dis1.readUTF();
							if (isFinished.equals("finished")) {
								// 可以生成excel结果文件
								if (createMidPlanToExcel(dlFileRealPath, res1, res2)) {
									sourceFileList.add(dlFileRealPath);
								}
								;
							}
						}

					} catch (Exception e) {
						e.printStackTrace();
						try {
							if (dis1 != null) {
								dis1.close();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
							log.info("获取Pci规划结果源文件中，读取数据出错");
							error = "获取Pci规划结果源文件中，读取数据出错";
							result = "error";
						}
						log.info("获取Pci规划结果源文件中，读取数据出错");
						error = "获取Pci规划结果源文件中，读取数据出错";
						result = "error";
					}

					if (result.equals("error")) {
						return "fail";
					}
				}
			}
		}
		if ("success".equals(result) && pciTaskInfo.get(0).get("IS_EXPORT_NCCHECKPLAN").toString().equals("YES")) {
			// System.out.println("进来邻区核查方案生成方法");
			// 下载的Pci规划文件名称
			dlFileName = jobId + "_PCI优化邻区核查方案.xlsx";

			// 下载的Pci规划文件全路径
			dlFileRealPath = dlFileRealdir + dlFileName;
			// System.out.println("sourceFileList:"+sourceFileList);
			File realdir1 = new File(dlFileRealdir);
			if (!realdir1.exists()) {
				realdir1.mkdirs();
			}
			File realfile1 = new File(dlFileRealPath);
			if (!realfile1.exists()) {
				try {
					realfile1.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			String ncCheckPlanFileName = jobId + "_Nc_Check_Plan.current";

			File ncCheckPlanFile = FileTool.getFile(resFilePath + "/" + ncCheckPlanFileName);

			// 两个源文件不存在，不能提供下载
			if (ncCheckPlanFile == null) {
				log.info("Pci规划计算的源数据current文件不存在！ncCheckPlanFileName=" + resFilePath + "/" + ncCheckPlanFileName);
				error = "Pci规划计算的源数据backup文件不存在！ncCheckPlanFileName=" + resFilePath + "/" + ncCheckPlanFileName;
				return "fail";
			}

			DataInputStream dis1 = null;
			List<Map<String, Object>> res1 = new ArrayList<Map<String, Object>>();
			Map<String, Object> one1 = null;
			String cellId = "";
			List<String> cells = null;
			try {
				if (ncCheckPlanFile != null) {
					// System.out.println("ncCheckPlanFile不为空");
					// 当前数据文件存在，则选择current文件
					dis1 = new DataInputStream(new BufferedInputStream(new FileInputStream(ncCheckPlanFile)));
					dis1.readDouble();
					int dataNum = dis1.readInt();
					for (int i = 0; i < dataNum; i++) {
						one1 = new HashMap<String, Object>();
						cellId = dis1.readUTF();
						cells = cellIdToCessInfo.get(cellId);
						if (cells == null) {
							log.info("小区找不到对应工参数据：cellId = " + cellId);
							one1.put("cellName", "未知小区");
							one1.put("oldPci", -1);
							one1.put("oldEarfcn", -1);
						} else {
							// 小区名，pci,频点
							one1.put("cellName", cells.get(0));
							one1.put("oldPci", Integer.parseInt(cells.get(1)));
							one1.put("oldEarfcn", Integer.parseInt(cells.get(2)));
						}
						one1.put("cellId", cellId);
						int newEarfcn = dis1.readInt();
						if (newEarfcn == -1) {
							if (cells == null) {
								one1.put("newEarfcn", "找不到对应工参数据");
							} else {
								one1.put("newEarfcn", "找不到对应MR数据");
							}
						} else {
							one1.put("newEarfcn", newEarfcn);
						}
						int newPci = dis1.readInt();
						if (newPci == -1) {
							if (cells == null) {
								one1.put("newPci", "找不到对应工参数据");
							} else {
								one1.put("newPci", "找不到对应MR数据");
							}
						} else {
							one1.put("newPci", newPci);
						}
						one1.put("oriInterVal", dis1.readDouble());
						one1.put("interVal", dis1.readDouble());

						if (cellsMap.containsKey(cellId)) {
							one1.put("remark", "修改小区");
						} else {
							one1.put("remark", "MR其他小区");
						}
						res1.add(one1);
					}
					if (dis1.readUTF().equals("finished")) {
						// 可以生成excel结果文件
						if (createExcelFile(dlFileRealPath, res1)) {
							sourceFileList.add(dlFileRealPath);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					if (dis1 != null) {
						dis1.close();
					}
				} catch (IOException e1) {
					e1.printStackTrace();
					log.info("获取Pci规划结果源文件中，读取数据出错");
					error = "获取Pci规划结果源文件中，读取数据出错";
					result = "error";
				}
				log.info("获取Pci规划结果源文件中，读取数据出错");
				error = "获取Pci规划结果源文件中，读取数据出错";
				result = "error";
			}
		}
		if ("success".equals(result) && pciTaskInfo.get(0).get("FREQ_ADJ_TYPE") != null
				&& !"".equals(pciTaskInfo.get(0).get("FREQ_ADJ_TYPE").toString())) {
			// System.out.println("进来邻区核查方案生成方法");
			// 下载的Pci规划文件名称
			dlFileName = jobId + "_D1小区表.csv";
			// 下载的Pci规划文件全路径
			dlFileRealPath = dlFileRealdir + dlFileName;
			String d1Path = dlFileRealPath;

			// System.out.println("sourceFileList:"+sourceFileList);
			File realdir = new File(dlFileRealdir);
			if (!realdir.exists()) {
				realdir.mkdirs();
			}
			File realfile = new File(dlFileRealPath);
			if (!realfile.exists()) {
				try {
					realfile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			String resfileName = jobId + "_d1List.current";
			File resFile = FileTool.getFile(resFilePath + "/" + resfileName);
			String line = "";
			BufferedReader br = null;
			BufferedWriter bw = null;

			// 源文件不存在，不能提供下载
			if (resFile == null) {
				error = "Pci规划计算的源数据d1频文件不存在！resFile=" + resFilePath + "/" + resFile;
				log.info(error);
				result = "error";
			} else {
				try {
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dlFileRealPath), "GBK"));
					// 标题头
					line = "小区标识," + "频点," + "干扰值";
					bw.write(line);
					bw.newLine();
					br = new BufferedReader(new InputStreamReader(new FileInputStream(resFile), "utf-8"));
					while ((line = br.readLine()) != null) {
						bw.write(line.replace("#", ","));
						bw.newLine();
					}
					bw.close();
					br.close();
				} catch (IOException e) {
					error = "Pci规划计算的源数据d1频文件不存在！resFile=" + resFilePath + "/" + resFile;
					log.info(error);
					result = "error";
				}
			}
			if ("success".equals(result)) {
				// 下载的Pci规划文件名称
				dlFileName = jobId + "_D2小区表.csv";
				// 下载的Pci规划文件全路径
				dlFileRealPath = dlFileRealdir + dlFileName;
				resfileName = jobId + "_d2List.current";
				resFile = FileTool.getFile(resFilePath + "/" + resfileName);
				// 源文件不存在，不能提供下载
				if (resFile == null) {
					error = "Pci规划计算的源数据d2频文件不存在！resFile=" + resFilePath + "/" + resFile;
					log.info(error);
					result = "error";
				} else {
					try {
						bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dlFileRealPath), "GBK"));
						// 标题头
						line = "小区标识," + "频点," + "干扰值";
						bw.write(line);
						bw.newLine();
						br = new BufferedReader(new InputStreamReader(new FileInputStream(resFile), "utf-8"));
						while ((line = br.readLine()) != null) {
							bw.write(line.replace("#", ","));
							bw.newLine();
						}
						bw.close();
						br.close();
					} catch (IOException e) {
						error = "Pci规划计算的源数据d2频文件不存在！resFile=" + resFilePath + "/" + resFile;
						log.info(error);
						result = "error";
					}
				}
				sourceFileList.add(d1Path);
				sourceFileList.add(dlFileRealPath);
			}
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if (bw != null) {
				try {
					bw.close();
				} catch (IOException e) {
				}
			}
		}
		// 下载的Pci规划文件名称
		dlFileName = pciTaskInfo.get(0).get("DL_FILE_NAME").toString();
		// 下载的Pci规划文件全路径
		dlFileRealPath = dlFileRealdir + jobId + "_PCI优化方案.xlsx";
		sourceFileList.add(dlFileRealPath);
		// System.out.println("sourceFileList:"+sourceFileList);
		File realdir = new File(dlFileRealdir);
		if (!realdir.exists()) {
			realdir.mkdirs();
		}
		File realfile = new File(dlFileRealPath);
		if (!realfile.exists()) {
			try {
				realfile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Pci规划数据源文件的名称,源数据文件有两个，一个是current后缀，一个是backup后缀
		String currentFileName = pciTaskInfo.get(0).get("RD_FILE_NAME").toString() + ".current";
		String backupFileName = pciTaskInfo.get(0).get("RD_FILE_NAME").toString() + ".backup";

		File currentFile = FileTool.getFile(resFilePath + "/" + currentFileName);
		File backupFile = FileTool.getFile(resFilePath + "/" + backupFileName);

		// 两个源文件不存在，不能提供下载
		if (currentFile == null && backupFile == null) {
			log.info("Pci规划计算的源数据current文件不存在！currentFileName=" + resFilePath + "/" + currentFileName);
			log.info("Pci规划计算的源数据backup文件不存在！backupFileName=" + resFilePath + "/" + backupFileName);
			error = "Pci规划计算的源数据backup文件不存在！backupFileName=" + resFilePath + "/" + backupFileName;
			return "fail";
		}

		DataInputStream disCur = null;
		List<Map<String, Object>> res = new ArrayList<Map<String, Object>>();
		Map<String, Object> one = null;
		String cellId = "";
		List<String> cells = null;
		boolean createFile = false;
		try {
			if (currentFile != null) {
				// System.out.println("currentFile不为空");
				// 当前数据文件存在，则选择current文件
				disCur = new DataInputStream(new BufferedInputStream(new FileInputStream(currentFile)));
				disCur.readDouble();
				int dataNum = disCur.readInt();
				for (int i = 0; i < dataNum; i++) {
					one = new HashMap<String, Object>();
					cellId = disCur.readUTF();
					cells = cellIdToCessInfo.get(cellId);
					if (cells == null) {
						log.info("小区找不到对应工参数据：cellId = " + cellId);
						one.put("cellName", "未知小区");
						one.put("oldPci", -1);
						one.put("oldEarfcn", -1);
					} else {
						// 小区名，pci,频点
						one.put("cellName", cells.get(0));
						one.put("oldPci", Integer.parseInt(cells.get(1)));
						one.put("oldEarfcn", Integer.parseInt(cells.get(2)));
					}
					one.put("cellId", cellId);
					int newEarfcn = disCur.readInt();
					if (newEarfcn == -1) {
						if (cells == null) {
							one.put("newEarfcn", "找不到对应工参数据");
						} else {
							one.put("newEarfcn", "找不到对应MR数据");
						}
					} else {
						one.put("newEarfcn", newEarfcn);
					}
					int newPci = disCur.readInt();
					if (newPci == -1) {
						if (cells == null) {
							one.put("newPci", "找不到对应工参数据");
						} else {
							one.put("newPci", "找不到对应MR数据");
						}
					} else {
						one.put("newPci", newPci);
					}
					one.put("oriInterVal", disCur.readDouble());
					one.put("interVal", disCur.readDouble());

					if (cellsMap.containsKey(cellId)) {
						one.put("remark", "修改小区");
					} else {
						one.put("remark", "MR其他小区");
					}
					res.add(one);
				}

				if (disCur.readUTF().equals("finished")) {
					createFile = true;
				} else {
					// 当前文件没有正常结束，读取backup文件
					res.clear();
				}
			}
			if (!createFile && backupFile != null) {
				// current为null，读取backup文件
				disCur = new DataInputStream(new BufferedInputStream(new FileInputStream(backupFile)));
				disCur.readDouble();
				int dataNum = disCur.readInt();
				for (int i = 0; i < dataNum; i++) {
					one = new HashMap<String, Object>();
					cellId = disCur.readUTF();
					cells = cellIdToCessInfo.get(cellId);
					if (cells == null) {
						log.info("小区找不到对应工参数据：cellId = " + cellId);
						one.put("cellName", "未知小区");
						one.put("oldPci", -1);
						one.put("oldEarfcn", -1);
					} else {
						// 小区名，pci,频点
						one.put("cellName", cells.get(0));
						one.put("oldPci", Integer.parseInt(cells.get(1)));
						one.put("oldEarfcn", Integer.parseInt(cells.get(2)));
					}
					one.put("cellId", cellId);
					int newEarfcn = disCur.readInt();
					if (newEarfcn == -1) {
						if (cells == null) {
							one.put("newEarfcn", "找不到对应工参数据");
						} else {
							one.put("newEarfcn", "找不到对应MR数据");
						}
					} else {
						one.put("newEarfcn", newEarfcn);
					}
					int newPci = disCur.readInt();
					if (newPci == -1) {
						if (cells == null) {
							one.put("newPci", "找不到对应工参数据");
						} else {
							one.put("newPci", "找不到对应MR数据");
						}
					} else {
						one.put("newPci", newPci);
					}
					one.put("oriInterVal", disCur.readDouble());
					one.put("interVal", disCur.readDouble());

					if (cellsMap.containsKey(cellId)) {
						one.put("remark", "修改小区");
					} else {
						one.put("remark", "MR其他小区");
					}
					res.add(one);
				}
				if (disCur.readUTF().equals("finished")) {
					createFile = true;
				} else {
					log.info("Pci规划结果源文件中，backup文件没有正常结尾，current文件则不存在");
					error = "Pci规划结果源文件中，backup文件没有正常结尾，current文件则不存在";
					result = "error";
				}
			}

			if (createFile) {
				File outFile = null;
				String outFileName = dlFileName;
				String outFilePath = dlFileRealPath;
				createExcelFile(dlFileRealPath, res);
				if (sourceFileList.size() > 1) {
					outFileName = jobId + "_PCI优化.zip";
					outFilePath = ZipCommand.AddtoZip(sourceFileList, outFileName);
				}
				outFile = new File(outFilePath);
				if (outFile.exists()) {
					setFileName(new String(outFileName.getBytes(), "iso-8859-1"));
					try {
						exportInputStream = new FileInputStream(outFile);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
						result = "error";
						error = exportInputStream + "获取文件流异常,文件路径：" + outFilePath;
						System.out.println(error);
					}
				} else {
					log.error("Pci规划结果文件不存在！");
					error = "Pci规划结果文件不存在！,文件路径：" + outFilePath;
					if (disCur != null) {
						disCur.close();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				if (disCur != null) {
					disCur.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
				log.info("获取Pci规划结果源文件中，读取数据出错");
				error = "获取Pci规划结果源文件中，读取数据出错";
				result = "error";
			}
			log.info("获取Pci规划结果源文件中，读取数据出错");
			error = "获取Pci规划结果源文件中，读取数据出错";
			result = "error";
		}

		if (result.equals("error")) {
			return "fail";
		}
		log.debug("退出方法downloadNewPciFileAction。 jobId=" + jobId);
		return result;
	}

	/**
	 * 在项目的数据目录创建Pci结果excel文件
	 * 
	 * @param fileRealPath
	 * @param res
	 * @return
	 */
	private static boolean createExcelFile(String fileRealPath, List<Map<String, Object>> res) {

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileRealPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		Workbook workbook = new SXSSFWorkbook();
		Sheet sheet = workbook.createSheet();
		Row row;
		Cell cell;
		row = sheet.createRow(0);
		cell = row.createCell(0);
		cell.setCellValue("小区名称");
		cell = row.createCell(1);
		cell.setCellValue("小区标识");
		cell = row.createCell(2);
		cell.setCellValue("原频点");
		cell = row.createCell(3);
		cell.setCellValue("新频点");
		cell = row.createCell(4);
		cell.setCellValue("原PCI");
		cell = row.createCell(5);
		cell.setCellValue("新PCI");
		cell = row.createCell(6);
		cell.setCellValue("原干扰值");
		cell = row.createCell(7);
		cell.setCellValue("干扰值");
		cell = row.createCell(8);
		cell.setCellValue("备注");
		for (int i = 0; i < res.size(); i++) {
			row = sheet.createRow(i + 1);
			// 小区名称
			cell = row.createCell(0);
			cell.setCellValue(res.get(i).get("cellName").toString());
			// 小区标识
			cell = row.createCell(1);
			cell.setCellValue(res.get(i).get("cellId").toString());
			// 原频点
			cell = row.createCell(2);
			cell.setCellValue((Integer) res.get(i).get("oldEarfcn"));
			// 新频点
			cell = row.createCell(3);
			if (isNumeric(res.get(i).get("newEarfcn").toString())) {
				cell.setCellValue((Integer) res.get(i).get("newEarfcn"));
			} else {
				cell.setCellValue(res.get(i).get("newEarfcn").toString());
			}
			// 原pci
			cell = row.createCell(4);
			cell.setCellValue((Integer) res.get(i).get("oldPci"));
			// 新pci
			cell = row.createCell(5);
			if (isNumeric(res.get(i).get("newPci").toString())) {
				cell.setCellValue((Integer) res.get(i).get("newPci"));
			} else {
				cell.setCellValue(res.get(i).get("newPci").toString());
			}
			// 原干扰值
			cell = row.createCell(6);
			cell.setCellValue((Double) res.get(i).get("oriInterVal"));
			// 新干扰值
			cell = row.createCell(7);
			cell.setCellValue((Double) res.get(i).get("interVal"));
			// 备注
			cell = row.createCell(8);
			cell.setCellValue(res.get(i).get("remark").toString());
		}
		// 最终写入文件
		try {
			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 在项目的数据目录创建关联度排序表excel文件
	 * 
	 * @param fileRealPath
	 * @param res
	 * @return
	 */
	private static boolean createAssoTableToExcel(String fileRealPath, List<Map<String, Object>> res) {

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileRealPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		Workbook workbook = new SXSSFWorkbook();
		Sheet sheet1 = workbook.createSheet();
		Row row1;
		Cell cell1;
		row1 = sheet1.createRow((short) 0);
		cell1 = row1.createCell(0);
		cell1.setCellValue("排序号");
		cell1 = row1.createCell(1);
		cell1.setCellValue("小区标识");
		cell1 = row1.createCell(2);
		cell1.setCellValue("关联度");
		cell1 = row1.createCell(3);
		for (int i = 0; i < res.size(); i++) {
			row1 = sheet1.createRow((short) i + 1);
			cell1 = row1.createCell(0);
			cell1.setCellValue(i + 1);
			cell1 = row1.createCell(1);
			cell1.setCellValue(res.get(i).get("cellId").toString());
			cell1 = row1.createCell(2);
			cell1.setCellValue(Double.parseDouble(res.get(i).get("asso").toString()));
		}
		// 最终写入文件
		try {
			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * 在项目的数据目录创建PCI优化中间方案excel文件
	 * 
	 * @param fileRealPath
	 * @param res
	 * @return
	 */
	private static boolean createMidPlanToExcel(String fileRealPath, List<Map<String, Object>> res,
			List<Map<String, Object>> res2) {

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fileRealPath);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}

		Workbook workbook = new SXSSFWorkbook();
		Sheet sheet1 = workbook.createSheet();
		Sheet sheet2 = workbook.createSheet();
		Row row1, row2;
		Cell cell1, cell2;
		row1 = sheet1.createRow((short) 0);
		cell1 = row1.createCell(0);
		cell1.setCellValue("小区名称");
		cell1 = row1.createCell(1);
		cell1.setCellValue("小区标识");
		cell1 = row1.createCell(2);
		cell1.setCellValue("原频点");
		cell1 = row1.createCell(3);
		cell1.setCellValue("新频点");
		cell1 = row1.createCell(4);
		cell1.setCellValue("原PCI");
		cell1 = row1.createCell(5);
		cell1.setCellValue("新PCI");
		cell1 = row1.createCell(6);
		cell1.setCellValue("原干扰值");
		cell1 = row1.createCell(7);
		cell1.setCellValue("干扰值");
		cell1 = row1.createCell(8);
		cell1.setCellValue("备注");
		row2 = sheet2.createRow((short) 0);
		cell2 = row2.createCell(0);
		cell2.setCellValue("排序号");
		cell2 = row2.createCell(1);
		cell2.setCellValue("小区标识");
		cell2 = row2.createCell(2);
		cell2.setCellValue("干扰值");
		for (int i = 0; i < res.size(); i++) {
			row1 = sheet1.createRow(i + 1);
			// 小区名称
			cell1 = row1.createCell(0);
			cell1.setCellValue(res.get(i).get("cellName").toString());
			// 小区标识
			cell1 = row1.createCell(1);
			cell1.setCellValue(res.get(i).get("cellId").toString());
			// 原频点
			cell1 = row1.createCell(2);
			cell1.setCellValue((Integer) res.get(i).get("oldEarfcn"));
			// 新频点
			cell1 = row1.createCell(3);
			if (isNumeric(res.get(i).get("newEarfcn").toString())) {
				cell1.setCellValue((Integer) res.get(i).get("newEarfcn"));
			} else {
				cell1.setCellValue(res.get(i).get("newEarfcn").toString());
			}
			// 原pci
			cell1 = row1.createCell(4);
			cell1.setCellValue((Integer) res.get(i).get("oldPci"));
			// 新pci
			cell1 = row1.createCell(5);
			if (isNumeric(res.get(i).get("newPci").toString())) {
				cell1.setCellValue((Integer) res.get(i).get("newPci"));
			} else {
				cell1.setCellValue(res.get(i).get("newPci").toString());
			}
			// 原干扰值
			cell1 = row1.createCell(6);
			cell1.setCellValue((Double) res.get(i).get("oriInterVal"));
			// 新干扰值
			cell1 = row1.createCell(7);
			cell1.setCellValue((Double) res.get(i).get("interVal"));
			// 备注
			cell1 = row1.createCell(8);
			cell1.setCellValue(res.get(i).get("remark").toString());
		}
		for (int j = 0; j < res2.size(); j++) {
			row2 = sheet2.createRow(j + 1);
			cell2 = row2.createCell(0);
			cell2.setCellValue(j + 1);
			cell2 = row2.createCell(1);
			cell2.setCellValue(res2.get(j).get("cell").toString());
			cell2 = row2.createCell(2);
			cell2.setCellValue(Double.parseDouble(res2.get(j).get("inter").toString()));
		}

		// 最终写入文件
		try {
			workbook.write(fos);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		try {
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static boolean isNumeric(String str) {
		Pattern pattern = Pattern.compile("[0-9]*");
		Matcher isNum = pattern.matcher(str);
		if (!isNum.matches()) {
			return false;
		}
		return true;
	}

	/** ############lte pci 计算新算法 部分 结束################### **/

	/** ############lte 干扰矩阵 计算新算法 部分 开始################### **/
	/**
	 * 
	 * @title 初始化4G干扰矩阵管理页面
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午9:48:28
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String initNewLteInterferMartixManageAction() {
		initAreaList();// 加载区域相关信息
		return "success";
	}

	/**
	 * 
	 * @title 初始化新增4G干扰矩阵页面
	 * @return
	 * @author chao.xj
	 * @date 2015-4-15上午9:52:12
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String initNewLteInterferMartixAddForAjaxAction() {

		initAreaList();// 加载区域相关信息

		Calendar cal = Calendar.getInstance();
		// n为推迟的周数，1本周，-1向前推迟一周，2下周，依次类推
		int n = -1;
		cal.add(Calendar.DATE, n * 7);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		lastMonday = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		// System.out.println(monday);

		n = 1;
		cal.add(Calendar.DATE, n * 7);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		lastSunday = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(cal.getTime());
		// System.out.println(sunday);

		return "success";
	}

	/**
	 * 
	 * @title 分页查询4g干扰矩阵信息
	 * @author chao.xj
	 * @date 2015-4-15上午10:58:16
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void queryNewLteInterferMartixByPageForAjaxAction() {
		log.debug("进入方法queryNewLteInterferMartixByPageForAjaxAction. page=" + page + ",cond=" + cond);

		Map<String, Object> result = new HashMap<String, Object>();
		if (page == null) {
			log.error("方法queryNewLteInterferMartixByPageForAjaxAction的page参数为空！");
			HttpTools.writeToClient(gson.toJson(result));
			return;
		}

		Page newPage = page.copy();
		List<Map<String, Object>> res = rnoLtePciService.queryLteInterferMartixByPage(cond, newPage);

		log.info("计算以后，page=" + newPage);
		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		result.put("page", newPage);
		result.put("data", res);
		log.debug("退出方法queryNewLteInterferMartixByPageForAjaxAction. result=" + result);
		HttpTools.writeToClient(gson.toJson(result));
	}

	/**
	 * 通过城市ID检查任务名是否可用
	 * 
	 * @author chen.c10
	 * @date 2015年10月23日 下午12:18:40
	 * @company 怡创科技
	 * @version V1.0
	 */
	public void checkTaskNameByCityIdNewAjaxAction() {
		log.debug("进入方法checkTaskNameByCityIdNewAjaxAction. attachParams=" + attachParams);
		if (attachParams == null || attachParams.isEmpty()) {
			log.info("未传入查询条件");
		}
		String taskName = attachParams.get("taskName").toString();
		long cityId = Long.parseLong(attachParams.get("cityId").toString());
		List<String> taskNameList = rnoLtePciService.queryTaksNameListByCityId(cityId);
		String result = "success";
		if (taskNameList.contains(taskName)) {
			result = "fail";
		}
		log.debug("退出方法checkTaskNameByCityIdNewAjaxAction，result=" + result);
		HttpTools.writeToClient(result);
	}

	/**
	 * 
	 * @title 检查这周是否计算过4g pci干扰矩阵
	 * @author chao.xj
	 * @date 2015-4-15下午2:02:05
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void isExistedNewLteInterMartixThisWeekAction() {
		log.debug("进入方法isExistedNewLteInterMartixThisWeekAction. cond=" + cond);
		if (cond == null || cond.isEmpty()) {
			log.info("未传入查询条件");
		}

		Map<String, Object> result = new HashMap<String, Object>();

		long areaId = Long.parseLong(cond.get("cityId").toString());
		Calendar cal = Calendar.getInstance();
		// 获取这个周一
		cal.add(Calendar.DATE, 0 * 7);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		String thisMonday = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		// 获取今天
		cal = Calendar.getInstance();
		String today = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
		// 查询结果
		List<Map<String, Object>> res = rnoLtePciService.checkLteInterMartixThisWeek(areaId, thisMonday, today);
		// 转为星期命名
		String desc = "";
		boolean flag = false;
		if (res != null && res.size() > 0) {
			String createTime = res.get(0).get("CREATE_DATE").toString();
			String timeArray[] = createTime.split(" ");
			Date date = null;
			try {
				date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(createTime);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			cal = Calendar.getInstance();
			cal.setTime(date);
			int weekNum = cal.get(Calendar.DAY_OF_WEEK);

			switch (weekNum) {
			case 1:
				desc = "本周周日 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 2:
				desc = "本周周一 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 3:
				desc = "本周周二 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 4:
				desc = "本周周三 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 5:
				desc = "本周周四 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 6:
				desc = "本周周五 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			case 7:
				desc = "本周周六 " + timeArray[1] + " 曾经计算过干扰矩阵";
				flag = true;
				break;
			default:
				break;
			}
		}

		result.put("flag", flag);
		result.put("desc", desc);
		log.debug("退出方法isExistedNewLteInterMartixThisWeekAction. result=" + result);
		HttpTools.writeToClient(gson.toJson(result));
	}

	/**
	 * 终止Pci规划任务
	 * 
	 * @author peng.jm
	 * @date 2015年3月31日15:23:10
	 */
	public void stopNewLteInterMartixJobByJobIdAndMrJobIdForAjaxAction() {
		log.debug("进入方法：stopNewLteInterMartixJobByJobIdAndMrJobIdForAjaxAction。cond=" + cond);
		long jobId = Long.parseLong(cond.get("jobId").toString());
		String mrJobId = cond.get("mrJobId").toString();
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		mrJobCond = new MrJobCond(jobId, mrJobId, account, "");
		Map<String, Object> res = new HashMap<String, Object>();
		boolean flag = rnoLtePciService.stopJobByJobIdAndMrJobIdForAjaxAction(mrJobCond);
		res.put("flag", flag);
		String result = gson.toJson(res);
		HttpTools.writeToClient(result);
		log.debug("退出方法：stopNewPciJobByJobIdAndMrJobIdForAjaxAction。result=" + result);
	}

	/**
	 * 
	 * @title 新增4g pci 干扰矩阵
	 * @author chao.xj
	 * @date 2015-4-15下午3:09:45
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void addNewLteInterMartixForAjaxAction() {
		log.debug("进入方法addNewLteInterMartixForAjaxAction。cond=" + cond);
		if (cond == null || cond.isEmpty()) {
			log.info("未传入查询条件");
		}
		RnoLteInterMatrixTaskInfo taskInfo = new RnoLteInterMatrixTaskInfo();
		taskInfo.setAccount((String) SessionService.getInstance().getValueByKey("userId"));
		taskInfo.setJobType(cond.get("jobType"));
		taskInfo.setBegTime(cond.get("begTime"));
		taskInfo.setEndTime(cond.get("endTime"));
		taskInfo.setDataType(cond.get("dataType"));
		taskInfo.setCityId(Long.parseLong(cond.get("cityId")));
		taskInfo.setTaskName(cond.get("taskName"));
		taskInfo.setSameFreqCellCoefWeight(Double.parseDouble(cond.get("SAMEFREQCELLCOEFWEIGHT")));
		taskInfo.setSwitchRatioWeight(Double.parseDouble(cond.get("SWITCHRATIOWEIGHT")));
		taskInfo.setSfFiles(cond.get("sffilenames"));

		Calendar cal = Calendar.getInstance();
		// 获取上周周一
		cal.add(Calendar.DATE, -1 * 7);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		lastMonday = new SimpleDateFormat("yyyy-MM-dd 00:00:00").format(cal.getTime());
		// 获取上周周日
		cal.add(Calendar.DATE, 1 * 7);
		cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
		lastSunday = new SimpleDateFormat("yyyy-MM-dd 23:59:59").format(cal.getTime());

		DateUtil dateUtil = new DateUtil();
		Date startTime = dateUtil.parseDateArbitrary(taskInfo.getBegTime());
		Date endTime = dateUtil.parseDateArbitrary(taskInfo.getEndTime());
		Date lastMon = dateUtil.parseDateArbitrary(lastMonday);
		Date lastSun = dateUtil.parseDateArbitrary(lastSunday);

		boolean isDateRight = false;
		boolean isMrExist = false;
		boolean isHoExist = false;
		// 判断日期是否符合要求
		if ((endTime.after(startTime) || endTime.equals(startTime))
				&& (lastSun.after(endTime) || lastSun.equals(endTime))
				&& (lastSun.after(startTime) || lastSun.equals(startTime))
				&& (startTime.after(lastMon) || startTime.equals(lastMon))
				&& (endTime.after(lastMon) || endTime.equals(lastMon))) {

			isDateRight = true;
		}
		// 判断日期范围是否存在MR数据
		long mrCnt = rno4gPciService.queryMrDataCountByCond(cond);
		long hoCnt = rno4gPciService.queryHoDataCountByCond(cond);
		if (mrCnt > 0) {
			isMrExist = true;
		}
		if (hoCnt > 0) {
			isHoExist = true;
		}

		isDateRight = true;
		Map<String, Object> result = new HashMap<String, Object>();
		String dataType = taskInfo.getDataType();
		if (dataType.equals("MR")) {
			if (isMrExist) {
				// 提交mr干扰矩阵计算任务
				rnoLtePciService.addLteInterMartix(taskInfo);
			}
		} else if (dataType.equals("HO")) {
			if (isHoExist) {
				// 提交mr干扰矩阵计算任务
				rnoLtePciService.addLteInterMartix(taskInfo);
			}
		} else {
			if (isMrExist || isHoExist) {
				// 提交mr干扰矩阵计算任务
				rnoLtePciService.addLteInterMartix(taskInfo);
			}
		}
		result.put("isMrExist", isMrExist);
		result.put("isHoExist", isHoExist);
		result.put("isDateRight", isDateRight);
		result.put("dataType", dataType);
		log.debug("退出方法addNewLteInterMartixForAjaxAction。result=" + result);
		HttpTools.writeToClient(gson.toJson(result));
	}

	/**
	 * TODO
	 * 
	 * @title 分页加载MR数据
	 * @author chao.xj
	 * @date 2015-4-15上午11:23:20
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void querySfFilesByPageForAjaxAction() {
		log.debug("进入方法querySfFilesByPageForAjaxAction. page=" + page + ",cond=" + cond);
		if (cond == null || cond.isEmpty()) {
			log.info("未传入查询条件");
		}

		Map<String, Object> result = new HashMap<String, Object>();
		if (page == null) {
			log.error("方法querySfFilesByPageForAjaxAction的page参数为空！");
			HttpTools.writeToClient(gson.toJson(result));
			return;
		}

		long cityId = Long.parseLong(cond.get("cityId").toString());
		String startTime = cond.get("begTime").toString();
		String endTime = cond.get("endTime").toString();

		G4SfDescQueryCond g4SfDescQueryCond = new G4SfDescQueryCond();
		g4SfDescQueryCond.setCityId(cityId);
		g4SfDescQueryCond.setMeaBegTime(startTime);
		g4SfDescQueryCond.setMeaEndTime(endTime);
		g4SfDescQueryCond.setFactory("ALL");

		Page newPage = page.copy();

		List<Map<String, String>> res = rnoLtePciService.querySfDataFromHbaseByPage(g4SfDescQueryCond, newPage);

		log.info("计算以后，page=" + newPage);
		int totalCnt = newPage.getTotalCnt();
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		newPage.setForcedStartIndex(-1);

		result.put("page", newPage);
		result.put("data", res);

		log.warn("退出方法querySfFilesByPageForAjaxAction. result=" + result);
		HttpTools.writeToClient(gson.toJson(result));
	}

	/**
	 * 
	 * @title 下载4g矩阵计算任务结果文件
	 * @return
	 * @author chao.xj
	 * @date 2015-4-29下午3:51:23
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String downloadNewLteInterMatrixFileAction() {
		log.debug("进入方法downloadNewLteInterMatrixFileAction。 下载4g矩阵计算结果文件， jobId=" + jobId);
		// 获取任务的信息。
		DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
		Connection connection = DataSourceConn.initInstance().getConnection();
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String sql = "select * from rno_4g_inter_martix_rec where job_id=" + jobId + " order by create_date desc";
		List<Map<String, Object>> pciTaksInfo = RnoHelper.commonQuery(stmt, sql);

		if (pciTaksInfo.size() <= 0) {
			error = "不存在该" + jobId + "任务信息";
			return "fail";
		}

		// 4g矩阵计算数据源文件的hdfs路径
		String resFilePath = pciTaksInfo.get(0).get("FILE_PATH").toString() + "/"
				+ pciTaksInfo.get(0).get("FILE_NAME").toString();

		// 下载的4g矩阵计算文件名称
		String dlFileName = jobId + "_4G矩阵结果表.csv";
		// 下载的4g矩阵计算文件全路径
		DateUtil dateUtil = new DateUtil();
		Calendar calendar = new GregorianCalendar();
		Date createDate = dateUtil.parseDateArbitrary(pciTaksInfo.get(0).get("CREATE_DATE").toString());
		calendar.setTime(createDate);
		String path = ServletActionContext.getServletContext().getRealPath("/op/rno/ana_result/");
		String dlFileRealdir = path + "/" + calendar.get(Calendar.YEAR) + "/" + (calendar.get(Calendar.MONTH) + 1)
				+ "/";
		String dlFileRealPath = dlFileRealdir + dlFileName;
		File realdir = new File(dlFileRealdir);
		if (!realdir.exists()) {
			realdir.mkdirs();
		}
		File realfile = new File(dlFileRealPath);
		if (!realfile.exists()) {
			try {
				realfile.createNewFile();
			} catch (IOException e) {
				error = "4g矩阵计算结果文件不存在！,文件路径：" + dlFileRealPath;
				log.error(error);
				return "fail";
			}
		}

		try {
			setFileName(new String(dlFileName.getBytes(), "iso-8859-1"));
		} catch (UnsupportedEncodingException e1) {
			error = "设置文件名失败";
			log.error(error);
			return "fail";
		}

		try {
			exportInputStream = new FileInputStream(realfile);
		} catch (FileNotFoundException e) {
			error = exportInputStream + "获取文件流异常,文件路径：" + dlFileRealPath;
			log.error(error);
			return "fail";
		}
		String result = "success";
		// 标题头
		String line = "小区标识," + "邻区标识," + "关联度," + "小区PCI," + "邻区PCI," + "小区频点," + "邻区频点," + "MR关联度," + "HO关联度,"
				+ "扫频关联度";

		FileSystem fs = null;
		Path hdfsPath = null;

		BufferedReader br = null;
		BufferedWriter bw = null;

		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dlFileRealPath), "GBK"));
			// 写入标题头
			bw.write(line);
			bw.newLine();
			fs = FileSystem.get(new Configuration());
			hdfsPath = new Path(URI.create(resFilePath));
			// 源文件不存在，不能提供下载
			if (!fs.exists(hdfsPath)) {
				error = "4g矩阵计算的源数据文件不存在！currentFileName=" + resFilePath;
				log.info(error);
				result = "error";
			} else {
				br = new BufferedReader(new InputStreamReader(fs.open(hdfsPath), "utf-8"));// 从字符输入流中读取文件中的内容,封装了一个new
				while ((line = br.readLine()) != null) {
					try {
						bw.write(line.replace("#", ","));
						bw.newLine();
					} catch (Exception e) {
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			error = "获取4g矩阵计算结果源文件中，读取数据出错";
			log.info(error);
			result = "error";
		} finally {
			try {
				if (br != null) {
					br.close();
				}
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		if ("error".equals(result)) {
			return "fail";
		}
		log.debug("离开方法downloadNewLteInterMatrixFileAction。 下载4g矩阵计算结果文件， result=" + result);
		return result;
	}
}
