/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.spi.commons.iterator;

/**
 * Type safe counter part of {@code org.apache.commons.collections.Transformer}.
 *
 * @param <A>  argument type to transform from
 * @param <R>  result type to transform to
 */
public interface Transformer<A, R> {

    /**
     * Transforms the input object (leaving it unchanged) into some output object.
     *
     * @param argument  the object to be transformed, should be left unchanged
     * @return a transformed object
     */
    public R transform(A argument);
}
