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
package org.apache.jackrabbit.core.fs;

import java.io.ByteArrayOutputStream;
import java.util.BitSet;

/**
 * The <code>FileSystemPathUtil</code> utility class ...
 */
public final class FileSystemPathUtil {

    private static final char[] hexTable = "0123456789abcdef".toCharArray();

    private static final char ESCAPE_CHAR = '%';

    /**
     * The list of characters that are not encoded by the <code>escapeName(String)</code>
     * and <code>unescape(String)</code> methods. They contains the characters
     * which can savely be used in file names:
     */
    public static final BitSet SAVE_NAMECHARS;

    /**
     * The list of characters that are not encoded by the <code>escapePath(String)</code>
     * and <code>unescape(String)</code> methods. They contains the characters
     * which can savely be used in file paths:
     */
    public static final BitSet SAVE_PATHCHARS;

    static {
        // build list of valid name characters
        SAVE_NAMECHARS = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            SAVE_NAMECHARS.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            SAVE_NAMECHARS.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            SAVE_NAMECHARS.set(i);
        }
        SAVE_NAMECHARS.set('-');
        SAVE_NAMECHARS.set('_');
        SAVE_NAMECHARS.set('.');

        // build list of valid path characters (inlcudes name characters)
        SAVE_PATHCHARS = (BitSet) SAVE_NAMECHARS.clone();
        SAVE_PATHCHARS.set(FileSystem.SEPARATOR_CHAR);
    }

    /**
     * private constructor
     */
    private FileSystemPathUtil() {
    }

    private static String escape(String s, BitSet saveChars) {
        byte[] bytes = s.getBytes();
        StringBuffer out = new StringBuffer(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xff;
            if (saveChars.get(c) && c != ESCAPE_CHAR) {
                out.append((char) c);
            } else {
                out.append(ESCAPE_CHAR);
                out.append(hexTable[(c >> 4) & 0x0f]);
                out.append(hexTable[(c) & 0x0f]);
            }
        }
        return out.toString();
    }

    /**
     * Encodes the specified <code>path</code>. Same as
     * <code>{@link #escapeName(String)}</code> except that the separator
     * character <b><code>/</code></b> is regarded as a legal path character
     * that needs no escaping.
     *
     * @param path the path to encode.
     * @return the escaped path
     */
    public static String escapePath(String path) {
        return escape(path, SAVE_PATHCHARS);
    }

    /**
     * Encodes the specified <code>name</code>. Same as
     * <code>{@link #escapePath(String)}</code> except that the separator character
     * <b><code>/</code></b> is regarded as an illegal character that needs
     * escaping.
     *
     * @param name the name to encode.
     * @return the escaped name
     */
    public static String escapeName(String name) {
        return escape(name, SAVE_NAMECHARS);
    }

    /**
     * Decodes the specified path/name.
     *
     * @param pathOrName the escaped path/name
     * @return the unescaped path/name
     */
    public static String unescape(String pathOrName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(pathOrName.length());
        for (int i = 0; i < pathOrName.length(); i++) {
            char c = pathOrName.charAt(i);
            if (c == ESCAPE_CHAR) {
                try {
                    out.write(Integer.parseInt(pathOrName.substring(i + 1, i + 3), 16));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException();
                }
                i += 2;
            } else {
                out.write(c);
            }
        }
        return new String(out.toByteArray());
    }

    /**
     * Returns the parent directory of the specified <code>path</code>.
     *
     * @param path a file system path denoting a directory or a file.
     * @return the parent directory.
     */
    public static String getParentDir(String path) {
        int pos = path.lastIndexOf(FileSystem.SEPARATOR_CHAR);
        if (pos > 0) {
            return path.substring(0, pos);
        }
        return FileSystem.SEPARATOR;
    }

    /**
     * Returns the name of the specified <code>path</code>.
     *
     * @param path a file system path denoting a directory or a file.
     * @return the name.
     */
    public static String getName(String path) {
        int pos = path.lastIndexOf(FileSystem.SEPARATOR_CHAR);
        if (pos != -1) {
            return path.substring(pos + 1);
        }
        return path;
    }
}
