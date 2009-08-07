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
package org.apache.jackrabbit.name;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.util.XMLChar;

/**
 * Dummy namespace resolver for use in unit testing. This namespace resolver
 * maps each valid XML prefix string to the same string as the namespace URI
 * and vice versa.
 */
class DummyNamespaceResolver implements NamespaceResolver {

    /**
     * Returns the given prefix as the corresponding namespace URI after
     * validating the correct format of the prefix.
     *
     * @param prefix namespace prefix
     * @return the given prefix
     * @throws NamespaceException if the given prefix is not a valid XML prefix
     */
    public String getURI(String prefix) throws NamespaceException {
        if (XMLChar.isValidNCName(prefix)) {
            return prefix;
        } else {
            throw new NamespaceException("Invalid prefix: " + prefix);
        }
    }

    /**
     * Returns the given namespace URI as the corresponding prefix.
     *
     * @param uri namespace URI
     * @return the given URI
     */
    public String getPrefix(String uri) {
        return uri;
    }

    /**
     * Not implemented.
     *
     * @param qName ignored
     * @return nothing
     * @throws UnsupportedOperationException always thrown
     */
    public String getJCRName(QName qName) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Not implemented.
     *
     * @param jcrName ignored
     * @return nothing
     * @throws UnsupportedOperationException always thrown
     */
    public QName getQName(String jcrName) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
