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

import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.DummyNamespaceResolver;
import org.apache.jackrabbit.spi.commons.query.QueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.DefaultQueryNodeFactory;
import org.apache.jackrabbit.spi.commons.query.QueryRootNode;
import org.apache.jackrabbit.spi.commons.query.QueryParser;
import org.apache.jackrabbit.spi.commons.query.OrderQueryNode;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;

import junit.framework.TestCase;

/**
 * <code>XPathOrderByTest</code> performs various tests related to parsing an
 * XPath statement with order by.
 */
public class XPathOrderByTest extends TestCase {

    private static final NameResolver RESOLVER = new DefaultNamePathResolver(
            new DummyNamespaceResolver());

    private static final QueryNodeFactory FACTORY = new DefaultQueryNodeFactory(Collections.EMPTY_LIST);

    public void testSimpleOrderBy() throws Exception {
        String stmt = "//* order by @bar";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        OrderQueryNode.OrderSpec[] specs = root.getOrderNode().getOrderSpecs();
        assertEquals(1, specs.length);
        assertTrue(specs[0].isAscending());
        checkName(Name.NS_DEFAULT_URI, "bar", specs[0].getProperty());
        Path propPath = specs[0].getPropertyPath();
        assertEquals(1, propPath.getLength());
        checkName(Name.NS_DEFAULT_URI, "bar", propPath.getName());
    }

    public void testAscending() throws Exception {
        String stmt = "//* order by @bar ascending";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        OrderQueryNode.OrderSpec[] specs = root.getOrderNode().getOrderSpecs();
        assertEquals(1, specs.length);
        assertTrue(specs[0].isAscending());
    }

    public void testDescending() throws Exception {
        String stmt = "//* order by @bar descending";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        OrderQueryNode.OrderSpec[] specs = root.getOrderNode().getOrderSpecs();
        assertEquals(1, specs.length);
        assertFalse(specs[0].isAscending());
    }

    public void testChildAxis() throws Exception {
        String stmt = "//* order by foo_x0020_bar/@bar";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        assertEquals(1, root.getOrderNode().getOrderSpecs().length);
        OrderQueryNode.OrderSpec[] specs = root.getOrderNode().getOrderSpecs();
        assertEquals(1, specs.length);
        assertTrue(specs[0].isAscending());
        checkName(Name.NS_DEFAULT_URI, "bar", specs[0].getProperty());
        Path propPath = specs[0].getPropertyPath();
        Path.Element[] elements = propPath.getElements();
        assertEquals(2, elements.length);
        checkName(Name.NS_DEFAULT_URI, "foo bar", elements[0].getName());
        checkName(Name.NS_DEFAULT_URI, "bar", elements[1].getName());
    }

    public void testRoundTrip() throws Exception {
        String stmt = "//* order by foo_x0020_bar/@bar";
        QueryRootNode root = QueryParser.parse(stmt, Query.XPATH, RESOLVER, FACTORY);
        assertEquals(stmt, QueryFormat.toString(root, RESOLVER));
    }

    private void checkName(String uri, String localName, Name name) {
        assertEquals(uri, name.getNamespaceURI());
        assertEquals(localName, name.getLocalName());
    }
}
