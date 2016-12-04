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
package org.apache.jackrabbit.core.observation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.apache.jackrabbit.core.Tail;
import org.apache.jackrabbit.test.api.observation.AbstractObservationTest;
import org.apache.jackrabbit.test.api.observation.EventResult;

/**
 * <code>WarningOnSaveWithNotificationThreadTest</code> checks if the repository
 * writes a warning to the log file when changes are performed with the event
 * notification thread. See JCR-3426.
 */
public class WarningOnSaveWithNotificationThreadTest extends
        AbstractObservationTest {

    private static final String MESSAGE = "Save call with event notification thread detected";

    public void testWarning() throws Exception {
        final List<Exception> exceptions = new ArrayList<Exception>();
        EventResult result = new EventResult(log) {
            @Override
            public void onEvent(EventIterator events) {
                try {
                    Session s = getHelper().getSuperuserSession();
                    try {
                        s.getNode(testRoot).addNode("bar");
                        s.save();
                    } finally {
                        s.logout();
                    }
                } catch (RepositoryException e) {
                    exceptions.add(e);
                }
                super.onEvent(events);
            }
        };
        addEventListener(result);

        Tail tail = Tail.start(new File("target", "jcr.log"), MESSAGE);
        try {
            testRootNode.addNode("foo");
            superuser.save();

            removeEventListener(result);
            result.getEvents(5000);
            assertTrue("Warn message expected in log file.",
                    tail.getLines().iterator().hasNext());
        } finally {
            tail.close();
        }

        if (!exceptions.isEmpty()) {
            fail(exceptions.get(0).toString());
        }
    }
}
