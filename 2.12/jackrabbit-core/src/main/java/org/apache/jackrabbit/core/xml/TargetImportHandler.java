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
package org.apache.jackrabbit.core.xml;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;

/**
 * <code>TargetImportHandler</code> serves as the base class for the concrete
 * classes <code>{@link DocViewImportHandler}</code> and
 * <code>{@link SysViewImportHandler}</code>.
 */
abstract class TargetImportHandler extends DefaultHandler {

    protected final Importer importer;

    protected final ValueFactory valueFactory;

    /**
     * The current namespace context. A new namespace context is created
     * for each XML element and the parent reference is used to link the
     * namespace contexts together in a tree hierarchy. This variable contains
     * a reference to the namespace context associated with the XML element
     * that is currently being processed.
     */
    protected NamespaceContext nsContext;

    protected NamePathResolver resolver;

    protected TargetImportHandler(Importer importer, ValueFactory valueFactory) {
        this.importer = importer;
        this.valueFactory = valueFactory;
    }

    /**
     * Initializes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document starts.
     *
     * @throws SAXException if the importer can not be initialized
     * @see DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        try {
            importer.start();
            nsContext = null;
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * Closes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document ends.
     *
     * @throws SAXException if the importer can not be closed
     * @see DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * Starts a local namespace context for the current XML element.
     * This method is called by {@link ImportHandler} when the processing of
     * an XML element starts. The given local namespace mappings have been
     * recorded by {@link ImportHandler#startPrefixMapping(String, String)}
     * for the current XML element.
     *
     * @param mappings local namespace mappings
     */
    public final void startNamespaceContext(Map<String, String> mappings) {
        nsContext = new NamespaceContext(nsContext, mappings);
        resolver = new DefaultNamePathResolver(nsContext);
    }

    /**
     * Restores the parent namespace context. This method is called by
     * {@link ImportHandler} when the processing of an XML element ends.
     */
    public final void endNamespaceContext() {
        nsContext = nsContext.getParent();
    }

}
