package com.iscreate.op.dao.rno;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.Transformers;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.iscreate.op.action.rno.Page;
import com.iscreate.op.pojo.rno.Cell;
import com.iscreate.op.pojo.rno.RnoGisCell;
import com.iscreate.op.pojo.rno.RnoLteCell;

public class RnoLteMapOperDaoImpl implements RnoLteMapOperDao {
	
	private static Log log = LogFactory.getLog(RnoCellDaoImpl.class);
	
	private List<RnoLteCell> lteCells = new ArrayList<RnoLteCell>();
	private RnoLteCell lteCell;
	
	// ---注入----//
	private HibernateTemplate hibernateTemplate;
	

	public HibernateTemplate getHibernateTemplate() {
		return hibernateTemplate;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * 获取指定区/县区域下的小区
	 * @param areaId
	 * @param page
	 * @return
	 * @author peng.jm
	 * 2014-5-15 17:12:09
	 */
	
	@SuppressWarnings("unchecked")
	public List<RnoLteCell> getRnoLteCellInArea(final long areaId) {
		log.info("进入方法getRnoLteCellInArea:  areaId: "+areaId);
		return hibernateTemplate
				.executeFind(new HibernateCallback<List<RnoLteCell>>() {
					public List<RnoLteCell> doInHibernate(Session arg0)
							throws HibernateException, SQLException {
						
						String sql = "select   rlc.CELL_NAME    as chineseName," +
											" rlc.LONGITUDE    as lng," +
											" rlc.LATITUDE     as lat," +
											" rlc.BUSINESS_CELL_ID  as lcid," +
											" rlc.AZIMUTH   as azimuth," +
											" rlcms.MAP_TYPE   as mapType," +
											" rlcms.SHAPE_TYPE as shapeType," +
											" rlcms.SHAPE_DATA as allLngLats" +
								" from  RNO_LTE_CELL rlc," +
								"  		RNO_LTE_CELL_MAP_SHAPE rlcms," +
								"  		(select AREA_ID" +
									"     	 from SYS_AREA s" +
									"        start with s.AREA_ID = ?" +
									"     connect by prior s.AREA_ID = s.PARENT_ID) ar" +
							   "   where rlc.LTE_CELL_ID = rlcms.LTE_CELL_ID" +
							   "  and rlc.AREA_ID = ar.AREA_ID ";
							
						log.info(areaId+"获取指定区域下的lte小区的sql：" + sql);
						SQLQuery query = arg0.createSQLQuery(sql);
						query.setLong(0, areaId);
						query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
						List<Map<String,Object>> result = query.list();
						
						Map<String,Object> map;

						for (int i = 0; i < result.size(); i++) {
							map = (Map<String,Object>)result.get(i);
							lteCell = new RnoLteCell();
							lteCell.setCell( map.get("LCID").toString());
							lteCell.setLng(((BigDecimal) map.get("LNG")).doubleValue());
							lteCell.setLat(((BigDecimal) map.get("LAT")).doubleValue());
							lteCell.setChineseName(map.get("CHINESENAME").toString());
							if(map.get("AZIMUTH")==null){
								lteCell.setAzimuth(null);
							} else {
								lteCell.setAzimuth(((BigDecimal)map.get("AZIMUTH")).floatValue());
							}
							lteCell.setMapType(map.get("MAPTYPE").toString());
							lteCell.setShapeType(map.get("SHAPETYPE").toString());
							lteCell.setAllLngLats(map.get("ALLLNGLATS").toString());
							
							//System.out.println(lteCell.toString());
							lteCells.add(lteCell);
						}
						
						return lteCells;
					}
				});
	}

	/**
	 * 通过Lte小区id获取该小区详情
	 * @author peng.jm
	 * 2014-5-20 14:33:29
	 */
	@Override
	public RnoLteCell getLteCellById(final long lcid) {
		return hibernateTemplate.execute(new HibernateCallback<RnoLteCell>() {
			public RnoLteCell doInHibernate(Session arg0) throws HibernateException,
					SQLException {
				String sql = " select rlc.CELL_NAME as chineseName," +
				 		"rlc.BUSSINESS_CELL_ID as lcid," +
						"rlc.BAND_TYPE as bandType," +
						"rlc.COVER_TYPE as coverType," +
						"rlc.BAND as band," +
						"rlc.EARFCN as earfcn," +
						"rlc.GROUND_HEIGHT as groundHeight," +
						"rlc.RRUNUM as rrunum," +
						"rlc.RRUVER as rruver," +
						"rlc.ANTENNA_TYPE as antennaType," +
						"rlc.INTEGRATED as integrated," +
						"rlc.RSPOWER as rspower," +
						"rlc.COVER_RANGE as coverRange " +
					" from RNO_LTE_CELL rlc " +
					" where rlc.LTE_CELL_ID = ?";
				log.info("获取指定id的lte小区详情的sql：" + sql);
				SQLQuery query = arg0.createSQLQuery(sql);
				query.setLong(0, lcid);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
				List<Map<String,Object>> result = query.list();
				Map<String,Object> map;
				for (int i = 0; i < result.size(); i++) {
					map = (Map<String,Object>)result.get(i);
					lteCell = new RnoLteCell();
					lteCell.setCell(map.get("LCID").toString());
					if(map.get("CHINESENAME")==null){
						lteCell.setChineseName(null);
					} else {
						lteCell.setChineseName(map.get("CHINESENAME").toString());
					}
					if(map.get("BANDTYPE")==null){
						lteCell.setBandType(null);
					} else {
						lteCell.setBandType(map.get("BANDTYPE").toString());
					}
					if(map.get("COVERTYPE")==null){
						lteCell.setCoverType(null);
					} else {
						lteCell.setCoverType(map.get("COVERTYPE").toString());
					}
					if(map.get("BAND")==null){
						lteCell.setBand(null);
					} else {
						lteCell.setBand(map.get("BAND").toString());
					}
					if(map.get("EARFCN")==null){
						lteCell.setEarfcn(null);
					} else {
						lteCell.setEarfcn(map.get("EARFCN").toString());
					}
					if(map.get("GROUNDHEIGHT")==null){
						lteCell.setGroundHeight(null);
					} else {
						lteCell.setGroundHeight(((BigDecimal) map.get("GROUNDHEIGHT")).longValue());
					}
					if(map.get("RRUNUM")==null){
						lteCell.setRrunum(null);
					} else {
						lteCell.setRrunum(((BigDecimal) map.get("RRUNUM")).longValue());
					}
					if(map.get("RRUVER")==null){
						lteCell.setRruver(null);
					} else {
						lteCell.setRruver(map.get("RRUVER").toString());
					}
					if(map.get("ANTENNATYPE")==null){
						lteCell.setAntennaType(null);
					} else {
						lteCell.setAntennaType(map.get("ANTENNATYPE").toString());
					}
					if(map.get("INTEGRATED")==null){
						lteCell.setIntegrated(null);
					} else {
						lteCell.setIntegrated(map.get("INTEGRATED").toString());
					}
					if(map.get("RSPOWER")==null){
						lteCell.setRspower(null);
					} else {
						lteCell.setRspower(((BigDecimal) map.get("RSPOWER")).longValue());
					}
					if(map.get("COVERRANGE")==null){
						lteCell.setCoverRange(null);
					} else {
						lteCell.setCoverRange(map.get("COVERRANGE").toString());
					}
				}
				return lteCell;
			}
		});
	}
}
