package com.iscreate.op.action.rno;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.dom4j.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Controller;

import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.action.rno.model.Point;
import com.iscreate.op.pojo.system.SysOrgUser;
import com.iscreate.op.pojo.system.SysRole;
import com.iscreate.op.service.publicinterface.SessionService;
import com.iscreate.op.service.rno.RnoCommonService;
import com.iscreate.op.service.system.SysAccountService;
import com.iscreate.op.service.system.SysOrgUserService;
import com.iscreate.op.service.system.SysPermissionService;
import com.iscreate.op.service.system.SysSuperAdminService;
import com.iscreate.op.service.system.SysUserRelaPermissionService;
import com.iscreate.plat.login.action.LoginAction;
import com.iscreate.plat.login.constant.UserInfo;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;
import com.iscreate.plat.tools.xmlhelper.XmlService;

@Controller
@Scope("prototype")
public class RnoLoginAction extends LoginAction {
	private static final Logger logger = LoggerFactory.getLogger(RnoLoginAction.class);
	// --------注入--------------------//
	@Autowired
	private SysOrgUserService sysOrgUserService;
	@Autowired
	protected RnoCommonService rnoCommonService;
	@Autowired
	private XmlService xmlService;
	// --------父类注入--------------------//
	@Autowired
	public void setSysSuperAdminService(SysSuperAdminService sysSuperAdminService) {
		super.setSysSuperAdminService(sysSuperAdminService);
	}

	@Autowired
	public void setSysUserRelaPermissionService(SysUserRelaPermissionService sysUserRelaPermissionService) {
		super.setSysUserRelaPermissionService(sysUserRelaPermissionService);
	}

	@Autowired
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		super.setHibernateTemplate(hibernateTemplate);
	}

	@Autowired
	public void setSysAccountService(SysAccountService sysAccountService) {
		super.setSysAccountService(sysAccountService);
	}

	@Autowired
	public void setSysPermissionService(SysPermissionService sysPermissionService) {
		super.setSysPermissionService(sysPermissionService);
	}

	private SysOrgUser sysOrgUser;

	private List<Map<String, Object>> permissionModuleList;

	private List<Map<String, Object>> menuList;

	private String jump;

	private boolean flag;
	// 区域
	private List<Area> provinceAreas;
	private List<Area> cityAreas;
	private List<Area> countryAreas;
	private Point centerPoint;// 中心点
	private String areaName;

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

	public Point getCenterPoint() {
		return centerPoint;
	}

	public void setCenterPoint(Point centerPoint) {
		this.centerPoint = centerPoint;
	}

	public String getAreaName() {
		return areaName;
	}

	public void setAreaName(String areaName) {
		this.areaName = areaName;
	}

	public List<Map<String, Object>> getMenuList() {
		return menuList;
	}

	public void setMenuList(List<Map<String, Object>> menuList) {
		this.menuList = menuList;
	}

	public SysOrgUser getSysOrgUser() {
		return sysOrgUser;
	}

	public void setSysOrgUser(SysOrgUser sysOrgUser) {
		this.sysOrgUser = sysOrgUser;
	}

	public List<Map<String, Object>> getPermissionModuleList() {
		return permissionModuleList;
	}

	public void setPermissionModuleList(List<Map<String, Object>> permissionModuleList) {
		this.permissionModuleList = permissionModuleList;
	}

	public String getJump() {
		return jump;
	}

	public void setJump(String jump) {
		this.jump = jump;
	}

	public boolean isFlag() {
		return flag;
	}

	public void setFlag(boolean flag) {
		this.flag = flag;
	}

	public String getGoingToURL() {
		return super.getGoingToURL();
	}

	public void setGoingToURL(String goingToURL) {
		super.setGoingToURL(goingToURL);
	}

	/**
	 * 登录
	 * 
	 * @return
	 * @author brightming
	 *         2013-12-11 下午6:49:52
	 */
	public String rnoUserLogin() {
		DataSourceContextHolder.setDataSourceType(DataSourceConst.authDs);
		super.prepareLoginPageParams();
		HttpServletRequest request = ServletActionContext.getRequest();
		String version = null;
		String projectName = null;
		String corporationName = null;
		String yearInterval = null;
		try {
			version = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/version");
			projectName = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/name");
			corporationName = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/corporation");
			yearInterval = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/year");
			// log.info("版本："+projectName+" "+version);
		} catch (DocumentException e) {
			logger.error("获取版本信息失败");
		}
		request.getSession().setAttribute("version", version);
		request.getSession().setAttribute("projectName", projectName);
		request.getSession().setAttribute("corporationName", corporationName);
		request.getSession().setAttribute("yearInterval", yearInterval);
		return "success";
	}

	public String rnoAuthenticate() {
		String result = super.authenticate();
		if ("success".equals(result)) {
			// 执行用户没登录系统前请求的URL回显
			if (jump != null && !"".equals(jump) && !"null".equalsIgnoreCase(jump)) {
				try {
					jump = URLDecoder.decode(jump, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				setGoingToURL(jump);
			} else {
				// 没有jump, 跳至门户首页
				setGoingToURL("/op/rno/rnoUserIndexAction.action");
			}
		}
		return result;
	}

	public String rnoUserIndexAction() {
		logger.info("进入rnoUserIndexAction方法");
		// 从session获取user
		HttpServletRequest request = ServletActionContext.getRequest();
		String userId = (String) request.getSession().getAttribute(UserInfo.USERID);
		long orgUserId = Long.parseLong(request.getSession()
				.getAttribute(com.iscreate.plat.login.constant.UserInfo.ORG_USER_ID).toString());

		// 通过账号获取用户可以查询的区域
		String account = (String) SessionService.getInstance().getValueByKey("userId");
		// 设置数据源
		DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
		// 首先通过获取默认城市，然后置顶
		long cfgCityId = rnoCommonService.getUserConfigAreaId(account);
		long cfgProvinceId = 0;
		if (cfgCityId != -1) {
			cfgProvinceId = rnoCommonService.getParentIdByCityId(cfgCityId);
		}
		// 再恢复数据源
		DataSourceContextHolder.setDataSourceType(DataSourceConst.authDs);

		provinceAreas = rnoCommonService.getSpecialLevalAreaByAccount(account, "省");
		// 如果该帐户没有设定过默认区域，哪就默认第一个省份为默认区域
		if (cfgProvinceId == 0) {
			cfgProvinceId = provinceAreas.get(0).getArea_id();
		}
		// 先保存第一个省的对象信息:为了使默认的省排在首位
		Area tmp = provinceAreas.get(0);
		for (int i = 0; i < provinceAreas.size(); i++) {
			// 替换次序
			if (provinceAreas.get(i).getArea_id() == cfgProvinceId) {
				provinceAreas.set(0, provinceAreas.get(i));
				provinceAreas.set(i, tmp);
				break;
			}
		}
		cityAreas = new ArrayList<Area>();
		countryAreas = new ArrayList<Area>();
		if (provinceAreas != null && provinceAreas.size() > 0) {
			// 通过缺省的省获取城市
			cityAreas = rnoCommonService.getSpecialSubAreasByAccountAndParentArea(account, cfgProvinceId, "市");
			// 设置数据源
			DataSourceContextHolder.setDataSourceType(DataSourceConst.rnoDS);
			Area areaTemp = new Area();
			for (int i = 0; i < cityAreas.size(); i++) {
				if (cityAreas.get(i).getArea_id() == cfgCityId) {
					areaTemp = cityAreas.get(0);
					cityAreas.set(0, cityAreas.get(i));
					cityAreas.set(i, areaTemp);
				}
			}
			if (cityAreas != null && cityAreas.size() > 0) {
				centerPoint = new Point();
				countryAreas = rnoCommonService.getSpecialSubAreasByAccountAndParentArea(account, cityAreas.get(0)
						.getArea_id(), "区/县");
				if (countryAreas != null && countryAreas.size() > 0) {
					areaName = countryAreas.get(0).getName();
					centerPoint.setLng(countryAreas.get(0).getLongitude());
					centerPoint.setLat(countryAreas.get(0).getLatitude());
				} else {
					centerPoint.setLng(cityAreas.get(0).getLongitude());
					centerPoint.setLat(cityAreas.get(0).getLatitude());
				}
			}
		}

		// 恢复数据源
		DataSourceContextHolder.setDataSourceType(DataSourceConst.authDs);

		// 根据登录人账号获取用户信息
		this.sysOrgUser = this.sysOrgUserService.getSysOrgUserByAccount(userId);
		// 判断登录人是否只有拥有系统管理员身份 true有 false没有
		flag = false;
		List<SysRole> userRolesByAccount = this.sysOrgUserService.getUserRolesByAccount(userId);
		if (userRolesByAccount != null) {
			for (SysRole s : userRolesByAccount) {
				// 判断角色是否为系统管理员
				if (s.getCode().equals("systemManager")) {
					flag = true;
					break;
				} else {
					flag = false;
				}
			}
		} else {
			flag = false;
		}
		this.permissionModuleList = super.getSysUserRelaPermissionService().getFirstPermissionListByUserId(orgUserId,
				"RNO", "RNO_MenuResource", false);
		// 只要网优工具的
		String code = "";
		String effectflag = "";
		List<Map<String, Object>> dels = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> one : permissionModuleList) {
			code = one.get("CODE") + "";
			effectflag = one.get("FLAG") + "";

			if ((!code.startsWith("RNO") && !code.startsWith("rno")) || Integer.parseInt(effectflag) == 0) {
				dels.add(one);
			}
		}
		if (dels.size() > 0) {
			for (Map<String, Object> one : dels) {
				permissionModuleList.remove(one);
			}
		}
		long permissionId = ((BigDecimal) permissionModuleList.get(0).get("PERMISSION_ID")).longValue();
		try {
			menuList = getChildrenMenuListByParentIdAndType(permissionId);
		} catch (Exception e) {
			logger.error("查找导航菜单失败");
			e.printStackTrace();
		}
		// log.info("导航菜单: "+menuList);
		logger.info("执行rnoUserIndexAction方法成功，实现了”登录我的首页");
		logger.info("退出rnoUserIndexAction方法,返回success");
		return "success";
	}

	/**
	 * 根据类型标识返回其下所有子节点
	 * 
	 * @throws IOException
	 * @throws IOException
	 */

	public List<Map<String, Object>> getChildrenMenuListByParentIdAndType(long permissionId) throws IOException {
		long orgUserId = Long.parseLong(SessionService.getInstance()
				.getValueByKey(com.iscreate.plat.login.constant.UserInfo.ORG_USER_ID).toString());
		// HttpServletRequest request = ServletActionContext.getRequest();
		List<Map<String, Object>> permissionByParentIdccount = this.getSysUserRelaPermissionService()
				.getSysPermissionListByOrgUserIdAndParentId(orgUserId, permissionId);
		Map<String, List<Map<String, Object>>> map = new TreeMap<String, List<Map<String, Object>>>();
		// 一级菜单标题缓存
		List<String> titleStrings = null;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> list1 = new ArrayList<Map<String, Object>>();
		if (permissionByParentIdccount != null && permissionByParentIdccount.size() > 0) {
			titleStrings = new ArrayList<String>();
			// int i = 0;
			int j = 0;
			for (Map<String, Object> m : permissionByParentIdccount) {
				if (m != null && m.get("PARENT_ID") != null && !m.get("PARENT_ID").toString().equals("0")) {
					if (j++ == 0) {
						m.put("isDefault", "1");
						// i++;
					}
					if (m.get("TITLE") == null) {
						continue;
					} else if (map.containsKey(m.get("TITLE").toString())) {
						if (m.get("PARENT_ID").toString().equals(permissionId + "")) {
							list = map.get(m.get("TITLE"));
							String url = "../" + m.get("URL");
							if (m.get("PARAMETER") != null && !m.get("PARAMETER").equals("")) {
								url = url + "?" + m.get("PARAMETER");
							}
							m.put("href", url);
							m.put("cls", "forum");
							m.put("expanded", true);
							m.put("iconCls", "icon-forum");
							m.put("hrefTarget", "main_right");
							m.put("TEXT", m.get("NAME"));
							list.add(m);
						} else {
							String url = "../" + m.get("URL");
							if (m.get("PARAMETER") != null && !m.get("PARAMETER").equals("")) {
								url = url + "?" + m.get("PARAMETER");
							}
							m.put("href", url);
							m.put("url", m.get("URL"));
							m.put("TITLE", m.get("TITLE"));
							m.put("TEXT", m.get("NAME"));
							list1.add(m);
						}
					} else {
						if (m.get("PARENT_ID").toString().equals(permissionId + "")) {
							titleStrings.add(m.get("TITLE").toString());
							String url = "../" + m.get("URL");
							if (m.get("PARAMETER") != null && !m.get("PARAMETER").equals("")) {
								url = url + "?" + m.get("PARAMETER");
							}
							m.put("href", url);
							m.put("cls", "forum");
							m.put("iconCls", "icon-forum");
							m.put("expanded", true);
							m.put("hrefTarget", "main_right");
							m.put("TITLE", m.get("TITLE"));
							m.put("TEXT", m.get("NAME"));
							list.add(m);
							map.put(m.get("TITLE").toString(), list);
						} else if (!m.get("PARENT_ID").toString().equals("0")) {
							String url = "../" + m.get("URL");
							if (m.get("PARAMETER") != null && !m.get("PARAMETER").equals("")) {
								url = url + "?" + m.get("PARAMETER");
							}
							m.put("href", url);
							m.put("url", m.get("URL"));
							m.put("TITLE", m.get("TITLE"));
							m.put("TEXT", m.get("NAME"));
							list1.add(m);
						}
					}
				} else {
					continue;
				}
			}
		}

		Map<String, List<Map<String, Object>>> map1 = new TreeMap<String, List<Map<String, Object>>>();
		List<String> nameListLel2 = new ArrayList<String>();
		for (Map<String, Object> lis : list) {
			nameListLel2.add(lis.get("NAME").toString());
		}

		Map<String, List<Map<String, Object>>> tempMap3 = new HashMap<String, List<Map<String, Object>>>();
		for (String l : titleStrings) {
			tempMap3.put(l, new ArrayList<Map<String, Object>>());
		}

		for (Map<String, Object> lis : list) {

			// List<Map<String, Object>> tempList = new ArrayList<Map<String, Object>>();
			Map<String, List<Map<String, Object>>> tempMap = new HashMap<String, List<Map<String, Object>>>();
			List<Map<String, Object>> tempList1 = new ArrayList<Map<String, Object>>();
			tempMap.put(lis.get("NAME").toString(), tempList1);
			for (Map<String, Object> lis1 : list1) {
				if (lis.get("NAME").toString().equals(lis1.get("TITLE").toString())) {
					Map<String, Object> tempMap1 = new HashMap<String, Object>();
					tempMap1.put("TEXT", lis1.get("NAME"));
					tempMap1.put("TITLE", lis1.get("TITLE"));
					tempMap1.put("URL", lis1.get("url"));
					tempMap.get(lis.get("NAME").toString()).add(tempMap1);
					if (tempMap.get(lis.get("NAME").toString()) != null) {
						lis.put("leaf", true);
					} else {
						lis.put("leaf", false);
					}
				}
			}
			lis.put("grandchild", tempMap.get(lis.get("NAME").toString()));

			tempMap3.get(lis.get("TITLE").toString()).add(lis);
		}

		for (String l : titleStrings) {
			map1.put(l, tempMap3.get(l));
		}

		List<Map<String, Object>> rList = new ArrayList<Map<String, Object>>();
		if (map != null) {
			int i = 0;
			for (String s : titleStrings) {
				if (s != null && !s.equals("")) {
					Map<String, Object> ma = new HashMap<String, Object>();
					ma.put("text", s);
					ma.put("leaf", false);
					ma.put("cls", "forum-ct");
					ma.put("iconCls", "forum-parent");
					ma.put("expanded", true);
					ma.put("id", i);
					ma.put("children", map1.get(s));
					rList.add(ma);
					i++;
				}
			}
		}
		return rList;
	}
}
