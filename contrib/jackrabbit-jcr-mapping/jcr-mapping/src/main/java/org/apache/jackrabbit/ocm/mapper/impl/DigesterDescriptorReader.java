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
package org.apache.jackrabbit.ocm.mapper.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.digester.Digester;
import org.apache.jackrabbit.ocm.exception.InitMapperException;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.CollectionDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.FieldDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.ImplementDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

/**
 * Helper class that reads the xml mapping file and load all class descriptors into memory (object graph)
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DigesterDescriptorReader implements DescriptorReader
{
    private boolean validating = true;
    private URL dtdResolver;
    
    private Collection configFileStreams = new ArrayList();
    
    public DigesterDescriptorReader(InputStream stream)
    {
        configFileStreams.add(stream);	
    }
    
    public DigesterDescriptorReader(InputStream[] streams)
    {
        for (int i = 0; i < streams.length; i++) 
        {
        	configFileStreams.add(streams[i]);	
		}
    	
    }
    
    public DigesterDescriptorReader(String xmlFile)
    {
    	try
		{
    		configFileStreams.add(new FileInputStream(xmlFile));
		}		
		catch (FileNotFoundException e)
		{
			throw new InitMapperException("Mapping file not found : " + xmlFile,e);
		}
    } 
    
    public DigesterDescriptorReader(String[] xmlFiles)
    {
   	
    	for (int i = 0; i < xmlFiles.length; i++) 
    	{
        	try
    		{
        		configFileStreams.add(new FileInputStream(xmlFiles[i]));
    		}
    		
    		catch (FileNotFoundException e)
    		{
    			throw new InitMapperException("Mapping file not found : " + xmlFiles[i],e);
    		}    				
		}
    } 
    
    
    /**
     * Set if the mapping should be validated.
     * @param flag <tt>true</tt> if the mapping should be validated
     */
    public void setValidating(boolean flag) {
        this.validating= flag;
    }

    public void setResolver(URL dtdResolver) {
        this.dtdResolver = dtdResolver;
    }
    
	/**
	 * Load all class descriptors found in the xml mapping file.
	 * 
	 * @return a {@link MappingDescriptor}
	 * 
	 */
	public MappingDescriptor loadClassDescriptors()
	{
		try
		{
			MappingDescriptor mappingDescriptor = new MappingDescriptor();
			for (Iterator iter = configFileStreams.iterator(); iter.hasNext();) 
			{
				InputStream xmlMappingDescriptorFile = (InputStream) iter.next();
				Digester digester = new Digester();
				digester.setValidating(this.validating);
				if (null != this.dtdResolver) 
				{
	                digester.register("-//The Apache Software Foundation//DTD Repository//EN",
	                                  this.dtdResolver.toString());
	            }
		        digester.setClassLoader(getClass().getClassLoader());
				
		        MappingDescriptor currentMappingDescriptor = new MappingDescriptor();
		        digester.push(currentMappingDescriptor);
				

				// --------------------------------------------------------------------------------
				// Rules used for the class-descriptor element
				// --------------------------------------------------------------------------------	                        
				digester.addObjectCreate("jackrabbit-ocm/class-descriptor", ClassDescriptor.class);
				digester.addSetProperties("jackrabbit-ocm/class-descriptor");
				digester.addSetNext("jackrabbit-ocm/class-descriptor", "addClassDescriptor");		

				// --------------------------------------------------------------------------------
				// Rules used for the implement-descriptor element
				// --------------------------------------------------------------------------------
				digester.addObjectCreate("jackrabbit-ocm/class-descriptor/implement-descriptor", ImplementDescriptor.class);
				digester.addSetProperties("jackrabbit-ocm/class-descriptor/implement-descriptor");
	            digester.addSetNext("jackrabbit-ocm/class-descriptor/implement-descriptor", "addImplementDescriptor");
				
				// --------------------------------------------------------------------------------
				// Rules used for the field-descriptor element
				// --------------------------------------------------------------------------------
				digester.addObjectCreate("jackrabbit-ocm/class-descriptor/field-descriptor", FieldDescriptor.class);
				digester.addSetProperties("jackrabbit-ocm/class-descriptor/field-descriptor");
	            digester.addSetNext("jackrabbit-ocm/class-descriptor/field-descriptor", "addFieldDescriptor");

				// --------------------------------------------------------------------------------
				// Rules used for the bean-descriptor element
				// --------------------------------------------------------------------------------
				digester.addObjectCreate("jackrabbit-ocm/class-descriptor/bean-descriptor", BeanDescriptor.class);
				digester.addSetProperties("jackrabbit-ocm/class-descriptor/bean-descriptor");
	            digester.addSetNext("jackrabbit-ocm/class-descriptor/bean-descriptor", "addBeanDescriptor");

				// --------------------------------------------------------------------------------
				// Rules used for the collection-descriptor element
				// --------------------------------------------------------------------------------
				digester.addObjectCreate("jackrabbit-ocm/class-descriptor/collection-descriptor", CollectionDescriptor.class);
				digester.addSetProperties("jackrabbit-ocm/class-descriptor/collection-descriptor");
	            digester.addSetNext("jackrabbit-ocm/class-descriptor/collection-descriptor", "addCollectionDescriptor");			

	            currentMappingDescriptor = (MappingDescriptor) digester.parse(xmlMappingDescriptorFile);
				
				mappingDescriptor.getClassDescriptorsByClassName().putAll(currentMappingDescriptor.getClassDescriptorsByClassName());
				mappingDescriptor.getClassDescriptorsByNodeType().putAll(currentMappingDescriptor.getClassDescriptorsByNodeType());
                
			}
			return mappingDescriptor;
		}
		catch (Exception e)
		{
			throw new InitMapperException("Impossible to read the xml mapping descriptor file(s)", e);
		}
	}


}
