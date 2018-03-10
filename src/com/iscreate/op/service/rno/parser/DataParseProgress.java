package com.iscreate.op.service.rno.parser;

public enum DataParseProgress {

	GetFile("获取文件"),
	Decompress("解压"),
	Prepare("准备数据库条件"),
	Decode("文件解析"),
	Report("文件处理总结"),
	SaveToDb("数据入库");

	String desc;
	
	private DataParseProgress(String desc){
		this.desc=desc;
	}
	
	@Override
	public String toString() {
		return this.desc;
	}
}
