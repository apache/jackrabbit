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
package org.apache.jackrabbit.spi.commons.query.jsr283.qom;

/**
 * Evaluates to a <code>NAME</code> value equal to the namespace-qualified
 * name of a node.
 * <p/>
 * The query is invalid if {@link #getSelectorName selector} is not the name
 * of a selector in the query.
 *
 * @since JCR 2.0
 */
public interface NodeName extends DynamicOperand {

    /**
     * Gets the name of the selector against which to evaluate this operand.
     *
     * @return the selector name; non-null
     */
    String getSelectorName();

}
