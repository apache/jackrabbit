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
package org.apache.jackrabbit.core.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Entity resolver for Jackrabbit configuration files.
 * This simple resolver contains mappings for the following
 * public identifiers used for the Jackrabbit configuration files:
 * <ul>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 2.6//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 2.4//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 2.0//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 1.6//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 1.5//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 1.4//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 1.2//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Jackrabbit 1.0//EN</code></li>
 * </ul>
 * <p>
 * Also the following system identifiers are mapped to local resources:
 * <ul>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-2.6.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-2.4.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-2.0.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-1.6.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-1.5.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-1.4.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-1.2.dtd</code></li>
 * <li><code>http://jackrabbit.apache.org/dtd/repository-1.0.dtd</code></li>
 * </ul>
 * <p>
 * The public identifiers are mapped to document type definition
 * files included in the Jackrabbit jar archive.
 */
public class ConfigurationEntityResolver implements EntityResolver {

    /**
     * The singleton instance of this class.
     */
    public static final EntityResolver INSTANCE =
        new ConfigurationEntityResolver();

    /**
     * Public identifiers.
     */
    private final Map<String, String> publicIds = new HashMap<String, String>();

    /**
     * System identifiers.
     */
    private final Map<String, String> systemIds = new HashMap<String, String>();

    /**
     * Creates the singleton instance of this class.
     */
    private ConfigurationEntityResolver() {
        // Apache Jackrabbit 2.6 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.6//EN",
                "repository-2.6.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.6.dtd",
                "repository-2.6.dtd");
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.6 Elements//EN",
                "repository-2.6-elements.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.6-elements.dtd",
                "repository-2.6-elements.dtd");

        
        // Apache Jackrabbit 2.4 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.4//EN",
                "repository-2.4.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.4.dtd",
                "repository-2.4.dtd");
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.4 Elements//EN",
                "repository-2.4-elements.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.4-elements.dtd",
                "repository-2.4-elements.dtd");

        // Apache Jackrabbit 2.0 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.0//EN",
                "repository-2.0.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.0.dtd",
                "repository-2.0.dtd");
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 2.0 Elements//EN",
                "repository-2.0-elements.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-2.0-elements.dtd",
                "repository-2.0-elements.dtd");

        // Apache Jackrabbit 1.6 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 1.6//EN",
                "repository-1.6.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-1.6.dtd",
                "repository-1.6.dtd");

        // Apache Jackrabbit 1.5 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 1.5//EN",
                "repository-1.5.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-1.5.dtd",
                "repository-1.5.dtd");

        // Apache Jackrabbit 1.4 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 1.4//EN",
                "repository-1.4.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-1.4.dtd",
                "repository-1.4.dtd");

        // Apache Jackrabbit 1.2 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 1.2//EN",
                "repository-1.2.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-1.2.dtd",
                "repository-1.2.dtd");

        // Apache Jackrabbit 1.0 DTD
        publicIds.put(
                "-//The Apache Software Foundation//DTD Jackrabbit 1.0//EN",
                "repository-1.0.dtd");
        systemIds.put(
                "http://jackrabbit.apache.org/dtd/repository-1.0.dtd",
                "repository-1.0.dtd");
    }

    /**
     * Resolves an entity to the corresponding input source.
     *
     * @param publicId public identifier
     * @param systemId system identifier
     * @return resolved entity source
     * @throws SAXException on SAX errors
     * @throws IOException on IO errors
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        String name;

        name = publicIds.get(publicId);
        if (name != null) {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream != null) {
                return new InputSource(stream);
            }
        }

        name = systemIds.get(systemId);
        if (name != null) {
            InputStream stream = getClass().getResourceAsStream(name);
            if (stream != null) {
                return new InputSource(stream);
            }
        }

        return null;
    }

}
