package com.iscreate.op.dao.rno;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.action.rno.model.G4MroMrsDescQueryCond;
import com.iscreate.op.action.rno.model.G4NiDescQueryCond;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.plat.networkresource.dataservice.DataSourceConn;
import com.iscreate.plat.system.datasourcectl.DataSourceConst;
import com.iscreate.plat.system.datasourcectl.DataSourceContextHolder;

public class RnoResourceManagerHiveDaoImpl implements RnoResourceManagerHiveDao {

	private static Log log=LogFactory.getLog(RnoResourceManagerHiveDaoImpl.class);
	
	private JdbcTemplate jdbcTemplate;
	public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
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
		log.debug("进入queryMroMrsDataFromHiveByPage cond="+cond+",page="+page);
		Statement stmt = null;
		Connection conn;
		List<Map<String, Object>> mromrsDescLists = null;
		
		try {
			conn = jdbcTemplate.getDataSource().getConnection();
			stmt = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String field_out = " cityid,meatime,datatype,recordcnt,createtime,modtime";
		String field_inner = "cityid,meatime,datatype,recordcnt,createtime,modtime";
		String where = cond.buildWhereCont();
		log.debug("queryMroMrsDataFromHiveByPage ,where=" + where);
		String whereResult = (where == null || where.trim().isEmpty()) ? ("")
				: (" where " + where);
		int start = (page.getPageSize() * (page.getCurrentPage() - 1) + 1);
		int end = (page.getPageSize() * page.getCurrentPage());
		//sort or order?
		String sql = "select "
				+ field_out
				+ " from (select "
				+ field_inner
				+ ",row_number() over (sort by meatime desc,createtime desc) rn from rno_4g_mromrs_desc  "
				+ whereResult + ") h where  rn>=" + start + " and rn<=" + end ;//+" order  by meatime desc,createtime desc";
		log.debug("queryMroMrsDataFromHiveByPage ,sql=" + sql);
		mromrsDescLists = RnoHelper.commonQuery(stmt, sql);
		// 封装分页对象
		page.setTotalCnt(mromrsDescLists.size());
		log.debug("退出queryMroMrsDataFromHiveByPage 获取数据大小:"+mromrsDescLists.size());
		return mromrsDescLists;
	}
}
