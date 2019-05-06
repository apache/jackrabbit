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
package org.apache.jackrabbit.core.query.lucene;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.apache.jackrabbit.test.JUnitTest;

/**
 * Tests converting BigDecimal to String and back.
 */
public class DecimalConvertTest extends JUnitTest {

    public void testCommon() {
        // System.out.println(DecimalField.decimalToString(new BigDecimal(0)));
        // System.out.println(DecimalField.decimalToString(new BigDecimal(2)));
        // System.out.println(DecimalField.decimalToString(new BigDecimal(120)));
        // System.out.println(DecimalField.decimalToString(new BigDecimal(-1)));

        ArrayList<BigDecimal> list = new ArrayList<BigDecimal>();
        list.add(BigDecimal.ZERO);
        list.add(BigDecimal.ONE);
        list.add(BigDecimal.TEN);
        list.add(BigDecimal.ONE.scaleByPowerOfTen(1));
        list.add(new BigDecimal("100"));
        list.add(new BigDecimal("1000"));
        list.add(new BigDecimal("0.1"));
        list.add(new BigDecimal("0.01"));
        list.add(new BigDecimal("0.001"));
        list.add(new BigDecimal("1.1"));
        list.add(new BigDecimal("0.09"));
        list.add(new BigDecimal("9.9"));
        list.add(new BigDecimal("9.99"));
        list.add(new BigDecimal("99"));
        list.add(new BigDecimal("99.0"));
        list.add(new BigDecimal("101"));
        list.add(new BigDecimal("1000.0"));
        list.add(new BigDecimal("-1.23E-10"));
        testWithList(list);
    }

    public void testMinimumScale() {
        if (true) { // JCR-3834
            return;
        }
        final BigDecimal d1 = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE);
        final BigDecimal d2 = new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE+1);
        final String s1 = DecimalField.decimalToString(d1);
        final String s2 = DecimalField.decimalToString(d2);
        assertEquals(Math.signum(d1.compareTo(d2)), Math.signum(s1.compareTo(s2)));
    }

    public void testRandomized() {
        if (true) { // JCR-3834
            return;
        }
        ArrayList<BigDecimal> list = new ArrayList<BigDecimal>();
        list.add(BigDecimal.ZERO);
        list.add(BigDecimal.ONE);
        list.add(BigDecimal.TEN);
        list.add(new BigDecimal(BigInteger.ONE, Integer.MAX_VALUE));
        list.add(new BigDecimal(BigInteger.ONE, Integer.MIN_VALUE));
        Random random = new Random(1);
        // a few regular values
        for (int i = 0; i < 10000; i++) {
            list.add(new BigDecimal(i));
        }
        for (int i = 0; i < 100; i++) {
            list.add(new BigDecimal(random.nextDouble()));
        }
        // scale -10 .. 10
        for (int i = 0; i < 1000; i++) {
            int scale = random.nextInt(20) - 10;
            BigInteger value = BigInteger.valueOf(random.nextLong());
            list.add(new BigDecimal(value, scale));
        }
        // extremely small and large values
        for (int i = 0; i < 100; i++) {
            int scale = random.nextInt(2000) - 1000;
            BigInteger value = new BigInteger(1000, random);
            list.add(new BigDecimal(value, scale));
        }
        testWithList(list);
    }

    private void testWithList(ArrayList<BigDecimal> list) {
        // add negative values
        for (BigDecimal d : new ArrayList<BigDecimal>(list)) {
            list.add(d.negate());
        }
        Collections.sort(list);
        BigDecimal lastDecimal = null;
        String lastString = null;
        for (BigDecimal d : list) {
            String s = DecimalField.decimalToString(d);
            if (lastDecimal != null) {
                int compDecimal = (int) Math.signum(lastDecimal.compareTo(d));
                int compString = (int) Math.signum(lastString.compareTo(s));
                if (compDecimal != compString) {
                    assertEquals(compDecimal, compString);
                }
            }
            BigDecimal test = DecimalField.stringToDecimal(s);
            if (test.compareTo(d) != 0) {
                assertEquals(d + "<>" + test.toPlainString(), 0, test.compareTo(d));
            }
            lastDecimal = d;
            lastString = s;
        }
    }
    
}
