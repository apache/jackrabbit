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
import org.apache.jackrabbit.webdav.DavResource;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.IOException;

/**
 * <code>DefaultIOManager</code>...
 */
public class DefaultIOManager implements IOManager {

    private static Logger log = Logger.getLogger(DefaultIOManager.class);

    private final List ioHandlers = new ArrayList();

    public DefaultIOManager() {
        this(true);
    }

    protected DefaultIOManager(boolean doInit) {
        if (doInit) {
           init();
        }
    }

    protected void init() {
        addIOHandler(new ZipHandler(this));
        addIOHandler(new XmlHandler(this));
        addIOHandler(new DirListingExportHandler(this));
        addIOHandler(new DefaultHandler(this));
    }

    public void addIOHandler(IOHandler ioHandler) {
        ioHandlers.add(ioHandler);
    }

    public IOHandler[] getIOHandlers() {
        return (IOHandler[]) ioHandlers.toArray(new IOHandler[ioHandlers.size()]);
    }

    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        boolean success = false;
        if (context != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }

            Iterator it = ioHandlers.iterator();
            while (it.hasNext() && !success) {
                IOHandler ioh = (IOHandler)it.next();
                if (ioh.canImport(context, isCollection)) {
                    ioListener.onBegin(ioh, context);
                    success = ioh.importContent(context, isCollection);
                    ioListener.onEnd(ioh, context, success);
                }
            }
            context.informCompleted(success);
        }
        return success;
    }

    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        boolean success = false;
        if (context != null && resource != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }
            Iterator it = ioHandlers.iterator();
            while (it.hasNext() && !success) {
                IOHandler ioh = (IOHandler)it.next();
                if (ioh.canImport(context, resource)) {
                    ioListener.onBegin(ioh, context);
                    success = ioh.importContent(context, resource);
                    ioListener.onEnd(ioh, context, success);
                }
            }
            context.informCompleted(success);
        }
        return success;
    }

    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException {
        boolean success = false;
        if (context != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }

            Iterator it = ioHandlers.iterator();
            while (it.hasNext() && !success) {
                IOHandler ioh = (IOHandler)it.next();
                if (ioh.canExport(context, isCollection)) {
                    ioListener.onBegin(ioh, context);
                    success = ioh.exportContent(context, isCollection);
                    ioListener.onEnd(ioh, context, success);
                }
            }
            context.informCompleted(success);
        }
        return success;
    }

    public boolean exportContent(ExportContext context, DavResource resource) throws IOException {
        boolean success = false;
        if (context != null && resource != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }

            Iterator it = ioHandlers.iterator();
            while (it.hasNext() && !success) {
                IOHandler ioh = (IOHandler)it.next();
                if (ioh.canExport(context, resource)) {
                    ioListener.onBegin(ioh, context);
                    success = ioh.exportContent(context, resource);
                    ioListener.onEnd(ioh, context, success);
                }
            }
            context.informCompleted(success);
        }
        return success;
    }
}