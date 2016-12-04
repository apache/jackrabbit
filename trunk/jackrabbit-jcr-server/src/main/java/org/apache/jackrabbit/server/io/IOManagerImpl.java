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
import java.util.ArrayList;
import java.util.List;

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.tika.detect.Detector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>IOManagerImpl</code> represents the most simple <code>IOManager</code>
 * implementation that provides a default constructor and does define any
 * <code>IOHandler</code>s.
 */
public class IOManagerImpl implements IOManager {

    private static Logger log = LoggerFactory.getLogger(IOManagerImpl.class);

    /**
     * Content type detector.
     */
    private Detector detector;

    private final List<IOHandler> ioHandlers = new ArrayList<IOHandler>();

    /**
     * Create a new <code>IOManager</code>.
     * Note, that this manager does not define any <code>IOHandler</code>s by
     * default. Use {@link #addIOHandler(IOHandler)} in order to populate the
     * internal list of handlers that are called for <code>importContent</code> and
     * <code>exportContent</code>.
     */
    public IOManagerImpl() {
    }

    /**
     * @see IOManager#addIOHandler(IOHandler)
     */
    public void addIOHandler(IOHandler ioHandler) {
        if (ioHandler == null) {
            throw new IllegalArgumentException("'null' is not a valid IOHandler.");
        }
        ioHandler.setIOManager(this);
        ioHandlers.add(ioHandler);
    }

    /**
     * @see IOManager#getIOHandlers()
     */
    public IOHandler[] getIOHandlers() {
        return ioHandlers.toArray(new IOHandler[ioHandlers.size()]);
    }

    /**
     * Return the configured type detector.
     *
     * @return content type detector
     */
    public Detector getDetector() {
        return detector;
    }

    /**
     * Sets the configured type detector.
     *
     * @param detector content type detector
     */
    public void setDetector(Detector detector) {
        this.detector = detector;
    }

    /**
     * @see IOManager#importContent(ImportContext, boolean)
     */
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException {
        boolean success = false;
        if (context != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }
            IOHandler[] ioHandlers = getIOHandlers();
            for (int i = 0; i < ioHandlers.length && !success; i++) {
                IOHandler ioh = ioHandlers[i];
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

    /**
     * @see IOManager#importContent(ImportContext, DavResource)
     */
    public boolean importContent(ImportContext context, DavResource resource) throws IOException {
        boolean success = false;
        if (context != null && resource != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }
            IOHandler[] ioHandlers = getIOHandlers();
            for (int i = 0; i < ioHandlers.length && !success; i++) {
                IOHandler ioh = ioHandlers[i];
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

    /**
     * @see IOManager#exportContent(ExportContext, boolean)
     */
    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException {
        boolean success = false;
        if (context != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }
            IOHandler[] ioHandlers = getIOHandlers();
            for (int i = 0; i < ioHandlers.length && !success; i++) {
                IOHandler ioh = ioHandlers[i];
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

    /**
     * @see IOManager#exportContent(ExportContext, DavResource)
     */
    public boolean exportContent(ExportContext context, DavResource resource) throws IOException {
        boolean success = false;
        if (context != null && resource != null) {
            IOListener ioListener = context.getIOListener();
            if (ioListener == null) {
                ioListener = new DefaultIOListener(log);
            }
            IOHandler[] ioHandlers = getIOHandlers();
            for (int i = 0; i < ioHandlers.length && !success; i++) {
                IOHandler ioh = ioHandlers[i];
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
