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

import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * A <code>WorkspaceConfig</code> ...
 */
public class WorkspaceConfig extends AbstractConfig {
    private static Logger log = Logger.getLogger(WorkspaceConfig.class);

    /**
     * name of workspace configuration file
     */
    public static final String CONFIG_FILE_NAME = "workspace.xml";

    /**
     * public id
     */
    public static final String PUBLIC_ID = "-//The Apache Software Foundation//DTD Workspace//EN";

    static final String PERSISTENCE_MANAGER_ELEMENT = "PersistenceManager";
    private static final String SEARCH_INDEX_ELEMENT = "SearchIndex";

    /**
     * wellknown variables (will be replaced with their respective values
     * whereever they occur within the configuration)
     */
    public static final String WORKSPACE_HOME_VARIABLE = "${wsp.home}";
    public static final String WORKSPACE_NAME_VARIABLE = "${wsp.name}";

    private final HashMap vars;

    /**
     * workspace home directory
     */
    private String wspHomeDir;

    /**
     * virtual file system where the workspace stores meta data etc.
     */
    private FileSystem wspFS;

    /**
     * workspace name
     */
    private String wspName;

    /**
     * configuration for the persistence manager
     */
    private PersistenceManagerConfig pmConfig;

    /**
     * configuration for the search manager
     */
    private SearchConfig searchConfig;

    /**
     * private constructor.
     *
     * @param is
     * @param wspHomeDir
     * @throws RepositoryException
     */
    private WorkspaceConfig(InputSource is, String wspHomeDir)
            throws RepositoryException {
        super(is);
        this.wspHomeDir = wspHomeDir;
        // initialize variables
        vars = new HashMap();
        vars.put(WORKSPACE_HOME_VARIABLE, wspHomeDir);
        // read config
        init(config);
    }

    /**
     * Initializes this <code>WorkspaceConfig</code> object.
     *
     * @param config
     * @throws RepositoryException
     */
    protected void init(Document config) throws RepositoryException {
        Element wspElem = config.getRootElement();
        // name
        wspName = wspElem.getAttributeValue(NAME_ATTRIB);
        if (wspName == null) {
            // init with wsp home dirname
            wspName = new File(wspHomeDir).getName();
        } else {
            wspName = replaceVars(wspName, vars);
        }
        
        // set name variable
        vars.put(WORKSPACE_NAME_VARIABLE, wspName);

        // file system
        Element fsElem = wspElem.getChild(FILE_SYSTEM_ELEMENT);
        wspFS = createFileSystem(fsElem, vars);

        // persistence manager config
        Element pmElem = wspElem.getChild(PERSISTENCE_MANAGER_ELEMENT);
        pmConfig = new PersistenceManagerConfig(pmElem, vars);

        // search config (optional)
        Element searchElem = wspElem.getChild(SEARCH_INDEX_ELEMENT);
        if (searchElem != null) {
            searchConfig = new SearchConfig(searchElem, vars);
        }
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
    public static WorkspaceConfig create(String configFilePath, String wspHomeDir)
            throws RepositoryException {
        try {
            File config = new File(configFilePath);
            InputSource is = new InputSource(new FileReader(config));
            is.setSystemId(config.toURI().toString());
            return new WorkspaceConfig(is, wspHomeDir);
        } catch (IOException ioe) {
            String msg = "error while reading config file " + configFilePath;
            log.debug(msg);
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
    public static WorkspaceConfig create(InputSource is, String wspHomeDir)
            throws RepositoryException {
        return new WorkspaceConfig(is, wspHomeDir);
    }

    /**
     * Returns the home directory of the workspace.
     *
     * @return the home directory of the workspace
     */
    public String getHomeDir() {
        return wspHomeDir;
    }

    /**
     * Returns the workspace name.
     *
     * @return the workspace name
     */
    public String getName() {
        return wspName;
    }

    /**
     * Returns the virtual file system where the workspace stores global state.
     *
     * @return the virtual file system where the workspace stores global state
     */
    public FileSystem getFileSystem() {
        return wspFS;
    }

    /**
     * Returns the configuration of the persistence manager.
     *
     * @return the <code>PersistenceManagerConfig</code> for this workspace
     */
    public PersistenceManagerConfig getPersistenceManagerConfig() {
        return pmConfig;
    }

    /**
     * Returns the configuration of the search manager.
     * Returns <code>null</code> if no search manager is configured.
     *
     * @return the <code>SearchConfig</code> for this workspace
     */
    public SearchConfig getSearchConfig() {
        return searchConfig;
    }

    //------------------------------------------------------< EntityResolver >
    /**
     * @see org.xml.sax.EntityResolver#resolveEntity(String, String)
     */
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
}
