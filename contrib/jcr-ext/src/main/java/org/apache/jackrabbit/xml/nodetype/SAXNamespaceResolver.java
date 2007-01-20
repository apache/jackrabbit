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
package org.apache.jackrabbit.xml.nodetype;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.name.IllegalNameException;
import org.apache.jackrabbit.name.NamespaceResolver;
import org.apache.jackrabbit.name.NoPrefixDeclaredException;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.UnknownPrefixException;

/**
 * TODO
 */
public class SAXNamespaceResolver implements NamespaceResolver {

    private final LinkedList stack = new LinkedList();

    public String getJCRName(QName qname) throws NoPrefixDeclaredException {
        return qname.toJCRName(this);
    }

    public QName getQName(String name) throws IllegalNameException, UnknownPrefixException {
        return QName.fromJCRName(name, this);
    }

    public void startDocument() {
        stack.addFirst(new HashMap());
    }

    public void endDocument() {
        stack.removeFirst();
    }

    public void startElement() {
        stack.addFirst(new HashMap());
    }

    public void endElement() {
        stack.removeFirst();
    }
    
    public void startPrefixMapping(String prefix, String uri) {
        Map context = (Map) stack.getFirst();
        context.put(prefix, uri);
    }

    public void endPrefixMapping(String prefix) {
        Map context = (Map) stack.getFirst();
        context.remove(prefix);
    }

    public String getURI(String prefix) throws NamespaceException {
        Iterator iterator = stack.iterator();
        while (iterator.hasNext()) {
            Map context = (Map) iterator.next();
            String uri = (String) context.get(prefix);
            if (uri != null) {
                return uri;
            }
        }
        throw new NamespaceException("Prefix " + prefix + " not found");
    }

    public String getPrefix(String uri) throws NamespaceException {
        Iterator iterator = stack.iterator();
        while (iterator.hasNext()) {
            Map context = (Map) iterator.next();
            Iterator entries = context.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry entry = (Map.Entry) entries.next();
                if (uri.equals(entry.getValue())) {
                    return (String) entry.getKey();
                }
            }
        }
        throw new NamespaceException("Namespace URI " + uri + " not found");
    }

}
