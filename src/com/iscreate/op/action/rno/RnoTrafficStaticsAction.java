package com.iscreate.op.action.rno;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.rubyeye.xmemcached.MemcachedClient;

import org.apache.struts2.ServletActionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.action.rno.model.GisCellQueryResult;
import com.iscreate.op.action.rno.model.Point;
import com.iscreate.op.action.rno.model.RnoStsQueryResult;
import com.iscreate.op.constant.RnoConstant;
import com.iscreate.op.pojo.rno.RnoStsResult;
import com.iscreate.op.pojo.rno.StsAnaItemDetail;
import com.iscreate.op.pojo.rno.StsConfig;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoCommonService;
import com.iscreate.op.service.rno.RnoResourceManagerService;
import com.iscreate.op.service.rno.RnoTrafficStaticsService;
import com.iscreate.op.service.rno.tool.HttpTools;
import com.opensymphony.xwork2.ActionContext;

@Controller
@Scope("prototype")
public class RnoTrafficStaticsAction extends RnoCommonAction {
	// -----------类静态-------------//
	private static final Logger logger = LoggerFactory.getLogger(RnoTrafficStaticsAction.class);
	private static final Gson gson = new GsonBuilder().create();// 线程安全

	// -------注入-------------//
	@Autowired
	private RnoResourceManagerService rnoResourceManagerService;
	@Autowired
	private RnoTrafficStaticsService rnoTrafficStaticsService;
	@Autowired
	private MemcachedClient memCached;

	// --------------小区查询 部分-------//
	private List<Area> zoneAreaLists;// 用户可见市区的列表

	// ---------页面变量----------------//
	private long loadedConfigId;// 加载的配置项id
	private Page page;
	private String selIds;// 选择的配置id，用逗号分隔
	private Point centerPoint;// 中心点
	private String stsCode;// 统计类型编码
	private int startIndex;// 起始编号
	private List<Area> zoneProvinceLists;// 省份LIST
	private List<Area> zoneCountyLists;// 区/县LIST
	private long areaId;
	private long provinceId;
	private long cityId;
	private long rptTemplateId;
	private String stsDescIds;
	// 话务性能查询
	private StsCondition queryCondition;// 查询条件vo
	private String searchType;// 查询指标类型（小区语音CELL_VIDEO，小区数据CELL_DATA,城市网络质量CITY_QUALITY）
	private boolean needLoad;// 是否需要加载到分析列表

	// 话务导入
	private List<Area> provinceAreas;
	private List<Area> cityAreas;
	private List<Area> countryAreas;
	private List<Map<String, Object>> bscEngNameLists;
	//
	private String cell;

	public List<Area> getZoneAreaLists() {
		return zoneAreaLists;
	}

	public void setZoneAreaLists(List<Area> zoneAreaLists) {
		this.zoneAreaLists = zoneAreaLists;
	}

	public long getLoadedConfigId() {
		return loadedConfigId;
	}

	public void setLoadedConfigId(long loadedConfigId) {
		this.loadedConfigId = loadedConfigId;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public String getSelIds() {
		return selIds;
	}

	public void setSelIds(String selIds) {
		this.selIds = selIds;
	}

	public Point getCenterPoint() {
		return centerPoint;
	}

	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	public String getStsCode() {
		return stsCode;
	}

	public void setStsCode(String stsCode) {
		this.stsCode = stsCode;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public StsCondition getQueryCondition() {
		return queryCondition;
	}

	public void setQueryCondition(StsCondition queryCondition) {
		this.queryCondition = queryCondition;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public long getAreaId() {
		return areaId;
	}

	public void setAreaId(long areaId) {
		this.areaId = areaId;
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

	public String getCell() {
		return cell;
	}

	public void setCell(String cell) {
		this.cell = cell;
	}

	public List<Area> getZoneProvinceLists() {
		return zoneProvinceLists;
	}

	public void setZoneProvinceLists(List<Area> zoneProvinceLists) {
		this.zoneProvinceLists = zoneProvinceLists;
	}

	public List<Area> getZoneCountyLists() {
		return zoneCountyLists;
	}

	public void setZoneCountyLists(List<Area> zoneCountyLists) {
		this.zoneCountyLists = zoneCountyLists;
	}

	public List<Area> getProvinceAreas() {
		return provinceAreas;
	}

	public void setProvinceAreas(List<Area> provinceAreas) {
		this.provinceAreas = provinceAreas;
	}

	public List<Area> getCityAreas() {
		return cityAreas;
	}

	public void setCityAreas(List<Area> cityAreas) {
		this.cityAreas = cityAreas;
	}

	public List<Area> getCountryAreas() {
		return countryAreas;
	}

	public void setCountryAreas(List<Area> countryAreas) {
		this.countryAreas = countryAreas;
	}

	public void setRnoCommonService(RnoCommonService rnoCommonService) {
		this.rnoCommonService = rnoCommonService;
	}

	public void setBscEngNameLists(List<Map<String, Object>> bscEngNameLists) {
		this.bscEngNameLists = bscEngNameLists;
	}

	public List<Map<String, Object>> getBscEngNameLists() {
		return bscEngNameLists;
	}

	public String getStsDescIds() {
		return stsDescIds;
	}

	public void setStsDescIds(String stsDescIds) {
		this.stsDescIds = stsDescIds;
	}

	public long getRptTemplateId() {
		return rptTemplateId;
	}

	public void setRptTemplateId(long rptTemplateId) {
		this.rptTemplateId = rptTemplateId;
	}

	/**
	 * 初始化加载话务性能页面
	 * 
	 * @return
	 * @author chao.xj 2013-10-16下午04:18:22
	 */
	public String initLoadTeleTrafficCapabilityPageAction() {
		initUserProvincesArea();
		initUserCountyArea();
		return "success";
	}

	/**
	 * 
	 * 通过传递省份Ajax加载市区信息
	 * 
	 * @author chao.xj 2013-10-15上午09:58:26
	 */
	public void getSpecifyCityAreaAction() {

		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		String result = this.rnoTrafficStaticsService.getCityAreasInSpecLevelListByUserId(loginPerson, "市", provinceId);

		try {
			response.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 通过传递市区Ajax加载区/县信息
	 * 
	 * @author chao.xj 2013-10-15上午09:56:49
	 */
	public void getSpecifyRegionAction() {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setContentType("text/json");
		response.setCharacterEncoding("utf-8");
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		String result = this.rnoTrafficStaticsService.getCityAreasInSpecLevelListByUserId(loginPerson, "区/县", cityId);

		try {
			response.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * 获取用户的 城市 区域 Sep 16, 2013 10:02:15 AM gmh
	 */
	private void initUserZoneArea() {
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		zoneAreaLists = this.rnoResourceManagerService.gisfindAreaInSpecLevelListByUserId(loginPerson, "市");

	}

	/**
	 * 
	 * 获取用户的 县/区 区域
	 * 
	 * @author chao.xj 2013-10-15下午02:30:32
	 */
	private void initUserCountyArea() {
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		zoneCountyLists = this.rnoTrafficStaticsService.getCountysInSpecLevelListByUserId(loginPerson, "区/县");

	}

	/**
	 * 
	 * 获取用户的 省 区域
	 * 
	 * @author chao.xj 2013-10-15上午09:59:13
	 */
	private void initUserProvincesArea() {
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		zoneProvinceLists = this.rnoTrafficStaticsService.getProvincesInSpecLevelListByUserId(loginPerson, "省");
	}

	/**
	 * 初始化话务专题统计页面
	 * 
	 * @return
	 * @author brightming 2013-10-11 下午4:20:59
	 */
	public String initRnoTrafficStaticsPageAction() {

		initAreaList();

		return "success";
	}

	/**
	 * 获取加载的分析列表
	 * 
	 * @author brightming 2013-10-11 下午2:41:03
	 */
	@SuppressWarnings("unchecked")
	public void getLoadedAnalysisListForAjaxAction() {
		logger.info("进入方法：getLoadedAnalysisListForAjaxAction。");
		List<StsConfig> stsConfigs = null;
		stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		if (stsConfigs == null) {
			stsConfigs = Collections.emptyList();
		}

		//
		String result = gson.toJson(stsConfigs);
		logger.info("退出方法：getLoadedAnalysisListForAjaxAction。输出：" + result);
		// System.out.println(result);
		writeToClient(result);
	}

	/**
	 * 重选挑选需要分析的列表
	 * 
	 * 传入参数：selIds 表示最新选择的
	 * 
	 * @author brightming 2013-10-12 下午2:52:26
	 */
	@SuppressWarnings("unchecked")
	public void reselectAnalysisListForAjaxAction() {
		logger.info("进入方法：reselectAnalysisListForAjaxAction。 selIds=" + selIds);

		List<StsConfig> stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);

		String[] ids = null;
		if (selIds != null) {
			ids = selIds.split(",");
		}

		if (stsConfigs != null && stsConfigs.size() > 0) {
			for (StsConfig sc : stsConfigs) {
				if (ids == null || ids.length == 0) {
					sc.setSelected(false);
				} else {
					sc.setSelected(false);
					for (String id : ids) {
						if ((sc.getConfigId() + "").equals(id)) {
							sc.setSelected(true);
							break;
						}
					}
				}
			}

			// 更新session的数据
			ActionContext ctx = ActionContext.getContext();
			HttpServletRequest request = (HttpServletRequest) ctx.get(ServletActionContext.HTTP_REQUEST);
			HttpSession session = request.getSession();
			session.setAttribute(RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID, stsConfigs);

		}
		writeToClient("ok");
	}

	/**
	 * 从加载列表中移除选定的加载项
	 * 
	 * @author brightming 2013-10-11 下午3:56:31
	 */
	@SuppressWarnings("unchecked")
	public void removeAnalysisItemFromLoadedListForAjaxAction() {
		logger.info("进入方法：removeAnalysisItemFromLoadedListForAjaxAction。loadedConfigId=" + loadedConfigId);
		List<StsConfig> stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		if (stsConfigs != null && stsConfigs.size() > 0) {
			boolean isFromQuery = false;
			int i = 0;
			for (; i < stsConfigs.size(); i++) {
				if (stsConfigs.get(i).getConfigId() == loadedConfigId) {
					break;
				}
			}
			if (i < stsConfigs.size()) {
				logger.info("移除指定的加载项。");
				isFromQuery = stsConfigs.get(i).isFromQuery();
				stsConfigs.remove(i);
				ActionContext ctx = ActionContext.getContext();
				HttpServletRequest request = (HttpServletRequest) ctx.get(ServletActionContext.HTTP_REQUEST);
				HttpSession session = request.getSession();
				session.setAttribute(RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID, stsConfigs);
				// 移除cache
				if (isFromQuery) {
					try {
						memCached.delete(RnoConstant.CacheConstant.CACHE_STATICS_QUERY_COND_PRE + loadedConfigId);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}
		}
		logger.info("退出方法：removeAnalysisItemFromLoadedListForAjaxAction。");
		writeToClient("success");
	}

	/**
	 * 获取选择的分析列表中的小区的gis信息
	 * 
	 * @author brightming 2013-10-11 下午5:15:43
	 */
	@SuppressWarnings("unchecked")
	public void getToBeAnaCellGisInfoFromSelAnaListForAjaxAction() {
		logger.info("进入getToBeAnaCellGisInfoFromSelAnaListForAjaxAction。");
		List<StsConfig> stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		List<StsConfig> selConfigs = new ArrayList<StsConfig>();
		if (stsConfigs != null && stsConfigs.size() > 0) {
			for (int i = 0; i < stsConfigs.size(); i++) {
				if (stsConfigs.get(i).isSelected()) {
					selConfigs.add(stsConfigs.get(i));
				}
			}
		}
		GisCellQueryResult gisCellResults = rnoTrafficStaticsService.getGisCellInfoFromSelectionList(selConfigs, page);

		int totalCnt = gisCellResults.getTotalCnt();

		Page newPage = new Page();
		newPage.setCurrentPage(page.getCurrentPage());
		newPage.setPageSize(page.getPageSize());
		newPage.setTotalCnt(totalCnt);
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		// if (page.getForcedStartIndex() >= 0) {
		// newPage.setForcedStartIndex(page.getForcedStartIndex()
		// + page.getPageSize());
		// }
		newPage.setForcedStartIndex(page.getForcedStartIndex());

		gisCellResults.setPage(newPage);

		String result = gson.toJson(gisCellResults);
		logger.info("退出getToBeAnaCellGisInfoFromSelAnaListForAjaxAction。");
		writeToClient(result);
	}

	/**
	 * 统计资源利用率
	 * 
	 * 输入： stsCode： radioresourcerate:无线资源利用率 accsucrate:接通率 droprate:掉话率
	 * dropnum:掉话数 handoversucrate:切换成功率
	 * 
	 * 
	 * startIndex:起始编号
	 * 
	 * 
	 * 输出： RnoStsQueryResult的json
	 * 
	 * @author brightming 2013-10-14 上午10:18:22
	 */
	@SuppressWarnings("unchecked")
	public void staticsResourceUtilizationRateForAjaxAction() {
		logger.info("进入方法：staticsRadioResourceUtilizationRate。stsCode=" + stsCode + ",startIndex=" + startIndex);
		List<StsConfig> stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		List<StsConfig> selConfigs = new ArrayList<StsConfig>();
		if (stsConfigs != null && stsConfigs.size() > 0) {
			for (int i = 0; i < stsConfigs.size(); i++) {
				if (stsConfigs.get(i).isSelected()) {
					selConfigs.add(stsConfigs.get(i));
				}
			}
		}
		int size = 1000;// 一次最多传送1000
		List<RnoStsResult> stsResults = rnoTrafficStaticsService.staticsResourceUtilizationRateInSelList(stsCode,
				selConfigs);

		logger.info("获取到stsCode=" + stsCode + "对应的统计数据：" + (stsResults == null ? stsResults : stsResults.size()));
		boolean hasmore = false;
		int toIndex = startIndex + size;
		int totalCnt = 0;

		RnoStsQueryResult queryResult = new RnoStsQueryResult();
		if (stsResults == null || stsResults.size() == 0) {

		} else {
			List<RnoStsResult> subList = null;
			totalCnt = stsResults.size();
			if (toIndex >= stsResults.size()) {
				toIndex = stsResults.size();
				subList = stsResults.subList(startIndex, toIndex);
			} else {
				subList = stsResults;
				hasmore = true;
			}
			queryResult.setRnoStsResults(subList);
		}
		queryResult.setHasMore(hasmore);
		queryResult.setTotalCnt(totalCnt);
		queryResult.setStartIndex(toIndex);// 告诉下一次的起点

		String result = gson.toJson(queryResult);
		logger.info("退出方法：staticsResourceUtilizationRateForAjaxAction。返回：" + result);
		writeToClient(result);
	}

	/**
	 * 
	 * @description: 初始化话务性能查询页面
	 * @author：yuan.yw
	 * @return
	 * @return String
	 * @date：Oct 10, 2013 10:19:26 AM
	 */
	public String initRnoStsManagerPageAction() {
		logger.info("Rno进入initRnoStsManagerPageAction.  初始化话务性能查询页面");
		this.initUserZoneArea();// 获取用户所在区/县区域列表
		this.initUserProvincesArea();
		this.initUserCountyArea();
		logger.info("成功退出initRnoStsManagerPageAction。初始化话务性能查询页面成功");
		return "success";

	}

	/**
	 * 
	 * @description: 分页查询话务数据
	 * @author：yuan.yw
	 * @return void
	 * @date：Oct 10, 2013 1:36:36 PM
	 */
	public void queryRnoStsListByPageAction() {
		logger.info("进入方法：queryRnoStsListByPageAction。分页查询话务数据 page=" + page + ",queryCondition=" + this.queryCondition);
		if (page.getCurrentPage() <= 0) {
			page.setCurrentPage(1);
		}
		boolean isAudio = false;
		if ("CELL_VIDEO".equals(searchType)) {
			isAudio = true;
		}
		List<Map<String, Object>> stsList = this.rnoTrafficStaticsService.queryStsByPage(this.page,
				this.queryCondition, this.searchType);
		int totalCnt = this.rnoTrafficStaticsService.getTotalStsCount(this.queryCondition, this.searchType, false,
				isAudio);
		Page newPage = new Page();
		newPage.setCurrentPage(page.getCurrentPage());
		newPage.setPageSize(page.getPageSize());
		newPage.setTotalCnt(totalCnt);
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		Gson gson = new GsonBuilder().create();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("stsList", stsList);
		resultMap.put("newPage", newPage);
		String result = gson.toJson(resultMap);
		logger.info("退出方法：queryRnoStsListByPageAction。 返回：" + result);
		writeToClient(result);
	}

	/**
	 * 统计符合某种要求的小区
	 * 
	 * @author brightming 2013-10-16 下午4:14:55
	 */
	@SuppressWarnings("unchecked")
	public void staticsSpecialCellForAjaxAction() {
		logger.info("进入方法：staticsSpecialCellForAjaxAction。stsCode=" + stsCode + ",startIndex=" + startIndex);

		List<StsConfig> stsConfigs = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		List<StsConfig> selConfigs = new ArrayList<StsConfig>();
		if (stsConfigs != null && stsConfigs.size() > 0) {
			for (int i = 0; i < stsConfigs.size(); i++) {
				if (stsConfigs.get(i).isSelected()) {
					selConfigs.add(stsConfigs.get(i));
				}
			}
		}
		int size = 1000;// 一次最多传送1000
		List<RnoStsResult> stsResults = rnoTrafficStaticsService.staticsSpecialCellInSelList(stsCode, selConfigs);

		logger.info("获取到cellType=" + stsCode + "对应的统计数据：" + (stsResults == null ? stsResults : stsResults.size()));
		boolean hasmore = false;
		int toIndex = startIndex + size;
		int totalCnt = 0;

		RnoStsQueryResult queryResult = new RnoStsQueryResult();
		if (stsResults == null || stsResults.size() == 0) {

		} else {
			List<RnoStsResult> subList = null;
			totalCnt = stsResults.size();
			if (toIndex >= stsResults.size()) {
				toIndex = stsResults.size();
				subList = stsResults.subList(startIndex, toIndex);
			} else {
				subList = stsResults;
				hasmore = true;
			}
			queryResult.setRnoStsResults(subList);
		}
		queryResult.setHasMore(hasmore);
		queryResult.setTotalCnt(totalCnt);
		queryResult.setStartIndex(toIndex);// 告诉下一次的起点

		String result = gson.toJson(queryResult);
		logger.info("退出方法：staticsSpecialCellForAjaxAction。返回：" + result);
		writeToClient(result);
	}

	/**
	 * 初始化话务统计性能数据导入页面
	 * 
	 * @return
	 * @author brightming 2013-10-17 上午11:05:51
	 */
	public String initTrafficStaticsImportPageAction() {
		bscEngNameLists = rnoTrafficStaticsService.getRnoBscEngName();
		initAreaList();

		return "success";
	}

	/**
	 * 初始化小区加载配置数据导入页面
	 * 
	 * @return
	 * @author chao.xj
	 * @date 2013-11-5上午10:10:44
	 */
	public String initCellLoadConfigureImportPageAction() {

		initAreaList();

		return "success";
	}

	/**
	 * 初始化忙小区邻区分析页面
	 * 
	 * @author brightming 2013-10-19 下午4:50:16
	 */
	public String initHeavyLoadCellAnalyPageAction() {
		initAreaList();
		return "success";
	}

	/**
	 * 获取指定区域的忙小区
	 * 
	 * 输入 ：areaId
	 * 
	 * @author brightming 2013-10-21 下午2:27:21
	 */
	public void getHeavyLoadCellWithAreaForAjaxAction() {
		logger.info("进入方法：getHeavyLoadCellWithAreaForAjaxAction。areaId=" + areaId);
		List<RnoStsResult> cells = rnoTrafficStaticsService.staticsHeavyLoadCellInArea(areaId);
		String result = gson.toJson(cells);
		writeToClient(result);
		logger.info("离开getHeavyLoadCellWithAreaForAjaxAction。输出忙小区数量：{}", cells == null ? 0 : cells.size());
	}

	/**
	 * 获取指定忙小区的邻区话务情况
	 * 
	 * @author brightming 2013-10-21 下午2:39:51
	 */
	public void getNcellInfoOfHeavyLoadCellForAjaxAction() {
		logger.info("进入getNcellInfoOfHeavyLoadCellForAjaxAction。cell=" + cell + ",areaId=" + areaId);
		List<RnoStsResult> sts = rnoTrafficStaticsService.staticsSpecialCellsNcellStsInfo(cell, areaId);
		String result = gson.toJson(sts);
		writeToClient(result);
		logger.info("离开getHeavyLoadCellWithAreaForAjaxAction。输出邻小区统计数量：{}", sts == null ? 0 : sts.size());
	}

	/**
	 * 查询统计阶段数据，并加载到分析列表
	 * 
	 * @author brightming 2013-10-24 下午3:05:01
	 * 
	 *         searchType:GSMAUDIOTRAFFICSTATICSFILE,GSMDATATRAFFICSTATICSFILE
	 */
	@SuppressWarnings("unchecked")
	public void queryAndLoadStsPeriodDataForAjaxAction() {
		logger.info("进入方法：queryAndLoadStsPeriodDataForAjaxAction。 searchType=" + searchType + ",queryCondition="
				+ queryCondition + ",needLoad=" + needLoad + ",page=" + page);

		List<StsConfig> stsconfigs = rnoTrafficStaticsService.queryStsPeriodByQueryCondition(page, searchType,
				queryCondition);

		List<StsConfig> exists = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);

		if (exists == null) {
			exists = new ArrayList<StsConfig>();
			ActionContext ctx = ActionContext.getContext();
			HttpServletRequest request = (HttpServletRequest) ctx.get(ServletActionContext.HTTP_REQUEST);
			HttpSession session = request.getSession();
			session.setAttribute(RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID, exists);
		}
		for (StsConfig sc : stsconfigs) {
			if (!exists.contains(sc)) {
				exists.add(sc);
			}
		}
		String result = gson.toJson(stsconfigs);
		writeToClient(result);
	}

	private InputStream excelStream;// 导出文件输入流
	private String fileName; // 导出文件名

	public InputStream getExcelStream() {
		return excelStream;
	}

	public void setExcelStream(InputStream excelStream) {
		this.excelStream = excelStream;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * 
	 * @description: 导出查询话务数据
	 * @author：yuan.yw
	 * @return
	 * @return String
	 * @date：Aug 2, 2013 5:36:33 PM
	 */
	public String exportRnoStsListAction() {
		HttpServletResponse response = ServletActionContext.getResponse();
		response.setCharacterEncoding("GBK");
		boolean isAudio = false;
		if ("CELL_VIDEO".equals(searchType)) {
			isAudio = true;
		}
		// excelStream =this.rnoTrafficStaticsService.exportQueryRnoStsList(queryCondition,isAudio);
		excelStream = this.rnoTrafficStaticsService.exportQueryRnoStsList(queryCondition, isAudio, rptTemplateId);
		try {
			this.fileName = new String(("话务查询结果导出").getBytes("GBK"), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			this.fileName = "result";
		}
		this.fileName = this.fileName + ".xlsx";
		return "success";
	}

	/**
	 * 刷新按钮查询统计阶段数据从分析列表
	 * 
	 * @author chao.xj
	 * @date 2013-12-12上午11:00:59
	 */
	@SuppressWarnings("unchecked")
	public void refreshQueryAndLoadStsPeriodDataForAjaxAction() {
		logger.info("进入方法：refreshQueryAndLoadStsPeriodDataForAjaxAction。");

		List<StsConfig> exists = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		String result = "";
		if (exists == null) {
			exists = Collections.emptyList();
			result = gson.toJson(exists);
		} else {
			result = gson.toJson(exists);
		}
		logger.info("result=" + result);
		// System.out.println(result);
		writeToClient(result);
	}

	/**
	 * 改造：分页查询话统指标描述数据
	 * 
	 * @author chao.xj
	 * @date 2013-11-16下午10:09:33
	 */
	public void queryCellAudioOrDataDescListByPageAction() {
		logger.info("进入方法：queryCellAudioOrDataDescListByPageAction。分页查询小区配置描述数据 page=" + page + " areaId=" + areaId);
		// System.out.println(page.getCurrentPage()+" "+areaId+" "+schemeName);
		if (page.getCurrentPage() <= 0) {
			page.setCurrentPage(1);
		}
		List<Map<String, Object>> cellAudioOrDataDesclists = rnoTrafficStaticsService.queryCellAudioOrDataStsByPage(
				page, queryCondition, searchType);
		/*
		 * for (int i = 0; i < lists.size(); i++) { System.out.println(i); Map
		 * map=lists.get(i); System.out.println(map.get("AREA_ID")); }
		 */
		int totalCnt = this.rnoTrafficStaticsService.getTotalCellAudioOrDataCount(searchType, queryCondition);
		Page newPage = new Page();
		newPage.setCurrentPage(page.getCurrentPage());
		newPage.setPageSize(page.getPageSize());
		newPage.setTotalCnt(totalCnt);
		newPage.setTotalPageCnt(totalCnt / newPage.getPageSize() + (totalCnt % newPage.getPageSize() == 0 ? 0 : 1));
		Gson gson = new GsonBuilder().create();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("cellaudioordatadescLists", cellAudioOrDataDesclists);
		resultMap.put("newPage", newPage);
		String result = gson.toJson(resultMap);
		logger.info("退出方法：queryCellAudioOrDataDescListByPageAction。 返回：" + result);
		HttpTools.writeToClient(result);
		// return "success";
	}

	/**
	 * 改造：将选中的话统指标描述加入分析列表
	 * 
	 * @return
	 * @author chao.xj
	 * @date 2013-11-6下午06:51:44
	 */
	@SuppressWarnings("unchecked")
	public void addCellAudioOrDataDescToAnalysisListForAjaxAction() {
		// 输入configId
		// System.out.println(configIds);
		HttpServletResponse response = ServletActionContext.getResponse();
		HttpServletRequest request = ServletActionContext.getRequest();
		response.setContentType("text/plain");
		response.setCharacterEncoding("utf-8");

		HttpSession session = request.getSession();
		List<Map<String, Object>> configlists = rnoTrafficStaticsService.getCellAudioOrDataDescByConfigIds(stsDescIds);
		// System.out.println(configlists.size());
		String result = "";

		for (int i = 0; i < configlists.size(); i++) {
			StsConfig stsConfig = new StsConfig();
			StsAnaItemDetail stsAnaItemDetail = new StsAnaItemDetail();
			String CELL_DESCRIPTOR_ID = configlists.get(i).get("STS_DESC_ID").toString();
			String NAME = configlists.get(i).get("NET_TYPE").toString();
			// System.out.println("NAME:"+NAME);
			String STS_DATE = configlists.get(i).get("STS_DATE").toString();
			String AREA_ID = configlists.get(i).get("AREA_ID").toString();
			String AREANAME = configlists.get(i).get("AREANAME").toString();
			String SPEC_TYPE = configlists.get(i).get("SPEC_TYPE").toString();
			String STS_PERIOD = configlists.get(i).get("STS_PERIOD").toString();
			// System.out.println(CELL_DESCRIPTOR_ID);
			// System.out.println(CELL_DESCRIPTOR_ID+" "+NAME+" "+CREATE_TIME+" "+AREA_ID+" "+AREANAME);

			stsConfig.setConfigId(Long.parseLong(CELL_DESCRIPTOR_ID));
			// System.out.println(stsAnaItemDetail);
			stsAnaItemDetail.setAreaId(Long.parseLong(AREA_ID));
			stsAnaItemDetail.setAreaName(AREANAME);
			stsAnaItemDetail.setStsDate(STS_DATE);
			stsAnaItemDetail.setPeriodType(STS_PERIOD);
			// System.out.println(SPEC_TYPE.equals("CELLAUDIOINDEX")?"小区语音业务指标":"小区数据业务指标");
			// System.out.println(SPEC_TYPE=="CELLAUDIOINDEX");
			// System.out.println(SPEC_TYPE);
			stsAnaItemDetail.setStsType(NAME + (SPEC_TYPE.equals("CELLAUDIOINDEX") ? "小区语音业务指标" : "小区数据业务指标"));

			stsConfig.setStsAnaItemDetail(stsAnaItemDetail);
			List<StsConfig> lists = (List<StsConfig>) session
					.getAttribute(RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
			if (lists != null) {
				if (!lists.contains(stsConfig)) {
					lists.add(stsConfig);
				}
				if (i == configlists.size() - 1) {
					result = gson.toJson(lists);
				}
			} else {
				List<StsConfig> planConfiglists = new ArrayList<StsConfig>();
				planConfiglists.add(stsConfig);
				// PLAN_LOAD_CELL_CONFIG_ID
				session.setAttribute(RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID, planConfiglists);
				result = gson.toJson(planConfiglists);
			}
		}

		try {
			response.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 从加载列表中删除话务性能数据
	 * 
	 * @author chao.xj
	 * @date 2013-12-18上午11:57:33
	 */
	@SuppressWarnings("unchecked")
	public void removeTrafficStaticItemFromLoadedListForAjaxAction() {
		logger.info("进入方法：removeTrafficStaticItemFromLoadedListForAjaxAction。configIds=" + stsDescIds);
		List<StsConfig> stsconfigList = (List<StsConfig>) SessionService.getInstance().getValueByKey(
				RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		removeFromAnalysisInner(stsconfigList, stsDescIds, RnoConstant.SessionConstant.STS_LOAD_CONFIG_ID);
		HttpTools.writeToClient("true");
	}

	/**
	 * 从分析列表中删除指定的若干项
	 * 
	 * @param stsconfigList
	 * @param configIds
	 * @param code
	 * @return
	 * @author chao.xj
	 * @date 2013-12-18下午12:14:03
	 */
	private int removeFromAnalysisInner(List<StsConfig> stsconfigList, String configIds, String code) {
		if (configIds == null) {
			logger.error("未指明需要删除的 id");
			HttpTools.writeToClient("false");
			return 0;
		}
		String[] ids = configIds.split(",");
		if (ids.length == 0) {
			logger.error("未指明需要删除的 id");
			HttpTools.writeToClient("false");
			return 0;
		}
		int cnt = 0;
		if (stsconfigList != null && stsconfigList.size() > 0) {
			int i = 0;
			String inid = "";
			List<StsConfig> del = new ArrayList<StsConfig>();
			for (String id : ids) {
				for (i = 0; i < stsconfigList.size(); i++) {
					inid = stsconfigList.get(i).getConfigId() + "";
					if (id.equals(inid)) {
						del.add(stsconfigList.get(i));
						break;
					}
				}
			}
			if (del.size() > 0) {
				cnt = del.size();
				logger.info("移除指定的加载项。");
				for (int j = 0; j < del.size(); j++) {
					stsconfigList.remove(del.get(j));
				}
				ActionContext ctx = ActionContext.getContext();
				HttpServletRequest request = (HttpServletRequest) ctx.get(ServletActionContext.HTTP_REQUEST);
				HttpSession session = request.getSession();
				session.setAttribute(code, stsconfigList);
			}
		}
		logger.info("退出方法：removeFromAnalysisInner。");
		return cnt;
	}

	/**
	 * 
	 * 通过小区名获取邻区:在忙小区的邻区分析模块下通过右键查询
	 * 
	 * @author chao.xj
	 * @date 2013-12-26下午03:36:56
	 */
	public void getNcellforNcellAnalysisOfBusyCellByCellForAjaxAction() {
		logger.info("进入getNcellforNcellAnalysisOfBusyCellByCellForAjaxAction cell:" + cell);
		List<Map<String, Object>> ncelList = rnoTrafficStaticsService.queryNcellByCell(cell);
		String result = "";
		if (ncelList == null) {
			result = Collections.EMPTY_LIST.toString();
		}
		result = gson.toJson(ncelList);
		logger.info("退出方法:getNcellforNcellAnalysisOfBusyCellByCellForAjaxAction result:" + result);
		HttpTools.writeToClient(result);
	}

}
