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

import org.apache.jackrabbit.util.Text;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.NodeIterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileInputStream;
import java.util.HashSet;

/**
 * This Class implements a collection export command that produces a HTML
 * directory listing of all child nodes. If {@link #isCollectionNodeType(String)}
 * returns true, when passed the primary node type of a child node, the result
 * will be a directory link.<p/>
 * The corresponding mapping is retrieved from the catalog. By default if no
 * behaviour is specified, nodes are displayed as directories.
 */
public class DirListingExportCommand extends AbstractCommand {

    /**
     * define which node types define collection resources
     */
    private HashSet collectionNodeTypes = new HashSet();

    /**
     * define which node types don't define collection resources
     */
    private HashSet nonCollectionNodeTypes = new HashSet();

    /**
     * Creates a DirListingExportCommand
     */
    public DirListingExportCommand() {
    }

    /**
     * Returns true if the given the node type name denotes a collection.
     *
     * @return true if the given the node type name denotes a collection node.
     */
    public boolean isCollectionNodeType(String nodeTypeName) {
        if (nonCollectionNodeTypes.isEmpty()) {
            // check collection-set
            return collectionNodeTypes.contains(nodeTypeName);
        } else {
            // check non-collection-set
            return !nonCollectionNodeTypes.contains(nodeTypeName);
        }
    }

    /**
     * Defines the given node type names to represent collection nodes.
     * Child nodes having this node type result in a directory link.
     *
     * @param nodeTypeNames comma separated String value
     */
    public void setCollectionNodeTypes(String nodeTypeNames) {
        String[] names = nodeTypeNames.split(",");
        for (int i = 0; i < names.length; i++) {
            collectionNodeTypes.add(names[i].trim());
        }
    }

    /**
     * Defines the given node type names to represent non-collection nodes.
     * Child nodes having this node type will never result in a directory
     * link.
     *
     * @param nodeTypeNames comma separated String value
     */
    public void setNonCollectionNodeTypes(String nodeTypeNames) {
        String[] names = nodeTypeNames.split(",");
        for (int i = 0; i < names.length; i++) {
             nonCollectionNodeTypes.add(names[i].trim());
        }
    }

    /**
     * Executes this command by delegating to {@link #execute(ExportContext)} if
     * the context has the correct class.
     *
     * @param context the (import) context.
     * @return <code>false</code>.
     * @throws Exception if an error occurrs.
     */
    public boolean execute(AbstractContext context) throws Exception {
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
            if (isCollectionNodeType(child.getPrimaryNodeType().getName())) {
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
