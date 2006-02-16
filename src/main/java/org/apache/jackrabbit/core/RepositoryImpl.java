/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.config.FileSystemConfig;
import org.apache.jackrabbit.core.fs.BasedFileSystem;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.fs.FileSystemException;
import org.apache.jackrabbit.core.fs.FileSystemResource;
import org.apache.jackrabbit.core.lock.LockManager;
import org.apache.jackrabbit.core.lock.LockManagerImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeImpl;
import org.apache.jackrabbit.core.nodetype.NodeTypeRegistry;
import org.apache.jackrabbit.core.nodetype.virtual.VirtualNodeTypeStateManager;
import org.apache.jackrabbit.core.observation.DelegatingObservationDispatcher;
import org.apache.jackrabbit.core.observation.ObservationManagerFactory;
import org.apache.jackrabbit.core.security.AuthContext;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.core.state.SharedItemStateManager;
import org.apache.jackrabbit.core.version.VersionManager;
import org.apache.jackrabbit.core.version.VersionManagerImpl;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;

/**
 * A <code>RepositoryImpl</code> ...
 */
public class RepositoryImpl implements JackrabbitRepository, SessionListener,
        EventListener {

    private static Logger log = Logger.getLogger(RepositoryImpl.class);

    /**
     * repository home lock
     */
    private static final String REPOSITORY_LOCK = ".lock";

    /**
     * hardcoded id of the repository root node
     */
    public static final NodeId ROOT_NODE_ID = NodeId.valueOf("cafebabe-cafe-babe-cafe-babecafebabe");

    /**
     * hardcoded id of the "/jcr:system" node
     */
    public static final NodeId SYSTEM_ROOT_NODE_ID = NodeId.valueOf("deadbeef-cafe-babe-cafe-babecafebabe");

    /**
     * hardcoded id of the "/jcr:system/jcr:versionStorage" node
     */
    public static final NodeId VERSION_STORAGE_NODE_ID = NodeId.valueOf("deadbeef-face-babe-cafe-babecafebabe");

    /**
     * hardcoded id of the "/jcr:system/jcr:nodeTypes" node
     */
    public static final NodeId NODETYPES_NODE_ID = NodeId.valueOf("deadbeef-cafe-cafe-cafe-babecafebabe");

    /**
     * the name of the filesystem resource containing the properties of the
     * repository.
     */
    private static final String PROPERTIES_RESOURCE = "rep.properties";

    /**
     * the repository properties.
     */
    private final Properties repProps;

    // names of well-known repository properties
    public static final String STATS_NODE_COUNT_PROPERTY = "jcr.repository.stats.nodes.count";
    public static final String STATS_PROP_COUNT_PROPERTY = "jcr.repository.stats.properties.count";

    private NodeId rootNodeId;

    private final NamespaceRegistryImpl nsReg;
    private final NodeTypeRegistry ntReg;
    private final VersionManager vMgr;
    private final VirtualNodeTypeStateManager virtNTMgr;

    /**
     * Search manager for the jcr:system tree. May be <code>null</code> if
     * none is configured.
     */
    private SearchManager systemSearchMgr;

    // configuration of the repository
    protected final RepositoryConfig repConfig;

    // the master filesystem
    private final FileSystem repStore;

    // sub file system where the repository stores meta data such as uuid of root node, etc.
    private final FileSystem metaDataStore;

    /**
     * the delegating observation dispatcher for all workspaces
     */
    private final DelegatingObservationDispatcher delegatingDispatcher =
            new DelegatingObservationDispatcher();

    /**
     * map of workspace names and <code>WorkspaceInfo<code>s.
     */
    private final HashMap wspInfos = new HashMap();

    /**
     * active sessions (weak references)
     */
    private final ReferenceMap activeSessions =
            new ReferenceMap(ReferenceMap.WEAK, ReferenceMap.WEAK);

    /**
     * workspace janitor thread that is responsible for temporarily
     * shutting down workspaces that have been idle for a specific
     * amount of time
     */
    private Thread wspJanitor;

    // misc. statistics
    private long nodesCount = 0;
    private long propsCount = 0;

    // flag indicating if respository has been shut down
    private boolean disposed = false;

    /**
     * the lock that guards instantiation of multiple repositories.
     */
    private FileLock repLock;

    /**
     * private constructor
     *
     * @param repConfig
     */
    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {

        log.info("Starting repository...");

        this.repConfig = repConfig;

        acquireRepositoryLock() ;

        // setup file systems
        repStore = repConfig.getFileSystem();
        String fsRootPath = "/meta";
        try {
            if (!repStore.exists(fsRootPath) || !repStore.isFolder(fsRootPath)) {
                repStore.createFolder(fsRootPath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to create folder for repository meta data";
            log.fatal(msg, fse);
            throw new RepositoryException(msg, fse);
        }
        metaDataStore = new BasedFileSystem(repStore, fsRootPath);

        // init root node uuid
        rootNodeId = loadRootNodeId(metaDataStore);

        // load repository properties
        repProps = loadRepProps();
        nodesCount = Long.parseLong(repProps.getProperty(STATS_NODE_COUNT_PROPERTY, "0"));
        propsCount = Long.parseLong(repProps.getProperty(STATS_PROP_COUNT_PROPERTY, "0"));

        // create registries
        nsReg = createNamespaceRegistry(new BasedFileSystem(repStore, "/namespaces"));
        ntReg = createNodeTypeRegistry(nsReg, new BasedFileSystem(repStore, "/nodetypes"));

        // init workspace configs
        Iterator iter = repConfig.getWorkspaceConfigs().iterator();
        while (iter.hasNext()) {
            WorkspaceConfig config = (WorkspaceConfig) iter.next();
            WorkspaceInfo info = createWorkspaceInfo(config);
            wspInfos.put(config.getName(), info);
        }

        // init version manager
        vMgr = createVersionManager(repConfig.getVersioningConfig(),
                delegatingDispatcher);

        // init virtual node type manager
        virtNTMgr = new VirtualNodeTypeStateManager(getNodeTypeRegistry(),
                delegatingDispatcher, NODETYPES_NODE_ID, SYSTEM_ROOT_NODE_ID);

        // initialize default workspace
        String wspName = repConfig.getDefaultWorkspaceName();
        try {
            initWorkspace((WorkspaceInfo) wspInfos.get(wspName));
        } catch (RepositoryException e) {
            // if default workspace failed to initialize, shutdown again
            log.fatal("Failed to initialize workspace '" + wspName + "'", e);
            log.fatal("Unable to start repository, forcing shutdown...");
            shutdown();
            throw e;
        }

        // amount of time in seconds before an idle workspace is automatically
        // shut down
        int maxIdleTime = repConfig.getWorkspaceMaxIdleTime();
        if (maxIdleTime != 0) {
            // start workspace janitor thread
            wspJanitor = new WorkspaceJanitor(maxIdleTime * 1000);
            wspJanitor.start();
        }

        // after the workspace is initialized we pass a system session to
        // the virtual node type manager

        // todo FIXME it seems odd that the *global* virtual node type manager
        // is using a session that is bound to a single specific workspace
        virtNTMgr.setSession(getSystemSession(repConfig.getDefaultWorkspaceName()));

        log.info("Repository started");
    }

    /**
     * Creates the version manager.
     *
     * @param vConfig the versioning config
     * @return the newly created version manager
     * @throws RepositoryException if an error occurrs
     */
    protected VersionManager createVersionManager(VersioningConfig vConfig,
                                                  DelegatingObservationDispatcher delegatingDispatcher)
            throws RepositoryException {
        PersistenceManager pm = createPersistenceManager(vConfig.getHomeDir(),
                vConfig.getFileSystem(),
                vConfig.getPersistenceManagerConfig(),
                rootNodeId,
                nsReg,
                ntReg);
        return new VersionManagerImpl(pm, ntReg, delegatingDispatcher,
                VERSION_STORAGE_NODE_ID, SYSTEM_ROOT_NODE_ID);
    }

    /**
     * Lock the repository home.
     *
     * @throws RepositoryException if the repository lock can not be acquired
     */
    protected void acquireRepositoryLock() throws RepositoryException {
        File home = new File(this.repConfig.getHomeDir());
        File lock = new File(home, REPOSITORY_LOCK);

        if (lock.exists()) {
            log.warn("Existing lock file at " + lock.getAbsolutePath() +
                    " deteteced. Repository was not shut down properly.");
        } else {
            try {
                lock.createNewFile();
            } catch (IOException e) {
                throw new RepositoryException(
                    "Unable to create lock file at " + lock.getAbsolutePath(), e);
            }
        }
        try {
            repLock = new RandomAccessFile(lock, "rw").getChannel().tryLock();
        } catch (IOException e) {
            throw new RepositoryException(
                "Unable to lock file at " + lock.getAbsolutePath(), e);
        }
        if (repLock == null) {
            throw new RepositoryException(
                    "The repository home at " + home.getAbsolutePath() +
                    " appears to be in use since the file at " +
                    lock.getAbsolutePath() + " is locked by another process.");
        }
    }

    /**
     * Release repository lock
     */
    protected void releaseRepositoryLock() {
        if (repLock != null) {
            try {
                FileChannel channel = repLock.channel();
                repLock.release();
                channel.close();
            } catch (IOException e) {
                // ignore
            }
        }
        repLock = null;

        File home = new File(this.repConfig.getHomeDir());
        File lock = new File(home, REPOSITORY_LOCK);
        if (!lock.delete()) {
            log.error("Unable to release repository lock");
        }
    }

    /**
     * Returns the root node uuid.
     * @param fs
     * @return
     * @throws RepositoryException
     */
    protected NodeId loadRootNodeId(FileSystem fs) throws RepositoryException {
        FileSystemResource uuidFile = new FileSystemResource(fs, "rootUUID");
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
                   rootNodeUUID = new UUID(bytes).toString();            // uuid is stored in binary format (16 bytes)
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
                    return NodeId.valueOf(new String(chars));
                } catch (Exception e) {
                    String msg = "failed to load persisted repository state";
                    log.debug(msg);
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
                        writer.write(ROOT_NODE_ID.toString());
                    } finally {
                        try {
                            writer.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                    return ROOT_NODE_ID;
                } catch (Exception e) {
                    String msg = "failed to persist repository state";
                    log.debug(msg);
                    throw new RepositoryException(msg, e);
                }
            }
        } catch (FileSystemException fse) {
            String msg = "failed to access repository state";
            log.debug(msg);
            throw new RepositoryException(msg, fse);
        }
    }

    /**
     * Creates the <code>NamespaceRegistry</code> instance.
     *
     * @param fs
     * @return
     * @throws RepositoryException
     */
    protected NamespaceRegistryImpl createNamespaceRegistry(FileSystem fs)
            throws RepositoryException {
        return new NamespaceRegistryImpl(fs);
    }

    /**
     * Creates the <code>NodeTypeRegistry</code> instance.
     *
     * @param fs
     * @return
     * @throws RepositoryException
     */
    protected NodeTypeRegistry createNodeTypeRegistry(NamespaceRegistry nsReg,
                                                      FileSystem fs)
            throws RepositoryException {
        return NodeTypeRegistry.create(nsReg, fs);
    }

    /**
     * Creates a new <code>RepositoryImpl</code> instance.
     * <p/>
     *
     * @param config the configuration of the repository
     * @return a new <code>RepositoryImpl</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryImpl create(RepositoryConfig config)
            throws RepositoryException {
        return new RepositoryImpl(config);
    }

    /**
     * Performs a sanity check on this repository instance.
     *
     * @throws IllegalStateException if this repository has been rendered
     *                               invalid for some reason (e.g. if it has
     *                               been shut down)
     */
    protected void sanityCheck() throws IllegalStateException {
        // check repository status
        if (disposed) {
            throw new IllegalStateException("repository instance has been shut down");
        }
    }

    private void initWorkspace(WorkspaceInfo wspInfo) throws RepositoryException {
        // first initialize workspace info
        wspInfo.initialize();
        // get system session and Workspace instance
        SessionImpl sysSession = wspInfo.getSystemSession();
        WorkspaceImpl wsp = (WorkspaceImpl) sysSession.getWorkspace();

        /**
         * todo implement 'System' workspace
         * FIXME
         * - the should be one 'System' workspace per repository
         * - the 'System' workspace should have the /jcr:system node
         * - versions, version history and node types should be reflected in
         *   this system workspace as content under /jcr:system
         * - all other workspaces should be dynamic workspaces based on
         *   this 'read-only' system workspace
         *
         * for now, we just create a /jcr:system node in every workspace
         */
        NodeImpl rootNode = (NodeImpl) sysSession.getRootNode();
        if (!rootNode.hasNode(QName.JCR_SYSTEM)) {
            NodeTypeImpl nt = sysSession.getNodeTypeManager().getNodeType(QName.REP_SYSTEM);
            NodeImpl sysRoot = rootNode.internalAddChildNode(QName.JCR_SYSTEM, nt, SYSTEM_ROOT_NODE_ID);
            // add version storage
            nt = sysSession.getNodeTypeManager().getNodeType(QName.REP_VERSIONSTORAGE);
            sysRoot.internalAddChildNode(QName.JCR_VERSIONSTORAGE, nt, VERSION_STORAGE_NODE_ID);
            // add node types
            nt = sysSession.getNodeTypeManager().getNodeType(QName.REP_NODETYPES);
            sysRoot.internalAddChildNode(QName.JCR_NODETYPES, nt, NODETYPES_NODE_ID);
            rootNode.save();
        }

        // register the repository as event listener for keeping repository statistics
        wsp.getObservationManager().addEventListener(this,
                Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED,
                "/", true, null, null, false);

        // register SearchManager as event listener
        SearchManager searchMgr = wspInfo.getSearchManager();
        if (searchMgr != null) {
            wsp.getObservationManager().addEventListener(searchMgr,
                    Event.NODE_ADDED | Event.NODE_REMOVED
                    | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
                    | Event.PROPERTY_CHANGED,
                    "/", true, null, null, false);
        }

        // register the observation factory of that workspace
        delegatingDispatcher.addDispatcher(wspInfo.getObservationManagerFactory());
    }

    /**
     * Returns the system search manager or <code>null</code> if none is
     * configured.
     */
    private SearchManager getSystemSearchManager(String wspName)
            throws RepositoryException {
        if (systemSearchMgr == null) {
            try {
                if (repConfig.getSearchConfig() != null) {
                    SystemSession defSysSession = getSystemSession(wspName);
                    systemSearchMgr = new SystemSearchManager(repConfig.getSearchConfig(),
                            nsReg, ntReg, defSysSession.getItemStateManager(), SYSTEM_ROOT_NODE_ID);
                    ObservationManager obsMgr = defSysSession.getWorkspace().getObservationManager();
                    obsMgr.addEventListener(systemSearchMgr, Event.NODE_ADDED |
                            Event.NODE_REMOVED | Event.PROPERTY_ADDED |
                            Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                            "/" + QName.JCR_SYSTEM.toJCRName(defSysSession.getNamespaceResolver()),
                            true, null, null, false);
                } else {
                    systemSearchMgr = null;
                }
            } catch (NoPrefixDeclaredException e) {
                throw new RepositoryException(e);
            }
        }
        return systemSearchMgr;
    }

    NamespaceRegistryImpl getNamespaceRegistry() {
        // check sanity of this instance
        sanityCheck();

        return nsReg;
    }

    NodeTypeRegistry getNodeTypeRegistry() {
        // check sanity of this instance
        sanityCheck();

        return ntReg;
    }

    VersionManager getVersionManager() {
        // check sanity of this instance
        sanityCheck();

        return vMgr;
    }

    NodeId getRootNodeId() {
        // check sanity of this instance
        sanityCheck();

        return rootNodeId;
    }

    /**
     * Returns the names of <i>all</i> workspaces in this repository.
     *
     * @return the names of all workspaces in this repository.
     * @see javax.jcr.Workspace#getAccessibleWorkspaceNames()
     */
    String[] getWorkspaceNames() {
        return (String[]) wspInfos.keySet().toArray(new String[wspInfos.keySet().size()]);
    }

    /**
     * Returns the {@link WorkspaceInfo} for the named workspace.
     *
     * @param workspaceName The name of the workspace whose {@link WorkspaceInfo}
     *                      is to be returned. This must not be <code>null</code>.
     * @return The {@link WorkspaceInfo} for the named workspace. This will
     *         never be <code>null</code>.
     * @throws IllegalStateException    If this repository has already been
     *                                  shut down.
     * @throws NoSuchWorkspaceException If the named workspace does not exist.
     */
    protected WorkspaceInfo getWorkspaceInfo(String workspaceName)
            throws IllegalStateException, NoSuchWorkspaceException {
        // check sanity of this instance
        sanityCheck();

        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(workspaceName);
        if (wspInfo == null) {
            throw new NoSuchWorkspaceException(workspaceName);
        }

        synchronized (wspInfo) {
            if (!wspInfo.isInitialized()) {
                try {
                    initWorkspace(wspInfo);
                } catch (RepositoryException e) {
                    log.error("Unable to initialize workspace '" + workspaceName + "'", e);
                    throw new NoSuchWorkspaceException(workspaceName);
                }
            }
        }
        return wspInfo;
    }

    /**
     * Creates a workspace with the given name.
     *
     * @param workspaceName name of the new workspace
     * @throws RepositoryException if a workspace with the given name
     *                             already exists or if another error occurs
     * @see SessionImpl#createWorkspace(String)
     */
    protected synchronized void createWorkspace(String workspaceName)
            throws RepositoryException {
        if (wspInfos.containsKey(workspaceName)) {
            throw new RepositoryException("workspace '"
                    + workspaceName + "' already exists.");
        }

        // create the workspace configuration
        WorkspaceConfig config = repConfig.createWorkspaceConfig(workspaceName);
        WorkspaceInfo info = createWorkspaceInfo(config);
        wspInfos.put(workspaceName, info);
    }

    SharedItemStateManager getWorkspaceStateManager(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check sanity of this instance
        sanityCheck();

        return getWorkspaceInfo(workspaceName).getItemStateProvider();
    }

    ObservationManagerFactory getObservationManagerFactory(String workspaceName)
            throws NoSuchWorkspaceException {
        // check sanity of this instance
        sanityCheck();

        return getWorkspaceInfo(workspaceName).getObservationManagerFactory();
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
        // check sanity of this instance
        sanityCheck();

        return getWorkspaceInfo(workspaceName).getSearchManager();
    }

    /**
     * Returns the {@link LockManager} for the workspace with name
     * <code>workspaceName</code>
     *
     * @param workspaceName workspace name
     * @return <code>LockManager</code> for the workspace
     * @throws NoSuchWorkspaceException if such a workspace does not exist
     * @throws RepositoryException      if some other error occurs
     */
    LockManager getLockManager(String workspaceName) throws
            NoSuchWorkspaceException, RepositoryException {
        // check sanity of this instance
        sanityCheck();

        return getWorkspaceInfo(workspaceName).getLockManager();
    }

    /**
     * Returns the {@link SystemSession} for the workspace with name
     * <code>workspaceName</code>
     *
     * @param workspaceName workspace name
     * @return system session of the specified workspace
     * @throws NoSuchWorkspaceException if such a workspace does not exist
     * @throws RepositoryException      if some other error occurs
     */
    SystemSession getSystemSession(String workspaceName)
            throws NoSuchWorkspaceException, RepositoryException {
        // check sanity of this instance
        sanityCheck();

        return getWorkspaceInfo(workspaceName).getSystemSession();
    }

    /**
     * Creates a new repository session on the specified workspace for the
     * <b><i>authenticated</i></b> subject of the given login context and
     * adds it to the <i>active</i> sessions.
     * <p/>
     * Calls {@link #createSessionInstance(AuthContext, WorkspaceConfig)} to
     * create the actual <code>SessionImpl</code> instance.
     *
    * @param loginContext  login context with authenticated subject
     * @param workspaceName workspace name
     * @return a new session
     * @throws NoSuchWorkspaceException if the specified workspace does not exist
     * @throws AccessDeniedException    if the subject of the given login context
     *                                  is not granted access to the specified
     *                                  workspace
     * @throws RepositoryException      if another error occurs
     */
    protected synchronized final SessionImpl createSession(AuthContext loginContext,
                              String workspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            RepositoryException {
        WorkspaceInfo wspInfo = getWorkspaceInfo(workspaceName);
        SessionImpl ses = createSessionInstance(loginContext, wspInfo.getConfig());
        ses.addListener(this);
        activeSessions.put(ses, ses);
        // reset idle timestamp
        wspInfo.setIdleTimestamp(0);
        return ses;
    }

    /**
     * Creates a new repository session on the specified workspace for the given
     * <b><i>authenticated</i></b> subject and adds it to the <i>active</i>
     * sessions.
     * <p/>
     * Calls {@link #createSessionInstance(Subject, WorkspaceConfig)} to
     * create the actual <code>SessionImpl</code> instance.
     *
     * @param subject       authenticated subject
     * @param workspaceName workspace name
     * @return a new session
     * @throws NoSuchWorkspaceException if the specified workspace does not exist
     * @throws AccessDeniedException    if the subject of the given login context
     *                                  is not granted access to the specified
     *                                  workspace
     * @throws RepositoryException      if another error occurs
     */
    protected synchronized final SessionImpl createSession(Subject subject,
                                              String workspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            RepositoryException {
        WorkspaceInfo wspInfo = getWorkspaceInfo(workspaceName);
        SessionImpl ses = createSessionInstance(subject, wspInfo.getConfig());
        ses.addListener(this);
        activeSessions.put(ses, ses);
        // reset idle timestamp
        wspInfo.setIdleTimestamp(0);
        return ses;
    }

    //-------------------------------------------------< JackrabbitRepository >
    /**
     * Shuts down this repository.
     */
    public synchronized void shutdown() {
        // check status of this instance
        if (disposed) {
            // there's nothing to do here because the repository has already been shut down
            return;
        }

        log.info("Shutting down repository...");

        // close active user sessions
        while (!activeSessions.isEmpty()) {
           ((Session) activeSessions.values().iterator().next()).logout();
       }

        // shut down workspaces
        for (Iterator it = wspInfos.values().iterator(); it.hasNext();) {
            WorkspaceInfo wspInfo = (WorkspaceInfo) it.next();
            synchronized(wspInfo) {
                if (wspInfo.isInitialized()) {
                    wspInfo.dispose();
                }
            }
        }

        // shutdown system search manager if there is one
        if (systemSearchMgr != null) {
            systemSearchMgr.close();
        }

        try {
            vMgr.close();
        } catch (Exception e) {
            log.error("Error while closing Version Manager.", e);
        }

        // persist repository properties
        try {
            storeRepProps(repProps);
        } catch (RepositoryException e) {
            log.error("failed to persist repository properties", e);
        }

        try {
            // close repository file system
            repStore.close();
        } catch (FileSystemException e) {
            log.error("error while closing repository filesystem", e);
        }

        try {
            // close versioning file system
            repConfig.getVersioningConfig().getFileSystem().close();
        } catch (FileSystemException e) {
            log.error("error while closing versioning filesystem", e);
        }

        // make sure this instance is not used anymore
        disposed = true;

        if (wspJanitor != null) {
            wspJanitor.interrupt();
            wspJanitor = null;
        }

        // finally release repository lock
        releaseRepositoryLock();

        log.info("Repository has been shutdown");
    }

    /**
     * Returns the configuration of this repository.
     * @return repository configuration
     */
    public RepositoryConfig getConfig() {
        return repConfig;
    }

    /**
     * Sets the default properties of the repository.
     * <p/>
     * This method loads the <code>Properties</code> from the
     * <code>org/apache/jackrabbit/core/repository.properties</code> resource
     * found in the class path and (re)sets the statistics properties, if not
     * present.
     *
     * @param props the properties object to load
     *
     * @throws RepositoryException if the properties can not be loaded
     */
    protected void setDefaultRepositoryProperties(Properties props)
            throws RepositoryException {
        InputStream in = RepositoryImpl.class.getResourceAsStream("repository.properties");
        try {
            props.load(in);
            in.close();

            // set counts
            if (!props.containsKey(STATS_NODE_COUNT_PROPERTY)) {
                props.setProperty(STATS_NODE_COUNT_PROPERTY, Long.toString(nodesCount));
            }
            if (!props.containsKey(STATS_PROP_COUNT_PROPERTY)) {
                props.setProperty(STATS_PROP_COUNT_PROPERTY, Long.toString(propsCount));
            }
        } catch (IOException e) {
            String msg = "Failed to load repository properties: " +e.toString();
            log.error(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Loads the repository properties by executing the following steps:
     * <ul>
     * <li> if the {@link #PROPERTIES_RESOURCE} exists in the meta data store,
     * the properties are loaded from that resource.</li>
     * <li> {@link #setDefaultRepositoryProperties(Properties)} is called
     * afterwards in order to initialize/update the repository properties
     * since some default properties might have changed and need updating.</li>
     * <li> finally {@link #storeRepProps(Properties)} is called in order to
     * persist the newly generated properties.</li>
     * </ul>
     *
     * @return the newly loaded/initialized repository properties
     *
     * @throws RepositoryException
     */
    protected Properties loadRepProps() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(metaDataStore, PROPERTIES_RESOURCE);
        try {
            Properties props = new Properties();
            if (propFile.exists()) {
                InputStream in = propFile.getInputStream();
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
            }
            // now set the default props
            setDefaultRepositoryProperties(props);

            // and store
            storeRepProps(props);

            return props;

        } catch (Exception e) {
            String msg = "failed to load repository properties";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Stores the properties to a persistent resource in the meta filesytem.
     *
     * @throws RepositoryException
     */
    protected void storeRepProps(Properties props) throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(metaDataStore, PROPERTIES_RESOURCE);
        try {
            propFile.makeParentDirs();
            OutputStream os = propFile.getOutputStream();
            try {
                props.store(os, null);
            } finally {
                // make sure stream is closed
                os.close();
            }
        } catch (Exception e) {
            String msg = "failed to persist repository properties";
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Creates a workspace persistence manager based on the given
     * configuration. The persistence manager is instantiated using
     * information in the given persistence manager configuration and
     * initialized with a persistence manager context containing the other
     * arguments.
     *
     * @return the created workspace persistence manager
     * @throws RepositoryException if the persistence manager could
     *                             not be instantiated/initialized
     */
    private static PersistenceManager createPersistenceManager(File homeDir,
                                                               FileSystem fs,
                                                               PersistenceManagerConfig pmConfig,
                                                               NodeId rootNodeId,
                                                               NamespaceRegistry nsReg,
                                                               NodeTypeRegistry ntReg)
            throws RepositoryException {
        try {
            PersistenceManager pm = (PersistenceManager) pmConfig.newInstance();
            pm.init(new PMContext(homeDir, fs, rootNodeId, nsReg, ntReg));
            return pm;
        } catch (Exception e) {
            String msg = "Cannot instantiate persistence manager " + pmConfig.getClassName();
            throw new RepositoryException(msg, e);
        }
    }

    //-----------------------------------------------------------< Repository >
    /**
     * {@inheritDoc}
     */
    public Session login(Credentials credentials, String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        // check sanity of this instance
        sanityCheck();

        if (workspaceName == null) {
            workspaceName = repConfig.getDefaultWorkspaceName();
        }

        // check if workspace exists (will throw NoSuchWorkspaceException if not)
        getWorkspaceInfo(workspaceName);

        if (credentials == null) {
            // null credentials, obtain the identity of the already-authenticated
            // subject from access control context
            AccessControlContext acc = AccessController.getContext();
            Subject subject;
            try {
                subject = Subject.getSubject(acc);
            } catch (SecurityException se) {
                throw new LoginException(se.getMessage());
            }
            if (subject == null) {
                throw new LoginException("No Subject associated with AccessControlContext");
            }
            // create session
            try {
                return createSession(subject, workspaceName);
            } catch (AccessDeniedException ade) {
                // authenticated subject is not authorized for the specified workspace
                throw new LoginException(ade.getMessage());
            }
        }

        // login either using JAAS or our own LoginModule
        AuthContext authCtx;
        try {
            LoginModuleConfig lmc = repConfig.getLoginModuleConfig();
            if (lmc == null) {
                authCtx = new AuthContext.JAAS(repConfig.getAppName(), credentials);
            } else {
                authCtx = new AuthContext.Local(
                        lmc.getLoginModule(), lmc.getParameters(), credentials);
            }
            authCtx.login();
        } catch (javax.security.auth.login.LoginException le) {
            throw new LoginException(le.getMessage());
        }

        // create session
        try {
            return createSession(authCtx, workspaceName);
        } catch (AccessDeniedException ade) {
            // authenticated subject is not authorized for the specified workspace
            throw new LoginException(ade.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Session login(String workspaceName)
            throws LoginException, NoSuchWorkspaceException, RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * {@inheritDoc}
     */
    public Session login() throws LoginException, RepositoryException {
        return login(null, null);
    }

    /**
     * {@inheritDoc}
     */
    public Session login(Credentials credentials)
            throws LoginException, RepositoryException {
        return login(credentials, null);
    }

    /**
     * {@inheritDoc}
     */
    public String getDescriptor(String key) {
        return repProps.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getDescriptorKeys() {
        String[] keys = (String[]) repProps.keySet().toArray(new String[repProps.keySet().size()]);
        Arrays.sort(keys);
        return keys;
    }

    //------------------------------------------------------< SessionListener >
    /**
     * {@inheritDoc}
     */
    public void loggingOut(SessionImpl session) {
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void loggedOut(SessionImpl session) {
        // remove session from active sessions
        activeSessions.remove(session);
    }

    //--------------------------------------------------------< EventListener >
    /**
     * {@inheritDoc}
     */
    public void onEvent(EventIterator events) {
        // check status of this instance
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

    //------------------------------------------< overridable factory methods >
    /**
     * Creates an instance of the {@link SessionImpl} class representing a
     * user authenticated by the <code>loginContext</code> instance attached
     * to the workspace configured by the <code>wspConfig</code>.
     *
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   If any other error occurrs creating the
     *                               session.
     */
    protected SessionImpl createSessionInstance(AuthContext loginContext,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(this, loginContext, wspConfig);
    }

    /**
     * Creates an instance of the {@link SessionImpl} class representing a
     * user represented by the <code>subject</code> instance attached
     * to the workspace configured by the <code>wspConfig</code>.
     *
     * @throws AccessDeniedException if the subject of the given login context
     *                               is not granted access to the specified
     *                               workspace
     * @throws RepositoryException   If any other error occurrs creating the
     *                               session.
     */
    protected SessionImpl createSessionInstance(Subject subject,
                                                WorkspaceConfig wspConfig)
            throws AccessDeniedException, RepositoryException {

        return new XASessionImpl(this, subject, wspConfig);
    }

    /**
     * Creates a new {@link RepositoryImpl.WorkspaceInfo} instance for
     * <code>wspConfig</code>.
     *
     * @param wspConfig the workspace configuration.
     * @return a new <code>WorkspaceInfo</code> instance.
     */
    protected WorkspaceInfo createWorkspaceInfo(WorkspaceConfig wspConfig) {
        return new WorkspaceInfo(wspConfig);
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>WorkspaceInfo</code> holds the objects that are shared
     * among multiple per-session <code>WorkspaceImpl</code> instances
     * representing the same named workspace, i.e. the same physical
     * storage.
     */
    protected class WorkspaceInfo {

        /**
         * workspace configuration (passed in constructor)
         */
        private final WorkspaceConfig config;

        /**
         * file system (instantiated on init)
         */
        private FileSystem fs;

        /**
         * persistence manager (instantiated on init)
         */
        private PersistenceManager persistMgr;

        /**
         * item state provider (instantiated on init)
         */
        private SharedItemStateManager itemStateMgr;

        /**
         * observation manager factory (instantiated on init)
         */
        private ObservationManagerFactory obsMgrFactory;

        /**
         * system session (lazily instantiated)
         */
        private SystemSession systemSession;

        /**
         * search manager (lazily instantiated)
         */
        private SearchManager searchMgr;

        /**
         * lock manager (lazily instantiated)
         */
        private LockManagerImpl lockMgr;

        /**
         * flag indicating whether this instance has been initialized.
         */
        private boolean initialized;

        /**
         * timestamp when the workspace has been determined being idle
         */
        private long idleTimestamp;

        /**
         * Creates a new <code>WorkspaceInfo</code> based on the given
         * <code>config</code>.
         *
         * @param config workspace configuration
         */
        protected WorkspaceInfo(WorkspaceConfig config) {
            this.config = config;
            idleTimestamp = 0;
            initialized = false;
        }

        /**
         * Returns the workspace name.
         *
         * @return the workspace name
         */
        String getName() {
            return config.getName();
        }

        /**
         * Returns the workspace configuration.
         *
         * @return the workspace configuration
         */
        public WorkspaceConfig getConfig() {
            return config;
        }

        /**
         * Returns the timestamp when the workspace has become idle or zero
         * if the workspace is currently not idle.
         *
         * @return the timestamp when the workspace has become idle or zero if
         *         the workspace is not idle.
         */
        long getIdleTimestamp() {
            return idleTimestamp;
        }

        /**
         * Sets the timestamp when the workspace has become idle. if
         * <code>ts == 0</code> the workspace is marked as being currently
         * active.
         *
         * @param ts timestamp when workspace has become idle.
         */
        void setIdleTimestamp(long ts) {
            idleTimestamp = ts;
        }

        /**
         * Returns <code>true</code> if this workspace info is initialized,
         * otherwise returns <code>false</code>.
         *
         * @return <code>true</code> if this workspace info is initialized.
         */
        synchronized boolean isInitialized() {
            return initialized;
        }

        /**
         * Returns the workspace file system.
         *
         * @return the workspace file system
         */
        synchronized FileSystem getFileSystem() {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            return fs;
        }

        /**
         * Returns the workspace persistence manager.
         *
         * @return the workspace persistence manager
         * @throws RepositoryException if the persistence manager could not be instantiated/initialized
         */
        synchronized PersistenceManager getPersistenceManager()
                throws RepositoryException {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            return persistMgr;
        }

        /**
         * Returns the workspace item state provider
         *
         * @return the workspace item state provider
         * @throws RepositoryException if the workspace item state provider
         *                             could not be created
         */
        synchronized SharedItemStateManager getItemStateProvider()
                throws RepositoryException {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            return itemStateMgr;
        }

        /**
         * Returns the observation manager factory for this workspace
         *
         * @return the observation manager factory for this workspace
         */
        synchronized ObservationManagerFactory getObservationManagerFactory() {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            return obsMgrFactory;
        }

        /**
         * Returns the search manager for this workspace.
         *
         * @return the search manager for this workspace, or <code>null</code>
         *         if no <code>SearchManager</code>
         * @throws RepositoryException if the search manager could not be created
         */
        synchronized SearchManager getSearchManager() throws RepositoryException {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            if (searchMgr == null) {
                if (config.getSearchConfig() == null) {
                    // no search index configured
                    return null;
                }
                // search manager is lazily instantiated in order to avoid
                // 'chicken & egg' bootstrap problems
                searchMgr = new SearchManager(config.getSearchConfig(),
                        nsReg,
                        ntReg,
                        itemStateMgr,
                        rootNodeId,
                        getSystemSearchManager(getName()),
                        SYSTEM_ROOT_NODE_ID);
            }
            return searchMgr;
        }

        /**
         * Returns the lock manager for this workspace.
         *
         * @return the lock manager for this workspace
         * @throws RepositoryException if the lock manager could not be created
         */
        synchronized LockManager getLockManager() throws RepositoryException {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            // lock manager is lazily instantiated in order to avoid
            // 'chicken & egg' bootstrap problems
            if (lockMgr == null) {
                lockMgr = new LockManagerImpl(getSystemSession(), fs);
            }
            return lockMgr;
        }

        /**
         * Returns the system session for this workspace.
         *
         * @return the system session for this workspace
         * @throws RepositoryException if the system session could not be created
         */
        synchronized SystemSession getSystemSession() throws RepositoryException {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            // system session is lazily instantiated in order to avoid
            // 'chicken & egg' bootstrap problems
            if (systemSession == null) {
                systemSession = SystemSession.create(RepositoryImpl.this, config);
            }
            return systemSession;
        }

        /**
         * Initializes this workspace info. The following components are
         * initialized immediately:
         * <ul>
         * <li>file system</li>
         * <li>persistence manager</li>
         * <li>shared item state manager</li>
         * <li>observation manager factory</li>
         * </ul>
         * The following components are initialized lazily (i.e. on demand)
         * in order to save resources and to avoid 'chicken & egg' bootstrap
         * problems:
         * <ul>
         * <li>system session</li>
         * <li>lock manager</li>
         * <li>search manager</li>
         * </ul>
         */
        synchronized void initialize() throws RepositoryException {
            if (initialized) {
                throw new IllegalStateException("already initialized");
            }

            log.info("initializing workspace '" + getName() + "'...");

            FileSystemConfig fsConfig = config.getFileSystemConfig();
            fsConfig.init();
            fs = fsConfig.getFileSystem();

            persistMgr = createPersistenceManager(new File(config.getHomeDir()),
                    fs,
                    config.getPersistenceManagerConfig(),
                    rootNodeId,
                    nsReg,
                    ntReg);

            // create item state manager
            try {
                itemStateMgr =
                        new SharedItemStateManager(persistMgr, rootNodeId, ntReg, true);
                try {
                    itemStateMgr.addVirtualItemStateProvider(
                            vMgr.getVirtualItemStateProvider());
                    itemStateMgr.addVirtualItemStateProvider(
                            virtNTMgr.getVirtualItemStateProvider());
                } catch (Exception e) {
                    log.error("Unable to add vmgr: " + e.toString(), e);
                }
            } catch (ItemStateException ise) {
                String msg = "failed to instantiate shared item state manager";
                log.debug(msg);
                throw new RepositoryException(msg, ise);
            }

            obsMgrFactory = new ObservationManagerFactory();

            initialized = true;

            log.info("workspace '" + getName() + "' initialized");
        }

        /**
         * Disposes all objects this <code>WorkspaceInfo</code> is holding.
         */
        synchronized void dispose() {
            if (!initialized) {
                throw new IllegalStateException("not initialized");
            }

            log.info("shutting down workspace '" + getName() + "'...");

            // dispose observation manager factory
            obsMgrFactory.dispose();
            obsMgrFactory = null;

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

            // dispose shared item state manager
            itemStateMgr.dispose();
            itemStateMgr = null;

            // close persistence manager
            try {
                persistMgr.close();
            } catch (Exception e) {
                log.error("error while closing persistence manager of workspace "
                        + config.getName(), e);
            }
            persistMgr = null;

            // close lock manager
            if (lockMgr != null) {
                lockMgr.close();
                lockMgr = null;
            }

            // close workspace file system
            FileSystemConfig fsConfig = config.getFileSystemConfig();
            fsConfig.dispose();
            fs = null;

            // reset idle timestamp
            idleTimestamp = 0;

            initialized = false;

            log.info("workspace '" + getName() + "' has been shutdown");
        }
    }

    /**
     * The workspace janitor thread that will shutdown workspaces that have
     * been idle for a certain amount of time.
     */
    private class WorkspaceJanitor extends Thread {

        /**
         * amount of time in mmilliseconds before an idle workspace is
         * automatically shutdown.
         */
        private long maxIdleTime;
        /**
         * interval in mmilliseconds between checks for idle workspaces.
         */
        private long checkInterval;

        /**
         * Creates a new <code>WorkspaceJanitor</code> instance responsible for
         * shutting down idle workspaces.
         *
         * @param maxIdleTime amount of time in mmilliseconds before an idle
         *                    workspace is automatically shutdown.
         */
        WorkspaceJanitor(long maxIdleTime) {
            super("WorkspaceJanitor");
            setPriority(Thread.MIN_PRIORITY);
            setDaemon(true);
            this.maxIdleTime = maxIdleTime;
            // compute check interval as 10% of maxIdleTime
            checkInterval = (long) (0.1 * maxIdleTime);
        }

        /**
         * {@inheritDoc}
         * <p/>
         * Performs the following tasks in a <code>while (true)</code> loop:
         * <ol>
         * <li>wait for <code>checkInterval</code> milliseconds</li>
         * <li>build list of initialized but currently inactive workspaces
         *     (excluding the default workspace)</li>
         * <li>shutdown those workspaces that have been idle for at least
         *     <code>maxIdleTime</code> milliseconds</li>
         * </ol>
         */
        public void run() {
            while (!disposed) {
                try {
                    Thread.sleep(checkInterval);
                } catch (InterruptedException e) {
                    /* ignore */
                }

                synchronized (RepositoryImpl.this) {
                    if (disposed) {
                        return;
                    }
                    // get names of workspaces
                    Set wspNames = new HashSet(wspInfos.keySet());
                    // remove default workspace (will never be shutdown when idle)
                    wspNames.remove(repConfig.getDefaultWorkspaceName());
                    // remove workspaces with active sessions
                    for (Iterator it = activeSessions.values().iterator(); it.hasNext();) {
                        SessionImpl ses = (SessionImpl) it.next();
                        wspNames.remove(ses.getWorkspace().getName());
                    }
                    // remove uninitialized workspaces
                    for (Iterator it = wspInfos.values().iterator(); it.hasNext();) {
                        WorkspaceInfo wspInfo = (WorkspaceInfo) it.next();
                        if (!wspInfo.isInitialized()) {
                            wspNames.remove(wspInfo.getName());
                        }
                    }

                    // remaining names denote workspaces which are currently idle
                    for (Iterator it = wspNames.iterator(); it.hasNext();) {
                        WorkspaceInfo wspInfo = (WorkspaceInfo) wspInfos.get(it.next());
                        long currentTS = System.currentTimeMillis();
                        long idleTS = wspInfo.getIdleTimestamp();
                        if (idleTS == 0) {
                            // set idle timestamp
                            wspInfo.setIdleTimestamp(currentTS);
                        } else {
                            if ((currentTS - idleTS) > maxIdleTime) {
                                // temporarily shutdown workspace
                                wspInfo.dispose();
                            }
                        }
                    }
                }
            }
        }
    }
}
