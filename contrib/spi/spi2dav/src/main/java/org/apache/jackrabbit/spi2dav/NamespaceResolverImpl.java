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
package org.apache.jackrabbit.spi2dav;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.name.AbstractNamespaceResolver;
import org.apache.jackrabbit.name.NamespaceResolver;

import javax.jcr.NamespaceException;
import java.util.Properties;

/**
 * <code>NamespaceResolverImpl</code>...
 */
public class NamespaceResolverImpl extends AbstractNamespaceResolver {

    private static Logger log = LoggerFactory.getLogger(NamespaceResolverImpl.class);

    private Properties prefixToURI = new Properties();
    private Properties uriToPrefix = new Properties();

    void add(String prefix, String uri) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    void remove(String prefix, String uri) {
        prefixToURI.remove(prefix);
        uriToPrefix.remove(uri);
    }

    Properties getNamespaces() {
        Properties namespaces = new Properties();
        namespaces.putAll(prefixToURI);
        return namespaces;
    }
    
    //--------------------------------------------------< NamespaceResolver >---
    /**
     * @see NamespaceResolver#getURI(String)
     */
    public String getURI(String prefix) throws NamespaceException {
        String uri = (String) prefixToURI.get(prefix);
        if (uri == null) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
        return uri;
    }

    /**
     * @see NamespaceResolver#getPrefix(String)
     */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = (String) uriToPrefix.get(uri);
        if (prefix == null) {
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
        return prefix;
    }
}