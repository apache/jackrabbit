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
import java.util.Properties;

/**
 * A <code>RepositoryImpl</code> ...
 */
public class RepositoryImpl implements Repository, EventListener {

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

    // map of workspace names and workspace configurations
    private final HashMap wspConfigs = new HashMap();

    // map of workspace names and workspace item state managers
    // (might be shared among multiple workspace instances representing
    // the same named workspace, i.e. the same physical storage)
    private final HashMap wspStateMgrs = new HashMap();

    // map of workspace names and workspace reference managers
    // (might be shared among multiple workspace instances representing
    // the same named workspace, i.e. the same physical storage)
    private final HashMap wspRefMgrs = new HashMap();

    // map of workspace names and observation managers
    private final HashMap wspObsMgrFactory = new HashMap();

    // map of search managers
    private final HashMap wspSearchMgrs = new HashMap();

    // map of workspace names and system sessions
    private final HashMap wspSystemSessions = new HashMap();

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
            wspConfigs.put(config.getName(), config);
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
        // (by now, we just create a system root node in all workspaces)
        Iterator wspNames = wspConfigs.keySet().iterator();
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
        iter = wspConfigs.values().iterator();
        while (iter.hasNext()) {
            String wspName = ((WorkspaceConfig) iter.next()).getName();
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
        return (String[]) wspConfigs.keySet().toArray(new String[wspConfigs.keySet().size()]);
    }

    /**
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws RepositoryException if a workspace with the given name
     *                             already exists or if another error occurs
     */
    public void createWorkspace(String workspaceName) throws RepositoryException {
        if (wspConfigs.containsKey(workspaceName)) {
            throw new RepositoryException("workspace '" + workspaceName + "' already exists.");
        }

        // create the workspace configuration
        repConfig.createWorkspaceConfig(workspaceName);
        // add new configuration to map of workspace configs
        wspConfigs.put(workspaceName, repConfig.getWorkspaceConfig(workspaceName));
    }

    synchronized PersistentItemStateProvider getWorkspaceStateManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceConfig wspConfig = (WorkspaceConfig) wspConfigs.get(workspaceName);
        if (wspConfig == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        // get/create per named workspace (i.e. per physical storage) item state manager
        PersistentItemStateProvider stateMgr =
                (PersistentItemStateProvider) wspStateMgrs.get(workspaceName);
        if (stateMgr == null) {
            // create state manager
            try {
                stateMgr = new PersistentItemStateManager(wspConfig.getPersistenceManager(), rootNodeUUID, ntReg);
            } catch (ItemStateException ise) {
                String msg = "failed to instantiate the persistent state manager";
                log.error(msg, ise);
                throw new RepositoryException(msg, ise);
            }
            wspStateMgrs.put(workspaceName, stateMgr);
        }
        return stateMgr;
    }

    synchronized ReferenceManager getWorkspaceReferenceManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceConfig wspConfig = (WorkspaceConfig) wspConfigs.get(workspaceName);
        if (wspConfig == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        ReferenceManager refMgr
                = (ReferenceManager) wspRefMgrs.get(workspaceName);
        if (refMgr == null) {
            // create reference mgr that uses the perstistence mgr configured
            // in the workspace definition
            refMgr = new ReferenceManager(wspConfig.getPersistenceManager());
            wspRefMgrs.put(workspaceName, refMgr);
        }
        return refMgr;
    }

    synchronized ObservationManagerFactory getObservationManagerFactory(String workspaceName)
            throws NoSuchWorkspaceException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        if (!wspConfigs.containsKey(workspaceName)) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        ObservationManagerFactory obsMgr
                = (ObservationManagerFactory) wspObsMgrFactory.get(workspaceName);
        if (obsMgr == null) {
            obsMgr = new ObservationManagerFactory();
            wspObsMgrFactory.put(workspaceName, obsMgr);
        }
        return obsMgr;
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
    synchronized SearchManager getSearchManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceConfig wspConfig = (WorkspaceConfig) wspConfigs.get(workspaceName);
        SearchManager searchMgr
                = (SearchManager) wspSearchMgrs.get(workspaceName);
        if (searchMgr == null) {
            try {
                if (wspConfig.getSearchConfig() == null) {
                    // no search index configured
                    return null;
                }
                SystemSession s = getSystemSession(workspaceName);
                searchMgr = new SearchManager(s, wspConfig.getSearchConfig());
            } catch (IOException e) {
                throw new RepositoryException("Exception opening search index.", e);
            }
            wspSearchMgrs.put(workspaceName, searchMgr);
        }
        return searchMgr;
    }

    synchronized SystemSession getSystemSession(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check state
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }

        WorkspaceConfig wspConfig = (WorkspaceConfig) wspConfigs.get(workspaceName);
        if (wspConfig == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        SystemSession systemSession
                = (SystemSession) wspSystemSessions.get(workspaceName);
        if (systemSession == null) {
            systemSession = new SystemSession(this, wspConfig);
            wspSystemSessions.put(workspaceName, systemSession);
        }
        return systemSession;
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

        // stop / dispose all ObservationManagers
        for (Iterator it = wspObsMgrFactory.values().iterator(); it.hasNext();) {
            ObservationManagerFactory obsMgr = (ObservationManagerFactory) it.next();
            obsMgr.dispose();
        }

        // shutdown search managers
        for (Iterator it = wspSearchMgrs.values().iterator(); it.hasNext();) {
            SearchManager searchMgr = (SearchManager) it.next();
            searchMgr.close();
        }

        /**
         * todo close sessions, close item state mgr's, free resources, etc.
         */

        for (Iterator it = wspConfigs.values().iterator(); it.hasNext();) {
            WorkspaceConfig wspConfig = (WorkspaceConfig) it.next();
            try {
                // close persistence manager
                wspConfig.getPersistenceManager().close();
            } catch (Exception e) {
                log.error("Error while closing persistence manager of workspace " + wspConfig.getName(), e);
            }
            try {
                // close workspace file system
                wspConfig.getFileSystem().close();
            } catch (FileSystemException e) {
                log.error("Error while closing filesystem of workspace " + wspConfig.getName(), e);
            }
        }

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
            log.error("Error while closing repository filesystem", e);
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
        WorkspaceConfig wspConfig = (WorkspaceConfig) wspConfigs.get(workspaceName);
        if (wspConfig == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }
        if (credentials == null) {
            // anonymous login
            return new XASessionImpl(this, ANONYMOUS_CREDENTIALS, wspConfig, txMgr);
        } else if (credentials instanceof SimpleCredentials) {
            // username/password credentials
            // @todo implement authentication/authorization
            return new XASessionImpl(this, credentials, wspConfig, txMgr);
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

    //--------------------------------------------------------< EventListener >
    /**
     * @see EventListener#onEvent(EventIterator)
     */
    public synchronized void onEvent(EventIterator events) {
        // check state
        if (disposed) {
            // ignore, repository instance has been shut down
            return;
        }

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
