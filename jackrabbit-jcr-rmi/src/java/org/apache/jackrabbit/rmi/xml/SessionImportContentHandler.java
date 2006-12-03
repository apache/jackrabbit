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
package org.apache.jackrabbit.rmi.xml;

import java.io.ByteArrayInputStream;

import javax.jcr.Session;


/**
 * SAX content handler for importing XML data to a JCR {@link Session Session}.
 * This utility class can be used to implement the
 * {@link Session#getImportContentHandler(String, int) Session.getImportContentHandler(String, int)}
 * method in terms of the
 * {@link Session#importXML(String, java.io.InputStream, int) Session.importXML(String, InputStream, int)}
 * method.
 *
 * @author Jukka Zitting
 */
public class SessionImportContentHandler extends ImportContentHandler {

    /** The repository session. */
    private Session session;

    /** The import content path. */
    private String path;

    /** The uuid behaviour mode */
    private int mode;

    /**
     * Creates a SAX content handler for importing XML data to the given
     * session and path.
     *
     * @param session repository session
     * @param path import content path
     * @param uuidBehaviour UUID behaviour mode
     */
    public SessionImportContentHandler(
            Session session, String path, int uuidBehaviour) {
        this.session = session;
        this.path = path;
        this.mode = uuidBehaviour;
    }

    /**
     * Imports the serialized XML stream using the standard
     * {@link Session#importXML(String, java.io.InputStream, int) Session.importXML(String, InputStream, int)}
     * method.
     *
     * {@inheritDoc}
     */
    protected void importXML(byte[] xml) throws Exception {
        session.importXML(path, new ByteArrayInputStream(xml), mode);
    }

}
