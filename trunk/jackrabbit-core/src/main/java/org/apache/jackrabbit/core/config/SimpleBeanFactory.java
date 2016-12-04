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
package org.apache.jackrabbit.core.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleBeanFactory implements BeanFactory {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Object newInstance(Class<?> klass, BeanConfig config) throws ConfigurationException{
        String cname = config.getClassName();
        try {
            Class<?> objectClass = Class.forName(cname, true, config.getClassLoader());
            if (!klass.isAssignableFrom(objectClass)) {
                throw new ConfigurationException(
                        "Configured class " + cname
                                + " does not implement " + klass.getName()
                                + ". Please fix the repository configuration.");
            }
            if (objectClass.getAnnotation(Deprecated.class) != null) {
                log.warn("{} has been deprecated", cname);
            }

            // Instantiate the object using the default constructor
            return objectClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + cname
                            + " was not found.", e);
        } catch (InstantiationException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + cname
                            + " can not be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException(
                    "Configured bean implementation class " + cname
                            + " is protected.", e);
        }
    }
}
