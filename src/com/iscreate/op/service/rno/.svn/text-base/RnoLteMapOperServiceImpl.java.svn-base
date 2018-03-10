package com.iscreate.op.service.rno;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.rubyeye.xmemcached.MemcachedClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.RnoMapGroundSupportAction;
import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.action.rno.model.LteCellQueryResult;
import com.iscreate.op.constant.RnoConstant;
import com.iscreate.op.dao.rno.RnoLteMapOperDao;
import com.iscreate.op.dao.system.SysAreaDao;
import com.iscreate.op.pojo.rno.RnoLteCell;
import com.iscreate.op.service.system.SysAreaService;

public class RnoLteMapOperServiceImpl implements RnoLteMapOperService {

	// -----------类静态-------------//
	private static Log log = LogFactory.getLog(RnoMapGroundSupportAction.class);
	
	// ---注入----//
	private RnoLteMapOperDao rnoLteMapOperDao;
	private SysAreaService sysAreaService;
	private SysAreaDao sysAreaDao;
	public MemcachedClient memCached;
	
	//------------set---------------//
	public void setRnoLteMapOperDao(RnoLteMapOperDao rnoLteMapOperDao) {
		this.rnoLteMapOperDao = rnoLteMapOperDao;
	}

	public void setSysAreaService(SysAreaService sysAreaService) {
		this.sysAreaService = sysAreaService;
	}

	public void setSysAreaDao(SysAreaDao sysAreaDao) {
		this.sysAreaDao = sysAreaDao;
	}

	public void setMemCached(MemcachedClient memCached) {
		this.memCached = memCached;
	}
	
	/**
	 * 获取用户可访问的指定级别的区域
	 */
	public List<Area> ltefindAreaInSpecLevelListByUserId(String accountId,
			String areaLevel) {
		log.info("进入 ltefindAreaInSpecLevelListByUserId 。 accountId=" + accountId);

		List<Map<String, Object>> user_area_list = sysAreaService
				.getAreaByAccount(accountId);

		log.info("user_area_list==" + user_area_list);
		if (user_area_list == null || user_area_list.size() == 0) {
			return null;
		}

		long[] areaIds = new long[user_area_list.size()];
		int i = 0;
		for (Map<String, Object> usea : user_area_list) {
			areaIds[i++] = Long.parseLong(usea.get("AREA_ID").toString());
		}

		// String area_level = "区/县";
		List<Map<String, Object>> user_area_in_spec_level_list = sysAreaDao
				.getSubAreasInSpecAreaLevel(areaIds, areaLevel);
		List<Area> result = new ArrayList<Area>();
		Area area;
		for (Map<String, Object> one : user_area_in_spec_level_list) {
			area = Area.fromMap(one);
			if (area != null) {
				if (areaLevel.equals(area.getArea_level())) {
					result.add(area);
				}
			}
		}
		return result;	
	}
	
	/**
	 * 分页获取区/县的ltecell
	 * 
	 * @param areaId 指定区域
	 * @param page 分页参数
	 * @author peng.jm
	 * 2014-5-15 16:50:37
	 */
	public LteCellQueryResult getLteCellByPage(long areaId, Page page) {
		log.info("进入方法：getLteCellByPage 。areaid=" + areaId + ",page=" + page);
		//
		List<RnoLteCell> lteCells = null;
		String cacheKey = RnoConstant.CacheConstant.CACHE_GISCELL_IN_AREA_PRE
				+ areaId;
		try {
			log.info("从缓存中获取->");
			lteCells = (List<RnoLteCell>) memCached.get(cacheKey);
			log.info("缓存获取的结果数量为 ："
					+ ((lteCells == null) ? 0 : lteCells.size()));
			// System.out.println("缓存获取的结果为
			// ："+gisCells==null?0:gisCells.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (lteCells == null) {
			log.info("从数据库获取->");
			lteCells = rnoLteMapOperDao.getRnoLteCellInArea(areaId);
			log.info("从数据库中获取结果数量为：" + lteCells == null ? 0 : lteCells.size());
			// System.out.println("从数据库中获取结果为:"+gisCells==null?0:gisCells.size());
			//
			log.info("放入缓存->");
			try {
				memCached.set(cacheKey, RnoConstant.TimeConstant.TokenTime,
						lteCells);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Integer totalCnt = 0;
		List<RnoLteCell> resultLteCells = new ArrayList<RnoLteCell>();
		if (lteCells != null && lteCells.size() > 0) {
			totalCnt = lteCells.size();
			int start = page.getForcedStartIndex()==-1?(page.getCurrentPage() - 1) * page.getPageSize():page.getForcedStartIndex();
//			int end = page.getCurrentPage() * page.getPageSize();
			int end=start+page.getPageSize();
			// /范围[start,end)
			int size = lteCells.size();
			if (start < 0) {
				start = 0;
			}
			if (start > size) {
				start = size;
			}
			if (end < 1) {
				end = 1;
			}
			if (end > size) {
				end = size;
			}
			log.info("start==" + start + ",end=" + end);
			// 截取
			resultLteCells = lteCells.subList(start, end);

		}

		LteCellQueryResult result = new LteCellQueryResult();
		result.setTotalCnt(totalCnt);
		result.setLteCells(resultLteCells);

		return result;
	}

	/**
	 * 通过Lte小区id获取详细信息
	 * @author peng.jm
	 * 2014-5-20 14:30:23
	 */
	@Override
	public RnoLteCell getLteCellDetail(long lcid) {
		RnoLteCell lte = rnoLteMapOperDao.getLteCellById(lcid);
		return lte;
	}


}
