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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.util.Text;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * TODO
 */
public class ConfigurationParser {

    /**
     * public id
     */
    public static final String PUBLIC_ID = "-//The Apache Software Foundation//DTD Workspace//EN";

    public static final String CONFIG_DTD_RESOURCE_PATH =
            "org/apache/jackrabbit/core/config/config.dtd";

    protected static final String FILE_SYSTEM_ELEMENT = "FileSystem";
    private static final String PERSISTENCE_MANAGER_ELEMENT = "PersistenceManager";
    private static final String SEARCH_INDEX_ELEMENT = "SearchIndex";

    private static final String CLASS_ATTRIBUTE = "class";

    private static final String PARAM_ELEMENT = "param";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String VALUE_ATTRIBUTE = "value";

    /** attribute name of home dir */
    private static final String ROOTPATH_ATTRIBUTE = "rootPath";

    /**
     * wellknown variables (will be replaced with their respective values
     * whereever they occur within the configuration)
     */
    public static final String WORKSPACE_HOME_VARIABLE = "${wsp.home}";
    public static final String WORKSPACE_NAME_VARIABLE = "${wsp.name}";

    /** FQN of the default query handler implementation */
    private static final String DEFAULT_QUERY_HANDLER
            = "org.apache.jackrabbit.core.search.lucene.SearchIndex";

    private Properties variables;

    public ConfigurationParser(Properties variables) {
        this.variables = variables;
    }

    /**
     * Creates a new <code>WorkspaceConfig</code> instance. The configuration
     * is read from the specified configuration file.
     *
     * @param configFilePath path to the configuration file
     * @param wspHomeDir     workspace home directory
     * @return a new <code>WorkspaceConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static WorkspaceConfig parseWorkspaceConfig(
            String configFilePath, String wspHomeDir)
            throws RepositoryException {
        try {
            File config = new File(configFilePath);
            InputSource is = new InputSource(new FileReader(config));
            is.setSystemId(config.toURI().toString());
            return parseWorkspaceConfig(is, wspHomeDir);
        } catch (IOException ioe) {
            String msg = "error while reading config file " + configFilePath;
            throw new RepositoryException(msg, ioe);
        }
    }

    /**
     * Creates a new <code>WorkspaceConfig</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param is         <code>InputSource</code> where the configuration is read from
     * @param wspHomeDir workspace home directory
     * @return a new <code>WorkspaceConfig</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static WorkspaceConfig parseWorkspaceConfig(
            InputSource xml, String home)
            throws RepositoryException {
        try {
            SAXBuilder builder = new SAXBuilder();
            builder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId)
                throws SAXException, IOException {
                    if (publicId.equals(PUBLIC_ID)) {
                        // load dtd resource
                        return new InputSource(getClass().getClassLoader().getResourceAsStream(CONFIG_DTD_RESOURCE_PATH));
                    } else {
                        // use the default behaviour
                        return null;
                    }
                }
            });
            Document config = builder.build(xml);

            Properties variables = new Properties();
            variables.setProperty(WORKSPACE_HOME_VARIABLE, home);
            ConfigurationParser parser = new ConfigurationParser(variables);
            return parser.parseWorkspaceConfig(config);
        } catch (JDOMException ex) {
            throw new RepositoryException(ex);
        } catch (IOException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Initializes this <code>WorkspaceConfig</code> object.
     *
     * @param config
     * @throws RepositoryException
     */
    public WorkspaceConfig parseWorkspaceConfig(Document config)
            throws RepositoryException {
        try {
            String wspHomeDir = variables.getProperty(WORKSPACE_HOME_VARIABLE);

            Element wspElem = config.getRootElement();
            // name
            String wspName = wspElem.getAttributeValue(NAME_ATTRIBUTE);
            if (wspName == null) {
                // init with wsp home dirname
                wspName = new File(wspHomeDir).getName();
            } else {
                wspName = replaceVariables(wspName);
            }

            // set name variable
            Properties newVariables = new Properties(variables);
            newVariables.put(WORKSPACE_NAME_VARIABLE, wspName);
            ConfigurationParser parser = new ConfigurationParser(newVariables);

            // file system
            BeanConfig fsc = parser.parseBeanConfig(wspElem, FILE_SYSTEM_ELEMENT);
            FileSystem wspFS = (FileSystem) fsc.newInstance();

            // persistence manager config
            BeanConfig pmc =
                parser.parseBeanConfig(wspElem, PERSISTENCE_MANAGER_ELEMENT);

            // search config (optional)
            Element searchElem = wspElem.getChild(SEARCH_INDEX_ELEMENT);
            SearchConfig sc = null;
            if (searchElem != null) {
                sc = parser.parseSearchConfig(searchElem);
            }
            
            return new WorkspaceConfig(wspHomeDir, wspName, wspFS, pmc, sc);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Creates a new <code>SearchConfig</code>.
     * @param config the config root element for this <code>SearchConfig</code>.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public SearchConfig parseSearchConfig(Element config) throws RepositoryException {
        try {
            // create FileSystem
            BeanConfig fsc = parseBeanConfig(config, FILE_SYSTEM_ELEMENT);
            FileSystem fs = (FileSystem) fsc.newInstance();

            // handler class name
            String handlerClassName = config.getAttributeValue(CLASS_ATTRIBUTE,
                    DEFAULT_QUERY_HANDLER);

            // gather params
            Properties params = parseParameters(config);

            return new SearchConfig(fs, handlerClassName, params);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    /**
     * Creates a new <code>VersioningConfig</code>.
     * @param config the config root element for this <code>VersioningConfig</code>.
     * @param vars map of variable values.
     * @throws RepositoryException if an error occurs while creating the
     *  <code>SearchConfig</code>.
     */
    public VersioningConfig parseVersioningConfig(Element config) throws RepositoryException {
        try {
            // home dir
            File homeDir = new File(replaceVariables(config.getAttributeValue(ROOTPATH_ATTRIBUTE)));

            // create FileSystem
            BeanConfig fsc = parseBeanConfig(config, FILE_SYSTEM_ELEMENT);
            FileSystem fs = (FileSystem) fsc.newInstance();

            // persistence manager config
            BeanConfig pmc = parseBeanConfig(config, PERSISTENCE_MANAGER_ELEMENT);

            return new VersioningConfig(homeDir, fs, pmc);
        } catch (ClassNotFoundException ex) {
            throw new RepositoryException(ex);
        } catch (InstantiationException ex) {
            throw new RepositoryException(ex);
        } catch (IllegalAccessException ex) {
            throw new RepositoryException(ex);
        } catch (ClassCastException ex) {
            throw new RepositoryException(ex);
        }
    }

    public BeanConfig parseBeanConfig(Element parent, String name)
            throws RepositoryException {
        Element element = parent.getChild(name);
        if (element != null) {
            String className = element.getAttributeValue(CLASS_ATTRIBUTE);
            Properties properties = parseParameters(element);
            return new BeanConfig(className, properties);
        } else {
            return null;
        }
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
