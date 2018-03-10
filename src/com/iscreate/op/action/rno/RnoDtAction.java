package com.iscreate.op.action.rno;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.constant.RnoConstant;
import com.iscreate.op.pojo.rno.PlanConfig;
import com.iscreate.op.pojo.rno.RnoDtDescriptor;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoDtService;
import com.iscreate.op.service.rno.tool.HttpTools;
import com.opensymphony.xwork2.ActionContext;

@Controller
@Scope("prototype")
public class RnoDtAction extends RnoCommonAction {
	// -----------类静态-------------//
	private static final Logger logger = LoggerFactory.getLogger(RnoMapGroundSupportAction.class);
	// -------------注入-----------------
	@Autowired
	private RnoDtService rnoDtService;

	// 页面变量
	private long areaId;// 区域ID
	private long dtDescId;// DT描述ID

	private String configIds;
	private String areaName;

	public long getAreaId() {
		return areaId;
	}

	public void setAreaId(long areaId) {
		this.areaId = areaId;
	}

	public long getDtDescId() {
		return dtDescId;
	}

	public void setDtDescId(long dtDescId) {
		this.dtDescId = dtDescId;
	}

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public String getConfigIds() {
		return configIds;
	}

	public void setConfigIds(String configIds) {
		this.configIds = configIds;
	}

	// ---------------------chao.xj dt begin-------------------//

	/**
	 * 通过分页查询采样点数据
	 * 
	 * @author chao.xj
	 * @date 2013-12-2上午10:21:25
	 */
	@SuppressWarnings("unchecked")
	public void querySampleDataInAreaByPageForAjaxAction() {

		HttpServletRequest request = ServletActionContext.getRequest();
		HttpSession session = request.getSession();
		String mapId = (String) session.getAttribute("mapId");
		Map<String, Object> map = new HashMap<String, Object>();

		Map<String, Object> object = (Map<String, Object>) session.getAttribute("sampledescidandcount");

		logger.info("进入querySampleDataInAreaByPageForAjaxAction。dtDescId=" + dtDescId + ",object=" + object + ",mapId="
				+ mapId);
		// System.out.println("dtDescId="+dtDescId);
		// System.out.println("object="+object);
		int cnt = 0;
		if (object != null) {
			String descid = object.get("descids").toString();
			if (Long.parseLong(descid) != dtDescId) {
				cnt = rnoDtService.getRnoGsmDtSampleCount(dtDescId);
				map.put("descids", dtDescId);
				map.put("samplecount", cnt);
			} else {
				cnt = Integer.parseInt(object.get("samplecount").toString());
			}
			// cnt=Integer.parseInt(object.toString());
		} else {
			cnt = rnoDtService.getRnoGsmDtSampleCount(dtDescId);
			map.put("descids", dtDescId);
			map.put("samplecount", cnt);
			// System.out.println("cnt内部="+cnt);
			session.setAttribute("sampledescidandcount", map);
		}
		// System.out.println("cnt外部="+cnt);
		// System.out.println("page.getPageSize()="+page.getPageSize());
		if (page.getPageSize() <= 0) {
			page.setPageSize(25);
		}
		if (page.getCurrentPage() <= 0) {
			page.setCurrentPage(1);
		}
		List<Map<String, Object>> sampLists = rnoDtService.queryRnoGsmDtSampleListMapsByDescIdAndPage(dtDescId, page,
				mapId);
		logger.info("sampLists size = " + sampLists.size());
		String result1 = gson.toJson(sampLists);
		boolean hasMore = false;
		Page newPage = new Page();
		newPage.setCurrentPage(page.getCurrentPage());
		newPage.setForcedStartIndex(-1);
		newPage.setPageSize(page.getPageSize());
		newPage.setTotalCnt(cnt);// 总记录

		if (cnt % page.getPageSize() == 0) {
			newPage.setTotalPageCnt(cnt / page.getPageSize());// 总页码数
		} else {
			newPage.setTotalPageCnt(cnt / page.getPageSize() + 1);
		}
		// System.out.println("getTotalPageCnt="+newPage.getTotalPageCnt());
		if (newPage.getCurrentPage() != newPage.getTotalPageCnt()) {
			hasMore = true;
		}
		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'list':" + result1 + ",'hasMore':" + hasMore + "}";
		// System.out.println(result);
		logger.info("");
		HttpTools.writeToClient(result);
	}

	/**
	 * 初始化dt专题分析页面
	 * 
	 * @return
	 * @author chao.xj
	 *         2013-11-27 下午2:16:10
	 */
	public String initRnoDtThematicMapAnalysisPageAction() {

		initAreaList();
		return "success";
	}

	// ---------------------chao.xj dt end-------------------//
	// ---------------------gmh area begin-------------------//
	/**
	 * 初始化dt数据导入页面
	 * 
	 * @return
	 * @author brightming
	 *         2013-11-27 下午2:16:10
	 */
	public String initRnoDtImportAndLoadPageAction() {
		initAreaList();

		return "success";
	}

	/**
	 * 从列表中删除dt分析项
	 * 
	 * @author brightming
	 *         2013-11-27 下午2:43:19
	 */
	public void removeDtItemFromAnalysisListForAjaxAction() {
		logger.info("进入方法：removeDtItemFromAnalysisListForAjaxAction.configIds=" + configIds);
		int i = super.removeFromAnalysis(RnoConstant.SessionConstant.DT_CONFIG_ID, configIds);
		logger.info("删除数量：" + i);
		if (i == 0) {
			HttpTools.writeToClient("false");
		} else {
			HttpTools.writeToClient("true");
		}
	}

	/**
	 * 增加dt选项到列表
	 * 
	 * @author brightming
	 *         2013-11-27 下午2:47:13
	 */
	@SuppressWarnings("unchecked")
	public void addDtItemToListForAjaxAction() {
		logger.info("进入方法：addDtItemToListForAjaxAction。configIds=" + configIds + ",areaName=" + areaName);
		if (configIds != null) {
			String[] confids = configIds.split(",");
			if (confids.length == 0) {
				logger.error("未提供需要添加到分析列表的dt数据 id");
				HttpTools.writeToClient("false");
				return;
			}
			String account = (String) SessionService.getInstance().getValueByKey("userId");
			List<Area> tempAreas = super.rnoCommonService.getSpecialLevalAreaByAccount(account, "区/县");
			List<Long> areaIds = new ArrayList<Long>();
			for (Area ta : tempAreas) {
				areaIds.add(ta.getArea_id());
			}
			List<Long> cfids = new ArrayList<Long>();
			for (String d : confids) {
				try {
					cfids.add(Long.parseLong(d));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// 查询
			List<RnoDtDescriptor> rnoDtDescriptors = rnoCommonService.getObjectByIdsInArea(RnoDtDescriptor.class,
					"dtDescId", "areaId", cfids, areaIds);

			// 转换为planconfig
			if (rnoDtDescriptors != null && rnoDtDescriptors.size() > 0) {
				List<PlanConfig> planConfigs = (List<PlanConfig>) SessionService.getInstance().getValueByKey(
						RnoConstant.SessionConstant.DT_CONFIG_ID);

				if (planConfigs == null) {
					planConfigs = new ArrayList<PlanConfig>();
					ActionContext ctx = ActionContext.getContext();
					HttpServletRequest request = (HttpServletRequest) ctx.get(ServletActionContext.HTTP_REQUEST);
					HttpSession session = request.getSession();
					session.setAttribute(RnoConstant.SessionConstant.DT_CONFIG_ID, planConfigs);
				}
				PlanConfig pc = null;
				String startTime = "";
				for (RnoDtDescriptor rn : rnoDtDescriptors) {
					pc = new PlanConfig();
					pc.setAreaName(areaName);
					pc.setConfigId(rn.getDtDescId());
					pc.setSelected(false);
					pc.setTemp(false);
					pc.setType(RnoConstant.DataType.DT_DATA);
					pc.setObj(rn);
					try {
						startTime = sdf_full.format(rn.getTestDate());
					} catch (Exception e) {
						e.printStackTrace();
					}
					pc.setName(rn.getName());
					pc.setTitle(rn.getName());

					pc.setCollectTime(startTime);
					if (!planConfigs.contains(pc)) {
						planConfigs.add(pc);
					}
				}
			}

			HttpTools.writeToClient("true");
		} else {
			HttpTools.writeToClient("false");
		}
	}

	/**
	 * 获取所有已经加载的dt分析列表项
	 * 
	 * @author brightming
	 *         2013-11-27 下午3:10:02
	 */
	@SuppressWarnings("unchecked")
	public void getAllLoadedDtListForAjaxAction() {
		logger.info("进入方法：getAllLoadedDtListForAjaxAction。");
		List<PlanConfig> planConfigs = (List<PlanConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.DT_CONFIG_ID);

		String result = "[]";
		if (planConfigs != null) {
			result = gson.toJson(planConfigs);
		}
		logger.info("getAllLoadedDtListForAjaxAction输出：" + result);
		HttpTools.writeToClient(result);
	}

	/**
	 * 查询ncs
	 * 
	 * @author brightming 2013-11-14 下午7:33:34
	 */
	public void queryDtDescriptorByPageWithConditionForAjaxAction() {
		logger.info("进入：queryDtDescriptorByPageWithConditionForAjaxAction。attachParams=" + attachParams + ",page="
				+ page);

		if (page.getPageSize() <= 0) {
			page.setPageSize(25);
		}
		if (page.getCurrentPage() <= 0) {
			page.setCurrentPage(1);
		}
		int cnt = 0;
		if (page.getTotalCnt() < 0) {
			cnt = rnoDtService.getDtDescriptorTotalCnt(attachParams);
		}
		List<Map<String, Object>> rnoDescs = rnoDtService.queryDtDescriptorByPage(page, attachParams);
		String result1 = gson.toJson(rnoDescs);

		Page newPage = new Page();
		newPage.setCurrentPage(page.getCurrentPage());
		newPage.setForcedStartIndex(-1);
		newPage.setPageSize(page.getPageSize());
		newPage.setTotalCnt(cnt);

		if (cnt % page.getPageSize() == 0) {
			newPage.setTotalPageCnt(cnt / page.getPageSize());
		} else {
			newPage.setTotalPageCnt(cnt / page.getPageSize() + 1);
		}

		String pstr = gson.toJson(newPage);
		String result = "{'page':" + pstr + ",'list':" + result1 + "}";
		logger.info("退出queryDtDescriptorByPageWithConditionForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}
	// ---------------------gmh area end-------------------//

	// -----------------common area begin------------------//

	// --------common area end------------------//
}
