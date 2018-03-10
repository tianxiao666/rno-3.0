/**
 * 
 */
package com.iscreate.op.service.rno.tool;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Properties;

import com.iscreate.op.dao.rno.RnoStructureAnalysisHiveDaoImpl;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

/**
 * @author chen.c10
 *
 */
public class SshExecuter implements Closeable {
	static int timeout = 3000;
	private SshInfo sshInfo = null;
	private JSch jsch = null;
	private Session session = null;

	private SshExecuter(SshInfo info) throws Exception {
		sshInfo = info;
		jsch = new JSch();
		session = jsch.getSession(sshInfo.getUser(), sshInfo.getHost(), sshInfo.getPort());
		session.setPassword(sshInfo.getPwd());
		UserInfo ui = new SshUserInfo(sshInfo.getPassPhrase());
		session.setUserInfo(ui);
		session.connect();
	}
	/**
	 * shell控制台，拥有登陆用户的完整env，可以输入多个指令用&&分割，末尾加上\n形成完整的语句才能正常得到输出，目前尚无很好的办法判断指令是否执行结束。
	 * @param cmd
	 * @param outputFileName
	 * @return
	 * @throws Exception
	 * @author chen.c10	
	 * @date 2016年4月22日
	 * @version RNO 3.0.1
	 */
	public long shell(String cmd, String outputFileName) throws Exception {
		long start = System.currentTimeMillis();
		ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
		PipedInputStream pipeIn = new PipedInputStream();
		PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
		FileOutputStream fileOut = new FileOutputStream(outputFileName);
		channelShell.setInputStream(pipeIn);
		channelShell.setOutputStream(System.out);
		channelShell.connect(timeout);
		exec("rm -rf sqoop.lock");
		exec("touch sqoop.lock");
		pipeOut.write(cmd.getBytes());
		// Thread.sleep(interval);

		while (true) {
			int i = exec("cat sqoop.lock");
			if (i != 0)
				break;
			Thread.sleep(1000);
		}
		System.out.println(channelShell.isClosed());
		System.out.println(channelShell.isEOF());
		pipeOut.close();
		pipeIn.close();
		fileOut.close();
		channelShell.disconnect();
		return System.currentTimeMillis() - start;
	}
	/**
	 * exec只能执行单个简单指令，无完整env，ChannelExec.setenv可以设置环境变量，可以得到命令结束标识，0为正常结束，非0异常
	 * @param cmd
	 * @return
	 * @throws Exception
	 * @author chen.c10	
	 * @date 2016年4月22日
	 * @version RNO 3.0.1
	 */
	public int exec(String cmd) throws Exception {
		ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
		channelExec.setCommand(cmd);
		channelExec.setInputStream(null);
		channelExec.setErrStream(System.err);

		InputStream in = channelExec.getInputStream();

		channelExec.connect();
		int res = -1;
		int buffLen = 1024;
		StringBuffer buf = new StringBuffer(buffLen);
		byte[] tmp = new byte[buffLen];
		while (true) {
			while (in.available() > 0) {
				int i = in.read(tmp, 0, buffLen);
				if (i < 0)
					break;
				buf.append(new String(tmp, 0, i));
			}
			if (channelExec.isClosed()) {
				res = channelExec.getExitStatus();
				//System.out.println(format("Exit-status: %d", res));
				break;
			}
			Thread.sleep(1000);
		}
		//System.out.println(buf);
		channelExec.disconnect();
		return res;
	}

	public static SshExecuter newInstance(String host,Integer port,String user,String pwd,String key,String passPhrase) throws Exception {
		SshInfo i = new SshInfo(host, port, user, pwd, key, passPhrase);
		return new SshExecuter(i);
	}

	public Session getSession() {
		return session;
	}

	public void close() throws IOException {
		getSession().disconnect();
	}

	public static void main(String[] args) throws Exception {
		SshExecuter ssh = null;
		InputStream is = null;
		try {
			/*-------------------------------更新工参------------------------------------------*/
			Properties p = new Properties();
			//is = new FileInputStream("ssh2sqoop.xml");
			is = RnoStructureAnalysisHiveDaoImpl.class.getResourceAsStream("ssh2sqoop.xml");
			p.loadFromXML(is);
			String host = p.getProperty("host", "");
			if (!host.isEmpty()) {
				Integer port = Integer.parseInt(p.getProperty("port", "22"));
				String user = p.getProperty("user", "");
				String pwd = p.getProperty("pwd", "");
				String key = p.getProperty("key", "");
				String passPhrase = p.getProperty("passPhrase", "");

				ssh = SshExecuter.newInstance(host, port, user, pwd, key, passPhrase);
				ssh.shell("/home/rnoprohbase/sqoop.sh &&rm -rf sqoop.lock\n",
						"E:/temp/sshtmp.txt");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				ssh.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}

class SshInfo {
	String host = null;
	Integer port = 22;
	String user = null;
	String pwd = null;
	String key = null;
	String passPhrase = null;

	public SshInfo(String host, Integer port, String user, String pwd, String key, String passPhrase) {
		super();
		this.host = host;
		this.port = port;
		this.user = user;
		this.pwd = pwd;
		this.key = key;
		this.passPhrase = passPhrase;
	}

	public String getHost() {
		return host;
	}

	public Integer getPort() {
		return port;
	}

	public String getUser() {
		return user;
	}

	public String getPwd() {
		return pwd;
	}

	public String getKey() {
		return key;
	}

	public String getPassPhrase() {
		return passPhrase;
	}
}

class SshUserInfo implements UserInfo {

	private String passphrase = null;

	public SshUserInfo(String passphrase) {
		super();
		this.passphrase = passphrase;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public String getPassword() {
		return null;
	}

	public boolean promptPassphrase(String pass) {
		return true;
	}

	public boolean promptPassword(String pass) {
		return true;
	}

	public boolean promptYesNo(String arg0) {
		return true;
	}

	public void showMessage(String m) {
		System.out.println(m);
	}
}
