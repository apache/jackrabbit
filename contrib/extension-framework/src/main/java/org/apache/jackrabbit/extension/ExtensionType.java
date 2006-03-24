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

import java.net.URL;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.classloader.RepositoryClassLoader;

/**
 * The <code>ExtensionType</code> class represents a collection of extensions
 * sharing the same Extension Type Identification. Instances of this class
 * maintain the extension types class loader and the set of extenions of the
 * same type which have been loaded through this instance.
 * <p>
 * The equality of instances of this class is defined by the equality of the
 * Extension Type Identifier. If two instances have same extension type
 * identifier, they are considered equal.
 *
 * @author Felix Meschberger
 *
 * @see org.apache.jackrabbit.extension.ExtensionManager
 * @see org.apache.jackrabbit.extension.ExtensionDescriptor
 */
public class ExtensionType {

    /** default log */
    private static final Log log = LogFactory.getLog(ExtensionType.class);

    /**
     * Pattern used to create an XPath query to look for extensions of a certain
     * type. The parameters in the patterns are the root path below which to
     * search (<i>{0}</i>) and the extension type identificatio (<i>{1}</i>).
     *
     * @see #findExtensions(String, String)
     */
    private static final MessageFormat EXTENSION_QUERY_PATTERN =
        new MessageFormat("{0}//element(*, " + ExtensionManager.NODE_EXTENSION_TYPE + ")[@rep:id = ''{1}'']");

    /**
     * Pattern used to create an XPath query to look for a specific extension
     * by its name and type type. The parameters in the patterns are the root
     * path below which to search (<i>{0}</i>), the extension type
     * identification (<i>{1}</i>) and the extension name (<i>{1}</i>).
     *
     * @see #findExtension(String, String, String)
     */
    private static final MessageFormat EXTENSION_QUERY_PATTERN2 =
        new MessageFormat("{0}//element(*, " + ExtensionManager.NODE_EXTENSION_TYPE + ")[@rep:id = ''{1}'' and @rep:name = ''{2}'']");

    /**
     * The {@link ExtensionManager} responsible for accessing this instance.
     */
    private final ExtensionManager manager;

    /**
     * The Extension Type Identification of this extension type instance. No
     * two instances of this class with the same manager share their id.
     */
    private final String id;

    /**
     * The set of extensions loaded with this extension type, indexed by their
     * names.
     */
    private final Map extensions;

    /**
     * The <code>RepositoryClassLoader</code> used for extensions of this type.
     * This field is set on demand by the
     * {@link #getClassLoader(ExtensionDescriptor)} method.
     */
    private RepositoryClassLoader loader;

    /**
     * Creates a new extension type instance in the {@link ExtensionManager}
     * with the given extension type identification.
     *
     * @param manager The {@link ExtensionManager} managing this extension
     *      type and its extensions.
     * @param id The Extension Type Identification of this instance.
     */
    /* package */ ExtensionType(ExtensionManager manager, String id) {
        this.manager = manager;
        this.id = id;
        this.extensions = new TreeMap();
    }

    /**
     * Returns the Extension Type Identification of this extension type.
     */
    public String getId() {
        return id;
    }

    /**
     * Searches in the workspace of this instance's <code>Session</code> for
     * extensions of this type returning an <code>Iterator</code> of
     * {@link ExtensionDescriptor} instances. If <code>root</code> is non-<code>null</code>
     * the search for extensions only takes place in the indicated subtree.
     * <p>
     * <b>NOTE</B>: This method may return more than one extension with the
     * same name for this type. This is the only place in the Jackrabbit
     * Extension Framework which handles duplicate extension names. The rest
     * relies on extensions to have unique <code>id/name</code> pairs.
     * <p>
     * Calling this method multiple times will return the same
     * {@link ExtensionDescriptor} instances. Previously available instances
     * will not be returned though if their extension node has been removed in
     * the meantime. Such instances will still be available through
     * {@link #getExtension(String, String)} but will not be available on next
     * system restart.
     *
     * @param root The root node below which the extensions are looked for. This
     *            path is taken as an absolute path regardless of whether it
     *            begins with a slash or not. If <code>null</code> or empty,
     *            the search takes place in the complete workspace.
     *
     * @return An {@link ExtensionIterator} providing the extensions of this
     *      type.
     *
     * @throws ExtensionException If an error occurrs looking for extensions.
     */
    public ExtensionIterator getExtensions(String root) throws ExtensionException {

        // make sure root is not null and has no leading slash
        if (root == null) {
            root = "";
        } else if (root.length() >= 1 && root.charAt(0) == '/') {
            root = root.substring(1);
        }

        // build the query string from the query pattern
        String queryXPath;
        synchronized (EXTENSION_QUERY_PATTERN) {
            queryXPath = EXTENSION_QUERY_PATTERN.format(new Object[]{ root, id });
        }

        log.debug("Looking for extensions of type " + id + " below /" + root);

        NodeIterator nodes = manager.findNodes(queryXPath);
        return new ExtensionIterator(this, nodes);
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
     * @param name The name of the extension of the indicated type to be found.
     * @param root The root node below which the extensions are looked for. This
     *      path is taken as an absolute path regardless of whether it begins
     *      with a slash or not. If <code>null</code> or empty, the search
     *      takes place in the complete workspace.
     *
     * @return The named {@link ExtensionDescriptor} instances.
     *
     * @throws IllegalArgumentException If <code>name</code> is empty or
     *      <code>null</code>.
     * @throws ExtensionException If no or more than one extensions with the
     *      same name and type can be found or if another error occurrs looking
     *      for extensions.
     */
    public ExtensionDescriptor getExtension(String name, String root)
            throws ExtensionException {

        // check name
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Extension name must not be" +
                    " null or empty string");
        }

        // check whether we already loaded the extension
        ExtensionDescriptor ed = getOrCreateExtension(name, null);
        if (ed != null) {
            return ed;
        }

        // make sure root is not null and has no leading slash
        if (root == null) {
            root = "";
        } else if (root.length() >= 1 && root.charAt(0) == '/') {
            root = root.substring(1);
        }

        // build the query string from the query pattern
        String queryXPath;
        synchronized (EXTENSION_QUERY_PATTERN2) {
            queryXPath = EXTENSION_QUERY_PATTERN2.format(new Object[]{ root, id, name});
        }

        log.debug("Looking for extension " + id + "/" + name + " below /" + root);

        NodeIterator nodes = manager.findNodes(queryXPath);
        if (!nodes.hasNext()) {
            throw new ExtensionException("Extension " + id + "/" + name +
                " not found");
        }

        Node extNode = nodes.nextNode();
        if (nodes.hasNext()) {
            throw new ExtensionException("More than one extension " +
                id + "/" + name + " found");
        }

        // load the descriptor and return
        return createExtension(name, extNode);
    }

    /**
     * Returns a repository class loader for the given extension. If the
     * extension contains a class path definition, that class path is added to
     * the class loader before returning.
     *
     * @param extension The {@link ExtensionDescriptor} for which to return
     *      the class loader.
     *
     * @return The <code>ClassLoader</code> used to load the extension and
     *      extension configuration class.
     *
     * @see ExtensionDescriptor#getExtensionLoader()
     * @see ExtensionDescriptor#getExtension()
     */
    /* package */ ClassLoader getClassLoader(ExtensionDescriptor extension) {

        if (loader == null) {
            // not created yet, so we create
            loader = manager.createClassLoader();
        }

        // make sure the class path for the class path is already defined
        fixClassPath(loader, extension);

        // return the class loader now
        return loader;
    }

    /**
     * Makes sure, the class path defined in the <code>extension</code> is
     * known to the <code>loader</code>.
     *
     * @param loader The repository class loader whose current class path is
     *      ensured to contain the extension's class path.
     * @param extension The extension providing additions to the repository
     *      class loader's class path.
     */
    private static void fixClassPath(RepositoryClassLoader loader,
        ExtensionDescriptor extension) {

        if (extension.getClassPath() == null) {
            return;
        }

        URL[] urls = loader.getURLs();
        Set paths = new HashSet();
        for (int i=0; i < urls.length; i++) {
            paths.add(urls[i].getPath());
        }

        String[] classPath = extension.getClassPath();
        for (int i=0; i < classPath.length; i++) {
            if (!paths.contains(classPath[i])) {
                loader.addHandle(classPath[i]);
            }
        }
    }

    //---------- Object overwrite ---------------------------------------------

    /**
     * Returns the hash code of this types extension type identification.
     */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns <code>true</code> if <code>obj</code> is <code>this</code> or
     * if it is an <code>ExtensionType</code> with the same extension type
     * identification as <code>this</code>.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ExtensionType) {
            return id.equals(((ExtensionType) obj).getId());
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of this instance including the extension
     * type identification.
     */
    public String toString() {
        return "Extension type " + getId();
    }

    //--------- internal helper -----------------------------------------------

    /**
     * Returns an {@link ExtensionDescriptor} for the name extension optionally
     * loaded from the <code>extNode</code>. If this type has already loaded
     * an extension with the given name, that extension descriptor is returned.
     * Otherwise a new extension descriptor is created from the extension node
     * and internally cached before being returned.
     *
     * @param name The name of the extension for which to return the descriptor.
     * @param extNode The <code>Node</code> containing the extension definition
     *      to be loaded if this instance has not loaded the named extension
     *      yet. This may be <code>null</code> to prevent loading an extension
     *      descriptor if the named extension has not been loaded yet.
     *
     * @return The name {@link ExtensionDescriptor} or <code>null</code> if this
     *      instance has not loaded the named extension yet and
     *      <code>extNode</code> is <code>null</code>.
     *
     * @throws ExtensionException If an error occurrs loading the extension
     *      descriptor from the <code>extNode</code>.
     */
    /* package */ ExtensionDescriptor getOrCreateExtension(String name, Node extNode)
            throws ExtensionException {

        // check whether we already loaded the extension
        ExtensionDescriptor ed = (ExtensionDescriptor) extensions.get(name);
        if (ed != null) {
            return ed;
        }

        if (extNode != null) {
            return createExtension(name, extNode);
        }

        // fallback to nothing
        return null;
    }

    /**
     * Creates and locally registers an {@link ExtensionDescriptor} instance
     * with the given <code>name</code> reading the descriptor from the
     * <code>extNode</code>.
     *
     * @param name The name of the extension to create. This is the name used to
     *            register the extension as.
     * @param extNode The <code>Node</code> from which the extension is
     *            loaded.
     *
     * @return The newly created and registered {@link ExtensionDescriptor}.
     *
     * @throws ExtensionException If an error occurrs loading the
     *      {@link ExtensionDescriptor} from the <code>extNode</code>.
     */
    private ExtensionDescriptor createExtension(String name, Node extNode)
            throws ExtensionException {
        ExtensionDescriptor ed = new ExtensionDescriptor(this, extNode);
        extensions.put(name, ed);
        return ed;
    }
}
