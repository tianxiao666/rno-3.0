package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.service.rno.mapreduce.model.Cell;
import com.iscreate.op.service.rno.mapreduce.model.Ncell;

public class PciReducer extends Reducer<Text, Text, Text, Text> {

	private static Logger logger = LoggerFactory.getLogger(PciReducer.class);

	// 配置
	private PciConfig config = null;

	/** 所有主小区 **/
	List<String> cellList = new ArrayList<String>();

	/** 所有邻区，不重复 **/
	List<String> ncellList = new ArrayList<String>();

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

	/** 小区与原PCI的映射 **/
	Map<String, Integer> cellToOriPciFromData = new HashMap<String, Integer>();

	/** 小区到经纬度的映射，不重复 **/
	Map<String, double[]> cellToLonLatFromData = new HashMap<String, double[]>();

	/** 小区与频率的映射 **/
	Map<String, String> cellToEarfcnFromData = new HashMap<String, String>();

	// reduce 处理次数统计
	long counter = 0;

	// 处理行数统计
	long lineCounter = 0;

	/** MR 处理行统计 **/
	long mrLineCounter = 0;

	/** HO 处理行统计 **/
	long hoLineCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	double samefreqcellcoefweight = 0.8; // 权值
	double switchratioweight = 0.2; // 切换比例权值

	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);

		startTimeMillis = System.currentTimeMillis();

		// 初始化配置信息
		config = new PciConfig(context);

		samefreqcellcoefweight = config.getSamefreqcellcoefweight();
		switchratioweight = config.getSwitchratioweight();

		cellToSameStationOtherCells = config.getCellToSameStationOtherCells();
		cellToNotSameStationCells = config.getCellToNotSameStationCells();
		cellToNcellAssocDegree = config.getCellToNcellAssocDegree();
		cellToTotalAssocDegree = config.getCellToTotalAssocDegree();
		cellToOriPci = config.getCellToOriPci();
		cellToLonLat = config.getCellToLonLat();
		cellToEarfcn = config.getCellToEarfcn();

		logger.info("reduce counter = " + counter);
		logger.info("config = " + config.toString());
	}

	/**
	 * key 为 cellId，如 1868801
	 * values 格式为 ncellId,timestotal,time1,enodebId,cellPci,ncellPci,cellBcch,ncellBcch 的集合
	 * 如：1868803,120,53,186880,462,335,38100,38100
	 */
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		if (++counter % 1000 == 0) {
			System.out.println("reduce counter = " + counter + "  ,reduce time =  "
					+ (System.currentTimeMillis() - startTimeMillis));
		}
		System.out.println("key = "+key.toString());
		String[] keyArr = key.toString().split(",");
		String cellId = keyArr[0];
		String enodebId = keyArr[1];
		int pci = Integer.parseInt(keyArr[2]);
		String cellBcch = keyArr[3];
		double cellLon = Double.parseDouble(keyArr[4]);
		double cellLat = Double.parseDouble(keyArr[5]);
		if (cellId.equals("6775831")) {
			System.out.println(key.toString());
			for (Text val : values) {
				System.out.println(val.toString());
			}
		}
		// 如果有重复的 cellId，则跳过不处理，一般是不会有这种情况的。
		if (cellList.contains(cellId)) {
			System.out.println("filter cellId = "+cellId);
			return;
		} else {
			cellList.add(cellId.intern());
		}
		Cell cell = new Cell(cellId);

		cell.setPci(pci);
		cell.setCellBcch(Double.parseDouble(cellBcch));
		cell.setLon(cellLon);
		cell.setLat(cellLat);

		// 暂存MR中的工参
		if (!cellToOriPciFromData.containsKey(cellId)) {
			cellToOriPciFromData.put(cellId.intern(), pci);
		}
		if (!cellToLonLatFromData.containsKey(cellId)) {
			cellToLonLatFromData.put(cellId.intern(), new double[] { cellLon, cellLat });
		}
		if (!cellToEarfcnFromData.containsKey(cellId)) {
			cellToEarfcnFromData.put(cellId.intern(), cellBcch.intern());
		}
		// 处理同站其他小区
		System.out.println("enodebId _ cellBcch="+enodebId + "_" + cellBcch);
		String strSameStationCells = config.getEnodebToCells().get(enodebId + "_" + cellBcch);
		if (strSameStationCells != null && strSameStationCells.length() != 0) {
			// 1868801,1868802,1868803
			// 把主小区ID从同站小区列表中删除，剩下的就是同站其他小区
			System.out.println("strSameStationCells="+strSameStationCells);
			String[] sameStationCells = strSameStationCells.split(",");
			for (String ssCellId : sameStationCells) {
				ssCellId = ssCellId.trim();
				if (!ssCellId.equals(cellId)) {
					// 缓存MR中的工参数据
					// 存入频点与小区的映射，cellBcch前面主小区已经存在
					if (!cellToEarfcnFromData.containsKey(ssCellId)) {
						cellToEarfcnFromData.put(ssCellId.intern(), cellBcch.intern());
					}
					// 经纬度也与主小区相同
					if (!cellToLonLatFromData.containsKey(ssCellId)) {
						cellToLonLatFromData.put(ssCellId.intern(), new double[] { cellLon, cellLat });
					}
					cell.getSameStationOtherCells().add(ssCellId.intern());
				}
			}
		}
		// 合并小区 MR 数据，如果返回 false，则过滤掉该小区
		if (!mergeMrData(cell, values)) {
			cell = null;
			System.out.println("cell = null cellid="+cellId);
			return;
		}

		// 添加同站其他小区
		cellToSameStationOtherCells.put(cellId.intern(), cell.getSameStationOtherCells());

		Map<String, Double> ncellAssocDegree = new HashMap<String, Double>();

		for (Ncell ncell : cell.getNcells()) {
			// 邻区和对应的关联度
			ncellAssocDegree.put(ncell.getId().intern(), ncell.getAssocDegree());
		}

		// 获取邻区与关联度的映射，邻区列
		Map<String, Double> notSameStationCellsAssocDegree = new HashMap<String, Double>();
		List<String> notSameStationCells = new ArrayList<String>();
		for (String ncellId : ncellAssocDegree.keySet()) {
			ncellId = ncellId.trim().intern();
			boolean ok = true;
			for (String sameStatCell : cellToSameStationOtherCells.get(cellId)) {
				if (sameStatCell.equals(ncellId)) {
					ok = false;
				}
			}
			if (ok) {
				notSameStationCellsAssocDegree.put(ncellId.intern(), ncellAssocDegree.get(ncellId));
				notSameStationCells.add(ncellId.intern());
			}
		}

		// 对小区的非同站小区按关联度从大到小排序
		String tmpNcell = "";
		for (int i = 0; i < notSameStationCells.size(); i++) {
			for (int j = i + 1; j < notSameStationCells.size(); j++) {
				if (notSameStationCellsAssocDegree.get(notSameStationCells.get(i)) < notSameStationCellsAssocDegree
						.get(notSameStationCells.get(j))) {
					tmpNcell = notSameStationCells.get(i);
					notSameStationCells.set(i, notSameStationCells.get(j).intern());
					notSameStationCells.set(j, tmpNcell.intern());
				}
			}
		}

		// 保存小区与邻区关联度的映射（包含了同站其他小区）
		cellToNcellAssocDegree.put(cellId.intern(), ncellAssocDegree);
		// 保存小区与非同站小区列表的映射
		cellToNotSameStationCells.put(cellId.intern(), notSameStationCells);
		// 保存小区与总关联度的映射
		cellToTotalAssocDegree.put(cellId.intern(), cell.getTotalAssocDegree());
	}

	@Override
	public void cleanup(Context context) {
		logger.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
		logger.info("reduce counter total = " + counter);
		logger.info("reduce line counter total = " + lineCounter);
		logger.info("reduce mr line counter total = " + mrLineCounter);
		logger.info("reduce ho line counter total = " + hoLineCounter);

		startTimeMillis = System.currentTimeMillis();
		// 用MR的部分工参更新数据库中的工参
		if (cellToOriPci != null && cellToOriPciFromData != null) {
			cellToOriPci.putAll(cellToOriPciFromData);
		}
		// 经纬度不需要更新
		if (cellToLonLat != null && cellToLonLatFromData != null) {
			cellToLonLat.putAll(cellToLonLatFromData);
		}
		if (cellToEarfcn != null && cellToEarfcnFromData != null) {
			cellToEarfcn.putAll(cellToEarfcnFromData);
		}
		// 把Pci计算的数据写入到临时文件中，用于分析和调试
		if (logger.isDebugEnabled()) {
			writeObject();
			logger.info("数据写入临时文件完成,准备写入HDFS..");
		}

		// 打印日志，输出一些全局变量的值
		logger.info("cellToNcellAssocDegree size=" + cellToNcellAssocDegree.size());
		logger.info("cellToNotSameStationCells size=" + cellToNotSameStationCells.size());
		logger.info("cellToTotalAssocDegree size=" + cellToTotalAssocDegree.size());
		logger.info("cellToSameStationOtherCells size=" + cellToSameStationOtherCells.size());
		logger.info("cellToOriPci size=" + cellToOriPci.size());
		logger.info("cellToLonLat size=" + cellToLonLat.size());
		logger.info("cellToEarfcn size=" + cellToEarfcn.size());

		config.savePciDataListToHdfs();

		logger.info("cleanup finished. Spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	/**
	 * 把Pci计算的数据写入到临时文件中，用于分析和调试
	 */
	public void writeObject() {
		try {
			FileOutputStream outStream = new FileOutputStream("/tmp/PciCalc-" + config.getFileName() + ".dat");
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);

			objectOutputStream.writeObject(cellToNcellAssocDegree);
			objectOutputStream.writeObject(cellToNotSameStationCells);
			objectOutputStream.writeObject(cellToTotalAssocDegree);
			objectOutputStream.writeObject(cellToSameStationOtherCells);
			objectOutputStream.writeObject(cellToOriPci);
			objectOutputStream.writeObject(cellToLonLat);
			objectOutputStream.writeObject(cellToEarfcn);

			outStream.close();
			logger.info("Export PciCalcData.dat Success.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 合并 MR 测量数据
	 * 
	 * @param cell
	 * @param values
	 * @return 返回 false 则 cell 对象被过滤
	 */
	public boolean mergeMrData(Cell cell, Iterable<Text> values) {
		// 总的测量次数
		long sumTimesTotals = 0;

		String cellId = cell.getId().intern();

		List<Ncell> ncells = cell.getNcells();

		MrObj mrObj = new MrObj(cell);
		HoObj hoObj = new HoObj(cell);

		List<Ncell> tmpPciRecs = new ArrayList<Ncell>();

		for (Text val : values) {
			lineCounter++;
			String indexs[] = val.toString().split(",");
			if (val.toString().endsWith(",MR")) {
				mrObj.sumMrData(indexs);
			} else if (val.toString().endsWith(",HO")) {
				hoObj.sumHoData(indexs);
			} else {
				System.out.println("cellId=" + cellId + ",其中一条数据不合法");
			}
		}
		// 计算mr关联度,计算ho关联度
		if (!mrObj.getMrRelaDegree() || !hoObj.getHoRelaDegree()) {
			System.out.println("no RelaDegree cellId="+cellId);
			return false;
		}

		List<Ncell> mrNcells = cell.getMrNcells(), hoNcells = cell.getHoNcells();

		String mrNcell = "", hoNcell = "";

		double mrCosi = 0, hoCosi = 0;

		if (mrNcells.isEmpty() && hoNcells.isEmpty()) {
			System.out.println("no Ncells cellId="+cellId);
			return false;
		} else if (!mrNcells.isEmpty() && !hoNcells.isEmpty()) {
			ncells.addAll(mrNcells);
			for (Ncell g4HoPciRec : hoNcells) {
				boolean flag = true;
				hoNcell = g4HoPciRec.getId();
				hoCosi = g4HoPciRec.getAssocDegree();
				for (Ncell sumPciRec : ncells) {
					mrNcell = sumPciRec.getId();
					mrCosi = sumPciRec.getAssocDegree();
					if (mrNcell == hoNcell) {
						flag = false;
						sumPciRec.setAssocDegree(mrCosi * samefreqcellcoefweight + hoCosi * switchratioweight);
					}
				}
				if (flag) {
					g4HoPciRec.setAssocDegree(hoCosi * switchratioweight);
					tmpPciRecs.add(g4HoPciRec);
				}
			}
			ncells.addAll(tmpPciRecs);
		} else if (mrNcells.isEmpty() && !hoNcells.isEmpty()) {
			for (Ncell tmpPciRec : hoNcells) {
				tmpPciRec.setAssocDegree(tmpPciRec.getAssocDegree() * switchratioweight);
			}
			ncells.addAll(tmpPciRecs);
		} else if (!mrNcells.isEmpty() && hoNcells.isEmpty()) {
			ncells.addAll(mrNcells);
		}
		// 合并完成，清理MR和HO数据
		cell.setMrNcells(new ArrayList<Ncell>());
		cell.setHoNcells(new ArrayList<Ncell>());

		List<Ncell> willFilteredNcell = new ArrayList<Ncell>();
		double totalAssocDegree = 0.0;
		for (Ncell ncell : ncells) {
			String ncellId = ncell.getId();
			if (!ncellList.contains(ncellId)) {
				ncellList.add(ncellId.intern());
				// 如果列表中不存在，则更新MR中工参
				if (!cellToOriPciFromData.containsKey(ncellId)) {
					cellToOriPciFromData.put(ncellId.intern(), ncell.getPci());
				}
				if (!cellToLonLatFromData.containsKey(ncellId)) {
					cellToLonLatFromData.put(ncellId.intern(), new double[] { ncell.getLon(), ncell.getLat() });
				}
				// 前面已经过滤过，所有邻区都是主小区的同频小区，直接用主小区频点
				if (!cellToEarfcnFromData.containsKey(ncellId)) {
					cellToEarfcnFromData.put(ncellId.intern(), String.valueOf(cell.getCellBcch()).intern());
				}
			}
			double assocDegree = ncell.getAssocDegree();
			if (assocDegree < config.getMincorrelation()) {
				// 暂存关联度小于“最小关联度”的邻区
				willFilteredNcell.add(ncell);
			} else {
				// 总关联度不累加小于“最小关联度”的值
				totalAssocDegree += assocDegree;
			}
		}

		// 过滤关联度小于“最小关联度”的邻区
		for (Ncell ncell : willFilteredNcell) {
			System.out.println("Filter: cellId = " + cellId + ", ncellId = " + ncell.getId() + ", assocDegree="
					+ ncell.getAssocDegree() + ", ncell.getNumerator() = " + ncell.getNumerator()
					+ ", sumTimesTotals = " + sumTimesTotals);
			ncells.remove(ncell);
		}
		cell.setTotalAssocDegree(totalAssocDegree);
		if(cellId.equals("6775831")){
			System.out.println(cell);
			System.out.println(cell.getNcells());
		}
		return true;
	}

	class MrObj {
		// 主小区
		private Cell scell = null;
		private List<Ncell> ncells = null;

		public MrObj(Cell cell) {
			super();
			this.scell = cell;
			this.ncells = scell.getMrNcells();
		}

		public void sumMrData(String indexs[]) {
			mrLineCounter++;

			int ncellId = 0;
			int ncellPci = 0;
			double ncellBcch = 0;
			double ncellLon = 1000.0;
			double ncellLat = 1000.0;

			double dis = -1.0;
			double timesTotal = 0;
			double time1 = 0;
			String meaTime = "";
			double mixingSum = 0;

			// 第一次整理数据据
			if (!indexs[0].equals("")) {
				ncellId = Integer.parseInt(indexs[0]);
			}
			ncellPci = Integer.parseInt(indexs[1]);
			ncellBcch = Double.parseDouble(indexs[2]);
			if (indexs[3] != null && !"".equals(indexs[3])) {
				ncellLon = Double.parseDouble(indexs[3].trim());
			}
			if (indexs[4] != null && !"".equals(indexs[4])) {
				ncellLat = Double.parseDouble(indexs[4].trim());
			}
			timesTotal = Double.parseDouble(indexs[5]);
			time1 = Double.parseDouble(indexs[6]);
			mixingSum = Double.parseDouble(indexs[7]);
			meaTime = indexs[8];
			if (!cellToEarfcnFromData.containsKey(String.valueOf(ncellId))) {
				cellToEarfcnFromData.put(String.valueOf(ncellId).intern(), String.valueOf(ncellBcch).intern());
			}
			scell.putMeaTimeToMixingSum(meaTime, mixingSum);
			// 同频累加
			if (scell.getCellBcch() == ncellBcch) {
				// 获取某邻区的RSRPtimes1 可设置相对数值2(建议值：-3)
				boolean nflag = true;
				for (Ncell n : ncells) {
					// 判断是否包含当前邻区
					if (Integer.parseInt(n.getId()) == ncellId) {
						// 如果包含则增加测量值
						nflag = false;
						n.setTimesTotal(n.getTimesTotal() + timesTotal);
						n.setNumerator(n.getNumerator() + time1);
						break;
					}
				}
				// 如果不包含，则将该小区加入列表
				if (nflag) {
					ncells.add(new Ncell(ncellId + "", ncellBcch, ncellPci, ncellLon, ncellLat, timesTotal, time1, dis));
				}
			}
		}

		public boolean getMrRelaDegree() {
			// 混频 新分母
			double totalMixingSum = scell.getMixingSum();
			if ("1187851".equals(scell.getId()) || "6688092".equals(scell.getId())) {
				System.out.println(scell.getId() + ",totalMixingSum=" + totalMixingSum);
			}
			// 总的测量次数
			double sumTimesTotals = 0;

			// 计算关联度
			for (Ncell ncell : ncells) {
				sumTimesTotals += ncell.getTimesTotal();
				// 2015-8-13 cc修改 用mixingSum为分母
				// mixingSum = ncells.get(i).getMixingSum();
				if (totalMixingSum != 0) {
					ncell.setAssocDegree(ncell.getNumerator() / totalMixingSum);
				} else {
					ncell.setAssocDegree(0.0);
				}
				if ("1187851".equals(scell.getId()) || "6688092".equals(scell.getId())) {
					System.out.println(scell.getId() + ",AssocDegree=" + ncell.getAssocDegree());
				}
			}
			if ("1187851".equals(scell.getId()) || "6688092".equals(scell.getId())) {
				double d = 0.0;
				for (Ncell ncell : ncells) {
					d += ncell.getAssocDegree();
				}
				System.out.println(scell.getId() + ",totalAssocDegree=" + d);
			}
			// 过滤测量总数小于最小测量总数的
			if (sumTimesTotals < config.getMinmeasuresum()) {
				System.out.println("Filter: cellId = " + scell.getId() + ", sumTimesTotals=" + sumTimesTotals);
				return false;
			}
			return true;
		}
	}

	class HoObj {
		private Cell scell = null;
		private List<Ncell> ncells = null;

		public HoObj(Cell cell) {
			super();
			this.scell = cell;
			this.ncells = scell.getHoNcells();
		}

		public void sumHoData(String indexs[]) {
			hoLineCounter++;

			int ncellId = 0;
			int ncellPci = 0;
			double ncellBcch = 0;
			double ncellLon = 1000.0;
			double ncellLat = 1000.0;

			double dis = -1.0;
			double timesTotal = 0;
			double time1 = 0;
			// String meaTime = "";
			// 第一次整理数据据

			if (!"".equals(indexs[0])) {
				ncellId = Integer.parseInt(indexs[0]);
			}
			ncellPci = Integer.parseInt(indexs[1]);
			ncellBcch = Double.parseDouble(indexs[2]);
			if (indexs[3] != null && !"".equals(indexs[3])) {
				ncellLon = Double.parseDouble(indexs[3].trim());
			}
			if (indexs[4] != null && !"".equals(indexs[4])) {
				ncellLat = Double.parseDouble(indexs[4].trim());
			}

			time1 = Double.parseDouble(indexs[5]);
			// meaTime = indexs[6];

			// 更新频点
			if (!cellToEarfcnFromData.containsKey(String.valueOf(ncellId))) {
				cellToEarfcnFromData.put(String.valueOf(ncellId).intern(), String.valueOf(ncellBcch).intern());
			}
			// 同频累加
			if (scell.getCellBcch() == ncellBcch) {
				// 获取某邻区的RSRPtimes1 可设置相对数值2(建议值：-3)
				boolean nflag = true;
				for (Ncell n : ncells) {
					if (Integer.parseInt(n.getId()) == ncellId) {
						nflag = false;
						n.setNumerator(n.getNumerator() + time1);
						break;
					}
				}
				if (nflag) {
					ncells.add(new Ncell(ncellId + "", ncellBcch, ncellPci, ncellLon, ncellLat, timesTotal, time1, dis));
				}
			}
		}

		public boolean getHoRelaDegree() {
			double timesTotal = 0;

			for (Ncell n : ncells) {
				timesTotal += n.getNumerator();
			}
			for (Ncell ncell : ncells) {
				if (timesTotal != 0) {
					ncell.setAssocDegree(ncell.getNumerator() / timesTotal);
				} else {
					ncell.setAssocDegree(0.0);
				}
			}
			return true;
		}
	}
}