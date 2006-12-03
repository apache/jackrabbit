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

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;


/**
 * Tests the method {@link javax.jcr.observation.ObservationManager#getRegisteredEventListeners()}.
 *
 * @test
 * @sources GetRegisteredEventListenersTest.java
 * @executeClass org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest
 * @keywords observation
 */
public class GetRegisteredEventListenersTest extends AbstractObservationTest {

    public void testGetSize() throws RepositoryException {
        EventListener[] listeners = toArray(obsMgr.getRegisteredEventListeners());
        assertEquals("A new session must not have any event listeners registered.", 0, listeners.length);

        EventListener listener1 = new EventResult(log);
        EventListener listener2 = new EventResult(log);
        addEventListener(listener1);
        addEventListener(listener2);

        listeners = toArray(obsMgr.getRegisteredEventListeners());
        assertEquals("Wrong number of event listeners.", 2, listeners.length);
    }

    /**
     * Tests if {@link javax.jcr.observation.ObservationManager#getRegisteredEventListeners()}
     * returns the correct listeners after an remove event listener.
     */
    public void testRemoveEventListener() throws RepositoryException {
        EventListener listener1 = new EventResult(log);
        EventListener listener2 = new EventResult(log);
        addEventListener(listener1);
        addEventListener(listener2);

        EventListener[] listeners = toArray(obsMgr.getRegisteredEventListeners());
        assertEquals("Wrong number of event listeners.", 2, listeners.length);

        removeEventListener(listener1);
        listeners = toArray(obsMgr.getRegisteredEventListeners());
        assertEquals("Wrong number of event listeners.", 1, listeners.length);
        assertEquals("Returned listener is not equal to regsitered one.", listener2, listeners[0]);
    }

}