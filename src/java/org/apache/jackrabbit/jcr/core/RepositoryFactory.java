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
package org.apache.jackrabbit.jcr.core;

import org.apache.commons.collections.BeanMap;
import org.apache.log4j.Logger;
import org.apache.jackrabbit.jcr.fs.FileSystem;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A <code>RepositoryFactory</code>.
 * A singleton class. On creation, it reads in the default config file "config.xml" which describes
 * the available repositories. A sample config.xml (including DTD):
 * <pre>
 *  &lt;?xml version="1.0" encoding="ISO-8859-1"?&gt;
 *  &lt;!DOCTYPE Repositories [
 *    &lt;!ELEMENT Repositories (Repository+)&gt;
 *    &lt;!ELEMENT Repository (RepositoryStore,StableWorkspace+)&gt;
 *    &lt;!ATTLIST Repository
 *      name CDATA #REQUIRED&gt;
 *    &lt;!ELEMENT RepositoryStore (FileSystem)&gt;
 *    &lt;!ELEMENT StableWorkspace (PersistenceManager,WorkspaceStore,BLOBStore?,DynamicWorkspace*)&gt;
 *    &lt;!ATTLIST StableWorkspace
 *      name CDATA #REQUIRED&gt;
 *    &lt;!ELEMENT PersistenceManager (EMPTY)&gt;
 *    &lt;!ATTLIST PersistenceManager
 *      class CDATA #REQUIRED&gt;
 *    &lt;!ELEMENT WorkspaceStore (FileSystem)&gt;
 *    &lt;!ELEMENT BLOBStore (FileSystem)&gt;
 *    &lt;!ELEMENT DynamicWorkspace (PersistenceManager,WorkspaceStore,BLOBStore?)&gt;
 *    &lt;!ATTLIST DynamicWorkspace
 *      name CDATA #REQUIRED&gt;
 *    &lt;!ELEMENT FileSystem (param*)&gt;
 *    &lt;!ATTLIST FileSystem
 *      class CDATA #REQUIRED&gt;
 *    &lt;!ELEMENT param EMPTY&gt;
 *    &lt;!ATTLIST param
 *      name CDATA #REQUIRED
 *      value CDATA #REQUIRED&gt;]&gt;
 *  &lt;Repositories&gt;
 *    &lt;!-- sample configuration for a repository that stores its state in a local file system --&gt;
 *    &lt;Repository name="localfs"&gt;
 *      &lt;RepositoryStore&gt;
 *        &lt;FileSystem class="org.apache.jackrabbit.jcr.fs.local.LocalFileSystem"&gt;
 *          &lt;param name="path" value="${factory.home}/localfs/repository"/&gt;
 *        &lt;/FileSystem&gt;
 *      &lt;/RepositoryStore&gt;
 *      &lt;!-- main workspace --&gt;
 *      &lt;StableWorkspace name="default"&gt;
 *        &lt;PersistenceManager class="org.apache.jackrabbit.jcr.core.state.xml.XMLPersistenceManager"/&gt;
 *        &lt;WorkspaceStore&gt;
 *          &lt;FileSystem class="org.apache.jackrabbit.jcr.fs.local.LocalFileSystem"&gt;
 *            &lt;param name="path" value="${factory.home}/localfs/workspaces/default/data"/&gt;
 *          &lt;/FileSystem&gt;
 *        &lt;/WorkspaceStore&gt;
 *        &lt;BLOBStore&gt;
 *          &lt;FileSystem class="org.apache.jackrabbit.jcr.fs.local.LocalFileSystem"&gt;
 *            &lt;param name="path" value="${factory.home}/localfs/workspaces/default/blobs"/&gt;
 *          &lt;/FileSystem&gt;
 *        &lt;/BLOBStore&gt;
 *        &lt;!-- dynamic workspace based on main workspace --&gt;
 *        &lt;DynamicWorkspace name="test1"&gt;
 *          &lt;PersistenceManager class="org.apache.jackrabbit.jcr.core.state.xml.XMLPersistenceManager"/&gt;
 *          &lt;WorkspaceStore&gt;
 *            &lt;FileSystem class="org.apache.jackrabbit.jcr.fs.local.LocalFileSystem"&gt;
 *              &lt;param name="path" value="${factory.home}/localfs/workspaces/test1/data"/&gt;
 *            &lt;/FileSystem&gt;
 *          &lt;/WorkspaceStore&gt;
 *        &lt;/DynamicWorkspace&gt;
 *      &lt;/StableWorkspace&gt;
 *    &lt;/Repository&gt;
 *  &lt;/Repositories&gt;
 * </pre>
 *
 * @author Stefan Guggisberg
 * @version $Revision: 1.22 $, $Date: 2004/08/24 12:57:01 $
 */
public final class RepositoryFactory {
    private static Logger log = Logger.getLogger(RepositoryFactory.class);

    public static final String REPOSITORY_CONFIG_SYSTEM_PROPERTY =
	    "Repository.factory.config";
    public static final String DEFAULT_CONFIG_FILE = "config.xml";

    private static final String REPOSITORY_ELEMENT = "Repository";
    private static final String REPOSITORY_STORE_ELEMENT = "RepositoryStore";
    private static final String FILE_SYSTEM_ELEMENT = "FileSystem";
    private static final String STABLE_WORKSPACE_ELEMENT = "StableWorkspace";
    private static final String PERSISTENCE_MANAGER_ELEMENT = "PersistenceManager";
    private static final String WORKSPACE_STORE_ELEMENT = "WorkspaceStore";
    private static final String BLOB_STORE_ELEMENT = "BLOBStore";
    private static final String DYNAMIC_WORKSPACE_ELEMENT = "DynamicWorkspace";
    private static final String PARAM_ELEMENT = "param";
    private static final String CLASS_ATTRIB = "class";
    private static final String NAME_ATTRIB = "name";
    private static final String VALUE_ATTRIB = "value";

    private static final String FACTORY_HOME_VARIABLE = "${factory.home}";

    /**
     * factory home dir; default location of config.xml
     */
    private String factoryHomeDir;

    private final String configId;
    private Document config;

    // map of repository names and repository instances
    private final HashMap reps = new HashMap();

    /**
     * private constructor.
     *
     * @param is
     * @param factoryHomeDir
     * @throws RepositoryException
     */
    private RepositoryFactory(InputSource is, String factoryHomeDir) throws RepositoryException {
	configId = is.getSystemId() == null ? "[???]" : is.getSystemId();
	this.factoryHomeDir = factoryHomeDir;
	init(is);
    }

    /**
     * Initializes repository factory.
     *
     * @throws RepositoryException
     */
    private void init(InputSource is) throws RepositoryException {
	try {
	    SAXBuilder parser = new SAXBuilder();
	    config = parser.build(is);
	} catch (Exception e) {
	    String msg = "error while parsing config file " + is.getSystemId();
	    log.error(msg, e);
	    throw new RepositoryException(msg, e);
	}
    }

    /**
     * Creates a new <code>RepositoryFactory</code> instance.
     * The configuration is loaded from the file located at the path stored in
     * the system property <b><code>"Repository.factory.config"</code></b>.
     * If this system property is not set then the path <code>./config.xml</code>
     * is tried.
     *
     * @return a new <code>RepositoryFactory</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryFactory create() throws RepositoryException {
	// no factory home dir specified, use cwd
	String factoryHomeDir = System.getProperty("user.dir");

	String configPath = System.getProperty(REPOSITORY_CONFIG_SYSTEM_PROPERTY);
	if (configPath == null) {
	    configPath = factoryHomeDir + File.separator + DEFAULT_CONFIG_FILE;
	}

	return create(configPath, factoryHomeDir);
    }

    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param configFilePath path to the configuration file
     * @return a new <code>RepositoryFactory</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryFactory create(String configFilePath) throws RepositoryException {
	// no factory home dir specified, use cwd
	String factoryHomeDir = System.getProperty("user.dir");
	return create(configFilePath, factoryHomeDir);
    }

    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param configFilePath path to the configuration file
     * @param factoryHomeDir factory home directory
     * @return a new <code>RepositoryFactory</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryFactory create(String configFilePath, String factoryHomeDir) throws RepositoryException {
	try {
	    File config = new File(configFilePath);
	    InputSource is = new InputSource(new FileReader(config));
	    is.setSystemId(config.toURI().toString());
	    return new RepositoryFactory(is, factoryHomeDir);
	} catch (IOException ioe) {
	    String msg = "error while reading config file " + configFilePath;
	    log.error(msg, ioe);
	    throw new RepositoryException(msg, ioe);
	}
    }

    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param is <code>InputSource</code> where the configuration is read from
     * @return a new <code>RepositoryFactory</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryFactory create(InputSource is) throws RepositoryException {
	// no factory home dir specified, use cwd
	String factoryHomeDir = System.getProperty("user.dir");
	return new RepositoryFactory(is, factoryHomeDir);
    }

    /**
     * Creates a new <code>RepositoryFactory</code> instance. The configuration
     * is read from the specified input source.
     *
     * @param is             <code>InputSource</code> where the configuration is read from
     * @param factoryHomeDir factory home directory
     * @return a new <code>RepositoryFactory</code> instance
     * @throws RepositoryException If an error occurs
     */
    public static RepositoryFactory create(InputSource is, String factoryHomeDir) throws RepositoryException {
	return new RepositoryFactory(is, factoryHomeDir);
    }

    /**
     * @param name
     * @return
     * @throws RepositoryException
     */
    public synchronized Repository getRepository(String name) throws RepositoryException {
	if (reps.containsKey(name)) {
	    return (Repository) reps.get(name);
	}
	Element repConfig = getRepositoryConfig(name);

	// read repository store config
	// (we know this element must exist since the XML has been checked
	// against the DTD)
	Element fsConfig = repConfig.getChild(REPOSITORY_STORE_ELEMENT).getChild(FILE_SYSTEM_ELEMENT);
	FileSystem fsRepStore = createFileSystem(fsConfig);

	// read definitions of stable workspaces
	List wspList = repConfig.getChildren(STABLE_WORKSPACE_ELEMENT);
	Iterator iter = wspList.iterator();
	ArrayList list = new ArrayList();
	while (iter.hasNext()) {
	    Element wspConfig = (Element) iter.next();
	    StableWorkspaceDef wspDef = createStableWspDef(wspConfig);
	    list.add(wspDef);
	}
	StableWorkspaceDef[] wspDefs = (StableWorkspaceDef[]) list.toArray(new StableWorkspaceDef[list.size()]);

	Repository rep = new RepositoryImpl(fsRepStore, wspDefs);
	reps.put(name, rep);
	return rep;
    }

    /**
     * @param name
     * @return
     * @throws RepositoryException
     */
    private Element getRepositoryConfig(String name) throws RepositoryException {
	List repList = config.getRootElement().getChildren(REPOSITORY_ELEMENT);
	Element repConfig = null;
	Iterator iter = repList.iterator();
	while (iter.hasNext()) {
	    repConfig = (Element) iter.next();
	    String n = repConfig.getAttributeValue(NAME_ATTRIB);
	    if (n.equals(name)) {
		return repConfig;
	    }
	}
	String msg = "The repository " + name + " was not found in the config file " + configId;
	log.error(msg);
	throw new RepositoryException(msg);
    }

    /**
     * @param stableWspConfig
     * @return
     * @throws RepositoryException
     */
    private StableWorkspaceDef createStableWspDef(Element stableWspConfig) throws RepositoryException {
	String name = stableWspConfig.getAttributeValue(NAME_ATTRIB);
	// we know this element must exist since the XML has been checked
	// against the DTD

	// FQN of class implementing PersistenceManager interface
	String persistenceManagerClassName = stableWspConfig.getChild(PERSISTENCE_MANAGER_ELEMENT).getAttributeValue(CLASS_ATTRIB);

	// read the PersistenceManager properties from the
	// param elements in the config
	HashMap persistenceManagerParams = new HashMap();
	List paramList = stableWspConfig.getChild(PERSISTENCE_MANAGER_ELEMENT).getChildren(PARAM_ELEMENT);
	for (Iterator i = paramList.iterator(); i.hasNext();) {
	    Element param = (Element) i.next();
	    String paramName = param.getAttributeValue(NAME_ATTRIB);
	    String paramValue = param.getAttributeValue(VALUE_ATTRIB);
	    persistenceManagerParams.put(paramName, paramValue);
	}

	// main workspace store (mandatory)
	Element fsConfig = stableWspConfig.getChild(WORKSPACE_STORE_ELEMENT).getChild(FILE_SYSTEM_ELEMENT);
	FileSystem wspStore = createFileSystem(fsConfig);

	// blob store is optional
	FileSystem blobStore = null;
	if (stableWspConfig.getChild(BLOB_STORE_ELEMENT) != null) {
	    fsConfig = stableWspConfig.getChild(BLOB_STORE_ELEMENT).getChild(FILE_SYSTEM_ELEMENT);
	    blobStore = createFileSystem(fsConfig);
	}

	// read config of dynamic workspaces
	List wspList = stableWspConfig.getChildren(DYNAMIC_WORKSPACE_ELEMENT);
	Iterator iter = wspList.iterator();
	ArrayList list = new ArrayList();
	while (iter.hasNext()) {
	    Element wspConfig = (Element) iter.next();
	    DynamicWorkspaceDef wspDef = createDynWspDef(wspConfig, name);
	    list.add(wspDef);
	}
	DynamicWorkspaceDef[] wspDefs = (DynamicWorkspaceDef[]) list.toArray(new DynamicWorkspaceDef[list.size()]);

	return new StableWorkspaceDef(name, wspStore, blobStore,
		persistenceManagerClassName, persistenceManagerParams, wspDefs);
    }

    /**
     * @param dynWspConfig
     * @return
     * @throws RepositoryException
     */
    private DynamicWorkspaceDef createDynWspDef(Element dynWspConfig, String stableWspName) throws RepositoryException {
	String name = dynWspConfig.getAttributeValue(NAME_ATTRIB);
	// we know this element must exist since the XML has been checked
	// against the DTD

	// FQN of class implementing PersistenceManager interface
	String persistenceManagerClassName = dynWspConfig.getChild(PERSISTENCE_MANAGER_ELEMENT).getAttributeValue(CLASS_ATTRIB);

	// read the PersistenceManager properties from the
	// param elements in the config
	HashMap persistenceManagerParams = new HashMap();
	List paramList = dynWspConfig.getChild(PERSISTENCE_MANAGER_ELEMENT).getChildren(PARAM_ELEMENT);
	for (Iterator i = paramList.iterator(); i.hasNext();) {
	    Element param = (Element) i.next();
	    String paramName = param.getAttributeValue(NAME_ATTRIB);
	    String paramValue = param.getAttributeValue(VALUE_ATTRIB);
	    persistenceManagerParams.put(paramName, paramValue);
	}

	// main workspace store (mandatory)
	Element fsConfig = dynWspConfig.getChild(WORKSPACE_STORE_ELEMENT).getChild(FILE_SYSTEM_ELEMENT);
	FileSystem wspStore = createFileSystem(fsConfig);

	// blob store is optional
	FileSystem blobStore = null;
	if (dynWspConfig.getChild(BLOB_STORE_ELEMENT) != null) {
	    fsConfig = dynWspConfig.getChild(BLOB_STORE_ELEMENT).getChild(FILE_SYSTEM_ELEMENT);
	    blobStore = createFileSystem(fsConfig);
	}

	return new DynamicWorkspaceDef(name, wspStore, blobStore,
		persistenceManagerClassName, persistenceManagerParams, stableWspName);
    }

    /**
     * @param fsConfig
     * @return
     * @throws RepositoryException
     */
    private FileSystem createFileSystem(Element fsConfig) throws RepositoryException {
	FileSystem fs;
	String className = "";
	try {
	    // Create the file system object
	    className = fsConfig.getAttributeValue(CLASS_ATTRIB);
	    Class c = Class.forName(className);
	    fs = (FileSystem) c.newInstance();

	    // Set the properties of the file system object from the
	    // param elements in the config
	    BeanMap bm = new BeanMap(fs);
	    List paramList = fsConfig.getChildren(PARAM_ELEMENT);
	    for (Iterator i = paramList.iterator(); i.hasNext();) {
		Element param = (Element) i.next();
		String paramName = param.getAttributeValue(NAME_ATTRIB);
		String paramValue = param.getAttributeValue(VALUE_ATTRIB);
		// @todo FIXME need a cleaner way to specify/configure root of abstract file system
		int pos;
		int lastPos = 0;
		StringBuffer sb = new StringBuffer(paramValue.length());
		while ((pos = paramValue.indexOf(FACTORY_HOME_VARIABLE, lastPos)) != -1) {
		    sb.append(paramValue.substring(lastPos, pos));
		    sb.append(factoryHomeDir);
		    lastPos = pos + FACTORY_HOME_VARIABLE.length();
		}
		if (lastPos < paramValue.length()) {
		    sb.append(paramValue.substring(lastPos));
		}
		bm.put(paramName, sb.toString());
	    }
	    fs.init();
	} catch (Exception e) {
	    String msg = "Cannot instantiate implementing class " + className;
	    log.error(msg, e);
	    throw new RepositoryException(msg, e);
	}
	return fs;
    }

    /**
     * Shuts down the repositories
     */
    public synchronized void shutdown() {
	Iterator iter = reps.keySet().iterator();
	while (iter.hasNext()) {
	    String name = (String) iter.next();
	    RepositoryImpl rep = (RepositoryImpl) reps.get(name);
	    log.info("Shutting down: " + name);
	    rep.shutdown();
	}
	reps.clear();
    }
}