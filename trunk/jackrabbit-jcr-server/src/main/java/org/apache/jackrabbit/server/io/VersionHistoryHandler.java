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
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.version.VersionHistory;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.property.PropEntry;

/**
 * <code>VersionHistoryHandler</code>...
 */
public class VersionHistoryHandler implements IOHandler, PropertyHandler {

    private IOManager ioManager;

    public VersionHistoryHandler() {
    }

    public VersionHistoryHandler(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    //----------------------------------------------------------< IOHandler >---
    public IOManager getIOManager() {
        return ioManager;
    }

    public void setIOManager(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    public String getName() {
        return getClass().getName();
    }

    public boolean canImport(ImportContext context, boolean isCollection) {
        return false;
    }

    public boolean canImport(ImportContext context, DavResource resource) {
        return false;
    }

    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean canExport(ExportContext context, boolean isCollection) {
        if (context == null) {
            return false;
        }
        return context.getExportRoot() instanceof VersionHistory;
    }

    public boolean canExport(ExportContext context, DavResource resource) {
        if (context == null) {
            return false;
        }
        return context.getExportRoot() instanceof VersionHistory;
    }

    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException {
        Item exportRoot = context.getExportRoot();
        if (exportRoot instanceof VersionHistory) {
            return export(context);
        } else {
            return false;
        }
    }

    public boolean exportContent(ExportContext context, DavResource resource) throws IOException {
        Item exportRoot = context.getExportRoot();
        if (exportRoot instanceof VersionHistory) {
            return export(context);
        } else {
            return false;
        }
    }

    //----------------------------------------------------< PropertyHandler >---
    public boolean canImport(PropertyImportContext context, boolean isCollection) {
        return false;
    }

    public Map<? extends PropEntry, ?> importProperties(PropertyImportContext importContext, boolean isCollection) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean canExport(PropertyExportContext context, boolean isCollection) {
        return canExport((ExportContext) context, isCollection);
    }

    public boolean exportProperties(PropertyExportContext exportContext, boolean isCollection) throws RepositoryException {
        if (!canExport(exportContext, isCollection)) {
            throw new RepositoryException("PropertyHandler " + getName() + " failed to export properties.");
        }
        return export(exportContext);
    }

    //--------------------------------------------------------------------------
    private boolean export(ExportContext exportContext) {
        // don't export any properties of the version history node. deltaV
        // defines a fix set of properties to be exported and the dav-resource
        // needs to take care of those.
        exportContext.setContentLength(0);
        exportContext.setModificationTime(IOUtil.UNDEFINED_TIME);
        return true;
    }
}
