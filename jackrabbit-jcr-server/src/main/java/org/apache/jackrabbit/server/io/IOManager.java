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

import org.apache.jackrabbit.webdav.DavResource;
import org.apache.tika.detect.Detector;

import java.io.IOException;

/**
 * <code>IOManager</code> interface provides the means to define a list of
 * <code>IOHandlers</code> that should be asked to perform an import or export.
 */
public interface IOManager {

    /**
     * Adds the specified handler to the list of handlers.
     *
     * @param ioHandler to be added
     */
    public void addIOHandler(IOHandler ioHandler);

    /**
     * Returns all handlers that have been added to this manager.
     *
     * @return Array of all handlers
     */
    public IOHandler[] getIOHandlers();

    /**
     * Return the configured type detector.
     *
     * @return content type detector
     */
    Detector getDetector();

    /**
     * Sets the configured type detector.
     *
     * @param detector content type detector.
     */
    void setDetector(Detector detector);

    /**
     * Passes the specified context and boolean value to the IOHandlers present
     * on this manager.
     * As soon as the first handler indicates success the import should be
     * considered completed. If none of the handlers can deal with the given
     * information this method must return false.
     *
     * @param context
     * @param isCollection
     * @return true if any of the handlers import the given context.
     * False otherwise.
     * @throws IOException
     * @see IOHandler#importContent(ImportContext, boolean)
     */
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException;

    /**
     * Passes the specified information to the IOHandlers present on this manager.
     * As soon as the first handler indicates success the import should be
     * considered completed. If none of the handlers can deal with the given
     * information this method must return false.
     *
     * @param context
     * @param resource
     * @return true if any of the handlers import the information present on the
     * specified context.
     * @throws IOException
     * @see IOHandler#importContent(ImportContext, DavResource)
     */
    public boolean importContent(ImportContext context, DavResource resource) throws IOException;

    /**
     * Passes the specified information to the IOHandlers present on this manager.
     * As soon as the first handler indicates success the export should be
     * considered completed. If none of the handlers can deal with the given
     * information this method must return false.
     *
     * @param context
     * @param isCollection
     * @return true if any of the handlers could run the export successfully,
     * false otherwise.
     * @throws IOException
     * @see IOHandler#exportContent(ExportContext, boolean)
     */
    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException;

    /**
     * Passes the specified information to the IOHandlers present on this manager.
     * As soon as the first handler indicates success the export should be
     * considered completed. If none of the handlers can deal with the given
     * information this method must return false.
     *
     * @param context
     * @param resource
     * @return true if any of the handlers could run the export successfully,
     * false otherwise.
     * @throws IOException
     * @see IOHandler#exportContent(ExportContext, DavResource)
     */
    public boolean exportContent(ExportContext context, DavResource resource) throws IOException;
}