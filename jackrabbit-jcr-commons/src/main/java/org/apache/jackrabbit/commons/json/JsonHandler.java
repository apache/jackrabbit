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
package org.apache.jackrabbit.commons.json;

import java.io.IOException;

/**
 * The <code>JSONHandler</code> interface recieves notifications from the
 * <code>JsonParser</code>.
 */
public interface JsonHandler {

    /**
     * Recieve notification about the start of an JSON object.
     *
     * @throws IOException If an error occurs.
     */
    void object() throws IOException;

    /**
     * Recieve notification about the end of an JSON object.
     *
     * @throws IOException If an error occurs.
     */
    void endObject() throws IOException;

    /**
     * Recieve notification about the start of an JSON array.
     *
     * @throws IOException If an error occurs.
     */
    void array() throws IOException;

    /**
     * Recieve notification about the end of an JSON array.
     *
     * @throws IOException If an error occurs.
     */
    void endArray() throws IOException;

    /**
     * Recieve notification about the given JSON key.
     *
     * @param key The key.
     * @throws IOException If an error occurs.
     */
    void key(String key) throws IOException;

    /**
     * Recieve notification about the given JSON String value.
     *
     * @param value The value.
     * @throws IOException If an error occurs.
     */
    void value(String value) throws IOException;

    /**
     * Recieve notification about the given JSON boolean value.
     *
     * @param value The value.
     * @throws IOException If an error occurs.
     */
    void value(boolean value) throws IOException;

    /**
     * Recieve notification about the given JSON number value (long).
     *
     * @param value The value.
     * @throws IOException If an error occurs.
     */
    void value(long value) throws IOException;

    /**
     * Recieve notification about the given JSON number value (double).
     *
     * @param value The value.
     * @throws IOException If an error occurs.
     */
    void value(double value) throws IOException;
}