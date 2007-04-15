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
package org.apache.jackrabbit.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 * The <code>PatternPathTest</code> class
 *
 * @author Felix Meschberger
 */
public class PatternPathTest extends ClassLoaderTestBase {

    private static final String DUMMY = "/dummy/classes";
    private static final String[] patterns = {
            "/apps|libs/*/classes",
            "/*/classes", null, "", DUMMY, "/", "*", "|",
            "/apps/developers/fmeschbe/lib/*.doc" };
    private static final Set included;
    private static final Set excluded;

    static {
        included = new HashSet();

        // matches *
        included.add("/apps");
        included.add("/libs");
        included.add("/jcr:system");       // Repository child node
        included.add("/jcr:primaryType");  // Repository property

        // matches "/"
        included.add("/");

        // matches: "/apps|libs/*/classes"
        included.add("/apps/CFC/classes");
        included.add("/apps/Forms/classes");
        included.add("/apps/playground/classes");
        included.add("/libs/CFC/classes");
        included.add("/libs/Forms/classes");

        // matches "/*/classes"
        included.add(DUMMY);

        // matches "/apps/developers/fmeschbe/lib/*.doc"
        included.add("/apps/developers/fmeschbe/lib/test1.doc");
        included.add("/apps/developers/fmeschbe/lib/other.doc");

        excluded = new HashSet();
        excluded.add("/apps/developers/fmeschbe/classes");
        excluded.add("/libs/developers/fmeschbe/classes");

        excluded.add("/apps/developers/fmeschbe/lib/readme.txt");
    }

    protected void setUp() throws Exception {
        super.setUp();

        InputStream ins = getClass().getResourceAsStream("PatternPathTest.preload.properties");
        if (ins != null) {
            try {
                loadRepository(session, ins);
            } finally {
                try {
                    ins.close();
                } catch (IOException ignore) {}
            }
        }
    }

    protected void tearDown() throws Exception {
        if (session != null) {
            clearRepository(session);
            session.logout();
            session = null;
        }

        super.tearDown();
    }

    public void testCreation() {
        PatternPath ppl = new PatternPath(session, patterns);

        // check completeness
        Set pplSet = new HashSet(Arrays.asList(ppl.getPath()));
        for (int i=0; i < patterns.length; i++) {
            String pattern = patterns[i];
            if (pattern == null || pattern.length() == 0) {
                assertFalse("Empty Entry must not be conained", pplSet.contains(pattern));
            } else {
                assertTrue("Non Empty Entry must be contained", pplSet.contains(pattern));
            }
        }
    }

    public void testExpandedPaths() throws RepositoryException {
        PatternPath ppl = new PatternPath(session, patterns);

        // expand the path
        List paths = ppl.getExpandedPaths();

        // check whether all expanded entries are expected
        Set expected = new HashSet(included);
        assertEquals("Number of path entries", expected.size(), paths.size());
        for (Iterator pi=paths.iterator(); pi.hasNext(); ) {
            String entry = (String) pi.next();
            assertTrue("Unexpected path entry " + entry, expected.remove(entry));
        }

        // check whether the expected inclusions have all been in the expansion
        assertTrue("Not all inclusions: " + expected, expected.isEmpty());

        // check that no exlusions are included
        for (Iterator pi=paths.iterator(); pi.hasNext(); ) {
            String entry = (String) pi.next();
            assertFalse("Path entry must be excluded" + entry, excluded.contains(entry));
        }
    }

    public void testMatchPath() {
        PatternPath ppl = new PatternPath(session, patterns);

        // paths expected to match
        for (Iterator ii=included.iterator(); ii.hasNext(); ) {
            String path = (String) ii.next();
            assertTrue("Expect Match: " + path, ppl.matchPath(path));
        }

        // paths expected to not match
        for (Iterator ei=excluded.iterator(); ei.hasNext(); ) {
            String path = (String) ei.next();
            assertFalse("Unexpect Match: " + path, ppl.matchPath(path));
        }
    }
}
