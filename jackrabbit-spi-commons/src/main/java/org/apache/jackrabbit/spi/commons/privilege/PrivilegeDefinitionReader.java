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
package org.apache.jackrabbit.spi.commons.privilege;

import org.apache.jackrabbit.spi.PrivilegeDefinition;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads privilege definitions for the specified <code>InputStream</code>. Note,
 * that this reader will not apply any validation.
 */
public class PrivilegeDefinitionReader {

    private final PrivilegeDefinition[] privilegeDefinitions;
    private final Map<String, String> namespaces = new HashMap<String, String>();

    /**
     * Creates a new <code>PrivilegeDefinitionReader</code> for the given
     * input stream. The specified content type is used in order to determine
     * the type of privilege serialization.
     *
     * @param in The input stream to read the privilege definitions from.
     * @param contentType Currently only types supported by
     * {@link PrivilegeXmlHandler#isSupportedContentType(String)}
     * are allowed.
     * @throws ParseException If an error occurs.
     * @throws IllegalArgumentException if the specified content type is not supported.
     */
    public PrivilegeDefinitionReader(InputStream in, String contentType) throws ParseException {
        if (PrivilegeXmlHandler.isSupportedContentType(contentType)) {
            PrivilegeHandler pxh = new PrivilegeXmlHandler();
            privilegeDefinitions = pxh.readDefinitions(in, namespaces);
        } else {
            // not yet supported
            throw new IllegalArgumentException("Unsupported content type " + contentType);
        }
    }

    /**
     * Creates a new <code>PrivilegeDefinitionReader</code> for the given
     * input stream. The specified content type is used in order to determine
     * the type of privilege serialization.
     *
     * @param reader The reader to read the privilege definitions from.
     * @param contentType Currently only types supported by
     * {@link PrivilegeXmlHandler#isSupportedContentType(String)}
     * are allowed.
     * @throws ParseException If an error occurs.
     * @throws IllegalArgumentException if the specified content type is not supported.
     */
    public PrivilegeDefinitionReader(Reader reader, String contentType) throws ParseException {
        if (PrivilegeXmlHandler.isSupportedContentType(contentType)) {
            PrivilegeHandler pxh = new PrivilegeXmlHandler();
            privilegeDefinitions = pxh.readDefinitions(reader, namespaces);
        } else {
            // not yet supported
            throw new IllegalArgumentException("Unsupported content type " + contentType);
        }
    }

    /**
     * Returns the privilege definitions retrieved from the input stream.
     *
     * @return an array of <code>PrivilegeDefinition</code>
     */
    public PrivilegeDefinition[] getPrivilegeDefinitions() {
        return privilegeDefinitions;
    }

    /**
     * Returns the namespace mappings such as retrieved during parsing.
     *
     * @return a mapping of namespace prefix to uri used by the privilege
     * definitions.
     */
    public Map<String,String> getNamespaces() {
        return namespaces;
    }
}