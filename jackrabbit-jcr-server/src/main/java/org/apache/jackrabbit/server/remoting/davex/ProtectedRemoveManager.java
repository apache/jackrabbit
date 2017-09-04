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
package org.apache.jackrabbit.server.remoting.davex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProtectedRemoveManager {

    private static final Logger log = LoggerFactory.getLogger(ProtectedRemoveManager.class);

    private List<ProtectedItemRemoveHandler> handlers = new ArrayList<ProtectedItemRemoveHandler>();

    ProtectedRemoveManager(){

    }

    ProtectedRemoveManager(String config) throws IOException {

        if (config == null) {
            log.warn("protectedhandlers-config is missing -> DIFF processing can fail for the Remove operation if the content to"
                    + "remove is protected!");
        } else {
            File file = new File(config);
            if (file.exists()) {
                try {
                    InputStream fis = new FileInputStream(file);
                    load(fis);
                } catch (FileNotFoundException e) {
                    throw new IOException(e.getMessage(), e);
                }
            } else { // config is an Impl class
                if (!config.isEmpty()) {
                    ProtectedItemRemoveHandler handler = createHandler(config);
                    addHandler(handler);
                } else {
                    log.debug("Fail to locate the protected-item-remove-handler properties file.");
                }
            }
        }
    }

    void load(InputStream fis) throws IOException {
        ProtectedRemoveConfig prConfig = new ProtectedRemoveConfig(this);
        prConfig.parse(fis);
    }

    boolean remove(Session session, String itemPath) throws RepositoryException {
        for (ProtectedItemRemoveHandler handler : handlers) {
            if (handler.remove(session, itemPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Instantiates and returns a concrete ProtectedItemRemoveHandler implementation.
     * @param className
     * @return
     * @throws RepositoryException
     */
    ProtectedItemRemoveHandler createHandler(String className) {
        ProtectedItemRemoveHandler irHandler = null;
        try {
            if (!className.isEmpty()) {
                Class<?> irHandlerClass = Class.forName(className);
                if (ProtectedItemRemoveHandler.class.isAssignableFrom(irHandlerClass)) {
                    irHandler = (ProtectedItemRemoveHandler) irHandlerClass.newInstance();
                }
            }
        } catch (ClassNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (InstantiationException e) {
            log.error(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            log.error(e.getMessage(), e);
        }
        return irHandler;
    }

    void addHandler(ProtectedItemRemoveHandler instance) {
        if (instance != null) {
            handlers.add(instance);
        }
    }
}
