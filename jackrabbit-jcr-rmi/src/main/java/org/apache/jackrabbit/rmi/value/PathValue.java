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
 * The <code>PathValue</code> class implements the committed value state for
 * Path values as a part of the State design pattern (Gof) used by this package.
 *
 * @since 0.16.4.1
 */
public class PathValue extends AbstractValue {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 6233090249008329224L;

    /** The path value. */
    private final String value;

    /**
     * Creates an instance for the given path <code>value</code>.
     */
    protected PathValue(String value) throws ValueFormatException {
        // TODO: Check path format
        this.value = value;
    }

    /**
     * Returns <code>PropertyType.PATH</code>.
     */
    public int getType() {
        return PropertyType.PATH;
    }

    /**
     * Returns the string representation of the path value.
     */
    public String getString() throws ValueFormatException, RepositoryException {
        return value;
    }
}
