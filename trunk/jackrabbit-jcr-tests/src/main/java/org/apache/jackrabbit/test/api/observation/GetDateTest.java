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
package org.apache.jackrabbit.test.api.observation;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * <code>GetDateTest</code> checks if the dates returned by events are
 * monotonically increasing.
 */
public class GetDateTest extends AbstractObservationTest {

    public void testLinearTime() throws RepositoryException {
        List<String> names = Arrays.asList(new String[]{nodeName1, nodeName2, nodeName3});
        List<Long> dates = new ArrayList<Long>();
        for (Iterator<String> it = names.iterator(); it.hasNext(); ) {
            final String name = it.next();
            Event[] events = getEvents(new Callable() {
                public void call() throws RepositoryException {
                    testRootNode.addNode(name, testNodeType);
                    testRootNode.getSession().save();
                }
            }, Event.NODE_ADDED);
            for (int i = 0; i < events.length; i++) {
                dates.add(new Long(events[i].getDate()));
            }
            try {
                // wait for a moment
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        List<Long> sortedDates = new ArrayList<Long>(dates);
        Collections.sort(sortedDates);
        assertEquals(sortedDates, dates);
    }
}
