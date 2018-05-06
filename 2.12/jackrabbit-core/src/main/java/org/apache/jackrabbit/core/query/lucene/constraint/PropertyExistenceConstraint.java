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
package org.apache.jackrabbit.core.query.lucene.constraint;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.core.query.lucene.LuceneQueryFactory;

/**
 * <code>PropertyExistenceConstraint</code> implements a property existence
 * constraint.
 */
public class PropertyExistenceConstraint extends QueryConstraint {

    /**
     * Creates a new property existence constraint.
     *
     * @param prop     the QOM property existence constraint.
     * @param selector the selector for this constraint.
     * @param factory  the lucene query factory.
     * @throws RepositoryException if an error occurs while creating a lucene
     *                             query for this constraint.
     */
    public PropertyExistenceConstraint(PropertyExistenceImpl prop,
                                       SelectorImpl selector,
                                       LuceneQueryFactory factory)
            throws RepositoryException {
        super(factory.create(prop), selector, factory);
    }
}
