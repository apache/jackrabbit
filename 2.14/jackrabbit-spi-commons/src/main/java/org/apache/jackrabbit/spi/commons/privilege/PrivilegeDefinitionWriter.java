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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

/**
 * Writes privilege definitions to an output stream.
 */
public class PrivilegeDefinitionWriter {

    private final PrivilegeHandler ph;

    /**
     * Creates a new <code>PrivilegeDefinitionWriter</code>.
     *
     * @param contentType The content type used to determine the type of
     * serialization.
     * @throws IllegalArgumentException if the specified content type is not
     * supported.
     */
    public PrivilegeDefinitionWriter(String contentType) {
        if (PrivilegeXmlHandler.isSupportedContentType(contentType)) {
            ph = new PrivilegeXmlHandler();
        } else {
            // not yet supported
            throw new IllegalArgumentException("Unsupported content type");
        }
    }

    /**
     * Writes the privilege definitions to the specified output stream.
     * 
     * @param out The output stream.
     * @param privilegeDefinitions The privilege definitions to write to the
     * given output stream.
     * @param namespaces The namespace mapping (prefix to uri) used by the
     * specified definitions.
     * @throws IOException If an error occurs.
     */
    public void writeDefinitions(OutputStream out, PrivilegeDefinition[] privilegeDefinitions, Map<String, String> namespaces) throws IOException {
        ph.writeDefinitions(out, privilegeDefinitions, namespaces);
    }

    /**
     * Writes the privilege definitions to the specified output stream.
     *
     * @param writer The writer.
     * @param privilegeDefinitions The privilege definitions to write to the
     * given output stream.
     * @param namespaces The namespace mapping (prefix to uri) used by the
     * specified definitions.
     * @throws IOException If an error occurs.
     */
    public void writeDefinitions(Writer writer, PrivilegeDefinition[] privilegeDefinitions, Map<String, String> namespaces) throws IOException {
        ph.writeDefinitions(writer, privilegeDefinitions, namespaces);
    }
}