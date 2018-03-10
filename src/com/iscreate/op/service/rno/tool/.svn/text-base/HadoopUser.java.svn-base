package com.iscreate.op.service.rno.tool;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.iscreate.plat.tools.xmlhelper.XmlService;
import com.iscreate.plat.tools.xmlhelper.XmlServiceImpl;
/**
 * 
 * @author chao_xj
 * 为hadoop环境装配各类用户之家类
 */
public class HadoopUser {
	
	private static Logger logger = Logger.getLogger(HadoopUser.class);
	/**
	 * 
	 * @title 返回带用户之家的路径信息
	 *         前开后闭
	 * @return
	 * @author chao.xj
	 * @date 2016年4月27日下午5:56:17
	 * @company 怡创科技
	 * @version 2.0.1
	 */
	public static String homeOfUser() {
		// TODO Auto-generated constructor stub
		XmlService xmlService = new XmlServiceImpl();
		String env = "";
		try {
			env = xmlService.getSingleElementValue("sysconfig.xml", "SystemConfig/Enviroment");
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			logger.debug("当前hdfs用户为="+env);
		}
		if("".equals(env) || env==null){
			env = "hdfs";
		}
		return "user/"+env.toLowerCase()+"/";
	}
}
