package com.iscreate.op.service.rno.task.pci;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PciCalc {
	// 方案优化结束的方式
	private enum PLAN_FINISH_WAY {
		PLAN_TYPE_ONE, PLAN_TYPE_TWO, SCHEME_TYPE_ONE, SCHEME_TYPE_TWO
	};

	private static Logger logger = LoggerFactory.getLogger(PciCalc.class);

	/** 地球平均半径,单位米 **/
	private static double R = 6371004;
	// 配置
	private PciConfig config = null;

	private Configuration conf = null;
	PciUtil pciUtil = null;

	/** 当前正在计算的方案 **/
	PlanObj bestPlan = new PlanObj();

	/** 小区与同站其他小区列的映射，同站其他小区已按关联度从大到小排列 **/
	Map<String, List<String>> cellToSameStationOtherCells;

	/** 小区与非同站小区列表的映射，非同站小区已按关联度从大到小排列 **/
	Map<String, List<String>> cellToNotSameStationCells;

	/** 小区与邻区关联度的映射（包含了同站其他小区） **/
	Map<String, Map<String, Double>> cellToNcellAssocDegree;

	/** 小区与小区总关联度的映射 **/
	Map<String, Double> cellToTotalAssocDegree;

	/** 小区与原PCI的映射 **/
	Map<String, Integer> cellToOriPci;

	/** 小区到经纬度的映射，不重复 **/
	Map<String, double[]> cellToLonLat;

	/** 小区与频率的映射 **/
	Map<String, String> cellToEarfcn;

	/** 小区与原始总干扰值的映射 **/
	Map<String, Double> cellToOriInterVal = new HashMap<String, Double>();

	/** 对 PCI 的分配次数进行记录，用于干扰值相同时 PCI 的判断依据。 **/
	Map<Integer, Integer> pciToAllocAmount = new HashMap<Integer, Integer>();

	/** pci+-30循环列表 **/
	Map<Integer, List<Integer>> pciToAlter30List = new HashMap<Integer, List<Integer>>();

	/** 距离太近小区映射 **/
	List<String> closeCell = new ArrayList<String>();

	/** 小区到流量的映射 **/
	Map<String, Float> cellToFlow = new HashMap<String, Float>();

	/** 小区到isDTCell的映射 **/
	Map<String, String> cellToIsDTCell = new HashMap<String, String>();

	// 同站其他小区分配 PCI 的列表，由于在递归中使用，所有必须是静态全局变量
	private static List<String> pciAllocList = null;

	// 收敛方案最终的差值比例或增均方差，用于返回信息
	double lastInterRate = 0.0;
	double lastVariance = 0.0;

	int planInterchangeTimes = 0;
	int generateToplistTimes = 0;
	int planNum = 0;// 中间方案个数
	float perFlow = 0;

	public PciCalc(PciConfig config) {
		System.out.println("mapreduce 处理成功，开始进行计算。。。");
		System.out.println("正在初始化计算类。。。");
		this.config = config;
		this.conf = config.getConf();

		this.cellToNcellAssocDegree = config.getCellToNcellAssocDegree();
		this.cellToNotSameStationCells = config.getCellToNotSameStationCells();
		this.cellToSameStationOtherCells = config.getCellToSameStationOtherCells();
		this.cellToTotalAssocDegree = config.getCellToTotalAssocDegree();
		this.cellToOriPci = config.getCellToOriPci();
		this.cellToEarfcn = config.getCellToEarfcn();
		this.cellToLonLat = config.getCellToLonLat();

		// 初始化距离限制
		bestPlan.setDisLimit(config.getDislimit());
		// 初始化 PCI 分配次数，初始值都为0
		for (int i = 0; i <= 503; i++) {
			pciToAllocAmount.put(i, 0);
		}
		for (int i = 0; i < 30; i++) {
			List<Integer> list = new ArrayList<Integer>();
			for (int j = i; j < 504; j += 30) {
				list.add(j);
			}
			pciToAlter30List.put(i, list);
		}
		for (int i = 30; i < 504; i++) {
			pciToAlter30List.put(i, pciToAlter30List.get(i % 30));
		}
		System.out.println("初始化计算类完成。");
	}

	public void setCloseCell(List<String> closeCell) {
		this.closeCell = closeCell;
	}

	public void setCellToFlow(Map<String, Float> cellToFlow) {
		this.cellToFlow = cellToFlow;
	}

	public void setCellToIsDTCell(Map<String, String> cellToIsDTCell) {
		this.cellToIsDTCell = cellToIsDTCell;
	}

	public void execCalc() throws Exception {
		System.out.println("计算正在进行。。。");
		if (!(config.getKs() == -1.0)) {
			System.out.println("进行全网流量计算。。。");
			// 计算全网平均业务量
			perFlow = calPerFlow(cellToFlow);
		}
		// 对所有主小区对应的同站其他小区列表按总关联度从大到小排序
		sortCellToSameStationOtherCells();

		// 对所有主小区按总关联度从大到小排序
		// List<String> descAllCellsByAssocDegree = new ArrayList<String>();
		Map<String, Double> descCellToRelaTotVal = sortMapByValue(cellToTotalAssocDegree);
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("D:\\tmp\\pcitest\\total_"
				+ System.currentTimeMillis() + ".csv")));
		for (String c : descCellToRelaTotVal.keySet()) {
			bw.write(c + "," + descCellToRelaTotVal.get(c));
			bw.newLine();
		}
		bw.close();
		bw = new BufferedWriter(
				new FileWriter(new File("D:\\tmp\\pcitest\\all_" + System.currentTimeMillis() + ".csv")));
		for (String c : cellToNcellAssocDegree.keySet()) {
			for (String nc : cellToNcellAssocDegree.get(c).keySet()) {
				bw.write(c + "," + nc + "," + cellToNcellAssocDegree.get(c).get(nc));
				bw.newLine();
			}
		}
		bw.close();
		/*
		 * for (String cellId : descCellToRelaTotVal.keySet()) {
		 * descAllCellsByAssocDegree.add(cellId.intern());
		 * }
		 */
		List<String> descAllCellsByAssocDegree = new ArrayList<String>(descCellToRelaTotVal.keySet());
		if ("YES".equals(config.getIsExportAssoTable())) {
			saveAssoTableInHdfs(descAllCellsByAssocDegree, descCellToRelaTotVal, "_Asso_Table.current");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("主小区数量descAllCellsByAssocDegree.size=" + descAllCellsByAssocDegree.size());
		}
		// 获取变小区关联度从大到小排序的队列（这里可以排除没有测量数据的变小区）
		List<String> descNeedToAssignCellList = new ArrayList<String>();
		for (String cellId : descAllCellsByAssocDegree) {
			if (config.getCellsNeedtoAssignList().contains(cellId)) {
				descNeedToAssignCellList.add(cellId.intern());
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("变小区数量descNeedToAssignCellList.size=" + descNeedToAssignCellList.size());
		}
		// 在首套方案中先为所有主小区分配原始PCI
		assignOriPciByDescCells(bestPlan, descAllCellsByAssocDegree);
		// 查看分配原始 pci 后 pci 分配资料情况

		// 计算使用原始PCI时各小区的原始干扰值和原始方案的干扰总值
		double oriPciTotalInterVal = calOriPciTotalInterVal();
		if (logger.isDebugEnabled()) {
			logger.debug("原始方案总干扰值：oriPciTotalInterVal=" + oriPciTotalInterVal);
		}

		// 首次变小区PCI分配，按变小区关联度大小顺序分配，生成首套方案。
		assignPciToCellsFirstPlan(bestPlan, descNeedToAssignCellList);

		// 计算首套方案的每小区干扰值
		bestPlan.calInterVal(cellToNotSameStationCells);

		// 计算首套方案总干拢值
		double minTotalInterVal = bestPlan.getTotalInterVal();
		if (logger.isDebugEnabled()) {
			logger.debug("首套方案总干拢值：minTotalInterVal=" + minTotalInterVal);
		}

		// 缓存第一次方案，并以文件形式保存到hdfs上
		savePlanInHdfs(bestPlan, "backup");
		// 统计首套方案
		planInterchangeTimes++;
		PlanObj plan = null;
		int f = 1;// 方案更换次数
		int topIndex = 0;
		boolean isCalFinish = false;

		// 以何种方案结束，用于返回信息。
		PLAN_FINISH_WAY planFinishWay;

		if (config.getSchemeType().equals("ONE")) {
			planFinishWay = PLAN_FINISH_WAY.SCHEME_TYPE_ONE;
		} else {
			planFinishWay = PLAN_FINISH_WAY.SCHEME_TYPE_TWO;
		}

		List<String> minInterValCellList = new ArrayList<String>();

		double lastTotalInterVal = 0.0;

		lastTotalInterVal = minTotalInterVal;

		// 收敛判断
		while (!isConvergence()) {
			logger.info("方案评估循环；{}", f++);

			// 创建新的方案
			plan = bestPlan.clone();

			// 清除需要重新分配的小区,才能重新分配 pci
			// plan.clearCellsInfo(topCellList);

			int b = 1;// 变PCI小区表生成次数

			// 获得 Top 小区表
			List<String> topCellList = getTopCellListByInter(plan);

			if (logger.isDebugEnabled()) {
				logger.debug("topCellList=" + topCellList);
			}

			if (logger.isDebugEnabled()) {
				for (String cellId : topCellList) {
					logger.debug("cellId={}, interVal={}", cellId, plan.getInterValByCell(cellId));
				}
			}
			if ("YES".equals(config.getIsExportMidPlan())) {
				planNum++;
				String suffix1 = "_Mid_Plan_" + planNum + ".current";
				saveExportPlanInHdfs(plan, topCellList, suffix1);
			}
			minInterValCellList.clear();
			minInterValCellList.addAll(topCellList);

			// 重新分配 pci 给 topCellList 的小区
			assignPciToCellsPlan(plan, topCellList);

			double currentTotalInterVal = 0.0;
			List<String> newTopCellList = null;
			// 方案评估，总干拢值越小越好
			while (true) {
				logger.info("当前第<{}>套方案，生成第<{}>个 TOP List。", f, b);

				// 计算当前方案的每个小区对应的干扰值
				plan.calInterVal(cellToNotSameStationCells);
				// 当前方案的总干拢值
				currentTotalInterVal = plan.getTotalInterVal();
				if (logger.isDebugEnabled()) {
					logger.debug("循环分配中，最小干扰总值：" + minTotalInterVal);
					logger.debug("循环分配中，上次干扰总值：" + lastTotalInterVal);
					logger.debug("循环分配中，当前干扰总值：" + currentTotalInterVal);
				}

				if (currentTotalInterVal < minTotalInterVal) {
					// 新方案全网干拢值优于旧方案，替换旧方案
					// 保存总干扰值低于上一次的方案
					savePlanInHdfs(plan, "current");

					// 备份方案文件，一份当前方案文件以current结尾，一份备份文件以backup结尾
					backupPlanFile();

					// 最小总干扰值设置为当前的总干扰值
					minTotalInterVal = currentTotalInterVal;

					// 当前的最好方案
					bestPlan = plan.clone();

					break;
				} else {
					// 产生新的 Top 小区列表（按干扰总值从大到小排序）
					newTopCellList = getTopCellListByInter(plan);
					// 累加当前方案的TOP list生成次数。
					b++;

					if (config.getPlanType().equals("ONE")) {
						// 评估方案1
						// 排名是否有变化
						if (topCellList.size() > 0
								&& isRankingChanged(topCellList, newTopCellList, topIndex, minTotalInterVal,
										lastTotalInterVal, currentTotalInterVal, minInterValCellList)) {
							// 总干扰值更差了，但最差的小区的提名提升了，用新列表，继续对当前位置小区进行优化。
							if (logger.isDebugEnabled()) {
								logger.debug("排名有变化，topIndex={}, topCellList={}, newTopCellList={}", topIndex,
										topCellList.get(topIndex), newTopCellList.get(topIndex));
							}
							topCellList = newTopCellList;
							assignPciToCellsPlan(plan, topCellList);
						} else {
							// 总干扰值更差了，并且最差的小区提名没有提升，用回原来列表，并对下一个位置小区进行优化。
							// 当 TOP 列表最后一个都优化后，优化计算结束。
							topIndex++;

							if (topIndex < topCellList.size()) {
								if (logger.isDebugEnabled()) {
									logger.debug("排名无变化，用 TOP" + (topIndex + 1) + " 进行计算。");
								}
								assignPciToCellsPlan(plan, topCellList, topCellList.get(topIndex));
							} else {
								if (logger.isDebugEnabled()) {
									logger.debug("排名无变化，TopList 已分配完，计算结束。");
								}
								isCalFinish = true;
								planFinishWay = PLAN_FINISH_WAY.PLAN_TYPE_ONE;
								break;
							}
						}
						lastTotalInterVal = currentTotalInterVal;
					} else {
						// 评估方案2
						// 保持原方案，对 TOP2 小区进行优化
						topIndex++;

						if (topIndex < topCellList.size()) {
							if (logger.isDebugEnabled()) {
								logger.debug("选用评估方案2，用 TOP" + (topIndex + 1) + " 进行计算。");
							}
							assignPciToCellsPlan(plan, topCellList, topCellList.get(topIndex));
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("选用评估方案2，TopList 已分配完，计算结束。");
							}
							isCalFinish = true;
							planFinishWay = PLAN_FINISH_WAY.PLAN_TYPE_TWO;
							break;
						}
					}

				}
			}
			// 累计TOP list 生成次数
			generateToplistTimes += b;
			if (isCalFinish)
				break;
		}
		planInterchangeTimes = f;
		if ("YES".equals(config.getIsExportMidPlan())) {
			saveMidPlanCountInHdfs(planNum);
		}
		// 输出当前方案之前，以最佳方案进行邻区核查。
		if ("YES".equals(config.getIsCheckNCell())) {
			System.out.println("进行邻区核查");
			checkNCellPci(bestPlan, descNeedToAssignCellList);
		}
		String suffix2 = "_Nc_Check_Plan.current";
		if ("YES".equals(config.getIsExportNcCheckPlan())) {
			saveNcCheckPlanInHdfs(bestPlan, suffix2);
		}
		// 输出当前方案之前，以最佳方案进行同频同PCI核查。
		checkNeighborAreaPci(bestPlan, descNeedToAssignCellList);
		// 保存最终方案
		savePlanInHdfs(bestPlan, "current");
		// 保存返回信息
		// saveReturnInfoInHdfs(planFinishWay, bestPlan.isSamePciTooClose());
		config.setReturnInfo(getReturnInfo(planFinishWay, bestPlan.isSamePciTooClose()));
		System.out.println("计算完成！");
	}

	/**
	 * 计算全网平均业务量
	 * 
	 * @param cellToFlow
	 */
	private float calPerFlow(Map<String, Float> cellToFlow) {
		float totalFlow = 0;
		int num = 0;
		for (Entry<String, Float> flow : cellToFlow.entrySet()) {
			totalFlow += flow.getValue() < 100 ? 100 : flow.getValue();
			num++;
		}
		return totalFlow / num;
	}

	/**
	 * 对所有主小区对应的同站其他小区列表按总关联度从大到小排序
	 */
	private void sortCellToSameStationOtherCells() {
		String tmpCellId = "";
		for (String cellId : cellToSameStationOtherCells.keySet()) {
			for (int i = 0; i < cellToSameStationOtherCells.get(cellId).size(); i++) {
				for (int j = i + 1; j < cellToSameStationOtherCells.get(cellId).size(); j++) {
					String ocellId1 = cellToSameStationOtherCells.get(cellId).get(i);
					String ocellId2 = cellToSameStationOtherCells.get(cellId).get(j);
					if (cellToTotalAssocDegree.get(ocellId1) == null || cellToTotalAssocDegree.get(ocellId2) == null)
						continue;
					if (cellToTotalAssocDegree.get(ocellId1) < cellToTotalAssocDegree.get(ocellId2)) {
						tmpCellId = ocellId1;
						cellToSameStationOtherCells.get(cellId).set(i, ocellId2);
						cellToSameStationOtherCells.get(cellId).set(j, tmpCellId);
					}
				}
			}
		}
	}

	/**
	 * Top 小区列表排名是否有变化
	 * 
	 * @param topCellList 旧的 Top 小区列表
	 * @param newTopCellList 新的 Top 小区列表
	 * @param minInterValCellList
	 * @param newTotalInterVal
	 * @param minTotalInterVal
	 * @param currentTotalInterVal
	 * @return
	 */
	private boolean isRankingChanged(List<String> topCellList, List<String> newTopCellList, int topIndex,
			double minTotalInterVal, double lastTotalInterVal, double currentTotalInterVal,
			List<String> minInterValCellList) {
		if (topCellList.get(topIndex).equals(newTopCellList.get(topIndex))) {
			// 总干扰值更差了，并且最差的小区提名没有提升，对下一个位置小区进行优化。
			return false;
		} else {
			if (currentTotalInterVal == minTotalInterVal) {
				for (int i = 0; i < newTopCellList.size(); i++) {
					if (!newTopCellList.get(i).equals(minInterValCellList.get(i))) {
						return true;
					}
				}
				return false;
			}

			if (currentTotalInterVal <= lastTotalInterVal) {
				return false;
			}
			// 总干扰值更差了，但最差的小区的提名提升了，继续对当前位置小区进行优化。
			return true;
		}
	}

	/**
	 * 收敛判断
	 * 
	 * @return 是否停止计算
	 */
	private boolean isConvergence() {
		boolean bool = false;
		if (config.getSchemeType().equals("ONE")) {
			// 收敛方法1: 尾数差异收敛算法 -- top n%小区干扰值与中间n%小区干扰值的差值比例 小于等于m%
			double interRate = getInterRate();
			double defInterRate = config.getDefInterRate();
			lastInterRate = interRate;
			if (logger.isDebugEnabled()) {
				logger.debug("TOP {}% 小区与中间 {}% 的干扰差值比例={}%, 给定值={}%", config.getTopRate() * 100,
						config.getTopRate() * 100, interRate * 100, defInterRate * 100);
			}
			if (interRate <= defInterRate) {
				if (logger.isDebugEnabled()) {
					logger.debug("【top n%小区干扰总值与中间n%小区干扰总值的差值比例 小于等于m%】，终止继续优化");
				}
				bool = true;
			}
		} else {
			double varianceByCurrentPlan = getVarianceByCurrentPlan();
			double defVariance = config.getDefVariance();
			lastVariance = varianceByCurrentPlan;
			// 收敛方法2: 均方差收敛算法 -- 全网以小区数均分为n份，求均方差，当值小于m
			if (logger.isDebugEnabled()) {
				logger.debug("全网小区干扰值方差=" + varianceByCurrentPlan + ", 给定值=" + defVariance);
			}
			if (varianceByCurrentPlan < defVariance) {
				if (logger.isDebugEnabled()) {
					logger.debug("【求全网小区干扰值方差，当值小于给定值m】，终止继续优化");
				}
				bool = true;
			}
		}
		return bool;
	}

	/**
	 * 计算使用原始PCI的干扰总值
	 * 
	 * @return 干扰总值
	 */
	private double calOriPciTotalInterVal() {
		// 计算原始PCI的干扰值
		for (String cellId : cellToOriPci.keySet()) {
			int cellPci = cellToOriPci.get(cellId);
			List<String> ncells = cellToNotSameStationCells.get(cellId);
			double interVal = 0;
			double ks = 1;
			if (!(config.getKs() == -1.0)) {
				ks = calKs(cellId);
			}
			if (ncells != null) {
				for (int i = 0; i < ncells.size(); i++) {
					double modVal = 0;
					Integer oldPci = cellToOriPci.get(ncells.get(i));
					if (oldPci != null && oldPci != -1) {
						if (cellPci % 3 == (oldPci % 3)) {
							modVal += config.getM3r();
						}
						if (cellPci % 6 == (oldPci % 6)) {
							modVal += config.getM6r();
						}
						if (cellPci % 30 == (oldPci % 30)) {
							modVal += config.getM30r();
						}
					}
					double relaVal = 0;
					if (cellToNcellAssocDegree.get(cellId) != null) {
						if (cellToNcellAssocDegree.get(cellId).get(ncells.get(i)) != null) {
							relaVal = cellToNcellAssocDegree.get(cellId).get(ncells.get(i));
						}
					}
					interVal += modVal * relaVal;
				}
			}
			cellToOriInterVal.put(cellId.intern(), ks * interVal);
		}

		// 计算原方案的干扰总值
		double oriTotInterVal = 0.0;
		for (String one : cellToOriInterVal.keySet()) {
			oriTotInterVal += cellToOriInterVal.get(one);
		}
		return oriTotInterVal;
	}

	/**
	 * 获到当前方案的 Top 变小区表（按变小区按干扰值从大到小的列表）
	 * 
	 * @param plan 当前方案
	 * @return
	 */
	private List<String> getTopCellListByInter(PlanObj plan) {
		// 获取当前方案的干扰值小区排序，从大到小
		Map<String, Double> descCellByInterValMap = sortMapByValue(plan.getCellToInterVal());
		if (logger.isDebugEnabled()) {
			logger.debug("当前方案总小区个数为 {} 个", descCellByInterValMap.keySet().size());
		}
		// 获取 TopList 表
		List<String> topCellList = new ArrayList<String>();
		// ToList表是变小区的10%
		int size = config.getCellsNeedtoAssignList().size();
		int num = (int) (config.getTopRate() * size);
		int n = 0;
		for (String cellId : descCellByInterValMap.keySet()) {
			if (n == num)
				break;
			// 是变小区才加进 TopList 列表中
			if (config.getCellsNeedtoAssignList().contains(cellId)) {
				topCellList.add(cellId.intern());
				n++;
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("TOP变小区表大小为=" + topCellList.size());
		}
		return topCellList;
	}

	/**
	 * 备份current文件，以backup结尾保存在hdfs的同目录上
	 *
	 * @return
	 * @author peng.jm
	 * @date 2015年3月26日15:43:19
	 */
	private void backupPlanFile() {
		String currentFile = config.getFilePath() + "/" + config.getFileName() + "." + "current";
		String backupFile = config.getFilePath() + "/" + config.getFileName() + "." + "backup";

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：backupPlanFile时打开hdfs系统出错！");
			e1.printStackTrace();
		}

		// 创建对应的path
		Path currentFilePath = new Path(URI.create(currentFile));
		Path backupFilePath = new Path(URI.create(backupFile));

		try {
			// 删除backup文件
			if (fs.exists(backupFilePath)) {
				fs.delete(backupFilePath, false);
			}
			// 将current文件命名成backup文件
			fs.rename(currentFilePath, backupFilePath);
		} catch (IOException e) {
			logger.error("PciReducer过程：backupPlanFile出错！");
			e.printStackTrace();
		}

	}

	/**
	 * 保存方案到hdfs文件系统上
	 *
	 * @param plan
	 *            需要保存的方案
	 * @param suffix
	 *            文件后缀（backup:最后一次成功的；current:当前最新）
	 * @return
	 * @author peng.jm
	 * @date 2015年3月26日14:53:26
	 */
	private void savePlanInHdfs(PlanObj plan, String suffix) {
		String realFilePath = config.getFilePath() + "/" + config.getFileName() + "." + suffix;
		if (logger.isDebugEnabled()) {
			logger.debug("把方案写入到HDFS文件:" + realFilePath);
		}

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}

		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("PciReducer过程：保存方案时，删除原有文件出错！");
			e2.printStackTrace();
		}

		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));

		// 创建流
		DataOutputStream dataDOS = null;
		try {
			FSDataOutputStream fdos = fs.create(filePathObj, true);
			if (fdos != null) {
				dataDOS = new DataOutputStream(fdos);
			}
		} catch (IOException e1) {
			logger.error("PciReducer过程：创建输出数据文件流出错！");
			e1.printStackTrace();
		}

		// 根据规则写入数据
		try {
			// 将MR数据和待优化PCI的小区都并在一起输出
			// MR数据存在原始干扰值，计算干扰值，计算PCI
			// 待优化PCI小区中可能存在着部分小区在这批MR数据中并不存在，所以不能进行计算干扰值和分配PCI
			// 这里处理的方法是将MR数据中的存在的所有小区都输出，然后检查待优化小区哪些是不存在于MR数据中的小区
			// 将这些小区也以默认值输出，默认原干扰值0，计算干扰值0，PCI为-1

			// 检查不存在与MR数据的小区
			List<String> cellsNotInData = new ArrayList<String>();
			for (String cellId : config.getCellsNeedtoAssignList()) {
				if (!plan.getCellToPci().containsKey(cellId)) {
					cellsNotInData.add(cellId.intern());
				}
			}
			int size = plan.getCellToPci().size() + cellsNotInData.size();

			// 总干扰值
			dataDOS.writeDouble(plan.getTotalInterVal());
			// 需要优化的小区数量
			dataDOS.writeInt(size);
			List<String> cellToPci = new ArrayList<String>(plan.getCellToPci().keySet());
			cellToPci = sortStringList(cellToPci);
			// 写入数据中存在的小区PCI以及干扰值
			for (String cellId : cellToPci) {
				// 小区ID
				dataDOS.writeUTF(cellId);
				// 小区计算PCI
				dataDOS.writeInt(plan.getPciByCell(cellId));
				// 小区原干扰值
				if (cellToOriInterVal.get(cellId) != null) {
					dataDOS.writeDouble(cellToOriInterVal.get(cellId));
				} else {
					dataDOS.writeDouble(0.0);
				}
				// 小区计算干扰值
				dataDOS.writeDouble(plan.getInterValByCell(cellId));
			}
			cellsNotInData = sortStringList(cellsNotInData);
			// 写入数据中不存在的小区默认PCI以及干扰值
			for (String cellId : cellsNotInData) {
				dataDOS.writeUTF(cellId);
				dataDOS.writeInt(-1);
				dataDOS.writeDouble(0.0);
				dataDOS.writeDouble(0.0);
			}

			// 结束符，判断文件是否完整
			dataDOS.writeUTF("finished");
		} catch (IOException e) {
			logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			e.printStackTrace();
		} finally {
			try {
				dataDOS.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 保存返回信息文件，用于在页面上显示 PCI 优化是以哪种方式结束计算。
	 * 
	 * @param planFinishWay 方案优化结束方式
	 */
	/*
	 * private void saveReturnInfoInHdfs(PLAN_FINISH_WAY planFinishWay, boolean isPciClose) {
	 * String realFilePath = config.getFilePath() + "/" + config.getFileName() + ".resultInfo";
	 * if (logger.isDebugEnabled()) {
	 * logger.debug("把返回信息写入到HDFS文件:" + realFilePath);
	 * }
	 * 
	 * FileSystem fs = null;
	 * try {
	 * fs = FileSystem.get(conf);
	 * } catch (IOException e1) {
	 * System.out.println("PciReducer过程：打开hdfs文件系统出错！");
	 * e1.printStackTrace();
	 * }
	 * 
	 * // 先删除原有文件
	 * Path oldFilePath = new Path(URI.create(realFilePath));
	 * try {
	 * if (fs.exists(oldFilePath)) {
	 * fs.delete(oldFilePath, false);
	 * }
	 * } catch (IOException e2) {
	 * System.out.println("PciReducer过程：保存返回信息文件时，删除原有文件出错！");
	 * e2.printStackTrace();
	 * }
	 * 
	 * // 创建新文件
	 * Path filePathObj = new Path(URI.create(realFilePath));
	 * 
	 * // 创建流
	 * OutputStream dataOs = null;
	 * try {
	 * dataOs = fs.create(filePathObj, true);
	 * } catch (IOException e1) {
	 * System.out.println("PciReducer过程：创建输出数据文件流出错！");
	 * e1.printStackTrace();
	 * }
	 * 
	 * // 根据规则写入数据
	 * DataOutputStream dataDOS = new DataOutputStream(dataOs);
	 * try {
	 * String info = "<br>";
	 * if (config.getPlanType().equals("ONE")) {
	 * info += "评估方案选择了：方案评估1(三步法)；";
	 * } else {
	 * info += "评估方案选择了：方案评估2(两步法)；";
	 * }
	 * 
	 * info += "<br>";
	 * 
	 * if (config.getSchemeType().equals("ONE")) {
	 * info += "收敛方式选择了：方案一(根据Top差值比例)；";
	 * } else {
	 * info += "收敛方式选择了：方案二(根据求方差)；";
	 * }
	 * 
	 * info += "<br>优化结束方式：";
	 * switch (planFinishWay) {
	 * case PLAN_TYPE_ONE:
	 * info += "方案评估1(三步法)";
	 * break;
	 * case PLAN_TYPE_TWO:
	 * info += "方案评估2(两步法)";
	 * break;
	 * case SCHEME_TYPE_ONE:
	 * info += "收敛方式1(根据Top差值比例)";
	 * break;
	 * case SCHEME_TYPE_TWO:
	 * info += "收敛方式2(根据求方差)";
	 * break;
	 * }
	 * 
	 * info += "<br>是否进行邻区核查：";
	 * if (config.getIsCheckNCell().equals("YES")) {
	 * info += "是；";
	 * } else {
	 * info += "否；";
	 * }
	 * 
	 * info += "<br> " + config.getDislimit() + "米内，有无同PCI情况：";
	 * if (isPciClose) {
	 * info += "有。";
	 * } else {
	 * info += "无。";
	 * }
	 * info += "<br>";
	 * if (config.getSchemeType().equals("ONE")) {
	 * info += "最终的差值比例：" + lastInterRate + "，差值比例目标值：" + config.getDefInterRate();
	 * } else {
	 * info += "最终的均方差：" + lastVariance + "，均方差目标值" + config.getDefVariance();
	 * }
	 * 
	 * info += "<br>";
	 * info += "方案替换次数：" + planInterchangeTimes;
	 * info += "<br>";
	 * info += "生成TOPlist次数：" + generateToplistTimes;
	 * if (!(config.getKs() == -1.0)) {
	 * info += "<br>";
	 * info += "Ks修正值：" + config.getKs();
	 * }
	 * dataDOS.writeUTF(info);
	 * 
	 * // 结束符，判断文件是否完整
	 * dataDOS.writeUTF("finished");
	 * } catch (IOException e) {
	 * logger.error("PciReducer过程: pci规划返回信息文件写入内容时失败！");
	 * try {
	 * dataDOS.close();
	 * } catch (IOException e1) {
	 * e1.printStackTrace();
	 * }
	 * e.printStackTrace();
	 * }
	 * try {
	 * dataDOS.close();
	 * } catch (IOException e1) {
	 * e1.printStackTrace();
	 * }
	 * }
	 */
	/**
	 * 返回任务信息
	 * 
	 * @param planFinishWay
	 * @param isPciClose
	 */
	public String getReturnInfo(PLAN_FINISH_WAY planFinishWay, boolean isPciClose) {
		String resultInfo = "<br>";
		if (config.getPlanType().equals("ONE")) {
			resultInfo += "评估方案选择了：方案评估1(三步法)；";
		} else {
			resultInfo += "评估方案选择了：方案评估2(两步法)；";
		}

		resultInfo += "<br>";

		if (config.getSchemeType().equals("ONE")) {
			resultInfo += "收敛方式选择了：方案一(根据Top差值比例)；";
		} else {
			resultInfo += "收敛方式选择了：方案二(根据求方差)；";
		}

		resultInfo += "<br>优化结束方式：";
		switch (planFinishWay) {
		case PLAN_TYPE_ONE:
			resultInfo += "方案评估1(三步法)";
			break;
		case PLAN_TYPE_TWO:
			resultInfo += "方案评估2(两步法)";
			break;
		case SCHEME_TYPE_ONE:
			resultInfo += "收敛方式1(根据Top差值比例)";
			break;
		case SCHEME_TYPE_TWO:
			resultInfo += "收敛方式2(根据求方差)";
			break;
		}

		resultInfo += "<br>是否进行邻区核查：";
		if (config.getIsCheckNCell().equals("YES")) {
			resultInfo += "是；";
		} else {
			resultInfo += "否；";
		}

		resultInfo += "<br> " + config.getDislimit() + "米内，有无同PCI情况：";
		if (isPciClose) {
			resultInfo += "有。";
		} else {
			resultInfo += "无。";
		}
		resultInfo += "<br>";
		if (config.getSchemeType().equals("ONE")) {
			resultInfo += "最终的差值比例：" + lastInterRate + "，差值比例目标值：" + config.getDefInterRate();
		} else {
			resultInfo += "最终的均方差：" + lastVariance + "，均方差目标值" + config.getDefVariance();
		}

		resultInfo += "<br>";
		resultInfo += "方案替换次数：" + planInterchangeTimes;
		resultInfo += "<br>";
		resultInfo += "生成TOPlist次数：" + generateToplistTimes;
		if (!(config.getKs() == -1.0)) {
			resultInfo += "<br>";
			resultInfo += "Ks修正值：" + config.getKs();
		}
		return resultInfo;
	}

	/**
	 * 收敛方式1(根据Top差值比例)
	 */
	private double getInterRate() {
		Map<String, Double> descCellByInterVal = sortMapByValue(bestPlan.getCellToInterVal());
		List<Double> descInterVals = new ArrayList<Double>();
		for (String cellId : descCellByInterVal.keySet()) {
			descInterVals.add(descCellByInterVal.get(cellId));
		}
		// top区间
		int start1 = 0;
		int start2 = (int) (descInterVals.size() * config.getTopRate());
		// 中间区间，如果 top 区间是奇数，则 mid 区间加大1
		int mid1 = (int) (descInterVals.size() / 2 - start2 / 2 - (start2 % 2));
		int mid2 = (int) (descInterVals.size() / 2 + start2 / 2);

		double topInterVal = 0;
		for (int i = start1; i < start2; i++) {
			topInterVal += descInterVals.get(i);
		}
		double midInterVal = 0;
		for (int i = mid1; i < mid2; i++) {
			midInterVal += descInterVals.get(i);
		}
		if (logger.isDebugEnabled()) {
			logger.debug("topInterVal={}, midInterVal={},start1={},start2={},mid1={},mid2={}", topInterVal,
					midInterVal, start1, start2, mid1, mid2);
		}
		double result = (topInterVal == 0.0) ? 0.0 : (topInterVal - midInterVal) / topInterVal;
		return result;
	}

	/**
	 * 收敛方式2(根据求方差)
	 */
	private double getVarianceByCurrentPlan() {
		// 获取当前方案的干扰值小区排序，从大到小
		Map<String, Double> descCellByInterValMap = sortMapByValue(bestPlan.getCellToInterVal());

		int size = bestPlan.getCellToInterVal().size();
		int div = size / config.getDivideNumber();
		int mod = size % config.getDivideNumber();

		int tmpDevideNumber = config.getDivideNumber();
		if (mod > 0) {
			div = div + 1;
			tmpDevideNumber = tmpDevideNumber - 1;
		}

		List<Double> tmplist = new ArrayList<Double>();
		for (String cellId : descCellByInterValMap.keySet()) {
			tmplist.add(descCellByInterValMap.get(cellId));
		}

		double totVal = 0;
		List<Double> listTotal = new ArrayList<Double>();
		// 以小区数均分为 n 份
		int k = 0;
		for (int i = 0; i < tmpDevideNumber; i++) {
			double total = 0;
			for (int j = 0; j < div; j++) {
				total += tmplist.get(k);
				k++;
			}
			totVal += total;
			listTotal.add(total);
		}

		if (mod > 0) {
			double total = 0;
			for (int i = tmplist.size() - div; i < tmplist.size(); i++) {
				total += tmplist.get(i);
			}
			totVal += total;
			listTotal.add(total);
		}

		// 平均值
		double averageVal = totVal / config.getDivideNumber();

		double val = 0;
		for (double total : listTotal) {
			double tmpVal = total - averageVal;
			val += tmpVal * tmpVal;
		}
		double result = Math.sqrt(val / size);
		return result;
	}

	/**
	 * 首次变小区PCI分配，按变小区关联度大小顺序分配，生成首套方案。
	 * 
	 * @param plan 首套方案
	 * @param descNeedToAssignCellList 排序后的变小区列表
	 */
	private void assignPciToCellsFirstPlan(PlanObj plan, List<String> descNeedToAssignCellList) {
		// 已手动分配了PCI的小区列表，防止再被重新分配
		List<String> assignedPciCellsList = new ArrayList<String>();

		for (String cellId : descNeedToAssignCellList) {

			// 如果主小区已分配过，则它的同站小区也都分配过，直接跳过。
			if (assignedPciCellsList.contains(cellId))
				continue;

			// 主小区的非同站小区列表
			List<String> notSameStationCells = cellToNotSameStationCells.get(cellId);

			// 同站其他小区
			List<String> sameStationOtherCells = cellToSameStationOtherCells.get(cellId);

			// 依次将0-503个PCI分配给A1小区的，计算非同站小区总干扰值；
			// 得出最小的一个干扰值对应的PCI，若有几个PCI对应干扰值相同，则选择最小值，赋予A1；
			int bestPci = 0;
			double minInterValue = calNotSameStationCellsTotalInterVal(plan, cellId, 0, notSameStationCells);
			for (int pci = 1; pci <= 503; pci++) {
				double currentInterValue = calNotSameStationCellsTotalInterVal(plan, cellId, pci, notSameStationCells);
				if (currentInterValue < minInterValue) {
					minInterValue = currentInterValue;
					bestPci = pci;
				} else if (currentInterValue == minInterValue) {
					// 当干扰值相等时，以 PCI 已被分配次数 pciToAllocAmount 作为分配的依据
					if (pciToAllocAmount.get(pci) < pciToAllocAmount.get(bestPci)) {
						bestPci = pci;
					}
				}
			}

			// 把主小区PCI加到方案中
			plan.addCellToPci(cellId.intern(), bestPci);

			// 给同站其他小区分配 PCI
			selectBestPciForSameStationOtherCell(plan, cellId, bestPci, notSameStationCells, sameStationOtherCells);

			// 把同站小区添加进已分配 PCI 的列表中
			assignedPciCellsList.add(cellId.intern());
			assignedPciCellsList.addAll(sameStationOtherCells);
		}
	}

	/**
	 * 选择最佳的同站其他小区PCI分配，同站其他小区有可能为0~6个
	 */
	private void selectBestPciForSameStationOtherCell(PlanObj plan, String cellId, int bestPci,
			List<String> notSameStationCells, List<String> sameStationOtherCells) {
		pciUtil = new PciUtil();

		int scellsSize = sameStationOtherCells.size();

		if (scellsSize == 0 || scellsSize > 5) {
			// 没有同站其他小区或者同站其他小区数多于5个，不处理直接返回
			return;
		}

		// 根据A1的PCI获取对应PCI组
		List<Integer> pciGroup = pciUtil.getPciGroup(bestPci);

		// 如果同站小区为4~6个时
		if (scellsSize > 2) {
			int nextPci = bestPci + 3;
			if (nextPci > 503)
				nextPci = 0;
			pciGroup.addAll(pciUtil.getPciGroup(nextPci));
		}

		// 在 PCI 组中去除已分配给 A1 的 PCI 值，这里一定要使用 new Integer，否则会认为 bestPci 是 index。
		pciGroup.remove(new Integer(bestPci));

		pciAllocList = new ArrayList<String>();

		allocSameStationOtherCellsPci("", pciGroup, new ArrayList<Integer>(), scellsSize);

		String bestPciString = "";
		double minInterVal = Double.MAX_VALUE;
		for (String strPics : pciAllocList) {
			if (strPics.length() > 0) {
				String[] picArray = strPics.split(",");
				// 分配PCI
				for (int i = 0; i < scellsSize; i++) {
					plan.addCellToPci(sameStationOtherCells.get(i), Integer.parseInt(picArray[i]));
				}
				double interVal = calNotSameStationCellsTotalInterVal(plan, cellId, bestPci, notSameStationCells);
				if (interVal < minInterVal) {
					minInterVal = interVal;
					bestPciString = strPics;
				}
			}
		}
		// 判断bestPciString是否为空，不为空则更新
		if (bestPciString.length() > 0) {
			String[] bestPciArray = bestPciString.split(",");
			for (int i = 0; i < scellsSize; i++) {
				if (bestPciArray[i] == null) {
					System.out.println(">>>bestPciString=" + bestPciString);
				}
				plan.addCellToPci(sameStationOtherCells.get(i), Integer.parseInt(bestPciArray[i]));
			}
		}
		pciAllocList.clear();
		pciAllocList = null;
	}

	/**
	 * 分配同站其他小区的递归算法
	 * 
	 * @param strPcis 初始调用时设置为空子符串
	 * @param pcis PCI组
	 * @param tmpPcis 初始调用时设置为空列表 new ArrayList<Integer>()
	 * @param length 同站其他小区的个数（1~5个）
	 */
	private void allocSameStationOtherCellsPci(String strPcis, List<Integer> pcis, List<Integer> tmpPcis, int length) {
		if (length == 0) {
			pciAllocList.add(strPcis.substring(0, strPcis.length() - 1));
			return;
		}
		List<Integer> tmpPics2;
		for (int i = 0; i < pcis.size(); i++) {
			tmpPics2 = new ArrayList<Integer>();
			tmpPics2.addAll(tmpPcis);
			if (!tmpPcis.contains(i)) {
				String str = strPcis + pcis.get(i) + ",";
				tmpPics2.add(i);
				allocSameStationOtherCellsPci(str, pcis, tmpPics2, length - 1);
			}
		}
	}

	/**
	 * 计算非同站小区的总干扰值
	 * 
	 * @param notSameStationCells 非同站小区
	 * @return 总干扰值
	 */
	private Double calNotSameStationCellsTotalInterVal(PlanObj plan, String cellId, int cellPci,
			List<String> notSameStationCells) {

		double interVal = 0;
		double ks = 1;
		if (notSameStationCells != null) {
			for (String nssCell : notSameStationCells) {
				// 如果未分配有原始PCI，缺省干拢值为0
				double modVal = 0;
				Integer oldPci = plan.getPciByCell(nssCell);
				if (oldPci == null || oldPci == -1) {
					oldPci = cellToOriPci.get(nssCell);
				}
				if (oldPci != null && oldPci != -1) {
					if (cellPci % 3 == (oldPci % 3)) {
						modVal += config.getM3r();
					}
					if (cellPci % 6 == (oldPci % 6)) {
						modVal += config.getM6r();
					}
					if (cellPci % 30 == (oldPci % 30)) {
						modVal += config.getM30r();
					}
				}

				double relaVal = 0;
				if (cellToNcellAssocDegree.get(cellId) != null) {
					if (!(config.getKs() == -1.0)) {
						if (cellToFlow.get(cellId) != null) {
							ks = calKs(cellId);
						}
					}
					if (cellToNcellAssocDegree.get(cellId).get(nssCell) != null) {
						relaVal = cellToNcellAssocDegree.get(cellId).get(nssCell);
					}
				}
				interVal += modVal * relaVal;
			}
		}
		return ks * interVal;
	}

	/**
	 * 在非首套方案中分配PCI
	 * 
	 * @param plan
	 * @param topList
	 * @param cellId
	 */
	private void assignPciToCellsPlan(PlanObj plan, List<String> topList, String cellId) {

		// 获取主小区的非同站小区列表
		List<String> notSameStationCells = cellToNotSameStationCells.get(cellId);

		// 获取同站其他小区
		List<String> sameStationOtherCells = cellToSameStationOtherCells.get(cellId);

		// 依次将0-503个PCI分配给A1小区的，计算非同站小区总干扰值；
		// 得出最小的一个干扰值对应的PCI，若有几个PCI对应干扰值相同，则选择最小值，赋予A1；
		int bestPci = 0;
		double minInterValue = calNotSameStationCellsTotalInterVal(plan, cellId, 0, notSameStationCells);
		for (int pci = 1; pci <= 503; pci++) {
			double currentInterValue = calNotSameStationCellsTotalInterVal(plan, cellId, pci, notSameStationCells);
			if (currentInterValue < minInterValue) {
				minInterValue = currentInterValue;
				bestPci = pci;
			}
		}

		// 把主小区PCI加到方案中
		plan.addCellToPci(cellId.intern(), bestPci);

		// 给同站其他小区分配 PCI
		selectBestPciForSameStationOtherCell(plan, cellId, bestPci, notSameStationCells, sameStationOtherCells);
	}

	private void assignPciToCellsPlan(PlanObj plan, List<String> topList) {
		if (topList != null && topList.size() > 0) {
			assignPciToCellsPlan(plan, topList, topList.get(0));
		}
	}

	/**
	 * 在首套方案中为所有主小区分配原始PCI
	 * 
	 * @param plan
	 * @param cellList
	 */
	private void assignOriPciByDescCells(PlanObj plan, List<String> cellList) {
		for (int i = 0; i < cellList.size(); i++) {
			String cellId = cellList.get(i).intern();
			// 为当前小区分配原始PCI
			plan.addCellToPci(cellId, cellToOriPci.get(cellId));
			// 为同站其他小区分配原始PCI
			List<String> ocells = cellToSameStationOtherCells.get(cellId);
			if (ocells != null) {
				for (int j = 0; j < ocells.size(); j++) {
					String ocellId = ocells.get(j).intern();
					if (cellToOriPci.get(ocellId) != null) {
						System.out.println("ocellId = " + ocellId + ",cellToOriPci.get(ocellId)="
								+ cellToOriPci.get(ocellId));
						plan.addCellToPci(ocellId, cellToOriPci.get(ocellId));
					}
				}
			}
		}
		for (String cellId : plan.getCellToInterVal().keySet()) {
			System.out.println("cellId= " + cellId + ",val = " + plan.getCellToInterVal().get(cellId));
		}
	}

	/**
	 * 进行邻区核查，为了保证服务小区跟所有邻区之间的pci不相等，相等就+-30来修正
	 * 
	 * @param plan 当前方案
	 * @param descCellList 小区与邻区列的映射的列表
	 */
	private void checkNCellPci(PlanObj plan, List<String> descCellList) {
		if (logger.isDebugEnabled()) {
			logger.debug("===>>>checkNCellPci():进入pci+-30循环");
		}
		int count = 0;
		// 检查服务小区，邻区之间是否存在同pci,存在则+30或-30
		for (int i = 0; i < descCellList.size(); i++) {
			List<String> cells = cellToNotSameStationCells.get(descCellList.get(i));
			if (cells == null)
				continue;

			// 最多检查前50个非同站小区
			int maxCheck = cells.size();
			if (maxCheck > 50) {
				maxCheck = 50;
			}

			// 加入服务小区,服务小区不能和其他小区相同pci
			cells.add(descCellList.get(i).intern());

			// 获取小区列所使用的pci
			Map<String, Integer> ncellToPci = new HashMap<String, Integer>();
			for (String c : cells) {
				ncellToPci.put(c.intern(), plan.getPciByCell(c));
			}

			for (int k = 0; k < maxCheck; k++) {
				for (int j = 0; j < maxCheck; j++) {
					if (count > 1000) {
						// logger.debug("保证服务小区跟所有邻区之间的pci不相等,修正循环已执行了1000次，break掉！");
						count = 0;
						break;
					}
					// logger.debug("pci+-30循环中："+count);
					if (k == j)
						continue;
					// 不需要优化的小区，跳过
					if (!config.getCellsNeedtoAssignList().contains(cells.get(k)))
						continue;
					int pci1 = plan.getPciByCell(cells.get(k));
					int pci2 = plan.getPciByCell(cells.get(j));
					if (pci1 == -1 || pci2 == -1)
						continue;
					if (pci1 == pci2) {
						if (503 >= (pci1 + 30)) {
							pci1 += 30;
							while (ncellToPci.containsValue(pci1)) {
								if (503 < (pci1 + 30))
									break;
								pci1 += 30;
							}
							plan.addCellToPci(cells.get(k), pci1);
							ncellToPci.put(cells.get(k).intern(), pci1);
						} else {
							pci1 -= 30;
							while (ncellToPci.containsValue(pci1)) {
								if (pci1 < 30)
									break;
								pci1 -= 30;
							}
							plan.addCellToPci(cells.get(k), pci1);
							ncellToPci.put(cells.get(k).intern(), pci1);
						}
					}
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("===>>>checkNCellPci():退出pci+-30循环");
		}
	}

	/**
	 * 检查在主小区距离限制范围内有无同频同pci的情况，有则进行+-30处理
	 * 
	 * @param plan
	 * @param descCellList
	 * @author chen.c10
	 * @date 2015年10月29日 上午9:42:44
	 * @company 怡创科技
	 * @version V1.0
	 */
	private void checkNeighborAreaPci(PlanObj plan, List<String> descNeedToAssignCellList) {
		logger.debug("===>>>checkNeighborAreaPci():进入pci+-30循环");

		long t1 = System.nanoTime();
		Map<String, Integer> cellToNewPci = new HashMap<String, Integer>(cellToOriPci);
		Map<Integer, Integer> pciToNewAllocAmount = new HashMap<Integer, Integer>(pciToAllocAmount);
		Map<String, Integer> planPci = plan.getCellToPci();
		for (String cid : planPci.keySet()) {
			cellToNewPci.put(cid, planPci.get(cid));
		}
		logger.debug("工参小区数量为:" + cellToNewPci.size());
		logger.debug("方案小区数量为：" + planPci.size());
		logger.debug("修改小区数量为：" + descNeedToAssignCellList.size());
		Map<Integer, List<String>> pciToCell = new HashMap<Integer, List<String>>();
		for (String cid : cellToNewPci.keySet()) {
			int pci = cellToNewPci.get(cid);
			// 如果方案列表里面不包含该小区
			if (!planPci.containsKey(cid)) {
				pciToNewAllocAmount.put(pci, pciToNewAllocAmount.get(pci) + 1);
			}
			if (!pciToCell.containsKey(pci)) {
				pciToCell.put(pci, new ArrayList<String>());
			}
			pciToCell.get(pci).add(cid);
		}

		List<String> cellToAllocList = new ArrayList<String>();
		List<String> descCellList = new ArrayList<String>(descNeedToAssignCellList);
		boolean flag = false;

		List<String> sstCells = null;
		int mPci = -1;
		int mPciGp = -1;
		Map<Integer, List<Integer>> pciGroup = new HashMap<Integer, List<Integer>>();
		List<String> tmpCells = null;

		for (String cellId : descCellList) {
			// 如果作为同站小区分配过了，则跳过
			if (cellToAllocList.contains(cellId)) {
				logger.debug("小区：" + cellId + "，作为同站小区处理过了。");
				continue;
			}
			mPci = cellToNewPci.get(cellId);
			mPciGp = (int) Math.floor(mPci / 3);
			// 获取该小区的同站小区
			sstCells = new ArrayList<String>();
			if (cellToSameStationOtherCells.containsKey(cellId)) {
				sstCells = cellToSameStationOtherCells.get(cellId);
			}
			// 与主小区同组的PCI列表，都需要参与计算
			Map<String, Integer> sgCellToPci = new HashMap<String, Integer>();
			// 将主小区本身加入其中
			sgCellToPci.put(cellId, mPci);
			// 计算同组小区
			for (String sstCellId : sstCells) {
				if (cellToNewPci.containsKey(sstCellId)) {
					int sstPci = cellToNewPci.get(sstCellId);
					if ((int) Math.floor(sstPci / 3) == mPciGp) {
						sgCellToPci.put(sstCellId.intern(), sstPci);
					}
				}
			}
			boolean sespCloseFlag = false;
			// 先检查有无同频同pci不满足距离限制
			for (String sgCell : sgCellToPci.keySet()) {
				int sgPci = sgCellToPci.get(sgCell);
				String sgEarfcn = cellToEarfcn.get(cellId);
				tmpCells = pciToCell.get(sgPci);
				// 检查五公里有无同频同PCI
				// 先找出同频同pci的小区
				List<String> sespCells = new ArrayList<String>();
				for (String spCellId : tmpCells) {
					if (!sgCell.equals(spCellId) && cellToEarfcn.get(spCellId).equals(sgEarfcn)) {
						sespCells.add(spCellId.intern());
					}
				}
				tmpCells = null;
				// 检查有无小于距离限制的小区
				if (sespCells.size() > 0) {
					for (String sespCellId : sespCells) {
						double dis = calDistance(cellToLonLat.get(sespCellId), cellToLonLat.get(sgCell));
						if (dis < config.getDislimit()) {
							sespCloseFlag = true;
							break;
						}
					}
				}
				sespCells = null;
			}
			// 如果有同频同pci不满足距离限制
			if (sespCloseFlag) {
				// 按PCI除以3的整数部分进行分组，如1和2都为0，为0组，4，和5都为1，为1组。
				pciGroup.clear();
				// 0到503可以分为168组
				for (int i = 0; i < 168; i++) {
					// 排除主小区本身的组，选择与主小区组模10值相同的组，这样能保证PCI模30值相同。
					if (i != mPci / 3 && i % 10 == mPci / 3 % 10) {
						List<Integer> l = new ArrayList<Integer>();
						// 组号*3再加上PCI的模3值可得到与PCI模30值相同的PCI
						for (int j : sgCellToPci.values()) {
							l.add(i * 3 + j % 3);
						}
						pciGroup.put(i, l);
					}
				}
				// 用于配对的模30PCI组分配完毕。
				Map<Integer, Double> groupToDis = new HashMap<Integer, Double>();
				Map<Integer, Double> mcPciGpToDis = new HashMap<Integer, Double>();
				// 按组号进行循环，计算每一组的情况。
				for (int pciGpId : pciGroup.keySet()) {
					// 获取该组的PCI
					List<Integer> oneGpPcilist = pciGroup.get(pciGpId);
					List<Double> minDisList = new ArrayList<Double>();
					// 对该组中每个PCI进行计算。
					for (int pci : oneGpPcilist) {
						// 获取该PCI对应的cellId,只为了获取该小区频点，待优化。
						String tmpCellId = "";
						for (String c : sgCellToPci.keySet()) {
							if (pci % 30 == sgCellToPci.get(c) % 30) {
								tmpCellId = c;
							}
						}
						// 防止获取不到cellId，保护
						if (tmpCellId.equals("")) {
							continue;
						}
						// 获取频点
						String sgEarfcn = cellToEarfcn.get(tmpCellId);
						// 检查五公里有无同频PCI为待选PCI的小区
						List<String> sespCells = new ArrayList<String>();
						if (pciToCell.containsKey(pci)) {// 判断已分配pci
							for (String spCellId : pciToCell.get(pci)) {
								if (cellToEarfcn.get(spCellId).equals(sgEarfcn)) {
									sespCells.add(spCellId.intern());
								}
							}
						}
						Map<String, Double> closeCells = new HashMap<String, Double>();
						Map<String, Double> farCells = new HashMap<String, Double>();
						// 检查有无小于距离限制的小区
						if (sespCells.size() > 0) {
							for (String sespCellId : sespCells) {
								// 计算同频小区与计算小区的距离。这里因为都是主小区的同站小区，tmpCellId可以用主小区ID代替。
								double dis = calDistance(cellToLonLat.get(sespCellId), cellToLonLat.get(tmpCellId));
								// 保存小区距离限制的小区
								if (dis < config.getDislimit()) {
									closeCells.put(sespCellId.intern(), dis);
								} else {
									farCells.put(sespCellId.intern(), dis);
								}
							}
						}
						// 如果5公里内有同频小区，保存最小距离
						if (closeCells.size() > 0) {
							// 一组最多3个pci
							// 采用Double.MAX_VALUE/6来表示单个距离的最大值，保证后续求平均计算时不会导致超过double的情况
							double minDis = Double.MAX_VALUE / 6;
							for (String c : closeCells.keySet()) {
								if (minDis > closeCells.get(c)) {
									minDis = closeCells.get(c);
								}
							}
							minDisList.add(minDis);
						} else if (farCells.size() > 0) {// 保存结果为距离限制
							double minDis = Double.MAX_VALUE / 6;
							for (String c : farCells.keySet()) {
								if (minDis > farCells.get(c)) {
									minDis = farCells.get(c);
								}
							}
							minDisList.add(minDis);
						} else {
							// 如果以上两个条件都不满足，说明当前pci未使用过
							minDisList.add(Double.MAX_VALUE / 6);
						}
					}
					if (minDisList.size() > 0) {
						// 最近距离
						double minDis = Double.MAX_VALUE / 6;
						for (double d : minDisList) {
							if (minDis > d) {
								minDis = d;
							}
						}
						// 如果最近距离小于距离限制，则保存。
						if (minDis < config.getDislimit()) {
							groupToDis.put(pciGpId, minDis);
						} else {// 如果最近距离不小于距离限制，说明该组PCI每个PCI都满足要求
								// 选择找到符合要求的组就结束运算采用这组还是缓存这组数据？在所有满足条件的组中选择最优的？
							mcPciGpToDis.put(pciGpId, minDis);
						}
					} else {
						// 如果不满足距离限制的距离列表为空，说明该pci组可用
						mcPciGpToDis.put(pciGpId, Double.MAX_VALUE / 6);
					}
				}
				// 缓存最终分配PCI的组
				int tmpPciGp = mPciGp;
				// 如果不存在满足条件的PCI组，只能选择平均距离最远的组
				if (mcPciGpToDis.size() == 0) {
					// 发现同频同PCI不满足距离限制的小区，将标记改为真。
					flag = true;

					double maxDis = 0.0;
					// 获取平均距离最远的PCI组
					for (int pciGp : groupToDis.keySet()) {
						if (maxDis < groupToDis.get(pciGp)) {
							maxDis = groupToDis.get(pciGp);
							tmpPciGp = pciGp;
						}
					}
					// 如果有多个PCI距离相等，缓存
					List<Integer> sameDisPciList = new ArrayList<Integer>();
					for (int pciGp : groupToDis.keySet()) {
						if (maxDis == groupToDis.get(pciGp)) {
							sameDisPciList.add(pciGp);
						}
					}
					// 如果缓存多余一个，则选择复用度较小的,复用度再相等的情况不再考虑
					if (sameDisPciList.size() > 1) {
						int tmpLastAllocAmount = Integer.MAX_VALUE;
						// 循环平均距离相同的组
						for (int pciGp : sameDisPciList) {
							// 计算该组PCI的总复用度
							int totAllocAmount = 0;
							for (int pci : pciGroup.get(pciGp)) {
								totAllocAmount += pciToNewAllocAmount.get(pci);
							}
							// 如果总复用度有减少，更新，并保存相应的组号。
							if (tmpLastAllocAmount > totAllocAmount) {
								tmpLastAllocAmount = totAllocAmount;
								tmpPciGp = pciGp;
							}
						}
					}
				} else {
					// 允许一组或多组距离大于距离限制的组
					double maxDis = 0.0;
					// 获取平均距离最远的PCI组
					for (int pciGp : mcPciGpToDis.keySet()) {
						if (maxDis < mcPciGpToDis.get(pciGp)) {
							maxDis = mcPciGpToDis.get(pciGp);
							tmpPciGp = pciGp;
						}
					}
					// 如果有多个PCI距离相等，缓存
					List<Integer> sameDisPciList = new ArrayList<Integer>();
					for (int pciGp : mcPciGpToDis.keySet()) {
						if (maxDis == mcPciGpToDis.get(pciGp)) {
							sameDisPciList.add(pciGp);
						}
					}
					if (sameDisPciList.size() > 1) {
						int tmpLastAllocAmount = Integer.MAX_VALUE;
						// 循环平均距离相同的组
						for (int pciGp : sameDisPciList) {
							// 计算该组PCI的总复用度
							int totAllocAmount = 0;
							for (int pci : pciGroup.get(pciGp)) {
								totAllocAmount += pciToNewAllocAmount.get(pci);
							}
							// 如果总复用度有减少，更新，并保存相应的组号。
							if (tmpLastAllocAmount > totAllocAmount) {
								tmpLastAllocAmount = totAllocAmount;
								tmpPciGp = pciGp;
							}
						}
					}
				}
				// 如果最优的组有变化，则需要修改
				if (tmpPciGp != mPciGp) {
					// 为这组分配PCI
					for (int pci : pciGroup.get(tmpPciGp)) {
						for (String c : sgCellToPci.keySet()) {
							// 对PCI进行mod3匹配，找到这个pci对应的小区
							if (pci % 3 == sgCellToPci.get(c) % 3) {
								// 分配新PCI
								plan.addCellToPci(c, pci);

								// 全网小区缓存也要更新
								// 进行 PCI 分配次数的记录，先为新的 pci 分配次数累加1
								// 初始化时为复用次数分配了0，不进行null判断
								pciToNewAllocAmount.put(pci, pciToNewAllocAmount.get(pci) + 1);
								// 把cellId分配给新的pci
								if (!pciToCell.containsKey(pci)) {
									pciToCell.put(pci, new ArrayList<String>());
								}
								pciToCell.get(pci).add(c.intern());
								// 再把旧的 pci 分配次数减1
								if (cellToNewPci.containsKey(c)) {
									int oldPci = cellToNewPci.get(c);
									pciToNewAllocAmount.put(oldPci, pciToNewAllocAmount.get(oldPci) - 1);
									// 把cellId从旧的pci中删除
									pciToCell.get(cellToNewPci.get(c)).remove(c);
								}
								cellToNewPci.put(c.intern(), pci);

								// 添加到已经分配过的列表
								if (!cellToAllocList.contains(c)) {
									cellToAllocList.add(c.intern());
								}
							}
						}
					}
				} else {
					// 无变化也要将小区加入已经分配过的列表
					for (String sstCellId : sgCellToPci.keySet()) {
						if (!cellToAllocList.contains(sstCellId)) {
							cellToAllocList.add(sstCellId.intern());
						}
					}
				}
			} else {
				// 如果不需要更改，也要将主小区和他的同组同站小区加入到已处理列表。
				for (String sstCellId : sgCellToPci.keySet()) {
					if (!cellToAllocList.contains(sstCellId)) {
						cellToAllocList.add(sstCellId.intern());
					}
				}
			}
		}

		plan.setSamePciTooClose(flag);

		logger.info("-----本次距离运算，结果为：" + flag + "，descCellList size=" + plan.getCellToPci().size() + ",用时："
				+ ((System.nanoTime() - t1) / 1000000) + " 毫秒。---------");
		logger.debug("===>>>checkNeighborAreaPci:退出pci+-30循环");
	}

	/**
	 * map根据value值,从大到小排序
	 *
	 * @param unsortMap
	 * @return
	 * @author peng.jm
	 */
	private Map<String, Double> sortMapByValue(Map<String, Double> unsortMap) {
		List<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
		// sort list based on comparator
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return ((Comparable<Double>) o2.getValue()).compareTo(o1.getValue());
			}
		});
		// put sorted list into map again
		// LinkedHashMap make sure order in which keys were inserted
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = (Map.Entry<String, Double>) it.next();
			sortedMap.put(entry.getKey().intern(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * 对由long值组成的List<String>按照从小到大排序
	 * 
	 * @param unsortList
	 * @return
	 * @author chen.c10
	 * @date 2015年10月29日 下午12:03:23
	 * @company 怡创科技
	 * @version V1.0
	 */
	private List<String> sortStringList(List<String> unsortList) {
		Collections.sort(unsortList, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return Long.compare(Long.parseLong(o1), Long.parseLong(o2));
			}
		});
		return unsortList;
	}

	/**
	 * 计算两点距离
	 * 在赤道和本初子午线分成4个区域中，同区域两点间没有正负要求，不同区域时，以北半球和东半球的经纬度为正。
	 * 
	 * @param lonA
	 * @param latA
	 * @param lonB
	 * @param latB
	 * @return
	 * @author chen.c10
	 * @date 2015-9-2 下午2:07:42
	 * @company 怡创科技
	 * @version V1.0
	 */
	public static double calDistance(double lonA, double latA, double lonB, double latB) {
		double mLatA = latA;
		double mLatB = latB;
		double mLonA = 90.0 - lonA;
		double mLonB = 90.0 - lonB;
		double C = Math.sin(mLatA) * Math.sin(mLatB) * Math.cos(mLonA - mLonB) + Math.cos(mLatA) * Math.cos(mLatB);
		return R * Math.acos(C) * Math.PI / 180;
	}

	public static double calDistance(double[] A, double[] B) {
		return calDistance(A[0], A[1], B[0], B[1]);
	}

	/**
	 * 是否有距离大于阈值的情况
	 * 
	 * @param ld
	 * @param disLimit
	 * @return
	 * @author chen.c10
	 * @date 2015-9-2 下午4:53:20
	 * @company 怡创科技
	 * @version V1.0
	 */
	private boolean isOverDistance(Map<String, double[]> md, double disLimit) {
		boolean flag = false;
		double dis = 0.0;
		for (String c1 : md.keySet()) {
			for (String c2 : md.keySet()) {
				if (!c1.equals(c2) && !cellToEarfcn.get(c1).equals(cellToEarfcn.get(c2))) {
					dis = calDistance(md.get(c1), md.get(c2));
				}
				if (dis > disLimit) {
					flag = true;
				}
			}
		}
		return flag;
	}

	/**
	 * 将PCI分成168个组，每组三个PCI。
	 */
	class PciUtil {
		// 按照[0,1,2],[3,4,5]....分组
		List<List<Integer>> pciGroups = new ArrayList<List<Integer>>();

		// 当前获取到第几个pci组
		int step = 0;

		public PciUtil() {
			List<Integer> temp = new ArrayList<Integer>();

			for (int i = 0; i <= 503; i++) {
				temp.add(i);
				if ((i + 1) % 3 == 0) {
					pciGroups.add(temp);
					temp = new ArrayList<Integer>();
				}
			}
		}

		/**
		 * 取到 PCI 所在的分组
		 * 
		 * @param pci PCI 值
		 * @return 所在组的 PCI 列表
		 */
		public List<Integer> getPciGroup(int pci) {
			List<Integer> result = new ArrayList<Integer>();
			for (Integer one : pciGroups.get(pci / 3)) {
				result.add(one);
			}
			return result;
		}

		/**
		 * 按顺序取到一个 PCI 分组
		 * 
		 * @return 一组的 PCI 列表
		 */
		public List<Integer> getOneGroup() {
			if (step > 167) {
				step = 0;
			}
			// 创建新的list不影响源数据
			List<Integer> result = new ArrayList<Integer>();
			for (Integer one : pciGroups.get(step)) {
				result.add(one);
			}
			step = step + 1;
			return result;
		}
	}

	/**
	 * PCI优化方案
	 */
	class PlanObj implements Cloneable {

		/** 保存已分配 PCI 的小区 **/
		HashMap<String, Integer> cellToPci = new HashMap<String, Integer>();

		/** 保存已计算干拢值的小区 **/
		HashMap<String, Double> cellToInterVal = new HashMap<String, Double>();

		/** 保存PCI与小区的映射，用于计算同PCI小区距离 **/
		Map<Integer, List<String>> pciToCell = new HashMap<Integer, List<String>>();

		/** 距离限制,单位米 **/
		double disLimit = 5000;

		/** 是否同pci出现的距离太近 **/
		boolean isSamePciTooClose = false;

		public void setDisLimit(double disLimit) {
			this.disLimit = disLimit;
		}

		public void setSamePciTooClose(boolean isSamePciTooClose) {
			this.isSamePciTooClose = isSamePciTooClose;
		}

		public boolean isSamePciTooClose() {
			return isSamePciTooClose;
		}

		public HashMap<String, Integer> getCellToPci() {
			return cellToPci;
		}

		/**
		 * 获取已计算干拢值的小区MAP
		 * 
		 * @return 已计算干拢值的小区MAP
		 */
		public HashMap<String, Double> getCellToInterVal() {
			return cellToInterVal;
		}

		public void setCellToInterVal(HashMap<String, Double> cellToInterVal) {
			this.cellToInterVal = cellToInterVal;
		}

		public void addCellToPci(String cellId, Integer pci) {
			if (pci > 503 || pci < 0)
				return;
			// 进行 PCI 分配次数的记录，先为新的 pci 分配次数累加1
			pciToAllocAmount.put(pci, pciToAllocAmount.get(pci) + 1);
			// 把cellId分配给新的pci
			if (pciToCell.get(pci) != null && pciToCell.get(pci).size() > 0) {
				pciToCell.get(pci).add(cellId.intern());
			} else {
				pciToCell.put(pci, new ArrayList<String>());
				pciToCell.get(pci).add(cellId.intern());
			}
			// 再把旧的 pci 分配次数减1
			if (cellToPci.get(cellId) != null) {
				int oldPci = cellToPci.get(cellId);
				pciToAllocAmount.put(oldPci, pciToAllocAmount.get(oldPci) - 1);
				// 把cellId从旧的pci中删除
				pciToCell.get(cellToPci.get(cellId)).remove(cellId);
			}
			this.cellToPci.put(cellId.intern(), pci);
		}

		public void addCellToInterVal(String cellId, double interVal) {
			this.cellToInterVal.put(cellId.intern(), interVal);
		}

		public Map<Integer, List<String>> getPciToCell() {
			return pciToCell;
		}

		public int getPciByCell(String cellId) {
			int pci = -1;
			if (isAssigned(cellId)) {
				pci = cellToPci.get(cellId);
			}
			return pci;
		}

		public double getInterValByCell(String cellId) {
			double result = 0;
			if (cellToInterVal.containsKey(cellId)) {
				result = cellToInterVal.get(cellId);
			}
			return result;
		}

		/**
		 * 是否已分配
		 * 
		 * @param cellId 小区ID
		 * @return
		 */
		public boolean isAssigned(String cellId) {
			boolean flag = false;
			if (cellToPci.containsKey(cellId)) {
				flag = true;
				if (cellToPci.get(cellId) == -1) {
					flag = false;
				}
			}
			return flag;
		}

		/**
		 * 计算当前方案的每个小区对应的干扰值
		 * 
		 * @param cellToNcells
		 */
		public void calInterVal(Map<String, List<String>> cellToNcells) {
			for (String cellId : cellToPci.keySet()) {
				double ks = 1;
				if (cellToFlow.get(cellId) != null) {
					if (!(config.getKs() == -1.0)) {
						ks = calKs(cellId);
					}
				}
				int cellPci = cellToPci.get(cellId);
				List<String> ncells = cellToNcells.get(cellId);
				if (ncells == null)
					continue;
				double interVal = 0;
				for (int i = 0; i < ncells.size(); i++) {
					interVal += getModValByPciAndCell(cellId, ncells.get(i), cellPci)
							* getRelaValByCell1AndCell2(cellId, ncells.get(i));
				}
				cellToInterVal.put(cellId.intern(), ks * interVal);
			}
		}

		/**
		 *
		 * @param cellId
		 * @param ncellId
		 * @param cellPci
		 * @return
		 */
		public double getModValByPciAndCell(String cellId, String ncellId, int cellPci) {
			double result = 0;
			Integer ncellPci = getPciByCell(ncellId);
			if (ncellPci == null || ncellPci == -1) {
				ncellPci = cellToOriPci.get(ncellId);
			}
			if (ncellPci != null && ncellPci != -1) {
				if (cellPci % 3 == (ncellPci % 3)) {
					result += config.getM3r();
				}
				if (cellPci % 6 == (ncellPci % 6)) {
					result += config.getM6r();
				}
				if (cellPci % 30 == (ncellPci % 30)) {
					result += config.getM30r();
				}
			}
			return result;
		}

		public double getRelaValByCell1AndCell2(String cell1, String cell2) {
			double interVal = 0;
			if (cellToNcellAssocDegree.get(cell1) != null) {
				if (cellToNcellAssocDegree.get(cell1).get(cell2) != null) {
					interVal = cellToNcellAssocDegree.get(cell1).get(cell2);
				}
			}
			return interVal;
		}

		public double getTotalInterVal() {
			double result = 0;
			for (String cellId : cellToInterVal.keySet()) {
				result += cellToInterVal.get(cellId);
			}
			return result;
		}

		/**
		 * 清除需要重新分配的小区,才能重新分配pci
		 * 
		 * @param descCellList 小区列表
		 */
		public void clearCellsInfo(List<String> descCellList) {
			if (descCellList != null) {
				for (String cellId : descCellList) {
					// 去除小区pci
					cellToPci.remove(cellId);
					// 去除同站小区pci
					if (cellToSameStationOtherCells.get(cellId) != null) {
						for (String c : cellToSameStationOtherCells.get(cellId)) {
							cellToPci.remove(c);
						}
					}
					/*
					 * // 去除邻区
					 * if (cellToMaxNcells.get(cellId) != null) {
					 * for (String c : cellToMaxNcells.get(cellId)) {
					 * cellToPci.remove(c);
					 * }
					 * }
					 */
				}
			}
		}

		public boolean ifSamePciTooClose() {
			boolean closeFlag = false;
			List<String> cellList;
			Map<String, double[]> lonLatList;
			int n = 0;
			do {
				isSamePciTooClose = false;
				for (int doPci : pciToCell.keySet()) {
					cellList = pciToCell.get(doPci);
					lonLatList = new HashMap<String, double[]>();
					for (String cTmp : cellList) {
						if (cellToLonLat.get(cTmp) != null && cellToLonLat.get(cTmp).length == 2
								&& (cellToLonLat.get(cTmp)[0] < 1000.0 && cellToLonLat.get(cTmp)[1] < 1000.0)) {
							lonLatList.put(cTmp, cellToLonLat.get(cTmp));
						}
					}
					if (lonLatList.size() > 1) {
						closeFlag = isOverDistance(lonLatList, disLimit);
						if (closeFlag) {
							isSamePciTooClose = closeFlag;
						}
					}
				}
			} while (isSamePciTooClose && n < 10);
			System.out.println("====》》本次检查有无同频同pci结果为：" + isSamePciTooClose);
			return isSamePciTooClose;
		}

		@SuppressWarnings("unchecked")
		public PlanObj clone() {
			PlanObj res = null;
			try {
				res = (PlanObj) super.clone();
				res.cellToPci = (HashMap<String, Integer>) cellToPci.clone();
				res.cellToInterVal = (HashMap<String, Double>) cellToInterVal.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return res;
		}
	}

	/**
	 * 保存需要输出的关联表到hdfs文件系统上
	 *
	 * @param plan
	 *            需要输出的关联表
	 * @param suffix
	 *            文件后缀
	 * @return
	 * @author li.tf
	 * @date 2015年12月4日10:56:26
	 */
	private void saveAssoTableInHdfs(List<String> cellList, Map<String, Double> asso, String suffix) {
		String realFilePath = config.getFilePath() + "/" + config.getJobId() + suffix;
		if (logger.isDebugEnabled()) {
			logger.debug("把关联表写入到HDFS文件:" + realFilePath);
		}

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}

		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("PciReducer过程：保存关联表时，删除原有关联表出错！");
			e2.printStackTrace();
		}

		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));

		// 创建流
		OutputStream dataOs = null;
		try {
			dataOs = fs.create(filePathObj, true);
		} catch (IOException e1) {
			logger.error("PciReducer过程：创建输出数据文件流出错！");
			e1.printStackTrace();
		}

		// 根据规则写入数据
		DataOutputStream dataDOS = new DataOutputStream(dataOs);
		try {
			// 小区数量
			dataDOS.writeInt(cellList.size());
			// 写入数据中存在的小区ID以及关联度
			for (String cellId : cellList) {
				// 小区ID
				dataDOS.writeUTF(cellId);
				// 小区关联度
				if (asso.get(cellId) != null) {
					dataDOS.writeDouble(asso.get(cellId));
				} else {
					dataDOS.writeDouble(0.0);
				}
			}
			// 结束符，判断文件是否完整
			dataDOS.writeUTF("finished");
		} catch (IOException e) {
			logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			e.printStackTrace();
		} finally {
			try {
				dataDOS.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 保存需要输出的方案到hdfs文件系统上
	 *
	 * @param plan
	 *            需要输出的方案
	 * @param suffix
	 *            文件后缀（backup:最后一次成功的；current:当前最新）
	 * @return
	 * @author li.tf
	 * @date 2015年12月4日10:15:26
	 */
	private void saveExportPlanInHdfs(PlanObj plan, List<String> topCellList, String suffix) {
		String realFilePath = config.getFilePath() + "/" + config.getJobId() + suffix;
		if (logger.isDebugEnabled()) {
			logger.debug("把方案写入到HDFS文件:" + realFilePath);
		}

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}

		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("PciReducer过程：保存方案时，删除原有文件出错！");
			e2.printStackTrace();
		}

		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));

		// 创建流
		OutputStream dataOs = null;
		try {
			dataOs = fs.create(filePathObj, true);
		} catch (IOException e1) {
			logger.error("PciReducer过程：创建输出数据文件流出错！");
			e1.printStackTrace();
		}

		// 根据规则写入数据
		DataOutputStream dataDOS = new DataOutputStream(dataOs);
		try {
			// 将MR数据和待优化PCI的小区都并在一起输出
			// MR数据存在原始干扰值，计算干扰值，计算PCI
			// 待优化PCI小区中可能存在着部分小区在这批MR数据中并不存在，所以不能进行计算干扰值和分配PCI
			// 这里处理的方法是将MR数据中的存在的所有小区都输出，然后检查待优化小区哪些是不存在于MR数据中的小区
			// 将这些小区也以默认值输出，默认原干扰值0，计算干扰值0，PCI为-1

			// 检查不存在与MR数据的小区
			List<String> cellsNotInData = new ArrayList<String>();
			for (String cellId : config.getCellsNeedtoAssignList()) {
				if (!plan.getCellToPci().containsKey(cellId)) {
					cellsNotInData.add(cellId.intern());
				}
			}
			int size = plan.getCellToPci().size() + cellsNotInData.size();
			int topCellListSize = topCellList.size();
			// 总干扰值
			dataDOS.writeDouble(plan.getTotalInterVal());
			// 需要优化的小区数量
			dataDOS.writeInt(size);
			dataDOS.writeInt(topCellListSize);

			List<String> cellToPci = new ArrayList<String>(plan.getCellToPci().keySet());
			cellToPci = sortStringList(cellToPci);
			// 写入数据中存在的小区PCI以及干扰值
			for (String cellId : cellToPci) {
				// 小区ID
				dataDOS.writeUTF(cellId);
				// 小区计算PCI
				dataDOS.writeInt(plan.getPciByCell(cellId));
				// 小区原干扰值
				if (cellToOriInterVal.get(cellId) != null) {
					dataDOS.writeDouble(cellToOriInterVal.get(cellId));
				} else {
					dataDOS.writeDouble(0.0);
				}
				// 小区计算干扰值
				dataDOS.writeDouble(plan.getInterValByCell(cellId));
			}
			cellsNotInData = sortStringList(cellsNotInData);
			// 写入数据中不存在的小区默认PCI以及干扰值
			for (String cellId : cellsNotInData) {
				dataDOS.writeUTF(cellId);
				dataDOS.writeInt(-1);
				dataDOS.writeDouble(0.0);
				dataDOS.writeDouble(0.0);
			}
			for (String cellId : topCellList) {
				dataDOS.writeUTF(cellId);
				dataDOS.writeDouble(plan.getInterValByCell(cellId));
			}
			// 结束符，判断文件是否完整
			dataDOS.writeUTF("finished");
		} catch (IOException e) {
			logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			e.printStackTrace();
		} finally {
			try {
				dataDOS.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 保存中间方案的个数到hdfs文件系统上
	 *
	 * @param planNum
	 *            需要输出的中间方案的个数
	 * @return
	 * @author li.tf
	 * @date 2015年12月4日14:16:26
	 */
	private void saveMidPlanCountInHdfs(int planNum) {
		String realFilePath = config.getFilePath() + "/" + config.getJobId() + "_MidPlanCount.current";
		if (logger.isDebugEnabled()) {
			logger.debug("把方案写入到HDFS文件:" + realFilePath);
		}

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}

		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("PciReducer过程：保存方案时，删除原有文件出错！");
			e2.printStackTrace();
		}

		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));

		// 创建流
		OutputStream dataOs = null;
		try {
			dataOs = fs.create(filePathObj, true);
		} catch (IOException e1) {
			logger.error("PciReducer过程：创建输出数据文件流出错！");
			e1.printStackTrace();
		}

		// 根据规则写入数据
		DataOutputStream dataDOS = new DataOutputStream(dataOs);
		try {
			dataDOS.writeInt(planNum);
			// 结束符，判断文件是否完整
			dataDOS.writeUTF("finished");
		} catch (IOException e) {
			logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			e.printStackTrace();
		} finally {
			try {
				dataDOS.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 保存需要输出的邻区核查方案到hdfs文件系统上
	 *
	 * @param plan
	 *            需要输出的邻区核查方案
	 * @param suffix
	 *            文件后缀
	 * @return
	 * @author li.tf
	 * @date 2015年12月4日15:16:26
	 */
	private void saveNcCheckPlanInHdfs(PlanObj plan, String suffix) {
		String realFilePath = config.getFilePath() + "/" + config.getJobId() + suffix;
		if (logger.isDebugEnabled()) {
			logger.debug("把关联表写入到HDFS文件:" + realFilePath);
		}

		FileSystem fs = null;
		try {
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			logger.error("PciReducer过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}

		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			logger.error("PciReducer过程：保存关联表时，删除原有关联表出错！");
			e2.printStackTrace();
		}

		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));

		// 创建流
		DataOutputStream dataDOS = null;
		try {
			FSDataOutputStream fdos = fs.create(filePathObj, true);
			if (fdos != null) {
				dataDOS = new DataOutputStream(fdos);
			}
		} catch (IOException e1) {
			logger.error("PciReducer过程：创建输出数据文件流出错！");
			e1.printStackTrace();
		}

		// 根据规则写入数据

		try {
			List<String> cellsNotInData = new ArrayList<String>();
			for (String cellId : config.getCellsNeedtoAssignList()) {
				if (!plan.getCellToPci().containsKey(cellId)) {
					cellsNotInData.add(cellId.intern());
				}
			}
			int size = plan.getCellToPci().size() + cellsNotInData.size();

			// 总干扰值
			dataDOS.writeDouble(plan.getTotalInterVal());
			// 需要优化的小区数量
			dataDOS.writeInt(size);

			List<String> cellToPci = new ArrayList<String>(plan.getCellToPci().keySet());
			cellToPci = sortStringList(cellToPci);
			// 写入数据中存在的小区PCI以及干扰值
			for (String cellId : cellToPci) {
				// 小区ID
				dataDOS.writeUTF(cellId);
				// 小区计算PCI
				dataDOS.writeInt(plan.getPciByCell(cellId));
				// 小区原干扰值
				if (cellToOriInterVal.get(cellId) != null) {
					dataDOS.writeDouble(cellToOriInterVal.get(cellId));
				} else {
					dataDOS.writeDouble(0.0);
				}
				// 小区计算干扰值
				dataDOS.writeDouble(plan.getInterValByCell(cellId));
			}
			cellsNotInData = sortStringList(cellsNotInData);
			// 写入数据中不存在的小区默认PCI以及干扰值
			for (String cellId : cellsNotInData) {
				dataDOS.writeUTF(cellId);
				dataDOS.writeInt(-1);
				dataDOS.writeDouble(0.0);
				dataDOS.writeDouble(0.0);
			}

			// 结束符，判断文件是否完整
			dataDOS.writeUTF("finished");
		} catch (IOException e) {
			logger.error("PciReducer过程: pci规划方案文件写入内容时失败！");
			e.printStackTrace();
		} finally {
			try {
				dataDOS.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private double calKs(String cellId) {
		double ks = 1;
		double flow = 0;
		if (cellToFlow.get(cellId) != null) {
			flow = cellToFlow.get(cellId) < 100 ? 100 : cellToFlow.get(cellId);
			if (cellToIsDTCell.get(cellId).equals("Y")) {
				ks = 1 + config.getKs() * flow / perFlow;
			} else {
				ks = flow / perFlow;
			}
		}
		return ks;
	}
}