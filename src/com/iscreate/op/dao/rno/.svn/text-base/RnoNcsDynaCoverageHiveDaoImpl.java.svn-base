package com.iscreate.op.dao.rno;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.iscreate.op.dao.rno.AuthDsDataDaoImpl.SysArea;
import com.iscreate.op.pojo.rno.ResultInfo;
import com.iscreate.op.pojo.rno.RnoLteAzimuthAssessJobRec;
import com.iscreate.op.pojo.rno.RnoLteStructAnaJobRec;
import com.iscreate.op.service.rno.job.JobReport;
import com.iscreate.op.service.rno.task.angle.RnoLteAzimuthAssessJobRunnable;
import com.iscreate.op.service.rno.task.structana.RnoLteStructAnaJobRunnable;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.FileTool;
import com.iscreate.op.service.rno.tool.RnoHelper;
import com.iscreate.op.service.rno.tool.ZipFileHandler;

public class RnoNcsDynaCoverageHiveDaoImpl implements RnoNcsDynaCoverageHiveDao {

	private static Log log=LogFactory.getLog(RnoNcsDynaCoverageHiveDaoImpl.class);

	/**
	 * 
	 * @title LTE方位角评估分析入口
	 * @param worker
	 * @param connection
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-16下午5:45:53
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public ResultInfo doLteAzimuthAssess(
			RnoLteAzimuthAssessJobRunnable worker,
			Connection connection, RnoLteAzimuthAssessJobRec jobRec) {
		log.debug("doLteAzimuthAssess当前运行线程：" + Thread.currentThread());
		Statement stmt = null;
		ResultInfo result = new ResultInfo(true);
		if(jobRec==null){
			result.setFlag(false);
			result.setMsg("LTE方位角评估分析任务对象为空！");
			return result;
		}
		try {
			stmt = connection.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
			result.setFlag(false);
			result.setMsg("准备HIVE数据仓库连接失败！");
			return result;
		}
		// 准备工作路径
		log.debug("准备LTE方位角评估分析结果保存的路径...");
		String tmpParentDir = System.getProperty("java.io.tmpdir");
		String tmpDir = tmpParentDir + "/"
				+ UUID.randomUUID().toString().replaceAll("-", "") + "/";
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
		long t1;
		long t2;
		Date stepBegTime, stepEndTime;
		JobReport report = new JobReport(jobRec.getJobId());
		log.debug("汇总MRO>>>>>>>>>>>>>>>开始计算方位角评估...");
		t1 = System.currentTimeMillis();
		// sourceTab,targetTab
		stepBegTime = new Date();
		boolean ok = true;//具体工作内容
		/*-------------------------------计算方位角评估数据------------------------------------------*/
		List<Map<String, Object>> indexLists = calcAzimuthAssess(worker, report, stmt, jobRec);
		/*-------------------------------保存方位角评估数据------------------------------------------*/
		saveAzimuthAssess(indexLists, jobRec);
		/*-------------------------------压缩方位角评估数据------------------------------------------*/
		saveLteAzimuthAssessResult(jobRec);
		stepEndTime = new Date();
		t2 = System.currentTimeMillis();
		log.debug("汇总MRO<<<<<<<<<<<<<<<完成计算LTE方位角评估，结果:" + ok + ",耗时："
				+ (t2 - t1));
		return result;
	}
	/**
	 * 
	 * @title 计算方位角评估
	 * @param worker
	 * @param report
	 * @param stmt
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-17下午4:55:14
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public List<Map<String, Object>> calcAzimuthAssess(RnoLteAzimuthAssessJobRunnable worker,JobReport report,
			Statement stmt, RnoLteAzimuthAssessJobRec jobRec) {
		log.debug("calcAzimuthAssess 计算方位角评估当前运行线程：" + Thread.currentThread());
		
		long t1 = System.currentTimeMillis();
		long t2 = t1;
		DateUtil dateUtil = new DateUtil();
		Long cityId = jobRec.getCityId();
		String begTime = dateUtil.format_yyyyMMdd(jobRec.getBegMeaTime());
		String endTime = dateUtil.format_yyyyMMdd(jobRec.getEndMeaTime());
		String sql = "";
		sql =    "select cellid,calangle,sysangle,if(abs(calangle-sysangle)>180,360-abs(calangle-sysangle),abs(calangle-sysangle)) diffval from (																	"
				+"select cellid,if(calangle<0,calangle+360,calangle) calangle,sysangle from (                                                                                                                       "
				+"select t1.cellid,if(t1.ycell>=0,asin(t1.xcell/sqrt(pow(t1.xcell,2)+pow(t1.ycell,2)))*57.295,180-asin(t1.xcell/sqrt(pow(t1.xcell,2)+pow(t1.ycell,2)))*57.295) calangle,t2.azimuth sysangle from (  "
				+"select cellid,sum(sin(360-ltescaoa/2)*cnt) xcell,sum(cos(360-ltescaoa/2)*cnt) ycell from (                                                                                                        "
				+"select cellid,ltescaoa, count(*) cnt                                                                                                                                                              "
				+"                                           from (select cellid,min(ltescaoa) ltescaoa,                                                                                                            "
				+"                                                        meatime,                                                                                                                                  "
				+"                                                        count(*) cnt                                                                                                                              "
				+"                                                   from rno_4g_mro_data                                                                                                                           "
				+"                                                  where ncellid <> '' and ltescaoa <> ''                                                                                                          "
				+"                                                    and cityid = '"+cityId+"'                                                                                                                             "
				+"                                                    and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) >='"+begTime+"'                                                              "
				+"                                                    and from_unixtime(unix_timestamp(meatime, 'yyyy/MM/dd HH:mm:ss')) <='"+endTime+"'                                                              "
				+"                                                  group by cellid,                                                                                                                                "
				+"                                                           meatime,                                                                                                                               "
				+"                                                           mmeues1apid,                                                                                                                           "
				+"                                                           mmecode) t                                                                                                                             "
				+"                                          group by CellId,ltescaoa                                                                                                                                "
				+"                                          ) t                                                                                                                                                     "
				+"                                          group by cellid                                                                                                                                         "
				+"                                          ) t1                                                                                                                                                    "
				+"                                          left join (select * from rno_lte_cell where area_id ="+cityId+") t2 on(regexp_replace(t1.cellid,'-','') = t2.business_cell_id)                                                                    "
				+"                                          )t                                                                                                                                                      "
				+"                                          )t                                                                                                                                                      ";
		List<Map<String, Object>> azimuthList = null;
		try {
			azimuthList = RnoHelper.commonQuery(stmt, sql);
		} catch (Exception e) {
			e.printStackTrace();
			log.error("转时出错！");
			try {
				stmt.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
		}finally{
			
		}
		t2 = System.currentTimeMillis();
		log.debug("准备计算方位角评估耗时：" + (t2 - t1) + "ms");
		return azimuthList;
	}
	/**
	 * 
	 * @title 保存方位角评估
	 * @param indexLists
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-17下午5:00:16
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private static boolean saveAzimuthAssess(
			List<Map<String, Object>> indexLists, RnoLteAzimuthAssessJobRec jobRec) {

		BufferedWriter bw = null;
		DateUtil dateUtil = new DateUtil();
		String tmpDir = jobRec.getTmpDir();
		String cityName = jobRec.getCityName();
		String saveFullPath = tmpDir+"/"+cityName+" MRO方位角评估报告.csv";
		try {
			bw = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(saveFullPath), "gbk"));
			StringBuffer buf = new StringBuffer();
			// 输出标题
			List<String> titles = Arrays.asList("CELLID", "评估方位角", "工参方位角",
					"差值");
			for (String str : titles) {
				buf.append(str).append(",");
			}
			buf.deleteCharAt(buf.length() - 1);
			bw.write(buf.toString());
			bw.newLine();
			// 输出内容
			Object val = null;
			Map<String, Object> indexMap = null;
			java.text.DecimalFormat   df   =new   java.text.DecimalFormat("#.00");
			for (int i = 0; i < indexLists.size(); i++) {
				buf.setLength(0);
				indexMap = indexLists.get(i);
				//cellid
				buf.append(indexMap.get("cellid").toString()).append(",");
				//评估方位角
				buf.append(indexMap.get("calangle") == null ? "0":df.format(Double.parseDouble(indexMap.get("calangle").toString()))).append(",");
				//工参方位角
				buf.append(indexMap.get("sysangle") == null ? "0":indexMap.get("sysangle").toString()).append(",");
				//差值
				buf.append(indexMap.get("diffval") == null ? "0":df.format(Double.parseDouble(indexMap.get("diffval").toString())));
				bw.write(buf.toString());
				bw.newLine();
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

		return true;
	}
	/**
	 * 
	 * @title 保存压缩LTE方位角评估分析结果文件
	 * @param jobRec
	 * @return
	 * @author chao.xj
	 * @date 2015-11-17下午5:05:07
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	private ResultInfo saveLteAzimuthAssessResult(
			RnoLteAzimuthAssessJobRec jobRec) {

		ResultInfo resultInfo = new ResultInfo();
		resultInfo.setFlag(true);

		boolean ok;
		// ---------输出结果------//
		log.debug("准备输出lte方位角评估分析结果...");

		String tmpDir = jobRec.getTmpDir();
		String cityName = jobRec.getCityName();
		try {
			// 将用户文件进行压缩，并转移到最终目录
			String realUserDataName = jobRec.getDownLoadFileName();
			if (StringUtils.isBlank(realUserDataName)) {
				realUserDataName = jobRec.getCityName() + "_MRO方位角评估报告.zip";
			}
			String realDataDir = jobRec.getResultDir();
			if (!StringUtils.endsWith(realDataDir, "/")
					&& !StringUtils.endsWith(realDataDir, "\\")) {
				realDataDir += "/";
			}
			String realUserDataFullPath = realDataDir + realUserDataName;
			log.debug("LTE方位角评估分析结果供用户下载的结果的最终保存路径：" + realUserDataFullPath);

			ok = ZipFileHandler.zip(tmpDir, "",
					realUserDataFullPath);
			log.debug("LTE方位角评估分析结果供用户下载的结果保存情况：" + ok);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// 删除临时目录
			try {
				FileTool.deleteDir(tmpDir);
			} catch (Exception e2) {
				// TODO: handle exception
				e2.printStackTrace();
			}
			// System.gc();
		}
		return resultInfo;

	}
}
