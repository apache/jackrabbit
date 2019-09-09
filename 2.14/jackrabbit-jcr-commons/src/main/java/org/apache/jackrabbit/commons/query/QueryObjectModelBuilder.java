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
package org.apache.jackrabbit.commons.query;

import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.ValueFactory;
import javax.jcr.RepositoryException;

/**
 * <code>QueryObjectModelBuilder</code> defines an interface for building a
 * query object model from a string based query statement and vice versa.
 */
public interface QueryObjectModelBuilder {

    /**
     * Creates a new query object model from the given <code>statement</code>
     * using the passed QOM and value factory.
     *
     * @param statement the query statement.
     * @param qf        the query object model factory.
     * @param vf        the value factory.
     * @return the query object model for the given statement.
     * @throws InvalidQueryException if the statement is invalid.
     * @throws RepositoryException   if another error occurs.
     */
    public QueryObjectModel createQueryObjectModel(String statement,
                                                   QueryObjectModelFactory qf,
                                                   ValueFactory vf)
            throws InvalidQueryException, RepositoryException;

    /**
     * Returns <code>true</code> if this QOM builder can handle a statement in
     * <code>language</code>.
     *
     * @param language the language of a query statement to build a QOM.
     * @return <code>true</code> if this builder can handle
     *         <code>language</code>; <code>false</code> otherwise.
     */
    boolean canHandle(String language);

    /**
     * Returns the set of query languages supported by this builder.
     *
     * @return String array containing the names of the supported languages.
     */
    String[] getSupportedLanguages();

    /**
     * Creates a String representation of the query object model in the syntax
     * this <code>QueryObjectModelBuilder</code> can handle.
     *
     * @param qom      the query object model.
     * @return a String representation of the QOM.
     * @throws InvalidQueryException if the query object model cannot be
     *                               converted into a String representation due
     *                               to restrictions in this syntax.
     */
    String toString(QueryObjectModel qom)
            throws InvalidQueryException;
}
