package com.iscreate.op.dao.rno;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.iscreate.op.action.rno.model.Eri2GFasQueryCond;
import com.iscreate.op.action.rno.model.Eri2GMrrQueryCond;

public class RnoIndexDisplayDaoImpl implements RnoIndexDisplayDao {
private static Log log=LogFactory.getLog(RnoIndexDisplayDaoImpl.class);
	
	private HibernateTemplate hibernateTemplate;

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}
	
	/**
	 * 
	 * @title 查询爱立信2G小区MRR指标
	 * @param cond
	 * @return
	 * @author chao.xj
	 * @date 2014-11-18上午10:22:41
	 * @company 怡创科技
	 * @version 1.2
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> queryEri2GCellMrrIndex(final Eri2GMrrQueryCond cond){
		return hibernateTemplate
				.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
					public List<Map<String, Object>> doInHibernate(Session arg0)
							throws HibernateException, SQLException {
						String table= cond.buildTableCont();
						String field = cond.buildFieldCont();
						String where = cond.buildWhereCont();
						log.debug("queryEri2GCellMrrIndex ,where=" + where);
						String whereResult = (where == null || where.trim()
								.isEmpty()) ? ("") : (" where " + where);
						String sql = "select "+field+" from "+table+" where "+where;
						log.debug("queryEri2GCellMrrIndex ,sql=" + sql);
						SQLQuery query = arg0.createSQLQuery(sql);
						query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
						List<Map<String, Object>> rows = query.list();
						return rows;
					}
				});
	}
	/**
	 * 
	 * @title 查询爱立信2G小区FAS指标
	 * @param cond
	 * @return
	 * @author chao.xj
	 * @date 2015-2-2上午10:16:06
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> queryEri2GCellFasIndex(final Eri2GFasQueryCond cond){
		return hibernateTemplate
				.executeFind(new HibernateCallback<List<Map<String, Object>>>() {
					public List<Map<String, Object>> doInHibernate(Session arg0)
							throws HibernateException, SQLException {
						String table= cond.buildTableCont();
						String field = cond.buildFieldCont();
						String where = cond.buildWhereCont();
						log.debug("queryEri2GCellFasIndex ,where=" + where);
						String whereResult = (where == null || where.trim()
								.isEmpty()) ? ("") : (" where " + where);
						String sql = "select "+field+" from "+table+" where "+where;
						log.debug("queryEri2GCellFasIndex ,sql=" + sql);
						SQLQuery query = arg0.createSQLQuery(sql);
						query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
						List<Map<String, Object>> rows = query.list();
						return rows;
					}
				});
	}
}
