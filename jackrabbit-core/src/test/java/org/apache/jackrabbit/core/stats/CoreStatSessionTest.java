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
package org.apache.jackrabbit.core.stats;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.stats.CoreStat;

/**
 * CoreStats tests.
 */
public class CoreStatSessionTest extends AbstractStatTest {

    // TODO add test for number of operations executed

    private CoreStat cs;

    protected void setUp() throws Exception {
        super.setUp();
        cs = statManager.getCoreStat();
        cs.resetNumberOfSessions();
    }

    public void testNumberOfSessions1() throws Exception {

        cs.setEnabled(true);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(1, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }

    public void testNumberOfSessions2() throws Exception {

        cs.setEnabled(false);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(0, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }

    public void testNumberOfSessions3() throws Exception {

        cs.setEnabled(true);
        assertEquals(0, cs.getNumberOfSessions());

        Session s = superuser.impersonate(new SimpleCredentials("anonymous", ""
                .toCharArray()));
        assertNotNull(s);
        assertEquals(1, cs.getNumberOfSessions());

        cs.resetNumberOfSessions();
        assertEquals(0, cs.getNumberOfSessions());

        s.logout();
        assertEquals(0, cs.getNumberOfSessions());
    }
}
