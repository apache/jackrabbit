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
package org.apache.jackrabbit.webdav.util;

import java.util.Collections;

import junit.framework.TestCase;

/**
 * <code>LinkHeaderFieldParserTest</code>...
 */
public class LinkHeaderFieldParserTest extends TestCase {

    public void testSimple() {
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                Collections.singletonList("<a>; rel=foo"));
        assertEquals("a", lhfp.getFirstTargetForRelation("foo"));
    }

    public void testMulti() {
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                Collections.singletonList("<a>; rel=foo, <b>; rel=bar"));
        assertEquals("b", lhfp.getFirstTargetForRelation("bar"));
    }

    public void testMultiQs() {
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                Collections
                        .singletonList("<a,>; rel=\"fo\\\"o,\", <b,>; rel=bar"));
        assertEquals("b,", lhfp.getFirstTargetForRelation("bar"));
    }

    // broken by change to httpclient 4
//    public void testTruncated() {
//        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
//                Collections.singletonList("<a,>; rel=\"x\\\""));
//        assertEquals("a,", lhfp.getFirstTargetForRelation("x\\"));
//    }

    public void testCommas() {
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                Collections.singletonList(",<a>; rel=\"xy,z\","));
        assertEquals("a", lhfp.getFirstTargetForRelation("xy,z"));
    }

    public void testMultiRel() {
        LinkHeaderFieldParser lhfp = new LinkHeaderFieldParser(
                Collections.singletonList(",<a>; rel=\"a b\""));
        assertEquals("a", lhfp.getFirstTargetForRelation("a"));
    }
}
