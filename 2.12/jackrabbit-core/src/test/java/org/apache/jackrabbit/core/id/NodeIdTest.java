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
package org.apache.jackrabbit.core.id;

import junit.framework.TestCase;

public class NodeIdTest extends TestCase {

    private static final NodeId[] ids = {
        NodeId.randomId(), // random id
        new NodeId(0, 0),
        new NodeId(-1, -1),
        new NodeId("cafebabe-cafe-babe-cafe-babecafebabe")
    };

    public void testDenotesNode() {
        for (NodeId id : ids) {
            assertTrue(id.denotesNode());
        }
    }

    public void testGetMostAndLeastSignificantBits() {
        for (NodeId id : ids) {
            long msb = id.getMostSignificantBits();
            long lsb = id.getLeastSignificantBits();
            assertEquals(id, new NodeId(msb, lsb));
        }
    }

    public void testGetRawBytes() {
        for (NodeId id : ids) {
            assertEquals(id, new NodeId(id.getRawBytes()));
        }
    }

    public void testToString() {
        for (NodeId id : ids) {
            assertEquals(id, new NodeId(id.toString()));
        }
    }

    public void testCompareTo() {
        for (NodeId id : ids) {
            assertEquals(0, id.compareTo(id));
        }

        NodeId[] ordered = {
                new NodeId(-1, -1),
                new NodeId(-1, 0),
                new NodeId(0, -1),
                new NodeId(0, 0),
                new NodeId(0, 1),
                new NodeId(1, 0),
                new NodeId(1, 1)
        };
        for (int i = 0; i < ordered.length; i++) {
            for (int j = 0; j < i; j++) {
                assertEquals(1, ordered[i].compareTo(ordered[j]));
            }
            assertEquals(0, ordered[i].compareTo(ordered[i]));
            for (int j = i + 1; j < ordered.length; j++) {
                assertEquals(-1, ordered[i].compareTo(ordered[j]));
            }
        }
    }

    public void testUuidFormat() {
        long maxHigh = 0, maxLow = 0, minHigh = -1L, minLow = -1L;
        for (int i = 0; i < 100; i++) {
            NodeId id = NodeId.randomId();
            assertUuidFormat(id);
            maxHigh |= id.getMostSignificantBits();
            maxLow |= id.getLeastSignificantBits();
            minHigh &= id.getMostSignificantBits();
            minLow &= id.getLeastSignificantBits();
        }
        NodeId max = new NodeId(maxHigh, maxLow);
        assertEquals("ffffffff-ffff-4fff-bfff-ffffffffffff", max.toString());
        NodeId min = new NodeId(minHigh, minLow);
        assertEquals("00000000-0000-4000-8000-000000000000", min.toString());
    }

    private void assertUuidFormat(NodeId id) {
        long high = id.getMostSignificantBits();
        long low = id.getLeastSignificantBits();
        long high2 = (high & (~0xf000L)) | 0x4000L; // version 4 (random)
        assertEquals(high, high2);
        long low2 = (low & 0x3fffffffffffffffL) | 0x8000000000000000L; // variant (Leach-Salz)
        assertEquals(low, low2);
    }

}
