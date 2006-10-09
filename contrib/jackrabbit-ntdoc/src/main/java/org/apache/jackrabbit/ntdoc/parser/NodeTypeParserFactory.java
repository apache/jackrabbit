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
package org.apache.jackrabbit.ntdoc.parser;

import java.io.*;

/**
 * This class defines the node type parser helper.
 */
public final class NodeTypeParserFactory {
    /**
     * Return the parser for extension.
     */
    public static NodeTypeParser newParser(String ext)
            throws IOException {
        if (ext.equalsIgnoreCase("cnd")) {
            return new CNDNodeTypeParser();
        } else if (ext.equalsIgnoreCase("xml")) {
            return new XMLNodeTypeParser();
        } else {
            throw new IOException("Extension '" + ext + "' is not supported");
        }
    }

    /**
     * Create a new parser for file.
     */
    public static NodeTypeParser newParser(File file)
            throws IOException {
        String systemId = file.getPath();
        InputStream in = new FileInputStream(file);
        return newParser(systemId, in);
    }

    /**
     * Create a new parser for reader.
     */
    public static NodeTypeParser newParser(String systemId, InputStream in)
            throws IOException {
        int pos = systemId.lastIndexOf('.');
        NodeTypeParser parser = newParser(pos > -1 ? systemId.substring(pos + 1) : "");
        parser.setSystemId(systemId);
        parser.setInputStream(in);
        return parser;
    }
}
