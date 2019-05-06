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
package org.apache.jackrabbit.server.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;

/**
 * <code>XmlHandler</code> imports xml files and exports nodes that have
 * the proper {@link #XML_MIMETYPE} defined with their content. The export is
 * performed by running a {@link Session#exportDocumentView(String, OutputStream, boolean, boolean)
 * document view export} for the content of the export root defined with the
 * specified {@link ExportContext}.
 * <p>
 * Please note that this handler is not suited for a generic system or document
 * view import/export of {@link Node}s because an extra root node is always
 * created during import and expected during export, respectively.
 */
public class XmlHandler extends DefaultHandler {

    /**
     * the xml mimetype
     */
    public static final String XML_MIMETYPE = "text/xml";

    /**
     * the alternative xml mimetype. tika detects xml as this.
     */
    public static final String XML_MIMETYPE_ALT = "application/xml";

    private static final Set<String> supportedTypes;
    static {
        supportedTypes = new HashSet<String>();
        supportedTypes.add(XML_MIMETYPE);
        supportedTypes.add(XML_MIMETYPE_ALT);
    }


    /**
     * Creates a new <code>XmlHandler</code> with default nodetype definitions
     * and without setting the IOManager.
     *
     * @see IOHandler#setIOManager(IOManager)
     */
    public XmlHandler() {
    }

    /**
     * Creates a new <code>XmlHandler</code> with default nodetype definitions:<br>
     * <ul>
     * <li>Nodetype for Collection: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
     * <li>Nodetype for Non-Collection: {@link JcrConstants#NT_FILE nt:file}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
     * </ul>
     *
     * @param ioManager
     */
    public XmlHandler(IOManager ioManager) {
        super(ioManager, JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_FILE, JcrConstants.NT_UNSTRUCTURED);
    }

    /**
     * Creates a new <code>XmlHandler</code>
     *
     * @param ioManager
     * @param collectionNodetype
     * @param defaultNodetype
     * @param contentNodetype
     */
    public XmlHandler(IOManager ioManager, String collectionNodetype, String defaultNodetype, String contentNodetype) {
        super(ioManager, collectionNodetype, defaultNodetype, contentNodetype);
    }

    /**
     * @see IOHandler#canImport(ImportContext, boolean)
     */
    @Override
    public boolean canImport(ImportContext context, boolean isCollection) {
        return !(context == null || context.isCompleted())
                && supportedTypes.contains(context.getMimeType())
                && context.hasStream()
                && context.getContentLength() > 0
                && super.canImport(context, isCollection);
    }

    /**
     * @see DefaultHandler#importData(ImportContext, boolean, Node)
     */
    @Override
    protected boolean importData(ImportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        InputStream in = context.getInputStream();
        int uuidBehavior = (isCollection)
            ? ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING
            : ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        try {
            contentNode.getSession().importXML(contentNode.getPath(), in, uuidBehavior);
        } finally {
            in.close();
        }
        return true;
    }

    /**
     * @see DefaultHandler#importProperties(ImportContext, boolean, Node)
     */
    @Override
    protected boolean importProperties(ImportContext context, boolean isCollection, Node contentNode) {
        boolean success = super.importProperties(context, isCollection, contentNode);
        if (success) {
            // encoding: always UTF-8 for the xml import
            try {
                contentNode.setProperty(JcrConstants.JCR_ENCODING, "UTF-8");
            } catch (RepositoryException e) {
                // ignore, since given nodetype could not allow encoding
                // deliberately not re-throwing an IOException.
            }
        }
        return success;
    }

    /**
     * {@inheritDoc}
     *
     * @return <code>true</code>, always.
     */
    @Override
    protected boolean forceCompatibleContentNodes() {
        return true;
    }

    /**
     * @see IOHandler#canExport(ExportContext, boolean)
     */
    @Override
    public boolean canExport(ExportContext context, boolean isCollection) {
        if (super.canExport(context, isCollection)) {
            String mimeType = null;
            try {
                Node contentNode = getContentNode(context, isCollection);
                if (contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                    mimeType = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                } else {
                    mimeType = detect(context.getExportRoot().getName());
                }
            } catch (RepositoryException e) {
                // ignore and return false
            }
            return XML_MIMETYPE.equals(mimeType);
        }
        return false;
    }

    /**
     * @see DefaultHandler#exportData(ExportContext, boolean, Node)
     */
    @Override
    protected void exportData(ExportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        // first child of content is XML document root
        if (contentNode.getNodes().hasNext()) {
            contentNode = contentNode.getNodes().nextNode();
        }
        OutputStream out = context.getOutputStream();
        contentNode.getSession().exportDocumentView(contentNode.getPath(), out, true, false);
    }

    /**
     * @see DefaultHandler#exportProperties(ExportContext, boolean, Node)
     */
    @Override
    protected void exportProperties(ExportContext context, boolean isCollection, Node contentNode) throws IOException {
        super.exportProperties(context, isCollection, contentNode);
        // set mimetype if the content node did not provide the
        // jcr property (thus not handled by super class)
        try {
            if (!contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                context.setContentType("text/xml", "UTF-8");
            }
        } catch (RepositoryException e) {
            // should never occur
            throw new IOException(e.getMessage());
        }
    }
}
