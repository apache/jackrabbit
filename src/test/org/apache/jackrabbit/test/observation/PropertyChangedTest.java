/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.observation;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

/**
 * Test cases for {@link javax.jcr.observation.Event#PROPERTY_CHANGED
 * PROPERTY_CHANGED} events.
 */
public class PropertyChangedTest extends AbstractObservationTest {

    public void testSinglePropertyChanged() throws RepositoryException {
        EventResult result = new EventResult(log);
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bar", new String[]{"foo"});
        testRootNode.save();
        addEventListener(result, Event.PROPERTY_CHANGED);
        foo.getProperty("bar").setValue(new String[]{"foobar"});
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{"foo/bar"});
    }

    public void testMultiPropertyChanged() throws RepositoryException {
        EventResult result = new EventResult(log);
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("prop1", new String[]{"foo"});
        foo.setProperty("prop2", new String[]{"bar"});
        testRootNode.save();
        addEventListener(result, Event.PROPERTY_CHANGED);
        foo.getProperty("prop1").setValue(new String[]{"foobar"});
        foo.getProperty("prop2").setValue(new String[]{"foobar"});
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{"foo/prop1", "foo/prop2"});
    }

    public void testSinglePropertyChangedWithAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("bar", new String[]{"foo"});
        testRootNode.save();
        addEventListener(result, Event.PROPERTY_CHANGED);
        foo.getProperty("bar").setValue(new String[]{"foobar"});
        foo.setProperty("foo", new String[]{"bar"});    // will not fire prop changed event
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{"foo/bar"});
    }

    public void testMultiPropertyChangedWithAdded() throws RepositoryException {
        EventResult result = new EventResult(log);
        Node foo = testRootNode.addNode("foo");
        foo.setProperty("prop1", new String[]{"foo"});
        foo.setProperty("prop2", new String[]{"bar"});
        testRootNode.save();
        addEventListener(result, Event.PROPERTY_CHANGED);
        foo.getProperty("prop1").setValue(new String[]{"foobar"});
        foo.getProperty("prop2").setValue(new String[]{"foobar"});
        foo.setProperty("prop3", new String[]{"foo"}); // will not fire prop changed event
        testRootNode.save();
        removeEventListener(result);
        Event[] events = result.getEvents(DEFAULT_WAIT_TIMEOUT);
        checkPropertyChanged(events, new String[]{"foo/prop1", "foo/prop2"});
    }


}
