package com.iscreate.op.action.rno;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iscreate.op.action.rno.model.Eri2GFasQueryCond;
import com.iscreate.op.action.rno.model.Eri2GMrrQueryCond;
import com.iscreate.op.service.rno.RnoIndexDisplayService;
import com.iscreate.op.service.rno.tool.HttpTools;

public class RnoIndexDisplayAction extends RnoCommonAction {
	private static Log log = LogFactory.getLog(RnoIndexDisplayAction.class);
	private static Gson gson = new GsonBuilder().create();// 线程安全
	//注入
	private RnoIndexDisplayService rnoIndexDisplayService;
	
	//--爱立信 2G MRR cell信息指标查询
	private Eri2GMrrQueryCond eri2gMrrQueryCond;
	//--爱立信 2G FAS cell信息指标查询
	private Eri2GFasQueryCond eri2gFasQueryCond;
	
	public Eri2GFasQueryCond getEri2gFasQueryCond() {
		return eri2gFasQueryCond;
	}

	public void setEri2gFasQueryCond(Eri2GFasQueryCond eri2gFasQueryCond) {
		this.eri2gFasQueryCond = eri2gFasQueryCond;
	}

	public Eri2GMrrQueryCond getEri2gMrrQueryCond() {
		return eri2gMrrQueryCond;
	}

	public void setEri2gMrrQueryCond(Eri2GMrrQueryCond eri2gMrrQueryCond) {
		this.eri2gMrrQueryCond = eri2gMrrQueryCond;
	}
	public RnoIndexDisplayService getRnoIndexDisplayService() {
		return rnoIndexDisplayService;
	}

	public void setRnoIndexDisplayService(
			RnoIndexDisplayService rnoIndexDisplayService) {
		this.rnoIndexDisplayService = rnoIndexDisplayService;
	}
	/**
	 * 
	 * @title 初始化MRR指标图表展现小区查看页面
	 * @return
	 * @author chao.xj
	 * @date 2014-11-13上午11:46:14
	 * @company 怡创科技
	 * @version 1.2
	 */
	public String initEri2GMrrIndexDisplayCellQueryPageAction() {
		initAreaList();//  加载区域相关信息
		return "success";
	}
	/**
	 * 
	 * @title 搜索爱立信2G小区MRR指标
	 * @author chao.xj
	 * @date 2014-11-13下午2:51:19
	 * @company 怡创科技
	 * @version 1.2
	 */
	public void searchEri2GMrrCellIndexForAjaxAction() {
		
		eri2gMrrQueryCond=new Eri2GMrrQueryCond();
		eri2gMrrQueryCond.setCell((String)attachParams.get("cell"));
		eri2gMrrQueryCond.setCityId(Long.parseLong((String) attachParams.get("cityId")));
		eri2gMrrQueryCond.setMeaBegTime((String)attachParams.get("meaBegTime"));
		eri2gMrrQueryCond.setMeaEndTime((String)attachParams.get("meaEndTime"));
		eri2gMrrQueryCond.setDataType((String)attachParams.get("dataType"));
		eri2gMrrQueryCond.setChgr((String)attachParams.get("chgr"));
		
		log.debug("searchEri2GMrrCellIndexForAjaxAction.attmap="+attachParams+",queryCond="+eri2gMrrQueryCond);
		
		List<Map<String, Object>> obj=rnoIndexDisplayService.queryEri2GCellMrrIndex(eri2gMrrQueryCond);
		String datas = gson.toJson(obj);
		String result = "{'data':" + datas + "}";
		log.debug("退出searchEri2GMrrCellIndexForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}
	/**
	 * 
	 * @title 初始化FAS指标图表展现小区查看页面
	 * @return
	 * @author chao.xj
	 * @date 2015-2-2上午9:40:58
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String initEri2GFasIndexDisplayCellQueryPageAction() {
		initAreaList();//  加载区域相关信息
		return "success";
	}
	
	/**
	 * 
	 * @title 初始化新站优化页面
	 * @return
	 * @author li.tf
	 * @date 2015-10-23上午10:50:58
	 * @company 怡创科技
	 * @version 2.0.9
	 */
	public String initNewCellOptimizeAction() {
		initAreaList();//  加载区域相关信息
		return "success";
	}
	/**
	 * 
	 * @title 搜索爱立信2G小区Fas指标
	 * @author chao.xj
	 * @date 2015-2-2上午10:10:15
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void searchEri2GFasCellIndexForAjaxAction() {
		
		eri2gFasQueryCond=new Eri2GFasQueryCond();
		eri2gFasQueryCond.setCell((String)attachParams.get("cell"));
		eri2gFasQueryCond.setCityId(Long.parseLong((String) attachParams.get("cityId")));
		eri2gFasQueryCond.setMeaBegTime((String)attachParams.get("meaBegTime"));
		eri2gFasQueryCond.setMeaEndTime((String)attachParams.get("meaEndTime"));
		
		
		log.debug("searchEri2GFasCellIndexForAjaxAction.attmap="+attachParams+",queryCond="+eri2gFasQueryCond);
		
		List<Map<String, Object>> obj=rnoIndexDisplayService.queryEri2GCellFasIndex(eri2gFasQueryCond);
		String datas = gson.toJson(obj);
		String result = "{'data':" + datas + "}";
		log.debug("退出searchEri2GFasCellIndexForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}
	/**
	 * 
	 * @title 初始化NI指标图表展现小区查看页面
	 * @return
	 * @author chao.xj
	 * @date 2016年3月29日下午3:55:14
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public String init4GNiIndexDisplayCellQueryPageAction() {
		initAreaList();//  加载区域相关信息
		return "success";
	}
	/**
	 * 
	 * @title 搜索爱立信4G小区NI指标
	 * @author chao.xj
	 * @date 2016年3月29日下午3:56:12
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public void search4GNiCellIndexForAjaxAction() {
		
		/*eri2gMrrQueryCond=new Eri2GMrrQueryCond();
		eri2gMrrQueryCond.setCell((String)attachParams.get("cell"));
		eri2gMrrQueryCond.setCityId(Long.parseLong((String) attachParams.get("cityId")));
		eri2gMrrQueryCond.setMeaBegTime((String)attachParams.get("meaBegTime"));
		eri2gMrrQueryCond.setMeaEndTime((String)attachParams.get("meaEndTime"));
		eri2gMrrQueryCond.setDataType((String)attachParams.get("dataType"));
		eri2gMrrQueryCond.setChgr((String)attachParams.get("chgr"));
		
		log.debug("searchEri2GMrrCellIndexForAjaxAction.attmap="+attachParams+",queryCond="+eri2gMrrQueryCond);
		
		List<Map<String, Object>> obj=rnoIndexDisplayService.queryEri2GCellMrrIndex(eri2gMrrQueryCond);*/
		List<Map<String, Object>> obj= new ArrayList<Map<String,Object>>();
		String datas = gson.toJson(obj);
		String result = "{'data':" + datas + "}";
		log.debug("退出search4GNiCellIndexForAjaxAction。输出：" + result);
		HttpTools.writeToClient(result);
	}
}
