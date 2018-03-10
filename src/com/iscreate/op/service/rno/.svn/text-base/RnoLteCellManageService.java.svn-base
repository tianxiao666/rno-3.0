package com.iscreate.op.service.rno;

import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.LteCellQueryCondition;
import com.iscreate.op.action.rno.Page;

public interface RnoLteCellManageService {

	/**
	 * 分页查询LTE小区信息
	 * @param page
	 * @param cond
	 * @return
	 * @author brightming
	 * 2014-5-19 下午1:40:21
	 */
	public List<Map<String,Object>> queryLteCellByPage(Page page,LteCellQueryCondition cond);
	
	/**
	 * 获取小区详情
	 * @param lteCellId
	 * @return
	 * @author brightming
	 * 2014-5-19 下午1:41:05
	 */
	public Map<String,Object> getLteCellDetail(long lteCellId);
	
	/**
	 * 修改lte小区信息
	 * @param lteCellId
	 * @param lteCell
	 * @return
	 * @author brightming
	 * 2014-5-19 下午1:42:05
	 */
	public boolean modifyLteCellDetail(long lteCellId,Map<String,Object> lteCell);

	/**
	 * 查询指定小区的详情，以及与该小区同站的其他小区的详情。
	 * 第一个为目标小区，后面的为其他的小区。
	 * @param lteCellId
	 * @return
	 * @author brightming
	 * 2014-5-19 下午3:54:38
	 */
	public List<Map<String, Object>> queryLteCellAndCositeCells(long lteCellId);

	/**
	 * 删除指定的lte id
	 * @param ids
	 * @return
	 * @author brightming
	 * 2014-5-21 上午11:24:55
	 */
	public boolean deleteRnoLteCellByIds(String ids);
}
