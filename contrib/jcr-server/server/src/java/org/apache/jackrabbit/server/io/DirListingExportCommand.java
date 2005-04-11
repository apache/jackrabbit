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

import org.apache.commons.chain.Command;
import org.apache.commons.chain.Context;
import org.apache.jackrabbit.webdav.util.Text;
import org.apache.jackrabbit.JCRConstants;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.NodeIterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;

/**
 * This Class implements a collection export command that produces a HTML
 * directory listing of all child nodes. All child nodes having the
 * {@link #getCollectionNodeType()} result in a directory link.
 */
public class DirListingExportCommand implements Command, JCRConstants {

    /**
     * the node type of a collection
     */
    private String collectionNodeType = NT_FOLDER;

    /**
     * Creates a DirListingExportCommand
     */
    public DirListingExportCommand() {
    }

    /**
     * Creates a DirListingExportCommand with the given collection node type.
     * @param collectionNodeType
     */
    public DirListingExportCommand(String collectionNodeType) {
        this.collectionNodeType = collectionNodeType;
    }

    /**
     * Returns the node type of a collection node.
     * @return the node type of a collection node.
     */
    public String getCollectionNodeType() {
        return collectionNodeType;
    }

    /**
     * Sets the node type of collection nodes. child nodes having this node
     * type result in a directory link.
     *
     * @param collectionNodeType
     */
    public void setCollectionNodeType(String collectionNodeType) {
        this.collectionNodeType = collectionNodeType;
    }

    /**
     * Executes this command by delegating to {@link #execute(ExportContext)} if
     * the context has the correct class.
     * @param context the (import) context.
     * @return <code>false</code>.
     * @throws Exception if an error occurrs.
     */
    public boolean execute(Context context) throws Exception {
        if (context instanceof ExportContext) {
            return execute((ExportContext) context);
        } else {
            return false;
        }
    }

    /**
     * Executes this command. It generates a HTML directory listing of all
     * child nodes of the collection node.
     *
     * @param context the export context
     * @return <code>true</code>
     * @throws Exception in an error occurrs
     */
    public boolean execute(ExportContext context) throws Exception {
        Node node = context.getNode();
        File tmpfile = File.createTempFile("__webdav", ".xml");
        FileOutputStream out = new FileOutputStream(tmpfile);

        String repName = node.getSession().getRepository().getDescriptor(Repository.REP_NAME_DESC);
        String repURL = node.getSession().getRepository().getDescriptor(Repository.REP_VENDOR_URL_DESC);
        String repVersion = node.getSession().getRepository().getDescriptor(Repository.REP_VERSION_DESC);
        PrintWriter writer = new PrintWriter(out);
        writer.print("<html><head><title>");
        writer.print(repName);
        writer.print(" ");
        writer.print(repVersion);
        writer.print(" ");
        writer.print(node.getPath());
        writer.print("</title></head>");
        writer.print("<body><h2>");
        writer.print(node.getPath());
        writer.print("</h2><ul>");
        writer.print("<li><a href=\"..\">..</a></li>");
        NodeIterator iter = node.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            String label = Text.getLabel(child.getPath());
            writer.print("<li><a href=\"");
            writer.print(Text.escape(label));
            if (child.getPrimaryNodeType().getName().equals(collectionNodeType)) {
                writer.print("/");
            }
            writer.print("\">");
            writer.print(label);
            writer.print("</a></li>");
        }
        writer.print("</ul><hr size=\"1\"><em>Powered by <a href=\"");
        writer.print(repURL);
        writer.print("\">");
        writer.print(repName);
        writer.print("</a> version ");
        writer.print(repVersion);
        writer.print("</em></body></html>");

        writer.close();
        out.close();

        // set output
        context.setInputStream(new FileInputStream(tmpfile));
        context.setContentLength(tmpfile.length());
        context.setModificationTime(tmpfile.lastModified());
        context.setContentType("text/html");
        tmpfile.deleteOnExit();

        return true;
    }

}
