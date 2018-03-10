package com.iscreate.op.dao.rno;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iscreate.op.dao.rno.AuthDsDataDaoImpl.SysArea;
import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoLteStructAnaJobRec;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.task.structana.RnoLteStructAnaJobRunnable;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.HiveHelper;
import com.iscreate.op.service.rno.tool.HiveHelper.UdfObj;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.SshExecuter;
import com.iscreate.op.service.rno.tool.ZipFileHandler;

public class RnoStructureAnalysisHiveDaoImpl implements RnoStructureAnalysisHiveDao {
	private static Log log = LogFactory.getLog(RnoStructureAnalysisHiveDaoImpl.class);

	/**
	 * 
	 * @title LTE结构分析入口
	 * @param worker
	 * @param connection
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-10-29下午4:04:15
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public ResultInfo doLteStructAnalysis(RnoLteStructAnaJobRunnable worker, Connection connection,
			RnoLteStructAnaJobRec jobRec) {
		log.debug("doLteStructAnalysis当前运行线程：" + Thread.currentThread());
		Statement stmt = null;
		ResultInfo result = new ResultInfo(true);
		if (jobRec == null) {
			result.setFlag(false);
			result.setMsg("LTE结构分析任务对象为空！");
			return result;
		}
		long t1 = System.currentTimeMillis(), t2;
		String stage = "";
		SshExecuter ssh = null;
		FileInputStream fis = null;
		boolean ok = true;// 具体工作内容
		try {
			stage = "准备HIVE数据仓库连接";
			/*-------------------------------获取链接------------------------------------------*/
			stmt = connection.createStatement();

			stage = "配置环境";
			/*-------------------------------配置环境------------------------------------------*/
			// 准备工作路径
			log.debug("准备LTE结构分析结果保存的路径...");
			String tmpParentDir = System.getProperty("java.io.tmpdir");
			String tmpDir = tmpParentDir + "/" + UUID.randomUUID().toString().replaceAll("-", "") + "/";
			// 确定临时目录存在
			File tmpDirFile = new File(tmpDir);
			if (!tmpDirFile.exists()) {
				tmpDirFile.mkdirs();
			}
			jobRec.setTmpDir(tmpDir);
			// ---工作路径准备完毕
			long cityId = jobRec.getCityId();
			// 准备城市信息
			String cityName = "";
			SysArea sa = AuthDsDataDaoImpl.getSysAreaByAreaId(cityId);
			if (sa != null) {
				cityName = sa.getName();
			}
			jobRec.setCityName(cityName);
			JobReport report = new JobReport(jobRec.getJobId());
			log.debug("汇总MRO>>>>>>>>>>>>>>>开始计算4G弱覆盖...");
			t1 = System.currentTimeMillis();
			// sourceTab,targetTab
			stage = "更新工参";
			/*-------------------------------更新工参------------------------------------------*/
			Properties p = new Properties();
			fis = new FileInputStream("ssh2sqoop.xml");
			p.loadFromXML(fis);
			String host = p.getProperty("host", "192.168.50.81");
			Integer port = Integer.parseInt(p.getProperty("port", "22"));
			String user = p.getProperty("user", "rnoprohbase");
			String pwd = p.getProperty("pwd", "rnoproHbase");
			String key = p.getProperty("key", "");
			String passPhrase = p.getProperty("passPhrase", "");

			ssh = SshExecuter.newInstance(host, port, user, pwd, key, passPhrase);
			ssh.shell(p.getProperty("cmd"), jobRec.getTmpDir() + "/sshtmp.log");

			stage = "计算重叠覆盖数据";
			/*-------------------------------计算重叠覆盖数据------------------------------------------*/
			List<Map<String, Object>> overLayLists = calcOverLay(worker, report, stmt, jobRec);
			System.out.println("overLayLists===" + overLayLists.size());
			/*-------------------------------保存重叠覆盖数据------------------------------------------*/
			stage = "保存重叠覆盖数据";
			saveOverLayIndex(overLayLists, jobRec);

			stage = "计算过覆盖";
			/*-------------------------------计算过覆盖数据------------------------------------------*/
			List<Map<String, Object>> overCoverLists = calcOverCover(worker, report, stmt, jobRec);
			System.out.println("overCoverLists===" + overCoverLists.size());
			/*-------------------------------保存过覆盖数据------------------------------------------*/
			stage = "保存过覆盖";
			saveOverCoverIndex(overCoverLists, jobRec);

			stage = "计算汇总指标";
			/*-------------------------------计算指标汇总数据------------------------------------------*/
			List<Map<String, Object>> indexSumLists = calcIndexSum(worker, report, stmt, jobRec);
			System.out.println("indexSumLists===" + indexSumLists.size());
			/*-------------------------------保存指标汇总数据------------------------------------------*/
			stage = "保存汇总指标";
			saveIndexSum(indexSumLists, jobRec);
			/*-------------------------------压缩结构数据------------------------------------------*/
			stage = "保存最终数据";
			saveLteStructAnaResult(jobRec);
		} catch (Exception e1) {
			e1.printStackTrace();
			result.setFlag(false);
			result.setMsg(stage + "失败！");
			t2 = System.currentTimeMillis();
			log.debug("汇总MRO<<<<<<<<<<<<<<<完成计算4G弱覆盖，结果:false" + ",耗时：" + (t2 - t1));
			return result;
		} finally {
			if (ssh != null) {
				try {
					ssh.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		t2 = System.currentTimeMillis();
		log.debug("汇总MRO<<<<<<<<<<<<<<<完成计算4G弱覆盖，结果:" + ok + ",耗时：" + (t2 - t1));
		/*
		 * if (!ok) {
		 * result.setFlag(false);
		 * result.setMsg("无法转移4G小区到4G临时小区表，无法进行运算！");
		 * report.setSystemFields("整理小区数据", stepBegTime, stepEndTime,
		 * JobState.Failed.getCode(), "");
		 * worker.addJobReport(report);
		 * return result;
		 * } else {
		 * report.setSystemFields("整理小区数据", stepBegTime, stepEndTime,
		 * JobState.Finished.getCode(), "");
		 * worker.addJobReport(report);
		 * }
		 */

		return result;
	}

	/**
	 * 
	 * @title 计算指标汇总
	 * @param worker
	 * @param report
	 * @param stmt
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-6上午9:41:14
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> calcIndexSum(RnoLteStructAnaJobRunnable worker, JobReport report, Statement stmt,
			RnoLteStructAnaJobRec jobRec) {
		log.debug("calcIndexSum 计算指标汇总当前运行线程：" + Thread.currentThread());

		long t1 = System.currentTimeMillis();
		DateUtil dateUtil = new DateUtil();
		Long cityId = jobRec.getCityId();
		String begTime = dateUtil.format_yyyyMMdd(jobRec.getBegMeaTime());
		String endTime = dateUtil.format_yyyyMMdd(jobRec.getEndMeaTime());
		String sql = "";
		List<Map<String, Object>> weakList = null;
		// 获取hive函数配置对象
		UdfObj udfObj = HiveHelper.readHiveFunCfgFromXml("lnglathelper");
		// 将udf-lnglathelper.jar加入到HIVE类路径下
		sql = "add jar " + udfObj.getClassPath();
		try {
			stmt.execute(sql);
			// 创建临时函数
			sql = "create temporary function lnglathelper as '" + udfObj.getPackageStr() + "'";
			stmt.execute(sql);
			sql = "select t.cellid cellid, "
					+ "       t.cellname cellname, "
					+ "       t.weakflag weakflag, "
					+ "       t.overlayflag overlayflag, "
					+ "       t.db1percnt overcovergroupcnt, "
					+ "       if(t.db1percnt >= 8, '是', '否') overcovergroupflag, "
					+ "       t.db1per16cnt overcoverprovincnt, "
					+ "       if(t.db1per16cnt >= 8, '是', '否') overcoverprovinflag "
					+ "  from (select t.cellid, "
					+ "               min(t.cellname) cellname, "
					+ "               min(t.weakflag) weakflag, "
					+ "               min(t.overlayflag) overlayflag, "
					+ "               count(case when t.ncellper > 0.01 then 1 end) db1percnt,"
					+ "               count(case when t.ncellper > 0.01 and t.dis > t.stspacing * 1.6 then 1 end) db1per16cnt "
					+ "          from (select t1.cellid, "
					+ "                       t1.cellname, "
					+ "                       t1.stspacing, "
					+ "                       t1.ncellcnt / t2.totalcnt ncellper, "
					+ "                       t1.dis, "
					+ "                       t2.weakflag, "
					+ "                       t2.overlayflag "
					+ "                  from (select t1.cellid, "
					+ "                               t1.ncellcnt, "
					+ "                               t2.cellname, "
					+ "                               t2.stspacing, "
					+ "                               lnglathelper(t2.celllon, t2.celllat, t3.ncelllon, t3.ncelllat) dis "
					+ "                          from (select cellid, ncellid," + "										  count(*) ncellcnt "
					+ "                                  from rno_4g_mro_data "
					+ "                                 where ncellid <> '' "
					+ "                                   and ltencrsrp - ltescrsrp >= -6 "
					+ "                                   and cityid = '"
					+ cityId
					+ "'"
					+ "                                   and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) >= '"
					+ begTime
					+ "' "
					+ "                                   and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) <= '"
					+ endTime
					+ "' "
					+ "                                 group by cellid, ncellid) t1 "
					+ "                          left join (select business_cell_id cellid, "
					+ "                                           cell_name        cellname, "
					+ "                                           longitude        celllon, "
					+ "                                           latitude         celllat, "
					+ "                                           station_space    stspacing "
					+ "                                      from rno_lte_cell "
					+ "                                     where area_id = "
					+ cityId
					+ ") t2 "
					+ "                            on (regexp_replace(t1.cellid, '-', '') = t2.cellid) "
					+ "                          left join (select business_cell_id ncellid, "
					+ "                                           longitude        ncelllon, "
					+ "                                           latitude         ncelllat "
					+ "                                      from rno_lte_cell "
					+ "                                     where area_id = "
					+ cityId
					+ ") t3 "
					+ "                            on (regexp_replace(t1.ncellid, '-', '') = t3.ncellid)) t1 "
					+ "                  left join (select t.cellid, "
					+ "                                   t.totalcnt, "
					+ "                                   if(t.partanti110cnt / t.totalcnt > 0.6 and t.totalcnt > 100, '是', '否') weakflag, "
					+ "                                   if(t.part11063cnt / t.totalcnt > 0.1 and t.totalcnt > 100, '是', '否') overlayflag "
					+ "                              from (select t.cellid, "
					+ "                                           count(*) totalcnt, "
					+ "                                           count(case when t.partanti110initcnt > 0 then 1 end) partanti110cnt, "
					+ "                                           count(case when t.part1106initcnt > 3 then 1 end) part11063cnt "
					+ "                                      from (select cellid, "
					+ "                                                   count(case when ltescrsrp < -110 then 1 end) partanti110initcnt, "
					+ "                                                   count(case when ltescrsrp > -110 and ltencrsrp - ltescrsrp > -6 then 1 end) part1106initcnt "
					+ "                                              from rno_4g_mro_data "
					+ "                                             where ncellid <> '' "
					+ "                                               and cityid = '"
					+ cityId
					+ "' "
					+ "                                               and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) >= '"
					+ begTime
					+ "' "
					+ "                                               and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) <= '"
					+ endTime
					+ "' "
					+ "                                             group by cellid, meatime, mmeues1apid, mmecode) t "
					+ "                                     group by t.cellId) t) t2 "
					+ "                    on (t1.cellid = t2.cellid)) t " + "         group by t.cellId) t";
			// 准备弱覆盖
			// 首先，采样点数要大于100；
			// 其次，RSRP<-110dbm的采样点占比要大于60%；
			// System.out.println("指标汇总sql ="+sql);
			weakList = RnoHelper.commonQuery(stmt, sql);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("转时出错！");
		}
		log.debug("准备计算指标汇总耗时：" + (System.currentTimeMillis() - t1) + "ms");
		return weakList;
	}

	/**
	 * 
	 * @title 计算重叠覆盖
	 * @param worker
	 * @param report
	 * @param stmt
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-5下午5:04:18
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> calcOverLay(RnoLteStructAnaJobRunnable worker, JobReport report, Statement stmt,
			RnoLteStructAnaJobRec jobRec) {
		log.debug("calcOverLay 计算重叠覆盖当前运行线程：" + Thread.currentThread());

		long t1 = System.currentTimeMillis();
		DateUtil dateUtil = new DateUtil();
		Long cityId = jobRec.getCityId();
		String begTime = dateUtil.format_yyyyMMdd(jobRec.getBegMeaTime());
		String endTime = dateUtil.format_yyyyMMdd(jobRec.getEndMeaTime());
		String sql = "";
		List<Map<String, Object>> weakList = null;
		try {
			// 准备重叠覆盖
			// 1、采样点总数大于1000；
			// 2、主小区RSRP＞-110dBm；
			// 3、邻小区信号强度 - 主小区信号强度>-6;
			// 4、一个样本中符合上述条件的邻区数目大于等于3的样本点为问题采样点；
			// 5、问题采样点/总采样点 >10%
			// 使用case when 的时候，如果用count来统计，只能写成count(case when cond then val
			// end)的形式，因为count只关注值存在与否，其中val的值不重要，只是作为条件满足的标记，
			// 切勿使用count(case when cond then val else val1 end) 的形式，因为在then else条件全不覆盖的情况下，count的统计怎么都会成立。
			// 可以用sum(case when cond then 1 else 0 end) 改写，满足条件为1，不满足为0，也能取得正确的统计结果
			sql = "select t.cellid cellid,t.totalcnt totalcnt, "
					+ " t.db105partcnt db105partcnt,"
					+ " t.db110partcnt db110partcnt,"
					+ " t.db105partcnt / t.totalcnt db105partcntper,"
					+ " t.db110partcnt / t.totalcnt db110partcntper,"
					+ " t.db11063partcnt db11063partcnt,"
					+ " t.db11064partcnt db11064partcnt,"
					+ " t.db11065partcnt db11065partcnt,"
					+ " t.db11066partcnt db11066partcnt,"
					+ " t.db11063partcnt / t.totalcnt db11063partcntper,"
					+ " t.db11064partcnt / t.totalcnt db11064partcntper,"
					+ " t.db11065partcnt / t.totalcnt db11065partcntper,"
					+ " t.db11066partcnt / t.totalcnt db11066partcntper,"
					+ " t.db110103partcnt db110103partcnt,"
					+ " t.db110104partcnt db110104partcnt,"
					+ " t.db110105partcnt db110105partcnt,"
					+ " t.db110106partcnt db110106partcnt,"
					+ " t.db110103partcnt / t.totalcnt db110103partcntper,"
					+ " t.db110104partcnt / t.totalcnt db110104partcntper,"
					+ " t.db110105partcnt / t.totalcnt db110105partcntper,"
					+ " t.db110106partcnt / t.totalcnt db110106partcntper,"
					+ " t.db1101partcnt db1101partcnt,"
					+ " t.db1102partcnt db1102partcnt,"
					+ " t.db1103partcnt db1103partcnt,"
					+ " t.db1104partcnt db1104partcnt,"
					+ " t.db1105partcnt db1105partcnt,"
					+ " t.db1106partcnt db1106partcnt,"
					+ " t.db1107partcnt db1107partcnt,"
					+ " t.db1108partcnt db1108partcnt,"
					+ " t.db1109partcnt db1109partcnt"
					+ " from (select t2.cellid, count(t2.cellid) totalcnt, "
					+ " count(case when t2.db105initcnt > 0 then 1 end) db105partcnt, "
					+ " count(case when t2.db110initcnt > 0 then 1 end) db110partcnt, "
					+ " count(case when t2.db1106initcnt > 3 then 1 end) db11063partcnt, "
					+ " count(case when t2.db1106initcnt > 4 then 1 end) db11064partcnt, "
					+ " count(case when t2.db1106initcnt > 5 then 1 end) db11065partcnt, "
					+ " count(case when t2.db1106initcnt > 6 then 1 end) db11066partcnt, "
					+ " count(case when t2.db11010initcnt > 3 then 1 end) db110103partcnt, "
					+ " count(case when t2.db11010initcnt > 4 then 1 end) db110104partcnt, "
					+ " count(case when t2.db11010initcnt > 5 then 1 end) db110105partcnt, "
					+ " count(case when t2.db11010initcnt > 6 then 1 end) db110106partcnt, "
					+ " count(case when t2.db110initcnt = 1 then 1 end) db1101partcnt, "
					+ " count(case when t2.db110initcnt = 2 then 1 end) db1102partcnt, "
					+ " count(case when t2.db110initcnt = 3 then 1 end) db1103partcnt, "
					+ " count(case when t2.db110initcnt = 4 then 1 end) db1104partcnt, "
					+ " count(case when t2.db110initcnt = 5 then 1 end) db1105partcnt, "
					+ " count(case when t2.db110initcnt = 6 then 1 end) db1106partcnt, "
					+ " count(case when t2.db110initcnt = 7 then 1 end) db1107partcnt, "
					+ " count(case when t2.db110initcnt = 8 then 1 end) db1108partcnt, "
					+ " count(case when t2.db110initcnt = 9 then 1 end) db1109partcnt "
					+ " from (select t1.cellid, "
					+ " count(case when t1.ltescrsrp > -105 then 1 end) db105initcnt, "
					+ " count(case when t1.ltescrsrp > -110 then 1 end) db110initcnt, "
					+ " count(case when t1.ltescrsrp > -110 and t1.ltencrsrp - t1.ltescrsrp > -6 then 1 end) db1106initcnt, "
					+ " count(case when t1.ltescrsrp > -110 and t1.ltencrsrp - t1.ltescrsrp > -10 then  1 end) db11010initcnt "
					+ " from (select cellid,meatime,mmeues1apid,mmecode,ltescrsrp,ltencrsrp "
					+ " from rno_4g_mro_data " + " where ncellid<>'' and cityid = '" + cityId + "' "
					+ " and from_unixtime(unix_timestamp(meatime,'yyyy/MM/dd HH:mm:ss')) >= '" + begTime + "' "
					+ " and from_unixtime(unix_timestamp(meatime,'yyyy/MM/dd HH:mm:ss')) <= '" + endTime + "' "
					+ " ) t1 group by t1.cellid, t1.meatime, t1.mmeues1apid, t1.mmecode) t2 group by t2.cellid) t";
			// System.out.println("重叠覆盖sql ="+sql);
			weakList = RnoHelper.commonQuery(stmt, sql);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("转时出错！");
		}
		log.debug("准备计算重叠覆盖耗时：" + (System.currentTimeMillis() - t1) + "ms");
		return weakList;
	}

	/**
	 * 
	 * @title 计算过覆盖
	 * @param worker
	 * @param report
	 * @param stmt
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-6上午10:01:37
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> calcOverCover(RnoLteStructAnaJobRunnable worker, JobReport report, Statement stmt,
			RnoLteStructAnaJobRec jobRec) {
		log.debug("calcOverCover 计算过覆盖当前运行线程：" + Thread.currentThread());

		long t1 = System.currentTimeMillis();
		DateUtil dateUtil = new DateUtil();
		Long cityId = jobRec.getCityId();
		String begTime = dateUtil.format_yyyyMMdd(jobRec.getBegMeaTime());
		String endTime = dateUtil.format_yyyyMMdd(jobRec.getEndMeaTime());
		String sql = "";
		List<Map<String, Object>> weakList = null;
		try {
			// 获取hive函数配置对象
			UdfObj udfObj = HiveHelper.readHiveFunCfgFromXml("lnglathelper");
			// 将udf-lnglathelper.jar加入到HIVE类路径下
			sql = "add jar " + udfObj.getClassPath();
			stmt.execute(sql);
			// 创建临时函数
			sql = "create temporary function lnglathelper as '" + udfObj.getPackageStr() + "'";
			stmt.execute(sql);
			// 准备过覆盖
			// 1.集团过覆盖小区定义：
			// 过覆盖小区：小区与8个小区相关系数＞1%，该小区为过覆盖小区。
			// 2.省内生产过覆盖小区定义：
			// 某小区与自身站间距X倍以外的8个邻小区相关联系数>1%，该小区为过覆盖小区；其中默认X=1.6。
			// 主小区（scell）MR报告总数为分母，针对每个MR报告，若scell RSRP-ncell（邻小区）RSRP<6dB，则分子加1。当关联系数大于1%，定义该邻小区与主小区关联。
			// 或者说该邻小区被主小区测量到且可能对主小区产生干扰。
			// 注解：一个小区会和很多个小区有相关，如小区A对小区B相关系数为2%，小区A对小区B相关系数为3%...等等假设小区A和10个小区的有相关系数，其中8个或以上相关系数大于1%
			// 邻小区采样点就是该邻小区 - 主小区 >=-6db的采样点数。
			sql = "select t1.cellid cellid,																		"
					+ "       t1.ncellid ncellid,                                                                    "
					+ "       t1.ncellcnt ncellcnt,                                                                  "
					+ "       t2.totalcnt totalcnt,                                                                  "
					+ "       t1.stspacing stspacing,                                                                "
					+ "       t1.celllon celllon,                                                                    "
					+ "       t1.celllat celllat,                                                                    "
					+ "       t1.ncelllon ncelllon,                                                                  "
					+ "       t1.ncelllat ncelllat,                                                                  "
					+ "       t1.ltencpci ltencpci,"
					+ "t3.cell_name cellname,"
					+ "t4.cell_name ncellname,"
					+ "       t1.ncellcnt / t2.totalcnt ncellper,                                                    "
					+ "       lnglathelper(t1.celllon, t1.celllat, t1.ncelllon, t1.ncelllat) dis                     "
					+ "  from (select cellid,                                                                        "
					+ "               ncellid,                                                                       "
					+ "               count(*) ncellcnt,                                                             "
					+ "               min(ltencpci) ltencpci,                                                        "
					+ "               min(stspacing) stspacing,                                                      "
					+ "               min(celllon) celllon,                                                          "
					+ "               min(celllat) celllat,                                                          "
					+ "               min(ncelllon) ncelllon,                                                        "
					+ "               min(ncelllat) ncelllat,"
					+ "				 min(cellname) cellname,"
					+ "               min(ncellname) ncellname                "
					+ "          from rno_4g_mro_data                                                                "
					+ "         where ncellid <> '' and ltencrsrp-ltescrsrp>=-6                                                                 "
					+ "           and cityid = '"
					+ cityId
					+ "'                                                                  "
					+ "           and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) >=               "
					+ "               '"
					+ begTime
					+ "'                                                                   "
					+ "           and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) <=               "
					+ "               '"
					+ endTime
					+ "'                                                                   "
					+ "         group by cellid, ncellid) t1                                                         "
					+ "  left join (select cellid, count(*) totalcnt                                                 "
					+ "               from (select cellid, meatime, count(*)                                         "
					+ "                       from rno_4g_mro_data                                                   "
					+ "                      where ncellid <> ''                                                     "
					+ " and cityid = '"
					+ cityId
					+ "'                                                     "
					+ " and from_unixtime(unix_timestamp(meatime,'yyyy/MM/dd HH:mm:ss')) >='"
					+ begTime
					+ "'"
					+ " and from_unixtime(unix_timestamp(meatime,'yyyy/MM/dd HH:mm:ss')) <='"
					+ endTime
					+ "'"
					+ " group by cellid, meatime, mmeues1apid, mmecode) t "
					+ " group by CellId) t2 "
					+ " on (t1.cellid = t2.cellid)  "
					+ " left join (select * from rno_lte_cell where area_id ="
					+ cityId
					+ ") t3 "
					+ " on(regexp_replace(t2.cellid,'-','') = t3.business_cell_id)  "
					+ " left join (select * from rno_lte_cell where area_id ="
					+ cityId
					+ ") t4 "
					+ " on(regexp_replace(t1.ncellid,'-','') = t4.business_cell_id)";
			// System.out.println("过覆盖sql ="+sql);
			weakList = RnoHelper.commonQuery(stmt, sql);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("转时出错！");
		}
		log.debug("准备计算过覆盖耗时：" + (System.currentTimeMillis() - t1) + "ms");
		return weakList;
	}

	/**
	 * 
	 * @title 保存重叠覆盖指标
	 * @param indexLists
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-5下午4:14:39
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private static boolean saveOverLayIndex(List<Map<String, Object>> indexLists, RnoLteStructAnaJobRec jobRec) {
		log.debug("进入方法：saveOverLayIndex。indexLists.size=" + indexLists.size() + ",jobRec=" + jobRec);
		BufferedWriter bw = null;
		String saveFullPath = jobRec.getTmpDir() + jobRec.getCityName() + " MRO重叠覆盖详细.csv";
		log.debug("保存重叠覆盖指标，saveFullPath=" + saveFullPath);
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFullPath), "gbk"));
			StringBuffer buf = new StringBuffer();
			// 输出重叠覆盖标题
			List<String> titles = Arrays.asList("时间", "DATATYPE", "厂家", "CELLID", "日期", "HOURS", "总采样点数",
					"大于负105dBm的采样点数", "大于负110dBm的采样点数", "MRO覆盖率大于负105dBm", "MRO覆盖率大于负110dBm", "db6内3邻区采样数",
					"db6内4邻区采样数", "db6内5邻区采样数", "db6内6邻区采样数", "db10内3邻区采样数", "db10内4邻区采样数", "db10内5邻区采样数",
					"db10内6邻区采样数", "db6内3邻区采样比例", "db6内4邻区采样比例", "db6内5邻区采样比例", "db6内6邻区采样比例", "db10内3邻区采样比例",
					"db10内4邻区采样比例", "db10内5邻区采样比例", "db10内6邻区采样比例", "无邻区采样点数", "邻区1个及以上采样数", "邻区2个及以上采样数",
					"邻区3个及以上采样数", "邻区4个及以上采样数", "邻区5个及以上采样数", "邻区6个及以上采样数", "邻区7个及以上采样数", "邻区8个及以上采样数", "邻区9个及以上采样数");
			for (String str : titles) {
				buf.append(str).append(",");
			}
			buf.deleteCharAt(buf.length() - 1);
			bw.write(buf.toString());
			bw.newLine();
			// 输出内容
			Map<String, Object> indexMap = null;
			for (int i = 0; i < indexLists.size(); i++) {
				try {
					buf.setLength(0);
					indexMap = indexLists.get(i);
					// 时间
					buf.append("").append(",");
					// 频点类型
					buf.append("仅同频").append(",");
					// 厂家
					buf.append("").append(",");
					// cellid
					buf.append(indexMap.get("cellid").toString()).append(",");
					// 日期
					buf.append("").append(",");
					// HOURS
					buf.append("").append(",");
					// 总采样点数
					buf.append(indexMap.get("totalcnt") == null ? "0" : indexMap.get("totalcnt").toString())
							.append(",");
					// 大于负105dBm的采样点数
					buf.append(indexMap.get("db105partcnt") == null ? "0" : indexMap.get("db105partcnt").toString())
							.append(",");
					// 大于负110dBm的采样点数
					buf.append(indexMap.get("db110partcnt") == null ? "0" : indexMap.get("db110partcnt").toString())
							.append(",");
					// MRO覆盖率大于负105dBm
					buf.append(
							indexMap.get("db105partcntper") == null ? "0" : indexMap.get("db105partcntper").toString())
							.append(",");
					// MRO覆盖率大于负110dBm
					buf.append(
							indexMap.get("db110partcntper") == null ? "0" : indexMap.get("db110partcntper").toString())
							.append(",");
					// db6内3邻区采样数
					buf.append(indexMap.get("db11063partcnt") == null ? "0" : indexMap.get("db11063partcnt").toString())
							.append(",");
					// db6内4邻区采样数
					buf.append(indexMap.get("db11064partcnt") == null ? "0" : indexMap.get("db11064partcnt").toString())
							.append(",");
					// db6内5邻区采样数
					buf.append(indexMap.get("db11065partcnt") == null ? "0" : indexMap.get("db11065partcnt").toString())
							.append(",");
					// db6内6邻区采样数
					buf.append(indexMap.get("db11066partcnt") == null ? "0" : indexMap.get("db11066partcnt").toString())
							.append(",");
					// db10内3邻区采样数
					buf.append(
							indexMap.get("db110103partcnt") == null ? "0" : indexMap.get("db110103partcnt").toString())
							.append(",");
					// db10内4邻区采样数
					buf.append(
							indexMap.get("db110104partcnt") == null ? "0" : indexMap.get("db110104partcnt").toString())
							.append(",");
					// db10内5邻区采样数
					buf.append(
							indexMap.get("db110105partcnt") == null ? "0" : indexMap.get("db110105partcnt").toString())
							.append(",");
					// db10内6邻区采样数
					buf.append(
							indexMap.get("db110106partcnt") == null ? "0" : indexMap.get("db110106partcnt").toString())
							.append(",");
					// db6内3邻区采样比例
					buf.append(
							indexMap.get("db11063partcntper") == null ? "0" : indexMap.get("db11063partcntper")
									.toString()).append(",");
					// db6内4邻区采样比例
					buf.append(
							indexMap.get("db11064partcntper") == null ? "0" : indexMap.get("db11064partcntper")
									.toString()).append(",");
					// db6内5邻区采样比例
					buf.append(
							indexMap.get("db11065partcntper") == null ? "0" : indexMap.get("db11065partcntper")
									.toString()).append(",");
					// db6内6邻区采样比例
					buf.append(
							indexMap.get("db11066partcntper") == null ? "0" : indexMap.get("db11066partcntper")
									.toString()).append(",");
					// db10内3邻区采样比例
					buf.append(
							indexMap.get("db110103partcntper") == null ? "0" : indexMap.get("db110103partcntper")
									.toString()).append(",");
					// db10内4邻区采样比例
					buf.append(
							indexMap.get("db110104partcntper") == null ? "0" : indexMap.get("db110104partcntper")
									.toString()).append(",");
					// db10内5邻区采样比例
					buf.append(
							indexMap.get("db110105partcntper") == null ? "0" : indexMap.get("db110105partcntper")
									.toString()).append(",");
					// db10内6邻区采样比例
					buf.append(
							indexMap.get("db110106partcntper") == null ? "0" : indexMap.get("db110106partcntper")
									.toString()).append(",");
					// 无邻区采样点数：已经将邻区为空的过滤
					buf.append("0").append(",");
					// 邻区1个及以上采样数
					buf.append(indexMap.get("db1101partcnt") == null ? "0" : indexMap.get("db1101partcnt").toString())
							.append(",");
					// 邻区2个及以上采样数
					buf.append(indexMap.get("db1102partcnt") == null ? "0" : indexMap.get("db1102partcnt").toString())
							.append(",");
					// 邻区3个及以上采样数
					buf.append(indexMap.get("db1103partcnt") == null ? "0" : indexMap.get("db1103partcnt").toString())
							.append(",");
					// 邻区4个及以上采样数
					buf.append(indexMap.get("db1104partcnt") == null ? "0" : indexMap.get("db1104partcnt").toString())
							.append(",");
					// 邻区5个及以上采样数
					buf.append(indexMap.get("db1105partcnt") == null ? "0" : indexMap.get("db1105partcnt").toString())
							.append(",");
					// 邻区6个及以上采样数
					buf.append(indexMap.get("db1106partcnt") == null ? "0" : indexMap.get("db1106partcnt").toString())
							.append(",");
					// 邻区7个及以上采样数
					buf.append(indexMap.get("db1107partcnt") == null ? "0" : indexMap.get("db1107partcnt").toString())
							.append(",");
					// 邻区8个及以上采样数
					buf.append(indexMap.get("db1108partcnt") == null ? "0" : indexMap.get("db1108partcnt").toString())
							.append(",");
					// 邻区9个及以上采样数
					buf.append(indexMap.get("db1109partcnt") == null ? "0" : indexMap.get("db1109partcnt").toString());
					bw.write(buf.toString());
					bw.newLine();
				} catch (Exception e) {
					// 捕获单行错误，过滤
				}
			}
			bw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long fileSize = new File(saveFullPath).length();
		String fileSizeStr = "";
		if (fileSize < 1024) {
			fileSizeStr = fileSize + "B";
		} else if (fileSize < 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 + "K";
		} else if (fileSize < 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 + "M";
		} else if (fileSize < 1024 * 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 + "G";
		} else {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 / 1024 + "T";
		}
		log.debug("退出方法：saveOverLayIndex。fileSize=" + fileSizeStr);
		return true;
	}

	/**
	 * 
	 * @title 保存过覆盖指标
	 * @param indexLists
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-6上午9:56:11
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private static boolean saveOverCoverIndex(List<Map<String, Object>> indexLists, RnoLteStructAnaJobRec jobRec) {
		log.debug("进入方法：saveOverCoverIndex。indexLists.size=" + indexLists.size() + ",jobRec=" + jobRec);
		BufferedWriter bw = null;
		String saveFullPath = jobRec.getTmpDir() + jobRec.getCityName() + " MRO过覆盖详细.csv";
		log.debug("保存过覆盖指标，saveFullPath=" + saveFullPath);
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFullPath), "gbk"));
			StringBuffer buf = new StringBuffer();
			// 输出过覆盖标题
			List<String> titles = Arrays.asList("CELLID", "小区名", "经度", "纬度", "邻区PCI", "邻区ID", "邻区名", "邻区经度", "邻区纬度",
					"总采样点数", "邻区采样点数", "采样比例", "距离", "理想覆盖距离");
			for (String str : titles) {
				buf.append(str).append(",");
			}
			buf.deleteCharAt(buf.length() - 1);
			bw.write(buf.toString());
			bw.newLine();
			// 输出内容
			Map<String, Object> indexMap = null;
			for (int i = 0; i < indexLists.size(); i++) {
				try {
					buf.setLength(0);
					indexMap = indexLists.get(i);
					// cellid
					buf.append(indexMap.get("cellid").toString()).append(",");
					// 小区名
					buf.append(indexMap.get("cellname").toString()).append(",");
					// cell 经度
					buf.append(indexMap.get("celllon").toString()).append(",");
					// cell 纬度
					buf.append(indexMap.get("celllat").toString()).append(",");
					// ncell PCI
					buf.append(indexMap.get("ltencpci").toString()).append(",");
					// ncellid
					buf.append(indexMap.get("ncellid").toString()).append(",");
					// 邻区名
					buf.append(indexMap.get("ncellname").toString()).append(",");
					// ncell 经度
					buf.append(indexMap.get("ncelllon").toString()).append(",");
					// ncell 纬度
					buf.append(indexMap.get("ncelllat").toString()).append(",");
					// 总采样点数
					buf.append(indexMap.get("totalcnt") == null ? "0" : indexMap.get("totalcnt").toString())
							.append(",");
					// 邻区采样点数
					buf.append(indexMap.get("ncellcnt") == null ? "0" : indexMap.get("ncellcnt").toString())
							.append(",");
					// 邻区采样比例
					buf.append(indexMap.get("ncellper") == null ? "0" : indexMap.get("ncellper").toString())
							.append(",");
					// 距离
					buf.append(indexMap.get("dis").toString()).append(",");
					// 理想距离
					buf.append(indexMap.get("stspacing").toString());
					bw.write(buf.toString());
					bw.newLine();
				} catch (Exception e) {
					// 捕获单行错误，不处理
				}
			}
			bw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long fileSize = new File(saveFullPath).length();
		String fileSizeStr = "";
		if (fileSize < 1024) {
			fileSizeStr = fileSize + "B";
		} else if (fileSize < 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 + "K";
		} else if (fileSize < 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 + "M";
		} else if (fileSize < 1024 * 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 + "G";
		} else {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 / 1024 + "T";
		}
		log.debug("退出方法：saveOverCoverIndex。fileSize=" + fileSizeStr);
		return true;
	}

	/**
	 * 
	 * @title 保存MRO指标汇总
	 * @param indexSumLists
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-6上午9:40:15
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private static boolean saveIndexSum(List<Map<String, Object>> indexSumLists, RnoLteStructAnaJobRec jobRec) {
		log.debug("进入方法：saveIndexSum。indexSumLists.size=" + indexSumLists.size() + ",jobRec=" + jobRec);
		BufferedWriter bw = null;
		String saveFullPath = jobRec.getTmpDir() + jobRec.getCityName() + " MRO指标汇总.csv";
		log.debug("保存指标汇总，saveFullPath=" + saveFullPath);
		try {
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFullPath), "gbk"));
			StringBuffer buf = new StringBuffer();
			// 输出指标汇总标题
			List<String> titles = Arrays.asList("CELLID", "小区名", "弱覆盖", "重叠覆盖", "过覆盖", "过覆盖数", "1.6过覆盖", "1.6过覆盖数");
			for (String str : titles) {
				buf.append(str).append(",");
			}
			buf.deleteCharAt(buf.length() - 1);
			bw.write(buf.toString());
			bw.newLine();
			// 输出内容
			Map<String, Object> indexMap = null;
			for (int i = 0; i < indexSumLists.size(); i++) {
				try {
					buf.setLength(0);
					indexMap = indexSumLists.get(i);
					buf.append(indexMap.get("cellid").toString()).append(",");
					buf.append(indexMap.get("cellname").toString()).append(",");
					buf.append(indexMap.get("weakflag").toString()).append(",");
					buf.append(indexMap.get("overlayflag").toString()).append(",");
					buf.append(
							indexMap.get("overcovergroupflag") == null ? "否" : indexMap.get("overcovergroupflag")
									.toString()).append(",");
					buf.append(
							indexMap.get("overcovergroupcnt") == null ? "0" : indexMap.get("overcovergroupcnt")
									.toString()).append(",");
					buf.append(
							indexMap.get("overcoverprovinflag") == null ? "否" : indexMap.get("overcoverprovinflag")
									.toString()).append(",");
					buf.append(indexMap.get("overcoverprovincnt") == null ? "0" : indexMap.get("overcoverprovincnt")
							.toString());
					bw.write(buf.toString());
					bw.newLine();
				} catch (Exception e) {
				}
			}
			bw.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null) {
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		long fileSize = new File(saveFullPath).length();
		String fileSizeStr = "";
		if (fileSize < 1024) {
			fileSizeStr = fileSize + "B";
		} else if (fileSize < 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 + "K";
		} else if (fileSize < 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 + "M";
		} else if (fileSize < 1024 * 1024 * 1024 * 1024) {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 + "G";
		} else {
			fileSizeStr = (double) fileSize / 1024 / 1024 / 1024 / 1024 + "T";
		}
		log.debug("退出方法：saveIndexSum。fileSize=" + fileSizeStr);
		return true;
	}

	/**
	 * 
	 * @title 保存压缩LTE结构分析结果文件
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-6上午9:41:44
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private ResultInfo saveLteStructAnaResult(RnoLteStructAnaJobRec jobRec) {

		ResultInfo resultInfo = new ResultInfo();
		resultInfo.setFlag(true);

		boolean ok;
		// ---------输出结果------//
		log.debug("准备输出lte结构分析结果...");

		String tmpDir = jobRec.getTmpDir();
		try {
			// 将用户文件进行压缩，并转移到最终目录
			String realUserDataName = jobRec.getDownLoadFileName();
			if (StringUtils.isBlank(realUserDataName)) {
				realUserDataName = jobRec.getCityName() + "_LTE结构分析指标.zip";
			}
			String realDataDir = jobRec.getResultDir();
			if (!StringUtils.endsWith(realDataDir, "/") && !StringUtils.endsWith(realDataDir, "\\")) {
				realDataDir += "/";
			}
			String realUserDataFullPath = realDataDir + realUserDataName;
			log.debug("LTE结构分析结果供用户下载的结果的最终保存路径：" + realUserDataFullPath);

			ok = ZipFileHandler.zip(tmpDir, "", realUserDataFullPath);
			log.debug("LTE结构分析结果供用户下载的结果保存情况：" + ok);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 删除临时目录
			try {
				FileTool.deleteDir(tmpDir);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return resultInfo;

	}
}
