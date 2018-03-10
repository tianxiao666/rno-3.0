package com.iscreate.op.service.rno.mapreduce.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.Text;

import com.iscreate.op.service.rno.mapreduce.model.NewCell;
import com.iscreate.op.service.rno.mapreduce.model.NewNcell;
import com.iscreate.op.service.rno.mapreduce.pci.NewPciConfig;

public class PciUtil {

	/** 工参小区表 **/
	List<String> oriCellList = new ArrayList<String>();
	/** 所有主小区 **/
	List<String> cellList = new ArrayList<String>();

	List<String> abCellList;

	String valStr = "";

	String[] keyArr;

	NewCell cell;

	double samefreqcellcoefweight = 0.8; // 权值
	double switchratioweight = 0.2; // 切换比例权值

	double rsrp0minus1weight = 0.8; // 邻区减主小区大于-6小于-3的测量报告数 RSRPtimes0- RSRPtimes1 权值
	double rsrp1weight = 1.2; // 邻区减主小区大于-3的测量报告数 RSRPtimes1 权值

	long minmeasuresum = 0;
	double mincorrelation = 0;

	/** 小区与原PCI的映射 **/
	Map<String, Integer> cellToOriPciFromData;

	/** 小区到经纬度的映射，不重复 **/
	Map<String, double[]> cellToLonLatFromData;

	/** 小区与频率的映射 **/
	Map<String, Integer> cellToEarfcnFromData;

	// reduce 处理次数统计
	long counter = 0;

	// 处理行数统计
	long lineCounter = 0;

	/** MR 处理行统计 **/
	long mrLineCounter = 0;

	/** HO 处理行统计 **/
	long hoLineCounter = 0;

	/** SF 处理行统计 **/
	long sfLineCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	public PciUtil(NewPciConfig config, long startTimeMillis) {
		super();
		this.startTimeMillis = startTimeMillis;
		this.minmeasuresum = config.getMinmeasuresum();
		this.mincorrelation = config.getMincorrelation();
		this.oriCellList = config.getCellList();
		this.samefreqcellcoefweight = config.getSamefreqcellcoefweight();
		this.switchratioweight = config.getSwitchratioweight();
		this.rsrp0minus1weight = config.getRsrp0minus1weight();
		this.rsrp1weight = config.getRsrp1weight();
		this.abCellList = config.getAbCellList();
		this.cellToOriPciFromData = config.getCellToOriPciFromData();
		this.cellToLonLatFromData = config.getCellToLonLatFromData();
		this.cellToEarfcnFromData = config.getCellToEarfcnFromData();
	}

	public long getCounter() {
		return counter;
	}

	public long getLineCounter() {
		return lineCounter;
	}

	public long getMrLineCounter() {
		return mrLineCounter;
	}

	public long getHoLineCounter() {
		return hoLineCounter;
	}

	public long getSfLineCounter() {
		return sfLineCounter;
	}

	public NewCell handleReducerData(Text key, Iterable<Text> values) {
		if (++counter % 1000 == 0) {
			System.out.println("reduce counter = " + counter + "  ,reduce time =  "
					+ (System.currentTimeMillis() - startTimeMillis));
		}
		keyArr = key.toString().split(",");
		if ("".equals(keyArr[0])) {
			return null;
		}
		// 如果有重复的 cellId，则跳过不处理，一般是不会有这种情况的。
		if ((!oriCellList.isEmpty() && !oriCellList.contains(keyArr[0])) || cellList.contains(keyArr[0])) {
			// System.out.println("cellId="+cellId +",工参不匹配，或重复，过滤");
			return null;
		} else {
			cellList.add(keyArr[0].intern());
		}
		cell = new NewCell(keyArr[0]);

		cell.setPci(Integer.parseInt(keyArr[1]));
		cell.setCellBcch(Integer.parseInt(keyArr[2]));
		// 暂存MR中的工参
		if (!cellToOriPciFromData.containsKey(keyArr[0])) {
			cellToOriPciFromData.put(keyArr[0].intern(), Integer.parseInt(keyArr[1]));
		}
		if (!cellToEarfcnFromData.containsKey(keyArr[0])) {
			cellToEarfcnFromData.put(keyArr[0].intern(), Integer.parseInt(keyArr[2]));
		}
		// 合并小区 MR 数据，如果返回 false，则过滤掉该小区
		if (!mergeMrData(cell, values)) {
			cell = null;
			return cell;
		}
		return cell;
	}

	/**
	 * 合并 MR 测量数据
	 * 
	 * @param cell
	 * @param values
	 * @return 返回 false 则 cell 对象被过滤
	 */
	private boolean mergeMrData(NewCell cell, Iterable<Text> values) {
		int num = 0;
		for (Text val : values) {
			lineCounter++;
			valStr = val.toString();
			if (valStr.endsWith(",MR")) {
				sumMrData(valStr.split(","), cell);
			} else if (valStr.endsWith(",HO")) {
				sumHoData(valStr.split(","), cell);
			} else if (valStr.endsWith(",SF")) {
				if (!(num++ > 0) && !abCellList.contains(cell.getId())) {
					abCellList.add(cell.getId());
				}
				sumSfData(valStr.split(","), cell);
			} else {
				System.out.println("cellId=" + cell.getId() + ",其中一条数据不合法");
			}
		}

		if (cell.getNcells().isEmpty()) {
			return false;
		}
		// 计算mr关联度,计算ho关联度
		if (!cell.calcAssocDegree(rsrp1weight, rsrp0minus1weight, minmeasuresum, samefreqcellcoefweight,
				switchratioweight)) {
			return false;
		}
		// 计算总关联度
		cell.calcTotalAssocDegree(mincorrelation, cellToOriPciFromData, cellToEarfcnFromData);
		// 清理缓存数据
		return true;
	}

	public void sumMrData(String indexs[], NewCell scell) {
		mrLineCounter++;

		int ncellId = 0;

		// 第一次整理数据据
		if (indexs.length == 9 && !"".equals(indexs[0])) {
			ncellId = Integer.parseInt(indexs[0]);
		} else {
			return;
		}
		if (!cellToEarfcnFromData.containsKey(String.valueOf(ncellId))) {
			cellToEarfcnFromData.put(String.valueOf(ncellId).intern(), Integer.parseInt(indexs[2]));
		}
		// meaTime mixingSum
		scell.putMeaTimeToMixingSum(indexs[7], Double.parseDouble(indexs[6]));

		// ncellid ncellbcch ncellpci
		NewNcell ncell = new NewNcell(ncellId, Integer.parseInt(indexs[2]), Integer.parseInt(indexs[1]));
		// timetotal rsrp0 rsrp1
		ncell.addMrData(Double.parseDouble(indexs[3]), Double.parseDouble(indexs[4]), Double.parseDouble(indexs[5]));
		scell.addNcell(ncell);
	}

	public void sumSfData(String indexs[], NewCell scell) {
		sfLineCounter++;

		int ncellId = 0;
		// 第一次整理数据据

		// 第一次整理数据据
		if (indexs.length == 8 && !"".equals(indexs[0])) {
			ncellId = Integer.parseInt(indexs[0]);
			if (!abCellList.contains(ncellId)) {
				abCellList.add((ncellId + "").intern());
			}
		} else {
			return;
		}
		// ncellId ncellBcch ncellPci
		NewNcell ncell = new NewNcell(ncellId, Integer.parseInt(indexs[2]), Integer.parseInt(indexs[1]));
		// timesTotal rsrpTimes0 rsrpTimes1
		ncell.addRsData(Double.parseDouble(indexs[5]), Double.parseDouble(indexs[3]), Double.parseDouble(indexs[4]));
		scell.addNcell(ncell);
	}

	public void sumHoData(String indexs[], NewCell scell) {
		hoLineCounter++;

		int ncellId = 0;

		if (indexs.length == 6 && !"".equals(indexs[0])) {
			ncellId = Integer.parseInt(indexs[0]);
		} else {
			return;
		}

		// 更新频点
		if (!cellToEarfcnFromData.containsKey(String.valueOf(ncellId))) {
			cellToEarfcnFromData.put(String.valueOf(ncellId).intern(), Integer.parseInt(indexs[2]));
		}
		// ncellId ncellBcch ncellPci
		NewNcell ncell = new NewNcell(ncellId, Integer.parseInt(indexs[2]), Integer.parseInt(indexs[1]));
		// times
		ncell.addHoData(Double.parseDouble(indexs[3]));
		scell.addNcell(ncell);
	}

	/**
	 * map根据value值,从大到小排序
	 *
	 * @param unsortMap
	 * @return
	 * @author peng.jm
	 */
	public Map<String, Double> sortMapByValue(Map<String, Double> unsortMap) {
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
		for (Map.Entry<String, Double> entry : list) {
			sortedMap.put(entry.getKey().intern(), entry.getValue());
		}
		return sortedMap;
	}
}
