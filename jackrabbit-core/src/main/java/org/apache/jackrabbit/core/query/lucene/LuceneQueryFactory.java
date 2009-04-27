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
package org.apache.jackrabbit.core.query.lucene;

import javax.jcr.RepositoryException;

import org.apache.lucene.search.Query;
import org.apache.jackrabbit.spi.commons.query.qom.SelectorImpl;
import org.apache.jackrabbit.spi.commons.query.qom.FullTextSearchImpl;
import org.apache.jackrabbit.spi.commons.query.qom.PropertyExistenceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.SourceImpl;
import org.apache.jackrabbit.spi.commons.query.qom.JoinImpl;

/**
 * <code>LuceneQueryFactory</code> implements a factory that creates lucene
 * queries from given QOM elements.
 */
public interface LuceneQueryFactory {

    /**
     * Creates a lucene query for the given QOM selector.
     *
     * @param selector the selector.
     * @return a lucene query for the given selector.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(SelectorImpl selector) throws RepositoryException;

    /**
     * Creates a lucene query for the given QOM full text search.
     *
     * @param constraint the full text search constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(FullTextSearchImpl constraint) throws RepositoryException;

    /**
     * Creates a lucene query for the given QOM property existence constraint.
     *
     * @param constraint the QOM constraint.
     * @return the lucene query for the given constraint.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public Query create(PropertyExistenceImpl constraint) throws RepositoryException;

    /**
     * Creates a multi column query for the given QOM source.
     *
     * @param source the QOM source.
     * @return a multi column query for the given source.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public MultiColumnQuery create(SourceImpl source) throws RepositoryException;

    /**
     * Creates a multi column query for the given QOM join.
     *
     * @param join the QOM join.
     * @return the multi column query for the given join.
     * @throws RepositoryException if an error occurs while creating the query.
     */
    public MultiColumnQuery create(JoinImpl join) throws RepositoryException;
}
