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

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.Random;
import java.util.Arrays;

/**
 * Test cases for Base64 encode / decode.
 */
public class Base64Test extends TestCase {

    private Random _random = new Random();

    /**
     * @return Returns the _random.
     */
    protected Random getRandom() {
        return this._random;
    }

    /**
     * Tests that whitespace characters are ignored within base64 data.
     */
    public void testWhitespace() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Base64.decode(" d G\tV \tzdA\n= =\n", buffer);
        byte[] data = buffer.toByteArray();
        assertEquals("test", new String(data, "US-ASCII"));
    }

    /**
     * Tests that base 64 encoding/decoding round trips are lossless.
     */
    public void testBase64() throws Exception {
        base64RoundTrip(new byte[0]);

        for (int i = 0; i < 10000; i++) {
            base64RoundTrip();
        }
    }

    public void testBase64Streaming() throws Exception {
        byte[] data = new byte[0x100000];
        getRandom().nextBytes(data);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64.encode(new ByteArrayInputStream(data), baos);
        byte[] encData = baos.toByteArray();

        baos = new ByteArrayOutputStream();
        Base64.decode(new ByteArrayInputStream(encData), baos);
        byte[] decData = baos.toByteArray();
        assertTrue(Arrays.equals(data, decData));       
    }

    private void base64RoundTrip() throws Exception {
        int len = getRandom().nextInt(0x1000);
        byte[] b1 = new byte[len];
        getRandom().nextBytes(b1);

        base64RoundTrip(b1);
    }

    private void base64RoundTrip(byte[] ba) throws Exception {
        StringWriter sw = new StringWriter();
        Base64.encode(ba, 0, ba.length, sw);
        String s = sw.toString();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64.decode(s, baos);
        byte[] ba2 = baos.toByteArray();
        assertNotNull(ba2);
        assertEquals(ba.length, ba2.length);

        for (int i = 0; i < ba.length; i++) {
            assertEquals(ba[i], ba2[i]);
        }
    }

    public void testDecodeOrEncode() throws IOException {
        assertEquals("", Base64.decodeOrEncode(Base64.decodeOrEncode("")));
        assertEquals("test", Base64.decodeOrEncode(Base64.decodeOrEncode("test")));
        assertEquals("{base64}dGVzdA==", Base64.decodeOrEncode("test"));
        assertEquals("test", Base64.decodeOrEncode("{base64}dGVzdA=="));
    }

    public void testDecodeIfEncoded() throws IOException {
        assertEquals(null, Base64.decodeIfEncoded(null));
        assertEquals("", Base64.decodeIfEncoded(""));
        assertEquals("", Base64.decodeIfEncoded("{base64}"));
        assertEquals("test", Base64.decodeIfEncoded("test"));
        assertEquals("test", Base64.decodeIfEncoded("{base64}dGVzdA=="));
    }

    public void testStringEncodeDecode() throws IOException {
        assertEquals("", Base64.decode(Base64.encode("")));
        assertEquals("test", Base64.decode(Base64.encode("test")));
        assertEquals("dGVzdA==", Base64.encode("test"));
        assertEquals("test", Base64.decode("dGVzdA=="));
    }

}