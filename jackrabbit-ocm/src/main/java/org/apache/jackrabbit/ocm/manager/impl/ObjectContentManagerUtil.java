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
package org.apache.jackrabbit.ocm.manager.impl;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.exception.RepositoryException;
import org.apache.jackrabbit.ocm.mapper.model.BeanDescriptor;


/**
* Utility class for used in the object content manager and in the converters
*
* @author <a href="mailto:christophe.lombart@gmail.com">Lombart Christophe </a>
*/
public class ObjectContentManagerUtil
{
	
    public static String getPath(Session session, BeanDescriptor beanDescriptor, Node parentNode) throws ObjectContentManagerException
    {		
		 try 
		 {
			String path = "";
			if (parentNode != null)
		    {				
				 path +=  parentNode.getPath();
			}
		    return path + "/"  + beanDescriptor.getJcrName();

		} 
		catch (javax.jcr.RepositoryException e) 
		{
			throw new RepositoryException(e);
		}
	}
}
