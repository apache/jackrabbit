/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
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
package org.apache.jackrabbit.extension;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.classloader.RepositoryClassLoader;

/**
 * The <code>ExtensionManager</code> class provides the core functionality
 * of the Jackrabbit Extension Framework by methods for finding extensions.
 * <p>
 * Instances of this class are created with a <code>Session</code> to the
 * repository. Consequently all access to the repository is confined to the
 * workspace to which the session is attached. That is, only extensions located
 * in the session's workspace are found.
 * <p>
 * Additionally the class provides functionality to define the extension node
 * types on demand.
 *
 * @author Felix Meschberger
 */
public final class ExtensionManager {

    /** default logger */
    private static final Log log = LogFactory.getLog(ExtensionManager.class);

    /**
     * The name of the repository node type defining the properties making up
     * an extension description (value is "rep:extension").
     */
    public static final String NODE_EXTENSION_TYPE = "rep:extension";

    /**
     * The session providing access to the repository for loading extensions.
     */
    private final Session session;

    /**
     * The application's class loader as provided to the constructor.
     */
    private final ClassLoader applicationLoader;

    /**
     * The map of extension types by the extension type identifiers.
     */
    private Map extensionTypes;

    /**
     * Creates an instance of this manager accessing the repository through the
     * given <code>session</code>.
     * <p>
     * This also confines the extensions available to this manager to the
     * extensions available through the workspace accessed by the
     * <code>session</code>.
     * <p>
     * If the <code>applicationLoader</code> parameter is <code>null</code>
     * either the current thread's context class loader is used or if that one
     * is <code>null</code>, too, the class loader of this class is used.
     * It is recommended, that the caller of the constructor provides a
     * non-<code>null</code> class loader to prevent unexpected class loading
     * issues.
     * <p>
     * To make sure extensions may actually be correctly handled, the
     * {@link #checkNodeType(Session)} method is called.
     *
     * @param session The <code>Session</code> to search for and load extensions.
     * @param applicationLoader The <code>ClassLoader</code> used as the parent
     *      for the repository class loaders.
     *
     * @throws NullPointerException If <code>session</code> is <code>null</code>.
     */
    public ExtensionManager(Session session, ClassLoader applicationLoader)
            throws ExtensionException {

        // check session
        if (session == null) {
            throw new NullPointerException("session");
        }

        // make sure the extension system node type is available
        checkNodeType(session);

        // make sure the application class loader is non-null
        // - first try current thread context loader
        // - otherwise use this class's class loader
        if (applicationLoader == null) {
            applicationLoader = Thread.currentThread().getContextClassLoader();
        }
        if (applicationLoader == null) {
            applicationLoader = getClass().getClassLoader();
        }

        // assign fields
        this.session = session;
        this.applicationLoader = applicationLoader;
    }

    /**
     * Searches in the workspace of this instance's <code>Session</code> for
     * extensions of type <code>id</code> returning an <code>Iterator</code>
     * of {@link ExtensionDescriptor} instances. If <code>root</code> is
     * non-<code>null</code> the search for extensions only takes place in the
     * indicated subtree.
     * <p>
     * <b>NOTE</B>: This method may return more than one extension with the same
     * name for a given <code>id</code>. This is the only place in the Jackrabbit
     * Extension Framework which handles duplicate extension names. The rest
     * relies on extensions to have unique <code>id</code>/name pairs.
     * <p>
     * Calling this method multiple times with the same <code>id</code> will
     * return the same {@link ExtensionDescriptor} instances. Previously
     * available instances will not returned though if their extension node
     * has been removed in the meantime. Such instances will still be available
     * through {@link #getExtension(String, String, String)} but will not be
     * available on next system restart.
     *
     * @param id The extension type identification describing the extensions to
     *      be found.
     * @param root The root node below which the extensions are looked for. This
     *      path is taken as an absolute path regardless of whether it begins
     *      with a slash or not. If <code>null</code> or empty, the search
     *      takes place in the complete workspace.
     *
     * @return An <code>Iterator</code> of {@link ExtensionDescriptor} instances
     *      describing the extensions found.
     *
     * @throws IllegalArgumentException If <code>id</code> is empty or
     *      <code>null</code>.
     * @throws ExtensionException If an error occurrs looking for extensions.
     */
    public Iterator getExtensions(String id, String root)
            throws ExtensionException {

        // delegate finding/loading to the extension type
        return getExtensionType(id).getExtensions(root);
    }

    /**
     * Searches in the workspace of this instance's <code>Session</code> for
     * an extension with the given <code>name</code> of type <code>id</code>.
     * If <code>root</code> is non-<code>null</code> the search for extensions
     * only takes place in the indicated subtree.
     * <p>
     * This method fails with an exception if more than one extension with the
     * same name of the same type is found in the workspace. Not finding the
     * requested extension also yields an exception.
     * <p>
     * Two consecutive calls to this method with the same arguments, namely
     * the same <code>id</code> and <code>name</code> will return the same
     * {@link ExtensionDescriptor} instance.
     *
     * @param id The extension type identification describing the extensions to
     *      be found.
     * @param name The name of the extension of the indicated type to be found.
     * @param root The root node below which the extensions are looked for. This
     *      path is taken as an absolute path regardless of whether it begins
     *      with a slash or not. If <code>null</code> or empty, the search
     *      takes place in the complete workspace.
     *
     * @return The named {@link ExtensionDescriptor} instances.
     *
     * @throws IllegalArgumentException If <code>id</code> or <code>name</code>
     *      is empty or <code>null</code>.
     * @throws ExtensionException If no or more than one extensions with the
     *      same name and type can be found or if another error occurrs looking
     *      for extensions.
     */
    public ExtensionDescriptor getExtension(String id, String name, String root)
            throws ExtensionException {

        // delegate finding/loading to the extension type
        return getExtensionType(id).getExtension(name, root);
    }

    //---------- Extension type helper methods --------------------------------

    /**
     * Creates a new instance of the <code>RepositoryClassLoader</code> class
     * with an empty path accessing the repository through the session of this
     * manager instance.
     * <p>
     * Note, that each call to this method returns a new RepositoryClassLoader
     * instance.
     */
    /* package */ RepositoryClassLoader createClassLoader() {
        return new RepositoryClassLoader(session, new String[0],
            applicationLoader);
    }

    /**
     * Executes the given XPath query on this mnanager's session and returns
     * a <code>NodeIterator</code> over the nodes matching the query.
     *
     * @param xpath The XPath query to execute
     *
     * @return The <code>NodeIterator</code> on the nodes matching the query.
     *
     * @throws ExtensionException If an error occurrs executing the query.
     *      The underlying exception is available as the cause of the exception.
     */
    /* package */ NodeIterator findNodes(String xpath) throws ExtensionException {
        try {
            // look for the extension nodes
            QueryManager qm = session.getWorkspace().getQueryManager();
            Query query = qm.createQuery(xpath, Query.XPATH);
            QueryResult res = query.execute();

            // check whether we found at least one node
            return res.getNodes();
        } catch (RepositoryException re) {
            throw new ExtensionException("Problem executing query '" +
                xpath + "'", re);
        }
    }

    //---------- internal helper ----------------------------------------------

    /**
     * Returns an {@link ExtensionType} instance for the given name.
     *
     * @throws IllegalArgumentException if <code>id</code> is <code>null</code>
     *      or an empty string.
     */
    /* package */ ExtensionType getExtensionType(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("Extension type identifier " +
                    "must not be null or empty string");
        }

        if (extensionTypes == null) {
            extensionTypes = new TreeMap();
        }

        ExtensionType type = (ExtensionType) extensionTypes.get(id);
        if (type == null) {
            type = new ExtensionType(this, id);
            extensionTypes.put(id, type);
        }

        return type;
    }

    /**
     * Makes sure the <code>rep:extension</code> node type is registered with
     * the <code>session</code>'s repository.
     * <p>
     * If the required extension descriptor node type is not defined in the
     * repository yet, it is tried to be registered. If an error occurrs
     * registering the node type a message is logged and an
     * <code>ExtensionException</code> is thrown.
     *
     * @param session The <code>Session</code> used to access the repository
     *      to test and optionally register the node.
     *
     * @throws ExtensionException If an error occurrs checking ro defining the
     *      node type.
     */
    public static void checkNodeType(Session session) throws ExtensionException {
        // quick check for the node type, succeed if defined
        try {
            session.getWorkspace().getNodeTypeManager().getNodeType(NODE_EXTENSION_TYPE);
            log.debug("Required node type exists, can expand archives");
            return;
        } catch (NoSuchNodeTypeException nst) {
            log.debug("Required node types does not exist, try to define");
        } catch (RepositoryException re) {
            log.debug("Cannot check for required node type, cannot expand " +
                    "archives", re);
            throw new ExtensionException("Cannot check for required node type", re);
        }

        try {
            Workspace workspace = session.getWorkspace();
            if (!NodeTypeSupport.registerNodeType(workspace)) {
                throw new ExtensionException("Registering required node type failed");
            }
        } catch (ExtensionException ee) {
            throw ee;
        } catch (Throwable t) {
            // Prevent anything from hapening if node type registration fails
            // due to missing libraries or other errors
            log.debug("Error registering node type", t);
            throw new ExtensionException("Cannot register required node type", t);
        }
    }
}
