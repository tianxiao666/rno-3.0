package com.iscreate.op.service.rno.mapreduce.matrix;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.service.rno.mapreduce.model.NewCell;
import com.iscreate.op.service.rno.mapreduce.model.NewNcell;
import com.iscreate.op.service.rno.mapreduce.pci.NewPciConfig;
import com.iscreate.op.service.rno.mapreduce.util.PciUtil;

public class NewPciMatrixReducer extends Reducer<Text, Text, NullWritable, Text> {

	private static Logger log = LoggerFactory.getLogger(NewPciMatrixReducer.class);

	// 配置
	private NewPciConfig config = null;

	private PciUtil util = null;

	private StringBuilder sb = new StringBuilder();

	private NullWritable nullKey = NullWritable.get();

	private Text val = new Text();

	/** 运行时间 **/
	private long startTimeMillis = 0;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		// 获取配置信息
		config = new NewPciConfig(context);

		util = new PciUtil(config, startTimeMillis);

		log.info("reduce counter = " + util.getCounter());
	}

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		NewCell cell = util.handleReducerData(key, values);
		if (cell == null) {
			return;
		}

//		for (NewNcell ncell : cell.getNcells()) {
//			sb.setLength(0);
//			sb.append(cell.getId()).append("#");
//			sb.append(ncell.getId()).append("#");
//			sb.append(ncell.getAssocDegree()).append("#");
//			sb.append(ncell.getMrAssocDegree()).append("#");
//			sb.append(ncell.getHoAssocDegree()).append("#");
//			sb.append(ncell.getSfAssocDegree()).append("#");
//			sb.append(cell.getCellBcch()).append("#");
//			sb.append(cell.getPci()).append("#");
//			sb.append(ncell.getCellBcch()).append("#");
//			sb.append(ncell.getPci());
//			val.set(sb.toString());
//			context.write(nullKey, val);
//		}
		for (NewNcell ncell : cell.getNcells()) {
			sb.setLength(0);
			sb.append(cell.getId()).append("#");
			sb.append(ncell.getId()).append("#");
			sb.append(ncell.getAssocDegree()).append("#");
			sb.append(ncell.getMrAssocDegree()).append("#");
			if (null != ncell.getMrData()) {
				sb.append(ncell.getMrData().getTimes0()).append("#");
				sb.append(ncell.getMrData().getTimes1()).append("#");
			} else {
				sb.append(0).append("#");
				sb.append(0).append("#");
			}
			sb.append(cell.getMixingSum()).append("#");
			sb.append(ncell.getHoAssocDegree()).append("#");
			sb.append(ncell.getSfAssocDegree()).append("#");
			if (null != ncell.getRsData()) {
				sb.append(ncell.getRsData().getTimes0()).append("#");
				sb.append(ncell.getRsData().getTimes1()).append("#");
			} else {
				sb.append(0).append("#");
				sb.append(0).append("#");
			}
			sb.append(cell.getCellBcch()).append("#");
			sb.append(cell.getPci()).append("#");
			sb.append(ncell.getCellBcch()).append("#");
			sb.append(ncell.getPci());
			val.set(sb.toString());
			context.write(nullKey, val);
		}
	}

	@Override
	protected void cleanup(Context context) throws InterruptedException, IOException {
		log.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
		log.info("reduce counter total = " + util.getCounter());
		log.info("reduce line counter total = " + util.getLineCounter());
		log.info("reduce mr line counter total = " + util.getMrLineCounter());
		log.info("reduce ho line counter total = " + util.getHoLineCounter());

		super.cleanup(context);
	}
}