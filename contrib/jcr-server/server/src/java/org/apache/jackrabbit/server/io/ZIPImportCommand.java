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
import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import java.io.InputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

/**
 * This Class implements an import command that reads entries from a zip input
 * stream and delegates the extracted file back to the import chain.
 */
public class ZIPImportCommand extends AbstractCommand {

    /** the default logger */
    private static final Logger log = Logger.getLogger(ZIPImportCommand.class);

    /**
     * the zip content type
     */
    public static final String ZIP_CONTENT_TYPE = "application/zip";

    /**
     * flag, indicating if zip should be extracted recusively
     */
    private boolean recursive = false;
            
    /**
     * Executes this command by calling {@link #importResource} if
     * the given context is of the correct class.
     *
     * @param context the (import) context.
     * @return the return value of the delegated method or false;
     * @throws Exception in an error occurrs
     */
    public boolean execute(AbstractContext context) throws Exception {
        if (context instanceof ImportContext) {
            return execute((ImportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Override default behaviour and abort chain if input is processed.
     * @param context
     * @return
     * @throws Exception
     */
    public boolean execute(ImportContext context) throws Exception {
        if (!canHandle(context.getContentType())) {
            // ignore imports
            return false;
        }
        // disable this command for further subcommands if recursive is false
        context.enableCommand(getId(), recursive);

        Node parentNode = context.getNode();
        InputStream in = context.getInputStream();
        if (in == null) {
            // assume already consumed
            return false;
        }
        if (importResource(context, parentNode, in)) {
            context.setInputStream(null);
            return true;
        }
        return false;
    }

    /**
     * Imports a resource by extracting the input stream and delegating to
     * import chain.
     *
     * @param parentNode the parent node
     * @param in the input stream
     * @throws Exception in an error occurrs
     */

    public boolean importResource(ImportContext context, Node parentNode, InputStream in)
            throws Exception {

        // assuming zip content
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry entry;
        while ((entry=zin.getNextEntry())!=null) {
            log.info("entry: " + entry.getName() + " size: " + entry.getSize());
            if (entry.isDirectory()) {
                AbstractImportCommand.mkDirs(context, parentNode, makeValidJCRPath(entry.getName()));
                zin.closeEntry();
            } else {
                String path = makeValidJCRPath(entry.getName());
                if (path.charAt(0)!='/') {
                    path  = "/" + path;
                }
                Node parent = AbstractImportCommand.mkDirs(context, parentNode, Text.getRelativeParent(path, 1));

                BoundedInputStream bin = new BoundedInputStream(zin);
                bin.setPropagateClose(false);

                ImportContext subctx = context.createSubContext(parent);
                subctx.setInputStream(bin);
                subctx.setSystemId(Text.getName(path));
                subctx.setModificationTime(entry.getTime());
                ImportResourceChain.getChain().execute(subctx);
                zin.closeEntry();
            }
        }
        zin.close();
        return true;
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

    /**
     * Sets if the zips should be extracted again
     * @param recursive
     */
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    /**
     * Creates a valid jcr label from the given one
     *
     * @param label
     * @return
     */
    public static String makeValidJCRPath(String label) {
        StringBuffer ret = new StringBuffer(label.length());
        for (int i=0; i<label.length(); i++) {
            char c = label.charAt(i);
            if (c=='*' || c=='\'' || c=='\"') {
                c='_';
            } else if (c=='[') {
                c='(';
            } else if (c==']') {
                c=')';
            }
            ret.append(c);
        }
        return ret.toString();
    }


}
