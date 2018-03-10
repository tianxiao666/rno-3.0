package com.iscreate.op.service.rno.mapreduce.matrix;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.service.rno.mapreduce.model.Cell;
import com.iscreate.op.service.rno.mapreduce.model.Ncell;

public class G4PciMatrixReducer extends Reducer<Text, Text, Text, Text> {

	private static Logger log = LoggerFactory.getLogger(G4PciMatrixReducer.class);

	Configuration conf = null;

	/** 所有主小区 **/
	List<String> cellList = new ArrayList<String>();
	/** 保存计算结果 **/
	List<Cell> cellObjList = new ArrayList<Cell>();

	double samefreqcellcoefweight = 0.8; // 权值
	double switchratioweight = 0.2; // 切换比例权值

	// 同站小区判断条件变更由 enodeb->enodeb+earfcn
	/** map格式的同站邻区信息 **/
	Map<String, String> enodebToCells = null;

	/** 文件路径 **/
	String pciOriPath = "";

	/** reduce 处理次数统计 **/
	long counter = 0;

	/** MR 处理行统计 **/
	long mrLineCounter = 0;

	/** HO 处理行统计 **/
	long hoLineCounter = 0;

	/** 处理行统计 **/
	long lineCounter = 0;

	/** 运行时间 **/
	private long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// 获取配置信息
		conf = context.getConfiguration();
		if (conf != null) {
			String arr[] = conf.getStrings("enodebToCells");
			if (arr != null && arr.length != 0) {
				enodebToCells = string2Map(arr[0]);
			}
			pciOriPath = conf.get("pciOriPath");
			if (conf.get("samefreqcellcoefweight") != null && conf.get("samefreqcellcoefweight") != null) {
				// 获取权值
				samefreqcellcoefweight = Double.parseDouble(conf.get("samefreqcellcoefweight"));
				switchratioweight = Double.parseDouble(conf.get("switchratioweight"));
			}
		}
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();

		log.info("reduce counter = " + counter);
	}

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		if (++counter % 1000 == 0) {
			log.info("reduce counter = " + counter);
		}
		String[] keyArr = key.toString().split(",");
		String cellId = keyArr[0];
		String enodebId = keyArr[1];
		int pci = Integer.parseInt(keyArr[2]);
		String cellBcch = keyArr[3];
		double cellLon = Double.parseDouble(keyArr[4]);
		double cellLat = Double.parseDouble(keyArr[5]);

		// 如果有重复的 cellId，则跳过不处理，一般是不会有这种情况的。
		if (cellList.contains(cellId)) {
			return;
		} else {
			cellList.add(cellId.intern());
		}
		Cell cell = new Cell(cellId);

		cell.setPci(pci);
		cell.setCellBcch(Double.parseDouble(cellBcch));
		cell.setLon(cellLon);
		cell.setLat(cellLat);

		// 处理同站其他小区
		String strSameStationCells = enodebToCells.get(enodebId + "_" + cellBcch);
		if (strSameStationCells != null && strSameStationCells.length() != 0) {
			// 1868801,1868802,1868803
			// 把主小区ID从同站小区列表中删除，剩下的就是同站其他小区
			String[] sameStationCells = strSameStationCells.split(",");
			for (String ssCellId : sameStationCells) {
				ssCellId = ssCellId.trim();
				if (!ssCellId.equals(cellId)) {
					cell.getSameStationOtherCells().add(ssCellId.intern());
				}
			}
		}
		// 合并小区 MR 数据，如果返回 false，则过滤掉该小区
		if (!mergeMrData(cell, values)) {
			cell = null;
			return;
		}
		cellObjList.add(cell);
	}

	@Override
	protected void cleanup(Context context) throws InterruptedException, IOException {
		System.out.println("sumRelaDegre 最终大小:" + cellObjList.size());
		System.out.println("pciOriPath 文件路径是:" + pciOriPath);

		log.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
		log.info("reduce counter total = " + counter);
		log.info("reduce line counter total = " + lineCounter);
		log.info("reduce mr line counter total = " + mrLineCounter);
		log.info("reduce ho line counter total = " + hoLineCounter);

		save4GInterferMatrixInHdfs(pciOriPath, cellObjList);
	}

	/**
	 * 处理一个小区的数据
	 * 
	 * @param cell
	 * @param values
	 * @return
	 */
	public boolean mergeMrData(Cell cell, Iterable<Text> values) {
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
			return false;
		}
		/*
		 * // 计算mr关联度
		 * mrObj.getMrRelaDegree();
		 * // 计算ho关联度
		 * hoObj.getHoRelaDegree();
		 */
		List<Ncell> mrNcells = cell.getMrNcells(), hoNcells = cell.getHoNcells();
		String mrNcell = "", hoNcell = "";

		double mrCosi = 0, hoCosi = 0;

		if (mrNcells.isEmpty() && hoNcells.isEmpty()) {
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
			double timesTotal = 0.0;
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

			if (indexs[8] != null && !"".equals(indexs[8])) {
				dis = Double.parseDouble(indexs[8].trim());
			}
			// timesTotal = Long.parseLong(indexs[5]);
			meaTime = indexs[5];
			mixingSum = Double.parseDouble(indexs[7]);
			time1 = Double.parseDouble(indexs[6]);

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

			// 计算关联度
			for (Ncell ncell : ncells) {
				// 2015-8-13 cc修改 用mixingSum为分母
				// mixingSum = ncells.get(i).getMixingSum();
				if (totalMixingSum != 0) {
					ncell.setAssocDegree(ncell.getNumerator() / totalMixingSum);
				} else {
					ncell.setAssocDegree(0.0);
				}
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

			double timesTotal = 0.0;
			double time1 = 0;
			double dis = -1.0;
			// String meaTime = "";

			// ncellId + "," + enodebId + "," + cellPci + "," + ncellPci + "," + cellBcch + "," + ncellBcch + "," +
			// cellLon + "," + cellLat + "," + ncellLon + "," + ncellLat + "," + meaTime + "," + time1 + "," + cityId +
			// ",HO";
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

			if (indexs[7] != null && !"".equals(indexs[7])) {
				dis = Double.parseDouble(indexs[7].trim());
			}
			time1 = Double.parseDouble(indexs[6]);
			// meaTime = indexs[5];

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

	public void save4GInterferMatrixInHdfs(String filePath, List<Cell> cellObjList) {
		String realFilePath = filePath;

		FileSystem fs = null;
		try {
			// Configuration conf = new YarnConfiguration();
			fs = FileSystem.get(conf);
		} catch (IOException e1) {
			System.err.println("save4GInterferMatrixInHdfs过程：打开hdfs文件系统出错！");
			e1.printStackTrace();
		}
		// 先删除原有文件
		Path oldFilePath = new Path(URI.create(realFilePath));
		try {
			if (fs.exists(oldFilePath)) {
				fs.delete(oldFilePath, false);
			}
		} catch (IOException e2) {
			System.err.println("save4GInterferMatrixInHdfs过程：保存文件时，删除原有文件出错！");
			e2.printStackTrace();
		}
		// 创建新文件
		Path filePathObj = new Path(URI.create(realFilePath));
		// 创建流
		OutputStream dataOs = null;
		try {
			dataOs = fs.create(filePathObj, true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		BufferedOutputStream bo = null;
		// String line="";
		int cellId = 0;
		int ncellId = 0;
		double cosi = 0;
		int cellPci = 0;
		int ncellPci = 0;
		double cellLon = 1000.0;
		double cellLat = 1000.0;
		double ncellLon = 1000.0;
		double ncellLat = 1000.0;
		double dis = -1;
		StringBuffer cells;
		double cellBcch = 0;
		double ncellBcch = 0;

		List<Ncell> ncells = null;

		int num = 0;
		int nnum = 0;
		try {
			bo = new BufferedOutputStream(dataOs);
			if (!cellObjList.isEmpty()) {
				for (Cell cell : cellObjList) {
					num++;
					System.out.println();
					cellId = Integer.parseInt(cell.getId());
					cellBcch = cell.getCellBcch();
					cellPci = cell.getPci();
					cellLon = cell.getLon();
					cellLat = cell.getLat();
					cells = new StringBuffer();
					cells.append(cellId);
					for (String ssCell : cell.getSameStationOtherCells()) {
						cells.append("_").append(ssCell);
					}
					ncells = cell.getNcells();

					for (Ncell ncell : ncells) {
						nnum++;
						ncellId = Integer.parseInt(ncell.getId());
						cosi = ncell.getAssocDegree();
						ncellBcch = ncell.getCellBcch();
						ncellPci = ncell.getPci();
						ncellLon = ncell.getLon();
						ncellLat = ncell.getLat();
						dis = ncell.getDis();

						bo.write(Bytes.toBytes(String.valueOf(cellId)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(ncellId)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cosi)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cellBcch)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cellPci)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(ncellBcch)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(ncellPci)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cellLon)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cellLat)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(ncellLon)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(ncellLat)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(dis)));
						bo.write(Bytes.toBytes("#"));
						bo.write(Bytes.toBytes(String.valueOf(cells.toString())));
						bo.write(Bytes.toBytes("\n"));
					}
				}
			} else {
				bo.write(Bytes.toBytes("结果为空"));
			}
			System.out.println("一共处理了" + num + "个小区");
			System.out.println("一共处理了" + nnum + "个邻区");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bo.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Map<String, String> string2Map(String enodebs) {
		Map<String, String> enodebToCells = new HashMap<String, String>();
		String keyarr[] = null;
		String valarr[] = null;
		keyarr = enodebs.split("\\|");
		for (int i = 0; i < keyarr.length; i++) {
			valarr = keyarr[i].split("=");
			enodebToCells.put(valarr[0], valarr[1].replace("#", ","));
		}
		return enodebToCells;
	}
}