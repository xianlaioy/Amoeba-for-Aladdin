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
package com.meidusa.amoeba.context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.LogLog;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.meidusa.amoeba.config.BeanObjectEntityConfig;
import com.meidusa.amoeba.config.ConfigurationException;
import com.meidusa.amoeba.config.DBServerConfig;
import com.meidusa.amoeba.config.DocumentUtil;
import com.meidusa.amoeba.config.ParameterMapping;
import com.meidusa.amoeba.config.ProxyServerConfig;
import com.meidusa.amoeba.net.ConnectionManager;
import com.meidusa.amoeba.net.poolable.MultipleLoadBalanceObjectPool;
import com.meidusa.amoeba.net.poolable.ObjectPool;
import com.meidusa.amoeba.net.poolable.PoolableObject;
import com.meidusa.amoeba.net.poolable.PoolableObjectFactory;
import com.meidusa.amoeba.route.QueryRouter;
import com.meidusa.amoeba.util.Initialisable;
import com.meidusa.amoeba.util.InitialisationException;
import com.meidusa.amoeba.util.Reporter;
import com.meidusa.amoeba.util.StringUtil;

/**
 * Amoeba运行时的一些信息（context），包括amoeba.xml中的所有信息等、、、、、
 * @author <a href=mailto:piratebase@sina.com>Struct chen</a>
 */
public abstract class ProxyRuntimeContext implements Reporter {

    public static final String             DEFAULT_SERVER_CONNECTION_MANAGER_CLASS = "com.meidusa.amoeba.net.AuthingableConnectionManager";
    public static final String             DEFAULT_REAL_POOL_CLASS                 = "com.meidusa.amoeba.net.poolable.PoolableObjectPool";
    public static final String             DEFAULT_VIRTUAL_POOL_CLASS              = "com.meidusa.amoeba.server.MultipleServerPool";

    protected static Logger                logger                                  = Logger.getLogger(ProxyRuntimeContext.class);

    private static ProxyRuntimeContext     context;

    private ProxyServerConfig              config;

    private Executor                       readExecutor;
    private Executor                       clientSideExecutor;
    private Executor                       serverSideExecutor;
    private Map<String, ConnectionManager> conMgrMap                               = new HashMap<String, ConnectionManager>();
    private Map<String, ConnectionManager> readOnlyConMgrMap                       = Collections.unmodifiableMap(conMgrMap);

    private Map<String, ObjectPool>        poolMap                                 = new HashMap<String, ObjectPool>();
    private Map<String, ObjectPool>        readOnlyPoolMap                         = Collections.unmodifiableMap(poolMap);

    private QueryRouter                    queryRouter;
    private String                         serverCharset;
    /**
     * 返回context的值
     * @return context
     */
    public static ProxyRuntimeContext getInstance() {
        return context;
    }
    /**
     * 构造函数，给成员变量context赋值
     * @param context
     */
    protected static void setInstance(ProxyRuntimeContext context) {
        ProxyRuntimeContext.context = context;
    }
    /**
     * 抽象函数
     * @return
     */
    protected abstract String getDefaultServerConnectionFactoryClassName();
    /**
     * 返回DEFAULT_SERVER_CONNECTION_MANAGER_CLASS的值
     * @return "com.meidusa.amoeba.net.AuthingableConnectionManager"
     */
    protected String getDefaultServerConnectionManagerClassName() {
        return DEFAULT_SERVER_CONNECTION_MANAGER_CLASS;
    }
    /**
     * 返回 DEFAULT_REAL_POOL_CLASS的值
     * @return "com.meidusa.amoeba.net.poolable.PoolableObjectPool"
     */
    protected String getDefaultRealPoolClassName() {
        return DEFAULT_REAL_POOL_CLASS;
    }
    /**
     * 返回DEFAULT_VIRTUAL_POOL_CLASS的值
     * @return "com.meidusa.amoeba.server.MultipleServerPool"
     */
    protected String getDefaultVirtualPoolClassName() {
        return DEFAULT_VIRTUAL_POOL_CLASS;
    }
    /**
     * 服务器的字符集serverCharset
     * @return serverCharset
     */
    public String getServerCharset() {
        return serverCharset;
    }
    /**
     * 给成员变量serverCharset赋值
     * @param serverCharset
     */
    public void setServerCharset(String serverCharset) {
        this.serverCharset = serverCharset;
    }
    /**
     * 返回config的值，config中保持的有amoeba.xml的全部信息，包括ip，ipaddress port,dbserver,server1,connectionManagerList等等
     * @return config
     */
    public ProxyServerConfig getConfig() {
        return config;
    }
    /**
     * 
     * @return queryRouter
     */
    public QueryRouter getQueryRouter() {
        return queryRouter;
    }

    static class ReNameableThreadExecutor extends ThreadPoolExecutor {

        // Map<Thread,String> threadNameMap = new HashMap<Thread,String>();
    	/**
    	 * 
    	 * @param poolSize 譬如amoeba.xml server结点的readThreadPoolSize的值
    	 */
        public ReNameableThreadExecutor(int poolSize){
            super(poolSize, poolSize, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new LinkedBlockingQueue<Runnable>());
        }

        // protected void beforeExecute(Thread t, Runnable r) {
        // if (r instanceof NameableRunner) {
        // NameableRunner nameableRunner = (NameableRunner) r;
        // String name = t.getName();
        // if (name != null) {
        // threadNameMap.put(t, t.getName());
        // t.setName(nameableRunner.getRunnerName() + ":" + t.getName());
        // }
        // }
        // };
        //
        // protected void afterExecute(Runnable r, Throwable t) {
        // if (r instanceof NameableRunner) {
        // String name = threadNameMap.remove(Thread.currentThread());
        // if (name != null) {
        // Thread.currentThread().setName(name);
        // }
        // }
        // };
    }

    protected ProxyRuntimeContext(){
    }

    public Map<String, ConnectionManager> getConnectionManagerList() {
        return readOnlyConMgrMap;
    }

    public Executor getClientSideExecutor() {
        return clientSideExecutor;
    }

    public Executor getReadExecutor() {
        return readExecutor;
    }

    public Executor getServerSideExecutor() {
        return serverSideExecutor;
    }

    public Map<String, ObjectPool> getPoolMap() {
        return readOnlyPoolMap;
    }

    private List<Initialisable> initialisableList = new ArrayList<Initialisable>();
    /**
     * 
     * @param file
     */
    public void init(String file) {
        config = loadConfig(file);//譬如file指代的是amoeba.xml,config保存的是amoeba.xml中的全部信息
        readExecutor = new ReNameableThreadExecutor(config.getReadThreadPoolSize());
        serverSideExecutor = new ReNameableThreadExecutor(config.getServerSideThreadPoolSize());
        clientSideExecutor = new ReNameableThreadExecutor(config.getClientSideThreadPoolSize());
        serverCharset = config.getServerCharset();
        /**
         * 猜测：config中保持的有amoeba.xml中的全部配置信息，下面这个for是根据connectionManagerList进行相应处理
         */
        for (Map.Entry<String, BeanObjectEntityConfig> entry : config.getManagers().entrySet()) {
            BeanObjectEntityConfig beanObjectEntityConfig = entry.getValue();
            try {
                ConnectionManager manager = (ConnectionManager) beanObjectEntityConfig.createBeanObject(false);
                manager.setName(entry.getKey());
                initialisableList.add(manager);
                conMgrMap.put(manager.getName(), manager);
            } catch (Exception e) {
                throw new ConfigurationException("manager instance error", e);
            }
        }
        /**
         * config中保存有amoeba.xml的全部配置信息，下面这个for是根据dbServerList中的信息进行相应的处理
         */
        for (Map.Entry<String, DBServerConfig> entry : config.getDbServers().entrySet()) {
            DBServerConfig dbServerConfig = entry.getValue();
            try {
                BeanObjectEntityConfig poolConfig = dbServerConfig.getPoolConfig();
                ObjectPool pool = (ObjectPool) poolConfig.createBeanObject(false);
                if (pool instanceof Initialisable) {
                    initialisableList.add((Initialisable) pool);
                }
                if (dbServerConfig.getFactoryConfig() != null) {
                    PoolableObjectFactory factory = (PoolableObjectFactory) dbServerConfig.getFactoryConfig().createBeanObject(false);
                    if (factory instanceof Initialisable) {
                        initialisableList.add((Initialisable) factory);
                    }
                    pool.setFactory(factory);
                }
                poolMap.put(entry.getKey(), pool);
            } catch (Exception e) {
//                throw new ConfigurationException("manager instance error", e);//原作者有这个抛出异常的语句,调试的时候会报错，就把它注释掉了
            }
        }
        //config中保存有amoeba.xml的全部配置信息，下面这个for是根据queryRouter中的信息进行相应的处理
        if (config.getQueryRouterConfig() != null) {
            BeanObjectEntityConfig queryRouterConfig = config.getQueryRouterConfig();
            try {
                queryRouter = (QueryRouter) queryRouterConfig.createBeanObject(false);
                if (queryRouter instanceof Initialisable) {
                    initialisableList.add((Initialisable) queryRouter);
                }
            } catch (Exception e) {
//                throw new ConfigurationException("queryRouter instance error", e);//原作者有这个抛出异常的语句,调试的时候会报错，就把它注释掉了
            }
        }

        initAllInitialisableBeans();
        initialisableList.clear();
        for (ConnectionManager cm : getConnectionManagerList().values()) {
            cm.setExecutor(this.getReadExecutor());
            cm.start();
        }
        initPools();
    }

    protected void initPools() {
        for (Map.Entry<String, ObjectPool> entry : poolMap.entrySet()) {
            ObjectPool pool = entry.getValue();
            if (pool instanceof MultipleLoadBalanceObjectPool) {
                MultipleLoadBalanceObjectPool multiPool = (MultipleLoadBalanceObjectPool) pool;
                multiPool.initAllPools();
            } else {
                PoolableObject object = null;
                try {
                    object = (PoolableObject) pool.borrowObject();
                } catch (Exception e) {
                    logger.error("init pool error!", e);
                } finally {
                    if (object != null) {
                        try {
                            pool.returnObject(object);
                        } catch (Exception e) {
                            logger.error("return init pools error", e);
                        }
                    }
                }
            }
        }
    }
    /**
     * initialisableList中保持的是amoeba.xml的配置信息的另一种形式（先前的形式是config）
     * 把initialisableList中的信息添加到bean中
     */
    private void initAllInitialisableBeans() {
        for (Initialisable bean : initialisableList) {
            try {
                bean.init();
            } catch (InitialisationException e) {
                throw new ConfigurationException("Initialisation Exception", e);
            }
        }
    }
    /**
     * 解析amoeba.dtd,其实就是解析amoeba.xml
     * configFileName = "F:\\project\\Amoeba\\Aladdin\\conf\\amoeba.xml"
     * @param configFileName 
     * @return 保存的是amoeba.xml中的全部信息（ProxyServerConfig类型）
     */
    private ProxyServerConfig loadConfig(String configFileName) {
        DocumentBuilder db;
//        System.out.println(configFileName);
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setNamespaceAware(false);

            db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {

                public InputSource resolveEntity(String publicId, String systemId) {
                    if (systemId.endsWith("amoeba.dtd")) {
                        InputStream in = ProxyRuntimeContext.class.getResourceAsStream("/com/meidusa/amoeba/xml/amoeba.dtd");
                        if (in == null) {
                            LogLog.error("Could not find [amoeba.dtd]. Used [" + ProxyRuntimeContext.class.getClassLoader() + "] class loader in the search.");
                            return null;
                        } else {
                            return new InputSource(in);
                        }
                    } else {
                        return null;
                    }
                }
            });
            db.setErrorHandler(new ErrorHandler() {

                public void warning(SAXParseException exception) {
                }

                public void error(SAXParseException exception) throws SAXException {
                    logger.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
                    throw exception;
                }

                public void fatalError(SAXParseException exception) throws SAXException {
                    logger.fatal(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
                    throw exception;
                }
            });
            // configFileName = "F:\\project\\Amoeba\\Aladdin\\conf\\amoeba.xml"
            return loadConfigurationFile(configFileName, db);
        } catch (Exception e) {
            logger.fatal("Could not load configuration file, failing", e);
            throw new ConfigurationException("Error loading configuration file " + configFileName, e);
        }
    }
    /**
     * 解析amoeba.xml
     * @param fileName 类似 "F:\\project\\Amoeba\\Aladdin\\conf\\amoeba.xml"
     * @param db
     * @return config 保存的是amoeba.xml中的全部信息
     */
    private ProxyServerConfig loadConfigurationFile(String fileName, DocumentBuilder db) {
        Document doc = null;
        InputStream is = null;
        ProxyServerConfig config = new ProxyServerConfig();
        try {
            is = new FileInputStream(new File(fileName));

            if (is == null) {
                throw new Exception("Could not open file " + fileName);
            }

            doc = db.parse(is);
        } catch (Exception e) {
            final String s = "Caught exception while loading file " + fileName;
            logger.error(s, e);
            throw new ConfigurationException(s, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    logger.error("Unable to close input stream", e);
                }
            }
        }
        Element rootElement = doc.getDocumentElement();
        NodeList children = rootElement.getChildNodes();
        int childSize = children.getLength();

        for (int i = 0; i < childSize; i++) {
            Node childNode = children.item(i);

            if (childNode instanceof Element) {
                Element child = (Element) childNode;

                final String nodeName = child.getNodeName();
                if (nodeName.equals("server")) {
//                	System.out.println(child.getChildNodes());
                    loadServerConfig(child, config);
                } else if (nodeName.equals("connectionManagerList")) {
                    loadConnectionManagers(child, config);
                } else if (nodeName.equals("dbServerList")) {
                    loadServers(child, config);
                } else if (nodeName.equals("queryRouter")) {
                    loadQueryRouter(rootElement, config);
                }
            }
        }

        if (logger.isInfoEnabled()) {
            logger.info("Loaded Engine configuration from: " + fileName);
        }
        return config;
    }
    /**
     * 分析amoeba.xml中queryRouter结点信息
     * @param current
     * @param config
     */
    private void loadQueryRouter(Element current, ProxyServerConfig config) {
        BeanObjectEntityConfig queryRouter = DocumentUtil.loadBeanConfig(DocumentUtil.getTheOnlyElement(current, "queryRouter"));
        config.setQueryRouterConfig(queryRouter);
    }
    /**
     * 分析amoeba.xml中dbServerList结点信息
     * @param current
     * @param config
     * @author Li Hui
     */
    private void loadServers(Element current, ProxyServerConfig config) {
        NodeList children = current.getChildNodes();
        int childSize = children.getLength();
        for (int i = 0; i < childSize; i++) {
            Node childNode = children.item(i);
            if (childNode instanceof Element) {
                Element child = (Element) childNode;
                DBServerConfig serverConfig = loadServer(child);
                if (serverConfig.isVirtual()) {
                    if (serverConfig.getPoolConfig() != null) {
                        if (StringUtil.isEmpty(serverConfig.getPoolConfig().getClassName())) {
                            serverConfig.getPoolConfig().setClassName(getDefaultVirtualPoolClassName());
                        }
                    }
                } else {
                    if (serverConfig.getPoolConfig() != null) {
                        if (StringUtil.isEmpty(serverConfig.getPoolConfig().getClassName())) {
                            serverConfig.getPoolConfig().setClassName(getDefaultRealPoolClassName());
                        }
                    }
                }

                if (serverConfig.getFactoryConfig() != null) {
                    if (StringUtil.isEmpty(serverConfig.getFactoryConfig().getClassName())) {
                        serverConfig.getFactoryConfig().setClassName(getDefaultServerConnectionFactoryClassName());
                    }
                }
                /**
                 * 把每一个dbServer，譬如dbServer1节点中的信息所有信息保存到serverConfig中
                 */
                config.addServer(serverConfig.getName(), serverConfig);//添加config为(dbServer1,dbServer1中的全部信息）
            }
        }
    }
    /**
     * 分析amoeba.xml中dbServerList结点中dbServer子节点信息
     * @param current
     * @return
     */
    private DBServerConfig loadServer(Element current) {
        DBServerConfig serverConfig = new DBServerConfig();
        NamedNodeMap nodeMap = current.getAttributes();
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < nodeMap.getLength(); i++) {
            Node node = nodeMap.item(i);
            if (node instanceof org.w3c.dom.Attr) {
                Attr attr = (Attr) node;
                map.put(attr.getName(), attr.getNodeValue());
            }
        }

        ParameterMapping.mappingObject(serverConfig, map);

        BeanObjectEntityConfig factory = DocumentUtil.loadBeanConfig(DocumentUtil.getTheOnlyElement(current, "factoryConfig"));
        BeanObjectEntityConfig pool = DocumentUtil.loadBeanConfig(DocumentUtil.getTheOnlyElement(current, "poolConfig"));
        if (pool != null) {
            serverConfig.setPoolConfig(pool);
        }

        if (factory != null) {
            serverConfig.setFactoryConfig(factory);
        }

        return serverConfig;
    }
    /**
     * 分析amoeba.xml中connectionManagerList结点信息
     * 通过调试，目前并没有发现有什么具体作用
     * @param current
     * @param config
     */
    private void loadConnectionManagers(Element current, ProxyServerConfig config) {
        NodeList children = current.getChildNodes();
        int childSize = children.getLength();
        for (int i = 0; i < childSize; i++) {
            Node childNode = children.item(i);
            if (childNode instanceof Element) {
                Element child = (Element) childNode;
                BeanObjectEntityConfig managerConfig = DocumentUtil.loadBeanConfig(child);
                if (StringUtil.isEmpty(managerConfig.getClassName())) {
                    managerConfig.setClassName(getDefaultServerConnectionManagerClassName());
                }
                config.addManager(managerConfig.getName(), managerConfig);
            }
        }
    }
    /**
     * 把conf/amoeba.xml中server结点对应的信息添加到map中
     * @param current
     * @param config
     */
    private void loadServerConfig(Element current, ProxyServerConfig config) {
        NodeList children = current.getChildNodes();
        int childSize = children.getLength();
        Map<String, Object> map = new HashMap<String, Object>();
        for (int i = 0; i < childSize; i++) {
            Node childNode = children.item(i);
            if (childNode instanceof Element) {
                Element child = (Element) childNode;
                final String nodeName = child.getNodeName();
                if (nodeName.equals("property")) {
                    String key = child.getAttribute("name");
                    String value = child.getTextContent();
                    map.put(key, value);
                }
            }
        }
        ParameterMapping.mappingObject(config, map);
    }
    /**
     * 把一些log信息输入到project.log文件中
     */
    public void appendReport(StringBuilder buffer, long now, long sinceLast, boolean reset, Level level) {
    	/**
    	 * 把log输出到project.log文件中，生成的代码类似下面这个样子的
    	 * * Server pool=server1 - pool active Size=0, pool Idle size=1
    	   * Server pool=server2 - pool active Size=0, pool Idle size=1
           * Server pool=multiPool - pool active Size=0, pool Idle size=2
           * Server pool=_null_table_ - pool active Size=0, pool Idle size=1
    	 */
    	for (Map.Entry<String, ObjectPool> entry : getPoolMap().entrySet()) {
            ObjectPool pool = entry.getValue();
            String poolName = entry.getKey();
            buffer.append("* Server pool=").append(poolName == null ? "default pool" : poolName).append("\n").append(" - pool active Size=").append(pool.getNumActive());
            buffer.append(", pool Idle size=").append(pool.getNumIdle()).append("\n");
        }
    }
}
