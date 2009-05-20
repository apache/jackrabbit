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
package org.apache.jackrabbit.core.state;

import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;

import junit.framework.TestCase;

/**
 * <code>NameSetTest</code> checks if equals and hashCode semantics are correct
 * on {@link NameSet}.
 */
public class NameSetTest extends TestCase {

    private static final NameFactory FACTORY = NameFactoryImpl.getInstance();

    private static final String NAME_STRING = "{}foo";

    public void testEquals() {
        NameSet set1 = new NameSet();
        NameSet set2 = new NameSet();
        assertEquals(set1, set2);

        set1.add(FACTORY.create(NAME_STRING));
        set2.add(FACTORY.create(NAME_STRING));
        assertEquals(set1, set2);
    }

    public void testHashCode() {
        NameSet set1 = new NameSet();
        NameSet set2 = new NameSet();
        assertEquals(set1.hashCode(), set2.hashCode());

        set1.add(FACTORY.create(NAME_STRING));
        set2.add(FACTORY.create(NAME_STRING));
        assertEquals(set1.hashCode(), set2.hashCode());
    }
}
