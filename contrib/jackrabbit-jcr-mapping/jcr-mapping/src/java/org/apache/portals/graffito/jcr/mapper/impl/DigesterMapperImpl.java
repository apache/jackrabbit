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



import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.exception.IncorrectPersistentClassException;
import org.apache.portals.graffito.jcr.exception.InitMapperException;
import org.apache.portals.graffito.jcr.exception.JcrMappingException;
import org.apache.portals.graffito.jcr.mapper.Mapper;
import org.apache.portals.graffito.jcr.mapper.model.ClassDescriptor;
import org.apache.portals.graffito.jcr.mapper.model.MappingDescriptor;

/**
 *
 * Digester implementation for {@link org.apache.portals.graffito.jcr.mapper.Mapper}
 *
 * @author <a href="mailto:christophe.lombart@sword-technologies.com">Lombart Christophe </a>
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class DigesterMapperImpl implements Mapper {
    private static final Log log = LogFactory.getLog(DigesterMapperImpl.class);

    private MappingDescriptor mappingDescriptor;
    private Collection rootClassDescriptors = new ArrayList(); // contains the class descriptor which have not ancestors 

    private String[] mappingFiles;
    private InputStream[] mappingStreams;
    private DigesterDescriptorReader descriptorReader;

    /**
     * No-arg constructor.
     */
    public DigesterMapperImpl() {
    }

    /**
     * Constructor
     *
     * @param xmlFile The xml mapping file to read
     *
     */
    public DigesterMapperImpl(String xmlFile) {
        this.mappingFiles = new String[] { xmlFile };
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param files a set of xml mapping files to read
     *
     */
    public DigesterMapperImpl(String[] files) {
        this.mappingFiles = files;
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param stream The xml mapping file to read
     */
    public DigesterMapperImpl(InputStream stream) {
        this.mappingStreams = new InputStream[] { stream };
        this.buildMapper();
    }

    /**
     * Constructor
     *
     * @param streams a set of mapping files to read
     *
     */
    public DigesterMapperImpl(InputStream[] streams) {
        this.mappingStreams = streams;
        this.buildMapper();
    }

    /**
     * Set a mapping file.
     * 
     * @param file path to mapping file
     */
    public void setMappingFile(String file) {
        setMappingFiles(new String[] { file });
        this.buildMapper();
    }

    /**
     * 
     * @param files
     */
    public void setMappingFiles(String[] files) {
        this.mappingFiles = files;
    }

    public void setMappingStream(InputStream stream) {
        setMappingStreams(new InputStream[] { stream });
    }

    public void setMappingStreams(InputStream[] streams) {
        this.mappingStreams = streams;
    }

    public void setDescriptorReader(DigesterDescriptorReader reader) {
        this.descriptorReader = reader;
    }

    private Mapper buildMapper() {
        if (this.descriptorReader == null) {
            this.descriptorReader = new DigesterDescriptorReader();
        }
        if (this.mappingFiles != null && this.mappingFiles.length > 0) {
            log.info("Read the xml mapping file : " +  this.mappingFiles[0]);
            this.mappingDescriptor = this.descriptorReader.loadClassDescriptors(this.mappingFiles[0]);
            this.mappingDescriptor.setMapper(this);

            for (int i = 1; i < this.mappingFiles.length; i++) {
                log.info("Read the xml mapping file : " +  this.mappingFiles[i]);
                MappingDescriptor anotherMappingDescriptor = this.descriptorReader.loadClassDescriptors(this.mappingFiles[i]);
                this.mappingDescriptor.getClassDescriptorsByClassName().putAll(anotherMappingDescriptor.getClassDescriptorsByClassName());
                this.mappingDescriptor.getClassDescriptorsByNodeType().putAll(anotherMappingDescriptor.getClassDescriptorsByNodeType());
                
            }
        }
        else if (this.mappingStreams != null && this.mappingStreams.length > 0) {
            log.info("Read the stream mapping file : " +  this.mappingStreams[0].toString());
            this.mappingDescriptor = this.descriptorReader.loadClassDescriptors(this.mappingStreams[0]);
            this.mappingDescriptor.setMapper(this);

            for (int i = 1; i < this.mappingStreams.length; i++) {
                log.info("Read the stream mapping file : " +  this.mappingStreams[i].toString());
                MappingDescriptor anotherMappingDescriptor = this.descriptorReader.loadClassDescriptors(this.mappingStreams[i]);
                this.mappingDescriptor.getClassDescriptorsByClassName().putAll(anotherMappingDescriptor.getClassDescriptorsByClassName());
                this.mappingDescriptor.getClassDescriptorsByNodeType().putAll(anotherMappingDescriptor.getClassDescriptorsByNodeType());
            }
        }
        if (null != this.mappingDescriptor) {
            List errors = new ArrayList();
            errors =  solveReferences(errors);            
            errors = validateDescriptors(errors, rootClassDescriptors);

            if (!errors.isEmpty()) {
                throw new InitMapperException("Mapping files contain errors."
                        + getErrorMessage(errors));
            }
        }
        else {
            throw new InitMapperException("No mappings were provided");
        }
        
        return this;
    }

    private List solveReferences(List errors) {
        for(Iterator it = this.mappingDescriptor.getClassDescriptorsByClassName().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            ClassDescriptor cd = (ClassDescriptor) entry.getValue();

            if (null != cd.getExtend() && !"".equals(cd.getExtend())) 
            {
                ClassDescriptor superClassDescriptor = this.mappingDescriptor.getClassDescriptorByName(cd.getExtend());

                if (null == superClassDescriptor) 
                {
                    errors.add("Cannot find mapping for class "
                            + cd.getExtend()
                            + " referenced as extends from "
                            + cd.getClassName());
                }
                else 
                {
            	       log.debug("Class " +cd.getClassName() +  " extends " + cd.getExtend());
                    cd.setSuperClassDescriptor(superClassDescriptor);
                }
            }
            else
            {
                   rootClassDescriptors.add(cd);
            }
            
            Collection interfaces = cd.getImplements();
            if (interfaces.size() > 0) 
            {	
            	      for (Iterator iterator = interfaces.iterator(); iterator.hasNext();)
            	      {
            	    	          String interfaceName= (String) iterator.next();
                          ClassDescriptor interfaceClassDescriptor = this.mappingDescriptor.getClassDescriptorByName(interfaceName);

                          if (null == interfaceClassDescriptor) 
                          {
                              errors.add("Cannot find mapping for interface "
                                      + interfaceName
                                      + " referenced as implements from "
                                      + cd.getClassName());
                          }
                          else 
                          {
                      	       log.debug("Class " +cd.getClassName() +  " implements " + interfaceName);
                              //cd.setSuperClassDescriptor(interfaceClassDescriptor);
                      	      interfaceClassDescriptor.addDescendantClassDescriptor(cd); 
                          }
            	    	      
            	      }
            }
            
        }

        return errors;
    }
    
    /**
     * Validate all class descriptors.
     * This method validates the toplevel ancestors and after the descendants. 
     * Otherwise, we can have invalid settings in the class descriptors
     * @param errors all errors found during the validation process
     * @param classDescriptors the ancestor classdescriptors
     * @return
     */
    private List  validateDescriptors(List errors, Collection classDescriptors ) {
        for(Iterator it = classDescriptors.iterator(); it.hasNext(); ) {
            ClassDescriptor classDescriptor = (ClassDescriptor) it.next();
            try {
                classDescriptor.afterPropertiesSet();
                if (classDescriptor.hasDescendants()) {
                    errors = validateDescriptors(errors, classDescriptor.getDescendantClassDescriptors());
                }
            }
            catch(JcrMappingException jme) {
                log.warn("Mapping of class " + classDescriptor.getClassName() + " is invalid", jme);
                errors.add(jme.getMessage());
            }
        }
        return errors;
    }    
    
    private String getErrorMessage(List errors) {
        final String lineSep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        for(Iterator it = errors.iterator(); it.hasNext();) {
            buf.append(lineSep).append(it.next());
        }

        return buf.toString();
    }    
    
    /**
    *
    * @see org.apache.portals.graffito.jcr.mapper.Mapper#getClassDescriptorByClass(java.lang.Class)
    */
   public ClassDescriptor getClassDescriptorByClass(Class clazz) {
	   ClassDescriptor descriptor = mappingDescriptor.getClassDescriptorByName(clazz.getName());
	   if (descriptor==null) {
			throw new IncorrectPersistentClassException("Class of type: " + clazz.getName() + " has no descriptor.");
	   }
       return descriptor ; 
   }
   
   /**
   * @see org.apache.portals.graffito.jcr.mapper.Mapper#getClassDescriptorByNodeType(String)
   */
  public ClassDescriptor getClassDescriptorByNodeType(String jcrNodeType) {
	  ClassDescriptor descriptor = mappingDescriptor.getClassDescriptorByNodeType(jcrNodeType);
	   if (descriptor==null) {
			throw new IncorrectPersistentClassException("Node type: " + jcrNodeType + " has no descriptor.");
	   }
      return descriptor ;      
  }
   
}
