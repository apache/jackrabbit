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
package org.apache.jackrabbit.iterator;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.PropertyIterator PropertyIterator} interface.
 * This class is used by the JCR-RMI client adapters to convert
 * property arrays to iterators.
 * 
 * @author Jukka Zitting
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
    
    /** {@inheritDoc} */
    public Property nextProperty() {
        return (Property) next();
    }

}
