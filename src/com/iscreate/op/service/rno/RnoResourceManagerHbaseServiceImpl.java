package com.iscreate.op.service.rno;

import java.util.List;
import java.util.Map;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4HoDescQueryCond;
import com.iscreate.op.action.rno.model.G4MrDescQueryCond;
import com.iscreate.op.action.rno.model.G4SfDescQueryCond;
import com.iscreate.op.dao.rno.RnoResourceManageHbaseDao;

public class RnoResourceManagerHbaseServiceImpl implements RnoResourceManagerHbaseService {

	//注入
	private RnoResourceManageHbaseDao rnoResourceManageHbaseDao;

	public void setRnoResourceManageHbaseDao(
			RnoResourceManageHbaseDao rnoResourceManageHbaseDao) {
		this.rnoResourceManageHbaseDao = rnoResourceManageHbaseDao;
	}

	public RnoResourceManageHbaseDao getRnoResourceManageHbaseDao() {
		return rnoResourceManageHbaseDao;
	}

	/**
	 * 
	 * @title 分页获取Hbase的Mr数据描述表的数据
	 * @param cond
	 * @param newPage
	 * @return
	 * @author chao.xj
	 * @date 2015-3-18下午1:52:28
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, String>> queryMrDataFromHbaseByPage(
			G4MrDescQueryCond cond, Page newPage){
		return rnoResourceManageHbaseDao.queryMrDataFromHbaseByPage(cond, newPage);
	}
	
	/**
	 * 
	 * @title 分页获取Hbase的Ho数据描述表的数据
	 * @param cond
	 * @param newPage
	 * @return
	 */
	public List<Map<String, String>> queryHoDataFromHbaseByPage(
			G4HoDescQueryCond cond, Page newPage){
		return rnoResourceManageHbaseDao.queryHoDataFromHbaseByPage(cond, newPage);
	}
	/**
	 * @title 分页获取Hbase的SF数据描述表的数据
	 * @param cond
	 * @param newPage
	 */
	@Override
	public List<Map<String, String>> querySfDataFromHbaseByPage(G4SfDescQueryCond cond, Page newPage) {
		return rnoResourceManageHbaseDao.querySfDataFromHbaseByPage(cond, newPage);
	}
}
