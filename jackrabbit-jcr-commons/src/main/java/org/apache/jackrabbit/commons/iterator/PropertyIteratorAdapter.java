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
package org.apache.jackrabbit.commons.iterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RangeIterator;

/**
 * Adapter class for turning {@link RangeIterator}s or {@link Iterator}s
 * into {@link PropertyIterator}s.
 */
public class PropertyIteratorAdapter extends RangeIteratorDecorator
        implements PropertyIterator {

    /**
     * Static instance of an empty {@link PropertyIterator}.
     */
    public static final PropertyIterator EMPTY =
        new PropertyIteratorAdapter(RangeIteratorAdapter.EMPTY);

    /**
     * Creates an adapter for the given {@link RangeIterator}.
     *
     * @param iterator iterator of {@link Property} instances
     */
    public PropertyIteratorAdapter(RangeIterator iterator) {
        super(iterator);
    }

    /**
     * Creates an adapter for the given {@link Iterator}.
     *
     * @param iterator iterator of {@link Property} instances
     */
    public PropertyIteratorAdapter(Iterator iterator) {
        super(new RangeIteratorAdapter(iterator));
    }

    public PropertyIteratorAdapter(Iterator iterator, long size) {
        super(new RangeIteratorAdapter(iterator, size));
    }

    /**
     * Creates an iterator for the given collection.
     *
     * @param collection collection of {@link Property} instances
     */
    public PropertyIteratorAdapter(Collection collection) {
        super(new RangeIteratorAdapter(collection));
    }

    //----------------------------------------------------< PropertyIterator >

    /**
     * Returns the next property.
     *
     * @return next property
     * @throws NoSuchElementException if there is no next property
     */
    public Property nextProperty() {
        return (Property) next();
    }

}
