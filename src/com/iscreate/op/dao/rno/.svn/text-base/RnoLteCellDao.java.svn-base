package com.iscreate.op.dao.rno;

import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.LteCellQueryCondition;
import com.iscreate.op.action.rno.Page;

public interface RnoLteCellDao {
	
	
	/**
	 * 计算满足要求的小区总数量
	 * @param cond
	 * @return
	 * @author brightming
	 * 2014-5-19 下午2:03:25
	 */
	public long getLteCellCount(LteCellQueryCondition cond);
	
	
	
	/**
	 * 分页查询LTE小区信息
	 * @param cond
	 * @param startIndex
	 * @param cnt
	 * @return
	 * @author brightming
	 * 2014-5-19 下午2:01:57
	 */
	public List<Map<String, Object>> queryLteCellByPage(LteCellQueryCondition cond,
			long startIndex,long cnt);
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
	 * 2014-5-19 下午3:55:54
	 */
	public List<Map<String, Object>> queryLteCellAndCositeCells(long lteCellId);



	/**
	 * 删除指定的lte 小区
	 * @param ids
	 * @return
	 * @author brightming
	 * 2014-5-21 上午11:25:46
	 */
	public boolean deleteRnoLteCellByIds(String ids);

}
