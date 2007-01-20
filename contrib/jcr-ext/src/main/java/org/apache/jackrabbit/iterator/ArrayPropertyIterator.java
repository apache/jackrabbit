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
package org.apache.jackrabbit.iterator;

import java.util.Collection;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.PropertyIterator PropertyIterator} interface.
 */
public class ArrayPropertyIterator extends ArrayIterator
        implements PropertyIterator {

    /**
     * Creates an iterator for the given array of properties.
     *
     * @param properties the properties to iterate
     */
    public ArrayPropertyIterator(Property[] properties) {
        super(properties);
    }

    /**
     * Creates an iterator for the given collection of properties.
     *
     * @param properties the properties to iterate
     */
    public ArrayPropertyIterator(Collection properties) {
        this((Property[]) properties.toArray(new Property[properties.size()]));
    }

    /**
     * Returns the next property in the array.
     *
     * @return next property
     * @see PropertyIterator#nextProperty()
     */
    public Property nextProperty() {
        return (Property) next();
    }

}
