/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import org.apache.commons.collections.BeanMap;
import org.apache.commons.collections.ReferenceMap;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.observation.ObservationManagerFactory;
import org.apache.jackrabbit.core.state.*;
import org.apache.jackrabbit.core.state.tx.TransactionManager;
import org.apache.jackrabbit.core.state.tx.XASessionImpl;
import org.apache.jackrabbit.core.util.uuid.UUID;
import org.apache.jackrabbit.core.version.PersistentVersionManager;
import org.apache.log4j.Logger;

import javax.jcr.*;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * A <code>RepositoryImpl</code> ...
 */
public class RepositoryImpl implements Repository, SessionListener, EventListener {

    private static Logger log = Logger.getLogger(RepositoryImpl.class);

    /**
     * hardcoded uuid of the repository root node
     */
    private static final String ROOT_NODE_UUID = "ac3b5c25-613d-4798-8494-ffbcca9c5c6c";

    private static final String ANONYMOUS_USER = "anonymous";

    private static final Credentials ANONYMOUS_CREDENTIALS =
            new SimpleCredentials(ANONYMOUS_USER, new char[0]);

    private static final String PROPERTIES_RESOURCE = "rep.properties";
    private final Properties repProps;

    // names of well known repository properties
    public static final String STATS_NODE_COUNT_PROPERTY = "jcr.repository.stats.nodes.count";
    public static final String STATS_PROP_COUNT_PROPERTY = "jcr.repository.stats.properties.count";

    // pre-defined values of well known repository properties
    // @todo update as necessary
    private static final String SPEC_VERSION = "0.15";
    private static final String SPEC_NAME = "Content Repository API for Java(TM) Technology Specification";
    private static final String REP_VENDOR = "Apache Software Foundation";
    private static final String REP_VENDOR_URL = "http://www.apache.org/";
    private static final String REP_NAME = "Jackrabbit";
    private static final String REP_VERSION = "0.15";

    // system root location
    public static final QName SYSTEM_ROOT_NAME = new QName(NamespaceRegistryImpl.NS_JCR_URI, "system");

    private String rootNodeUUID;

    private final NamespaceRegistryImpl nsReg;
    private final NodeTypeRegistry ntReg;
    private final PersistentVersionManager vMgr;
    private final TransactionManager txMgr;

    // configuration of the repository
    private final RepositoryConfig repConfig;
    // the master filesystem
    private final FileSystem repStore;
    // sub file system where the repository stores meta data such as uuid of root node, etc.
    private final FileSystem metaDataStore;
    // sub file system where the repository stores versions
    private final FileSystem versionStore;

    /**
     * map of workspace names and <code>WorkspaceInfo<code>s.
     */
    private final HashMap wspInfos = new HashMap();

    /**
     * active sessions (weak references)
     */
    private final ReferenceMap activeSessions =
            new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    // misc. statistics
    private long nodesCount = 0;
    private long propsCount = 0;

    // flag indicating if respository has been shut down
    private boolean disposed = false;

    /**
     * private constructor
     *
     * @param repConfig
     */
    private RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {
        this.repConfig = repConfig;
        // setup file systems
        repStore = repConfig.getFileSystem();
        String fsRootPath = "/meta";
        try {
            if (!repStore.exists(fsRootPath) || !repStore.isFolder(fsRootPath)) {
                repStore.createFolder(fsRootPath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to create folder for repository meta data";
            log.error(msg, fse);
            throw new RepositoryException(msg, fse);
        }
        metaDataStore = new BasedFileSystem(repStore, fsRootPath);

        fsRootPath = "/versions";
        try {
            if (!repStore.exists(fsRootPath) || !repStore.isFolder(fsRootPath)) {
                repStore.createFolder(fsRootPath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to create folder for repository version store";
            log.error(msg, fse);
            throw new RepositoryException(msg, fse);
        }
        versionStore = new BasedFileSystem(repStore, fsRootPath);

        FileSystemResource uuidFile = new FileSystemResource(metaDataStore, "rootUUID");
        try {
            if (uuidFile.exists()) {
                try {
                    // load uuid of the repository's root node
                    InputStream in = uuidFile.getInputStream();
/*
		    // uuid is stored in binary format (16 bytes)
		    byte[] bytes = new byte[16];
		    try {
			in.read(bytes);
		    } finally {
			try {
			    in.close();
			} catch (IOException ioe) {
			    // ignore
			}
		    }
		    rootNodeUUID = new UUID(bytes).toString();
*/
                    // uuid is stored in text format (36 characters) for better readability
                    char[] chars = new char[36];
                    InputStreamReader reader = new InputStreamReader(in);
                    try {
                        reader.read(chars);
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                    rootNodeUUID = new UUID(new String(chars)).toString();
                } catch (Exception e) {
                    String msg = "failed to load persisted repository state";
                    log.error(msg, e);
                    throw new RepositoryException(msg, e);
                }
            } else {
                // create new uuid
/*
                UUID rootUUID = UUID.randomUUID();     // version 4 uuid
                rootNodeUUID = rootUUID.toString();
*/
                /**
                 * use hard-coded uuid for root node rather than generating
                 * a different uuid per repository instance; using a
                 * hard-coded uuid makes it easier to copy/move entire
                 * workspaces from one repository instance to another.
                 */
                rootNodeUUID = ROOT_NODE_UUID;
                try {
                    // persist uuid of the repository's root node
                    OutputStream out = uuidFile.getOutputStream();
/*
                    // store uuid in binary format
                    try {
                        out.write(rootUUID.getBytes());
                    } finally {
                        try {
                            out.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
*/
                    // store uuid in text format for better readability
                    OutputStreamWriter writer = new OutputStreamWriter(out);
                    try {
                        writer.write(rootNodeUUID);
                    } finally {
                        try {
                            writer.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                } catch (Exception e) {
                    String msg = "failed to persist repository state";
                    log.error(msg, e);
                    throw new RepositoryException(msg, e);
                }
            }
        } catch (FileSystemException fse) {
            String msg = "failed to access repository state";
            log.error(msg, fse);
            throw new RepositoryException(msg, fse);
        }

        // setup internal transaction manager
        // @todo rewrite to use file system abstraction (FileSystem interface)
        try {
            File txRootDir = new File(repConfig.getHomeDir(), "tx");
            txMgr = new TransactionManager(txRootDir);
        } catch (IOException ioe) {
            String msg = "failed to initialize internal transaction manager";
            log.error(msg, ioe);
            throw new RepositoryException(msg, ioe);
        }

        // workspaces
        Iterator iter = repConfig.getWorkspaceConfigs().iterator();
        while (iter.hasNext()) {
            WorkspaceConfig config = (WorkspaceConfig) iter.next();
            WorkspaceInfo info = new WorkspaceInfo(config);
            wspInfos.put(config.getName(), info);
        }

        nsReg = new NamespaceRegistryImpl(new BasedFileSystem(repStore, "/namespaces"));

        ntReg = NodeTypeRegistry.create(nsReg, new BasedFileSystem(repStore, "/nodetypes"));

        /**
         * todo implement 'System' workspace
         * - the system workspace should have the /jcr:system node
         * - versions, version history and node types should be reflected in
         *   this system workspace as content under /jcr:system
         * - all other workspaces should be dynamic workspaces based on
         *   this 'read-only' system workspace
         */

        // check system root node of system workspace
        // (for now, we just create a system root node in all workspaces)
        Iterator wspNames = wspInfos.keySet().iterator();
        while (wspNames.hasNext()) {
            String wspName = (String) wspNames.next();
            NodeImpl rootNode = (NodeImpl) getSystemSession(wspName).getRootNode();
            if (!rootNode.hasNode(SYSTEM_ROOT_NAME)) {
                rootNode.addNode(SYSTEM_ROOT_NAME, NodeTypeRegistry.NT_UNSTRUCTURED);
                rootNode.save();
            }
        }

        // init version manager
        // todo: as soon as dynamic workspaces are available, base on system ws
        SessionImpl verSession = getSystemSession(repConfig.getDefaultWorkspaceName());
        NodeImpl vRootNode = (NodeImpl) verSession.getRootNode();
        try {
            if (!vRootNode.hasNode(SYSTEM_ROOT_NAME)) {
                verSession.getWorkspace().clone(repConfig.getDefaultWorkspaceName(),
                        SYSTEM_ROOT_NAME.toJCRName(verSession.getNamespaceResolver()),
                        SYSTEM_ROOT_NAME.toJCRName(verSession.getNamespaceResolver()));
            }
        } catch (NoPrefixDeclaredException npde) {
            String msg = "failed to initialize version manager";
            log.error(msg, npde);
            throw new RepositoryException(msg, npde);
        }
        vMgr = new PersistentVersionManager(verSession);

        // load repository properties
        repProps = new Properties();
        loadRepProps();
        nodesCount = Long.parseLong(repProps.getProperty(STATS_NODE_COUNT_PROPERTY, "0"));
        propsCount = Long.parseLong(repProps.getProperty(STATS_PROP_COUNT_PROPERTY, "0"));

        // get the system session for every defined workspace and
        // register as an event listener
        iter = wspInfos.values().iterator();
        while (iter.hasNext()) {
            String wspName = ((WorkspaceInfo) iter.next()).getName();
            Session s = getSystemSession(wspName);
            s.getWorkspace().getObservationManager().addEventListener(this,
                    Event.NODE_ADDED | Event.NODE_REMOVED
                    | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED,
                    "/", true, null, null, false);

            // register SearchManager as EventListener
            SearchManager searchMgr = getSearchManager(wspName);

            if (searchMgr != null) {
                s.getWorkspace().getObservationManager().addEventListener(searchMgr,
                        Event.NODE_ADDED | Event.NODE_REMOVED |
                        Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED |
                        Event.PROPERTY_CHANGED,
                        "/", true, null, null, false);
            }
        }

        // finally register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdown();
            }
        });
    }

    /**
     * Creates a new <code>RepositoryImpl</code> instance.
     * <p/>
     * todo prevent multiple instantiation from same configuration as this could lead to data corruption/loss
     *
     * @param config the configuration of the repository
     * @return a new <code>RepositoryImpl</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryImpl create(RepositoryConfig config)
            throws RepositoryException {
        return new RepositoryImpl(config);
    }

    RepositoryConfig getConfig() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return repConfig;
    }

    NamespaceRegistryImpl getNamespaceRegistry() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return nsReg;
    }

    NodeTypeRegistry getNodeTypeRegistry() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return ntReg;
    }

    PersistentVersionManager getPersistentVersionManager() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return vMgr;
    }

    String getRootNodeUUID() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return rootNodeUUID;
    }

    /**
     * Returns the names of all workspaces of this repository.
     *
     * @return the names of all workspaces of this repository.
     */
    protected String[] getWorkspaceNames() {
        return (String[]) wspInfos.keySet().toArray(new String[wspInfos.keySet().size()]);
    }

    /**
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws RepositoryException if a workspace with the given name
     *                             already exists or if another error occurs
     */
    public void createWorkspace(String workspaceName) throws RepositoryException {
        if (wspInfos.containsKey(workspaceName)) {
            throw new RepositoryException("workspace '" + workspaceName + "' already exists.");
        }

        // create the workspace configuration
        WorkspaceConfig config = repConfig.createWorkspaceConfig(workspaceName);
        WorkspaceInfo info = new WorkspaceInfo(config);
        wspInfos.put(workspaceName, info);
    }

    PersistentItemStateProvider getWorkspaceStateManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        return wspInfo.getItemStateProvider();
    }

    ReferenceManager getWorkspaceReferenceManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        return wspInfo.getReferenceManager();
    }

    ObservationManagerFactory getObservationManagerFactory(String workspaceName)
            throws NoSuchWorkspaceException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        return wspInfo.getObservationManagerFactory();
    }

    /**
     * Returns the {@link SearchManager} for the workspace with name
     * <code>workspaceName</code>.
     *
     * @param workspaceName the name of the workspace.
     * @return the <code>SearchManager</code> for the workspace, or
     *         <code>null</code> if the workspace does not have a
     *         <code>SearchManager</code> configured.
     * @throws NoSuchWorkspaceException if there is no workspace with name
     *                                  <code>workspaceName</code>.
     * @throws RepositoryException      if an error occurs while opening the
     *                                  search index.
     */
    SearchManager getSearchManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        return wspInfo.getSearchManager();
    }

    SystemSession getSystemSession(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        return wspInfo.getSystemSession();
    }

    /**
     * Shuts down this repository. Note that this method is called automatically
     * through a shutdown hook.
     *
     * @see Runtime#addShutdownHook(Thread)
     */
    public synchronized void shutdown() {
        // check state
        if (disposed) {
            // there's nothing to do here because the repository has already been shut down
            return;
        }

        // close active user sessions
        for (Iterator it = activeSessions.values().iterator(); it.hasNext();) {
            SessionImpl session = (SessionImpl) it.next();
            session.removeListener(this);
            session.logout();
        }
        activeSessions.clear();

        // shut down workspaces 
        for (Iterator it = wspInfos.values().iterator(); it.hasNext();) {
            WorkspaceInfo wspInfo = (WorkspaceInfo) it.next();
            wspInfo.dispose();
        }

        /**
         * todo further cleanup tasks, free resources, etc.
         */

        // persist repository properties
        try {
            storeRepProps();
        } catch (RepositoryException e) {
            log.error("failed to persist repository properties", e);
        }

        try {
            // close repository file system
            repStore.close();
        } catch (FileSystemException e) {
            log.error("error while closing repository filesystem", e);
        }

        // make sure this instance is not used anymore
        disposed = true;
    }

    private void loadRepProps() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(metaDataStore, PROPERTIES_RESOURCE);
        try {
            repProps.clear();
            if (!propFile.exists() || propFile.length() == 0) {
                // initialize properties with pre-defined values
                repProps.setProperty(SPEC_VERSION_PROPERTY, SPEC_VERSION);
                repProps.setProperty(SPEC_NAME_PROPERTY, SPEC_NAME);
                repProps.setProperty(REP_VENDOR_PROPERTY, REP_VENDOR);
                repProps.setProperty(REP_VENDOR_URL_PROPERTY, REP_VENDOR_URL);
                repProps.setProperty(REP_NAME_PROPERTY, REP_NAME);
                repProps.setProperty(REP_VERSION_PROPERTY, REP_VERSION);

                repProps.setProperty(STATS_NODE_COUNT_PROPERTY, Long.toString(nodesCount));
                repProps.setProperty(STATS_PROP_COUNT_PROPERTY, Long.toString(propsCount));

                // persist properties
                storeRepProps();
                return;
            }

            InputStream in = propFile.getInputStream();
            try {
                repProps.load(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            String msg = "failed to load repository properties";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    private void storeRepProps() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(metaDataStore, PROPERTIES_RESOURCE);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
            try {
                repProps.store(os, null);
            } finally {
                // make sure stream is closed
                os.close();
            }
        } catch (Exception e) {
            String msg = "failed to persist repository properties";
            log.error(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * @return
     */
    public Properties getProperties() {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return (Properties) repProps.clone();
    }

    /**
     * @param key
     * @return
     */
    public String getProperty(String key) {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return repProps.getProperty(key);
    }

    /**
     * Returns the system root node (i.e. /jcr:system)
     *
     * @param session
     * @return
     * @throws RepositoryException
     */
    public NodeImpl getSystemRootNode(SessionImpl session) throws RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        return ((NodeImpl) session.getRootNode()).getNode(SYSTEM_ROOT_NAME);
    }

    //-----------------------------------------------------------< Repository >
    /**
     * @see Repository#login(Credentials, String)
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        if (workspaceName == null) {
            workspaceName = repConfig.getDefaultWorkspaceName();
        }
        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        if (credentials == null) {
            // anonymous login
            SessionImpl ses = new XASessionImpl(this, ANONYMOUS_CREDENTIALS, wspInfo.getConfig(), txMgr);
            activeSessions.put(ses, ses);
            return ses;
        } else if (credentials instanceof SimpleCredentials) {
            // username/password credentials
            // @todo implement authentication/authorization
            Session ses = new XASessionImpl(this, credentials, wspInfo.getConfig(), txMgr);
            activeSessions.put(ses, ses);
            return ses;
        } else {
            String msg = "login failed: incompatible credentials";
            log.error(msg);
            throw new RepositoryException(msg);
        }
    }

    /**
     * @see Repository#login(String)
     */
    public Session login(String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    //------------------------------------------------------< SessionListener >
    /**
     * @see SessionListener#loggedOut(SessionImpl)
     */
    public void loggedOut(SessionImpl session) {
        // remove session from active sessions
        activeSessions.remove(session);
    }

    //--------------------------------------------------------< EventListener >
    /**
     * @see EventListener#onEvent(EventIterator)
     */
    public void onEvent(EventIterator events) {
        // check state
        if (disposed) {
            // ignore, repository instance has been shut down
            return;
        }

        synchronized (repProps) {
            while (events.hasNext()) {
                Event event = events.nextEvent();
                long type = event.getType();
                if ((type & Event.NODE_ADDED) == Event.NODE_ADDED) {
                    nodesCount++;
                    repProps.setProperty(STATS_NODE_COUNT_PROPERTY, Long.toString(nodesCount));
                }
                if ((type & Event.NODE_REMOVED) == Event.NODE_REMOVED) {
                    nodesCount--;
                    repProps.setProperty(STATS_NODE_COUNT_PROPERTY, Long.toString(nodesCount));
                }
                if ((type & Event.PROPERTY_ADDED) == Event.PROPERTY_ADDED) {
                    propsCount++;
                    repProps.setProperty(STATS_PROP_COUNT_PROPERTY, Long.toString(propsCount));
                }
                if ((type & Event.PROPERTY_REMOVED) == Event.PROPERTY_REMOVED) {
                    propsCount--;
                    repProps.setProperty(STATS_PROP_COUNT_PROPERTY, Long.toString(propsCount));
                }
            }
        }
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>WorkspaceInfo</code> holds the objects that are shared
     * among multiple per-session <code>WorkspaceImpl</code> instances
     * representing the same named workspace, i.e. the same physical
     * storage.
     */
    class WorkspaceInfo {

        /**
         * workspace configuration
         */
        private final WorkspaceConfig config;

        /**
         * persistence manager
         */
        private PersistenceManager persistMgr;

        /**
         * item state provider
         */
        private PersistentItemStateManager itemStateMgr;

        /**
         * reference manager
         */
        private ReferenceManager refMgr;

        /**
         * system session
         */
        private SystemSession systemSession;

        /**
         * observation manager factory
         */
        private ObservationManagerFactory obsMgrFactory;

        /**
         * search manager
         */
        private SearchManager searchMgr;

        /**
         * Creates a new <code>WorkspaceInfo</code> based on the given
         * <code>config</code>.
         *
         * @param config workspace configuration
         */
        WorkspaceInfo(WorkspaceConfig config) {
            this.config = config;
        }

        /**
         * Returns the workspace name
         *
         * @return the workspace name
         */
        String getName() {
            return config.getName();
        }

        /**
         * Returns the workspace file system
         *
         * @return the workspace file system
         */
        FileSystem getFileSystem() {
            return config.getFileSystem();
        }

        /**
         * Returns the workspace configuration
         *
         * @return the workspace configuration
         */
        WorkspaceConfig getConfig() {
            return config;
        }

        /**
         * Returns the workspace persistence manager
         *
         * @return the workspace persistence manager
         * @throws RepositoryException if the persistence manager could not be instantiated/initialized
         */
        synchronized PersistenceManager getPersistenceManager() throws RepositoryException {
            if (persistMgr == null) {
                PersistenceManagerConfig pmConfig = config.getPersistenceManagerConfig();
                String className = pmConfig.getClassName();
                Map params = pmConfig.getParameters();
                try {
                    Class c = Class.forName(className);
                    persistMgr = (PersistenceManager) c.newInstance();
                    /**
                     * set the properties of the persistence manager object
                     * from the param map
                     */
                    BeanMap bm = new BeanMap(persistMgr);
                    Iterator iter = params.keySet().iterator();
                    while (iter.hasNext()) {
                        Object name = iter.next();
                        Object value = params.get(name);
                        bm.put(name, value);
                    }
                    PMContext ctx = new PMContext(config, rootNodeUUID, nsReg, ntReg);
                    persistMgr.init(ctx);
                } catch (Exception e) {
                    persistMgr = null;
                    log.error("Cannot instantiate implementing class " + className, e);
                    throw new RepositoryException("Cannot instantiate implementing class " + className, e);
                }
            }
            return persistMgr;
        }

        /**
         * Returns the system session for this workspace
         *
         * @return the system session for this workspace
         * @throws RepositoryException if the system session could not be created
         */
        synchronized SystemSession getSystemSession() throws RepositoryException {
            if (systemSession == null) {
                systemSession = new SystemSession(RepositoryImpl.this, config);
            }
            return systemSession;
        }

        /**
         * Returns the reference manager for this workspace
         *
         * @return the reference manager for this workspace
         * @throws RepositoryException if the reference manager could not be created
         */
        synchronized ReferenceManager getReferenceManager()
                throws RepositoryException {
            if (refMgr == null) {
                // create reference mgr that uses this workspace's perstistence mgr
                refMgr = new ReferenceManager(getPersistenceManager());
            }
            return refMgr;
        }

        /**
         * Returns the workspace item state provider
         *
         * @return the workspace item state provider
         * @throws RepositoryException if the workspace item state provider could not be created
         */
        synchronized PersistentItemStateProvider getItemStateProvider()
                throws RepositoryException {
            if (itemStateMgr == null) {
                // create item state manager
                try {
                    itemStateMgr = new PersistentItemStateManager(getPersistenceManager(), rootNodeUUID, ntReg);
                } catch (ItemStateException ise) {
                    String msg = "failed to instantiate persistent item state manager";
                    log.error(msg, ise);
                    throw new RepositoryException(msg, ise);
                }
            }
            return itemStateMgr;
        }

        /**
         * Returns the observation manager factory for this workspace
         *
         * @return the observation manager factory for this workspace
         */
        synchronized ObservationManagerFactory getObservationManagerFactory() {
            if (obsMgrFactory == null) {
                obsMgrFactory = new ObservationManagerFactory();
            }
            return obsMgrFactory;
        }

        /**
         * Returns the search manager for this workspace
         *
         * @return the search manager for this workspace
         * @throws RepositoryException if the search manager could not be created
         */
        synchronized SearchManager getSearchManager() throws RepositoryException {
            if (searchMgr == null) {
                if (config.getSearchConfig() == null) {
                    // no search index configured
                    return null;
                }
                try {
                    searchMgr = new SearchManager(getSystemSession(), config.getSearchConfig());
                } catch (IOException ioe) {
                    String msg = "failed to instantiate search manager";
                    log.error(msg, ioe);
                    throw new RepositoryException(msg, ioe);
                }
            }
            return searchMgr;
        }

        /**
         * Disposes all objects this <code>WorkspaceInfo</code> is holding.
         */
        void dispose() {
            // dispose observation manager factory
            if (obsMgrFactory != null) {
                obsMgrFactory.dispose();
                obsMgrFactory = null;
            }

            // shutdown search managers
            if (searchMgr != null) {
                searchMgr.close();
                searchMgr = null;
            }

            // close system session
            if (systemSession != null) {
                systemSession.removeListener(RepositoryImpl.this);
                systemSession.logout();
                systemSession = null;
            }

            // dispose persistent item state mgr
            if (itemStateMgr != null) {
                itemStateMgr.dispose();
                itemStateMgr = null;
            }

            // close persistence manager
            if (persistMgr != null) {
                try {
                    persistMgr.close();
                } catch (Exception e) {
                    log.error("error while closing persistence manager of workspace " + config.getName(), e);
                }
                persistMgr = null;
            }

            // dispose reference manager
            if (refMgr != null) {
                //refMgr.dispose();
                refMgr = null;
            }

            // close workspace file system
            try {
                // close workspace file system
                config.getFileSystem().close();
            } catch (FileSystemException e) {
                log.error("error while closing filesystem of workspace " + config.getName(), e);
            }
        }
    }
}
