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
package org.apache.jackrabbit.sanitycheck;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.commons.collections.BeanMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.config.VersioningConfig;
import org.apache.jackrabbit.core.config.WorkspaceConfig;
import org.apache.jackrabbit.core.state.PMContext;
import org.apache.jackrabbit.core.state.PersistenceManager;
import org.apache.jackrabbit.sanitycheck.check.SanityCheckContext;
import org.apache.jackrabbit.sanitycheck.fix.FixContext;
import org.apache.jackrabbit.sanitycheck.inconsistency.NodeInconsistency;

/**
 * Helper class
 */
public class SanityCheckHelper
{
    /** Logger */
    private static Log log = LogFactory.getLog(SanityCheckHelper.class);

    /** fix catalog */
    private static final String FIX_CATALOG_NAME = "fixes";

    /** versioning catalog */
    private static final String CHAIN_CONFIG = "sanity-check-chain.xml";

    /** versioning catalog */
    private static final String VERSIONING_CHAIN = "versioning";

    /** workspace catalog */
    private static final String WORKSPACE_CHAIN = "workspace";

    /** root uuid */
    private static final String WORKSPACE_ROOT_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

    /** versioning uuid */
    private static final String VERSIONING_ROOT_UUID = "deadbeef-face-babe-cafe-babecafebabe";

    /** Versioning pm name */
    private static final String VERSIONING_PM_NAME = "versioning";

    /** Workspace pm name */
    private static final String WORKSPACE_PM_NAME = "workspace";

    /** Catalog name */
    private static final String CATALOG_NAME = "sanitycheck";

    /**
     * Repairs the given Inconsistency. It gets the command that matchs the
     * class name of the Inconsistency implementor.
     * 
     * @param inconsistency
     *            to repair
     * @throws SanityCheckException
     */
    public static void repair(NodeInconsistency inconsistency)
            throws SanityCheckException
    {
        FixContext ctx = new FixContext();
        ctx.setInconsistency(inconsistency);
        Catalog catalog = CatalogFactoryBase.getInstance().getCatalog(
            FIX_CATALOG_NAME);
        try
        {
            Command fix = catalog.getCommand(inconsistency.getClass().getName());
            log.info("Trying to repair inconsistency "
                    + inconsistency.getDescription() + " with "
                    + fix.getClass().getName());
            fix.execute(ctx);
        } catch (Exception e)
        {
            throw new SanityCheckException("Unable to fix "
                    + inconsistency.getDescription(), e);
        }
    }

    /**
     * Gets the inconsistencies for the given repository
     * 
     * @param config
     * @return
     * @throws SanityCheckException
     */
    public static Collection getInconsistencies(File configFile, String homePath)
            throws SanityCheckException
    {
        try
        {
            // inconsistencies Collection
            Collection inconsistencies = new ArrayList();

            // parse the config
            URL catalogURL = SanityCheckHelper.class.getClassLoader()
                                                    .getResource(CHAIN_CONFIG);
            ConfigParser parser = new ConfigParser();
            parser.parse(catalogURL);
            Catalog catalog = CatalogFactoryBase.getInstance().getCatalog(
                CATALOG_NAME);

            // Create a repositoryConfig instance
            RepositoryConfig rConfig = getRepositoryConfig(configFile, homePath);

            // versioning PersistenceManager
            PersistenceManager vPm = getPersistenceManager(
                rConfig.getVersioningConfig(),
                rConfig);

            // workspaces Collection
            Collection workspacesPm = new ArrayList();

            // Run the chain agains the workspaces PM
            Iterator iter = rConfig.getWorkspaceConfigs().iterator();
            while (iter.hasNext())
            {
                // Config
                WorkspaceConfig wConfig = (WorkspaceConfig) iter.next();
                // PersistenceManager
                PersistenceManager wPm = getPersistenceManager(wConfig, rConfig);
                // Log inconsistencies
                inconsistencies.addAll(getInconsistencies(
                    wPm,
                    wConfig.getName(),
                    vPm));
                workspacesPm.add(wPm);
            }

            // Log versioning inconsistencies
            // inconsistencies.addAll(getInconsistencies(vPm, workspacesPm));

            // return inconsistencies
            return inconsistencies;
        } catch (Exception e)
        {
            throw new SanityCheckException("Unable to get inconsistencies.", e);
        }
    }

    /**
     * @param workspace
     * @param versioning
     * @return the inconsistencies for the given workspace PersistenceManage
     */
    public static Collection getInconsistencies(
        PersistenceManager workspace,
        String name,
        PersistenceManager versioning) throws SanityCheckException
    {
        try
        {
            // Create sanity check context
            SanityCheckContext wCtx = new SanityCheckContext();

            // Set root node
            wCtx.setRootUUID(WORKSPACE_ROOT_UUID);

            // Add workspaces's PersistenceManagers
            wCtx.setPersistenceManager(workspace);
            wCtx.setPersistenceManagerName(WORKSPACE_PM_NAME + " " + name);

            // Set versioning PM
            wCtx.setVersioningPM(versioning);

            // Run chain
            Catalog catalog = CatalogFactoryBase.getInstance().getCatalog(
                CATALOG_NAME);
            catalog.getCommand(WORKSPACE_CHAIN).execute(wCtx);

            // return inconsistencies
            return wCtx.getInconsistencies();
        } catch (Exception e)
        {
            throw new SanityCheckException("Unable to get inconsistencies.", e);
        }
    }

    /**
     * @param workspace
     * @param versioning
     * @return the inconsistencies for the given versioning PersistenceManager
     * @throws SanityCheckException
     */
    public static Collection getInconsistencies(
        PersistenceManager versioning,
        Collection workspaces) throws SanityCheckException
    {
        try
        {
            // Create context
            SanityCheckContext vCtx = new SanityCheckContext();
            vCtx.setRootUUID(VERSIONING_ROOT_UUID);
            vCtx.setPersistenceManager(versioning);
            vCtx.setPersistenceManagerName(VERSIONING_PM_NAME);
            // Run chain
            Catalog catalog = CatalogFactoryBase.getInstance().getCatalog(
                CATALOG_NAME);
            catalog.getCommand(VERSIONING_CHAIN).execute(vCtx);
            return vCtx.getInconsistencies();
        } catch (Exception e)
        {
            throw new SanityCheckException(
                "Unable to get inconsistencies from the versioning PersistenceManager.",
                e);
        }
    }

    /**
     * @param configFile
     * @param homePath
     * @return the RepositoryConfig
     * @throws SanityCheckException
     */
    private static RepositoryConfig getRepositoryConfig(
        File configFile,
        String homePath) throws SanityCheckException
    {
        RepositoryConfig config = null;
        try
        {
            config = RepositoryConfig.create(
                configFile.getAbsolutePath(),
                homePath);
        } catch (ConfigurationException e)
        {
            throw new SanityCheckException(e);
        }
        return config;
    }

    /**
     * @return the Workspace Persistence Manager
     * @throws SanityCheckException
     */
    private static PersistenceManager getPersistenceManager(
        WorkspaceConfig wConfig,
        RepositoryConfig rConfig) throws SanityCheckException
    {
        try
        {
            // FIXME: create instances of NodeTyperegistry and NameSpaceRegistry
            PersistenceManager pm = (PersistenceManager) Class.forName(
                                                                  wConfig.getPersistenceManagerConfig()
                                                                         .getClassName())
                                                              .newInstance();
            PMContext ctx = new PMContext(
                new File(wConfig.getHomeDir()),
                wConfig.getFileSystem(),
                WORKSPACE_ROOT_UUID,
                null,
                null);
            addParameters(pm, wConfig.getPersistenceManagerConfig()
                                     .getParameters());
            pm.init(ctx);
            return pm;
        } catch (Exception e)
        {
            throw new SanityCheckException(e);
        }
    }

    /**
     * @return the Versioning Persistence Manager
     * @throws SanityCheckException
     */
    private static PersistenceManager getPersistenceManager(
        VersioningConfig vConfig,
        RepositoryConfig rConfig) throws SanityCheckException
    {
        try
        {
            // FIXME: create instances of NodeTyperegistry and NameSpaceRegistry
            PersistenceManager pm = (PersistenceManager) Class.forName(
                                                                  vConfig.getPersistenceManagerConfig()
                                                                         .getClassName())
                                                              .newInstance();
            PMContext ctx = new PMContext(
                vConfig.getHomeDir(),
                vConfig.getFileSystem(),
                VERSIONING_ROOT_UUID,
                null,
                null);
            addParameters(pm, vConfig.getPersistenceManagerConfig()
                                     .getParameters());
            pm.init(ctx);
            return pm;
        } catch (Exception e)
        {
            throw new SanityCheckException(e);
        }
    }

    /**
     * Add config parameters to the given PersistenceManager
     * 
     * @param pm
     * @param params
     */
    private static void addParameters(PersistenceManager pm, Properties params)
    {
        BeanMap bm = new BeanMap(pm);
        Iterator iter = params.keySet().iterator();
        while (iter.hasNext())
        {
            Object paramName = iter.next();
            Object paramValue = params.get(paramName);
            bm.put(paramName, paramValue);
        }
    }

}
