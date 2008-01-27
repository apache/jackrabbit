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



import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.exception.InitMapperException;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.mapper.DescriptorReader;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.apache.jackrabbit.ocm.mapper.model.ClassDescriptor;
import org.apache.jackrabbit.ocm.mapper.model.MappingDescriptor;

/**
 *
 * Abstract class for {@link org.apache.jackrabbit.ocm.mapper.Mapper}
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
 *
 * TODO : Add more reference tests. For exemple, the mapper has to check if the class used for the elements
 *        of a collectiondescriptor exists. For performance reasone, we can defined some optional validations.
 */
public abstract class AbstractMapperImpl implements Mapper {
    protected static final Log log = LogFactory.getLog(AbstractMapperImpl.class);

    protected DescriptorReader descriptorReader;
    protected MappingDescriptor mappingDescriptor;
    protected Collection rootClassDescriptors = new ArrayList(); // contains the class descriptor which have not ancestors

    public void buildMapper()
    {
    	mappingDescriptor = descriptorReader.loadClassDescriptors();
    	mappingDescriptor.setMapper(this);
    	
        if (null != this.mappingDescriptor)
        {
            List errors = new ArrayList();
            errors =  solveReferences(errors);
            errors = validateDescriptors(errors, rootClassDescriptors);

            if (!errors.isEmpty())
            {
                throw new InitMapperException("Mapping descriptors contain errors."
                        + getErrorMessage(errors));
            }
        }
        else
        {
            throw new InitMapperException("No mappings were provided");
        }

    }


    protected List solveReferences(List errors) {
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
    protected List  validateDescriptors(List errors, Collection classDescriptors ) {
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

    protected String getErrorMessage(List errors) {
        final String lineSep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        for(Iterator it = errors.iterator(); it.hasNext();) {
            buf.append(lineSep).append(it.next());
        }

        return buf.toString();
    }

    /**
    *
    * @see org.apache.jackrabbit.ocm.mapper.Mapper#getClassDescriptorByClass(java.lang.Class)
    */
   public ClassDescriptor getClassDescriptorByClass(Class clazz) {
	   ClassDescriptor descriptor = mappingDescriptor.getClassDescriptorByName(clazz.getName());
	   if (descriptor==null) {
			throw new IncorrectPersistentClassException("Class of type: " + clazz.getName() + " has no descriptor.");
	   }
       return descriptor ;
   }

   /**
   * @see org.apache.jackrabbit.ocm.mapper.Mapper#getClassDescriptorByNodeType(String)
   */
  public ClassDescriptor getClassDescriptorByNodeType(String jcrNodeType) {
	  ClassDescriptor descriptor = mappingDescriptor.getClassDescriptorByNodeType(jcrNodeType);
	   if (descriptor==null) {
			throw new IncorrectPersistentClassException("Node type: " + jcrNodeType + " has no descriptor.");
	   }
      return descriptor ;
  }
}
