/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.decorator;

import javax.jcr.Session;

/**
 * Provides fields to common objects used by any decorator:
 * <ul>
 * <li><code>DecoratorFactory</code>: the decorator factory in use</li>
 * <li><code>Session</code>: the decorated session which was used to create
 * this decorator</li>
 * </ul>
 */
public abstract class AbstractDecorator {

    /**
     * The decorator factory. Used to decorate returned objects.
     */
    protected final DecoratorFactory factory;

    /**
     * The decorated session to which the returned objects belong.
     */
    protected final Session session;

    /**
     * Constructs an abstract decorator.
     *
     * @param factory decorator factory
     * @param session decorated session
     */
    public AbstractDecorator(DecoratorFactory factory, Session session) {
        this.factory = factory;
        this.session = session;
    }
}
