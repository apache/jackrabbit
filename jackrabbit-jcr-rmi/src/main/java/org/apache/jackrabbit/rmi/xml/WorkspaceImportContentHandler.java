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

import javax.jcr.Workspace;


/**
 * SAX content handler for importing XML data to a JCR {@link Workspace Workspace}.
 * This utility class can be used to implement the
 * {@link Workspace#getImportContentHandler(String, int) Workspace.getImportContentHandler(String, int)}
 * method in terms of the
 * {@link Workspace#importXML(String, java.io.InputStream, int) Workspace.importXML(String, InputStream, int)}
 * method.
 *
 * @author Jukka Zitting
 */
public class WorkspaceImportContentHandler extends ImportContentHandler {

    /** The repository workspace. */
    private Workspace workspace;

    /** The import content path. */
    private String path;

    /** The UUID behaviour. */
    private int uuidBehaviour;

    /**
     * Creates a SAX content handler for importing XML data to the given
     * workspace and path using the given UUID behaviour.
     *
     * @param workspace repository workspace
     * @param path import content path
     * @param uuidBehaviour UUID behaviour
     */
    public WorkspaceImportContentHandler(Workspace workspace, String path, int uuidBehaviour) {
        this.workspace = workspace;
        this.path = path;
        this.uuidBehaviour = uuidBehaviour;
    }

    /**
     * Imports the serialized XML stream using the standard
     * {@link Workspace#importXML(String, java.io.InputStream, int) Workspace.importXML(String, InputStream, int)}
     * method.
     *
     * {@inheritDoc}
     */
    protected void importXML(byte[] xml) throws Exception {
        workspace.importXML(path, new ByteArrayInputStream(xml), uuidBehaviour);
    }

}
