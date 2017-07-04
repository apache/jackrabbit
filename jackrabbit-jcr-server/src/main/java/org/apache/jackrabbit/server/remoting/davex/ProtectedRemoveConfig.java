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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.jackrabbit.webdav.xml.DomUtil;
import org.apache.jackrabbit.webdav.xml.ElementIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * A configuration of ProtectedItemRemoveHandler(s).
 * 
 * <code>ProtectedRemoveConfig</code>
 */
class ProtectedRemoveConfig {

    private static final Logger log = LoggerFactory.getLogger(ProtectedRemoveConfig.class);

    private static final String ELEMENT_HANDLER = "protecteditemremovehandler";
    private static final String ELEMENT_CLASS = "class";
    private static final String ATTRIBUTE_NAME = "name";

    private final ProtectedRemoveManager manager;

    ProtectedRemoveConfig(ProtectedRemoveManager manager) {
        this.manager = manager;
    }

    void parse(InputStream inputStream) throws IOException {
        Element config;
        ProtectedItemRemoveHandler instance = null;
        try {
            config = DomUtil.parseDocument(inputStream).getDocumentElement();
            if (config == null) {
                log.warn("Missing mandatory config element");
                return;
            }
            ElementIterator handlers = DomUtil.getChildren(config, ELEMENT_HANDLER, null);
            while (handlers.hasNext()) {
                Element handler = handlers.nextElement();
                instance = createHandler(handler);
                manager.addHandler(instance);
            }
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        } catch (SAXException e) {
            log.error(e.getMessage(), e);
        }
    }

    private ProtectedItemRemoveHandler createHandler(Element parent) {
        ProtectedItemRemoveHandler instance = null;
        Element classElem = DomUtil.getChildElement(parent, ELEMENT_CLASS, null);
        if (classElem != null) {
            String className = DomUtil.getAttribute(classElem, ATTRIBUTE_NAME, null);
            if (className != null) {
                instance = manager.createHandler(className);
            }
        }
        return instance;
    }
}
