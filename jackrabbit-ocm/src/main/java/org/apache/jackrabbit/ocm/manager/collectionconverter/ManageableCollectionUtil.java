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


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.apache.jackrabbit.ocm.exception.JcrMappingException;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableArrayList;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableSet;
import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.ManageableVector;
import org.apache.jackrabbit.ocm.reflection.ReflectionUtils;

/**
 * Utility class used to instantiate {@link ManageableCollection}
 *
 * @author <a href="mailto:christophe.lombart@gmail.com">Christophe Lombart</a>
 *
 */
public class ManageableCollectionUtil {

    /**
     * Instantiate a new {@link ManageableCollection}
     * @param manageableCollectionClassName The manageable collection class name
     * @return an emtpy created {@link ManageableCollection}
     */
    public static ManageableCollection getManageableCollection(String manageableCollectionClassName) {
        try {
            return (ManageableCollection) ReflectionUtils.newInstance(manageableCollectionClassName);
        }
        catch (Exception e) {
            throw new JcrMappingException("Cannot create manageable collection : "
                                           + manageableCollectionClassName,
                                           e);
        }
    }

    /**
     * Instantiate a new {@link ManageableCollection}
     * @param collectionClass the collection class name
     * @return an emtpy created {@link ManageableCollection}
     */

    public static ManageableCollection getManageableCollection(Class collectionClass) {
        try {

            if (collectionClass.equals(ArrayList.class)) {
                return new ManageableArrayList();
            }

            if (collectionClass.equals(Vector.class)) {
                return new ManageableVector();
            }

            if (collectionClass.equals(HashSet.class)) {
                return new ManageableSet();
            }
            
            if (collectionClass.equals(Collection.class) || collectionClass.equals(List.class)) {
                return new ManageableArrayList();
            }

            if (collectionClass.equals(Set.class)) {
                return new ManageableSet();
            }
            
            Object collection = collectionClass.newInstance();
            if (!(collection instanceof ManageableCollection)) {
                throw new JcrMappingException("Unsupported collection type :"
                                               + collectionClass.getName());
            }
            else {
                return (ManageableCollection) collection;
            }
        }
        catch (Exception e) {
            throw new JcrMappingException("Cannot create manageable collection", e);
        }
    }

    /**
     * Convert a java Collection object into a {@link ManageableCollection}.
     * Until now, only the following class are supported :
     * Collection, List, ArrayList, Vector
     *
     * If you need a Map, you have to write your own {@link ManageableCollection}.
     * @param object the java collection or Map
     * @return The converted {@link ManageableCollection}
     *
     */
    public static ManageableCollection getManageableCollection(Object object) {
        try {
            if (object == null) {
                return null;
            }

            if (object instanceof ManageableCollection) {
                return (ManageableCollection) object;

            }
            if (object.getClass().equals(ArrayList.class)) {
                ManageableArrayList manageableArrayList = new ManageableArrayList();
                manageableArrayList.addAll((Collection) object);

                return manageableArrayList;
            }

            if (object.getClass().equals(Vector.class)) {
                ManageableVector manageableVector = new ManageableVector();
                manageableVector.addAll((Collection) object);

                return manageableVector;
            }

            if (object.getClass().equals(HashSet.class)) {
                return new ManageableSet((Set) object);
            }
            
            if (object.getClass().equals(Collection.class)
                || object.getClass().equals(List.class)) {
                ManageableArrayList manageableArrayList = new ManageableArrayList();
                manageableArrayList.addAll((Collection) object);

                return manageableArrayList;
            }
            if (object.getClass().equals(Set.class)) {
                return new ManageableSet((Set) object);
            }
        }
        catch (Exception e) {
            throw new JcrMappingException("Impossible to create the manageable collection", e);
        }
        
        throw new JcrMappingException("Unsupported collection type :" + object.getClass().getName());
    }
}
