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
import org.apache.jackrabbit.core.config.LoginModuleConfig;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
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
import org.apache.jackrabbit.uuid.UUID;
import org.apache.log4j.Logger;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.security.auth.Subject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

/**
 * A <code>RepositoryImpl</code> ...
 */
public class RepositoryImpl implements Repository, SessionListener,
        EventListener {

    private static Logger log = Logger.getLogger(RepositoryImpl.class);

    /**
     * repository home lock
     */
    private static final String REPOSITORY_LOCK = ".lock";

    /**
     * hardcoded uuid of the repository root node
     */
    private static final String ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    private static final String SYSTEM_ROOT_NODE_UUID = "deadbeef-cafe-babe-cafe-babecafebabe";
    private static final String VERSION_STORAGE_NODE_UUID = "deadbeef-face-babe-cafe-babecafebabe";
    private static final String NODETYPES_NODE_UUID = "deadbeef-cafe-cafe-cafe-babecafebabe";

    private static final String PROPERTIES_RESOURCE = "rep.properties";
    private final Properties repProps;

    /**
     * Name of the lock file, relative to the workspace home directory
     */
    private static final String LOCKS_FILE = "locks";

    // names of well-known repository properties
    public static final String STATS_NODE_COUNT_PROPERTY = "jcr.repository.stats.nodes.count";
    public static final String STATS_PROP_COUNT_PROPERTY = "jcr.repository.stats.properties.count";

    private String rootNodeUUID;

    private final NamespaceRegistryImpl nsReg;
    private final NodeTypeRegistry ntReg;
    private final VersionManager vMgr;
    private final VirtualNodeTypeStateManager virtNTMgr;

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
    protected RepositoryImpl(RepositoryConfig repConfig) throws RepositoryException {

        this.repConfig = repConfig;

        this.acquireRepositoryLock() ;

        // setup file systems
        repStore = repConfig.getFileSystem();
        String fsRootPath = "/meta";
        try {
            if (!repStore.exists(fsRootPath) || !repStore.isFolder(fsRootPath)) {
                repStore.createFolder(fsRootPath);
            }
        } catch (FileSystemException fse) {
            String msg = "failed to create folder for repository meta data";
            log.debug(msg);
            throw new RepositoryException(msg, fse);
        }
        metaDataStore = new BasedFileSystem(repStore, fsRootPath);

        // init root node uuid
        rootNodeUUID = loadRootNodeUUID(metaDataStore);

        // load repository properties
        repProps = new Properties();
        loadRepProps();
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
        VersioningConfig vConfig = repConfig.getVersioningConfig();
        PersistenceManager pm = createPersistenceManager(vConfig.getHomeDir(),
                vConfig.getFileSystem(),
                vConfig.getPersistenceManagerConfig(),
                rootNodeUUID,
                nsReg,
                ntReg);
        vMgr = new VersionManagerImpl(pm, ntReg, delegatingDispatcher,
                VERSION_STORAGE_NODE_UUID, SYSTEM_ROOT_NODE_UUID);

        // init virtual nodetype manager
        virtNTMgr = new VirtualNodeTypeStateManager(getNodeTypeRegistry(),
                delegatingDispatcher, NODETYPES_NODE_UUID, SYSTEM_ROOT_NODE_UUID);

        // initialize workspaces
        try {
            iter = wspInfos.keySet().iterator();
            while (iter.hasNext()) {
                String wspName = (String) iter.next();
                initWorkspace(wspName);
            }
        } catch (RepositoryException e) {
            // if any workspace failed to initialize, shutdown again
            log.error("Unable to start repository. forcing shutdown.");
            shutdown();
            throw e;
        }

        // after the workspaces are initialized, we setup a system session for
        // the virtual nodetype manager
        virtNTMgr.setSession(getSystemSession(repConfig.getDefaultWorkspaceName()));
    }

    /**
     * Lock the repository home.
     * @throws RepositoryException
     *         if the repository lock can not be acquired
     */
    private void acquireRepositoryLock() throws RepositoryException {
        File home = new File(this.repConfig.getHomeDir());
        File lock  = new File(home, REPOSITORY_LOCK) ;
        if (lock.exists()) {
            throw new RepositoryException("The repository home at " + home.getAbsolutePath() + 
                " appears to be in use. If you are sure it's not in use please delete the file at " + 
                lock.getAbsolutePath() + ". Probably the repository was not shutdown properly.");
        }
        try {
            lock.createNewFile() ;
        } catch (IOException e) {
            throw new RepositoryException("Unable to create lock file at " + lock.getAbsolutePath());
        }
    }

    /**
     * Release repository lock
     */
    private void releaseRepositoryLock() {
        File home = new File(this.repConfig.getHomeDir());
        File lock  = new File(home, REPOSITORY_LOCK) ;
        if (!lock.delete()) {
            log.error("Unable to release repository lock") ;
        }
    }

    /**
     * Returns the root node uuid.
     * @param fs
     * @return
     * @throws RepositoryException
     */
    protected String loadRootNodeUUID(FileSystem fs) throws RepositoryException {
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
                    return new UUID(new String(chars)).toString();
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
                        writer.write(ROOT_NODE_UUID);
                    } finally {
                        try {
                            writer.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                    return ROOT_NODE_UUID;
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

    private void initWorkspace(String wspName) throws RepositoryException {
        // get system session and Workspace instance
        SessionImpl sysSession = getSystemSession(wspName);
        WorkspaceImpl wsp = (WorkspaceImpl) sysSession.getWorkspace();

        /**
         * todo implement 'System' workspace
         * FIXME
         * - the should be one 'System' workspace per repositoy
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
            NodeImpl sysRoot = rootNode.internalAddChildNode(QName.JCR_SYSTEM, nt, SYSTEM_ROOT_NODE_UUID);
            // add version storage
            nt = sysSession.getNodeTypeManager().getNodeType(QName.REP_VERSIONSTORAGE);
            sysRoot.internalAddChildNode(QName.JCR_VERSIONSTORAGE, nt, VERSION_STORAGE_NODE_UUID);
            // add nodetypes
            nt = sysSession.getNodeTypeManager().getNodeType(QName.REP_NODETYPES);
            sysRoot.internalAddChildNode(QName.JCR_NODETYPES, nt, NODETYPES_NODE_UUID);
            rootNode.save();
        }

        // register the repository as event listener for keeping repository statistics
        wsp.getObservationManager().addEventListener(this,
                Event.NODE_ADDED | Event.NODE_REMOVED
                | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED,
                "/", true, null, null, false);

        // register SearchManager as event listener
        SearchManager searchMgr = getSearchManager(wspName);
        if (searchMgr != null) {
            wsp.getObservationManager().addEventListener(searchMgr,
                    Event.NODE_ADDED | Event.NODE_REMOVED
                    | Event.PROPERTY_ADDED | Event.PROPERTY_REMOVED
                    | Event.PROPERTY_CHANGED,
                    "/", true, null, null, false);
        }

        // register the observation factory of that workspace
        delegatingDispatcher.addDispatcher(getObservationManagerFactory(wspName));
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

    String getRootNodeUUID() {
        // check sanity of this instance
        sanityCheck();

        return rootNodeUUID;
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

        // setup/initialize new workspace
        initWorkspace(workspaceName);
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
     * @param workspaceName
     * @return
     * @throws NoSuchWorkspaceException
     * @throws RepositoryException
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
    protected final SessionImpl createSession(AuthContext loginContext,
                              String workspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            RepositoryException {
        WorkspaceInfo wspInfo = getWorkspaceInfo(workspaceName);
        SessionImpl ses = createSessionInstance(loginContext, wspInfo.getConfig());
        ses.addListener(this);
        activeSessions.put(ses, ses);
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
    protected final SessionImpl createSession(Subject subject,
                                              String workspaceName)
            throws NoSuchWorkspaceException, AccessDeniedException,
            RepositoryException {
        WorkspaceInfo wspInfo = getWorkspaceInfo(workspaceName);
        SessionImpl ses = createSessionInstance(subject, wspInfo.getConfig());
        ses.addListener(this);
        activeSessions.put(ses, ses);
        return ses;
    }

    /**
     * Shuts down this repository. Note that this method is called automatically
     * through a shutdown hook.
     *
     * @see Runtime#addShutdownHook(Thread)
     */
    public synchronized void shutdown() {
        // check status of this instance
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

        try {
            vMgr.close();
        } catch (Exception e) {
            log.error("Error while closing Version Manager.", e);
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

        this.releaseRepositoryLock() ;
        
    }

    /**
     * Returns the configuration of this repository.
     * @return repository configuration
     */
    public RepositoryConfig getConfig() {
        return repConfig;
    }

    /**
     * Returns an <code>InputStream</code> on a <code>Properties</code> resource
     * which contains the default properties for the repository. This method is
     * only called once during repository initialization.
     * <p/>
     * The <code>InputStream</code> returned is closed by the caller.
     * <p/>
     * This method returns an <code>InputStream</code> on the
     * <code>org/apache/jackrabbit/core/repository.properties</code> resource
     * found in the class path.
     *
     * @return <code>InputStream</code> on a <code>Properties</code> resource
     *         or <code>null</code> if the resource does not exist.
     */
    protected InputStream getDefaultRepositoryProperties() {
        return RepositoryImpl.class.getResourceAsStream("repository.properties");
    }

    private void loadRepProps() throws RepositoryException {
        FileSystemResource propFile = new FileSystemResource(metaDataStore, PROPERTIES_RESOURCE);
        try {
            repProps.clear();
            if (!propFile.exists() || propFile.length() == 0) {
                // initialize properties with pre-defined values
                InputStream in = getDefaultRepositoryProperties();
                if (in != null) {
                    try {
                        repProps.load(in);
                    } finally {
                        in.close();
                    }
                }

                // set counts
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
            log.debug(msg);
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
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    /**
     * Creates a workspace persistence manager based on the given
     * configuration. The persistence manager is instantiated using
     * information in the given persistencem manager configuration and
     * initialized with a persistence manager context containing the other
     * arguments.
     *
     * @return the created workspace persistence manager
     * @throws RepositoryException if the persistence manager could
     *                             not be instantiated/initialized
     */
    private static PersistenceManager createPersistenceManager(File homeDir, FileSystem fs, PersistenceManagerConfig pmConfig,
                                                               String rootNodeUUID, NamespaceRegistry nsReg, NodeTypeRegistry ntReg)
            throws RepositoryException {
        try {
            PersistenceManager pm = (PersistenceManager) pmConfig.newInstance();
            pm.init(new PMContext(homeDir, fs, rootNodeUUID, nsReg, ntReg));
            return pm;
        } catch (Exception e) {
            String msg = "Cannot instantiate persistence manager " + pmConfig.getClassName();
            log.error(msg, e);
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
    public void loggedOut(SessionImpl session) {
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
        private SharedItemStateManager itemStateMgr;

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
         * Lock manager
         */
        private LockManagerImpl lockMgr;

        /**
         * Creates a new <code>WorkspaceInfo</code> based on the given
         * <code>config</code>.
         *
         * @param config workspace configuration
         */
        protected WorkspaceInfo(WorkspaceConfig config) {
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
        public WorkspaceConfig getConfig() {
            return config;
        }

        /**
         * Returns the workspace persistence manager
         *
         * @return the workspace persistence manager
         * @throws RepositoryException if the persistence manager could not be instantiated/initialized
         */
        synchronized PersistenceManager getPersistenceManager(PersistenceManagerConfig pmConfig)
                throws RepositoryException {
            if (persistMgr == null) {
                persistMgr = createPersistenceManager(new File(config.getHomeDir()),
                        config.getFileSystem(),
                        pmConfig,
                        rootNodeUUID,
                        nsReg,
                        ntReg);
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
                systemSession = SystemSession.create(RepositoryImpl.this, config);
            }
            return systemSession;
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
            if (itemStateMgr == null) {
                // create item state manager
                try {
                    itemStateMgr = new SharedItemStateManager(
                            getPersistenceManager(config.getPersistenceManagerConfig()),
                            rootNodeUUID, ntReg);
                    try {
                        itemStateMgr.addVirtualItemStateProvider(vMgr.getVirtualItemStateProvider());
                        itemStateMgr.addVirtualItemStateProvider(virtNTMgr.getVirtualItemStateProvider());
                    } catch (Exception e) {
                        log.error("Unable to add vmgr: " + e.toString(), e);
                    }
                } catch (ItemStateException ise) {
                    String msg = "failed to instantiate persistent item state manager";
                    log.debug(msg);
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
         * @return the search manager for this workspace, or <code>null</code>
         *         if no <code>SearchManager</code>
         * @throws RepositoryException if the search manager could not be created
         */
        synchronized SearchManager getSearchManager() throws RepositoryException {
            if (searchMgr == null) {
                if (config.getSearchConfig() == null) {
                    // no search index configured
                    return null;
                }
                searchMgr = new SearchManager(getSystemSession(),
                        config.getSearchConfig(),
                        ntReg,
                        getItemStateProvider());
            }
            return searchMgr;
        }

        /**
         * Returns the lock manager for this workspace
         *
         * @return the lock manager for this workspace
         * @throws RepositoryException if the lock manager could not be created
         */
        synchronized LockManager getLockManager() throws RepositoryException {
            if (lockMgr == null) {
                lockMgr = new LockManagerImpl(getSystemSession(),
                        new File(config.getHomeDir(), LOCKS_FILE));
            }
            return lockMgr;
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
                    log.error("error while closing persistence manager of workspace "
                            + config.getName(), e);
                }
                persistMgr = null;
            }

            if (lockMgr != null) {
                lockMgr.close();
            }

            // close workspace file system
            try {
                // close workspace file system
                config.getFileSystem().close();
            } catch (FileSystemException e) {
                log.error("error while closing filesystem of workspace "
                        + config.getName(), e);
            }
        }
    }
}
