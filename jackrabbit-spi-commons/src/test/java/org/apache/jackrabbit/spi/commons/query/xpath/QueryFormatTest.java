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
package org.apache.jackrabbit.spi.commons.query.xpath;

import java.util.Collections;

import javax.jcr.query.Query;
import javax.jcr.query.InvalidQueryException;

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DummyNamespaceResolver;
import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.QueryParser;

import junit.framework.TestCase;

/**
 * <code>QueryFormatTest</code> performs tests on {@link QueryFormat}.
 */
public class QueryFormatTest extends TestCase {

    private static final NameResolver RESOLVER = new DefaultNamePathResolver(
            new DummyNamespaceResolver());

    private static final QueryNodeFactory FACTORY = new DefaultQueryNodeFactory(Collections.EMPTY_LIST);

    public void testSelectWithOrderBy() throws InvalidQueryException {
        String stmt = "//element(*, foo)/(@a|@b) order by @bar";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        assertEquals(stmt, QueryFormat.toString(root, RESOLVER));
    }
}
