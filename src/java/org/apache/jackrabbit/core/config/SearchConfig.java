/*
 * Copyright 2004 The Apache Software Foundation.
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

import org.jdom.Element;
import org.apache.jackrabbit.core.fs.FileSystem;

import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * Implements the search configuration.
 */
public class SearchConfig {

    /** The <code>FileSystem</code> for the search index. */
    private final FileSystem fs;

    /** Parameters for configuring the search index. */
    private Map params = new HashMap();

    /**
     * Creates a new <code>SearchConfig</code>.
     * @param config the config root element for this <code>SearchConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    SearchConfig(Element config, Map vars) throws RepositoryException {
        // create FileSystem
        Element fsElement = config.getChild(AbstractConfig.FILE_SYSTEM_ELEMENT);
        this.fs = AbstractConfig.createFileSystem(fsElement, vars);

        // gather params
        List paramList = config.getChildren(AbstractConfig.PARAM_ELEMENT);
        for (Iterator i = paramList.iterator(); i.hasNext();) {
            Element param = (Element) i.next();
            String paramName = param.getAttributeValue(AbstractConfig.NAME_ATTRIB);
            String paramValue = param.getAttributeValue(AbstractConfig.VALUE_ATTRIB);
            // replace variables in param value
            params.put(paramName, AbstractConfig.replaceVars(paramValue, vars));
        }
        // seal
        params = Collections.unmodifiableMap(params);
    }

    /**
     * Returns configuration parameters. Each entry in the map represents
     * a name value pair. Where both name and values are <code>String</code>s.
     * @return Map of config parameters.
     */
    public Map getParameters() {
        return params;
    }

    /**
     * Returns <code>FileSystem</code> for search index persistence.
     * @return <code>FileSystem</code> for search index persistence.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

}
