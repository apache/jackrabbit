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
package org.apache.jackrabbit.core.query.qom;

import org.apache.jackrabbit.test.api.query.AbstractQueryTest;

import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelFactory;
import org.apache.jackrabbit.spi.commons.query.jsr283.qom.QueryObjectModelConstants;
import org.apache.jackrabbit.core.query.QueryManagerImpl;
import org.apache.jackrabbit.core.query.QueryImpl;

import javax.jcr.query.Query;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * <code>AbstractQOMTest</code> is a base class for test cases on the JQOM.
 */
public class AbstractQOMTest
        extends AbstractQueryTest
        implements QueryObjectModelConstants {

    protected QueryObjectModelFactory qomFactory;

    protected void setUp() throws Exception {
        super.setUp();
        QueryManagerImpl qm = (QueryManagerImpl) superuser.getWorkspace().getQueryManager();
        qomFactory = qm.getQOMFactory();
    }

    /**
     * Binds the given <code>value</code> to the variable named
     * <code>var</code>.
     *
     * @param q     the query
     * @param var   name of variable in query
     * @param value value to bind
     * @throws IllegalArgumentException if <code>var</code> is not a valid
     *                                  variable in this query.
     * @throws RepositoryException      if an error occurs.
     */
    protected void bindVariableValue(Query q, String var, Value value)
            throws RepositoryException {
        // TODO: remove cast when bindValue() is available on JSR 283 Query
        ((QueryImpl) q).bindValue(var, value);
    }
}
