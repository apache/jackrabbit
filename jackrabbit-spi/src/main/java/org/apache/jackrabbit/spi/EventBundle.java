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
package org.apache.jackrabbit.spi;

import java.util.Iterator;

/**
 * An <code>EventBundle</code> is similar to the
 * {@link javax.jcr.observation.EventIterator} interface. Other than the
 * <code>EventIterator</code> an <code>EventBundle</code> allows to retrieve
 * the events multiple times using the {@link #getEvents} method.
 */
public interface EventBundle extends Iterable<Event> {

    /**
     * Returns the events of this bundle.
     *
     * @return the {@link Event events} of this bundle.
     */
    public Iterator<Event> getEvents();

    /**
     * Returns <code>true</code> if this event bundle is associated with a
     * change that was initiated by a local session info. Event bundles for
     * external changes will aways return <code>false</code>.
     *
     * @return <code>true</code> if this event bundle is associated with a local
     *         change, <code>false</code> if this event bundle contains external
     *         changes.
     */
    public boolean isLocal();
}
