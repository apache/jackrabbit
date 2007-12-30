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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows for the annotation of a Java class so mapping through OCM to a JCR node
 * 
 * @author Philip Dodds
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Node {
    
	String jcrType() default "nt:unstructured";
	String jcrSuperTypes() default "";
	String jcrMixinTypes() default "";
    // Define the extend setting in the mapping descriptor - Provide less flexibility if we use the java instrospection	
	Class extend() default Object.class;

	// Define the abstract setting in the mapping descriptor - Provide less flexibility if we use the java instrospection
	boolean isAbstract() default false; 
	
//	 Define the inteface setting in the mapping descriptor - Provide less flexibility if we use the java instrospection
	boolean isInterface() default false;
	
	// Discriminator is used when an object hierarchy tree is mapped into the same jcr node type
	// TODO : try to drop it from the mapping strategy. it should be hidden in the persistence manager impl. 
	boolean discriminator() default true;
	
	
}
