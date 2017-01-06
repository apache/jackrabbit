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
package org.apache.jackrabbit.rmi.iterator;

import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 * Array implementation of the JCR
 * {@link javax.jcr.observation.EventListenerIterator EventListenerIterator} interface.
 * This class is used by the JCR-RMI client adapters to convert
 * listener arrays to iterators.
 */
public class ArrayEventListenerIterator extends ArrayIterator
        implements EventListenerIterator {

    /**
     * Creates an iterator for the given array of listeners.
     *
     * @param listeners the listeners to iterate
     */
    public ArrayEventListenerIterator(EventListener[] listeners) {
        super(listeners);
    }

    /** {@inheritDoc} */
    public EventListener nextEventListener() {
        return (EventListener) next();
    }

}
