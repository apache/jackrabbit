/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the transaction on behalf of the component that wants to
 * explictely demarcate transcation boundaries.
 */
public class TransactionContext {

    /**
     * Transaction attributes
     */
    private final Map attributes = new HashMap();

    /**
     * Set an attribute on this transaction. If the value specified is
     * <code>null</code>, it is semantically equivalent to
     * {@link #removeAttribute}.
     *
     * @param name  attribute name
     * @param value attribute value
     */
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
        }
        attributes.put(name, value);
    }

    /**
     * Return an attribute value on this transaction.
     *
     * @param name attribute name
     * @return attribute value, <code>null</code> if no attribute with that
     *         name exists
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    /**
     * Remove an attribute on this transaction.
     *
     * @param name attribute name
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }
}
