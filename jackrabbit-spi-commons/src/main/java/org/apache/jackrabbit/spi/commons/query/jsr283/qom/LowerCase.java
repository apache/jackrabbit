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
 * Evaluates to the lower-case string value (or values, if multi-valued) of
 * {@link #getOperand operand}.
 * <p/>
 * If {@link #getOperand operand} does not evaluate to a string value, its
 * value is first converted to a string.  The lower-case string value is
 * computed as though the <code>toLowerCase()</code> method of
 * <code>java.lang.String</code> were called.
 * <p/>
 * If {@link #getOperand operand} evaluates to null, the <code>LowerCase</code>
 * operand also evaluates to null.
 *
 * @since JCR 2.0
 */
public interface LowerCase extends DynamicOperand {

    /**
     * Gets the operand whose value is converted to a lower-case string.
     *
     * @return the operand; non-null
     */
    DynamicOperand getOperand();

}
