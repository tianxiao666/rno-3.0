package com.iscreate.op.action.rno;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.action.rno.model.Point;
import com.iscreate.op.pojo.rno.RnoNcell;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoResourceManagerService;
import com.iscreate.op.service.rno.RnoTrafficStaticsService;

@Controller
@Scope("prototype")
public class RnoMapGroundSupportAction extends RnoCommonAction {
	// -----------类静态-------------//
	private static final Logger log = LoggerFactory.getLogger(RnoMapGroundSupportAction.class);
	private static final Gson gson = new GsonBuilder().create();// 线程安全

	// ----ioc---//
	@Autowired
	private RnoResourceManagerService rnoResourceManagerService;
	@Autowired
	private RnoTrafficStaticsService rnoTrafficStaticsService;

	// ----页面变量---//
	private long areaId;
	private Page page;// 分页类
	private List<Area> zoneAreaLists;// 用户可见区/县的列表
	private List<Area> zoneProvinceLists;// 省份LIST
	private List<Area> zoneCountyLists;// 区/县LIST

	private int zoom;// 缩放级别
	private Point centerPoint;// 中心点
	private String label;// 小区名

	private List<Area> provinceAreas;
	private List<Area> cityAreas;
	private List<Area> countryAreas;

	public long getAreaId() {
		return areaId;
	}

	public void setAreaId(long areaId) {
		this.areaId = areaId;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public int getZoom() {
		return zoom;
	}

	public void setZoom(int zoom) {
		this.zoom = zoom;
	}

	public Point getCenterPoint() {
		return centerPoint;
	}

	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public List<Area> getZoneAreaLists() {
		return zoneAreaLists;
	}

	public void setZoneAreaLists(List<Area> zoneAreaLists) {
		this.zoneAreaLists = zoneAreaLists;
	}

	public List<Area> getZoneProvinceLists() {
		return zoneProvinceLists;
	}

	public List<Area> getZoneCountyLists() {
		return zoneCountyLists;
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

	/**
	 * 初始化区域导入内存页面
	 * 
	 * @return
	 * @author chao.xj 2013-10-16下午04:18:22
	 */
	public String initMapSpotDataImportPageAction() {
		initUserProvincesArea();
		initUserCountyArea();
		return "success";
	}

	/**
	 * 进入地图操作页面
	 * 
	 * @return
	 */
	public String initRnoMapSearchAction() {
		initAreaList();
		return "success";
	}

	/**
	 * 
	 * 获取用户的 县/区 区域
	 * 
	 * @author chao.xj
	 *         2013-10-15下午02:30:32
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
	 * @author chao.xj
	 *         2013-10-15上午09:59:13
	 */
	private void initUserProvincesArea() {
		// 获取用户所在区域信息
		String loginPerson = (String) SessionService.getInstance().getValueByKey("userId");
		zoneProvinceLists = this.rnoTrafficStaticsService.getProvincesInSpecLevelListByUserId(loginPerson, "省");

	}

	/**
	 * 搜索指定小区的邻区
	 */
	public void searchNcellByCellForAjaxAction() {
		log.info("进入方法：searchNcellByCellForAjaxAction.label=" + label);
		List<RnoNcell> ncells = rnoResourceManagerService.getNcellsByCell(label);
		String result = gson.toJson(ncells);
		log.info("退出方法：searchNcellByCellForAjaxAction。返回：" + result);
		writeToClient(result);
	}
}
