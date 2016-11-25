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

import javax.jcr.RepositoryException;

import junit.framework.TestCase;

public class GQLTest extends TestCase {
    
    public void testGQL() throws RepositoryException {
        assertEquals(
                "//*[jcr:like(fn:lower-case(fn:name()), 'test')] ", 
                GQL.translateToXPath(
                        "order:- " +
                        "name:test", null, "assets"));
        assertEquals(
                "//*[1=1] order by @jcr:score descending", 
                GQL.translateToXPath(
                        "\"jcr:nativeXPath\":\"1=1\"", null, "assets"));
        assertEquals(
                "//*[(jcr:contains(assets/@a, '1') and 1=1)] ", 
                GQL.translateToXPath(
                        "order:- " +
                        "a: 1 " +
                        "\"jcr:nativeXPath\":\"1=1\"", null, "assets"));
        
    }

    public void testEscaping() throws RepositoryException {
        //simple things work
        assertEquals("//*[jcr:contains(assets/@a, 'b')] order by @jcr:score descending",
                GQL.translateToXPath("a:b", null, "assets"));

        //backslash is ignored (same as earlier) and only ", \ and : are escaped
        assertEquals("//*[jcr:contains(assets/@a, 'b')] order by @jcr:score descending",
                GQL.translateToXPath("a:b\\", null, "assets"));
        assertEquals("//*[jcr:contains(assets, 'ab')] order by @jcr:score descending",
                GQL.translateToXPath("a\\b", null, "assets"));
        assertEquals("//*[jcr:contains(assets/@a, 'b')] order by @jcr:score descending",
                GQL.translateToXPath("a:\\b", null, "assets"));

        //backward compatibility of quoted ":"
        assertEquals("//*[jcr:contains(assets/@a, '1:1')] order by @jcr:score descending",
                GQL.translateToXPath("a:\"1:1\"", null, "assets"));

        //escaping ":"
        assertEquals("//*[jcr:contains(assets/@a, '1:1')] order by @jcr:score descending",
                GQL.translateToXPath("a:\"1\\:1\"", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a, '1:1')] order by @jcr:score descending",
                GQL.translateToXPath("a:1\\:1", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a:, '1')] order by @jcr:score descending",
                GQL.translateToXPath("a\\::1", null, "assets"));

        //escaping \
        assertEquals("//*[jcr:contains(assets/@a, '1\\1')] order by @jcr:score descending",
                GQL.translateToXPath("a:\"1\\\\1\"", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a, '1\\1')] order by @jcr:score descending",
                GQL.translateToXPath("a:1\\\\1", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a_x005c_, '1')] order by @jcr:score descending",
                GQL.translateToXPath("a\\\\:1", null, "assets"));


        //escaping "
        assertEquals("//*[jcr:contains(assets/@a, '1\"1')] order by @jcr:score descending",
                GQL.translateToXPath("a:\"1\\\"1\"", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a, '1\"1')] order by @jcr:score descending",
                GQL.translateToXPath("a:1\\\"1", null, "assets"));

        assertEquals("//*[jcr:contains(assets/@a_x0022_, '1')] order by @jcr:score descending",
                GQL.translateToXPath("a\\\":1", null, "assets"));

    }

}
