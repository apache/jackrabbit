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
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.NamespaceException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <code>NamespaceResolverImpl</code>...
 */
class NamespaceResolverImpl extends AbstractNamespaceResolver {

    private static Logger log = LoggerFactory.getLogger(NamespaceResolverImpl.class);

    // TODO: TO_BE_FIXED. missing notification and subsequent reloading of namespaces causes this resolver to throw NameException

    private Map<String, String> prefixToURI = new HashMap<String, String>();
    private Map<String, String> uriToPrefix = new HashMap<String, String>();

    void add(String prefix, String uri) {
        prefixToURI.put(prefix, uri);
        uriToPrefix.put(uri, prefix);
    }

    void remove(String prefix, String uri) {
        prefixToURI.remove(prefix);
        uriToPrefix.remove(uri);
    }

    Map<String, String> getNamespaces() {
        return Collections.unmodifiableMap(prefixToURI);
    }
    
    //--------------------------------------------------< NamespaceResolver >---
    /**
     * @see NamespaceResolver#getURI(String)
     */
    public String getURI(String prefix) throws NamespaceException {
        String uri = prefixToURI.get(prefix);
        if (uri == null) {
            throw new NamespaceException(prefix + ": is not a registered namespace prefix.");
        }
        return uri;
    }

    /**
     * @see org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver#getPrefix(String)
     */
    public String getPrefix(String uri) throws NamespaceException {
        String prefix = uriToPrefix.get(uri);
        if (prefix == null) {
            throw new NamespaceException(uri + ": is not a registered namespace uri.");
        }
        return prefix;
    }
}
