/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import javax.jcr.RepositoryException;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <code>AbstractConfig</code> is the superclass of
 * <code>RepositoryConfig</code> and <code>WorkspaceConfig</code>.
 */
abstract class AbstractConfig implements EntityResolver {
    private static Logger log = Logger.getLogger(AbstractConfig.class);

    public static final String CONFIG_DTD_RESOURCE_PATH =
            "org/apache/jackrabbit/core/config/config.dtd";

    protected static final String FILE_SYSTEM_ELEMENT = "FileSystem";
    protected static final String PATH_ATTRIB = "path";
    protected static final String PARAM_ELEMENT = "param";
    protected static final String CLASS_ATTRIB = "class";
    protected static final String NAME_ATTRIB = "name";
    protected static final String VALUE_ATTRIB = "value";

    protected final String configId;
    protected final Document config;

    /**
     * constructor
     *
     * @param is
     */
    protected AbstractConfig(InputSource is) throws RepositoryException {
        configId = is.getSystemId() == null ? "[???]" : is.getSystemId();
        try {
            SAXBuilder parser = new SAXBuilder();
            parser.setEntityResolver(this);
            config = parser.build(is);
        } catch (Exception e) {
            String msg = "error while parsing config file " + is.getSystemId();
            log.debug(msg);
            throw new RepositoryException(msg, e);
        }
    }

    //-------------------------------------------------------< EntityResolver >
    /**
     * @see EntityResolver#resolveEntity(String, String)
     */
    public abstract InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException;
}
