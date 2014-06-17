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
package org.apache.jackrabbit.core.query.lucene;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a wildcard query on the name field.
 * <p>
 * Wildcards are:
 * <ul>
 * <li><code>%</code> : matches zero or more characters</li>
 * <li><code>_</code> : matches exactly one character</li>
 * </ul>
 * Wildcards in the namespace prefix are not supported and will not match.
 */
public class WildcardNameQuery extends WildcardQuery {

    private static final long serialVersionUID = -4705104992551930918L;

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(WildcardNameQuery.class);

    public WildcardNameQuery(String pattern,
                             int transform,
                             NamespaceResolver resolver,
                             NamespaceMappings nsMappings,
                             PerQueryCache cache) {
        super(FieldNames.LABEL, null,
                convertPattern(pattern, resolver, nsMappings), transform, cache);
    }

    private static String convertPattern(String pattern,
                                         NamespaceResolver resolver,
                                         NamespaceMappings nsMappings) {
        String prefix = "";
        int idx = pattern.indexOf(':');
        if (idx != -1) {
            prefix = pattern.substring(0, idx);
        }
        StringBuffer sb = new StringBuffer();
        // translate prefix
        try {
            sb.append(nsMappings.getPrefix(resolver.getURI(prefix)));
        } catch (NamespaceException e) {
            // prefix in pattern is probably unknown
            log.debug("unknown namespace prefix in pattern: " + pattern);
            // -> ignore and use empty string for index internal prefix
            //    this will not match anything
        }
        sb.append(":");
        // remaining pattern, may also be whole pattern
        sb.append(pattern.substring(idx + 1));
        return sb.toString();
    }
}
