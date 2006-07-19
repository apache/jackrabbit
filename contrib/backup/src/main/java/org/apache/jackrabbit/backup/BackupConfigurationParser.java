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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.ConfigurationParser;
import org.apache.jackrabbit.core.config.PersistenceManagerConfig;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @author ntoper
 *
 */
public class BackupConfigurationParser extends ConfigurationParser {

    private static final String WORKING_FOLDER = "WorkingFolder";
    private static final String WORKING_FOLDER_ATTRIBUTE = "path";
    private static final String RESOURCES = "Resources";
    private static final String RESOURCE = "Resource";
    private static final String SAVING_CLASS = "savingClass";
    
    /**
     * @param variables
     */
    public BackupConfigurationParser(Properties variables) {
        super(variables);
    }
    
    
    /*
     * A static method to get the XML conf file and return it as a String. 
     * It is static since it doesn't have to be used with this configuration XML file.
     * 
     * TODO: where is the best place for this method?
     * 
     * @param the InputSource 
     */
    public static String toXmlString(InputSource xml) throws IOException {
        
        String line;
        StringBuffer content = new StringBuffer();
        BufferedReader readBuffer = new BufferedReader(xml.getCharacterStream());
        
        while((line = readBuffer.readLine()) != null){
            content.append(line);
            content.append("\r\n");
        }
        readBuffer.close();
        return content.toString();
    } 
    
   
    /**
     * Parses backup? configuration. Backup configuration uses the
     * following format:
     * <p>
     * TODO comment. See wiki for format
     * @param xml repository configuration document
     * @return repository configuration
     * @throws ConfigurationException if the configuration is broken
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws ClassNotFoundException 
     * @throws SizeException 
     * @throws IOException 
     * @see #parseBeanConfig(Element, String)
     * @see #parseVersioningConfig(Element)
     */
    public BackupConfig parseBackupConfig(InputSource xml)
            throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException, IOException {
     
        Element root = parseXML(xml);
    
        //Working Folder
        Element workingFolder = getElement(root, WORKING_FOLDER);
        File path = new File(workingFolder.getAttribute(WORKING_FOLDER_ATTRIBUTE));
        
        //Persistence Manager
        PersistenceManagerConfig pmc = this.parsePersistenceManagerConfig(root);
        
        //Management of resources tag   
        Element resources = this.getElement(root, RESOURCES);
        Collection allResources = this.parseResourcesConfig(resources);     
          
        return new BackupConfig(pmc, path, allResources);
    }
    

    /**
     * TODO: to put in ConfigurationParser?
     * 
     * Returns the named children of the given parent element.
     *
     * @param parent parent element
     * @param name name of the child element
     * @param required indicates if the child element is required
     * @return named children elements, or <code>null</code> if not found 
     */
    protected List getElements(Element parent, String name)  {
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
        else
        {
            selected.trimToSize();
            return selected;
        }
    }
    
    
    /*
     * For now only support of all workspace backup. I think it is actually simpler to manage on the end-user side.
     * 
     * Pre-condition: there are resource tags in the conf file (otherwise there is a problem in the backup)
     */
    private Collection parseResourcesConfig( Element root) throws ConfigurationException, ClassNotFoundException, InstantiationException, IllegalAccessException, SizeException {

        /*
         * For each resource
         *      get class and instantiate 
         *      addResource to ManagerBackup
         */
        Vector objects = new Vector();
        Vector resources = (Vector) this.getElements(root, RESOURCE);
        Iterator it = resources.iterator();
        
        while (it.hasNext()) {
            //We don't care about the name. It is here only for humans: only the savingClass is important
            //Instantiate it and put it in the collection.
            Element resource = (Element) it.next();
            String savingClass = resource.getAttribute(SAVING_CLASS);   
            Class c = Class.forName(savingClass);
            objects.addElement( (Backup) c.newInstance());
            
            
        }
        return objects;
         
    }
}
