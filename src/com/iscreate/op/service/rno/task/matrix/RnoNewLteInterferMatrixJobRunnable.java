package com.iscreate.op.service.rno.task.matrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FilterList.Operator;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.jobcontrol.ControlledJob;
import org.apache.hadoop.mapreduce.lib.jobcontrol.JobControl;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.dao.rno.Rno4GPciDaoImpl;
import com.iscreate.op.dao.rno.RnoLtePciDaoImpl;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.mapreduce.IscreateHadoopBaseJob;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForHo;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForMr;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForSf;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForHo;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForMr;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForSf;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerSum;
import com.iscreate.op.service.rno.task.MapreduceBaseJobRunnable;
import com.iscreate.op.service.rno.tool.DateUtil;
import com.iscreate.op.service.rno.tool.HBTable;

public class RnoNewLteInterferMatrixJobRunnable extends MapreduceBaseJobRunnable {

	private static Logger log = LoggerFactory.getLogger(RnoNewLteInterferMatrixJobRunnable.class);
	private static String jobType = "CALC_LTE_INTERFER_MATRIX_NEW";

	private static List<String> mrTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// MR数据
			add(HBTable.valueOf("RNO_4G_ERI_MR"));
			add(HBTable.valueOf("RNO_4G_ZTE_MR"));
		}
	};
	private static List<String> hoTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// HO数据
			add(HBTable.valueOf("RNO_4G_ERI_HO"));
			add(HBTable.valueOf("RNO_4G_ZTE_HO"));
		}
	};

	private static List<String> sfTableNames = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			// SF数据
			add(HBTable.valueOf("RNO_4G_SF"));
		}
	};

	Rno4GPciDaoImpl rno4gPciDao = null;
	RnoLtePciDaoImpl rnoLtePciDao = null;
	private long matrixRecId = -1;

	List<String> fileNameList = new ArrayList<String>();
	/** 文件名与ID的映射 **/
	Map<String, String> fn2Id = new HashMap<String, String>();

	boolean isUseSf = false;

	@Override
	public boolean isMyJob(JobProfile job) {
		if (job == null)
			return false;
		else {
			return jobType.equals(job.getJobType());
		}
	}

	public JobStatus runJobInternal() {
		log.info("pci优化干扰矩阵任务开始");
		// int n=0;
		rno4gPciDao = new Rno4GPciDaoImpl();
		rnoLtePciDao = new RnoLtePciDaoImpl();
		// ---------准备数据库连接----------//
		log.debug("PCI 4g 干扰矩阵的job准备数据库连接");
		Date startTime = new Date();

		// 通过jobId获取干扰矩阵计算记录信息
		List<Map<String, Object>> pciPlanRec = rno4gPciDao.query4GInterferMatrixRecByJobId(updateStatusStmt, jobId);
		if (pciPlanRec.size() <= 0) {
			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			updateOwnProgress(status);
			return status;
		}
		matrixRecId = Long.parseLong(pciPlanRec.get(0).get("MARTIX_DESC_ID").toString());

		//
		String filePath = pciPlanRec.get(0).get("FILE_PATH").toString();
		String matrixName = pciPlanRec.get(0).get("FILE_NAME").toString();
		long cityId = Long.parseLong(pciPlanRec.get(0).get("CITY_ID").toString());
		String startMeaDate = pciPlanRec.get(0).get("START_MEA_DATE").toString();
		String endMeaDate = pciPlanRec.get(0).get("END_MEA_DATE").toString();
		// String fileName = pciPlanRec.get(0).get("FILE_NAME").toString();
		DateUtil dateUtil = new DateUtil();
		long startMeaMillis = dateUtil.parseDateArbitrary(startMeaDate).getTime();
		long endMeaMillis = dateUtil.parseDateArbitrary(endMeaDate).getTime();

		String samefreqcellcoefweight = "0.8"; // 权值
		String switchratioweight = "0.2"; // 切换比例权值
		if (pciPlanRec.get(0).get("SAMEFREQCELLCOEFWEIGHT") != null
				&& pciPlanRec.get(0).get("SWITCHRATIOWEIGHT") != null) {
			samefreqcellcoefweight = pciPlanRec.get(0).get("SAMEFREQCELLCOEFWEIGHT").toString();
			switchratioweight = pciPlanRec.get(0).get("SWITCHRATIOWEIGHT").toString();
		}

		// SF_FILES,MR_JOB_ID
		fileNameList = Arrays.asList(null == pciPlanRec.get(0).get("SF_FILES") ? new String[0] : pciPlanRec.get(0)
				.get("SF_FILES").toString().trim().split(","));

		if (!fileNameList.isEmpty() && !fileNameList.get(0).isEmpty()) {
			String inStr = "";
			for (String fn : fileNameList) {
				inStr += "'" + fn + "',";
			}
			inStr = inStr.substring(0, inStr.length() - 1);
			inStr = "(" + inStr + ")";
			String sql = "select file_name,rec_id from rno_lte_sf_file_rec where file_name in " + inStr
					+ " and area_id=" + cityId + " order by mod_time";
			log.debug("查询文件名sql:{}", sql);
			ResultSet rs;
			try {
				rs = updateStatusStmt.executeQuery(sql);
				while (rs.next()) {
					fn2Id.put(rs.getString(1), "fn*" + rs.getLong(2) + "*fn");
				}
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			isUseSf = true;
		}

		// 通过城市ID获取从基站标识到lte小区的映射集合：为的获取同站其他小区
		// 同站小区判断条件变更由 enodeb->enodeb+earfcn
		Map<String, String> enodebToCells = rno4gPciDao.getEnodebIdForCellsMap(stmt, cityId);
		try {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
		} catch (SQLException e2) {
			e2.printStackTrace();
		}
		// Map<String, Map<String, String>> enodebToCellsWithLonLat = rno4gPciDao.getEnodebIdForCellsMapWithLonLat(stmt,
		// cityId);
		if (enodebToCells == null) {
			// if (enodebToCellsWithLonLat == null) {
			report.setStage("通过城市ID获取从基站标识到lte小区的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");
			addJobReport(report);

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}
		// 获取工参

		Map<String, Object> cellToParameter = rnoLtePciDao.getParameterForCellsMap(updateStatusStmt, cityId);

		if (cellToParameter == null || cellToParameter.size() == 0) {
			report.setStage("通过城市ID获取小区到经纬度的映射集合");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("该区域下的lte小区数据不存在");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		/************ PCI干扰计算 start ************/
		Configuration conf = new YarnConfiguration();
		conf.set("CALC_TYPE", "MATRIX");
		log.debug("准备pci优化干扰矩阵的mapreduce任务...");

		// 工参中的小区列表，用于匹配数据 不进行工参过滤
		// conf.set("cellList", map2String((List<String>) cellToParameter.get("cellList")));

		double dislimit = 5000;
		conf.setDouble("dislimit", dislimit);

		// String nodebs = map2String(enodebToCells);
		// 设置同站小区的公参数据
		// conf.set("enodebToCells", nodebs);

		conf.set("samefreqcellcoefweight", samefreqcellcoefweight);
		conf.set("switchratioweight", switchratioweight);
		// String pciOriPath = filePath + "/" + fileName;
		// conf.set("DATA_FILE_PATH", pciOriPath);
		// 更新pci优化干扰矩阵状态
		updateOwnProgress("正在计算");

		log.debug("组装rowkey：cityId=" + cityId + ",开始日期毫秒表示=" + startMeaMillis + ",结束日期毫秒表示=" + endMeaMillis);

		try {
			List<Path> paths = new ArrayList<Path>();
			List<ControlledJob> jobList = new ArrayList<ControlledJob>();

			// 计算mr矩阵
			Job mrDataJob = Job.getInstance(conf, "MrDataJob");
			mrDataJob.setJarByClass(IscreateHadoopBaseJob.class);
			// SequenceFileOutputFormat 作为hadoop的一种高效的文件格式
			// 在使用过程中输出格式与后续的任务要匹配
			mrDataJob.setOutputKeyClass(LongWritable.class);
			mrDataJob.setOutputValueClass(Text.class);
			mrDataJob.setOutputFormatClass(SequenceFileOutputFormat.class);
			mrDataJob.setReducerClass(PciReducerForMr.class);
			mrDataJob.setNumReduceTasks(1);
			TableMapReduceUtil.initTableMapperJob(getScans(cityId, startMeaMillis, endMeaMillis, mrTableNames),
					PciMapperForMr.class, Text.class, Text.class, mrDataJob);
			Path mrPath = new Path(filePath + "/mrout");
			paths.add(mrPath);
			FileOutputFormat.setOutputPath(mrDataJob, mrPath);
			SequenceFileOutputFormat.setOutputCompressionType(mrDataJob, CompressionType.NONE);
			ControlledJob mrcj = new ControlledJob(conf);
			mrcj.setJob(mrDataJob);
			jobList.add(mrcj);

			// 计算ho矩阵
			Job hoDataJob = Job.getInstance(conf, "HoDataJob");
			hoDataJob.setJarByClass(IscreateHadoopBaseJob.class);
			// SequenceFileOutputFormat 作为hadoop的一种高效的文件格式
			// 在使用过程中输出格式与后续的任务要匹配
			hoDataJob.setOutputKeyClass(LongWritable.class);
			hoDataJob.setOutputValueClass(Text.class);
			hoDataJob.setOutputFormatClass(SequenceFileOutputFormat.class);
			hoDataJob.setReducerClass(PciReducerForHo.class);
			hoDataJob.setNumReduceTasks(1);
			TableMapReduceUtil.initTableMapperJob(getScans(cityId, startMeaMillis, endMeaMillis, hoTableNames),
					PciMapperForHo.class, Text.class, Text.class, hoDataJob);
			Path hoPath = new Path(filePath + "/hoout");
			paths.add(hoPath);
			FileOutputFormat.setOutputPath(hoDataJob, hoPath);
			SequenceFileOutputFormat.setOutputCompressionType(hoDataJob, CompressionType.NONE);
			ControlledJob hocj = new ControlledJob(conf);
			hocj.setJob(hoDataJob);
			jobList.add(hocj);

			// 计算sf矩阵
			Job sfDataJob = Job.getInstance(conf, "SfDataJob");
			sfDataJob.setJarByClass(IscreateHadoopBaseJob.class);
			// SequenceFileOutputFormat 作为hadoop的一种高效的文件格式
			// 在使用过程中输出格式与后续的任务要匹配
			sfDataJob.setOutputKeyClass(LongWritable.class);
			sfDataJob.setOutputValueClass(Text.class);
			sfDataJob.setOutputFormatClass(SequenceFileOutputFormat.class);
			sfDataJob.setReducerClass(PciReducerForSf.class);
			sfDataJob.setNumReduceTasks(1);
			TableMapReduceUtil.initTableMapperJob(getScans(cityId, startMeaMillis, endMeaMillis, sfTableNames),
					PciMapperForSf.class, Text.class, Text.class, sfDataJob);
			Path sfPath = new Path(filePath + "/sfout");
			paths.add(sfPath);
			FileOutputFormat.setOutputPath(sfDataJob, sfPath);
			SequenceFileOutputFormat.setOutputCompressionType(sfDataJob, CompressionType.NONE);
			ControlledJob sfcj = new ControlledJob(conf);
			sfcj.setJob(sfDataJob);
			jobList.add(sfcj);

			// 汇总
			Job allDataJob = Job.getInstance(conf, "AllDataJob");
			allDataJob.setJarByClass(IscreateHadoopBaseJob.class);
			// SequenceFileInputFormat作为中间文件的格式，这是一种高效的格式
			// mapper只用于对key排序和规整，用系统默认的NewOutputCollector就好，需要设置Map的输出格式与上个任务的输出格式匹配
			// 同时reducer的输入格式也要匹配，这个体现在reducer类中
			allDataJob.setMapOutputKeyClass(LongWritable.class);
			allDataJob.setMapOutputValueClass(Text.class);
			allDataJob.setInputFormatClass(SequenceFileInputFormat.class);
			allDataJob.setOutputKeyClass(NullWritable.class);
			allDataJob.setOutputValueClass(Text.class);
			allDataJob.setOutputFormatClass(TextOutputFormat.class);
			allDataJob.setReducerClass(PciReducerSum.class);
			allDataJob.setNumReduceTasks(1);
			FileInputFormat.setInputPaths(allDataJob, paths.toArray(new Path[paths.size()]));
			Path path = new Path(filePath + "/allout");
			FileOutputFormat.setOutputPath(allDataJob, path);
			ControlledJob allcj = new ControlledJob(conf);
			allcj.setJob(allDataJob);
			allcj.addDependingJob(mrcj);
			allcj.addDependingJob(hocj);
			allcj.addDependingJob(sfcj);
			jobList.add(allcj);

			// 提交任务
			JobControl jobCtrl = new JobControl("myctrl");
			jobCtrl.addJobCollection(jobList);

			new Thread(jobCtrl, "test").start();

			String mrJobId = allcj.getJobID().toString();
			// 获取 reduce 的 jobid 存储入 lte_pci_job 表，以备后面获取停止该 job
			// 通过主进程 jobId 更新 lte_pci_job 表中 MR_JOB_ID（MapReduce 的 jobId）
			rnoLtePciDao.addMapReduceJobId(updateStatusStmt, jobId, mrJobId, "MARTIX");

			// 通过 mrjobID 存储job对象，
			log.debug("+++>>>当前 mrJobId 为:" + mrJobId + "-----当前 mrJob 为: " + jobCtrl.getRunningJobList() + " 的信息!");

			// 等待任务完成
			int progMonitorPollIntervalMillis = Job.getProgressPollInterval(conf) * 30;
			while (!jobCtrl.allFinished()) {
				for (ControlledJob cj : jobCtrl.getRunningJobList()) {
					log.debug(cj.getMessage());
				}
				Thread.sleep(progMonitorPollIntervalMillis);
			}
			System.out.println(jobCtrl.getSuccessfulJobList());
			jobCtrl.stop();
			log.info("pci优化干扰矩阵MR集群运算完成");

			if (jobCtrl.getSuccessfulJobList().size() == jobList.size()) {
				// 计算正确
				log.info("集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：任务成功！");
				log.info("pci优化干扰矩阵完成");
				// 工参
				@SuppressWarnings("unchecked")
				Map<String, Integer> cellToOriPci = (Map<String, Integer>) cellToParameter.get("cellToOriPci");
				@SuppressWarnings("unchecked")
				Map<String, Integer> cellToEarfcn = (Map<String, Integer>) cellToParameter.get("cellToEarfcn");
				// 标题头
				String line = "小区标识," + "邻区标识," + "关联度," + "小区PCI," + "邻区PCI," + "小区频点," + "邻区频点," + "MR关联度,"
						+ "HO关联度," + "扫频关联度";
				// 文件流
				FileSystem fs = FileSystem.get(conf);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(filePath + "/"
						+ matrixName), true)));
				// 写入标题头
				// bw.write(line);
				// bw.newLine();
				// 源文件不存在，不能提供下载
				int cnt = 0;
				if (fs.exists(path)) {
					BufferedReader br = null;
					for (Path realPath : FileUtil.stat2Paths(fs.listStatus(path))) {
						if (realPath.getName().startsWith("part-")) {
							br = new BufferedReader(new InputStreamReader(fs.open(realPath), "utf-8"));// 从字符输入流中读取文件中的内容,封装了一个new
							Integer pci1;
							Integer pci2;
							Integer earfcn1;
							Integer earfcn2;
							StringBuilder sb = new StringBuilder();
							while ((line = br.readLine()) != null) {
								String[] lineArr = line.split("#");
								if (null != lineArr && lineArr.length == 6 && !lineArr[0].isEmpty()) {
									pci1 = cellToOriPci.get(lineArr[0]);
									pci2 = cellToOriPci.get(lineArr[1]);
									earfcn1 = cellToEarfcn.get(lineArr[0]);
									earfcn2 = cellToEarfcn.get(lineArr[1]);
									// 过滤空值
									if (pci1 == null || pci2 == null || earfcn1 == null || earfcn2 == null) {
										continue;
									}
									sb.setLength(0);
									sb.append(lineArr[0]).append(",");
									sb.append(lineArr[1]).append(",");
									sb.append(lineArr[2]).append(",");
									sb.append(pci1).append(",");
									sb.append(pci2).append(",");
									sb.append(earfcn1).append(",");
									sb.append(earfcn2).append(",");
									sb.append(lineArr[3]).append(",");
									sb.append(lineArr[4]).append(",");
									sb.append(lineArr[5]);
									bw.write(sb.toString());
									bw.newLine();
								}
							}
							if (null != br) {
								br.close();
							}
							cnt++;
						}
					}
				}
				if (cnt > 0) {
					log.info("4g矩阵计算的源数据文件转化完成！currentFileName=" + path.toString());
					// 保存报告信息
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("成功");
					report.setAttMsg("集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：任务成功！");
					addJobReport(report);

					updateOwnProgress("计算完成");
					status.setJobState(JobState.Finished);
					status.setUpdateTime(new Date());
				} else {
					log.info("4g矩阵计算的源数据文件不存在！currentFileName=" + path.toString());
					// 保存报告信息
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("失败");
					report.setAttMsg("集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：读取结果文件失败！");
					addJobReport(report);

					updateOwnProgress("计算失败");
					status.setJobState(JobState.Failed);
					status.setUpdateTime(new Date());
				}
				bw.close();
				fs.close();
			} else {
				log.info("集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：任务失败！");
				log.info("pci优化干扰矩阵失败");
				// 保存报告信息
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg("集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：任务失败！");
				addJobReport(report);

				updateOwnProgress("计算失败");
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		/************ pci优化干扰矩阵，并保存结果文件 end ************/
		log.info("pci优化干扰矩阵任务结束");
		return status;
	}

	@Override
	public void releaseRes() {
		rno4gPciDao = null;
		super.releaseRes();
	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		// 更新pci优化干扰矩阵表的进度
		updateOwnProgress(jobStatus.getJobState().getCode());
	}

	public void updateOwnProgress(String jobStatus) {
		if (updateStatusStmt != null && rno4gPciDao != null) {
			// 更新pci优化干扰矩阵表的进度
			rno4gPciDao.update4GInterMatrixWorkStatusByStmt(updateStatusStmt, matrixRecId, jobStatus);
		}
	}

	public static String map2String(Map<String, String> enodebToCells) {
		String str = "";
		for (String key : enodebToCells.keySet()) {
			str += key + "=" + enodebToCells.get(key) + "|";
		}
		return str.substring(0, str.length() - 1);
	}

	private List<Scan> getScans(long cityId, long startMeaMillis, long endMeaMillis, List<String> tableNames) {
		List<Scan> scans = new ArrayList<Scan>();
		for (String tableName : tableNames) {
			Scan scan = getScan(cityId, startMeaMillis, endMeaMillis, tableName);
			scans.add(scan);
		}
		return scans;
	}

	private Scan getScan(long cityId, long startMeaMillis, long endMeaMillis, String tableName) {
		Scan scan = new Scan();
		// 加入这两句速度快很多
		scan.setCacheBlocks(false);
		scan.setCaching(1000);
		scan.setStartRow(Bytes.toBytes(cityId + "_" + startMeaMillis + "_#"));
		scan.setStopRow(Bytes.toBytes(cityId + "_" + endMeaMillis + "_~"));
		scan.setAttribute(Scan.SCAN_ATTRIBUTES_TABLE_NAME, Bytes.toBytes(tableName));
		if (isUseSf && tableName.endsWith("SF")) {
			List<Filter> filters = new ArrayList<Filter>();
			for (String fileName : fileNameList) {
				filters.add(new RowFilter(CompareOp.EQUAL, new SubstringComparator(fn2Id.get(fileName))));
			}
			scan.setFilter(new FilterList(Operator.MUST_PASS_ONE, filters));
		}
		return scan;
	}

	/**
	 * 拼装configuration传递的数据
	 * 
	 * @param cellList
	 * @return
	 */
	public static String map2String(List<String> cellList) {
		StringBuffer sb = new StringBuffer();
		for (String key : cellList) {
			sb.append(key).append("#");
		}
		return sb.substring(0, sb.length() - 1);
	}
}
