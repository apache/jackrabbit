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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * <code>IndexingConfigurationEntityResolver</code> implements an entity
 * resolver for the indexing configuration DTD.
 */
public class IndexingConfigurationEntityResolver implements EntityResolver {

    /**
     * Maps system ids to DTD resource names.
     */
    private static final Map<String, String> SYSTEM_IDS;

    static {
        Map<String, String> systemIds = new HashMap<String, String>();
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/indexing-configuration-1.0.dtd",
                "indexing-configuration-1.0.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/indexing-configuration-1.1.dtd",
                "indexing-configuration-1.1.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/indexing-configuration-1.2.dtd",
                "indexing-configuration-1.2.dtd");
        SYSTEM_IDS = Collections.unmodifiableMap(systemIds);
    }

    /**
     * {@inheritDoc}
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String resourceName = SYSTEM_IDS.get(systemId);
        if (resourceName != null) {
            InputStream in = getClass().getResourceAsStream(resourceName);
            if (in != null) {
                return new InputSource(in);
            }
        }
        return null;
    }
}
