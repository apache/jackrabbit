/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.lite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.base.BaseNamespaceRegistry;

/**
 * TODO
 */
public class LiteNamespaceRegistry extends BaseNamespaceRegistry
        implements NamespaceRegistry {

    private final Map namespaces = new HashMap();

    public LiteNamespaceRegistry() {
        addNamespace("jcr", "http://www.jcp.org/jcr/1.0");
        addNamespace("nt", "http://www.jcp.org/jcr/nt/1.0");
        addNamespace("mix", "http://www.jcp.org/jcr/mix/1.0");
        addNamespace("xml", "http://www.w3.org/XML/1998/namespace");
        addNamespace("", "");
    }

    protected void addNamespace(String prefix, String uri) {
        namespaces.put(prefix, uri);
    }

    public String getPrefix(String uri) throws RepositoryException {
        Iterator iterator = namespaces.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (uri.equals(entry.getValue())) {
                return (String) entry.getKey();
            }
        }
        throw new NamespaceException("Namespace URI " + uri + " not found");
    }

    public String[] getPrefixes() throws RepositoryException {
        return (String[])
            namespaces.keySet().toArray(new String[namespaces.size()]);
    }

    public String getURI(String prefix) throws RepositoryException {
        String uri = (String) namespaces.get(prefix);
        if (uri != null) {
            return uri;
        } else {
            throw new NamespaceException("Prefix " + prefix + " not found");
        }
    }

    public String[] getURIs() throws RepositoryException {
        return (String[])
            namespaces.values().toArray(new String[namespaces.size()]);
    }

}
