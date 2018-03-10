package com.iscreate.op.service.rno;

import java.util.List;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.Area;
import com.iscreate.op.action.rno.model.LteCellQueryResult;
import com.iscreate.op.pojo.rno.RnoLteCell;

public interface RnoLteMapOperService {

	/**
	 * 获取用户可访问的指定级别的区域
	 */
	public List<Area> ltefindAreaInSpecLevelListByUserId(String accountId,
			String areaLevel);

	/**
	 * 分页获取区/县的ltecell
	 * 
	 * @param areaId 指定区域
	 * @param page 分页参数
	 * @author peng.jm
	 * 2014-5-15 16:50:37
	 */
	public LteCellQueryResult getLteCellByPage(long areaId, Page page);

	/**
	 * 通过Lte小区id获取小区详情
	 */
	public RnoLteCell getLteCellDetail(long lcid);

}
