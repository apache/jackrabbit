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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.jdom.Element;

/**
 * Implements the search configuration.
 */
public class SearchConfig extends BeanConfig {

    /** FQN of the default query handler implementation */
    private static final String DEFAULT_QUERY_HANDLER
            = "org.apache.jackrabbit.core.search.lucene.SearchIndex";

    /** The <code>FileSystem</code> for the search index. */
    private final FileSystem fs;

    /**
     * Creates a new <code>SearchConfig</code>.
     * @param config the config root element for this <code>SearchConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    static SearchConfig parse(Element config, Map vars) throws RepositoryException {
        // create FileSystem
        Element fsElement = config.getChild(AbstractConfig.FILE_SYSTEM_ELEMENT);
        FileSystem fs = AbstractConfig.createFileSystem(fsElement, vars);

        // gather params
        Properties params = new Properties();
        List paramList = config.getChildren(AbstractConfig.PARAM_ELEMENT);
        for (Iterator i = paramList.iterator(); i.hasNext();) {
            Element param = (Element) i.next();
            String paramName = param.getAttributeValue(AbstractConfig.NAME_ATTRIB);
            String paramValue = param.getAttributeValue(AbstractConfig.VALUE_ATTRIB);
            // replace variables in param value
            params.put(paramName, AbstractConfig.replaceVars(paramValue, vars));
        }

        // handler class name
        String handlerClassName = config.getAttributeValue(AbstractConfig.CLASS_ATTRIB,
                DEFAULT_QUERY_HANDLER);

        return new SearchConfig(fs, handlerClassName, params);
    }

    public SearchConfig(FileSystem fs, String className, Properties properties) {
        super(className, properties);
        this.fs = fs;
    }

    /**
     * Returns <code>FileSystem</code> for search index persistence.
     * @return <code>FileSystem</code> for search index persistence.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns the name of the class implementing the <code>QueryHandler</code>
     * interface.
     * @return the name of the class implementing the <code>QueryHandler</code>
     *   interface.
     */
    public String getHandlerClassName() {
        return getClassName();
    }

}
