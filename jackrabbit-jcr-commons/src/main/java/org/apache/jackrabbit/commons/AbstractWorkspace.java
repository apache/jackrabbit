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
package org.apache.jackrabbit.commons;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Workspace;

import org.apache.jackrabbit.commons.xml.DefaultContentHandler;
import org.xml.sax.ContentHandler;

/**
 * Abstract base class for implementing the JCR {@link Workspace} interface.
 */
public abstract class AbstractWorkspace implements Workspace {

    /**
     * Parses the given input stream as an XML document and processes the
     * SAX events using the {@link ContentHandler} returned by
     * {@link Workspace#getImportContentHandler(String, int)}.
     *
     * @param parentAbsPath passed through
     * @param in input stream to be parsed as XML and imported
     * @param uuidBehavior passed through
     * @throws IOException if an I/O error occurs
     * @throws RepositoryException if another error occurs
     */
    public void importXML(
            String parentAbsPath, InputStream in, int uuidBehavior)
            throws IOException, RepositoryException {
        ContentHandler handler =
            getImportContentHandler(parentAbsPath, uuidBehavior);
        new DefaultContentHandler(handler).parse(in);
    }

}
