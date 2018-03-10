package com.iscreate.op.service.rno.tool;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.iscreate.plat.tools.xmlhelper.XmlService;
import com.iscreate.plat.tools.xmlhelper.XmlServiceImpl;
/**
 * 
 * @author chao_xj
 * 为hbase表加装namespace类
 */
public class HBTable {

	private static Logger logger = Logger.getLogger(HBTable.class);

	/**
	 * 
	 * @title 返回带namespace的表信息
	 * @param tab
	 * @return
	 * @author chao.xj
	 * @date 2016年4月21日上午10:34:21
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public static String valueOf(String tab) {
		// TODO Auto-generated constructor stub
		XmlService xmlService = new XmlServiceImpl();
		String env = "";
		try {
			env = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/Enviroment");
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			logger.debug("当前hbase用户为="+env);
		}
		if("".equals(env) || env==null){
			env = "default";
		}
		return env.toLowerCase()+":"+tab;
	}
}
