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
package org.apache.portals.graffito.jcr.mapper.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.digester.Digester;
import org.apache.portals.graffito.jcr.exception.InitMapperException;
import org.apache.portals.graffito.jcr.mapper.model.BeanDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.CollectionDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.FieldDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.ImplementDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.MappingDescriptor;

/**
 * Helper class that reads the xml mapping file and load all class descriptors into memory (object graph)
 * 
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DigesterDescriptorReader
{
    private boolean validating = true;
    private URL dtdResolver;
    
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
	 * @param stream the xml mapping file reference
	 * @return a {@link MappingDescriptor}
	 * 
	 */
	public MappingDescriptor loadClassDescriptors(InputStream stream)
	{
		try
		{
			Digester digester = new Digester();
			digester.setValidating(this.validating);
			if (null != this.dtdResolver) {
                digester.register("-//The Apache Software Foundation//DTD Repository//EN",
                                  this.dtdResolver.toString());
            }
	        digester.setClassLoader(getClass().getClassLoader());
			
	        MappingDescriptor mappingDescriptor = new MappingDescriptor();
	        digester.push(mappingDescriptor);
			
	       // TODO : activater the following line wich cause some bugs when loading the xml stream  
           //digester.addSetProperties("graffito-jcr", package, pa);

			// --------------------------------------------------------------------------------
			// Rules used for the class-descriptor element
			// --------------------------------------------------------------------------------	                        
			digester.addObjectCreate("graffito-jcr/class-descriptor", ClassDescriptor.class);
			digester.addSetProperties("graffito-jcr/class-descriptor");
			digester.addSetNext("graffito-jcr/class-descriptor", "addClassDescriptor");		

			// --------------------------------------------------------------------------------
			// Rules used for the implement-descriptor element
			// --------------------------------------------------------------------------------
			digester.addObjectCreate("graffito-jcr/class-descriptor/implement-descriptor", ImplementDescriptor.class);
			digester.addSetProperties("graffito-jcr/class-descriptor/implement-descriptor");
            digester.addSetNext("graffito-jcr/class-descriptor/implement-descriptor", "addImplementDescriptor");
			
			// --------------------------------------------------------------------------------
			// Rules used for the field-descriptor element
			// --------------------------------------------------------------------------------
			digester.addObjectCreate("graffito-jcr/class-descriptor/field-descriptor", FieldDescriptor.class);
			digester.addSetProperties("graffito-jcr/class-descriptor/field-descriptor");
            digester.addSetNext("graffito-jcr/class-descriptor/field-descriptor", "addFieldDescriptor");

			// --------------------------------------------------------------------------------
			// Rules used for the bean-descriptor element
			// --------------------------------------------------------------------------------
			digester.addObjectCreate("graffito-jcr/class-descriptor/bean-descriptor", BeanDescriptor.class);
			digester.addSetProperties("graffito-jcr/class-descriptor/bean-descriptor");
            digester.addSetNext("graffito-jcr/class-descriptor/bean-descriptor", "addBeanDescriptor");

			// --------------------------------------------------------------------------------
			// Rules used for the collection-descriptor element
			// --------------------------------------------------------------------------------
			digester.addObjectCreate("graffito-jcr/class-descriptor/collection-descriptor", CollectionDescriptor.class);
			digester.addSetProperties("graffito-jcr/class-descriptor/collection-descriptor");
            digester.addSetNext("graffito-jcr/class-descriptor/collection-descriptor", "addCollectionDescriptor");			

            return (MappingDescriptor) digester.parse(stream);
		}
		catch (Exception e)
		{
			throw new InitMapperException("Impossible to read the xml mapping file", e);
		}
	}

	/**
	 * Load all class descriptors found in the xml mapping file.
	 * 
	 * @param xmlFile the xml mapping file reference
	 * @return a {@link MappingDescriptor}
	 * 
	 */	
	public MappingDescriptor loadClassDescriptors(String xmlFile)
	{
		try
		{
			return loadClassDescriptors(new FileInputStream(xmlFile));
		}
		
		catch (FileNotFoundException e)
		{
			throw new InitMapperException("Mapping file not found : " + xmlFile,e);
		}
	}
}
