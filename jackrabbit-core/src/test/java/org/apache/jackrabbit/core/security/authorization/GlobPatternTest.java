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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.test.JUnitTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>GlobPatternTest</code>...
 */
public class GlobPatternTest extends JUnitTest {

    private static Logger log = LoggerFactory.getLogger(GlobPatternTest.class);

    public void testMatchesNoMetaChar() {
        GlobPattern gp = GlobPattern.create("/a/b/c");

        Map<String,Boolean> tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c", true);
        tests.put("/a/b/c/d", true);
        tests.put("/a/b/c/d/e", true);
        tests.put("/a/b/c/d/e/f", true);

        tests.put("/", false);
        tests.put("/a", false);
        tests.put("/b/c", false);
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(tests.get(toTest) == gp.matches(toTest));
        }
    }

    public void testMatchesWildcardAll() {

        Map<String,Boolean> tests = new HashMap<String,Boolean>();

        // restriction "*" matches /foo, all siblings of foo and foo's and the siblings' descendants
        GlobPattern gp = GlobPattern.create("/a/b/c", "*");
        tests.put("/a/b/c", true);        // foo itself
        tests.put("/a/b/c/d", true);      // child of foo
        tests.put("/a/b/c/d/e", true);    // child of foo
        tests.put("/a/b/c/d/e/f", true);  // child of foo
        tests.put("/a/b/cde", true);      // sibling
        tests.put("/a/b/cde/e/f", true);  // child of the sibling

        tests.put("/", false);
        tests.put("/a", false);
        tests.put("/b/c", false);

        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        // restriction "*cat" matches all siblings and descendants of /foo that have a name ending with cat
        gp = GlobPattern.create("/a/b/c", "*e");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c/e", true);      // descendant with name segment 'e'
        tests.put("/a/b/c/d/e", true);    // descendant with name segment 'e'
        tests.put("/a/b/c/gge", true);    // descendant with name segment ending with 'e'
        tests.put("/a/b/c/d/gge", true);  // descendant with name segment ending with 'e'
        tests.put("/a/b/ce", true);       // sibling whose name ends with 'e'
        tests.put("/a/b/chee", true);     // sibling whose name ends with 'e'
        tests.put("/a/b/cd/e", true);     // descendant of sibling named 'e'
        tests.put("/a/b/cd/f/e", true);   // descendant of sibling named 'e'
        tests.put("/a/b/cd/e", true);     // descendant of sibling with name ending with 'e'
        tests.put("/a/b/cd/f/e", true);   // descendant of sibling with name ending with 'e'

        tests.put("/", false);
        tests.put("/a", false);
        tests.put("/b/c", false);
        tests.put("/a/b/c", false);
        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/e/f", false);
        tests.put("/a/b/c/d/f/e/f", false);
        tests.put("/a/b/c/d/f/efg", false);
        tests.put("/a/b/c/d/f/f", false);
        tests.put("/a/b/c/e/f", false);
        tests.put("/a/b/ce/", false);
        tests.put("/a/b/ceg", false);
        
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        // restriction "*/cat" matches all descendants of /foo and foo's siblings that have a name segment "cat"
        gp = GlobPattern.create("/a/b/c", "*/e");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c/e", true);      // descendant with name segment 'e'
        tests.put("/a/b/c/d/e", true);    // descendant with name segment 'e'
        tests.put("/a/b/cd/e", true);     // descendant of sibling named 'e'
        tests.put("/a/b/cd/f/e", true);   // descendant of sibling named 'e'

        tests.put("/", false);
        tests.put("/a", false);
        tests.put("/b/c", false);
        tests.put("/a/b/c", false);
        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/e/f", false);
        tests.put("/a/b/c/d/f/e/f", false);
        tests.put("/a/b/c/d/f/efg", false);
        tests.put("/a/b/c/d/f/f", false);
        tests.put("/a/b/c/e/f", false);
        tests.put("/a/b/ce/", false);
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        // all descendants of '/a/b/c/e'
        gp = GlobPattern.create("/a/b/c", "e/*");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/ce/", true);
        tests.put("/a/b/ce/f/g/h", true);
        tests.put("/a/b/ce/d/e/f", true);

        tests.put("/a/b/c", false);
        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/e", false);
        tests.put("/a/b/c/d/e/f", false);
        tests.put("/a/b/c/d/f/f", false);
        tests.put("/a/b/c/d/f/e/f", false);
        tests.put("/a/b/cee/d/e/f", false);
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        // restriction "*e/*" matches all descendants of /foo that have an intermediate segment ending with 'e'
        gp = GlobPattern.create("/a/b/c", "*e/*");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/ce/", true);
        tests.put("/a/b/ceeeeeee/f/g/h", true);
        tests.put("/a/b/cde/d/e/f", true);
        tests.put("/a/b/c/d/e/f", true);
        tests.put("/a/b/c/d/e/", true);
        tests.put("/a/b/ced/d/e/f", true);        
        
        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/e", false);
        tests.put("/a/b/c/d/f/f", false);
        tests.put("/a/b/c/ed/f/f", false);
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        //  restriction /*cat  matches all children of /a/b/c whose path ends with "cat"
        gp = GlobPattern.create("/a/b/c", "/*cat");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c/cat", true);
        tests.put("/a/b/c/acat", true);
        tests.put("/a/b/c/f/cat", true);
        tests.put("/a/b/c/f/acat", true);

        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/cat/e", false);  // cat only intermediate segment
        tests.put("/a/b/c/d/acat/e", false);  // cat only intermediate segment
        tests.put("/a/b/c/d/cata/e", false);  // cat only intermediate segment
        tests.put("/a/b/c/d/cate", false);
        tests.put("/a/b/cat", false);        // siblings do no match
        tests.put("/a/b/cat/ed/f/f", false); // ... nor do siblings' children
        tests.put("/a/b/ced/cat", false);    // ... nor do siblings' children

        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        //  restriction /*/cat  matches all non-direct descendants of /foo named "cat"
        gp = GlobPattern.create("/a/b/c", "/*/cat");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c/a/cat", true);
        tests.put("/a/b/c/d/e/f/cat", true);

        tests.put("/a/b/c/cat", false);
        tests.put("/a/b/c/cate", false);
        tests.put("/a/b/c/acat", false);
        tests.put("/a/b/c/cat/d", false);
        tests.put("/a/b/c/d/acat", false);
        tests.put("/a/b/c/d/cate", false);        
        tests.put("/a/b/c/d/cat/e", false);   // cat only intermediate segment
        tests.put("/a/b/c/d/acat/e", false);  // cat only intermediate segment
        tests.put("/a/b/c/d/cata/e", false);  // cat only intermediate segment
        tests.put("/a/b/cat", false);        // siblings do no match
        tests.put("/a/b/cat/ed/f/f", false); // ... nor do siblings' children
        tests.put("/a/b/ced/cat", false);    // ... nor do siblings' children
        tests.put("/a/b/ced/f/cat", false);  // ... nor do siblings' children
        
        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }

        //  restriction /cat* matches all descendant paths of /foo that have the
        //  direct foo-descendant segment starting with "cat"
        gp = GlobPattern.create("/a/b/c", "/cat*");
        tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c/cat", true);
        tests.put("/a/b/c/cats", true);
        tests.put("/a/b/c/cat/s", true);
        tests.put("/a/b/c/cats/d/e/f", true);


        tests.put("/a/b/c/d/cat", false);
        tests.put("/a/b/c/d/cats", false);
        tests.put("/a/b/c/d/e/cat", false);
        tests.put("/a/b/c/d/e/cats", false);
        tests.put("/a/b/c/acat", false);
        tests.put("/a/b/c/d/acat", false);
        tests.put("/a/b/c/d/cat/e", false);
        tests.put("/a/b/c/d/acat/e", false);
        tests.put("/a/b/c/d/cata/e", false);  
        tests.put("/a/b/cat", false);        // siblings do no match
        tests.put("/a/b/cat/ed/f/f", false); // ... nor do siblings' children
        tests.put("/a/b/ced/cat", false);    // ... nor do siblings' children
        tests.put("/a/b/ced/f/cat", false);  // ... nor do siblings' children

        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }
    }

    public void testEmptyRestriction() {
        GlobPattern gp = GlobPattern.create("/a/b/c", "");

        Map<String,Boolean> tests = new HashMap<String,Boolean>();
        tests.put("/a/b/c", true);

        tests.put("/a/b/c/d", false);
        tests.put("/a/b/c/d/e", false);
        tests.put("/a/b/c/d/e/f", false);
        tests.put("/", false);
        tests.put("/a", false);
        tests.put("/a/b/cde", false);

        for (String toTest : tests.keySet()) {
            log.info(gp + " - " + toTest + " : " + tests.get(toTest));
            assertTrue(gp + " : " + toTest, tests.get(toTest) == gp.matches(toTest));
        }
    }

    public void testMatchesItem() {
       // TODO
    }
}