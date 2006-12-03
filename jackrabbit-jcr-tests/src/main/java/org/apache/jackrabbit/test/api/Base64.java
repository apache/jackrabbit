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
package org.apache.jackrabbit.test.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * <code>Base64</code> provides Base64 encoding/decoding of strings and streams.
 * NOTE: This is a copy of the original org.apache.jackrabbit.core.util.Base64
 * class to not introduce a dependency to jackrabbit from the test classes.
 */
public class Base64 {
    // charset used for base64 encoded data (7-bit ASCII)
    private static final String CHARSET = "US-ASCII";

    // encoding table (the 64 valid base64 characters)
    private static final char[] BASE64CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    // decoding table (used to lookup original 6-bit with base64 character as table index)
    private static final byte[] DECODETABLE = new byte[128];

    static {
        // initialize decoding table
        for (int i = 0; i < DECODETABLE.length; i++) {
            DECODETABLE[i] = 0x7f;
        }
        // build decoding table
        for (int i = 0; i < BASE64CHARS.length; i++) {
            DECODETABLE[BASE64CHARS[i]] = (byte) i;
        }
    }

    // pad character
    private static final char BASE64PAD = '=';

    /**
     * empty private constructor
     */
    private Base64() {
    }

    /**
     * Calculates the size (i.e. number of bytes) of the base64 encoded output
     * given the length (i.e. number of bytes) of the data to be encoded.
     *
     * @param dataLength length (i.e. number of bytes) of the data to be encoded
     * @return size (i.e. number of bytes) of the base64 encoded output
     */
    public static long calcEncodedLength(long dataLength) {
        long encLen = dataLength * 4 / 3;
        encLen += (encLen + 4) % 4;
        return encLen;
    }

    /**
     * Pessimistically guesses the size (i.e. number of bytes) of the decoded output
     * given the length (i.e. number of bytes) of the base64 encoded data.
     *
     * @param encLength length (i.e. number of bytes) of the base64 encoded data
     * @return size (i.e. number of bytes) of the decoded output
     */
    public static long guessDecodedLength(long encLength) {
        long decLen = encLength * 3 / 4;
        return decLen + 3;
    }

    /**
     * Outputs base64 representation of the specified stream data to an <code>OutputStream</code>.
     *
     * @param in     stream data to be encoded
     * @param writer writer to output the encoded data
     */
    public static void encode(InputStream in, Writer writer)
            throws IOException {
        // encode stream data in chunks;
        // chunksize must be a multiple of 3 in order
        // to avoid padding within output
        byte[] buffer = new byte[9 * 1024];
        int read = 0;
        while ((read = in.read(buffer)) > 0) {
            encode(buffer, 0, read, writer);
        }
    }

    /**
     * Outputs base64 representation of the specified stream data to an <code>OutputStream</code>.
     *
     * @param in  stream data to be encoded
     * @param out stream where the encoded data should be written to
     */
    public static void encode(InputStream in, OutputStream out)
            throws IOException {
        Writer writer = new OutputStreamWriter(out, CHARSET);
        encode(in, writer);
    }

    /**
     * Outputs base64 representation of the specified data to an <code>OutputStream</code>.
     *
     * @param data   data to be encoded
     * @param off    offset within data at which to start encoding
     * @param len    length of data to encode
     * @param writer writer to output the encoded data
     */
    public static void encode(byte[] data, int off, int len, Writer writer)
            throws IOException {
        if (len == 0) {
            return;
        }
        if (len < 0 || off >= data.length
                || len + off > data.length) {
            throw new IllegalArgumentException();
        }
        char[] enc = new char[4];
        while (len >= 3) {
            int i = ((data[off] & 0xff) << 16)
                    + ((data[off + 1] & 0xff) << 8)
                    + (data[off + 2] & 0xff);
            enc[0] = BASE64CHARS[i >> 18];
            enc[1] = BASE64CHARS[(i >> 12) & 0x3f];
            enc[2] = BASE64CHARS[(i >> 6) & 0x3f];
            enc[3] = BASE64CHARS[i & 0x3f];
            writer.write(enc, 0, 4);
            off += 3;
            len -= 3;
        }
        // add padding if necessary
        if (len == 1) {
            int i = data[off] & 0xff;
            enc[0] = BASE64CHARS[i >> 2];
            enc[1] = BASE64CHARS[(i << 4) & 0x3f];
            enc[2] = BASE64PAD;
            enc[3] = BASE64PAD;
            writer.write(enc, 0, 4);
        } else if (len == 2) {
            int i = ((data[off] & 0xff) << 8) + (data[off + 1] & 0xff);
            enc[0] = BASE64CHARS[i >> 10];
            enc[1] = BASE64CHARS[(i >> 4) & 0x3f];
            enc[2] = BASE64CHARS[(i << 2) & 0x3f];
            enc[3] = BASE64PAD;
            writer.write(enc, 0, 4);
        }
    }

    /**
     * Decode base64 encoded data.
     *
     * @param reader reader for the base64 encoded data to be decoded
     * @param out    stream where the decoded data should be written to
     */
    public static void decode(Reader reader, OutputStream out) throws IOException {
        char[] chunk = new char[8192];
        int read;
        while ((read = reader.read(chunk)) > -1) {
            decode(chunk, 0, read, out);
        }
    }

    /**
     * Decode base64 encoded data. The data read from the inputstream is
     * assumed to be of charset "US-ASCII".
     *
     * @param in  inputstream of the base64 encoded data to be decoded
     * @param out stream where the decoded data should be written to
     */
    public static void decode(InputStream in, OutputStream out) throws IOException {
        decode(new InputStreamReader(in, CHARSET), out);
    }

    /**
     * Decode base64 encoded data.
     *
     * @param data the base64 encoded data to be decoded
     * @param out  stream where the decoded data should be written to
     */
    public static void decode(String data, OutputStream out) throws IOException {
        char[] chars = data.toCharArray();
        decode(chars, 0, chars.length, out);
    }

    /**
     * Decode base64 encoded data.
     *
     * @param chars the base64 encoded data to be decoded
     * @param out   stream where the decoded data should be written to
     */
    public static void decode(char[] chars, OutputStream out) throws IOException {
        decode(chars, 0, chars.length, out);
    }

    /**
     * Decode base64 encoded data.
     *
     * @param chars the base64 encoded data to be decoded
     * @param off   offset within data at which to start decoding
     * @param len   length of data to decode
     * @param out   stream where the decoded data should be written to
     */
    public static void decode(char[] chars, int off, int len, OutputStream out) throws IOException {
        if (len == 0) {
            return;
        }
        if (len < 0 || off >= chars.length
                || len + off > chars.length) {
            throw new IllegalArgumentException();
        }
        char[] chunk = new char[4];
        byte[] dec = new byte[3];
        int posChunk = 0;
        // decode in chunks of 4 characters
        for (int i = off; i < (off + len); i++) {
            char c = chars[i];
            if (c < DECODETABLE.length && DECODETABLE[c] != 0x7f
                    || c == BASE64PAD) {
                chunk[posChunk++] = c;
                if (posChunk == chunk.length) {
                    int b0 = DECODETABLE[chunk[0]];
                    int b1 = DECODETABLE[chunk[1]];
                    int b2 = DECODETABLE[chunk[2]];
                    int b3 = DECODETABLE[chunk[3]];
                    if (chunk[3] == BASE64PAD && chunk[2] == BASE64PAD) {
                        dec[0] = (byte) (b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                        out.write(dec, 0, 1);
                    } else if (chunk[3] == BASE64PAD) {
                        dec[0] = (byte) (b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                        dec[1] = (byte) (b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                        out.write(dec, 0, 2);
                    } else {
                        dec[0] = (byte) (b0 << 2 & 0xfc | b1 >> 4 & 0x3);
                        dec[1] = (byte) (b1 << 4 & 0xf0 | b2 >> 2 & 0xf);
                        dec[2] = (byte) (b2 << 6 & 0xc0 | b3 & 0x3f);
                        out.write(dec, 0, 3);
                    }
                    posChunk = 0;
                }
            } else {
                throw new IllegalArgumentException("specified data is not base64 encoded");
            }
        }
    }
}
