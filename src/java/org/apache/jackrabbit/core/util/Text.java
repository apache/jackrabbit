/*
 * Copyright 2004 The Apache Software Foundation.
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
package org.apache.jackrabbit.core.util;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;

/**
 * This Class provides some text related utilities
 */
public class Text {

    /**
     * used for the md5
     */
    public final static char[] hexTable = "0123456789abcdef".toCharArray();

    /**
     * Calculate an MD5 hash of the string given.
     *
     * @param data the data to encode
     * @param enc  the character encoding to use
     * @return a hex encoded string of the md5 digested input
     */
    public static String md5(String data, String enc)
            throws UnsupportedEncodingException {
        try {
            return digest("MD5", data.getBytes(enc));
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 digest not available???");
        }
    }

    /**
     * Calculate an MD5 hash of the string given using 'utf-8' encoding.
     *
     * @param data the data to encode
     * @return a hex encoded string of the md5 digested input
     */
    public static String md5(String data) {
        try {
            return md5(data, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new InternalError("UTF8 digest not available???");
        }
    }

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      The plain text String to be digested.
     * @param enc       The character encoding to use
     * @return The digested plain text String represented as Hex digits.
     * @throws NoSuchAlgorithmException     if the desired algorithm is not supported by
     *                                      the MessageDigest class.
     * @throws UnsupportedEncodingException if the encoding is not supported
     */
    public static String digest(String algorithm, String data, String enc)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        return digest(algorithm, data.getBytes(enc));
    }

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      the data to digest with the given algorithm
     * @return The digested plain text String represented as Hex digits.
     * @throws NoSuchAlgorithmException if the desired algorithm is not supported by
     *                                  the MessageDigest class.
     */
    public static String digest(String algorithm, byte[] data)
            throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] digest = md.digest(data);
        StringBuffer res = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            res.append(hexTable[(b >> 4) & 15]);
            res.append(hexTable[b & 15]);
        }
        return res.toString();
    }

}
