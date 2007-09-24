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
package org.apache.jackrabbit.ocm.mapper.impl.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.apache.jackrabbit.ocm.manager.collectionconverter.impl.DefaultCollectionConverterImpl;

/**
 * Allows the annotation of getting methods to show that they reflect a child node in JCR
 * 
 * @author Philip Dodds
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Collection {

	Class converter() default DefaultCollectionConverterImpl.class;

    String jcrName() default ""; 
    
    boolean sameNameSiblings() default false;

    boolean autoCreate() default false;

    boolean protect() default false;

    String onParentVersion() default "COPY";

    boolean mandatory() default false;

    boolean autoInsert() default true;

    boolean autoRetrieve() default true;

    boolean autoUpdate() default true;

    Class type();    
    
    boolean proxy() default false;
    
    

}
