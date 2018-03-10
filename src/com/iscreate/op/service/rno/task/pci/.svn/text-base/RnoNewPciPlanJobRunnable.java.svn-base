package com.iscreate.op.service.rno.task.pci;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
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
import org.apache.hadoop.util.StringUtils;

import com.iscreate.op.dao.rno.RnoLtePciDaoImpl;
import com.iscreate.op.service.rno.job.JobProfile;
import com.iscreate.op.service.rno.job.JobStatus;
import com.iscreate.op.service.rno.job.common.JobState;
import com.iscreate.op.service.rno.mapreduce.IscreateHadoopBaseJob;
import com.iscreate.op.service.rno.mapreduce.pci.PciImportMapper;
import com.iscreate.op.service.rno.mapreduce.pci.PciImportReducer;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForHo;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForMr;
import com.iscreate.op.service.rno.mapreduce.pci.PciMapperForSf;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForHo;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForMr;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerForSf;
import com.iscreate.op.service.rno.mapreduce.pci.PciReducerSum;
import com.iscreate.op.service.rno.task.MapreduceBaseJobRunnable;

public class RnoNewPciPlanJobRunnable extends MapreduceBaseJobRunnable {
	private final static Log log = LogFactory.getLog(RnoNewPciPlanJobRunnable.class);
	private final static String jobType = "RNO_PCI_PLAN_NEW";

	private RnoLtePciDaoImpl rnoLtePciDao = null;

	@Override
	public boolean isMyJob(JobProfile job) {
		if (job == null)
			return false;
		else {
			return jobType.equals(job.getJobType());
		}
	}

	@Override
	public JobStatus runJobInternal() {
		rnoLtePciDao = new RnoLtePciDaoImpl();
		startTime = new Date();
		NewPciConfig config = new NewPciConfig(jobId);
		boolean canCalc = config.buildPciTaskConf(this, status, report, connection, updateStatusStmt, startTime);
		if (!canCalc) {
			addJobReport(report);
			updateOwnProgress("计算失败");
			return status;
		}
		updateOwnProgress("正在计算");
		Configuration conf = config.getConf();

		System.out.println("注册集群用于 pci规划干扰计算的job...");
		System.out.println("======>>>运行模式：" + conf.get("mapreduce.framework.name"));

		JobControl jobCtrl = null;
		List<ControlledJob> jobList = null;
		// 实例化一个 MapReduce 的 Job
		Job pciJob = null;
		String mrJobId = "";
		boolean isMapReduceSucc = false;
		String msg = "";

		if (config.getMatrixDfPath() == null) {
			// hbase源计算
			try {
				String filePath = config.getFilePath();
				List<Path> paths = new ArrayList<Path>();
				jobList = new ArrayList<ControlledJob>();

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
				TableMapReduceUtil.initTableMapperJob(config.getMrScans(), PciMapperForMr.class, Text.class,
						Text.class, mrDataJob);
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
				TableMapReduceUtil.initTableMapperJob(config.getHoScans(), PciMapperForHo.class, Text.class,
						Text.class, hoDataJob);
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
				TableMapReduceUtil.initTableMapperJob(config.getSfScans(), PciMapperForSf.class, Text.class,
						Text.class, sfDataJob);
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
				FileOutputFormat.setOutputPath(allDataJob, new Path(config.getRedOutPath()));
				ControlledJob allcj = new ControlledJob(conf);
				allcj.setJob(allDataJob);
				allcj.addDependingJob(mrcj);
				allcj.addDependingJob(hocj);
				allcj.addDependingJob(sfcj);
				jobList.add(allcj);

				// 提交任务
				jobCtrl = new JobControl("myctrl");
				jobCtrl.addJobCollection(jobList);

				new Thread(jobCtrl, "test").start();
				mrJobId = allcj.getJobID().toString();
				// 获取 reduce 的 jobid 存储入 lte_pci_job 表，以备后面获取停止该 job
				// 通过主进程 jobId 更新 lte_pci_job 表中 MR_JOB_ID（MapReduce 的 jobId）
				rnoLtePciDao.addMapReduceJobId(updateStatusStmt, jobId, mrJobId, "");

				// 通过 mrjobID 存储job对象，
				log.debug("+++>>>当前 mrJobId 为:" + mrJobId + "-----当前 mrJob 为: " + jobCtrl.getRunningJobList() + " 的信息!");
			} catch (Exception e) {
				e.printStackTrace();
				log.error("初始化 pci规划干扰计算job的Mapper失败！");
				// 保存报告信息
				report.setSystemFields("初始化 ", startTime, new Date(), "失败", "初始化 pci规划干扰计算job的Mapper失败");
				addJobReport(report);
				updateOwnProgress("计算失败");
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
			try {
				// 等待任务完成
				int progMonitorPollIntervalMillis = Job.getProgressPollInterval(conf) * 10;
				while (!jobCtrl.allFinished()) {
					for (ControlledJob cj : jobCtrl.getRunningJobList()) {
						log.info("+++>>>jobId=" + cj.getJobID() + " ,名称=" + cj.getJobName() + " ,+++>>>Map: "
								+ StringUtils.formatPercent(cj.getJob().mapProgress(), 0) + " Reduce: "
								+ StringUtils.formatPercent(cj.getJob().reduceProgress(), 0));
					}
					Thread.sleep(progMonitorPollIntervalMillis);
				}
				System.out.println(jobCtrl.getSuccessfulJobList());
				jobCtrl.stop();
				isMapReduceSucc = jobCtrl.getSuccessfulJobList().size() == jobList.size();
			} catch (Exception e) {
				e.printStackTrace();
				report.setSystemFields("MapReduce任务运行中", startTime, new Date(), "失败", "MapReduce任务运行失败");
				addJobReport(report);
				updateOwnProgress("计算失败");
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		} else {
			// 导入计算
			try {
				pciJob = Job.getInstance(conf, "PciCalc_Worker_New");
				pciJob.setJarByClass(IscreateHadoopBaseJob.class);
				pciJob.setOutputKeyClass(NullWritable.class);
				pciJob.setOutputValueClass(Text.class);
				pciJob.setMapperClass(PciImportMapper.class);
				pciJob.setReducerClass(PciImportReducer.class);
				FileInputFormat.addInputPath(pciJob, new Path(config.getMatrixDfPath()));
				Path path = new Path(config.getRedOutPath());
				// 先在 HDFS 中删除已经存在的文件，因为 HDFS 文件系统是不能修改的。
				FileSystem fs = FileSystem.get(conf);
				fs.delete(path, true);
				// 在 Hadoop 中设置结果输出路径
				FileOutputFormat.setOutputPath(pciJob, path);
				// 向 Hadoop 提交 Reduce 任务
				pciJob.submit();
				mrJobId = pciJob.getJobID().toString();
				// 获取 reduce 的 jobid 存储入 lte_pci_job 表，以备后面获取停止该 job
				// 通过主进程 jobId 更新 lte_pci_job 表中 MR_JOB_ID（MapReduce 的 jobId）
				rnoLtePciDao.addMapReduceJobId(updateStatusStmt, jobId, mrJobId, "");

				// 通过 mrjobID 存储job对象，
				log.debug("+++>>>当前 mrJobId 为:" + mrJobId + "-----当前 mrJob 为: " + pciJob + " 的信息!");

			} catch (Exception e) {
				e.printStackTrace();
				log.error("初始化 pci规划干扰计算job的Mapper失败！");
				// 保存报告信息
				report.setSystemFields("初始化 ", startTime, new Date(), "失败", "初始化 pci规划干扰计算job的Mapper失败");
				addJobReport(report);
				updateOwnProgress("计算失败");
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
			try {
				int progMonitorPollIntervalMillis = Job.getProgressPollInterval(conf) * 10;
				while (!pciJob.isComplete()) {
					if (pciJob.getJobState().getValue() == 4) {
						log.info("+++>>>jobId=" + mrJobId + ",名称=" + job.getJobName() + ",准备中...");
					}
					if (pciJob.getJobState().getValue() == 1) {
						log.info("+++>>>jobId=" + mrJobId + ",名称=" + job.getJobName() + ",执行中..."
								+ System.lineSeparator() + "+++>>>Map: "
								+ StringUtils.formatPercent(pciJob.mapProgress(), 0) + " Reduce: "
								+ StringUtils.formatPercent(pciJob.reduceProgress(), 0));
					}
					Thread.sleep(progMonitorPollIntervalMillis);
				}
				isMapReduceSucc = pciJob.isSuccessful();
			} catch (Exception e) {
				e.printStackTrace();
				report.setSystemFields("MapReduce任务运行中", startTime, new Date(), "失败", "MapReduce任务运行失败");
				addJobReport(report);
				updateOwnProgress("计算失败");
				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		}
		// 在 Hadoop 集群中 完成 PCI 规划干扰计算，并在 HDFS 中保存结果文件。
		try {
			boolean calcResFlag = true;
			if (isMapReduceSucc) {
				msg = "+++>>>集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：计算成功";
				log.info(msg);
				// 读取结果
				config.readDataFromFile();
				// 开始计算
				NewPciCalc pciCalc = new NewPciCalc(config);

				try {
					pciCalc.execCalc(this);
				} catch (Exception e) {
					calcResFlag = false;
					e.printStackTrace();
				}
				if (calcResFlag) {
					if (!"fail".equals(config.getReturnInfo())) {
						msg = "名称 = " + job.getJobName() + ", 结果：任务成功！<br>返回信息：" + config.getReturnInfo();
					} else {
						msg = "名称 = " + job.getJobName() + ", 结果：任务成功！<br>读取返回信息失败！";
					}
					log.info(msg);
					log.info("PCI 规划干扰计算完成。成功！");
					// 保存报告信息
					report.setStage("优化计算");
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("成功");
					report.setAttMsg(msg);
					addJobReport(report);

					// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算完成");
					updateOwnProgress("计算完成");
				} else {
					msg = "名称 = " + job.getJobName() + ", 结果：任务失败！原因：计算失败！";
					log.info("PCI 规划干扰计算完成。失败！");
					// 保存报告信息
					report.setStage("优化计算");
					report.setBegTime(startTime);
					report.setEndTime(new Date());
					report.setFinishState("失败");
					report.setAttMsg(msg);
					addJobReport(report);

					updateOwnProgress("计算失败");
				}
				pciCalc = null;
			} else {
				msg = "+++>>>集群jobId=" + mrJobId + ",名称=" + job.getJobName() + ",结果：任务失败！原因：MapReduce计算失败。";
				log.info(msg);
				log.info("PCI 规划干扰计算失败");
				// 保存报告信息
				report.setStage("MapReduce计算结束，未成功完成");
				report.setBegTime(startTime);
				report.setEndTime(new Date());
				report.setFinishState("失败");
				report.setAttMsg(msg);
				addJobReport(report);

				// structureQueryDao.updatePciPlanWorkStatusByStmt(stmt, jobId, "计算失败");
				updateOwnProgress("计算失败");

				status.setJobState(JobState.Failed);
				status.setUpdateTime(new Date());
				return status;
			}
		} catch (Exception e) {
			e.printStackTrace();
			report.setStage("优化计算");
			report.setBegTime(startTime);
			report.setEndTime(new Date());
			report.setFinishState("失败");
			report.setAttMsg("MapReduce计算结束，获取计算结果失败");
			addJobReport(report);

			updateOwnProgress("计算失败");

			status.setJobState(JobState.Failed);
			status.setUpdateTime(new Date());
			return status;
		}

		status.setJobState(JobState.Finished);
		status.setUpdateTime(new Date());
		return status;
	}

	@Override
	public void updateOwnProgress(JobStatus jobStatus) {
		if (updateStatusStmt != null && rnoLtePciDao != null) {
			// 更新 PCI 规划干扰计算表的进度
			rnoLtePciDao.updatePciPlanWorkStatusByStmt(updateStatusStmt, jobId, jobStatus.getJobState().getCode());
		}
	}

	public void updateOwnProgress(String jobStatus) {
		if (updateStatusStmt != null && rnoLtePciDao != null) {
			// 更新pci规划干扰计算表的进度
			rnoLtePciDao.updatePciPlanWorkStatusByStmt(updateStatusStmt, jobId, jobStatus);
		}
	}
}
