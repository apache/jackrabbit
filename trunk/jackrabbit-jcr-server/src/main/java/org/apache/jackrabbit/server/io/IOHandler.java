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

import java.io.IOException;

/**
 * <code>IOHandler</code> interface defines methods for importing and
 * exporting resource content as well as some fundamental resource properties
 * which use to be set/retrieved together with content import and export (e.g.
 * content length, modification date etc.).
 */
public interface IOHandler {

    /**
     * Returns the <code>IOManager</code> that called this handler or <code>null</code>.
     */
    public IOManager getIOManager();

    /**
     * Sets the <code>IOManager</code> that called this handler.
     */
    public void setIOManager(IOManager ioManager);

    /**
     * Returns a human readable name for this <code>IOHandler</code>.
     */
    public String getName();

    /**
     * Returns true, if this handler can run a successful import based on the
     * specified context.
     */
    public boolean canImport(ImportContext context, boolean isCollection);

    /**
     * Returns true, if this handler can run a successful import based on
     * the specified context and resource. A simple implementation may choose
     * to return the same as {@link IOHandler#canImport(ImportContext, boolean)}
     * where the isCollection flag is determined by
     * {@link DavResource#isCollection()}.
     */
    public boolean canImport(ImportContext context, DavResource resource);

    /**
     * Runs the import for the given context and indicates by a boolean return
     * value, if the import could be completed successfully. If the specified
     * <code>ImportContext</code> does not provide a {@link ImportContext#hasStream() stream}
     * the implementation is free, to only import properties of to refuse the
     * import.
     * <p>
     * Please note, that it is the responsibility of the specified
     * <code>ImportContext</code> to assert, that its stream is not consumed
     * multiple times when being passed to a chain of <code>IOHandler</code>s.
     *
     * @param context
     * @param isCollection
     * @return true if the import was successful.
     * @throws IOException if an unexpected error occurs or if this method has
     * been called although {@link IOHandler#canImport(ImportContext, boolean)}
     * returns false.
     */
    public boolean importContent(ImportContext context, boolean isCollection) throws IOException;

    /**
     * Runs the import for the given context and resource. It indicates by a boolean return
     * value, if the import could be completed successfully. If the specified
     * <code>ImportContext</code> does not provide a {@link ImportContext#hasStream() stream}
     * the implementation is free, to only import properties of to refuse the
     * import. A simple implementation may return the same as
     * {@link IOHandler#importContent(ImportContext, boolean)} where the
     * isCollection flag is determined by {@link DavResource#isCollection()}
     * <p>
     * Please note, that it is the responsibility of the specified
     * <code>ImportContext</code> to assert, that its stream is not consumed
     * multiple times when being passed to a chain of <code>IOHandler</code>s.
     *
     * @param context
     * @param resource
     * @return
     * @throws IOException if an unexpected error occurs or if this method has
     * been called although {@link IOHandler#canImport(ImportContext, DavResource)}
     * returns false.
     * @see IOHandler#importContent(ImportContext, boolean)
     */
    public boolean importContent(ImportContext context, DavResource resource) throws IOException;

    /**
     * Returns true, if this handler can run a successful export based on the
     * specified context.
     */
    public boolean canExport(ExportContext context, boolean isCollection);

    /**
     * Returns true, if this handler can run a successful export based on
     * the specified context and resource. A simple implementation may choose
     * to return the same as {@link IOHandler#canExport(ExportContext, boolean)}
     * where the isCollection flag is determined by
     * {@link DavResource#isCollection()}.
     */
    public boolean canExport(ExportContext context, DavResource resource);

    /**
     * Runs the export for the given context. It indicates by a boolean return
     * value, if the export could be completed successfully. If the specified
     * <code>ExportContext</code> does not provide a {@link ExportContext#hasStream() stream}
     * the implementation should set the properties only and ignore the content to
     * be exported. A simple implementation may return the same as
     * {@link IOHandler#exportContent(ExportContext, boolean)} where the
     * isCollection flag is determined by {@link DavResource#isCollection()}
     * <p>
     * Please note, that it is the responsibility of the specified
     * <code>ExportContext</code> to assert, that its stream is not written
     * multiple times when being passed to a chain of <code>IOHandler</code>s.
     *
     * @param context
     * @param isCollection
     * @return
     * @throws IOException if an unexpected error occurs or if this method has
     * been called although {@link IOHandler#canExport(ExportContext, boolean)}
     * returns false.
     */
    public boolean exportContent(ExportContext context, boolean isCollection) throws IOException;

    /**
     * Runs the export for the given context and resource. It indicates by a boolean return
     * value, if the export could be completed successfully. If the specified
     * <code>ExportContext</code> does not provide a {@link ExportContext#hasStream() stream}
     * the implementation should set the properties only and ignore the content to
     * be exported. A simple implementation may return the same as
     * {@link IOHandler#exportContent(ExportContext, boolean)} where the
     * isCollection flag is determined by {@link DavResource#isCollection()}
     * <p>
     * Please note, that it is the responsibility of the specified
     * <code>ExportContext</code> to assert, that its stream is not written
     * multiple times when being passed to a chain of <code>IOHandler</code>s.
     *
     * @param context
     * @param resource
     * @return
     * @throws IOException if an unexpected error occurs or if this method has
     * been called although {@link IOHandler#canExport(ExportContext, DavResource)}
     * returns false.
     */
    public boolean exportContent(ExportContext context, DavResource resource) throws IOException;
}