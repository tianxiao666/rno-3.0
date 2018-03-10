package com.iscreate.op.service.rno.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class HiveHelper {

	public static UdfObj readHiveFunCfgFromXml(String funName) {
		boolean flag = false;
		UdfObj dt = null;
		try {
			InputStream in = new FileInputStream(new File(
					HiveHelper.class.getClassLoader().getResource(
							"hive.xml").getPath()));
			SAXReader reader = new SAXReader();
			Document doc = reader.read(in);
			Element root = doc.getRootElement();
			for (Object o : root.elements()) {
				Element e = (Element) o;
				for (Object obj : e.elements()) {
					Element e1 = (Element) obj;
					String key = e1.getName();
					String val = e1.getTextTrim();
					if ("funname".equals(key)) {
						if(funName.equals(val)){
							flag = true;
							dt = new UdfObj();
						}else{
							break;
						}
						dt.setFunName(val);
					}
					if ("classpath".equals(key)) {
						dt.setClassPath(val);
					}
					if ("package".equals(key)) {
						dt.setPackageStr(val);
					}
					if ("desc".equals(key)) {
						dt.setDesc(val);
					}
				}
				if(flag){
					break;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return dt;
	}
	public static class UdfObj {
		String funName;
		String classPath;
		String packageStr;
		String desc;
		public String getFunName() {
			return funName;
		}
		public void setFunName(String funName) {
			this.funName = funName;
		}
		public String getClassPath() {
			return classPath;
		}
		public void setClassPath(String classPath) {
			this.classPath = classPath;
		}
		public String getPackageStr() {
			return packageStr;
		}
		public void setPackageStr(String packageStr) {
			this.packageStr = packageStr;
		}
		public String getDesc() {
			return desc;
		}
		public void setDesc(String desc) {
			this.desc = desc;
		}
		@Override
		public String toString() {
			return "UdfObj [funName=" + funName + ", classPath=" + classPath
					+ ", packageStr=" + packageStr + ", desc=" + desc + "]";
		}
		

	}
	/**
	 * @title 
	 * @param args
	 * @author chao.xj
	 * @date 2015-11-19下午1:35:57
	 * @company 怡创科技
	 * @version 2.0.1
	 */

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		UdfObj  aa = readHiveFunCfgFromXml("lnglathelper");
		System.out.println(aa);
	}

}
