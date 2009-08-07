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
package org.apache.jackrabbit.util;

import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.uuid.VersionFourGenerator;

import junit.framework.TestCase;

/**
 * Test the Version 4 (random), Leach-Salz UUID generation.
 *
 * @author Thomas Mueller
 */
public class UUIDTest extends TestCase {

    public void testUUID() {
        UUID uuid1 = UUID.randomUUID();
        checkUUIDFormat(uuid1);
        long maxHigh = 0, maxLow = 0, minHigh = -1L, minLow = -1L;
        for (int i = 0; i < 100; i++) {
            UUID uuid = UUID.randomUUID();
            maxHigh |= uuid.getMostSignificantBits();
            maxLow |= uuid.getLeastSignificantBits();
            minHigh &= uuid.getMostSignificantBits();
            minLow &= uuid.getLeastSignificantBits();
        }
        UUID max = new UUID(maxHigh, maxLow);
        assertEquals(max.toString(), "ffffffff-ffff-4fff-bfff-ffffffffffff");
        UUID min = new UUID(minHigh, minLow);
        assertEquals(min.toString(), "00000000-0000-4000-8000-000000000000");

        // test with a wrong provider
        // must fall back to the default
        VersionFourGenerator.setPRNGProvider("wrong", "wrong");
        UUID uuid2 = UUID.randomUUID();
        checkUUIDFormat(uuid2);
    }

    private void checkUUIDFormat(UUID uuid) {
        long high = uuid.getMostSignificantBits();
        long low = uuid.getLeastSignificantBits();
        long high2 = (high & (~0xf000L)) | 0x4000L; // version 4 (random)
        assertEquals(high, high2);
        long low2 = (low & 0x3fffffffffffffffL) | 0x8000000000000000L; // variant (Leach-Salz)
        assertEquals(low, low2);
    }

}
