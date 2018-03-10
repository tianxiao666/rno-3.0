package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapper;
import org.apache.hadoop.hbase.mapreduce.TableSplit;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;

public class NewPciMapper extends TableMapper<Text, Text> {

	//对hbase表中可能用的column初始化
	final static byte[] MRINFO = Bytes.toBytes("MRINFO");
	final static byte[] HOINFO = Bytes.toBytes("HOINFO");
	final static byte[] SFINFO = Bytes.toBytes("SFINFO");
	final static byte[] CELL_ID = Bytes.toBytes("CELL_ID");
	final static byte[] NCELL_ID = Bytes.toBytes("NCELL_ID");
	final static byte[] DISTANCE = Bytes.toBytes("DISTANCE");
	final static byte[] CELL_BCCH = Bytes.toBytes("CELL_BCCH");
	final static byte[] NCELL_BCCH = Bytes.toBytes("NCELL_BCCH");
	final static byte[] CELL_PCI = Bytes.toBytes("CELL_PCI");
	final static byte[] NCELL_PCI = Bytes.toBytes("NCELL_PCI");
	final static byte[] TIMESTOTAL = Bytes.toBytes("TIMESTOTAL");
	final static byte[] RSRPTIMES0 = Bytes.toBytes("RSRPTIMES0");
	final static byte[] RSRPTIMES1 = Bytes.toBytes("RSRPTIMES1");
	final static byte[] MIXINGSUM = Bytes.toBytes("MIXINGSUM");
	final static byte[] MEA_TIME = Bytes.toBytes("MEA_TIME");
	final static byte[] ENODEBID = Bytes.toBytes("ENODEBID");
	final static byte[] EUTRANCELLRELATION = Bytes.toBytes("EUTRANCELLRELATION");
	final static byte[] BUS_CELL_ID = Bytes.toBytes("BUS_CELL_ID");
	final static byte[] NCELL = Bytes.toBytes("NCELL");
	final static byte[] CELLID = Bytes.toBytes("CELLID");
	final static byte[] PMHOEXEATTLTEINTRAF = Bytes.toBytes("PMHOEXEATTLTEINTRAF");
	final static byte[] LONGITUDE = Bytes.toBytes("LONGITUDE");
	final static byte[] LATITUDE = Bytes.toBytes("LATITUDE");
	final static byte[] EARFCN = Bytes.toBytes("EARFCN");
	final static byte[] PCI = Bytes.toBytes("PCI");
	final static byte[] R0_RP = Bytes.toBytes("R0_RP");

	private enum DataType {
		MR, HO, SF
	}

	private Configuration conf = null;

	private String tableName = "";

	private DataType dataType = null;

	private NavigableMap<byte[], byte[]> navi = null;

	private StringBuffer sbKey = new StringBuffer();

	private StringBuffer sbValue = new StringBuffer();
	
	private Map<String,ObjList> objs = new HashMap<String, ObjList>();
	
	private Obj oriObj = null;
	
	private ObjCell objCell = null;
	
	private Obj obj = null;

	/** 基站标识 **/
	private String enodebId = "";

	/** 小区标识 **/
	private String cellId = "";

	/** 邻区标识 **/
	private String ncellId = "";

	/** 行统计 **/
	private long counter = 0;

	/** MR行统计 **/
	private long mrCounter = 0;

	/** HO行统计 **/
	private long hoCounter = 0;

	/** RS行统计 **/
	private long rsCounter = 0;

	/** 过滤后的同频行数统计 **/
	private long sameFreqCounter = 0;

	/** 过滤后的同频MR行数统计 **/
	private long sameFreqMrCounter = 0;

	/** 过滤后的同频HO行数统计 **/
	private long sameFreqHoCounter = 0;

	/** 过滤后的同频SF行数统计 **/
	private long sameFreqSfCounter = 0;

	// 运行开始时间
	private long startTimeMillis = 0;

	/** 距离限制 **/
	private double disLimit = 0;

	/** 邻区字段 **/
	String[] ncellArr = new String[5];

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// 获取配置信息
		conf = context.getConfiguration();

		if (conf != null) {
			if (conf.get("dislimit") == null || "".equals(conf.get("dislimit"))) {
				disLimit = 5000;
			} else {
				disLimit = Double.parseDouble(conf.get("dislimit"));
			}
		}
		System.out.println("PCIMapper 执行setup中..");
		super.setup(context);
		System.out.println("map counter = " + counter);
		startTimeMillis = System.currentTimeMillis();
		tableName = new String(((TableSplit) context.getInputSplit()).getTableName());
		if (tableName.endsWith("MR")) {
			dataType = DataType.MR;
		} else if (tableName.endsWith("HO")) {
			dataType = DataType.HO;
		} else if (tableName.endsWith("SF")) {
			dataType = DataType.SF;
		}
		System.out.println("tableName=" + tableName);
	}

	protected void map(ImmutableBytesWritable row, Result value, Context context) throws IOException,
			InterruptedException {
		if (++counter % 100000 == 0) {
			System.out.println("map counter = " + counter);
		}
		// 清空
		sbKey.setLength(0);
		sbValue.setLength(0);

		switch (dataType) {
		case MR:
			//handleMr(value, context);
			if (++mrCounter % 100000 == 0) {
				System.out.println("map mr counter = " + mrCounter);
			}
			// MR 数据（中兴或爱立信的）
			navi = value.getFamilyMap(MRINFO);
			cellId = Bytes.toString(navi.get(CELL_ID));
			ncellId = Bytes.toString(navi.get(NCELL_ID));
			// 过滤非同频数据
			if (null != cellId && !"".equals(cellId) && !"".equals(ncellId) && !cellId.equals(ncellId)
					&& Double.parseDouble(Bytes.toString(navi.get(DISTANCE))) * 1000 < disLimit) {
				// 拼装key
				sbKey.append(cellId).append(",");
				sbKey.append(Bytes.toString(navi.get(CELL_PCI))).append(",");
				sbKey.append(Bytes.toString(navi.get(CELL_BCCH)));
				// 拼装value
				sbValue.append(ncellId).append(",");
				sbValue.append(Bytes.toString(navi.get(NCELL_PCI))).append(",");
				sbValue.append(Bytes.toString(navi.get(NCELL_BCCH))).append(",");
				sbValue.append(Bytes.toString(navi.get(TIMESTOTAL))).append(",");
				sbValue.append(Bytes.toString(navi.get(RSRPTIMES0))).append(",");
				sbValue.append(Bytes.toString(navi.get(RSRPTIMES1))).append(",");
				sbValue.append(Bytes.toString(navi.get(MIXINGSUM))).append(",");
				sbValue.append(Bytes.toString(navi.get(MEA_TIME))).append(",MR");

				sameFreqCounter++;
				sameFreqMrCounter++;
				context.write(new Text(sbKey.toString()), new Text(sbValue.toString()));
			}
			break;
		case HO:
			//handleHo(value, context);
			if (++hoCounter % 100000 == 0) {
				System.out.println("map ho counter = " + hoCounter);
			}
			navi = value.getFamilyMap(HOINFO);
			enodebId = Bytes.toString(navi.get(ENODEBID));
			// 针对不同的厂家处理
			if (("RNO_4G_ERI_HO").equals(tableName)) {
				// 邻区ID
				ncellArr = Bytes.toString(navi.get(EUTRANCELLRELATION)).split("-");
				ncellId = ncellArr[1] + ncellArr[2];
				// 小区ID
				cellId = Bytes.toString(navi.get(BUS_CELL_ID));
			} else if (("RNO_4G_ZTE_HO").equals(tableName)) {
				// 邻区ID
				ncellArr = Bytes.toString(navi.get(NCELL)).split(":");
				ncellId = ncellArr[3] + ncellArr[4];
				// 小区ID
				cellId = enodebId + Bytes.toString(navi.get(CELLID));
			} else {
				return;
			}

			if (!"".equals(cellId) && !"".equals(ncellId)) {

				// 拼装key
				sbKey.append(cellId).append(",");
				sbKey.append(Bytes.toString(navi.get(CELL_PCI))).append(",");
				sbKey.append(Bytes.toString(navi.get(CELL_BCCH)));
				// 拼装value
				sbValue.append(ncellId).append(",");
				sbValue.append(Bytes.toString(navi.get(NCELL_PCI))).append(",");
				sbValue.append(Bytes.toString(navi.get(NCELL_BCCH))).append(",");
				sbValue.append(Bytes.toString(navi.get(PMHOEXEATTLTEINTRAF))).append(",");
				sbValue.append(Bytes.toString(navi.get(MEA_TIME))).append(",HO");

				sameFreqCounter++;
				sameFreqHoCounter++;
				context.write(new Text(sbKey.toString()), new Text(sbValue.toString()));
			}
			break;
		case SF:
			//handleSf(value, context);
			if (++rsCounter % 100000 == 0) {
				System.out.println("map rs counter = " + rsCounter);
			}
			navi = value.getFamilyMap(SFINFO);
			cellId = Bytes.toString(navi.get(CELL_ID));
			objCell = new ObjCell(Long.parseLong(cellId), Integer.parseInt(Bytes.toString(navi.get(PCI))),
					Integer.parseInt(Bytes.toString(navi.get(EARFCN))), Double.parseDouble(Bytes.toString(navi
							.get(R0_RP))));
			obj = new Obj(Bytes.toString(navi.get(MEA_TIME)), Double.parseDouble(Bytes.toString(navi.get(LONGITUDE))),
					Double.parseDouble(Bytes.toString(navi.get(LATITUDE))));
			if (oriObj != null) {
				if (!obj.equals(oriObj)) {
					//一个obj结束，结算AB小区，并保存
					oriObj.pickABcell();
					ncellId = oriObj.getCellId()+"";
					if(objs.containsKey(ncellId)){
						objs.get(ncellId).add(oriObj);
					}else {
						ObjList objList = new ObjList(Long.parseLong(ncellId));
						objList.add(oriObj);
						objs.put(ncellId, objList);
					}
					//进入下一个obj
					oriObj = obj;
				}
			}else {
				oriObj = obj;
			}
			oriObj.put(Long.parseLong(cellId), objCell);
			break;
		default:
			break;
		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		switch (dataType) {
		case SF:
			// writeSf(context);
			int i = 0;
			System.out.println("objs size = " + objs.size());
			for (ObjList objList : objs.values()) {
				i += objList.size();
				System.out.println("objList cell = " + objList.cellId + ",objList size = " + objList.size());
				objList.bulidWriteMap();
				for (String aStr : objList.getWriteMap().keySet()) {
					for (String bStr : objList.getWriteMap().get(aStr)) {
						sameFreqCounter++;
						sameFreqSfCounter++;
						context.write(new Text(aStr), new Text(bStr));
					}
				}
			}
			System.out.println("total objs size = " + i);
			break;
		default:
			break;
		}
		super.cleanup(context);
		System.out.println("map counter = " + counter);
		System.out.println("map mr counter = " + mrCounter);
		System.out.println("map ho counter = " + hoCounter);
		System.out.println("same freq total = " + sameFreqCounter);
		System.out.println("same freq mr total = " + sameFreqMrCounter);
		System.out.println("same freq ho total = " + sameFreqHoCounter);
		System.out.println("same freq sf total = " + sameFreqSfCounter);
		System.out.println("map spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	class ObjList {
		long cellId = 0;
		List<Obj> list = new ArrayList<Obj>();
		Map<String, List<String>> writeMap = new HashMap<String, List<String>>();

		public ObjList(long cellId) {
			super();
			this.cellId = cellId;
		}

		public void add(Obj obj) {
			list.add(obj);
		}

		public int size() {
			return list.size();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (cellId ^ (cellId >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ObjList)) {
				return false;
			}
			ObjList other = (ObjList) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (cellId != other.cellId) {
				return false;
			}
			return true;
		}

		private NewPciMapper getOuterType() {
			return NewPciMapper.this;
		}

		public Map<String, List<String>> getWriteMap() {
			return writeMap;
		}

		public void bulidWriteMap() {
			long totalCnt = list.size();
			String cellStr = list.get(0).getACellStr();
			List<String> bList = new ArrayList<String>();
			Map<Long, long[]> bCnt = new HashMap<Long, long[]>();
			for (Obj obj : list) {
				if (obj.hasRsrp0()) {
					if (obj.hasRsrp1()) {
						if (bCnt.containsKey(obj.getNcellId())) {
							bCnt.put(obj.getNcellId(),
									new long[] { (bCnt.get(obj.getNcellId())[0] + 1),
											(bCnt.get(obj.getNcellId())[1] + 1) });
						} else {
							bCnt.put(obj.getNcellId(), new long[] { 1, 1 });
						}
					} else {
						if (bCnt.containsKey(obj.getNcellId6())) {
							bCnt.put(obj.getNcellId6(),
									new long[] { (bCnt.get(obj.getNcellId6())[0] + 1),
											(bCnt.get(obj.getNcellId6())[1] + 0) });
						} else {
							bCnt.put(obj.getNcellId6(), new long[] { 1, 0 });
						}
					}
				}
			}
			List<Long> l = new ArrayList<Long>();
			for (Obj obj : list) {
				if (obj.hasRsrp1()) {
					if (!l.contains(obj.getNcellId6())) {
						bList.add(obj.getBCellStr(bCnt.get(obj.getNcellId6())[0], bCnt.get(obj.getNcellId6())[1],
								totalCnt));
						l.add(obj.getNcellId6());
					}
				}
			}
			writeMap.put(cellStr, bList);
		}
	}

	class Obj {
		String meaTime = "";
		double lng = -1.0;
		double lat = -1.0;
		long cellId = 0;
		long ncellId = 0;
		long ncellId6 = 0;
		List<ObjCell> objCells = new ArrayList<ObjCell>();

		public void pickABcell() {
			double maxVal = -Double.MAX_VALUE;
			long maxCell = 0;
			for (ObjCell objCell : objCells) {
				if (objCell.getR0_RP() > maxVal) {
					maxVal = objCell.getR0_RP();
					maxCell = objCell.getCellId();
				}
			}
			double secVal = -Double.MAX_VALUE;
			long secCell = 0;
			for (ObjCell objCell : objCells) {
				if (objCell.getCellId() != maxCell && objCell.getR0_RP() > secVal) {
					secVal = objCell.getR0_RP();
					secCell = objCell.getCellId();
				}
			}
			cellId = maxCell;
			if (maxVal - secVal <= 6) {
				ncellId6 = secCell;
				if (maxVal - secVal <= 3) {
					ncellId = secCell;
				}
			}
			// 清空多余小区
			for (ObjCell objCell : new ArrayList<ObjCell>(objCells)) {
				if (objCell.getCellId() != maxCell && objCell.getCellId() != secCell) {
					objCells.remove(objCell);
				}
			}
		}

		public boolean hasRsrp0() {
			if (ncellId6 > 0) {
				return true;
			}
			return false;
		}

		public boolean hasRsrp1() {
			if (ncellId > 0) {
				return true;
			}
			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			long temp;
			temp = Double.doubleToLongBits(lat);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			temp = Double.doubleToLongBits(lng);
			result = prime * result + (int) (temp ^ (temp >>> 32));
			result = prime * result + ((meaTime == null) ? 0 : meaTime.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Obj)) {
				return false;
			}
			Obj other = (Obj) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat)) {
				return false;
			}
			if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng)) {
				return false;
			}
			if (meaTime == null) {
				if (other.meaTime != null) {
					return false;
				}
			} else if (!meaTime.equals(other.meaTime)) {
				return false;
			}
			return true;
		}

		private NewPciMapper getOuterType() {
			return NewPciMapper.this;
		}

		public Obj(String meaTime, double lng, double lat) {
			super();
			this.meaTime = meaTime;
			this.lng = lng;
			this.lat = lat;
		}

		public long getCellId() {
			return cellId;
		}

		public void setCellId(long cellId) {
			this.cellId = cellId;
		}

		public long getNcellId() {
			return ncellId;
		}

		public long getNcellId6() {
			return ncellId6;
		}

		public void setNcellId(long ncellId) {
			this.ncellId = ncellId;
		}

		public String getMeaTime() {
			return meaTime;
		}

		public void put(long cellId, ObjCell objCell) {
			if (objCells.contains(objCell)) {
				for (ObjCell cell : objCells) {
					if (cell.equals(objCell)) {
						if (cell.getR0_RP() < objCell.getR0_RP()) {
							cell = objCell;
						}
					}
				}
			} else {
				objCells.add(objCell);
			}
		}

		public String getACellStr() {
			for (ObjCell cell : objCells) {
				if (cell.getCellId() == cellId) {
					sbKey.setLength(0);
					sbKey.append(cell.getCellId()).append(",");
					sbKey.append(cell.getPci()).append(",");
					sbKey.append(cell.getEarfcn());
					return sbKey.toString();
				}
			}
			return "";
		}

		public String getBCellStr(long rsrp0, long rsrp1, long cnt) {
			if (hasRsrp0()) {
				for (ObjCell cell : objCells) {
					if (cell.getCellId() == ncellId) {
						sbValue.setLength(0);
						sbValue.append(cell.getCellId()).append(",");
						sbValue.append(cell.getPci()).append(",");
						sbValue.append(cell.getEarfcn()).append(",");
						sbValue.append(rsrp0).append(",");
						sbValue.append(rsrp1).append(",");
						sbValue.append(cnt).append(",");
						sbValue.append(meaTime).append(",SF");
						return sbValue.toString();
					}
				}
			}
			return "";
		}
	}

	class ObjCell {
		long cellId = 0;
		int pci = -1;
		int earfcn = 0;
		double r0_RP = -1.0;
		public ObjCell(long cellId, int pci, int earfcn, double r0_RP) {
			super();
			this.cellId = cellId;
			this.pci = pci;
			this.earfcn = earfcn;
			this.r0_RP = r0_RP;
		}
		public long getCellId() {
			return cellId;
		}
		public int getPci() {
			return pci;
		}
		public int getEarfcn() {
			return earfcn;
		}
		public double getR0_RP() {
			return r0_RP;
		}
		@Override
		public String toString() {
			return "ObjCell [enodebId=" + enodebId + ", cellId=" + cellId + ", pci=" + pci + ", earfcn=" + earfcn
					+ ", r0_RP=" + r0_RP + "]";
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + (int) (cellId ^ (cellId >>> 32));
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof ObjCell)) {
				return false;
			}
			ObjCell other = (ObjCell) obj;
			if (!getOuterType().equals(other.getOuterType())) {
				return false;
			}
			if (cellId != other.cellId) {
				return false;
			}
			return true;
		}
		private NewPciMapper getOuterType() {
			return NewPciMapper.this;
		}
	}
}