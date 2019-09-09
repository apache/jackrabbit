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

import org.apache.jackrabbit.webdav.property.PropEntry;

import javax.jcr.RepositoryException;
import java.util.Map;

/**
 * <code>PropertyHandler</code> interface defines methods for importing and
 * exporting resource properties.
 */
public interface PropertyHandler {

    /**
     * Returns true, if this handler can run a successful export based on the
     * specified context.
     *
     * @param context
     * @param isCollection
     * @return true if this <code>PropertyHandler</code> is export properties
     * given the specified parameters.
     */
    public boolean canExport(PropertyExportContext context, boolean isCollection);

    /**
     * Exports properties to the given context. Note that the export must
     * be consistent with properties that might be exposed by content export
     * such as defined by {@link IOHandler#exportContent(ExportContext, boolean)}.
     *
     * @param exportContext
     * @param isCollection
     * @return true if the export succeeded.
     * @throws RepositoryException If an attempt is made to export properties
     * even if {@link PropertyHandler#canExport(PropertyExportContext, boolean)}
     * returns false or if some other unrecoverable error occurs.
     */
    public boolean exportProperties(PropertyExportContext exportContext, boolean isCollection) throws RepositoryException;

    /**
     * Returns true, if this handler can run a property import based on the
     * specified context.
     *
     * @param context
     * @param isCollection
     * @return true if this <code>PropertyHandler</code> can import properties
     * given the specified parameters.
     */
    public boolean canImport(PropertyImportContext context, boolean isCollection);

    /**
     * Imports, modifies or removes properties according the the
     * {@link PropertyImportContext#getChangeList() change list} available from
     * the import context. Note, that according to JSR 170 setting a property
     * value to <code>null</code> is equivalent to its removal.
     * <p>
     * The return value of this method must be used to provided detailed
     * information about any kind of failures.
     *
     * @param importContext
     * @param isCollection
     * @return Map listing those properties that failed to be updated. An empty
     * map indicates a successful import for all properties listed in the context.
     * @throws RepositoryException If
     * {@link PropertyHandler#canImport(PropertyImportContext, boolean)}
     * returns false for the given parameters or if some other unrecoverable
     * error occurred. Note, that normal failure of a property update must be
     * reported with the return value and should not result in an exception.
     */
    public Map<? extends PropEntry, ?> importProperties(PropertyImportContext importContext, boolean isCollection) throws RepositoryException;

}