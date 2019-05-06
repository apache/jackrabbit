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

import java.io.IOException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.property.PropEntry;

public class VersionHandler extends DefaultHandler implements IOHandler{

    public VersionHandler() {
    }

    public VersionHandler(IOManager ioManager) {
        super(ioManager);
    }

    //----------------------------------------------------------< IOHandler >---
    @Override
    public boolean canImport(ImportContext context, boolean isCollection) {
        // version node is read only.
        return false;
    }

    @Override
    public boolean canImport(ImportContext context, DavResource resource) {
        // version node is read only.
        return false;
    }

    @Override
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        // version node is read only.
        return false;
    }

    @Override
    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        // version node is read only.
        return false;
    }

    /**
     * @param context
     * @param isCollection
     * @return true if the export root is a <code>Version</code> node. False otherwise.
     */
    @Override
    public boolean canExport(ExportContext context, boolean isCollection) {
        if (context == null) {
            return false;
        }
        return context.getExportRoot() instanceof Version;
    }

    /**
     * @return true if the export root is a <code>Version</code> node. False otherwise.
     * @see IOHandler#canExport(ExportContext, DavResource)
     */
    @Override
    public boolean canExport(ExportContext context, DavResource resource) {
        if (context == null) {
            return false;
        }
        return context.getExportRoot() instanceof Version;
    }

    //----------------------------------------------------< PropertyHandler >---
    @Override
    public boolean canImport(PropertyImportContext context, boolean isCollection) {
        // version is read only
        return false;
    }

    @Override
    public Map<? extends PropEntry, ?> importProperties(PropertyImportContext importContext, boolean isCollection) throws RepositoryException {
        // version is read only
        throw new RepositoryException("Properties cannot be imported");
    }

    /**
     * @see PropertyHandler#exportProperties(PropertyExportContext, boolean)
     */
    @Override
    public boolean exportProperties(PropertyExportContext exportContext, boolean isCollection) throws RepositoryException {
        if (!canExport(exportContext, isCollection)) {
            throw new RepositoryException("PropertyHandler " + getName() + " failed to export properties.");
        }
        Node cn = getContentNode(exportContext, isCollection);
        try {
            // export the properties common with normal IO handling
            exportProperties(exportContext, isCollection, cn);
            // BUT don't export the special properties defined by nt:version
            return true;
        } catch (IOException e) {
            // should not occur (log output see 'exportProperties')
            return false;
        }
    }

    /**
     * Retrieves the content node that contains the data to be exported.
     *
     * @param context
     * @param isCollection
     * @return content node used for the export
     * @throws javax.jcr.RepositoryException
     */
    @Override
    protected Node getContentNode(ExportContext context, boolean isCollection) throws RepositoryException {
        Node node = (Node)context.getExportRoot();
        Node frozenNode = node.getNode(JcrConstants.JCR_FROZENNODE);
        if (frozenNode.hasNode(JcrConstants.JCR_CONTENT)) {
            return frozenNode.getNode(JcrConstants.JCR_CONTENT);
        } else {
            return frozenNode;
        }
    }
}
