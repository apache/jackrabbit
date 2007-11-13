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
