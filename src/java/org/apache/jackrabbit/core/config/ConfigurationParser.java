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

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.commons.collections.BeanMap;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.util.Text;
import org.jdom.Element;

/**
 * TODO
 */
public class ConfigurationParser {

    private static final String CLASS_ATTRIBUTE = "class";

    private static final String PARAM_ELEMENT = "param";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";

    /** attribute name of home dir */
    private static final String ROOTPATH_ATTRIBUTE = "rootPath";


    /** FQN of the default query handler implementation */
    private static final String DEFAULT_QUERY_HANDLER
            = "org.apache.jackrabbit.core.search.lucene.SearchIndex";

    private Map variables;

    public ConfigurationParser(Map variables) {
        this.variables = variables;
    }

    /**
     * Creates a new <code>PersistenceManagerConfig</code>.
     *
     * @param config the config root element for this <code>PersistenceManagerConfig</code>.
     */
    public AccessManagerConfig parseAccessManagerConfig(Element config) {
        // FQN of persistence manager class
        String className = config.getAttributeValue(CLASS_ATTRIBUTE);

        // read the PersistenceManager properties from the
        // <param/> elements in the config
        Properties params = parseParameters(config);

        return new AccessManagerConfig(className, params);
    }

    /**
     * Creates a new <code>PersistenceManagerConfig</code>.
     *
     * @param config the config root element for this <code>PersistenceManagerConfig</code>.
     */
    public PersistenceManagerConfig parsePersistenceManagerConfig(Element config) {
        // FQN of persistence manager class
        String className = config.getAttributeValue(CLASS_ATTRIBUTE);

        // read the PersistenceManager properties from the
        // <param/> elements in the config
        Properties params = parseParameters(config);

        return new PersistenceManagerConfig(className, params);
    }

    /**
     * Creates a new <code>SearchConfig</code>.
     * @param config the config root element for this <code>SearchConfig</code>.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public SearchConfig parseSearchConfig(Element config) throws RepositoryException {
        // create FileSystem
        Element fsElement = config.getChild(AbstractConfig.FILE_SYSTEM_ELEMENT);
        FileSystem fs = createFileSystem(fsElement);

        // handler class name
        String handlerClassName = config.getAttributeValue(CLASS_ATTRIBUTE,
                DEFAULT_QUERY_HANDLER);

        // gather params
        Properties params = parseParameters(config);

        return new SearchConfig(fs, handlerClassName, params);
    }

    /**
     * Creates a new <code>VersioningConfig</code>.
     * @param config the config root element for this <code>VersioningConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public VersioningConfig parseVersioningConfig(Element config) throws RepositoryException {
        // home dir
        File homeDir = new File(replaceVariables(config.getAttributeValue(ROOTPATH_ATTRIBUTE)));

        // create FileSystem
        Element fsElement = config.getChild(AbstractConfig.FILE_SYSTEM_ELEMENT);
        FileSystem fs = createFileSystem(fsElement);

        // persistence manager config
        Element pmElem = config.getChild(WorkspaceConfig.PERSISTENCE_MANAGER_ELEMENT);
        PersistenceManagerConfig pmConfig =
            parsePersistenceManagerConfig(pmElem);

        return new VersioningConfig(homeDir, fs, pmConfig);
    }

    public Properties parseParameters(Element element) {
        Properties parameters = new Properties();

        Iterator iterator = element.getChildren(PARAM_ELEMENT).iterator();
        while (iterator.hasNext()) {
            Element parameter = (Element) iterator.next();
            String name = parameter.getAttributeValue(NAME_ATTRIBUTE);
            String value = parameter.getAttributeValue(VALUE_ATTRIBUTE);
            // replace variables in param value
            parameters.put(name, replaceVariables(value));
        }

        return parameters;
    }

    /**
     * Creates a {@link org.apache.jackrabbit.core.fs.FileSystem} instance
     * based on the config <code>fsConfig</code>.
     *
     * @param fsConfig  a {@link #FILE_SYSTEM_ELEMENT}.
     * @return a {@link org.apache.jackrabbit.core.fs.FileSystem} instance.
     * @throws RepositoryException if an error occurs while creating the
     *                             {@link org.apache.jackrabbit.core.fs.FileSystem}.
     */
    public FileSystem createFileSystem(Element fsConfig) throws RepositoryException {
        FileSystem fs;
        String className = "";
        try {
            // create the file system object
            className = fsConfig.getAttributeValue(CLASS_ATTRIBUTE);
            Class c = Class.forName(className);
            fs = (FileSystem) c.newInstance();

            // set the properties of the file system object from the
            // param elements in the config
            BeanMap bm = new BeanMap(fs);
            List paramList = fsConfig.getChildren(PARAM_ELEMENT);
            for (Iterator i = paramList.iterator(); i.hasNext();) {
                Element param = (Element) i.next();
                String paramName = param.getAttributeValue(NAME_ATTRIBUTE);
                String paramValue = param.getAttributeValue(VALUE_ATTRIBUTE);
                // replace variables in param value
                bm.put(paramName, replaceVariables(paramValue));
            }
            fs.init();
        } catch (Exception e) {
            String msg = "Cannot instantiate implementing class " + className;
            throw new RepositoryException(msg, e);
        }
        return fs;
    }

    /**
     * Helper method that replaces in the given string any occurences of the keys
     * in the specified map with their associated values.
     *
     * @param s
     * @param vars
     * @return
     */
    public String replaceVariables(String value) {
        Iterator iterator = variables.keySet().iterator();
        while (iterator.hasNext()) {
            String varName = (String) iterator.next();
            String varValue = (String) variables.get(varName);
            value = Text.replace(value, varName, varValue);
        }
        return value;
    }

}
