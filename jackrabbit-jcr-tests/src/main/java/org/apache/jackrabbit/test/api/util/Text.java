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
package org.apache.jackrabbit.test.api.util;

// JCR-714: Class copied from jcr-commons to avoid the extra dependency

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;

/**
 * This Class provides some text related utilities
 */
public class Text {

    /**
     * Hidden constructor.
     */
    private Text() {
    }

    /**
     * used for the md5
     */
    public static final char[] hexTable = "0123456789abcdef".toCharArray();

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
     * @throws java.security.NoSuchAlgorithmException     if the desired algorithm is not supported by
     *                                      the MessageDigest class.
     * @throws java.io.UnsupportedEncodingException if the encoding is not supported
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
     * @throws java.security.NoSuchAlgorithmException if the desired algorithm is not supported by
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

    /**
     * returns an array of strings decomposed of the original string, split at
     * every occurance of 'ch'. if 2 'ch' follow each other with no intermediate
     * characters, empty "" entries are avoided.
     *
     * @param str the string to decompose
     * @param ch  the character to use a split pattern
     * @return an array of strings
     */
    public static String[] explode(String str, int ch) {
        return explode(str, ch, false);
    }

    /**
     * returns an array of strings decomposed of the original string, split at
     * every occurance of 'ch'.
     *
     * @param str          the string to decompose
     * @param ch           the character to use a split pattern
     * @param respectEmpty if <code>true</code>, empty elements are generated
     * @return an array of strings
     */
    public static String[] explode(String str, int ch, boolean respectEmpty) {
        if (str == null || str.length() == 0) {
            return new String[0];
        }

        List<String> strings = new ArrayList<String>();
        int pos;
        int lastpos = 0;

        // add snipples
        while ((pos = str.indexOf(ch, lastpos)) >= 0) {
            if (pos - lastpos > 0 || respectEmpty) {
                strings.add(str.substring(lastpos, pos));
            }
            lastpos = pos + 1;
        }
        // add rest
        if (lastpos < str.length()) {
            strings.add(str.substring(lastpos));
        } else if (respectEmpty && lastpos == str.length()) {
            strings.add("");
        }

        // return stringarray
        return strings.toArray(new String[strings.size()]);
    }

    /**
     * Concatenates all strings in the string array using the specified delimiter.
     * @param arr
     * @param delim
     * @return the concatenated string
     */
    public static String implode(String[] arr, String delim) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) {
                buf.append(delim);
            }
            buf.append(arr[i]);
        }
        return buf.toString();
    }

    /**
     * Replaces all occurences of <code>oldString</code> in <code>text</code>
     * with <code>newString</code>.
     *
     * @param text
     * @param oldString old substring to be replaced with <code>newString</code>
     * @param newString new substring to replace occurences of <code>oldString</code>
     * @return a string
     */
    public static String replace(String text, String oldString, String newString) {
        if (text == null || oldString == null || newString == null) {
            throw new IllegalArgumentException("null argument");
        }
        int pos = text.indexOf(oldString);
        if (pos == -1) {
            return text;
        }
        int lastPos = 0;
        StringBuffer sb = new StringBuffer(text.length());
        while (pos != -1) {
            sb.append(text.substring(lastPos, pos));
            sb.append(newString);
            lastPos = pos + oldString.length();
            pos = text.indexOf(oldString, lastPos);
        }
        if (lastPos < text.length()) {
            sb.append(text.substring(lastPos));
        }
        return sb.toString();
    }

    /**
     * Replaces illegal XML characters in the given string by their corresponding
     * predefined entity references.
     *
     * @param text text to be escaped
     * @return a string
     */
    public static String encodeIllegalXMLCharacters(String text) {
        if (text == null) {
            throw new IllegalArgumentException("null argument");
        }
        StringBuffer buf = null;
        int length = text.length();
        int pos = 0;
        for (int i = 0; i < length; i++) {
            int ch = text.charAt(i);
            switch (ch) {
                case '<':
                case '>':
                case '&':
                case '"':
                case '\'':
                    if (buf == null) {
                        buf = new StringBuffer();
                    }
                    if (i > 0) {
                        buf.append(text.substring(pos, i));
                    }
                    pos = i + 1;
                    break;
                default:
                    continue;
            }
            if (ch == '<') {
                buf.append("&lt;");
            } else if (ch == '>') {
                buf.append("&gt;");
            } else if (ch == '&') {
                buf.append("&amp;");
            } else if (ch == '"') {
                buf.append("&quot;");
            } else if (ch == '\'') {
                buf.append("&apos;");
            }
        }
        if (buf == null) {
            return text;
        } else {
            if (pos < length) {
                buf.append(text.substring(pos));
            }
            return buf.toString();
        }
    }

    /**
     * The list of characters that are not encoded by the <code>escape()</code>
     * and <code>unescape()</code> METHODS. They contains the characters as
     * defined 'unreserved' in section 2.3 of the RFC 2396 'URI generic syntax':
     * <p>
     * <pre>
     * unreserved  = alphanum | mark
     * mark        = "-" | "_" | "." | "!" | "~" | "*" | "'" | "(" | ")"
     * </pre>
     */
    public static BitSet URISave;

    /**
     * Same as {@link #URISave} but also contains the '/'
     */
    public static BitSet URISaveEx;

    static {
        URISave = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            URISave.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            URISave.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            URISave.set(i);
        }
        URISave.set('-');
        URISave.set('_');
        URISave.set('.');
        URISave.set('!');
        URISave.set('~');
        URISave.set('*');
        URISave.set('\'');
        URISave.set('(');
        URISave.set(')');

        URISaveEx = (BitSet) URISave.clone();
        URISaveEx.set('/');
    }

    /**
     * Does an URL encoding of the <code>string</code> using the
     * <code>escape</code> character. The characters that don't need encoding
     * are those defined 'unreserved' in section 2.3 of the 'URI generic syntax'
     * RFC 2396, but without the escape character.
     *
     * @param string the string to encode.
     * @param escape the escape character.
     * @return the escaped string
     * @throws NullPointerException if <code>string</code> is <code>null</code>.
     */
    public static String escape(String string, char escape) {
        return escape(string, escape, false);
    }

    /**
     * Does an URL encoding of the <code>string</code> using the
     * <code>escape</code> character. The characters that don't need encoding
     * are those defined 'unreserved' in section 2.3 of the 'URI generic syntax'
     * RFC 2396, but without the escape character. If <code>isPath</code> is
     * <code>true</code>, additionally the slash '/' is ignored, too.
     *
     * @param string the string to encode.
     * @param escape the escape character.
     * @param isPath if <code>true</code>, the string is treated as path
     * @return the escaped string
     * @throws NullPointerException if <code>string</code> is <code>null</code>.
     */
    public static String escape(String string, char escape, boolean isPath) {
        try {
            BitSet validChars = isPath ? URISaveEx : URISave;
            byte[] bytes = string.getBytes("utf-8");
            StringBuffer out = new StringBuffer(bytes.length);
            for (int i = 0; i < bytes.length; i++) {
                int c = bytes[i] & 0xff;
                if (validChars.get(c) && c != escape) {
                    out.append((char) c);
                } else {
                    out.append(escape);
                    out.append(hexTable[(c >> 4) & 0x0f]);
                    out.append(hexTable[(c) & 0x0f]);
                }
            }
            return out.toString();
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Does a URL encoding of the <code>string</code>. The characters that
     * don't need encoding are those defined 'unreserved' in section 2.3 of
     * the 'URI generic syntax' RFC 2396.
     *
     * @param string the string to encode
     * @return the escaped string
     * @throws NullPointerException if <code>string</code> is <code>null</code>.
     */
    public static String escape(String string) {
        return escape(string, '%');
    }

    /**
     * Does a URL encoding of the <code>path</code>. The characters that
     * don't need encoding are those defined 'unreserved' in section 2.3 of
     * the 'URI generic syntax' RFC 2396. In contrast to the
     * {@link #escape(String)} method, not the entire path string is escaped,
     * but every individual part (i.e. the slashes are not escaped).
     *
     * @param path the path to encode
     * @return the escaped path
     * @throws NullPointerException if <code>path</code> is <code>null</code>.
     */
    public static String escapePath(String path) {
        return escape(path, '%', true);
    }

    /**
     * Does a URL decoding of the <code>string</code> using the
     * <code>escape</code> character. Please note that in opposite to the
     * {@link java.net.URLDecoder} it does not transform the + into spaces.
     *
     * @param string the string to decode
     * @param escape the escape character
     * @return the decoded string
     * @throws NullPointerException           if <code>string</code> is <code>null</code>.
     * @throws IllegalArgumentException       if the 2 characters following the escape
     *                                        character do not represent a hex-number
     *                                        or if not enough characters follow an
     *                                        escape character
     */
    public static String unescape(String string, char escape)  {
        try {
            byte[] utf8 = string.getBytes("utf-8");

            // Check whether escape occurs at invalid position
            if ((utf8.length >= 1 && utf8[utf8.length - 1] == escape) ||
                (utf8.length >= 2 && utf8[utf8.length - 2] == escape)) {
                throw new IllegalArgumentException("Premature end of escape sequence at end of input");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream(utf8.length);
            for (int k = 0; k < utf8.length; k++) {
                byte b = utf8[k];
                if (b == escape) {
                    out.write((decodeDigit(utf8[++k]) << 4) + decodeDigit(utf8[++k]));
                }
                else {
                    out.write(b);
                }
            }

            return new String(out.toByteArray(), "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new InternalError(e.toString());
        }
    }

    /**
     * Does a URL decoding of the <code>string</code>. Please note that in
     * opposite to the {@link java.net.URLDecoder} it does not transform the +
     * into spaces.
     *
     * @param string the string to decode
     * @return the decoded string
     * @throws NullPointerException           if <code>string</code> is <code>null</code>.
     * @throws ArrayIndexOutOfBoundsException if not enough character follow an
     *                                        escape character
     * @throws IllegalArgumentException       if the 2 characters following the escape
     *                                        character do not represent a hex-number.
     */
    public static String unescape(String string) {
        return unescape(string, '%');
    }

    /**
     * Escapes all illegal JCR name characters of a string.
     * The encoding is loosely modeled after URI encoding, but only encodes
     * the characters it absolutely needs to in order to make the resulting
     * string a valid JCR name.
     * Use {@link #unescapeIllegalJcrChars(String)} for decoding.
     * <p>
     * QName EBNF:
     * <pre>
     * simplename ::= onecharsimplename | twocharsimplename | threeormorecharname
     * onecharsimplename ::= (* Any Unicode character except: '.', '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * twocharsimplename ::= '.' onecharsimplename | onecharsimplename '.' | onecharsimplename onecharsimplename
     * threeormorecharname ::= nonspace string nonspace
     * string ::= char | string char
     * char ::= nonspace | ' '
     * nonspace ::= (* Any Unicode character except: '/', ':', '[', ']', '*', ''', '"', '|' or any whitespace character *)
     * </pre>
     *
     * @param name the name to escape
     * @return the escaped name
     */
    public static String escapeIllegalJcrChars(String name) {
        StringBuffer buffer = new StringBuffer(name.length() * 2);
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (ch == '%' || ch == '/' || ch == ':' || ch == '[' || ch == ']'
                || ch == '*' || ch == '\'' || ch == '"' || ch == '|'
                || (ch == '.' && name.length() < 3)
                || (ch == ' ' && (i == 0 || i == name.length() - 1))
                || ch == '\t' || ch == '\r' || ch == '\n') {
                buffer.append('%');
                buffer.append(Character.toUpperCase(Character.forDigit(ch / 16, 16)));
                buffer.append(Character.toUpperCase(Character.forDigit(ch % 16, 16)));
            } else {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    /** Escapes all illegal XPath search characters of a string.
     * <p>Example:<br>
     * A search string like 'test?' will run into a ParseException
     * documented in http://issues.apache.org/jira/browse/JCR-1248
     *
     * @param s the string to encode
     * @return the escaped string
     */
    public static String escapeIllegalXpathSearchChars(String s) {
        StringBuffer sb = new StringBuffer();
        sb.append(s.substring(0, (s.length() - 1)));
        char c = s.charAt(s.length() - 1);
        // NOTE: keep this in sync with _ESCAPED_CHAR below!
        if (c == '!' || c == '(' || c == ':' || c == '^'
            || c == '[' || c == ']' || c == '\"' || c == '{'
            || c == '}' || c == '?') {
            sb.append('\\');
        }
        sb.append(c);
        return sb.toString();
    }

    /**
     * Unescapes previously escaped jcr chars.
     * <p>
     * Please note, that this does not exactly the same as the url related
     * {@link #unescape(String)}, since it handles the byte-encoding
     * differently.
     *
     * @param name the name to unescape
     * @return the unescaped name
     */
    public static String unescapeIllegalJcrChars(String name) {
        StringBuffer buffer = new StringBuffer(name.length());
        int i = name.indexOf('%');
        while (i > -1 && i + 2 < name.length()) {
            buffer.append(name.toCharArray(), 0, i);
            int a = Character.digit(name.charAt(i + 1), 16);
            int b = Character.digit(name.charAt(i + 2), 16);
            if (a > -1 && b > -1) {
                buffer.append((char) (a * 16 + b));
                name = name.substring(i + 3);
            } else {
                buffer.append('%');
                name = name.substring(i + 1);
            }
            i = name.indexOf('%');
        }
        buffer.append(name);
        return buffer.toString();
    }

    /**
     * Returns the name part of the path. If the given path is already a name
     * (i.e. contains no slashes) it is returned.
     *
     * @param path the path
     * @return the name part or <code>null</code> if <code>path</code> is <code>null</code>.
     */
    public static String getName(String path) {
        return getName(path, '/');
    }

    /**
     * Returns the name part of the path, delimited by the given <code>delim</code>.
     * If the given path is already a name (i.e. contains no <code>delim</code>
     * characters) it is returned.
     *
     * @param path the path
     * @param delim the delimiter
     * @return the name part or <code>null</code> if <code>path</code> is <code>null</code>.
     */
    public static String getName(String path, char delim) {
        return path == null
                ? null
                : path.substring(path.lastIndexOf(delim) + 1);
    }

    /**
     * Same as {@link #getName(String)} but adding the possibility
     * to pass paths that end with a trailing '/'
     *
     * @see #getName(String)
     */
    public static String getName(String path, boolean ignoreTrailingSlash) {
        if (ignoreTrailingSlash && path != null && path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length()-1);
        }
        return getName(path);
    }

    /**
     * Returns the namespace prefix of the given <code>qname</code>. If the
     * prefix is missing, an empty string is returned. Please note, that this
     * method does not validate the name or prefix.
     * <p>
     * The qname has the format: qname := [prefix ':'] local;
     *
     * @param qname a qualified name
     * @return the prefix of the name or "".
     *
     * @see #getLocalName(String)
     *
     * @throws NullPointerException if <code>qname</code> is <code>null</code>
     */
    public static String getNamespacePrefix(String qname) {
        int pos = qname.indexOf(':');
        return pos >=0 ? qname.substring(0, pos) : "";
    }

    /**
     * Returns the local name of the given <code>qname</code>. Please note, that
     * this method does not validate the name.
     * <p>
     * The qname has the format: qname := [prefix ':'] local;
     *
     * @param qname a qualified name
     * @return the localname
     *
     * @see #getNamespacePrefix(String)
     *
     * @throws NullPointerException if <code>qname</code> is <code>null</code>
     */
    public static String getLocalName(String qname) {
        int pos = qname.indexOf(':');
        return pos >=0 ? qname.substring(pos+1) : qname;
    }

    /**
     * Determines, if two paths denote hierarchical siblins.
     *
     * @param p1 first path
     * @param p2 second path
     * @return true if on same level, false otherwise
     */
    public static boolean isSibling(String p1, String p2) {
        int pos1 = p1.lastIndexOf('/');
        int pos2 = p2.lastIndexOf('/');
        return (pos1 == pos2 && pos1 >= 0 && p1.regionMatches(0, p2, 0, pos1));
    }

    /**
     * Determines if the <code>descendant</code> path is hierarchical a
     * descendant of <code>path</code>.
     *
     * @param path     the current path
     * @param descendant the potential descendant
     * @return <code>true</code> if the <code>descendant</code> is a descendant;
     *         <code>false</code> otherwise.
     */
    public static boolean isDescendant(String path, String descendant) {
        String pattern = path.endsWith("/") ? path : path + "/";
        return !pattern.equals(descendant) &&
                descendant.startsWith(pattern);
    }

    /**
     * Determines if the <code>descendant</code> path is hierarchical a
     * descendant of <code>path</code> or equal to it.
     *
     * @param path       the path to check
     * @param descendant the potential descendant
     * @return <code>true</code> if the <code>descendant</code> is a descendant
     *         or equal; <code>false</code> otherwise.
     */
    public static boolean isDescendantOrEqual(String path, String descendant) {
        if (path.equals(descendant)) {
            return true;
        } else {
            String pattern = path.endsWith("/") ? path : path + "/";
            return descendant.startsWith(pattern);
        }
    }

    /**
     * Returns the n<sup>th</sup> relative parent of the path, where n=level.
     * <p>Example:<br>
     * <code>
     * Text.getRelativeParent("/foo/bar/test", 1) == "/foo/bar"
     * </code>
     *
     * @param path the path of the page
     * @param level  the level of the parent
     */
    public static String getRelativeParent(String path, int level) {
        int idx = path.length();
        while (level > 0) {
            idx = path.lastIndexOf('/', idx - 1);
            if (idx < 0) {
                return "";
            }
            level--;
        }
        return (idx == 0) ? "/" : path.substring(0, idx);
    }

    /**
     * Same as {@link #getRelativeParent(String, int)} but adding the possibility
     * to pass paths that end with a trailing '/'
     *
     * @see #getRelativeParent(String, int)
     */
    public static String getRelativeParent(String path, int level, boolean ignoreTrailingSlash) {
        if (ignoreTrailingSlash && path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length()-1);
        }
        return getRelativeParent(path, level);
    }

    /**
     * Returns the n<sup>th</sup> absolute parent of the path, where n=level.
     * <p>Example:<br>
     * <code>
     * Text.getAbsoluteParent("/foo/bar/test", 1) == "/foo/bar"
     * </code>
     *
     * @param path the path of the page
     * @param level  the level of the parent
     */
    public static String getAbsoluteParent(String path, int level) {
        int idx = 0;
        int len = path.length();
        while (level >= 0 && idx < len) {
            idx = path.indexOf('/', idx + 1);
            if (idx < 0) {
                idx = len;
            }
            level--;
        }
        return level >= 0 ? "" : path.substring(0, idx);
    }

    /**
     * Performs variable replacement on the given string value.
     * Each <code>${...}</code> sequence within the given value is replaced
     * with the value of the named parser variable. If a variable is not found
     * in the properties an IllegalArgumentException is thrown unless
     * <code>ignoreMissing</code> is <code>true</code>. In the later case, the
     * missing variable is replaced by the empty string.
     *
     * @param value         the original value
     * @param ignoreMissing if <code>true</code>, missing variables are replaced
     *                      by the empty string.
     * @return value after variable replacements
     * @throws IllegalArgumentException if the replacement of a referenced
     *                                  variable is not found
     */
    public static String replaceVariables(Properties variables, String value,
                                          boolean ignoreMissing)
            throws IllegalArgumentException {
        StringBuffer result = new StringBuffer();

        // Value:
        // +--+-+--------+-+-----------------+
        // |  |p|-->     |q|-->              |
        // +--+-+--------+-+-----------------+
        int p = 0, q = value.indexOf("${");                // Find first ${
        while (q != -1) {
            result.append(value.substring(p, q));          // Text before ${
            p = q;
            q = value.indexOf("}", q + 2);                 // Find }
            if (q != -1) {
                String variable = value.substring(p + 2, q);
                String replacement = variables.getProperty(variable);
                if (replacement == null) {
                    if (ignoreMissing) {
                        replacement = "";
                    } else {
                        throw new IllegalArgumentException(
                                "Replacement not found for ${" + variable + "}.");
                    }
                }
                result.append(replacement);
                p = q + 1;
                q = value.indexOf("${", p);                // Find next ${
            }
        }
        result.append(value.substring(p, value.length())); // Trailing text

        return result.toString();
    }

    private static byte decodeDigit(byte b) {
        if (b >= 0x30 && b <= 0x39) {
            return (byte) (b - 0x30);
        }
        else if (b >= 0x41 && b <= 0x46) {
            return (byte) (b - 0x37);
        }
        else if (b >= 0x61 && b <= 0x66) {
            return (byte) (b - 0x57);
        }
        else {
            throw new IllegalArgumentException("Escape sequence is not hexadecimal: " + (char)b);
        }
    }

}
