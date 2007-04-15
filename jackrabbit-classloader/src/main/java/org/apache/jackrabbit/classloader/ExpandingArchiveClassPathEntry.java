/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>ExpandingArchiveClassPathEntry</code> extends the
 * {@link org.apache.jackrabbit.classloader.ArchiveClassPathEntry} class with support
 * to automatically expand the archive (JAR or ZIP) into the repository
 * below the path entry node. The path used to construct the instance is the
 * path of an item resolving to a property containing the jar archive to access.
 *
 * @author Felix Meschberger
 *
 * @see org.apache.jackrabbit.classloader.ArchiveClassPathEntry
 * @see org.apache.jackrabbit.classloader.ClassPathEntry
 */
/* package */ class ExpandingArchiveClassPathEntry extends ArchiveClassPathEntry {

    /** The name of the node type required to expand the archive */
    public static final String TYPE_JARFILE = "rep:jarFile";

    /** The name of the child node taking the expanded archive */
    public static final String NODE_JARCONTENTS = "rep:jarContents";

    /**
     * The name of the property taking the time at which the archive was
     * expanded
     */
    public static final String PROP_EXPAND_DATE = "rep:jarExpanded";

    /** Default logger */
    private static final Logger log =
        LoggerFactory.getLogger(ExpandingArchiveClassPathEntry.class);

    /** The node of the unpacked JAR contents */
    private Node jarContents;

    /**
     * Creates an instance of the <code>ExpandingArchiveClassPathEntry</code>
     * class.
     *
     * @param prop The <code>Property</code> containing the archive and
     *      the session used to access the repository.
     * @param path The original class path entry leading to the creation of
     *      this instance. This is not necessairily the same path as the
     *      property's path if the property was found through the primary
     *      item chain.
     *
     * @throws RepositoryException If an error occurrs retrieving the session
     *      from the property.
     */
    ExpandingArchiveClassPathEntry(Property prop, String path)
            throws RepositoryException {
        super(prop, path);
    }

    /**
     * Clones the indicated <code>ExpandingArchiveClassPathEntry</code> object
     * by taking over its path, session and property.
     *
     * @param base The base <code>ExpandingArchiveClassPathEntry</code> entry
     *      to clone.
     *
     * @see ClassPathEntry#ClassPathEntry(ClassPathEntry)
     */
    private ExpandingArchiveClassPathEntry(ExpandingArchiveClassPathEntry base) {
        super(base);
    }

    /**
     * Returns a {@link ClassLoaderResource} for the named resource if it
     * can be found in the archive identified by the path given at
     * construction time. Note that if the archive property would exist but is
     * not readable by the current session, no resource is returned.
     *
     * @param name The name of the resource to return. If the resource would
     *      be a class the name must already be modified to denote a valid
     *      path, that is dots replaced by slashes and the <code>.class</code>
     *      extension attached.
     *
     * @return The {@link ClassLoaderResource} identified by the name or
     *      <code>null</code> if no resource is found for that name.
     */
    public ClassLoaderResource getResource(final String name) {

        try {
            // find the resource for the name in the expanded archive contents
            Node jarContents = getJarContents();
            Item resItem = null;
            if (jarContents.hasNode(name)) {
                resItem = jarContents.getNode(name);
            } else if (jarContents.hasProperty(name)) {
                resItem = jarContents.getProperty(name);
            }

            // if the name resolved to an item, resolve the item to a
            // single-valued non-reference property
            Property resProp = (resItem != null)
                    ? Util.getProperty(resItem)
                    : null;

            // if found create the resource to return
            if (resProp != null) {
                return new ClassLoaderResource(this, name, resProp) {
                    public URL getURL() {
                        return ExpandingArchiveClassPathEntry.this.getURL(getName());
                    }

                    public URL getCodeSourceURL() {
                        return ExpandingArchiveClassPathEntry.this.getCodeSourceURL();
                    }

                    public Manifest getManifest() {
                        return ExpandingArchiveClassPathEntry.this.getManifest();
                    }

                    protected Property getExpiryProperty() {
                        return ExpandingArchiveClassPathEntry.this.getProperty();
                    }
                };
            }

            log.debug("getResource: resource {} not found in archive {}", name,
                path);

        } catch (RepositoryException re) {

            log.warn("getResource: problem accessing the archive {} for {}",
                new Object[] { path, name }, re.toString());

        }
        // invariant : not found or problem accessing the archive

        return null;
    }

    /**
     * Returns a <code>ClassPathEntry</code> with the same configuration as
     * this <code>ClassPathEntry</code>.
     * <p>
     * The <code>ExpandingArchiveClassPathEntry</code> class has internal state.
     * Therefore a new instance is created from the unmodifiable configuration
     * of this instance.
     */
    ClassPathEntry copy() {
        return new ExpandingArchiveClassPathEntry(this);
    }

    //----------- internal helper to find the entry ------------------------

    /**
     * Returns the root node of the expanded archive. If the archive's node
     * does not contain the expanded archive, it is expanded on demand. If the
     * archive has already been expanded, it is checked whether it is up to
     * date and expanded again if not.
     *
     * @throws RepositoryException if an error occurrs expanding the archive
     *      into the repository.
     */
    private Node getJarContents() throws RepositoryException {
        if (jarContents == null) {
            Node jarNode = null; // the node containing the jar file
            Node jarRoot = null; // the root node of the expanded contents
            try {
                Item jarItem = session.getItem(getPath());
                jarNode = (jarItem.isNode()) ? (Node) jarItem : jarItem.getParent();

                // if the jar been unpacked once, check for updated jar file,
                // which must be unpacked
                if (jarNode.isNodeType(TYPE_JARFILE)) {
                    long lastMod = Util.getLastModificationTime(getProperty());
                    long expanded =
                        jarNode.getProperty(PROP_EXPAND_DATE).getLong();

                    // get the content, remove if outdated or use if ok
                    jarRoot = jarNode.getNode(NODE_JARCONTENTS);

                    // if expanded content is outdated, remove it
                    if (lastMod <= expanded) {
                        jarRoot.remove();
                        jarRoot = null; // have to unpack below
                    }

                } else if (!jarNode.canAddMixin(TYPE_JARFILE)) {
                    // this is actually a problem, because I expect to be able
                    // to add the mixin node type due to checkExpandArchives
                    // having returned true earlier
                    throw new RepositoryException(
                        "Cannot unpack JAR file contents into "
                            + jarNode.getPath());

                } else {
                    jarNode.addMixin(TYPE_JARFILE);
                    jarNode.setProperty(PROP_EXPAND_DATE, Calendar.getInstance());
                }

                // if the content root is not set, unpack and save
                if (jarRoot == null) {
                    jarRoot = jarNode.addNode(NODE_JARCONTENTS, "nt:folder");
                    unpack(jarRoot);
                    jarNode.save();
                }

            } finally {

                // rollback changes on the jar node in case of problems
                if (jarNode != null && jarNode.isModified()) {
                    // rollback incomplete modifications
                    log.warn("Rolling back unsaved changes on JAR node {}",
                        getPath());

                    try {
                        jarNode.refresh(false);
                    } catch (RepositoryException re) {
                        log.warn("Cannot rollback changes after failure to " +
                                "expand " + getPath(), re);
                    }
                }
            }

            jarContents = jarRoot;
        }

        return jarContents;
    }

    /**
     * Expands the archive stored in the property of this class path entry into
     * the repositroy below the given <code>jarRoot</code> node.
     * <p>
     * This method leaves the subtree at and below <code>jarRoot</code> unsaved.
     * It is the task of the caller to save or rollback as appropriate.
     *
     * @param jarRoot The <code>Node</code> below which the archive is to be
     *      unpacked.
     *
     * @throws RepositoryException If an error occurrs creating the item
     *      structure to unpack the archive or if an error occurrs reading
     *      the archive.
     */
    private void unpack(Node jarRoot) throws RepositoryException {

        ZipInputStream zin = null;
        try {
            zin = new ZipInputStream(getProperty().getStream());
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                if (entry.isDirectory()) {
                    unpackFolder(jarRoot, entry.getName());
                } else {
                    unpackFile(jarRoot, entry, zin);
                }
                entry = zin.getNextEntry();
            }
        } catch (IOException ioe) {
            throw new RepositoryException(
                "Problem reading JAR contents of " + getPath(), ioe);
        } finally {
            // close the JAR stream if open
            if (zin != null) {
                try {
                    zin.close();
                } catch (IOException ignore) {}
            }
        }
    }

    /**
     * Makes sure a node exists at the <code>path</code> relative to
     * <code>root</code>. In other words, this method returns the node
     * <code>root.getNode(path)</code>, creating child nodes as required. Newly
     * created nodes are created with node type <code>nt:folder</code>.
     * <p>
     * If intermediate nodes or the actual node required already exist, they
     * must be typed such, that they may either accept child node creations
     * of type <code>nt:file</code> or <code>nt:folder</code>.
     *
     * @param root The <code>Node</code> relative to which a node representing
     *      a folder is to created if required.
     * @param path The path relative to <code>root</code> of the folder to
     *      ensure.
     *
     * @return The <code>Node</code> representing the folder below
     *      <code>root</code>.
     *
     * @throws RepositoryException If an error occurrs accessing the repository
     *      or creating missing node(s).
     */
    private Node unpackFolder(Node root, String path) throws RepositoryException {

        // remove trailing slash
        while (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }

        // quick check if the folder already exists
        if (root.hasNode(path)) {
            return root.getNode(path);
        }

        // go down and create the path
        StringTokenizer tokener = new StringTokenizer(path, "/");
        while (tokener.hasMoreTokens()) {
            String label = tokener.nextToken();
            if (root.hasNode(label)) {
                root = root.getNode(label);
            } else {
                root = root.addNode(label, "nt:folder");
            }
        }

        // return the final node
        return root;
    }

    /**
     * Creates a <code>nt:file</code> node with the path
     * <code>entry.getName()</code> relative to the <code>root</code> node. The
     * contents of the <code>jcr:content/jcr:data</code> property of the file
     * node is retrieved from <code>ins</code>.
     * <p>
     * The <code>jcr:content/jcr:lastModified</code> property is set to the
     * value of the <code>time</code> field of the <code>entry</code>. The
     * <code>jcr:content/jcr:mimeType</code> property is set to a best-effort
     * guess of the content type of the entry. To guess the content type, the
     * <code>java.net.URLConnection.guessContentType(String)</code> method
     * is called. If this results in no content type, the default
     * <code>application/octet-stream</code> is set.
     *
     * @param root The node relative to which the <code>nt:file</code> node
     *      is created.
     * @param entry The <code>ZipEntry</code> providing information on the
     *      file to be created. Namely the <code>name</code> and
     *      <code>time</code> fields are used.
     * @param ins The <code>InputStream</code> providing the data to be written
     *      to the <code>jcr:content/jcr:data</code> property.
     *
     * @throws RepositoryException If an error occurrs creating and filling
     *      the <code>nt:file</code> node.
     */
    private void unpackFile(Node root, ZipEntry entry, InputStream ins) throws RepositoryException {
        int slash = entry.getName().lastIndexOf('/');
        String label = entry.getName().substring(slash+1);
        Node parent = (slash <= 0)
                ? root
                : unpackFolder(root, entry.getName().substring(0, slash));

        // remove existing node (and all children by the way !!)
        if (parent.hasNode(label)) {
            parent.getNode(label).remove();
        }

        // prepare property values
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(entry.getTime());
        String mimeType = URLConnection.guessContentTypeFromName(label);
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        // create entry nodes
        Node ntFile = parent.addNode(label, "nt:file");
        Node content = ntFile.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", mimeType);
        content.setProperty("jcr:data", ins);
        content.setProperty("jcr:lastModified", lastModified);
    }

    /**
     * Checks whether it is possible to use this class for archive class path
     * entries in the workspace (and repository) to which the <code>session</code>
     * provides access.
     * <p>
     * This method works as follows. If the node type <code>rep:jarFile</code>
     * is defined in the session's repository, <code>true</code> is immediately
     * returned. If an error checking for the node type, <code>false</code> is
     * immediately returned.
     * <p>
     * If the node type is not defined, the
     * {@link NodeTypeSupport#registerNodeType(Workspace)} method is called
     * to register the node type. Any errors occurring while calling or
     * executing this method is logged an <code>false</code> is returned.
     * Otherwise, if node type registration succeeded, <code>true</code> is
     * returned.
     * <p>
     * This method is synchronized such that two paralell threads do not try
     * to create the node, which might yield wrong negatives.
     *
     * @param session The <code>Session</code> providing access to the
     *      repository.
     *
     * @return <code>true</code> if this class can be used to handle archive
     *      class path entries. See above for a description of the test used.
     */
    /* package */ synchronized static boolean canExpandArchives(Session session) {

        // quick check for the node type, succeed if defined
        try {
            session.getWorkspace().getNodeTypeManager().getNodeType(TYPE_JARFILE);
            log.debug("Required node type exists, can expand archives");
            return true;
        } catch (NoSuchNodeTypeException nst) {
            log.debug("Required node types does not exist, try to define");
        } catch (RepositoryException re) {
            log.info("Cannot check for required node type, cannot expand " +
                    "archives", re);
            return false;
        }

        try {
            Workspace workspace = session.getWorkspace();
            return NodeTypeSupport.registerNodeType(workspace);
        } catch (Throwable t) {
            // Prevent anything from hapening if node type registration fails
            // due to missing libraries or other errors
            log.info("Error registering node type", t);
        }

        // fallback to failure
        return false;
    }
}
