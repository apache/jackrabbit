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

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Entity resolver for Jackrabbit configuration files.
 * This simple resolver contains mappings for the following
 * public identifiers used for the Jackrabbit configuration files.
 * <ul>
 * <li><code>-//The Apache Software Foundation//DTD Workspace//EN</code></li>
 * <li><code>-//The Apache Software Foundation//DTD Repository//EN</code></li>
 * </ul>
 * <p>
 * The public identifiers are mapped to a document type definition
 * file included in the Jackrabbit jar archive.
 */
class ConfigurationEntityResolver implements EntityResolver {

    /**
     * Public identifier of the repository configuration DTD.
     */
    public static final String REPOSITORY_ID =
        "-//The Apache Software Foundation//DTD Repository//EN";

    /**
     * Public identifier of the workspace configuration DTD.
     */
    public static final String WORKSPACE_ID =
        "-//The Apache Software Foundation//DTD Workspace//EN";

    /**
     * Resource path of the internal configuration DTD file.
     */
    private static final String CONFIG_DTD =
            "org/apache/jackrabbit/core/config/config.dtd";

    /**
     * Resolves an entity to the corresponding input source.
     * {@inheritDoc}
     *
     * @param publicId public identifier
     * @param systemId system identifier
     * @return resolved entity source
     * @throws SAXException on SAX errors
     * @throws IOException on IO errors
     */
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        if (REPOSITORY_ID.equals(publicId) || WORKSPACE_ID.equals(publicId)) {
            InputStream dtd =
                getClass().getClassLoader().getResourceAsStream(CONFIG_DTD);
            return new InputSource(dtd);
        } else {
            return null;
        }
    }

}
