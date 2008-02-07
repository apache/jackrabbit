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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <code>PropertyManagerImpl</code>...
 */
public class PropertyManagerImpl implements PropertyManager {

    private static Logger log = LoggerFactory.getLogger(PropertyManagerImpl.class);

    private static PropertyManager DEFAULT_MANAGER;

    private final List propertyHandlers = new ArrayList();

    /**
     * Create a new <code>PropertyManagerImpl</code>.
     * Note, that this manager does not define any <code>PropertyHandler</code>s by
     * default. Use {@link #addPropertyHandler(PropertyHandler)} in order to populate the
     * internal list of handlers that are called for <code>importProperties</code> and
     * <code>exportProperties</code> respectively. See {@link #getDefaultManager()}
     * for an instance of this class populated with default handlers.
     */
    public PropertyManagerImpl() {
    }

    /**
     * @see PropertyManager#exportProperties(PropertyExportContext, boolean)
     */
    public boolean exportProperties(PropertyExportContext context, boolean isCollection) throws RepositoryException {
        boolean success = false;
        PropertyHandler[] propertyHandlers = getPropertyHandlers();
        for (int i = 0; i < propertyHandlers.length && !success; i++) {
            PropertyHandler ph = propertyHandlers[i];
            if (ph.canExport(context, isCollection)) {
                success = ph.exportProperties(context, isCollection);
            }
        }
        context.informCompleted(success);
        return success;
    }

    /**
     * @see PropertyManager#alterProperties(PropertyImportContext, boolean)
     */
    public Map alterProperties(PropertyImportContext context, boolean isCollection) throws RepositoryException {
        boolean foundHandler = false;
        Map failures = new HashMap(0);
        PropertyHandler[] propertyHandlers = getPropertyHandlers();
        for (int i = 0; i < propertyHandlers.length && !foundHandler; i++) {
            PropertyHandler ph = propertyHandlers[i];
            if ((foundHandler = ph.canImport(context, isCollection))) {
                failures = ph.importProperties(context, isCollection);
            }
        }
        if (!foundHandler) {
            throw new RepositoryException("Unable to alter properties: No matching handler found.");
        }
        context.informCompleted(failures.isEmpty());
        return failures;
    }

    /**
     * @see PropertyManager#addPropertyHandler(PropertyHandler)
     */
    public void addPropertyHandler(PropertyHandler propertyHandler) {
        if (propertyHandler == null) {
            throw new IllegalArgumentException("'null' is not a valid IOHandler.");
        }
        propertyHandlers.add(propertyHandler);
    }

    /**
     * @see PropertyManager#getPropertyHandlers()
     */
    public PropertyHandler[] getPropertyHandlers() {
        return (PropertyHandler[]) propertyHandlers.toArray(new PropertyHandler[propertyHandlers.size()]);
    }

    /**
     * @return an instance of PropertyManager populated with default handlers.
     */
    public static PropertyManager getDefaultManager() {
        if (DEFAULT_MANAGER == null) {
            DEFAULT_MANAGER = new PropertyManagerImpl();
            DEFAULT_MANAGER.addPropertyHandler(new ZipHandler());
            DEFAULT_MANAGER.addPropertyHandler(new XmlHandler());
            DEFAULT_MANAGER.addPropertyHandler(new DefaultHandler());
        }
        return DEFAULT_MANAGER;
    }
}