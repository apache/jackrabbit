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
package org.apache.jackrabbit.ocm.manager.collectionconverter;


import java.util.Collection;
import java.util.Map;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableCollectionImpl;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableMapImpl;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Utility class used to instantiate {@link ManageableObjects}
 * A ManageableObjects is a Collection or a Map
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ManageableObjectsUtil {

    /**
     * Instantiate a new {@link ManageableObjects}
     * @param manageableObjectsClassName The manageable objects class name
     * @return an emtpy created {@link ManageableObjects}
     */
    public static ManageableObjects getManageableObjects(String manageableObjectsClassName) {
        try {
            return (ManageableObjects) ReflectionUtils.newInstance(manageableObjectsClassName);
        }
        catch (Exception e) {
            throw new JcrMappingException("Cannot create manageable collection : "
                                           + manageableObjectsClassName,
                                           e);
        }
    }

    /**
     * Instantiate a new {@link ManageableObjects}
     * @param manageableObjectsClass the collection class name
     * @return an emtpy created {@link ManageableCollection}
     */

    public static ManageableObjects getManageableObjects(Class manageableObjectsClass) {
        try {

        	// if the class is an interface, try to find the default class implementation
        	if (manageableObjectsClass.isInterface())
        	{

        		Class defaultImplementation  = ReflectionUtils.getDefaultImplementation(manageableObjectsClass);
        		if (defaultImplementation == null)
        		{
        			new JcrMappingException("No default implementation for the interface " + manageableObjectsClass);
        		}
        		else
        		{
        			manageableObjectsClass = defaultImplementation;
        		}
        	}

        	//if the class is implementing the Collection interface
        	if (ReflectionUtils.implementsInterface(manageableObjectsClass, Collection.class))
        	{
        		return new ManageableCollectionImpl((Collection) ReflectionUtils.newInstance(manageableObjectsClass));
        	}

        	//if the class is implementing the Map interface
        	if (ReflectionUtils.implementsInterface(manageableObjectsClass, Map.class))
        	{
        		return new ManageableMapImpl((Map) ReflectionUtils.newInstance(manageableObjectsClass));
        	}


            Object manageableObjects = manageableObjectsClass.newInstance();
            if (!(manageableObjects instanceof ManageableObjects)) {
                throw new JcrMappingException("Unsupported collection type :"
                                               + manageableObjectsClass.getName());
            }
            else {
                return (ManageableObjects) manageableObjects;
            }
        }
        catch (Exception e) {
            throw new JcrMappingException("Cannot create manageable objects (Collection or Map)", e);
        }
    }

    /**
     * Convert a java Collection or a Map into a {@link ManageableObjects}.
     *
     * The elements of a Map should have an ID field (see the field descriptor definition).
     * @param object the collection or the Map objet
     * @return The converted {@link ManageableObjects}
     *
     */
    public static ManageableObjects getManageableObjects(Object object) {
        try {
            if (object == null) {
                return null;
            }

            if (object instanceof ManageableObjects) {
                return (ManageableObjects) object;

            }

        	//if the class is implementing the Collection interface
        	if (ReflectionUtils.implementsInterface(object.getClass(), Collection.class))
        	{
        		return new ManageableCollectionImpl((Collection) object);
        	}

        	//if the class is implementing the Map interface
        	if (ReflectionUtils.implementsInterface(object.getClass(), Map.class))
        	{
        		return new ManageableMapImpl((Map) object);
        	}


        }
        catch (Exception e) {
            throw new JcrMappingException("Impossible to create the manageable collection", e);
        }

        throw new JcrMappingException("Unsupported collection type :" + object.getClass().getName());
    }
}
