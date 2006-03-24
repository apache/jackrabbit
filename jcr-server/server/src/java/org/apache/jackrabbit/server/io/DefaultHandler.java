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
package org.apache.jackrabbit.server.io;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavResource;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Property;
import javax.jcr.Item;
import javax.jcr.NodeIterator;
import java.util.Calendar;
import java.util.Date;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>DefaultHandler</code> implements a simple IOHandler that creates 'file'
 * and 'folder' nodes. This handler will create the following nodes:
 * <ul>
 * <li>New <b>Collection</b>: creates a new node with the {@link #getCollectionNodeType()
 * collection nodetype}. The name of the node corresponds to the systemId
 * present on the import context.</li>
 *
 * <li>New <b>Non-Collection</b>: first creates a new node with the {@link #getNodeType()
 * non-collection nodetype}. The name of the node corresponds to the systemId
 * present on the import context. Below it creates a node with name
 * {@link JcrConstants#JCR_CONTENT jcr:content} and the nodetype specified
 * by {@link #getContentNodeType()}.</li>
 * </ul>
 * <p/>
 * Import of the content:<br>
 * The content is imported to the {@link JcrConstants#JCR_DATA} property of the
 * content node. By default this handler will fail on a attempt to create/replace
 * a collection if {@link ImportContext#hasStream()} is <code>true</code>.
 * Subclasses therefore should provide their own {@link #importData(ImportContext, boolean, Node)
 * importData} method, that handles the data according their needs.
 */
public class DefaultHandler implements IOHandler {

    private static Logger log = Logger.getLogger(DefaultHandler.class);

    private String collectionNodetype = JcrConstants.NT_FOLDER;
    private String defaultNodetype = JcrConstants.NT_FILE;
    private String contentNodetype = JcrConstants.NT_RESOURCE;

    private IOManager ioManager;

    /**
     * Creates a new <code>DefaultHandler</code> with default nodetype definitions:<br>
     * <ul>
     * <li>Nodetype for Collection: {@link JcrConstants#NT_FOLDER nt:folder}</li>
     * <li>Nodetype for Non-Collection: {@link JcrConstants#NT_FILE nt:file}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_RESOURCE nt:resource}</li>
     * </ul>
     *
     * @param ioManager
     */
    public DefaultHandler(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    /**
     * Creates a new <code>DefaultHandler</code>. Please note that the specified
     * nodetypes must match the definitions of the defaults.
     *
     * @param ioManager
     * @param collectionNodetype
     * @param defaultNodetype
     * @param contentNodetype
     */
    public DefaultHandler(IOManager ioManager, String collectionNodetype, String defaultNodetype, String contentNodetype) {
        this.ioManager = ioManager;
        
        this.collectionNodetype = collectionNodetype;
        this.defaultNodetype = defaultNodetype;
        this.contentNodetype = contentNodetype;
    }

    /**
     * @see IOHandler#getIOManager()
     */
    public IOManager getIOManager() {
        return ioManager;
    }

    /**
     * @see IOHandler#getName()
     */
    public String getName() {
        return getClass().getName();
    }

    /**
     * @see IOHandler#canImport(ImportContext, boolean)
     */
    public boolean canImport(ImportContext context, boolean isCollection) {
        if (context == null || context.isCompleted()) {
            return false;
        }
        Item contextItem = context.getImportRoot();
        return contextItem != null && contextItem.isNode() && context.getSystemId() != null;
    }

    /**
     * @see IOHandler#canImport(ImportContext, DavResource)
     */
    public boolean canImport(ImportContext context, DavResource resource) {
        if (resource == null) {
            return false;
        }
        return canImport(context, resource.isCollection());
    }

    /**
     * @see IOHandler#importContent(ImportContext, boolean)
     */
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        if (!canImport(context, isCollection)) {
            throw new IOException(getName() + ": Cannot import " + context.getSystemId());
        }

        boolean success = false;
        try {
            Node contentNode = getContentNode(context, isCollection);
            success = importData(context, isCollection, contentNode);
            if (success) {
                success = importProperties(context, isCollection, contentNode);
            }
        } catch (RepositoryException e) {
            success = false;
            throw new IOException(e.getMessage());
        } finally {
            // revert any changes made in case the import failed.
            if (!success) {
                try {
                    context.getImportRoot().refresh(false);
                } catch (RepositoryException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
        return success;
    }

    /**
     * @see IOHandler#importContent(ImportContext, DavResource)
     */
    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        if (!canImport(context, resource)) {
            throw new IOException(getName() + ": Cannot import " + context.getSystemId());
        }
        return importContent(context, resource.isCollection());
    }

    /**
     * Imports the data present on the import context to the specified content
     * node.
     *
     * @param context
     * @param isCollection
     * @param contentNode
     * @return
     * @throws IOException
     */
    protected boolean importData(ImportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        InputStream in = context.getInputStream();
        if (in != null) {
            // NOTE: with the default folder-nodetype (nt:folder) no inputstream
            // is allowed. setting the property would therefore fail.
            if (isCollection) {
                return false;
            }
            try {
                contentNode.setProperty(JcrConstants.JCR_DATA, in);
            } finally {
                in.close();
            }
        }
        // success if no data to import.
        return true;
    }

    /**
     * Imports the properties present on the specified context to the content
     * node.
     *
     * @param context
     * @param isCollection
     * @param contentNode
     * @return
     */
    protected boolean importProperties(ImportContext context, boolean isCollection, Node contentNode) {
        try {
            // if context-mimetype is null -> remove the property
            contentNode.setProperty(JcrConstants.JCR_MIMETYPE, context.getMimeType());
        } catch (RepositoryException e) {
            // ignore: property may not be present on the node
        }
        try {
            // if context-encoding is null -> remove the property
            contentNode.setProperty(JcrConstants.JCR_ENCODING, context.getEncoding());
        } catch (RepositoryException e) {
            // ignore: property may not be present on the node
        }
        try {
            Calendar lastMod = Calendar.getInstance();
            if (context.getModificationTime() != IOUtil.UNDEFINED_TIME) {
                lastMod.setTimeInMillis(context.getModificationTime());
            } else {
                lastMod.setTime(new Date());
            }
            contentNode.setProperty(JcrConstants.JCR_LASTMODIFIED, lastMod);
        } catch (RepositoryException e) {
            // ignore: property may not be present on the node.
            // deliberately not rethrowing as IOException.
        }
        return true;
    }

    /**
     * Retrieves/creates the node that will be used to import properties and
     * data. In case of a non-collection this includes and additional content node
     * to be created beside the 'file' node.<br>
     * Please note: If the jcr:content node already exists and contains child
     * nodes, those will be removed in order to make sure, that the import
     * really replaces the existing content of the file-node.
     *
     * @param context
     * @param isCollection
     * @return
     * @throws RepositoryException
     */
    protected Node getContentNode(ImportContext context, boolean isCollection) throws RepositoryException {
        Node parentNode = (Node)context.getImportRoot();
        String name = context.getSystemId();
        if (parentNode.hasNode(name)) {
            parentNode = parentNode.getNode(name);
        } else {
            String ntName = (isCollection) ? getCollectionNodeType() : getNodeType();
            parentNode = parentNode.addNode(name, ntName);
        }
        Node contentNode;
        if (isCollection) {
            contentNode = parentNode;
        } else {
            if (parentNode.hasNode(JcrConstants.JCR_CONTENT)) {
                contentNode = parentNode.getNode(JcrConstants.JCR_CONTENT);
                // remove all entries in the jcr:content since replacing content
                // includes properties (DefaultHandler) and nodes (e.g. ZipHandler)
                if (contentNode.hasNodes()) {
                    NodeIterator it = contentNode.getNodes();
                    while (it.hasNext()) {
                        it.nextNode().remove();
                    }
                }
            } else {
                contentNode = parentNode.addNode(JcrConstants.JCR_CONTENT, getContentNodeType());
            }
        }
        return contentNode;
    }

    /**
     * Returns true if the export root is a node and if it contains a child node
     * with name {@link JcrConstants#JCR_CONTENT jcr:content} in case this
     * export is not intended for a collection.
     *
     * @return true if the export root is a node. If the specified boolean paramter
     * is false (not a collection export) the given export root must contain a
     * child node with name {@link JcrConstants#JCR_CONTENT jcr:content}.
     *
     * @see IOHandler#canExport(ExportContext, boolean)
     */
    public boolean canExport(ExportContext context, boolean isCollection) {
        if (context == null || context.isCompleted()) {
            return false;
        }
        Item exportRoot = context.getExportRoot();
        boolean success = exportRoot != null && exportRoot.isNode();
        if (success && !isCollection) {
            try {
                Node n = ((Node)exportRoot);
                success = n.hasNode(JcrConstants.JCR_CONTENT);
            } catch (RepositoryException e) {
                // should never occur.
                success = false;
            }
        }
        return success;
    }

    /**
     * @see IOHandler#canExport(ExportContext, DavResource)
     */
    public boolean canExport(ExportContext context, DavResource resource) {
        if (resource == null) {
            return false;
        }
        return canExport(context, resource.isCollection());
    }

    /**
     * Retrieves the content node that will be used for exporting properties and
     * data and calls the corresponding methods.
     *
     * @param context
     * @param isCollection
     * @see #exportProperties(ExportContext, boolean, Node)
     * @see #exportData(ExportContext, boolean, Node)
     */
    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException {
        if (!canExport(context, isCollection)) {
            throw new IOException(getName() + ": Cannot export " + context.getExportRoot());
        }
        try {
            Node contentNode = getContentNode(context, isCollection);
            exportProperties(context, isCollection, contentNode);
            if (context.hasStream()) {
                exportData(context, isCollection, contentNode);
            } // else: missing stream. ignore.
            return true;
        } catch (RepositoryException e) {
            // should never occur, since the proper structure of the content
            // node must be asserted in the 'canExport' call.
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Same as (@link #exportContent(ExportContext, boolean)} where the boolean
     * values is defined by {@link DavResource#isCollection()}.
     *
     * @see IOHandler#exportContent(ExportContext, DavResource)
     */
    public boolean exportContent(ExportContext context, DavResource resource) throws IOException {
        if (!canExport(context, resource)) {
            throw new IOException(getName() + ": Cannot export " + context.getExportRoot());
        }
        return exportContent(context, resource.isCollection());
    }

    /**
     * Checks if the given content node contains a jcr:data property
     * and spools its value to the output stream fo the export context.<br>
     * Please note, that subclasses that define a different structure of the
     * content node should create their own
     * {@link #exportData(ExportContext, boolean, Node) exportData} method.
     *
     * @param context
     * @param isCollection
     * @param contentNode
     * @throws IOException
     */
    protected void exportData(ExportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        if (contentNode.hasProperty(JcrConstants.JCR_DATA)) {
            Property p = contentNode.getProperty(JcrConstants.JCR_DATA);
            IOUtil.spool(p.getStream(), context.getOutputStream());
        } // else: stream undefined -> contentlength was not set
    }

    /**
     * Retrieves mimetype, encoding and modification time from the content node.
     * The content length is determined by the length of the jcr:data property
     * if it is present. The creation time however is retrieved from the parent
     * node (in case of isCollection == false only).
     *
     * @param context
     * @param isCollection
     * @param contentNode
     */
    protected void exportProperties(ExportContext context, boolean isCollection, Node contentNode) throws IOException {
        try {
            // only non-collections: 'jcr:created' is present on the parent 'fileNode' only
            if (!isCollection && contentNode.getDepth() > 0 && contentNode.getParent().hasProperty(JcrConstants.JCR_CREATED)) {
                context.setCreationTime(contentNode.getParent().getProperty(JcrConstants.JCR_CREATED).getValue().getLong());
            }

            long length = IOUtil.UNDEFINED_LENGTH;
            if (contentNode.hasProperty(JcrConstants.JCR_DATA)) {
                Property p = contentNode.getProperty(JcrConstants.JCR_DATA);
                length = p.getLength();
                context.setContentLength(length);
            }

            String mimeType = null;
            String encoding = null;
            if (contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                mimeType = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
            }
            if (contentNode.hasProperty(JcrConstants.JCR_ENCODING)) {
                encoding = contentNode.getProperty(JcrConstants.JCR_ENCODING).getString();
                // ignore "" encodings (although this is avoided during import)
                if ("".equals(encoding)) {
                    encoding = null;
                }
            }
            context.setContentType(mimeType, encoding);

            long modTime = IOUtil.UNDEFINED_TIME;
            if (contentNode.hasProperty(JcrConstants.JCR_LASTMODIFIED)) {
                modTime = contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED).getLong();
                context.setModificationTime(modTime);
            } else {
                context.setModificationTime(System.currentTimeMillis());
            }

            if (length > IOUtil.UNDEFINED_LENGTH && modTime > IOUtil.UNDEFINED_TIME) {
                String etag = "\"" + length + "-" + modTime + "\"";
                context.setETag(etag);
            }
        } catch (RepositoryException e) {
            // should never occur
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Retrieves the content node that contains the data to be exported. In case
     * isCollection is true, this corresponds to the export root. Otherwise there
     * must be a child node with name {@link JcrConstants#JCR_CONTENT jcr:content}.
     *
     * @param context
     * @param isCollection
     * @return content node used for the export
     * @throws RepositoryException
     */
    protected Node getContentNode(ExportContext context, boolean isCollection) throws RepositoryException {
        Node contentNode = (Node)context.getExportRoot();
        // 'file' nodes must have an jcr:content child node (see canExport)
        if (!isCollection) {
            contentNode = contentNode.getNode(JcrConstants.JCR_CONTENT);
        }
        return contentNode;
    }

    /**
     * Name of the nodetype to be used to create a new collection node (folder)
     *
     * @return nodetype name
     */
    protected String getCollectionNodeType() {
        return collectionNodetype;
    }

    /**
     * Name of the nodetype to be used to create a new non-collection node (file)
     *
     * @return nodetype name
     */
    protected String getNodeType() {
        return defaultNodetype;
    }

    /**
     * Name of the nodetype to be used to create the content node below
     * a new non-collection node, whose name is always {@link JcrConstants#JCR_CONTENT
     * jcr:content}.
     *
     * @return nodetype name
     */
    protected String getContentNodeType() {
        return contentNodetype;
    }
}