/**
 * <pre>
 * 	This program is free software; you can redistribute it and/or modify it under the terms of 
 * the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, 
 * or (at your option) any later version. 
 * 
 * 	This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU General Public License for more details. 
 * 	You should have received a copy of the GNU General Public License along with this program; 
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * </pre>
 */
package com.meidusa.amoeba.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 指定一个端口,创建一个serverSocket. 将该ServerSocket所创建的Connection加入管理
 * 
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public class ServerableConnectionManager extends AuthingableConnectionManager {

    protected static Logger       log = Logger.getLogger(ServerableConnectionManager.class);

    protected int                 port;
    protected ServerSocketChannel ssocket;
    protected String              ipAddress;
    protected ConnectionFactory   connFactory;

    public ServerableConnectionManager() throws IOException{
    }

    public ServerableConnectionManager(String name, int port) throws IOException{
        super(name);
        this.port = port;
    }
    /**
     * 
     * @param name "Aladdin proxy Server"
     * @param ipAddress Aladdin proxy的ip
     * @param port Aladdin proxy的ip port
     * @throws IOException
     */
    public ServerableConnectionManager(String name, String ipAddress, int port) throws IOException{
        super(name);
        this.port = port;
        this.ipAddress = ipAddress;
    }
    /**
     * Amoeba Proxy Server的连接工厂，实际上就是应用程序与Amoeba Proxy Server连接的user,pwd等
     * @param connFactory
     */
    public void setConnectionFactory(ConnectionFactory connFactory) {
        this.connFactory = connFactory;
        if (connFactory instanceof AbstractConnectionFactory) {
            AbstractConnectionFactory afactory = (AbstractConnectionFactory) connFactory;
            if (afactory.getConnectionManager() == null) {
                afactory.setConnectionManager(this);
            }
        }
    }

    // documentation inherited
    protected void willStart() {
        super.willStart();
        try {
            // create a listening socket and add it to the select set
            ssocket = ServerSocketChannel.open();
            ssocket.configureBlocking(false);

            InetSocketAddress isa = null;
            if (ipAddress != null) {
                isa = new InetSocketAddress(ipAddress, port);
            } else {
                isa = new InetSocketAddress(port);
            }

            ssocket.socket().bind(isa);
            registerServerChannel(ssocket);

            Level level = log.getLevel();
            log.setLevel(Level.INFO);
            /**
             * 当执行amoeba start命令后，会在dos窗口输出这段话，类似于
             * 2012-03-08 13:12:39,234 INFO  net.ServerableConnectionManager - Amoeba Monitor Server listening on /127.0.0.1:36190.
             */
            log.info("Server listening on " + isa + ".");
            log.setLevel(level);

        } catch (IOException ioe) {
        	/**
        	 * 当执行amoeba start命令后，如果用于把amoba.xml中的amoeba的ip修改了，会出现下面这样的log
        	 * 2012-03-09 19:41:09,687 ERROR net.ServerableConnectionManager - Failure listening to socket on port '8066'.
        	 */
            log.error("Failure listening to socket on port '" + port + "'.", ioe);
            System.exit(-1);
        }
    }

    protected void registerServerChannel(final ServerSocketChannel listener) throws IOException {

        // register this listening socket and map its select key to a net event
        // handler that will
        // accept new connections
        NetEventHandler serverNetEvent = new NetEventHandler() {

            private SelectionKey key;

            public int handleEvent(long when) {
                acceptConnection(listener);
                return 0;
            }

            public boolean checkIdle(long now) {
                return false; // we're never idle
            }

            public SelectionKey getSelectionKey() {
                return key;
            }

            public void setSelectionKey(SelectionKey key) {
                this.key = key;
            }

            public boolean doWrite() {
                return true;
            }
        };
        SelectionKey sk = listener.register(_selector, SelectionKey.OP_ACCEPT, serverNetEvent);
        serverNetEvent.setSelectionKey(sk);
        postRegisterNetEventHandler(serverNetEvent, SelectionKey.OP_ACCEPT);
    }

    protected void acceptConnection(ServerSocketChannel listener) {
        SocketChannel channel = null;
        try {
            channel = listener.accept();
            if (channel == null) {
                log.info("Psych! Got ACCEPT_READY, but no connection.");
                return;
            }

            if (!(channel instanceof SelectableChannel)) {
                try {
                    log.warn("Provided with un-selectable socket as result of accept(), can't " + "cope [channel=" + channel + "].");
                } catch (Error err) {
                    log.warn("Un-selectable channel also couldn't be printed.");
                }
                // stick a fork in the socket
                channel.socket().close();
                return;
            }
            connFactory.createConnection(channel, System.currentTimeMillis());
            // this.postRegisterNetEventHandler(connection,SelectionKey.OP_READ);
        } catch (Exception e) {
            if (channel != null) {
                try {
                    channel.socket().close();
                } catch (IOException ioe) {
                    log.warn("Failed closing aborted connection: " + ioe);
                }
            }
        }
    }

    public void closeAll() {
        super.closeAll();
        try {
            ssocket.close();
        } catch (IOException e) {
        }
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

}
