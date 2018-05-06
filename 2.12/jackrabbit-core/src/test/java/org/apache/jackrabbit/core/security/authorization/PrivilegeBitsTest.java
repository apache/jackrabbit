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
package org.apache.jackrabbit.core.security.authorization;

import org.apache.jackrabbit.test.JUnitTest;

/**
 * <code>PrivilegeBitsTest</code>...
 */
public class PrivilegeBitsTest extends JUnitTest {

    private static final PrivilegeBits READ_PRIVILEGE_BITS = PrivilegeBits.getInstance(1);

    private static final long[] LONGS = new long[] {1, 2, 13, 199, 512, Long.MAX_VALUE/2, Long.MAX_VALUE-1, Long.MAX_VALUE};

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    //-----------------------------------------------------------< internal >---

    public void testLongValue() {
        // empty
        assertEquals(PrivilegeRegistry.NO_PRIVILEGE, PrivilegeBits.EMPTY.longValue());

        // long based privilege bits
        for (long l : LONGS) {
            assertEquals(l, PrivilegeBits.getInstance(l).longValue());
        }

        // long based privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        long l = pb.longValue();
        while (l < Long.MAX_VALUE/2) {
            l = l << 1;
            pb = pb.nextBits();
            assertEquals(l, pb.longValue());
        }

        // other privilege bits: long value not available.
        for (int i = 0; i < 10; i++) {
            pb = pb.nextBits();
            assertEquals(0, pb.longValue());
        }

        // modifiable privilege bits
        pb = READ_PRIVILEGE_BITS;
        for (int i = 0; i < 100; i++) {
            PrivilegeBits modifiable = PrivilegeBits.getInstance(pb);
            assertEquals(pb.longValue(), modifiable.longValue());
            pb = pb.nextBits();
        }
    }

    public void testNextBits() {
        // empty
        assertSame(PrivilegeBits.EMPTY, PrivilegeBits.EMPTY.nextBits());

        // long based privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        long l = pb.longValue();
        while (l < Long.MAX_VALUE/2) {
            l = l << 1;
            pb = pb.nextBits();
            assertEquals(l, pb.longValue());
        }

        // other privilege bits: long value not available.
        for (int i = 0; i < 10; i++) {
            PrivilegeBits nxt = pb.nextBits();
            assertEquals(nxt, pb.nextBits());
            assertFalse(pb.equals(nxt));
            pb = nxt;
        }

        // modifiable privilege bits
        pb = READ_PRIVILEGE_BITS;
        for (int i = 0; i < 100; i++) {
            PrivilegeBits modifiable = PrivilegeBits.getInstance(pb);
            try {
                modifiable.nextBits();
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }
            pb = pb.nextBits();
        }
    }


    public void testUnmodifiable() {
        assertSame(PrivilegeBits.EMPTY, PrivilegeBits.EMPTY.unmodifiable());

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance(pb);

        for (int i = 0; i < 100; i++) {
            PrivilegeBits nxt = pb.nextBits();
            assertSame(nxt, nxt.unmodifiable());
            assertEquals(nxt, nxt.unmodifiable());

            mod.add(nxt);
            assertNotSame(mod, mod.unmodifiable());
            assertEquals(mod, mod.unmodifiable());

            pb = nxt;
        }
    }

    //---------------------------------------------------------------< test >---

    public void testIncludesRead() {
        // empty
        assertFalse(PrivilegeBits.EMPTY.includesRead());

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        assertTrue(pb.includesRead());
        assertTrue(PrivilegeBits.getInstance(pb).includesRead());

        PrivilegeBits mod = PrivilegeBits.getInstance();
        for (int i = 0; i < 100; i++) {
            mod.add(pb);            
            assertTrue(mod.includesRead());

            pb = pb.nextBits();
            assertFalse(pb.toString(), pb.includesRead());
            assertFalse(PrivilegeBits.getInstance(pb).includesRead());

            PrivilegeBits modifiable = PrivilegeBits.getInstance(pb);
            modifiable.add(READ_PRIVILEGE_BITS);
            assertTrue(modifiable.includesRead());
        }
    }

    public void testIncludes() {
        // empty
        assertTrue(PrivilegeBits.EMPTY.includes(PrivilegeBits.EMPTY));

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance();

        for (int i = 0; i < 100; i++) {

            assertFalse(PrivilegeBits.EMPTY.includes(pb));
            assertTrue(pb.includes(PrivilegeBits.EMPTY));

            mod.add(pb);
            assertTrue(mod.includes(pb));

            PrivilegeBits nxt = pb.nextBits();
            assertTrue(nxt.includes(nxt));
            assertTrue(nxt.includes(PrivilegeBits.getInstance(nxt)));
            
            assertFalse(pb + " should not include " + nxt, pb.includes(nxt));
            assertFalse(nxt + " should not include " + pb, nxt.includes(pb));
            assertFalse(mod.includes(nxt));
            assertFalse(nxt.includes(mod));
           
            pb = nxt;
        }
    }

    public void testIsEmpty() {
        // empty
        assertTrue(PrivilegeBits.EMPTY.isEmpty());

        // any other bits should not be empty
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance(pb);
        for (int i = 0; i < 100; i++) {
            assertFalse(pb.isEmpty());
            assertFalse(PrivilegeBits.getInstance(pb).isEmpty());
            
            pb = pb.nextBits();
            mod.add(pb);
            assertFalse(mod.isEmpty());
            
            PrivilegeBits tmp = PrivilegeBits.getInstance(pb);
            tmp.diff(pb);
            assertTrue(tmp.toString(), tmp.isEmpty());
        }
    }

    //----------------------------------------------------------------< mod >---

    public void testAdd() {
        // empty
        try {
            PrivilegeBits.EMPTY.add(PrivilegeBits.EMPTY);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance(pb);

        for (int i = 0; i < 100; i++) {
            try {
                pb.add(PrivilegeBits.EMPTY);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            try {
                pb.add(mod);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            PrivilegeBits nxt = pb.nextBits();
            try {
                pb.add(nxt);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            long before = mod.longValue();
            long nxtLong = nxt.longValue();

            mod.add(nxt);
            if (nxt.longValue() != 0) {
                assertEquals(before | nxtLong, mod.longValue());
            }
            assertTrue(mod.includes(nxt));

            PrivilegeBits tmp = PrivilegeBits.getInstance(pb);
            assertTrue(tmp.includes(pb));
            assertFalse(tmp.includes(nxt));
            if (READ_PRIVILEGE_BITS.equals(pb)) {
                assertTrue(tmp.includesRead());            
            } else {
                assertFalse(tmp.includesRead());
            }
            tmp.add(nxt);
            assertTrue(tmp.includes(pb) && tmp.includes(nxt));
            if (READ_PRIVILEGE_BITS.equals(pb)) {
                assertTrue(tmp.includesRead());
                assertTrue(tmp.includes(READ_PRIVILEGE_BITS));
            } else {
                assertFalse(tmp.toString(), tmp.includesRead());
                assertFalse(tmp.includes(READ_PRIVILEGE_BITS));
            }
            tmp.add(READ_PRIVILEGE_BITS);
            assertTrue(tmp.includesRead());
            assertTrue(tmp.includes(READ_PRIVILEGE_BITS));

            pb = nxt;
        }
    }

    public void testDiff() {
        // empty
        try {
            PrivilegeBits.EMPTY.diff(PrivilegeBits.EMPTY);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance(pb);

        for (int i = 0; i < 100; i++) {
            PrivilegeBits nxt = pb.nextBits();
            try {
                pb.diff(nxt);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            try {
                pb.diff(mod);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            PrivilegeBits before = PrivilegeBits.getInstance(mod);
            mod.diff(nxt);
            assertEquals(before, mod);
            mod.add(nxt);
            assertFalse(before.equals(mod));
            mod.diff(nxt);
            assertEquals(before, mod);
            mod.add(nxt);

            // diff with same pb must leave original bits empty
            PrivilegeBits tmp = PrivilegeBits.getInstance(pb);
            tmp.add(nxt);
            tmp.add(READ_PRIVILEGE_BITS);
            tmp.diff(tmp);
            assertEquals(nxt.toString(), PrivilegeBits.EMPTY, tmp);

            tmp = PrivilegeBits.getInstance(pb);
            tmp.add(nxt);
            tmp.add(READ_PRIVILEGE_BITS);
            tmp.diff(PrivilegeBits.getInstance(tmp));
            assertEquals(nxt.toString(), PrivilegeBits.EMPTY, tmp);

            // diff without intersection -> leave privilege unmodified.
            tmp = PrivilegeBits.getInstance(pb);
            tmp.diff(nxt);
            assertEquals(PrivilegeBits.getInstance(pb), tmp);

            // diff with intersection -> privilege must be modified accordingly.
            tmp = PrivilegeBits.getInstance(nxt);
            tmp.add(READ_PRIVILEGE_BITS);
            assertTrue(tmp.includes(READ_PRIVILEGE_BITS));
            assertTrue(tmp.includes(nxt));
            tmp.diff(nxt);
            assertEquals(READ_PRIVILEGE_BITS, tmp);
            assertTrue(tmp.includes(READ_PRIVILEGE_BITS));
            assertFalse(tmp.includes(nxt));

            tmp = PrivilegeBits.getInstance(pb);
            tmp.add(READ_PRIVILEGE_BITS);
            PrivilegeBits tmp2 = PrivilegeBits.getInstance(pb);
            tmp2.add(nxt);
            PrivilegeBits tmp3 = PrivilegeBits.getInstance(tmp2);
            assertEquals(tmp2, tmp3);
            tmp.diff(tmp2);
            if (READ_PRIVILEGE_BITS.equals(pb)) {
                assertEquals(PrivilegeBits.EMPTY, tmp);
            } else {
                assertEquals(READ_PRIVILEGE_BITS, tmp);               
            }
            // but pb passed to the diff call must not be modified.
            assertEquals(tmp3, tmp2);

            pb = nxt;
        }
    }
        
    public void testAddDifference() {
        // empty
        try {
            PrivilegeBits.EMPTY.addDifference(PrivilegeBits.EMPTY, PrivilegeBits.EMPTY);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }

        // other privilege bits
        PrivilegeBits pb = READ_PRIVILEGE_BITS;
        PrivilegeBits mod = PrivilegeBits.getInstance(pb);

        for (int i = 0; i < 100; i++) {
            PrivilegeBits nxt = pb.nextBits();
            try {
                pb.addDifference(nxt, mod);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            try {
                pb.addDifference(nxt, READ_PRIVILEGE_BITS);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }

            PrivilegeBits tmp = PrivilegeBits.getInstance(mod);
            tmp.addDifference(nxt, READ_PRIVILEGE_BITS);
            mod.add(nxt);
            assertEquals(mod, tmp); // since there is diff(nxt, read) which results in nxt

            if (!pb.equals(READ_PRIVILEGE_BITS)) {
                tmp = PrivilegeBits.getInstance(nxt);
                PrivilegeBits mod2 = PrivilegeBits.getInstance(mod);
                tmp.addDifference(mod2, READ_PRIVILEGE_BITS);
                assertFalse(nxt.equals(tmp));  // tmp should be modified by addDifference call.
                assertEquals(mod2, mod);       // mod2 should not be modified here
                assertTrue(tmp.includes(pb));
                assertFalse(tmp.includes(READ_PRIVILEGE_BITS));
                assertFalse(tmp.includes(mod));
            }

            tmp = PrivilegeBits.getInstance(nxt);
            PrivilegeBits mod2 = PrivilegeBits.getInstance(mod);
            tmp.addDifference(READ_PRIVILEGE_BITS, mod2);
            assertEquals(nxt, tmp);  // tmp not modified by addDifference call.
            assertEquals(mod2, mod); // mod2 should not be modified here
            assertFalse(tmp.includes(pb));
            assertFalse(tmp.includes(READ_PRIVILEGE_BITS));
            assertFalse(tmp.includes(mod));

            tmp = PrivilegeBits.getInstance(nxt);
            tmp.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
            assertEquals(nxt, tmp);  // tmp not modified by addDifference call.
            assertFalse(tmp.includes(READ_PRIVILEGE_BITS));

            tmp = PrivilegeBits.getInstance(mod);
            tmp.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
            assertEquals(mod, tmp);  // tmp not modified by addDifference call.
            assertTrue(tmp.includes(READ_PRIVILEGE_BITS));

            pb = nxt;
        }
    }

    //------------------------------------------------------------< general >---
    public void testGetInstance() {
        PrivilegeBits pb = PrivilegeBits.getInstance();
        assertEquals(PrivilegeBits.EMPTY, pb);
        assertNotSame(PrivilegeBits.EMPTY, pb);
        assertNotSame(pb, pb.unmodifiable());        
        pb.add(READ_PRIVILEGE_BITS);
        pb.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
        pb.diff(READ_PRIVILEGE_BITS);

        pb = PrivilegeBits.getInstance(PrivilegeBits.EMPTY);
        assertEquals(PrivilegeBits.EMPTY, pb);
        assertNotSame(PrivilegeBits.EMPTY, pb);
        assertNotSame(pb, pb.unmodifiable());
        pb.add(READ_PRIVILEGE_BITS);
        pb.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
        pb.diff(READ_PRIVILEGE_BITS);

        pb = PrivilegeBits.getInstance(READ_PRIVILEGE_BITS);
        assertEquals(READ_PRIVILEGE_BITS, pb);
        assertNotSame(READ_PRIVILEGE_BITS, pb);
        assertNotSame(pb, pb.unmodifiable());
        pb.add(READ_PRIVILEGE_BITS);
        pb.addDifference(READ_PRIVILEGE_BITS, PrivilegeBits.EMPTY);
        pb.diff(READ_PRIVILEGE_BITS);

        pb = PrivilegeBits.getInstance(PrivilegeRegistry.NO_PRIVILEGE);
        assertEquals(pb, PrivilegeBits.EMPTY);
        assertSame(pb, PrivilegeBits.EMPTY);
        assertSame(pb, pb.unmodifiable());
        try {
            pb.add(READ_PRIVILEGE_BITS);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }
        try {
            pb.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }
        try {
            pb.diff(READ_PRIVILEGE_BITS);
            fail("UnsupportedOperation expected");
        } catch (UnsupportedOperationException e) {
            // success
        }

        try {
            PrivilegeBits.getInstance(-1);
            fail();
        } catch (IllegalArgumentException e) {
            // success.
        }

        PrivilegeBits bts = PrivilegeBits.getInstance(PrivilegeRegistry.NO_PRIVILEGE);
        assertSame(PrivilegeBits.EMPTY, bts);
        
        for (long l : LONGS) {
            pb = PrivilegeBits.getInstance(l);
            assertEquals(pb, PrivilegeBits.getInstance(l));
            assertSame(pb, pb.unmodifiable());

            assertEquals(pb, PrivilegeBits.getInstance(pb));
            assertEquals(PrivilegeBits.getInstance(pb), pb);
            assertNotSame(pb, PrivilegeBits.getInstance(pb));

            try {
                pb.add(READ_PRIVILEGE_BITS);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }
            try {
                pb.addDifference(READ_PRIVILEGE_BITS, READ_PRIVILEGE_BITS);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }
            try {
                pb.diff(READ_PRIVILEGE_BITS);
                fail("UnsupportedOperation expected");
            } catch (UnsupportedOperationException e) {
                // success
            }
        }

    }
}