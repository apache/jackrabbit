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
package org.apache.jackrabbit.taglib.utils;

/**
 * JCR Taglib Constants
 * 
 * @author <a href="mailto:edgarpoce@gmail.com">Edgar Poce </a>
 */
public interface JCRTagConstants
{
    public static String REPOSITORY_JNDI_PROPERTIES = "jcr/jndi/properties";
    
    public static String REPOSITORY_JNDI_ADDRESS = "jcr/jndi/address";

    public static String JNDI_BEAN_FACTORY = "jcr/beanFactory";

    
    public static String JNDI_ANON_USER = "jcr/login/anonuser";

    public static String JNDI_ANON_PWD = "jcr/login/anonpwd";

    // Bean factory Defaults 
    public static String JNDI_DEFAULT_TEMPLATE_ENGINE = "jcr/template/engine/default";
    
    public static String JNDI_DEFAULT_TRAVERSER = "jcr/traverser/default";

    public static String JNDI_DEFAULT_ITEM_FILTER = "jcr/filter/default";
    
    public static String JNDI_DEFAULT_ITEM_COMPARATOR = "jcr/comparator/default";

    public static String JNDI_DEFAULT_SIZE_CALCULATOR = "jcr/size/default";
    
    public static String JNDI_DEFAULT_SIZE_FORMAT = "jcr/size/format/default";
    
    // Default keys
    public static String KEY_SESSION = "jcrsession";
    
    public static String KEY_CD = "jcrcd";    
    
}