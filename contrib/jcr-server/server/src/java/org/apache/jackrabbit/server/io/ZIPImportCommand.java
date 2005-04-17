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
package org.apache.jackrabbit.server.io;

import org.apache.log4j.Logger;
import org.apache.jackrabbit.webdav.util.Text;
import org.apache.jackrabbit.JcrConstants;
import org.apache.commons.chain.Context;
import org.apache.commons.chain.Command;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * This Class implements an import command that reads entries from a zip input
 * stream and delegates the extracted file back to the import chain.
 */
public class ZIPImportCommand implements Command, JcrConstants {

    /** the default logger */
    private static final Logger log = Logger.getLogger(ZIPImportCommand.class);

    /**
     * the zip content type
     */
    public static final String ZIP_CONTENT_TYPE = "application/zip";

    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(Context context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It checks if this command can handle the content
     * type and delegates it to {@link #importResource}. If the import is
     * successfull, the input stream of the importcontext is cleared.
     *
     * @param context the import context
     * @return false
     * @throws Exception if an error occurrs
     */
    public boolean execute(ImportContext context) throws Exception {
        Node parentNode = context.getNode();
        InputStream in = context.getInputStream();
        if (in == null) {
            // assume already consumed
            return false;
        }
        if (!canHandle(context.getContentType())) {
            // ignore imports
            return false;
        }
        importResource(parentNode, in);
        context.setInputStream(null);
        return true;
    }

    /**
     * Imports a resource by extracting the input stream and delegating to
     * import chain.
     *
     * @param parentNode the parent node
     * @param in the input stream
     * @throws Exception in an error occurrs
     */
    private void importResource(Node parentNode, InputStream in)
            throws Exception {

        // assuming zip content
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry=zin.getNextEntry())!=null) {
            log.info("entry: " + entry.getName() + " size: " + entry.getSize());
            if (entry.isDirectory()) {
                mkDirs(parentNode, Text.makeValidJCRPath(entry.getName()));
                zin.closeEntry();
            } else {
                String path = Text.makeValidJCRPath(entry.getName());
                if (path.charAt(0)!='/') {
                    path  = "/" + path;
                }
                Node parent = mkDirs(parentNode, Text.getRelativeParent(path, 1));

                BoundedInputStream bin = new BoundedInputStream(zin);
                bin.setPropagateClose(false);

                ImportContext subctx = new ImportContext(parent);
                subctx.setInputStream(bin);
                subctx.setSystemId(Text.getLabel(path));
                subctx.setModificationTime(entry.getTime());
                ImportResourceChain.getChain().execute(subctx);
                zin.closeEntry();
            }
        }
        zin.close();
    }

    /**
     * Creates collection recursively.
     *
     * @param root
     * @param relPath
     * @return
     * @throws RepositoryException
     */
    private Node mkDirs(Node root, String relPath) throws RepositoryException {
        String[] seg = Text.explode(relPath, '/');
        for (int i=0; i< seg.length; i++) {
            if (!root.hasNode(seg[i])) {
                // not quite correct
                ImportContext subctx = new ImportContext(root);
                subctx.setSystemId(seg[i]);
                try {
                    ImportCollectionChain.getChain().execute(subctx);
                } catch (Exception e) {
                    throw new RepositoryException(e);
                }
            }
            root = root.getNode(seg[i]);
        }
        return root;
    }

    /**
     * Returns <code>true</code> if the given content type is equal to
     * {@link #ZIP_CONTENT_TYPE}.
     * @param contentType the content type to check.
     * @return <code>true</code> if equal to {@link #ZIP_CONTENT_TYPE}.
     */
    public boolean canHandle(String contentType) {
        return ZIP_CONTENT_TYPE.equals(contentType);
    }
}
