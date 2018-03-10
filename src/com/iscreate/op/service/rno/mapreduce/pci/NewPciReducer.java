package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iscreate.op.service.rno.mapreduce.model.NewCell;
import com.iscreate.op.service.rno.mapreduce.model.NewCellWritable;
import com.iscreate.op.service.rno.mapreduce.util.PciUtil;

public class NewPciReducer extends Reducer<Text, Text, NullWritable, Text> {

	private static Logger logger = LoggerFactory.getLogger(NewPciReducer.class);
	
	private final static NullWritable nullKey = NullWritable.get();
	// 配置
	private NewPciConfig config = null;
	
	private PciUtil util = null;

	private NewCellWritable cw = new NewCellWritable();
	
	/** 输出的value **/
	private Text outputVal = new Text();

	// 运行开始时间
	private long startTimeMillis = 0;

	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
		startTimeMillis = System.currentTimeMillis();
		// 初始化配置信息
		config = new NewPciConfig(context);

		util = new PciUtil(config, startTimeMillis);

		logger.info("reduce counter = " + util.getCounter());
		logger.info("config = " + config.toString());
		logger.info("setup finished. Spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}

	/**
	 * key 为 cellId，如 1868801
	 * values 格式为 ncellId,timestotal,time1,enodebId,cellPci,ncellPci,cellBcch,ncellBcch 的集合
	 * 如：1868803,120,53,186880,462,335,38100,38100
	 */
	@Override
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		NewCell cell = util.handleReducerData(key, values);
		if (cell == null) {
			return;
		}
		cw.set(cell);
		outputVal.set(cw.toString());
		context.write(nullKey, outputVal);
	}

	@Override
	public void cleanup(Context context) {
		logger.info("reduce spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
		logger.info("reduce counter total = " + util.getCounter());
		logger.info("reduce line counter total = " + util.getLineCounter());
		logger.info("reduce mr line counter total = " + util.getMrLineCounter());
		logger.info("reduce ho line counter total = " + util.getHoLineCounter());
		logger.info("reduce sf line counter total = " + util.getSfLineCounter());

		startTimeMillis = System.currentTimeMillis();
		config.updateOwnParam();
		config.savePciDataListToHdfs();
		logger.info("cleanup finished. Spent seconds = " + ((System.currentTimeMillis() - startTimeMillis) / 1000.0));
	}
}