package com.meidusa.amoeba.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Logger;

import com.meidusa.amoeba.context.ProxyRuntimeContext;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.InitialisationException;
import com.meidusa.amoeba.util.StringUtil;

/**
 * 后端连接工厂,负责与后端数据库交互的连接工厂
 * @author struct
 *
 */
public abstract class BackendConnectionFactory extends AuthingableConnectionFactory  implements Initialisable{
	private static Logger logger = Logger.getLogger(BackendConnectionFactory.class);
	protected String manager;
	protected int port;
	protected String ipAddress;
	protected String user;
	protected String password;
	protected String schema;
	
	protected SocketChannelFactory socketChannelFactory;
	
	public SocketChannelFactory getSocketChannelFactory() {
		return socketChannelFactory;
	}

	public void setSocketChannelFactory(SocketChannelFactory socketChannelFactory) {
		this.socketChannelFactory = socketChannelFactory;
	}

	public String getManager() {
		return manager;
	}

	public void setManager(String manager) {
		this.manager = manager;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}
	
	public void init() throws InitialisationException {
		this.setSocketChannelFactory(new SocketChannelFactory(){

			public SocketChannel createSokectChannel() throws IOException {
				SocketChannel socketChannel = null;
				try{
					if(ipAddress == null){
						socketChannel = SocketChannel.open(new InetSocketAddress(port));
					}else{
						socketChannel = SocketChannel.open(new InetSocketAddress(ipAddress, port));
					}
					socketChannel.configureBlocking(false);
				}catch(IOException e){
					logger.error("could not connect to server["+ipAddress+":"+port+"]",e);
					throw e;
				}
				return socketChannel;
			}
		});
		
		ConnectionManager conMgr = ProxyRuntimeContext.getInstance().getConnectionManagerList().get(manager);
		if(conMgr == null){
			throw new InitialisationException("can not found connectionManager by name="+manager);
		}
		this.setConnectionManager(conMgr);
		
	}
	
	protected void initConnection(Connection connection){
		super.initConnection(connection);
		if(connection instanceof  DatabaseConnection){
			DatabaseConnection conn = (DatabaseConnection) connection;
			conn.setSchema(this.getSchema());
			if(!StringUtil.isEmpty(user)){
				conn.setUser(user);
				conn.setPassword(password);
			}else{
				conn.setUser(ProxyRuntimeContext.getInstance().getConfig().getUser());
				conn.setPassword(ProxyRuntimeContext.getInstance().getConfig().getPassword());
			}
		}
	}
	
}
