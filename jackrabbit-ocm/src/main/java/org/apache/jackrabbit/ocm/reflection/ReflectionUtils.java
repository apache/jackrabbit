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
package org.apache.jackrabbit.ocm.reflection;

import java.lang.reflect.InvocationTargetException;

import net.sf.cglib.proxy.Enhancer;

import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.jackrabbit.ocm.exception.JcrMappingException;


/**
 * Utility class for handling reflection using BeanUtils.
 *
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
abstract public class ReflectionUtils {

    // default the class loader to the load of this class
    private static ClassLoader classLoader = ReflectionUtils.class.getClassLoader();

    /**
     * Sets the class loader to use in the {@link #forName(String)} method to
     * load classes.
     * <p>
     * Care must be taken when using this method as when setting an improperly
     * set up classloader, the mapper will not work again throwing tons of
     * exceptions.
     *
     * @param newClassLoader The new class loader to use. This may be
     *      <code>null</code> in which case the system class loader will be used.
     */
    public static void setClassLoader(ClassLoader newClassLoader) {
        classLoader = newClassLoader;
    }

    /**
     * Returns the class loader which is used by the {@link #forName(String)}
     * method to load classes.
     *
     * @return The class loader used by {@link #forName(String)} or
     *      <code>null</code> if the system class loader is used.
     */
    public static ClassLoader getClassLoader() {
        return classLoader;
    }

    public static Object getNestedProperty(Object object, String fieldName) {
        if (null == object) {
            return null;
        }

        try {
            return PropertyUtils.getNestedProperty(object, fieldName);
        }
        catch(IllegalAccessException e) {
            throw new JcrMappingException("Cannot access property "
                    + fieldName,
                    e);
        }
        catch(InvocationTargetException e) {
            throw new JcrMappingException("Cannot access property "
                    + fieldName,
                    e);
        }
        catch(NoSuchMethodException e) {
            throw new JcrMappingException("Cannot access property "
                    + fieldName,
                    e);
        }
    }

    public static Class getPropertyType(Object object, String fieldName) {
        try {
            return PropertyUtils.getPropertyType(object, fieldName);
        }
        catch(Exception ex) {
            throw new JcrMappingException("Cannot access property "
                    + fieldName,
                    ex);
        }
    }

    public static Object newInstance(Class clazz) {
        try {
            return clazz.newInstance();
        }
        catch(Exception ex) {
            throw new JcrMappingException("Cannot create instance for class "
                    + clazz,
                    ex);
        }
    }

    /**
     * @param className
     * @param objects
     * @return
     */
    public static Object  invokeConstructor(String className,  Object[] params) {
        try {
            Class converterClass= forName(className);

            return  ConstructorUtils.invokeConstructor(converterClass, params);
        }
        catch(Exception ex) {
            throw new JcrMappingException("Cannot create instance for class "  + className,  ex);
        }
    }

    /**
     * @param object
     * @param fieldName
     * @param path
     */
    public static void setNestedProperty(Object object, String fieldName, Object value) {
        try {
            PropertyUtils.setNestedProperty(object, fieldName, value);
        }
        catch(Exception ex) {
            String className = (object == null) ? "<null>" : object.getClass().getName();
            throw new JcrMappingException("Cannot set the field " + fieldName + " in the class : " + className,
                    ex);
        }
    }

    /**
     * @param string
     * @return
     */
    public static Object newInstance(String clazz) {
        try {
            return forName(clazz).newInstance();
        }
        catch(Exception ex) {
            throw new JcrMappingException("Cannot create instance for class "  + clazz, ex);
        }
    }

    /**
     * @param elementClassName
     * @return
     */
    public static Class forName(String clazz) {
        try {
            return Class.forName(clazz, true, getClassLoader());
        }
        catch(Exception ex) {
            throw new JcrMappingException("Cannot load class " + clazz, ex);
        }
    }

    public static boolean isProxy(Class beanClass)
    {    	        	
         return Enhancer.isEnhanced(beanClass);	
    }

    public static Class getBeanClass(Object bean)
    {
    	     Class beanClass = bean.getClass();
         if (isProxy(beanClass))
         {
        	     //CGLIB specific
        	 	return beanClass.getSuperclass();
         }
         return beanClass;
    }

}
