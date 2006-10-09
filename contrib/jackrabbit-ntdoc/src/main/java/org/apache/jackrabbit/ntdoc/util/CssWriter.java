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
package org.apache.jackrabbit.ntdoc.util;

import java.io.*;

/**
 * This class implements the CSS writer.
 */
public final class CssWriter {
    /**
     * Print writer.
     */
    private final PrintWriter out;

    /**
     * Construct the document.
     */
    public CssWriter(Writer out) {
        this.out = new PrintWriter(out);
    }

    /**
     * Start class.
     */
    public CssWriter enter(String name) {
        this.out.println(name + " {");
        return this;
    }

    /**
     * Add attribute.
     */
    public CssWriter attrib(String name, String value) {
        this.out.println(name + ": " + value + ";");
        return this;
    }

    /**
     * End class.
     */
    public CssWriter leave() {
        this.out.println("}");
        return this;
    }

    /**
     * Flush the document.
     */
    public void flush() {
        this.out.flush();
    }

    /**
     * Close the document.
     */
    public void close() {
        this.out.close();
    }
}
