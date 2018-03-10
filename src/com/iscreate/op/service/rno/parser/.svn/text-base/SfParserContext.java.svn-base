package com.iscreate.op.service.rno.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.BufferedMutatorParams;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;

import com.iscreate.op.service.rno.parser.jobrunnable.G4SfParserJobRunnable.DBFieldToTitle;
import com.iscreate.op.service.rno.tool.DateUtil;

public class SfParserContext {
	private long cityId = -1;
	private long fileId = -1;
	private StringBuilder errMsg = new StringBuilder();
	private DateUtil dateUtil = new DateUtil();
	// 将某日期对应的记录数量迭加缓存起来，不提供set方法，初始化为LinkedHashMap,通过clearCache方法清空数据
	private Map<String, Integer> dataNumMap = new LinkedHashMap<String, Integer>();
	private Map<String, DBFieldToTitle> dbFieldsToTitles;
	private Map<String, String> forParser;
	private Map<String, String> forSave;
	private Connection conn;
	// BufferedMutator为hbase新版api中用于代替HTable的提交工具,btw 查询功能由Table代替
	private Map<String, BufferedMutator> bufferedMutators = new HashMap<String, BufferedMutator>();
	// 每个HTable对应的put队列
	private Map<String, List<Put>> putsList = new HashMap<String, List<Put>>();

	public boolean buildConn(Configuration conf) {
		try {
			conn = ConnectionFactory.createConnection(conf);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public long getCityId() {
		return cityId;
	}

	public void setCityId(long cityId) {
		this.cityId = cityId;
	}

	public long getFileId() {
		return fileId;
	}

	public void setFileId(long fileId) {
		this.fileId = fileId;
	}

	public void appedErrorMsg(String msg) {
		errMsg.append(msg);
	}

	public String getErrorMsg() {
		return errMsg.toString();
	}

	public DateUtil getDateUtil() {
		return dateUtil;
	}

	public void setDateUtil(DateUtil dateUtil) {
		this.dateUtil = dateUtil;
	}

	public Map<String, Integer> getDataNumMap() {
		return dataNumMap;
	}

	public Map<String, DBFieldToTitle> getDbFieldsToTitles() {
		return dbFieldsToTitles;
	}

	public void setDbFieldsToTitles(Map<String, DBFieldToTitle> dbFieldsToTitles) {
		this.dbFieldsToTitles = dbFieldsToTitles;
	}

	public Map<String, String> getForParser() {
		return forParser;
	}

	public void setForParser(Map<String, String> forParser) {
		this.forParser = forParser;
	}

	public Map<String, String> getForSave() {
		return forSave;
	}

	public void setForSave(Map<String, String> forSave) {
		this.forSave = forSave;
	}

	/**
	 * 增加需要使用的HTable表<br/>
	 * hbase1.0后改用BufferedMutator，异步缓存提交，不支持自动提交，必须手动flush
	 * 
	 * @param key 获取table的key
	 * @param tableName 表名
	 * @author peng.jm
	 * @throws IOException
	 * @date 2014-12-5上午11:18:20
	 */
	public boolean addBufferedMutator(String key, String tableName) throws IOException {
		if (key == null || "".equals(key.trim())) {
			return false;
		}
		bufferedMutators.put(key, conn.getBufferedMutator(new BufferedMutatorParams(TableName.valueOf(tableName))
				.writeBufferSize(10 * 1024 * 1024)));
		return true;
	}

	/**
	 * 获取需要使用的HTable表
	 * 
	 * @param key
	 * @return
	 * @author peng.jm
	 * @date 2014-12-5上午11:29:30
	 */
	public BufferedMutator getBufferedMutator(String key) {
		if (key == null || "".equals(key.trim())) {
			return null;
		}
		return bufferedMutators.get(key);
	}

	/**
	 * 增加对应key的put队列
	 * 
	 * @param key
	 * @author peng.jm
	 * @date 2014-12-5上午11:33:41
	 */
	public boolean addPuts(String key) {
		if (key == null || "".equals(key.trim())) {
			return false;
		}
		putsList.put(key, new ArrayList<Put>());
		return true;
	}

	/**
	 * 获取对应key的put队列
	 * 
	 * @param key
	 * @return
	 * @author peng.jm
	 * @date 2014-12-5上午11:39:27
	 */
	public List<Put> getPuts(String key) {
		if (key == null || "".equals(key.trim())) {
			return null;
		}
		return putsList.get(key);
	}

	/**
	 * 清除put队列
	 * 
	 * @author peng.jm
	 * @date 2014-12-5上午11:36:37
	 */
	public void clearPuts() {
		for (List<Put> list : putsList.values()) {
			if (!list.isEmpty()) {
				list.clear();
			}
		}
	}

	/**
	 * 关闭对象内的HTable对象
	 * 
	 * @author peng.jm
	 * @throws
	 * @date 2014-12-5上午11:58:08
	 */
	public void closeBufferedMutators() {
		for (BufferedMutator t : bufferedMutators.values()) {
			if (t != null) {
				try {
					t.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 关闭hbase连接
	 * 
	 * @author chen.c10
	 * @date 2016年4月29日
	 * @version RNO 3.0.1
	 */
	public void closeConn() {
		if (conn != null) {
			try {
				conn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 当一个文件结束后，清理缓存
	 */
	public void clearCache() {
		dataNumMap.clear();
	}

	/**
	 * 清空全部
	 */
	public void clear() {
		clearPuts();
		closeBufferedMutators();
		closeConn();
	}
	/**
	 * 在每个文件开始前，清空上一个文件的缓存数据
	 * 
	 * @author chen.c10	
	 * @date 2016年4月29日
	 * @version RNO 3.0.1
	 */
	public void clearMidCache() {
		clearPuts();
		clearCache();
	}
	@Override
	public String toString() {
		return "SfParserContext [cityId=" + cityId + ", fileId=" + fileId + ", errMsg=" + errMsg + ", dateUtil="
				+ dateUtil + ", dataNumMap=" + dataNumMap + ", bufferedMutators=" + bufferedMutators + ", putsList="
				+ putsList + "]";
	}

}
