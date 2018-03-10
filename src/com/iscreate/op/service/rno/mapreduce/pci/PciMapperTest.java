package com.iscreate.op.service.rno.mapreduce.pci;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class PciMapperTest extends Mapper<Object, Text, Text, Text> {
	@Override
	public void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);
	}

	protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
		System.out.println(key.toString());
		context.write(new Text(key.toString()), value);
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);
	}
}
