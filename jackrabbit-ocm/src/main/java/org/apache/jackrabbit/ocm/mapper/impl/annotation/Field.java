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
 * Allows the annotation of a getter method to show that it will be mapped to a JCR property
 *
 * @author Philip Dodds
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD })
public @interface Field {

    String jcrName() default "";

    boolean id() default false;

    boolean path() default false;

    boolean uuid() default false;

    // Use Object.class as default value
    // because it is not possible to have a default null value in annotation field
    Class converter() default Object.class;

    String jcrDefaultValue() default "";

    String jcrValueConstraints() default "";

    String jcrType() default "";

    boolean jcrAutoCreated() default false;

    boolean jcrMandatory() default false;

    String jcrOnParentVersion() default "COPY";

    boolean jcrProtected() default false;

    boolean jcrMultiple () default false;

}
