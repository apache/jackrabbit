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
package org.apache.jackrabbit.commons.privilege;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Interface used to define the (de)serialization mode of the privilege definitions.
 */
public interface PrivilegeHandler {

    /**
     * Read the privilege definitions and update the specified namespace mapping.
     *
     * @param in
     * @param namespaces
     * @return the privilege definitions contained in the specified stream.
     * @throws ParseException
     */
    PrivilegeDefinition[] readDefinitions(InputStream in, Map<String, String> namespaces) throws ParseException;

    /**
     * Write the specified privilege definitions to the given output stream.
     * 
     * @param out
     * @param definitions
     * @param namespaces
     * @throws IOException
     */
    void writeDefinitions(OutputStream out, PrivilegeDefinition[] definitions, Map<String, String> namespaces) throws IOException;
}