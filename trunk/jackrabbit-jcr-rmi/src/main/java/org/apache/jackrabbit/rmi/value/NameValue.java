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
package org.apache.jackrabbit.rmi.value;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

/**
 * The <code>NameValue</code> class implements the committed value state for
 * Name values as a part of the State design pattern (Gof) used by this package.
 *
 * @since 0.16.4.1
 */
public class NameValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4598175360244278453L;

    /** The name value. */
    private final String value;

    /**
     * Creates an instance for the given name <code>value</code>.
     */
    protected NameValue(String value) throws ValueFormatException {
        // TODO: Check name format
        this.value = value;
    }

    /**
     * Returns <code>PropertyType.NAME</code>.
     */
    public int getType() {
        return PropertyType.NAME;
    }

    /**
     * Returns the string representation of the Name value.
     */
    public String getString() throws RepositoryException {
        return value;
    }
}
