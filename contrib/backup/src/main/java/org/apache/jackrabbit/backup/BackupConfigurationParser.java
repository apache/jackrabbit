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
package org.apache.jackrabbit.backup;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.ConfigurationParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * BackupConfigurationParser. Used to parse the Backup configuration XML file.
 * Please look at the documentation for the format: http://wiki.apache.org/jackrabbit/BackupTool
 *
 */
public class BackupConfigurationParser extends ConfigurationParser {

    private static final String WORKING_FOLDER = "WorkingFolder";
    private static final String WORKING_FOLDER_ATTRIBUTE = "path";
    private static final String RESOURCES = "Resources";
    private static final String RESOURCE = "Resource";
    private static final String SAVING_CLASS = "savingClass";
    //TODO Add parse to get the name if a specific wsp has to be backupped/restored.
    //TODO Add UUID choice


    /**
     * @param variables
     */
    public BackupConfigurationParser(Properties variables) {
        super(variables);
    }

    /**
     * Parses backup/restore configuration file.
     *
     * Please look at the documentation for the format: http://wiki.apache.org/jackrabbit/BackupTool
     *
     * @param xml repository configuration document
     * @param myFile path and name of the XML configuration file (TODO delete XML argument and build it with myFile)
     * @param repoConfFile: path and name of the repository configuration file 
     * @return repository configuration
     * @throws ConfigurationException if the configuration is broken
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     * @throws IOException
     * @see #parseBeanConfig(Element, String)
     * @see #parseVersioningConfig(Element)
     */
    public BackupConfig parseBackupConfig(InputSource xml, String myFile, String repoConfFile)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {

        Element root = parseXML(xml);

        //Working Folder
        Element workingFolder = getElement(root, WORKING_FOLDER);
        File path = new File(workingFolder.getAttribute(WORKING_FOLDER_ATTRIBUTE));

        //Management of resources tag
        Element resources = this.getElement(root, RESOURCES);
        Collection allResources = this.parseResourcesConfig(resources);

        return new BackupConfig(path, allResources, myFile, repoConfFile);
    }


    /**
     * Returns the named children of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @return named children elements, or <code>null</code> if not found
     */
    private List getElements(Element parent, String name)  {
        NodeList children = parent.getChildNodes();
        Vector selected = new Vector(10, 10);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {

                selected.addElement((Element) child);
            }
        }
        if (selected.size() == 0){
            return null;
        }
        else {
            selected.trimToSize();
            return selected;
        }
    }

    /**
     * For now only support of all workspace backup. I think it is actually simpler to manage on the end-user side.
     * Be careful the objects aren't properly initialized yet. You need to call init (in BackupManager).
     *
     * Pre-condition: there are resource tags in the conf file (otherwise there is a problem in the backup)
     * @root root Element of the XML
     * @throws ConfigurationException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @return Collection of all resources to backup found in the file
     */
    private Collection parseResourcesConfig(Element root) 
        throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException  {

        /*
         * For each resource
         *      get class and instantiate
         */
        Vector objects = new Vector();
        Vector resources = (Vector) this.getElements(root, RESOURCE);
        Iterator it = resources.iterator();

        while (it.hasNext()) {
            //We don't care about the name. It is here only for humans: only the savingClass is important
            //Instantiate it and put it in the collection.
            Element resource = (Element) it.next();
            String savingClass = resource.getAttribute(SAVING_CLASS);

            //Check we are not backupping/restoring a resource already backuped by BackupManager
            if (savingClass.equals("org.apache.jackrabbit.backup.RepositoryBackup")  ||
                        savingClass.equals("org.apache.jackrabbit.backup.BackupConfigBackup")) {
                throw new IllegalAccessException();
            }

            Class c = Class.forName(savingClass);
            objects.addElement( (Backup) c.newInstance());
        }
        return objects;
    }
}
