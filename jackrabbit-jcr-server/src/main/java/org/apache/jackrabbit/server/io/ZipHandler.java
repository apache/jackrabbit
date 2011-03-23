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

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * <code>ZipHandler</code> imports and extracts Zip files and exported nodes
 * (an their subnodes) to a Zip file. Please not that for the export the selected
 * export root must have the property {@link #ZIP_MIMETYPE} defined with its
 * content. Furthermore the content must not represent a zip-file that has
 * been imported to a binary {@link Property property}, which is properly
 * handled by the {@link DefaultHandler}.
 */
public class ZipHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(ZipHandler.class);

    /**
     * the zip mimetype
     */
    public static final String ZIP_MIMETYPE = "application/zip";

    private boolean intermediateSave;

    /**
     * Creates a new <code>ZipHandler</code> with default nodetype definitions
     * and without setting the IOManager.
     *
     * @see IOHandler#setIOManager(IOManager)
     */
    public ZipHandler() {
    }

    /**
     * Creates a new <code>ZipHandler</code> with default nodetype definitions:<br>
     * <ul>
     * <li>Nodetype for Collection: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
     * <li>Nodetype for Non-Collection: {@link JcrConstants#NT_FILE nt:file}</li>
     * <li>Nodetype for Non-Collection content: {@link JcrConstants#NT_UNSTRUCTURED nt:unstructured}</li>
     * </ul>
     *
     * @param ioManager
     * @throws IllegalArgumentException if the specified <code>IOManager</code>
     * is <code>null</code>
     */
    public ZipHandler(IOManager ioManager) {
        this(ioManager, JcrConstants.NT_FOLDER, JcrConstants.NT_FILE, JcrConstants.NT_UNSTRUCTURED);
    }

    /**
     * Creates a new <code>ZipHandler</code>
     *
     * @param ioManager
     * @param collectionNodetype
     * @param defaultNodetype
     * @param contentNodetype
     * @throws IllegalArgumentException if the specified <code>IOManager</code>
     * is <code>null</code>
     */
    public ZipHandler(IOManager ioManager, String collectionNodetype, String defaultNodetype, String contentNodetype) {
        super(ioManager, collectionNodetype, defaultNodetype, contentNodetype);
        if (ioManager == null) {
            throw new IllegalArgumentException("The IOManager must not be null.");
        }
    }

    /**
     * If set to <code>true</code> the import root will be {@link Item#save() saved}
     * after every imported zip entry. Note however, that this removes the possibility
     * to revert all modifications if the import cannot be completed successfully.
     * By default the intermediate save is disabled.
     *
     * @param intermediateSave
     */
    public void setIntermediateSave(boolean intermediateSave) {
        this.intermediateSave = intermediateSave;
    }

    /**
     * @see IOHandler#canImport(ImportContext, boolean)
     */
    @Override
    public boolean canImport(ImportContext context, boolean isCollection) {
        if (context == null || context.isCompleted()) {
            return false;
        }
        boolean isZip = ZIP_MIMETYPE.equals(context.getMimeType());
        return isZip && context.hasStream() && super.canImport(context, isCollection);
    }

    /**
     * @see DefaultHandler#importData(ImportContext, boolean, Node)
     */
    @Override
    protected boolean importData(ImportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        boolean success = true;
        InputStream in = context.getInputStream();
        ZipInputStream zin = new ZipInputStream(in);
        try {
            ZipEntry entry;
            while ((entry=zin.getNextEntry())!=null && success) {
                success = importZipEntry(zin, entry, context, contentNode);
                zin.closeEntry();
            }
        } finally {
            zin.close();
            in.close();
        }
        return success;
    }

    /**
     * @see IOHandler#canExport(ExportContext, boolean)
     */
    @Override
    public boolean canExport(ExportContext context, boolean isCollection) {
        if (super.canExport(context, isCollection)) {
            // mimetype must be application/zip
            String mimeType = null;
            // if zip-content has not been extracted -> delegate to some other handler
            boolean hasDataProperty = false;
            try {
                Node contentNode = getContentNode(context, isCollection);
                // jcr:data property indicates that the zip-file has been imported as binary (not extracted)
                hasDataProperty = contentNode.hasProperty(JcrConstants.JCR_DATA);
                if (contentNode.hasProperty(JcrConstants.JCR_MIMETYPE)) {
                    mimeType  = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
                } else {
                    mimeType = detect(context.getExportRoot().getName());
                }
            } catch (RepositoryException e) {
                // ignore and return false
            }
            return ZIP_MIMETYPE.equals(mimeType) && !hasDataProperty;
        }
        return false;
    }

    /**
     * @see DefaultHandler#exportData(ExportContext,boolean,Node)
     */
    @Override
    protected void exportData(ExportContext context, boolean isCollection, Node contentNode) throws IOException, RepositoryException {
        ZipOutputStream zout = new ZipOutputStream(context.getOutputStream());
        zout.setMethod(ZipOutputStream.DEFLATED);
        try {
            int pos = contentNode.getPath().length();
            exportZipEntry(context, zout, contentNode, pos > 1 ? pos+1 : pos);
        } finally {
            zout.finish();
        }
    }

    /**
     * If the specified node is the defined non-collection nodetype a new
     * Zip entry is created and the exportContent is called on the IOManager
     * defined with this handler. If in contrast the specified node does not
     * represent a non-collection this method is called recursively for all
     * child nodes.
     *
     * @param context
     * @param zout
     * @param node
     * @param pos
     * @throws IOException
     */
    private void exportZipEntry(ExportContext context, ZipOutputStream zout, Node node, int pos) throws IOException{
        try {
            if (node.isNodeType(getNodeType())) {
                ZipEntryExportContext subctx = new ZipEntryExportContext(node, zout, context, pos);
                // try if iomanager can treat node as zip entry otherwise recurs.
                zout.putNextEntry(subctx.entry);
                getIOManager().exportContent(subctx, false);
            } else {
                // recurs
                NodeIterator niter = node.getNodes();
                while (niter.hasNext()) {
                    exportZipEntry(context, zout, niter.nextNode(), pos);
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
            // should never occur
        }
    }

    /**
     * Creates a new sub context for the specified Zip entry and passes it to
     * the IOManager defined with this handler.
     *
     * @param zin
     * @param entry
     * @param context
     * @param node
     * @return
     * @throws RepositoryException
     * @throws IOException
     */
    private boolean importZipEntry(ZipInputStream zin, ZipEntry entry, ImportContext context, Node node) throws RepositoryException, IOException {
        boolean success = false;
        log.debug("entry: " + entry.getName() + " size: " + entry.getSize());
        if (entry.isDirectory()) {
            IOUtil.mkDirs(node, makeValidJCRPath(entry.getName(), false), getCollectionNodeType());
            success = true;
        } else {
            // import zip entry as file
            BoundedInputStream bin = new BoundedInputStream(zin);
            bin.setPropagateClose(false);
            ImportContext entryContext = new ZipEntryImportContext(context, entry, bin, node);

            // let the iomanager deal with the individual entries.
            IOManager ioManager = getIOManager();
            success = (ioManager != null) ? ioManager.importContent(entryContext, false) : false;

            // intermediate save in order to avoid problems with large zip files
            if (intermediateSave) {
                context.getImportRoot().save();
            }
        }
        return success;
    }

    /**
     * Creates a valid jcr label from the given one
     *
     * @param label
     * @return
     */
    private static String makeValidJCRPath(String label, boolean appendLeadingSlash) {
        if (appendLeadingSlash && !label.startsWith("/")) {
            label = "/" + label;
        }
        StringBuffer ret = new StringBuffer(label.length());
        for (int i=0; i<label.length(); i++) {
            char c = label.charAt(i);
            if (c=='*' || c=='\'' || c=='\"') {
                c='_';
            /* not quite correct: [] may be the index of a previously exported item. */
            } else if (c=='[') {
                c='(';
            } else if (c==']') {
                c=')';
            }
            ret.append(c);
        }
        return ret.toString();
    }

    //--------------------------------------------------------< inner class >---
    /**
     * Inner class used to create subcontexts for the import of the individual
     * zip file entries.
     */
    private class ZipEntryImportContext extends ImportContextImpl {

        private final Item importRoot;
        private final ZipEntry entry;

        private ZipEntryImportContext(ImportContext context, ZipEntry entry, BoundedInputStream bin, Node contentNode) throws IOException, RepositoryException {
            super(contentNode, Text.getName(makeValidJCRPath(entry.getName(), true)),
                    null, bin, context.getIOListener(), getIOManager().getDetector());
            this.entry = entry;
            String path = makeValidJCRPath(entry.getName(), true);
            importRoot = IOUtil.mkDirs(contentNode, Text.getRelativeParent(path, 1), getCollectionNodeType());
        }

        @Override
        public Item getImportRoot() {
            return importRoot;
        }

        @Override
        public long getModificationTime() {
            return entry.getTime();
        }

        @Override
        public long getContentLength() {
            return entry.getSize();
        }
    }

    /**
     * Inner class used to create subcontexts for the export of the individual
     * zip file entries.
     */
    private static class ZipEntryExportContext extends AbstractExportContext {

        private ZipEntry entry;
        private OutputStream out;

        private ZipEntryExportContext(Item exportRoot, OutputStream out, ExportContext context, int pos) {
            super(exportRoot, out != null, context.getIOListener());
            this.out = out;
            try {
                String entryPath = (exportRoot.getPath().length() > pos) ? exportRoot.getPath().substring(pos) : "";
                entry = new ZipEntry(entryPath);
            } catch (RepositoryException e) {
                // should never occur
            }
        }

        /**
         * Returns the Zip output stream. Note, that this context does not
         * deal properly with multiple IOHandlers writing to the stream.
         *
         * @return
         */
        public OutputStream getOutputStream() {
            return out;
        }

        public void setContentType(String mimeType, String encoding) {
            if (entry != null) {
                entry.setComment(mimeType);
            }
        }

        public void setContentLanguage(String contentLanguage) {
            // ignore
        }

        public void setContentLength(long contentLength) {
            if (entry != null) {
                entry.setSize(contentLength);
            }
        }

        public void setCreationTime(long creationTime) {
            // ignore
        }

        public void setModificationTime(long modificationTime) {
            if (entry != null) {
                entry.setTime(modificationTime);
            }
        }

        public void setETag(String etag) {
            // ignore
        }

        public void setProperty(Object propertyName, Object propertyValue) {
            // ignore
        }
    }
}
