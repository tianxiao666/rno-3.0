package com.iscreate.op.service.rno;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4MroMrsDescQueryCond;
import com.iscreate.op.dao.rno.RnoResourceManagerHiveDao;

public class RnoResourceManagerHiveServiceImpl implements
		RnoResourceManagerHiveService {

	private static Log log=LogFactory.getLog(RnoResourceManagerHiveServiceImpl.class);
	private RnoResourceManagerHiveDao rnoResourceManagerHiveDao;
	
	public RnoResourceManagerHiveDao getRnoResourceManagerHiveDao() {
		return rnoResourceManagerHiveDao;
	}

	public void setRnoResourceManagerHiveDao(
			RnoResourceManagerHiveDao rnoResourceManagerHiveDao) {
		this.rnoResourceManagerHiveDao = rnoResourceManagerHiveDao;
	}

	/**
	 * 
	 * @title 分页查询符合条件的4g mromrs的描述记录
	 * @param cond
	 * @param page
	 * @return
	 * @author chao.xj
	 * @date 2015-10-22下午12:03:10
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> queryMroMrsDataFromHiveByPage(final 
			G4MroMrsDescQueryCond  cond,final Page page){
		
		return rnoResourceManagerHiveDao.queryMroMrsDataFromHiveByPage(cond, page);
	}
}
