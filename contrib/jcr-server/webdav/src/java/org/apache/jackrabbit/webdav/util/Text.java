/*
 * Copyright 2005 The Apache Software Foundation.
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
package org.apache.jackrabbit.webdav.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class holds a collection of string utility operations.
 */
public final class Text {

    /**
     * used for the md5
     */
    public final static char[] HEXTABLE = "0123456789abcdef".toCharArray();

    /**
     * The default format pattern used in strftime() if no pattern
     * parameter has been supplied. This is the default format used to format
     * dates in Communiqu&eacute; 2
     */
    public static final String DEFAULT_DATE_FORMAT_PATTERN = "dd.MM.yyyy HH:mm:ss";

    /**
     * Common used DateFormat implementation. When the supplied formatting
     * pattern has been translated it is applied to this formatter and then
     * executed. Both steps occur in a synchronized section, such that no
     * two threads disturb each other.
     * <ol>
     * <li>A single instance of the formatter is used to prevent the object
     * creation overhead. But then how much is this ?
     * <li>Using one static formatter for each thread may prove to give even
     * more overhead given all those synchronized blocks waiting for each
     * other during real formatting. But then how knows ?
     * </ol>
     * <p/>
     * This formatter must always be used synchronized as follows :
     * <pre>
     * synchronized (dateFormatter) {
     * dateFormatter.applyPattern( ... );
     * dateFormatter.setTimezone( ... );
     * String result = dateFormatter.format( ... );
     * }
     * 	</pre>
     * <p/>
     * To parse date and time strings , the formatter is used as follows :
     * <pre>
     * synchronized (dateFormatter) {
     * dateFormatter.applyPattern( ... );
     * dateFormatter.setTimezone( ... );
     * try {
     * Date result = dateFormatter.parse(dateString);
     * } catch (ParseException pe) {
     * // handle exception
     * }
     * }
     * 	</pre>
     */
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat();

    /**
     * The UTC timezone
     */
    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");

    /**
     * format for RFC 1123 date string -- "Sun, 06 Nov 1994 08:49:37 GMT"
     */
    private final static SimpleDateFormat rfc1123Format =
            new SimpleDateFormat("EEE, dd MMM yyyyy HH:mm:ss z", Locale.US);

    static {
        rfc1123Format.setTimeZone(TIMEZONE_UTC);
    }

    /**
     * The local timezone
     */
    public static final TimeZone TIMEZONE_LOCAL = TimeZone.getDefault();

    /**
     * Empty result
     */
    private final static String[] empty = new String[0];

    /**
     * avoid instantiation
     */
    private Text() {
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
        if (str == null) {
            return empty;
        }

        Vector strings = new Vector();
        int pos = 0;
        int lastpos = 0;

        // add snipples
        while ((pos = str.indexOf(ch, lastpos)) >= 0) {
            if (pos - lastpos > 0 || respectEmpty)
                strings.add(str.substring(lastpos, pos));
            lastpos = pos + 1;
        }
        // add rest
        if (lastpos < str.length()) {
            strings.add(str.substring(lastpos));
        } else if (respectEmpty && lastpos == str.length()) {
            strings.add("");
        }

        // return stringarray
        return (String[]) strings.toArray(new String[strings.size()]);
    }

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
     * Creates a valid jcr label from the given one
     *
     * @param label
     * @return
     */
    public static String makeValidJCRPath(String label) {
        StringBuffer ret = new StringBuffer(label.length());
        for (int i=0; i<label.length(); i++) {
            char c = label.charAt(i);
            if (c=='*' || c=='\'' || c=='\"') {
                c='_';
            } else if (c=='[') {
                c='(';
            } else if (c==']') {
                c=')';
            }
            ret.append(c);
        }
        return ret.toString();
    }

    /**
     * compares to handles lexigographically with one exception: the '/'
     * character is always considered smaller than all other chars. this results
     * in a ordering, in which the parent pages come first (it's about 6 times
     * slower than the string impl. of compareTo).
     * <ul>example (normal string compare):
     * <li>/foo
     * <li>/foo-bar
     * <li>/foo/bar</li>
     * </ul>
     * <ul>example (this handle compare):
     * <li>/foo
     * <li>/foo/bar</li>
     * <li>/foo-bar
     * </ul>
     *
     * @param h1 the first handle
     * @param h2 the second handle
     * @return the return is positive, if the first handle is bigger than the
     *         second; negative, if the first handle is smaller than the second;
     *         and zero, if the two handles are equal.
     */
    public static int comparePaths(String h1, String h2) {
        char[] ca1 = h1.toCharArray(); // this is faster, than a .charAt everytime
        char[] ca2 = h2.toCharArray();
        int n = ca1.length < ca2.length ? ca1.length : ca2.length;
        int i = 0;
        while (i < n) {
            if (ca1[i] != ca2[i]) {
                char c1 = ca1[i];
                char c2 = ca2[i];
                // we give the '/' the highest priority
                if (c1 == '/')
                    c1 = 1;
                else if (c2 == '/') c2 = 1;
                return c1 - c2;
            }
            i++;
        }
        return ca1.length - ca2.length;
    }

    /**
     * this method does the similar than the {@link #fullPath(String, String)}
     * but it considers the <i>parent</i> as parent directory rather than a base
     * handle. if further respects full qualified uri's.
     * <br>examples:
     * <xmp>
     * parent    | path     | result
     * ----------+----------+------------
     * ""        | ""       | /
     * /foo      | ""       | /foo
     * ""        | /foo     | /foo
     * "."       | foo      | foo
     * /foo/bar  | bla      | /foo/bar/bla
     * /foo/bar  | /bla     | /bla
     * /foo/bar  | ../bla   | /foo/bla
     * /foo/bar  | ./bla    | /foo/bar/bla
     * foo       | bar      | foo/bar
     * c:/bla    | gurk     | c:/bla/gurk
     * /foo      | c:/bar   | c:/bar
     * </xmp>
     *
     * @param parent the base handle
     * @param path   the path
     */
    public static String fullFilePath(String parent, String path) {
        if (parent == null) parent = "";
        if (path == null) path = "";

        // compose source string
        StringBuffer source;
        if (path.equals("") || (path.charAt(0) != '/' && path.indexOf(':') < 0)) {
            // relative
            source = new StringBuffer(parent);
            if (!path.equals("")) {
                source.append("/./");
                source.append(path);
            }
        } else {
            // absolute
            source = new StringBuffer(path == null ? "" : path);
        }
        return makeCanonicalPath(source);
    }

    /**
     * returns a full path.
     * if base is empty, '/' is assumed
     * if base and path are relative, a relative path will be generated.
     * <br>examples:
     * <pre>
     * base      | path     | result
     * ----------+----------+------------
     * ""        | ""       | /
     * /foo      | ""       | /foo
     * ""        | /foo     | /foo
     * "."       | foo      | foo
     * /foo/bar  | bla      | /foo/bla
     * /foo/bar  | /bla     | /bla
     * /foo/bar  | ../bla   | /bla
     * /foo/bar  | ./bla    | /foo/bla
     * foo       | bar      | bar
     * </pre>
     *
     * @param base the base handle
     * @param path the path
     */
    public static String fullPath(String base, String path) {
        if (base == null) base = "";
        if (path == null) path = "";

        // compose source string
        StringBuffer source;
        if (path.equals("") || path.charAt(0) != '/') {
            // relative
            source = new StringBuffer(base);
            if (!path.equals("")) {
                source.append("/../");
                source.append(path);
            }
        } else {
            // absolute
            source = new StringBuffer(path == null ? "" : path);
        }
        return makeCanonicalPath(source);
    }

    /**
     * Make a path canonical. This is a shortcut for
     * <code>
     * Text.makeCanonicalPath(new StringBuffer(path));
     * </code>
     *
     * @param path path to make canonical
     */
    public static String makeCanonicalPath(String path) {
        return makeCanonicalPath(new StringBuffer(path));
    }

    /**
     * make a cannonical path. removes all /./ and /../ and multiple slashes.
     *
     * @param source the input source
     * @return a string containing the cannonical path
     */
    public static String makeCanonicalPath(StringBuffer source) {
        // remove/resolve .. and .
        int dst = 0, pos = 0, slash = 0, dots = 0, last = 0, len = source.length();
        int[] slashes = new int[1024];
        slashes[0] = source.charAt(0) == '/' ? 0 : -1;
        while (pos < len) {
            int ch = source.charAt(pos++);
            switch (ch) {
                case '/':
                    // ignore multiple slashes
                    if (last == '/') continue;
                    // end of segment
                    if (dots == 1) {
                        // ignore
                        dst = slashes[slash];
                    } else if (dots == 2) {
                        // backtrack
                        if (--slash < 0) slash = 0;
                        dst = slashes[slash];
                    } else {
                        // remember slash position
                        slashes[++slash] = dst;
                    }
                    dots = 0;
                    break;
                case '.':
                    // count dots
                    dots++;
                    break;
                default:
                    // invalidate dots
                    dots = 3;
            }
            last = ch;
            if (dst >= 0) source.setCharAt(dst, (char) (last = ch));
            dst++;
        }
        // check dots again
        if (dots == 1) {
            dst = slashes[slash];
        } else if (dots == 2) {
            if (slash > 0) {
                slash--;
            }
            dst = slashes[slash];
        }

        // truncate result
        if (dst > 0) source.setLength(dst);
        return dst == 0 ? "/" : source.toString();
    }

    /**
     * Determines, if two handles are sister-pages, that meens, if they
     * represent the same hierarchic level and share the same parent page.
     *
     * @param h1 first handle
     * @param h2 second handle
     * @return true if on same level, false otherwise
     */
    public static boolean isSibling(String h1, String h2) {
        int pos1 = h1.lastIndexOf('/');
        int pos2 = h2.lastIndexOf('/');
        return (pos1 == pos2 && pos1 >= 0 && h1.regionMatches(0, h2, 0, pos1));
    }

    /**
     * Determines if the <code>descendant</code> handle is hierarchical a
     * descendant of <code>handle</code>.
     * <xmp>
     * /content/playground/en  isDescendantOf     /content/playground
     * /content/playground/en  isDescendantOf     /content
     * /content/playground/en  isNOTDescendantOf  /content/designground
     * /content/playground/en  isNOTDescendantOf  /content/playground/en
     * </xmp>
     *
     * @param handle     the current handle
     * @param descendant the potential descendant
     * @return <code>true</code> if the <code>descendant</code> is a descendant;
     *         <code>false</code> otherwise.
     * @since gumbear
     */
    public static boolean isDescendant(String handle, String descendant) {
        return !handle.equals(descendant) &&
                descendant.startsWith(handle) &&
                descendant.charAt(handle.length()) == '/';
    }

    /**
     * Determines if the <code>descendant</code> handle is hierarchical a
     * descendant of <code>handle</code> or equal to it.
     * <xmp>
     * /content/playground/en  isDescendantOrEqualOf     /content/playground
     * /content/playground/en  isDescendantOrEqualOf     /content
     * /content/playground/en  isDescendantOrEqualOf  /content/playground/en
     * /content/playground/en  isNOTDescendantOrEqualOf  /content/designground
     * </xmp>
     *
     * @param path       the path to check
     * @param descendant the potential descendant
     * @return <code>true</code> if the <code>descendant</code> is a descendant
     *         or equal; <code>false</code> otherwise.
     * @since gumbear
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
     * Returns the label of a handle
     *
     * @param handle the handle
     * @return the label
     */
    public static String getLabel(String handle) {
        int pos = handle.lastIndexOf('/');
        return pos >= 0 ? handle.substring(pos + 1) : "";
    }

    /**
     * Returns the label of a string
     *
     * @param handle the string
     * @param delim  the delimiter
     * @return the label
     */
    public static String getLabel(String handle, char delim) {
        int pos = handle.lastIndexOf(delim);
        return pos >= 0 ? handle.substring(pos + 1) : "";
    }

    /**
     * Digest the plain string using the given algorithm.
     *
     * @param algorithm The alogrithm for the digest. This algorithm must be
     *                  supported by the MessageDigest class.
     * @param data      The plain text String to be digested.
     * @return The digested plain text String represented as Hex digits.
     * @throws NoSuchAlgorithmException if the desired algorithm is not supported by
     *                                  the MessageDigest class.
     */
    public static String digest(String algorithm, String data)
            throws NoSuchAlgorithmException {

        return digest(algorithm, data.getBytes());
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
            res.append(HEXTABLE[(b >> 4) & 15]);
            res.append(HEXTABLE[b & 15]);
        }
        return res.toString();
    }

    /**
     * Returns the n<sup>th</sup> relative parent of the handle, where n=level.
     * <p>Example:<br>
     * <code>
     * Text.getRelativeParent("/en/home/index/about", 1) == "/en/home/index"
     * </code>
     *
     * @param handle the handle of the page
     * @param level  the level of the parent
     */
    public static String getRelativeParent(String handle, int level) {
        int idx = handle.length();
        while (level > 0) {
            idx = handle.lastIndexOf('/', idx - 1);
            if (idx < 0) {
                return "";
            }
            level--;
        }
        return (idx == 0) ? "/" : handle.substring(0, idx);
    }

    /**
     * Returns the n<sup>th</sup> absolute parent of the handle, where n=level.
     * <p>Example:<br>
     * <code>
     * Text.getAbsoluteParent("/en/home/index/about", 1) == "/en/home"
     * </code>
     *
     * @param handle the handle of the page
     * @param level  the level of the parent
     */
    public static String getAbsoluteParent(String handle, int level) {
        int idx = 0;
        int len = handle.length();
        while (level >= 0 && idx < len) {
            idx = handle.indexOf('/', idx + 1);
            if (idx < 0) {
                idx = len;
            }
            level--;
        }
        return level >= 0 ? "" : handle.substring(0, idx);
    }

    /**
     * The list of characters that are not encoded by the <code>escape()</code>
     * and <code>unescape()</code> METHODS. They contains the characters as
     * defined 'unreserved' in section 2.3 of the RFC 2396 'URI genric syntax':
     * <p/>
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
     * are those defined 'unreserved' in section 2.3 of the 'URI genric syntax'
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
     * are those defined 'unreserved' in section 2.3 of the 'URI genric syntax'
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
                    out.append(HEXTABLE[(c >> 4) & 0x0f]);
                    out.append(HEXTABLE[(c) & 0x0f]);
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
     * the 'URI genric syntax' RFC 2396.
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
     * the 'URI genric syntax' RFC 2396. In contrast to the
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
     * @throws ArrayIndexOutOfBoundsException if not enough character follow an
     *                                        escape character
     * @throws IllegalArgumentException       if the 2 characters following the escape
     *                                        character do not represent a hex-number.
     */
    public static String unescape(String string, char escape) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == escape) {
                try {
                    out.write(Integer.parseInt(string.substring(i + 1, i + 3), 16));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
                i += 2;
            } else {
                out.write(c);
            }
        }

        try {
            return new String(out.toByteArray(), "utf-8");
        } catch (UnsupportedEncodingException e) {
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
     * Returns a stringified date accoring to the date format specified in
     * RTF1123. this is of the form: "Sun, 06 Nov 1994 08:49:37 GMT"
     */
    public static String dateToRfc1123String(Date date) {
        synchronized (rfc1123Format) {
            return rfc1123Format.format(date);
        }
    }

    /**
     * Implements a date formatting routine supporting (a subset) of the POSIX
     * <code>strftime()</code> function.
     *
     * @param date          The date value to be formatted
     * @param formatPattern The pattern used to format the date. This pattern
     *                      supports a subset of the pattern characters of the POSIX
     *                      <code>strftime()</code> function. If this pattern is empty or
     *                      <code>null</code> the default pattern
     *                      <code>dd.MM.yyyy HH:mm:ss</code> is used.
     * @param zone          Defines for which time zone the date should be outputted. If
     *                      this parameter is <code>null</code>, then the local time zone is taken.
     * @return the formatted date as a String.
     */
    public static final String strftime(Date date, String formatPattern, TimeZone zone) {
        // Check whether to apply default format
        if (formatPattern == null || formatPattern.length() == 0) {
            formatPattern = DEFAULT_DATE_FORMAT_PATTERN;
        } else {
            formatPattern = convertFormat(formatPattern);
        }

        // check zone
        if (zone == null) zone = TIMEZONE_LOCAL;

        // Reuse the global SimpleDateFormat synchronizing on it to prevent
        // multiple tasks from interfering with each other
        synchronized (dateFormatter) {
            dateFormatter.applyPattern(formatPattern);
            dateFormatter.setTimeZone(zone);
            return dateFormatter.format(date);
        }
    }

    /**
     * Implements a date formatting routine supporting (a subset) of the POSIX
     * <code>strftime()</code> function.
     *
     * @param date          The date value to be formatted
     * @param formatPattern The pattern used to format the date. This pattern
     *                      supports a subset of the pattern characters of the POSIX
     *                      <code>strftime()</code> function. If this pattern is empty or
     *                      <code>null</code> the default pattern
     *                      <code>dd.MM.yyyy HH:mm:ss</code> is used.
     * @param asUTC         Defines whether to interpret the date as belong to the UTC
     *                      time zone or the local time zone.
     */
    public static final String strftime(Date date, String formatPattern, boolean asUTC) {
        return strftime(date, formatPattern, asUTC ? TIMEZONE_UTC : TIMEZONE_LOCAL);
    }

    /**
     * Implements a date formatting routine supporting (a subset) of the POSIX
     * <code>strftime()</code> function. The default pattern
     * <code>dd.MM.yyyy HH:mm:ss</code> is used to format the date.
     *
     * @param date  The date value to be formatted
     * @param asUTC Defines whether to interpret the date as belong to the UTC
     *              time zone or the local time zone.
     */
    public static final String strftime(Date date, boolean asUTC) {
        return strftime(date, null, asUTC);
    }

    /**
     * Implements a date formatting routine supporting (a subset) of the POSIX
     * <code>strftime()</code> function. The default pattern
     * <code>dd.MM.yyyy HH:mm:ss</code> is used to format the date, which is
     * interpreted to be in the local time zone.
     *
     * @param date The date value to be formatted
     */
    public static final String strftime(Date date) {
        return strftime(date, null, false);
    }

    /**
     * Parses the date string based on the format pattern which is in the
     * format used by the Java platfrom SimpleDateFormat class.
     *
     * @param dateString    The date string to be parsed
     * @param formatPattern the pattern to use for parsing. If <code>null</code>
     *                      or empty, the same default pattern is used as with
     *                      {@link #strftime(Date, String, boolean)}, namely
     *                      <code>dd.MM.yyyy HH:mm:ss</code>.
     * @throws ParseException if the date string cannot be parsed accordinung
     *                        to the format pattern.
     */
    public static final Date parseDate(String dateString, String formatPattern)
            throws ParseException {

        return parseDate(dateString, formatPattern, false);
    }

    /**
     * Parses the date string based on the format pattern which is in the
     * format used by the Java platfrom SimpleDateFormat class.
     *
     * @param dateString    The date string to be parsed
     * @param formatPattern the pattern to use for parsing. If <code>null</code>
     *                      or empty, the same default pattern is used as with
     *                      {@link #strftime(Date, String, boolean)}, namely
     *                      <code>dd.MM.yyyy HH:mm:ss</code>.
     * @param isUTC         if <code>true</code> the date string is considered in UTC,
     *                      otherwise the default timezone of the host is used.
     * @throws ParseException if the date string cannot be parsed accordinung
     *                        to the format pattern.
     */
    public static final Date parseDate(String dateString, String formatPattern,
                                       boolean isUTC)
            throws ParseException {

        synchronized (dateFormatter) {
            dateFormatter.applyPattern(formatPattern);
            if (isUTC) {
                dateFormatter.setTimeZone(TIMEZONE_UTC);
            } else {
                dateFormatter.setTimeZone(TimeZone.getDefault());
            }
            return dateFormatter.parse(dateString);
        }
    }

    /**
     * Parses the date string based on the format pattern which is in the
     * default format <code>dd.MM.yyyy HH:mm:ss</code>.
     *
     * @param dateString The date string to be parsed
     * @throws ParseException if the date string cannot be parsed accordinung
     *                        to the format pattern.
     */
    public static final Date parseDate(String dateString) throws ParseException {
        return parseDate(dateString, DEFAULT_DATE_FORMAT_PATTERN, false);
    }

    /**
     * Parses the date string based on the format pattern which is in the
     * default format <code>dd.MM.yyyy HH:mm:ss</code>.
     *
     * @param dateString The date string to be parsed
     * @param isUTC      if <code>true</code> the date string is considered in UTC,
     *                   otherwise the default timezone of the host is used.
     * @throws ParseException if the date string cannot be parsed accordinung
     *                        to the format pattern.
     */
    public static final Date parseDate(String dateString, boolean isUTC)
            throws ParseException {
        return parseDate(dateString, DEFAULT_DATE_FORMAT_PATTERN, isUTC);
    }

    //--------- sprintf() formatting constants ---------------------------------

    /**
     * left justified - '-' flag
     */
    private static final int FLAG_LJ = 1;

    /**
     * always show sign - '+' flag
     */
    private static final int FLAG_SI = 2;

    /**
     * space placeholder for omitted plus sign - ' ' flag, ignore if SI
     */
    private static final int FLAG_SP = 3;

    /**
     * zero padded if right aligned - '0' flag, ignore if LJ
     */
    private static final int FLAG_ZE = 4;

    /**
     * alternate format - '#' flag :
     * SI  - incr. precision to have zero as first char
     * x   - prefix '0x'
     * X   - prefix '0X'
     * eEf - always show decimal point, omit trailing zeroes
     * gG  - always show decimal point, show trailing zeroes
     */
    private static final int FLAG_AL = 5;

    /**
     * interpret ints as short - 'h' size
     */
    private static final int FLAG_SHORT = 8;

    /**
     * interpret ints as long - 'l' size
     */
    private static final int FLAG_LONG = 9;

    /**
     * waiting for format
     */
    private static final int PARSE_STATE_NONE = 0;

    /**
     * parsing flags
     */
    private static final int PARSE_STATE_FLAGS = 1;

    /**
     * parsing wdth
     */
    private static final int PARSE_STATE_WIDTH = 2;

    /**
     * parsing precision
     */
    private static final int PARSE_STATE_PRECISION = 3;

    /**
     * parsing size
     */
    private static final int PARSE_STATE_SIZE = 4;

    /**
     * parsing type
     */
    private static final int PARSE_STATE_TYPE = 5;

    /**
     * parsing finished
     */
    private static final int PARSE_STATE_END = 6;

    /**
     * incomplete pattern at end of format string, throw error
     */
    private static final int PARSE_STATE_ABORT = 7;

    /**
     * end of format string during plain text
     */
    private static final int PARSE_STATE_TERM = 8;

    /**
     * This method implements the famous and ubiquituous <code>sprintf</code>
     * formatter in Java. The arguments to the method are the formatting string
     * and the list of arguments defined by the formatting instructions
     * contained in the formatting string.
     * <p/>
     * Each element in the argument array is either a <code>Number</code> object
     * or any other <code>Object</code> type. Whenever the formatting string
     * stipulates the corresponding argument to be numeric, it is assumed this
     * array element to be a <code>Number</code>. If this is not the case an
     * <code>IllegalArgumentException</code> is thrown. If a <code>String</code>
     * argument is stipulated by the formatting string, a simple call to the
     * <code>toString()</code> method of the object yields the
     * <code>String</code> required.
     * <p/>
     * <b>SPECIFICATION</b>
     * <p/>
     * <code>sprintf</code> accepts a series of arguments, applies to each a
     * format specifier from <code>format</code>, and stores the formatted data
     * to the result <code>Strint</code>. The method throws an
     * <code>IllegalArgumentException</code> if either the <code>format</code>
     * is incorrect, there are not enough arguments for the <code>format</code>
     * or if any of the argument's type is wrong. <code>sprintf</code> returns
     * when it  reaches the end of the format string. If there are more
     * arguments than the format requires, excess arguments are ignored.
     * <p/>
     * <code>format</code> is a <code>String</code> containing two types of
     * objects: ordinary characters (other than <code><i>%</i></code>), which
     * are copied unchanged to the output, and conversion  specifications,
     * each of which is introduced by <code><i>%</i></code>. (To include
     * <code><i>%</i></code> in the output, use <code>%%</code> in the format
     * string.) A conversion specification has the following form:
     * <blockquote>
     * <code>
     * [ <i>flags</i> ] [ <i>width</i> ] [ "." <i>prec</i> ] [ <i>size</i> ]
     * <i>type</i>
     * </code>
     * </blockquote>
     * <p/>
     * The  fields  of the conversion specification have the following meanings:
     * <p/>
     * <dl>
     * <dt><i>flags</i>
     * <dd>an optional sequence of characters  which  control  output
     * justification, numeric  signs,  decimal  points, trailing zeroes, and
     * octal and hex prefixes. The  flag  characters are minus (<code>-</code>),
     * plus (<code>+</code>), space ("<code> </code>"), zero (<code>0</code>),
     * and sharp (<code>#</code> ). They can appear in any combination.
     * <p/>
     * <table>
     * <tr>
     * <td><code>-</code></td>
     * <td>The result of the conversion is left justified, and the right
     * is  padded with blanks. If you do not use this flag, the result
     * is right justified,  and  padded  on  the left.</td>
     * </tr>
     * <tr>
     * <td><code>+</code></td>
     * <td>The  result of a signed conversion (as determined by <i>type</i>)
     * will always begin with a plus or minus sign. (If you do not use
     * this flag, positive values do not begin with a plus sign.)</td>
     * </tr>
     * <tr>
     * <td>"<code> </code>" (space)</td>
     * <td>If the first character of a  signed conversion  specification is
     * not a sign, or if a signed conversion results in no characters,
     * the result will begin  with a space. If the space
     * (<code> </code>) flag and the plus (<code>+</code>) flag both
     * appear, the space flag is ignored.</td>
     * </tr>
     * <tr>
     * <td><code>0</code></td>
     * <td>If the <i>type</i> is <code>d</code>, <code>i</code>,
     * <code>o</code>, <code>u</code>, <code>x</code>, <code>X</code>,
     * <code>e</code>, <code>E</code>, <code>f</code>, <code>g</code>,
     * or <code>G</code>:  leading  zeroes,  are  used to pad the field
     * width (following any indication of sign or base); no spaces
     * are used  for  padding.  If the zero (<code>0</code>) and minus
     * (<code>-</code>) flags both appear, the zero (<code>0</code>)
     * flag will be ignored. For <code>d</code>, <code>i</code>,
     * <code>o</code>, <code>u</code>, <code>x</code>, <code>X</code>
     * conversions, if a precision <i>prec</i> is specified, the zero
     * (<code>0</code>) flag is ignored.
     * <br>
     * Note that <i>0</i> is interpreted as a flag, not as the
     * beginning of a field width.</td>
     * </tr>
     * <tr>
     * <td><code>#</code></td>
     * <td>The result is to be converted to an alternative form, according
     * to the <i>type</i> character:
     * <dl>
     * <dt><code>o</code>
     * <dd>Increases precision to force the first digit of the result
     * to be a zero.
     * <p/>
     * <dt><code>x</code>
     * <dl>A non-zero result will have a <code>0x</code> prefix.
     * <p/>
     * <dt><code>X</code>
     * <dl>A non-zero result will have a <code>0X</code> prefix.
     * <p/>
     * <dt><code>e</code>, <code>E</code>, or <code>f</code>
     * <dl>The result will always contain a decimal point even if no
     * digits follow the point. (Normally, a decimal point appears
     * only if a digit follows it.) Trailing zeroes are removed.
     * <p/>
     * <dt><code>g</code> or <code>G</code>
     * <dl>Same as <code>e</code> or <code>E</code>, but trailing
     * zeroes arenot removed.
     * <p/>
     * <dt>all others
     * <dd>Undefined.
     * </dl></td>
     * </tr>
     * </table>
     * <p/>
     * <dt><i>width</i>
     * <dd><i>width</i> is  an optional minimum field width. You can either
     * specify it directly as a decimal integer, or indirectly by using instead
     * an asterisk (<code>*</code>), in which case an integral numeric argument
     * is used as the field width. Negative field widths are not supported; if
     * you attempt to specify a negative field width, it is interpreted as a
     * minus (<code>i</code>) flag followed  by a positive field width.
     * <p/>
     * <dt><i>prec</i>
     * <dd>an  optional field; if present, it is introduced with
     * `<code>.</code>' (a period). This field gives the maximum number of
     * characters  to print in a conversion; the minimum number of digits of an
     * integer to print, for conversions with <i>type</i> <code>d</code>,
     * <code>i</code>, <code>o</code>, <code>u</code>, <code>x</code>, and
     * <code>X</code>; the maximum number of significant digits, for the
     * <code>g</code> and <code>G</code> conversions; or the number of digits
     * to  print after the decimal point, for <code>e</code>, <code>E</code>,
     * and <code>f</code> conversions. You can specify the precision either
     * directly as  a decimal  integer  or indirectly by using an asterisk
     * (<code>*</code>), in which case an integral numeric argument is used as
     * the  precision. Supplying  a negative precision is equivalent to
     * omitting the precision.  If only a period is specified  the  precision
     * is zero.  If a precision appears with any other conversion <i>type</i>
     * than those listed here, the behavior is undefined.
     * <p/>
     * <dt><i>size</i>
     * <dd><code>h</code>, <code>l</code>, and <code>L</code> are optional size
     * characters which override the default way that <code>sprintf</code>
     * interprets the  data  type  of the  corresponding argument.
     * <code>h</code> forces the following <code>d</code>, <code>i</code>,
     * <code>o</code>, <code>u</code>, <code>x</code>, or <code>X</code>
     * conversion <i>type</i> to apply to a <code>short</code> or <code>unsigned
     * short</code>. Similarily, an <code>l</code> forces the following
     * <code>d</code>, <code>i</code>, <code>o</code>, <code>u</code>,
     * <code>x</code>, or <code>X</code> conversion  <i>type</i> to apply  to
     * a <code>long</code> or <code>unsigned long</code>. If an <code>h</code>
     * or an <code>l</code> appears with another conversion specifier, the
     * behavior  is undefined. <code>L</code> forces  a following
     * <code>e</code>, <code>E</code>, <code>f</code>, <code>g</code>, or
     * <code>G</code> conversion <i>type</i> to apply to a <code>long
     * double</code> argument. If <code>L</code> appears  with any other
     * conversion <i>type</i>, the behavior is undefined.
     * <p/>
     * <dt><i>type</i>
     * <dd><i>type</i> specifies  what  kind of conversion <code>sprintf</code>
     * performs. Here is a table of these:
     * <p/>
     * <dl>
     * <dt><code>%</code>
     * <dd>prints the percent character (<code>%</code>)
     * <p/>
     * <dt><code>c</code>
     * <dd>prints <i>arg</i> as single character. That is the argument is
     * converted to a <code>String</code> of which only the first
     * character is printed.
     * <p/>
     * <dt><code>s</code>
     * <dd>Prints characters until precision is reached or the
     * <code>String</code> ends; takes any <code>Object</code> whose
     * <code>toString()</code> method is called to get the
     * <code>String</code> to print.
     * <p/>
     * <dt><code>d</code>
     * <dd>prints a signed decimal integer; takes a <code>Number</code> (same
     * as <code>i</code>)
     * <p/>
     * <dt><code>d</code>
     * <dd>prints a signed decimal integer; takes a <code>Number</code> (same
     * as <code>d</code>)
     * <p/>
     * <dt><code>d</code>
     * <dd>prints a signed octal integer; takes a <code>Number</code>
     * <p/>
     * <dt><code>u</code>
     * <dd>prints a unsigned decimal integer; takes a <code>Number</code>.
     * This conversion is not supported correctly by the Java
     * implementation and is really the same as <code>i</code>.
     * <p/>
     * <dt><code>x</code>
     * <dd>prints an unsigned hexadecimal integer (using <i>abcdef</i> as
     * digits beyond <i>9</i>; takes a <code>Number</code>
     * <p/>
     * <dt><code>X</code>
     * <dd>prints an unsigned hexadecimal integer (using <i>ABCDEF</i> as
     * digits beyond <i>9</i>; takes a <code>Number</code>
     * <p/>
     * <dt><code>f</code>
     * <dd>prints a signed value of the form <i>[-]9999.9999</i>; takes a
     * <code>Number</code>
     * <p/>
     * <dt><code>e</code>
     * <dd>prints a signed value of the form <i>[-]9.9999e[+|-]999</i>; takes
     * a <code>Number</code>
     * <p/>
     * <dt><code>E</code>
     * <dd>prints the same way as <code>e</code>, but using <i>E</i> to
     * introduce the exponent; takes a <code>Number</code>
     * <p/>
     * <dt><code>g</code>
     * <dd>prints a signed value in either <code>f</code> or <code>e</code>
     * form, based  on given value and precision &emdash; trailing zeros
     * and the decimal point are printed only if necessary; takes a
     * <code>Number</code>
     * <p/>
     * <dt><code>G</code>
     * <dd>prints the same way as <code>g</code>, but using <code>E</code>
     * for the exponent if an exponent is needed; takes a
     * <code>Number</code>
     * <p/>
     * <dt><code>n</code>
     * <dd>Not supported in the Java implementation, throws an
     * <code>IllegalArgumentException</code> if used.
     * <p/>
     * <dt><code>p</code>
     * <dd>Not supported in the Java implementation, throws an
     * <code>IllegalArgumentException</code> if used.
     * </dl>
     * <p/>
     * </dl>
     * <p/>
     * <p/>
     * <b>IMPLEMENTATION NOTES</b>
     * <p/>
     * Due to the nature of the Java programming language, neither pointer
     * conversions, nor <code>unsigned</code> and <code>long double</code>
     * conversions are supported.
     * <p/>
     * Also the Java implementation only distinguishes between
     * <code>Number</code> and other <code>Object</code> arguments. If a
     * numeric argument is expected as per the <code>format</code>
     * <code>String</code>, the current argument must be a <code>Number</code>
     * object that is converted to a basic type as expected. If a
     * <code>String</code> or <code>char</code> argument is expected, any
     * Object is valid, whose <code>toString()</code> method is used to convert
     * to a <code>String</code>.
     *
     * @param format The format string as known from the POSIX sprintf()
     *               C function.
     * @param args   The list of arguments to accomodate the format string. This
     *               argument list is supposed to contain at least as much entries as
     *               there are formatting options in the format string. If for a
     *               numeric option, the entry is not a number an
     *               <code>IllegalArgumentException</code> is thrown.
     * @return The formatted <code>String</code>. An empty <code>String</code>
     *         is only returned if the <code>format</code> <code>String</code>
     *         is empty. A <code>null</code> value is never returned.
     * @throws NullPointerException     if the formatting string or any of the
     *                                  argument values is <code>null</code>.
     * @throws IllegalArgumentException if the formatting string has wrong
     *                                  format tags, if the formatting string has an incomplete
     *                                  formatting pattern at the end of the string, if the argument
     *                                  vector has not enough values to satisfy the formatting string
     *                                  or if an argument's type does not match the requirements of the
     *                                  format string.
     */
    public static String sprintf(String format, Object[] args) {

        // Return immediately if we have no arguments ....
        if (format == null) {
            throw new NullPointerException("format");
        }

        if (format.length() == 0) {
            return "";
        }

        // Get the format string
        char[] s = format.toCharArray();

        // prepare the result, initial size has no sound basis
        StringBuffer res = new StringBuffer(s.length * 3 / 2);

        for (int i = 0, j = 0, length = format.length(); i < length;) {

            int parse_state = PARSE_STATE_NONE;
            BitSet flags = new BitSet(16);
            int width = 0;
            int precision = -1;
            char fmt = ' ';

            // find a start of a formatting ...
            while (parse_state == PARSE_STATE_NONE) {
                if (i >= length)
                    parse_state = PARSE_STATE_TERM;
                else if (s[i] == '%') {
                    if (i < length - 1) {
                        if (s[i + 1] == '%') {
                            res.append('%');
                            i++;
                        } else {
                            parse_state = PARSE_STATE_FLAGS;
                        }
                    } else {
                        throw new java.lang.IllegalArgumentException("Incomplete format at end of format string");
                    }
                } else {
                    res.append(s[i]);
                }
                i++;
            }

            // Get flags, if any
            while (parse_state == PARSE_STATE_FLAGS) {
                if (i >= length)
                    parse_state = PARSE_STATE_ABORT;
                else if (s[i] == ' ')
                    flags.set(FLAG_SP);
                else if (s[i] == '-')
                    flags.set(FLAG_LJ);
                else if (s[i] == '+')
                    flags.set(FLAG_SI);
                else if (s[i] == '0')
                    flags.set(FLAG_ZE);
                else if (s[i] == '#')
                    flags.set(FLAG_AL);
                else {
                    parse_state = PARSE_STATE_WIDTH;
                    i--;
                }
                i++;
            }

            // Get width specification
            while (parse_state == PARSE_STATE_WIDTH) {
                if (i >= length) {

                    parse_state = PARSE_STATE_ABORT;

                } else if ('0' <= s[i] && s[i] <= '9') {

                    width = width * 10 + s[i] - '0';
                    i++;

                } else {
                    // finished with digits or none at all

                    // if width is a '*' take width from arg
                    if (s[i] == '*') {
                        // Check whether we have an argument
                        if (j >= args.length) {
                            throw new IllegalArgumentException("Missing " +
                                    "argument for the width");
                        }
                        try {
                            width = ((Number) (args[j++])).intValue();
                        } catch (ClassCastException cce) {
                            // something wrong with the arg
                            throw new IllegalArgumentException("Width " +
                                    "argument is not numeric");
                        }
                        i++;
                    }

                    // if next is a dot, then we have a precision, else a size
                    if (s[i] == '.') {
                        parse_state = PARSE_STATE_PRECISION;
                        precision = 0;
                        i++;
                    } else {
                        parse_state = PARSE_STATE_SIZE;
                    }

                }
            }

            // Get precision
            while (parse_state == PARSE_STATE_PRECISION) {

                if (i >= length) {

                    parse_state = PARSE_STATE_ABORT;

                } else if ('0' <= s[i] && s[i] <= '9') {

                    precision = precision * 10 + s[i] - '0';
                    i++;

                } else {
                    // finished with digits or none at all

                    // if width is a '*' take precision from arg
                    if (s[i] == '*') {
                        // Check whether we have an argument
                        if (j >= args.length) {
                            throw new IllegalArgumentException("Missing " +
                                    "argument for the precision");
                        }
                        try {
                            width = ((Number) (args[j++])).intValue();
                        } catch (ClassCastException cce) {
                            // something wrong with the arg
                            throw new IllegalArgumentException("Precision " +
                                    "argument is not numeric");
                        }
                        i++;
                    }

                    parse_state = PARSE_STATE_SIZE;

                }

            }

            // Get size character
            if (parse_state == PARSE_STATE_SIZE) {
                if (i >= length)
                    parse_state = 6;
                else {
                    if (s[i] == 'h') {
                        flags.set(FLAG_SHORT);
                        i++;
                    } else if (s[i] == 'l' || s[i] == 'L') {
                        flags.set(FLAG_LONG);
                        i++;
                    }
                    parse_state = PARSE_STATE_TYPE;
                }
            }

            // Get format character
            if (parse_state == PARSE_STATE_TYPE) {
                if (i >= length)
                    parse_state = PARSE_STATE_ABORT;
                else {
                    fmt = s[i];
                    i++;
                    parse_state = PARSE_STATE_END;
                }
            }

            // Now that we have anything, format it ....
            if (parse_state == PARSE_STATE_END) {

                // Check whether we have an argument
                if (j >= args.length) {
                    throw new IllegalArgumentException("Not enough parameters for the format string");
                }

                try {

                    // Convert the argument according to the flag
                    switch (fmt) {
                        case 'd': // decimal - fall through
                        case 'i': // integral - fall through
                        case 'x': // hexadecimal, lower case - fall through
                        case 'X': // hexadecimal, upper case - fall through
                        case 'o': // octal - fall through
                        case 'u': // unsigned (not really supported)
                            format(res, (Number) args[j], fmt, width, precision,
                                    flags);
                            break;

                        case 'f': // float - fall through
                        case 'e': // exponential, lower case - fall through
                        case 'E': // exponential, upper case - fall through
                        case 'g': // float or exp., lower case - fall through
                        case 'G': // float or exp., upper case - fall through
                            format(res, ((Number) args[j]).doubleValue(), fmt,
                                    width, precision, flags);
                            break;

                        case 'c': // character
                            precision = 1;
                            // fall through

                        case 's': // string

                            String val = args[j].toString();
                            if (val.length() > precision && precision > 0) {
                                val = val.substring(0, precision);
                            }

                            flags.clear(FLAG_ZE);
                            format(res, val, "", width, flags);
                            break;

                        default : // unknown format

                            throw new IllegalArgumentException("Unknown " +
                                    "conversion type " + fmt);

                    }

                } catch (ClassCastException cce) {
                    // something wrong with the arg
                    throw new IllegalArgumentException("sprintf: Argument #" +
                            j + " of type " + args[j].getClass().getName() +
                            " does not match format " + fmt);
                }

                // goto the next argument
                j++;
            }

            // if the format string is not complete
            if (parse_state == PARSE_STATE_ABORT) {
                throw new java.lang.IllegalArgumentException("Incomplete format at end of format string");
            }

        } // while i

        return res.toString();
    }

    /**
     * Formats a string according to format and argument. This method only
     * supports string formats. See {@link #sprintf(String, Object[])} for details.
     *
     * @param format The format string
     * @param a0     The single parameter
     * @return the result from <code>sprintf(format, new Object[]{ a0 })</code>.
     * @throws IllegalArgumentException from {@link #sprintf(String, Object[])}.
     */
    public static String sprintf(String format, Object a0) {
        return sprintf(format, new Object[]{a0});
    }

    /**
     * Formats a string according to format and argument. This method only
     * supports string formats. See {@link #sprintf(String, Object[])} for details.
     *
     * @param format The format string
     * @param a0     The first parameter
     * @param a1     The second parameter
     * @return the result from <code>sprintf(format, new Object[]{ ... })</code>.
     * @throws IllegalArgumentException from {@link #sprintf(String, Object[])}.
     */
    public static String sprintf(String format, Object a0, Object a1) {
        return sprintf(format, new Object[]{a0, a1});
    }

    /**
     * Formats a string according to format and argument. This method only
     * supports string formats. See {@link #sprintf(String, Object[])} for details.
     *
     * @param format The format string
     * @param a0     The first parameter
     * @param a1     The second parameter
     * @param a2     The thrid parameter
     * @return the result from <code>sprintf(format, new Object[]{ ... })</code>.
     * @throws IllegalArgumentException from {@link #sprintf(String, Object[])}.
     */
    public static String sprintf(String format, Object a0, Object a1,
                                 Object a2) {

        return sprintf(format, new Object[]{a0, a1, a2});
    }

    /**
     * Formats a string according to format and argument. This method only
     * supports string formats. See {@link #sprintf(String, Object[])} for details.
     *
     * @param format The format string
     * @param a0     The first parameter
     * @param a1     The second parameter
     * @param a2     The thrid parameter
     * @param a3     The fourth parameter
     * @return the result from <code>sprintf(format, new Object[]{ ... })</code>.
     * @throws IllegalArgumentException from {@link #sprintf(String, Object[])}.
     */
    public static String sprintf(String format, Object a0, Object a1,
                                 Object a2, Object a3) {

        return sprintf(format, new Object[]{a0, a1, a2, a3});
    }

    /**
     * Formats a string according to format and argument. This method only
     * supports string formats. See {@link #sprintf(String, Object[])} for details.
     *
     * @param format The format string
     * @param a0     The first parameter
     * @param a1     The second parameter
     * @param a2     The thrid parameter
     * @param a3     The fourth parameter
     * @param a4     The fifth parameter
     * @return the result from <code>sprintf(format, new Object[]{ ... })</code>.
     * @throws IllegalArgumentException from {@link #sprintf(String, Object[])}.
     */
    public static String sprintf(String format, Object a0, Object a1,
                                 Object a2, Object a3, Object a4) {
        return sprintf(format, new Object[]{a0, a1, a2, a3, a4});
    }

    //---------- internal ------------------------------------------------------

    /**
     * Convert a Date formatting string in POSIX strftime() format to the
     * pattern format used by the Java SimpleDateFormat class.
     * <p/>
     * These are the symbols used in SimpleDateFormat to which we convert
     * our strftime() symbols.
     * <p/>
     * <table>
     * <tr><th>Symbol<th>Meaning<th>Presentation<th>Example</tr>
     * <tr><td>G<td>era designator      <td>(Text)     <td>AD</tr>
     * <tr><td>y<td>year                <td>(Number)   <td>1996</tr>
     * <tr><td>M<td>month in year       <td>(Text & Number)<td>July & 07</tr>
     * <tr><td>d<td>day in month        <td>(Number)   <td>10</tr>
     * <tr><td>h<td>hour in am/pm (1~12)<td>(Number)   <td>12</tr>
     * <tr><td>H<td>hour in day (0~23)  <td>(Number)   <td>0</tr>
     * <tr><td>m<td>minute in hour      <td>(Number)   <td>30</tr>
     * <tr><td>s<td>second in minute    <td>(Number)   <td>55</tr>
     * <tr><td>S<td>millisecond         <td>(Number)   <td>978</tr>
     * <tr><td>E<td>day in week         <td>(Text)     <td>Tuesday</tr>
     * <tr><td>D<td>day in year         <td>(Number)   <td>189</tr>
     * <tr><td>F<td>day of week in month<td>(Number)   <td>2 (2nd Wed in July)</tr>
     * <tr><td>w<td>week in year        <td>(Number)   <td>27</tr>
     * <tr><td>W<td>week in month       <td>(Number)   <td>2</tr>
     * <tr><td>a<td>am/pm marker        <td>(Text)     <td>PM</tr>
     * <tr><td>k<td>hour in day (1~24)  <td>(Number)   <td>24</tr>
     * <tr><td>K<td>hour in am/pm (0~11)<td>(Number)   <td>0</tr>
     * <tr><td>z<td>time zone           <td>(Text)     <td>Pacific Standard Time</tr>
     * <tr><td>'<td>escape for text     <td>(Delimiter)<td>&nbsp;</tr>
     * <tr><td>''<td>single quote       <td>(Literal)  <td>'</tr>
     * </table>
     *
     * @param posixFormat The formatting pattern in POSIX strftime() format.
     * @return The Date formatting pattern in SimpleDateFormat pattern format.
     *         <p/>
     *         todo: Check for the more or less complete support of all pattern tags.
     */
    private static String convertFormat(String posixFormat) {
        char[] format = posixFormat.toCharArray();
        StringBuffer jFormat = new StringBuffer(format.length);
        boolean inString = false;

        for (int i = 0; i < format.length; i++) {

            if (format[i] == '\'') {

                // insert a second tick
                jFormat.append('\'');

            } else if (format[i] == '%') {

                if (inString) {
                    jFormat.append('\'');
                    inString = false;
                }

                switch (format[++i]) {
                    case '%':
                        // just a single '%'
                        jFormat.append('%');
                        break;

                    case 'a':
                        // locale's abbreviated weekday name
                        jFormat.append("EEE");
                        break;

                    case 'A':
                        // locale's full weekday name
                        jFormat.append("EEEE");
                        break;

                    case 'b':
                        // locale's abbreviated month name
                        jFormat.append("MMM");
                        break;

                    case 'B':
                        // locale's full month name
                        jFormat.append("MMMM");
                        break;

                    case 'c':
                        // locale's appropriate date and time representation
                        break;

                    case 'C':
                        // century number as a decimal number (00-99)
                        // Not supported
                        break;

                    case 'd':
                        // day of month (01-31)
                        jFormat.append("dd");
                        break;

                    case 'D':
                        // date as %m/%d/%y
                        jFormat.append("MM/dd/yy");
                        break;

                    case 'e':
                        // day of month ( 1-31)
                        jFormat.append("d");
                        break;

                    case 'h':
                        // locale's abbreviated month name
                        jFormat.append("MMM");
                        break;

                    case 'H':
                        // hour (00-23)
                        jFormat.append("HH");
                        break;

                    case 'I':
                        // hour (01-12)
                        jFormat.append("hh");
                        break;

                    case 'j':
                        // day number of year (001-366)
                        jFormat.append("DDD");
                        break;

                    case 'K':
                        // plus C !!!
                        if (format[++i] == 'C') {
                            // locale's appropriate date and time representation
                        }
                        break;

                    case 'm':
                        // month number (01-12)
                        jFormat.append("MM");
                        break;

                    case 'M':
                        // minute (00-59)
                        jFormat.append("mm");
                        break;

                    case 'n':
                        // new line
                        jFormat.append(System.getProperty("line.separator", "\n"));
                        break;

                    case 'p':
                        // locale's equivalent of either AM and PM
                        jFormat.append("aa");
                        break;

                    case 'r':
                        // locale's 12-hour time representation, default %I:%M:%S [AM|PM]
                        jFormat.append("hh:mm:ss aa");
                        break;

                    case 'R':
                        // time as %H:%M
                        jFormat.append("hh:mm");
                        break;

                    case 'S':
                        // seconds (00-61) [ leap seconds ;-) ]
                        jFormat.append("ss");
                        break;

                    case 't':
                        // tab character
                        jFormat.append('\t');
                        break;

                    case 'T':
                        // time as %H:%M:%S
                        jFormat.append("HH:mm:ss");
                        break;

                    case 'U':
                        // week number of year (00-53), sunday is first day of week
                        jFormat.append("ww");
                        break;

                    case 'w':
                        // weekday number (0-6, 0=sunday)
                        jFormat.append("E");
                        break;

                    case 'W':
                        // week number of year (00-53), monday is first day of week
                        jFormat.append("ww");
                        break;

                    case 'x':
                        // locale's appropriate date representation
                        break;

                    case 'X':
                        // locale's appropriate time representation
                        break;

                    case 'y':
                        // year within century (00-99)
                        jFormat.append("yy");
                        break;

                    case 'Y':
                        // year as %c%y (e.g. 1986)
                        jFormat.append("yyyy");
                        break;

                    case 'Z':
                        // time zone name or no characters if no time zone exists
                        jFormat.append("zzz");
                        break;

                    default:
                        // ignore and ...
                        continue;
                }

            } else {

                if (!inString) {
                    inString = true;
                    jFormat.append('\'');
                }

                jFormat.append(format[i]);
            }
        }

        return jFormat.toString();
    }

    /**
     * Implements the integral number formatting part of the
     * <code>sprintf()</code> method, that is, this method handles d, i, o, u,
     * x, and X formatting characters.
     *
     * @param buf       The formatted number is appended to this string buffer
     * @param num       The number object to format
     * @param fmt       The format character defining the radix of the number
     * @param width     The minimum field width for the number
     * @param precision The minimum number of digits to print for the number,
     *                  this does not include any signs or prefix characters
     * @param flags     The flags governing the formatting. This is a combination
     *                  of the FLAG_* constants above.
     * @return The formatted string
     * @see <a href="sprintf.html"><code>sprintf()</code></a>
     */
    private static StringBuffer format(StringBuffer buf, Number num,
                                       char fmt, int width, int precision, BitSet flags) {

        String numStr;
        String prefStr = "";
        boolean toUpper = (fmt == 'X');

        // Check precision and make default
        if (precision >= 0) {
            flags.clear(FLAG_ZE);
        } else {
            precision = 1;
        }

        // Get the value and adjust size interpretation
        long val;
        long sizeMask;
        if (flags.get(FLAG_SHORT)) {
            val = num.shortValue();
            sizeMask = 0xffffL;
        } else if (flags.get(FLAG_LONG)) {
            val = num.longValue();
            sizeMask = 0xffffffffffffffffL;
        } else {
            val = num.intValue();
            sizeMask = 0xffffffffL;
        }

        // check formatting type
        if (fmt == 'x' || fmt == 'X') {

            numStr = Long.toHexString(val & sizeMask);

            if (toUpper) {
                numStr = numStr.toUpperCase();
            }

            if (flags.get(FLAG_AL)) {
                prefStr = toUpper ? "0X" : "0x";
            }

        } else if (fmt == 'o') {

            numStr = Long.toOctalString(val & sizeMask);

            if (flags.get(FLAG_AL) && val != 0 && precision <= numStr.length()) {
                precision = numStr.length() + 1;
            }

        } else {

            numStr = Long.toString(val);

            // move sign to prefix if negative, or set '+'
            if (val < 0) {
                prefStr = "-";
                numStr = numStr.substring(1);
            } else if (flags.get(FLAG_SI)) {
                prefStr = "+";
            }


        }

        // prefix 0 for precision
        if (precision > numStr.length()) {
            StringBuffer tmp = new StringBuffer(precision);
            for (precision -= numStr.length(); precision > 0; precision--) {
                tmp.append('0');
            }
            numStr = tmp.append(numStr).toString();
        }

        return format(buf, numStr, prefStr, width, flags);
    }

    /**
     * Implements the floating point number formatting part of the
     * <code>sprintf()</code> method, that is, this method handles f, e, E, g,
     * and G formatting characters.
     *
     * @param buf       The formatted number is appended to this string buffer
     * @param num       The numeric value to format
     * @param fmt       The format character defining the floating point format
     * @param width     The minimum field width for the number
     * @param precision Depending on <code>fmt</code> either the number of
     *                  digits after the decimal point or the number of significant
     *                  digits.
     * @param flags     The flags governing the formatting. This is a combination
     *                  of the FLAG_* constants above.
     * @return The formatted string
     * @see <a href="sprintf.html"><code>sprintf()</code></a>
     */
    private static StringBuffer format(StringBuffer buf, double num,
                                       char fmt, int width, int precision, BitSet flags) {

        BigDecimal val = new BigDecimal(num).abs();

        // the exponent character, will be defined if exponent is needed
        char expChar = 0;

        // the exponent value
        int exp;
        if (fmt != 'f') {
            exp = val.unscaledValue().toString().length() - val.scale() - 1;
        } else {
            exp = 0;
        }

        // force display of the decimal dot, if otherwise omitted
        boolean needDot = (precision == 0 && flags.get(FLAG_AL));

        // for fmt==g|G : treat trailing 0 and decimal dot specially
        boolean checkTrails = false;

        // get a sensible precision value
        if (precision < 0) {
            precision = 6;
        }

        switch (fmt) {
            case 'G': // fall through
            case 'g':
                // decrement precision, to simulate significance
                if (precision > 0) {
                    precision--;
                }

                // we have to check trailing zeroes later
                checkTrails = true;

                // exponent does not stipulate exp notation, break here
                if (exp <= precision) {
                    precision -= exp;
                    break;
                }

                // fall through for exponent handling

            case 'E': // fall through
            case 'e':
                // place the dot after the first decimal place
                val = val.movePointLeft(exp);

                // define the exponent character
                expChar = (fmt == 'e' || fmt == 'g') ? 'e' : 'E';

                break;
        }

        // only rescale if the precision is positive, may be negative
        // for g|G
        if (precision >= 0) {
            val = val.setScale(precision, BigDecimal.ROUND_HALF_UP);
        }

        // convert the number to a string
        String numStr = val.toString();

        // for g|G : check trailing zeroes
        if (checkTrails) {

            if (flags.get(FLAG_AL)) {

                // need a dot, if not existing for alternative format
                needDot |= (numStr.indexOf('.') < 0);

            } else {
                // remove trailing dots and zeros
                int dot = numStr.indexOf('.');
                if (dot >= 0) {
                    int i;
                    for (i = numStr.length() - 1; i >= dot && numStr.charAt(i) == '0';
                         i--)
                        ;
                    // if stopped at dot, remove it
                    if (i > dot) {
                        i++;
                    }
                    numStr = numStr.substring(0, i);
                }
            }
        }

        // Get a buffer with the number up to now
        StringBuffer numBuf = new StringBuffer(numStr);

        // if we need a decimal dot, add it
        if (needDot) {
            numBuf.append('.');
        }

        // we have an exponent to add
        if (expChar != 0) {
            numBuf.append(expChar);
            numBuf.append(exp < 0 ? '-' : '+');
            if (exp < 10) {
                numBuf.append('0');
            }
            numBuf.append(exp);
        }

        // define the number's sign as the prefix for later formatting
        String prefStr;
        if (num < 0) {
            prefStr = "-";
        } else if (flags.get(FLAG_SI)) {
            prefStr = "+";
        } else {
            prefStr = "";
        }

        // now format it and up we go
        return format(buf, numBuf.toString(), prefStr, width, flags);
    }

    /**
     * Formats the <code>String</code> appending to the
     * <code>StringBuffer</code> at least the <code>String</code> and
     * justifying according to the flags.
     * <p/>
     * The flags will be interpreted as follows :<br>
     * * if {@link #FLAG_LJ} is set, append blanks for left justification<br>
     * * else if {@link #FLAG_ZE} is set, insert zeroes between prefStr and str
     * * else prepend blanks for right justification
     *
     * @param buf     The <code>StringBuffer</code> to append the formatted result
     *                to.
     * @param str     The <code>String</code> to be appended with surrounding
     *                blanks, zeroes and <code>prefStr</code> depending on the
     *                <code>flags</code>. This is usually the real string to print
     *                like "ape" or "4.5E99".
     * @param prefStr An optional prefix <code>String</code> to be appended
     *                in front of the <code>String</code>. This is usually the prefix
     *                string for numeric <code>str</code> values, for example
     *                "-", "0x", or "+". The reason for this separation is that the
     *                {@link #FLAG_ZE} flag will insert zeroes between the prefix and
     *                the string itself to fill the field to the width.
     * @param width   The minimal field width. If the field width is larger than
     *                the sum of the lengths of <code>str</code> and
     *                <code>prefStr</code>, blanks or zeroes will be prepended or
     *                appended according to the flags value.
     * @param flags   The flags indicating where blanks or zeroes will be filled.
     *                See above for the interpretation of flags.
     * @throws NullPointerException if any of <code>buf</code>,
     *                              <code>str</code>, <code>prefStr</code> or <code>flags</code>
     *                              is <code>null</code>.
     */
    private static StringBuffer format(StringBuffer buf, String str,
                                       String prefStr, int width, BitSet flags) {

        int numFill = width - prefStr.length() - str.length();
        int preZero = 0;
        int preBlank = 0;
        int postBlank = 0;

        if (flags.get(FLAG_LJ)) {
            postBlank = numFill;
        } else if (flags.get(FLAG_ZE)) {
            preZero = numFill;
        } else {
            preBlank = numFill;
        }

        for (; preBlank > 0; preBlank--) buf.append(' ');
        buf.append(prefStr);
        for (; preZero > 0; preZero--) buf.append('0');
        buf.append(str);
        for (; postBlank > 0; postBlank--) buf.append(' ');

        return buf;
    }
}