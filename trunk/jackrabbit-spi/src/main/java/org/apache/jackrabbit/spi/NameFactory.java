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
package org.apache.jackrabbit.spi;

/**
 * <code>NameFactory</code>...
 */
public interface NameFactory {

    /**
     * Returns a <code>Name</code> with the given namespace URI and
     * local part and validates the given parameters.
     *
     * @param namespaceURI namespace uri
     * @param localName local part
     * @throws IllegalArgumentException if <code>namespaceURI</code> or
     * <code>localName</code> is invalid.
     */
    public Name create(String namespaceURI, String localName) throws IllegalArgumentException;

    /**
     * Returns a <code>Name</code> holding the value of the specified
     * string. The string must be in the format returned by the
     * <code>Name.toString()</code> method, i.e.
     * <p>
     * <code><b>{</b>namespaceURI<b>}</b>localName</code>
     *
     * @param nameString a <code>String</code> containing the <code>Name</code>
     * representation to be parsed.
     * @return the <code>Name</code> represented by the argument
     * @throws IllegalArgumentException if the specified string can not be parsed
     * as a <code>Name</code>.
     */
    public Name create(String nameString) throws IllegalArgumentException;
}