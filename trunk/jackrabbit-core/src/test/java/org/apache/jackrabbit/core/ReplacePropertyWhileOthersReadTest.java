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
package org.apache.jackrabbit.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.test.AbstractJCRTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>ReplacePropertyWhileOthersReadTest</code>...
 */
public class ReplacePropertyWhileOthersReadTest extends AbstractJCRTest {

    private static final Logger log = LoggerFactory.getLogger(ReplacePropertyWhileOthersReadTest.class);

    private final List<Value> values = new ArrayList<Value>();

    private Node test;

    private final Random rand = new Random();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        test = testRootNode.addNode("test");
        test.addMixin(mixReferenceable);
        superuser.save();
        values.add(vf.createValue("value"));
        values.add(vf.createValue(new BigDecimal(1234)));
        values.add(vf.createValue(Calendar.getInstance()));
        values.add(vf.createValue(1.234));
        values.add(vf.createValue(true));
        values.add(vf.createValue(test));
        values.add(vf.createValue(vf.createBinary(
                new ByteArrayInputStream(new byte[0]))));
    }

    @Override
    protected void tearDown() throws Exception {
        test = null;
        values.clear();
        super.tearDown();
    }

    public void testAddRemove() throws Exception {
        final Property prop = test.setProperty("prop", getRandomValue());
        superuser.save();

        Thread reader = new Thread(new Runnable() {

            String path = prop.getPath();

            public void run() {
                // run for three seconds
                long stop = System.currentTimeMillis()
                        + TimeUnit.SECONDS.toMillis(3);
                while (System.currentTimeMillis() < stop) {
                    try {
                        Session s = getHelper().getSuperuserSession();
                        try {
                            s.getProperty(path);
                        } finally {
                            s.logout();
                        }
                    } catch (RepositoryException e) {
                        log.warn("", e);
                    }
                }
            }
        });
        Tail tail = Tail.start(new File("target", "jcr.log"),
                "overwriting cached entry");
        try {
            reader.start();
            while (reader.isAlive()) {
                test.getProperty("prop").remove();
                int type;
                boolean isMultivalued;
                if (rand.nextBoolean()) {
                    Value v = getRandomValue();
                    isMultivalued = false;
                    type = v.getType();
                    test.setProperty("prop", v);
                } else {
                    Value[] v = getRandomMultiValue();
                    type = v[0].getType();
                    isMultivalued = true;
                    test.setProperty("prop", v);
                }
                superuser.save();
                assertEquals(isMultivalued, test.getProperty("prop").isMultiple());
                assertEquals(type, test.getProperty("prop").getType());
            }
            assertFalse("detected 'overwriting cached entry' messages in log",
                    tail.getLines().iterator().hasNext());
        } finally {
            tail.close();
        }
    }

    private Value getRandomValue() {
        return values.get(rand.nextInt(values.size()));
    }

    private Value[] getRandomMultiValue() {
        return new Value[]{getRandomValue()};
    }
}
