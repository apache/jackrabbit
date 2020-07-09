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

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceIterator;
import org.apache.jackrabbit.webdav.property.PropEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Map;

/**
 * <code>DirListingExportHandler</code> represents a simple export for collections:
 * a human-readable view listing the members.
 * <p>
 * Note: If {@link #exportContent(ExportContext, boolean)} is called the view list
 * child nodes only, without respecting their representation as <code>DavResource</code>s.
 */
public class DirListingExportHandler implements IOHandler, PropertyHandler {

    private static Logger log = LoggerFactory.getLogger(DirListingExportHandler.class);

    private IOManager ioManager;

    /**
     * Creates a new <code>DirListingExportHandler</code>
     */
    public DirListingExportHandler() {
    }

    /**
     * Creates a new <code>DirListingExportHandler</code>
     *
     * @param ioManager
     */
    public DirListingExportHandler(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    /**
     * Always returns false
     *
     * @see IOHandler#canImport(ImportContext, boolean)
     */
    public boolean canImport(ImportContext context, boolean isFolder) {
        return false;
    }

    /**
     * Always returns false
     *
     * @see IOHandler#canImport(ImportContext, DavResource)
     */
    public boolean canImport(ImportContext context, DavResource resource) {
        return false;
    }

    /**
     * Does nothing and returns false
     *
     * @see IOHandler#importContent(ImportContext, boolean)
     */
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        // can only handle export
        return false;
    }

    /**
     * Does nothing and returns false
     *
     * @see IOHandler#importContent(ImportContext, DavResource)
     */
    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        return false;
    }

    /**
     * @return true if the specified context is still valid and provides a
     * export root and if 'isCollection' is true. False otherwise
     * @see IOHandler#canExport(ExportContext, boolean)
     */
    public boolean canExport(ExportContext context, boolean isCollection) {
        if (context == null || context.isCompleted()) {
            return false;
        }
        return isCollection && context.getExportRoot() != null;
    }

    /**
     * @return true if the specified context is still valid and provides a
     * export root and if the specified resource is a collection. False otherwise.
     * @see IOHandler#canExport(ExportContext, DavResource)
     * @see DavResource#isCollection()
     */
    public boolean canExport(ExportContext context, DavResource resource) {
        if (resource == null) {
            return false;
        }
        return canExport(context, resource.isCollection());
    }

    /**
     * @see IOHandler#exportContent(ExportContext, boolean)
     */
    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException {
        if (!canExport(context, isCollection)) {
            throw new IOException(getName() + ": Cannot export " + context.getExportRoot());
        }

        // properties (content length undefined)
        context.setModificationTime(new Date().getTime());
        context.setContentType("text/html", "UTF-8");

        // data
        if (context.hasStream()) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(context.getOutputStream(), "utf8"));
            try {
                Item item = context.getExportRoot();
                Repository rep = item.getSession().getRepository();
                String repName = rep.getDescriptor(Repository.REP_NAME_DESC);
                String repURL = rep.getDescriptor(Repository.REP_VENDOR_URL_DESC);
                String repVersion = rep.getDescriptor(Repository.REP_VERSION_DESC);
                writer.print("<html><head><title>");
                writer.print(Text.encodeIllegalHTMLCharacters(repName));
                writer.print(" ");
                writer.print(Text.encodeIllegalHTMLCharacters(repVersion));
                writer.print(" ");
                writer.print(Text.encodeIllegalHTMLCharacters(item.getPath()));
                writer.print("</title></head>");
                writer.print("<body><h2>");
                writer.print(Text.encodeIllegalHTMLCharacters(item.getPath()));
                writer.print("</h2><ul>");
                writer.print("<li><a href=\"..\">..</a></li>");
                if (item.isNode()) {
                    NodeIterator iter = ((Node)item).getNodes();
                    while (iter.hasNext()) {
                        Node child = iter.nextNode();
                        String label = Text.getName(child.getPath());
                        writer.print("<li><a href=\"");
                        writer.print(Text.encodeIllegalHTMLCharacters(Text.escape(label)));
                        if (child.isNode()) {
                            writer.print("/");
                        }
                        writer.print("\">");
                        writer.print(Text.encodeIllegalHTMLCharacters(label));
                        writer.print("</a></li>");
                    }
                }
                writer.print("</ul><hr size=\"1\"><em>Powered by <a href=\"");
                writer.print(Text.encodeIllegalHTMLCharacters(repURL));
                writer.print("\">");
                writer.print(Text.encodeIllegalHTMLCharacters(repName));
                writer.print("</a> version ");
                writer.print(Text.encodeIllegalHTMLCharacters(repVersion));
                writer.print("</em></body></html>");
            } catch (RepositoryException e) {
                // should not occur
                log.debug(e.getMessage());
            }
            writer.close();
        }
        return true;
    }

    /**
     * @see IOHandler#exportContent(ExportContext, DavResource)
     */
    public boolean exportContent(ExportContext context, DavResource resource) throws IOException {
        if (!canExport(context, resource)) {
            throw new IOException(getName() + ": Cannot export " + context.getExportRoot());
        }

        // properties (content length undefined)
        context.setModificationTime(new Date().getTime());
        context.setContentType("text/html", "UTF-8");

        // data
        if (context.hasStream()) {
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(context.getOutputStream(), "utf8"));
            try {
                Item item = context.getExportRoot();
                Repository rep = item.getSession().getRepository();
                String repName = rep.getDescriptor(Repository.REP_NAME_DESC);
                String repURL = rep.getDescriptor(Repository.REP_VENDOR_URL_DESC);
                String repVersion = rep.getDescriptor(Repository.REP_VERSION_DESC);
                writer.print("<html><head><title>");
                writer.print(Text.encodeIllegalHTMLCharacters(repName));
                writer.print(" ");
                writer.print(Text.encodeIllegalHTMLCharacters(repVersion));
                writer.print(" ");
                writer.print(Text.encodeIllegalHTMLCharacters(resource.getResourcePath()));
                writer.print("</title></head>");
                writer.print("<body><h2>");
                writer.print(Text.encodeIllegalHTMLCharacters(resource.getResourcePath()));
                writer.print("</h2><ul>");
                writer.print("<li><a href=\"..\">..</a></li>");
                DavResourceIterator iter = resource.getMembers();
                while (iter.hasNext()) {
                    DavResource child = iter.nextResource();
                    String label = Text.getName(child.getResourcePath());
                    writer.print("<li><a href=\"");
                    writer.print(Text.encodeIllegalHTMLCharacters(child.getHref()));
                    writer.print("\">");
                    writer.print(Text.encodeIllegalHTMLCharacters(label));
                    writer.print("</a></li>");
                }
                writer.print("</ul><hr size=\"1\"><em>Powered by <a href=\"");
                writer.print(Text.encodeIllegalHTMLCharacters(repURL));
                writer.print("\">");
                writer.print(Text.encodeIllegalHTMLCharacters(repName));
                writer.print("</a> version ");
                writer.print(Text.encodeIllegalHTMLCharacters(repVersion));
                writer.print("</em></body></html>");
            } catch (RepositoryException e) {
                // should not occur
                log.debug(e.getMessage());
            }
            writer.close();
        }
        return true;
    }

    /**
     * @see IOHandler#getIOManager()
     */
    public IOManager getIOManager() {
        return ioManager;
    }

    /**
     * @see IOHandler#setIOManager(IOManager)
     */
    public void setIOManager(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    /**
     * @see IOHandler#getName()
     */
    public String getName() {
        return "DirListing Export";
    }

    //----------------------------------------------------< PropertyHandler >---
    /**
     * Always returns false.
     * @param context
     * @param isCollection
     * @return always returns false.
     */
    public boolean canExport(PropertyExportContext context, boolean isCollection) {
        return false;
    }

    /**
     * @see PropertyHandler#exportProperties(PropertyExportContext, boolean)
     */
    public boolean exportProperties(PropertyExportContext exportContext, boolean isCollection) throws RepositoryException {
        // export-content facility only... no responsible for PROPFIND.
        throw new RepositoryException(getName() + ": Cannot export properties for context " + exportContext);
    }

    public boolean canImport(PropertyImportContext context, boolean isCollection) {
        return false;
    }

    /**
     * @see PropertyHandler#importProperties(PropertyImportContext, boolean)
     */
    public Map<? extends PropEntry, ?> importProperties(PropertyImportContext importContext, boolean isCollection) throws RepositoryException {
        // export facilities only -> throw
        throw new RepositoryException(getName() + ": Cannot import properties.");
    }
}
