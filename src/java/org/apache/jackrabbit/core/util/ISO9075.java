/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
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

import org.apache.jackrabbit.core.QName;
import org.apache.xerces.util.XMLChar;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Implements the encode and decode routines as specified for XML name to SQL
 * identifier conversion in ISO 9075-14:2003.<br/>
 * If a character <code>c</code> is not valid at a certain position in an XML 1.0
 * Name it is encoded in the form: '_x' + hexValueOf(c) + '_'
 * todo or is it NCName
 * <p/>
 * Note that only the local part of a {@link org.apache.jackrabbit.core.QName}
 * is encoded / decoded. A URI namespace will always be valid and does not
 * need encoding.
 */
public class ISO9075 {

    /** Pattern on an encoded character */
    private static Pattern ENCODE_PATTERN = Pattern.compile("_x\\p{XDigit}{4}_");

    /** Padding characters */
    private static char[] PADDING = new char[] {'0', '0', '0'};

    /**
     * Encodes the local part of <code>name</code> as specified in ISO 9075.
     * @param name the <code>QName</code> to encode.
     * @return the encoded <code>QName</code> or <code>name</code> if it does
     *   not need encoding.
     */
    public static QName encode(QName name) {
        String encoded = encode(name.getLocalName());
        if (encoded == name.getLocalName()) {
            return name;
        } else {
            return new QName(name.getNamespaceURI(), encoded);
        }
    }

    /**
     * Encodes <code>name</code> as specified in ISO 9075.
     * @param name the <code>String</code> to encode.
     * @return the encoded <code>String</code> or <code>name</code> if it does
     *   not need encoding.
     */
    public static String encode(String name) {
        // quick check for root node name
        if (name.length() == 0) {
            return name;
        }
        if (XMLChar.isValidName(name) && name.indexOf("_x") < 0) {
            // already valid
            return name;
        } else {
            // encode
            StringBuffer encoded = new StringBuffer();
            for (int i = 0; i < name.length(); i++) {
                if (i == 0) {
                    // first character of name
                    if (XMLChar.isNameStart(name.charAt(i))) {
                        if (name.charAt(i) == '_'
                                && name.length() > (i + 1)
                                && name.charAt(i + 1) == 'x') {
                            // '_x' must be encoded
                            encode('_', encoded);
                        } else {
                            encoded.append(name.charAt(i));
                        }
                    } else {
                        // not valid as first character -> encode
                        encode(name.charAt(i), encoded);
                    }
                } else if (!XMLChar.isName(name.charAt(i))) {
                    encode(name.charAt(i), encoded);
                } else {
                    if (name.charAt(i) == '_'
                            && name.length() > (i + 1)
                            && name.charAt(i + 1) == 'x') {
                        // '_x' must be encoded
                        encode('_', encoded);
                    } else {
                        encoded.append(name.charAt(i));
                    }
                }
            }
            return encoded.toString();
        }
    }

    /**
     * Decodes the <code>name</code>.
     * @param name the <code>QName</code> to decode.
     * @return the decoded <code>QName</code>.
     */
    public static QName decode(QName name) {
        String decoded = decode(name.getLocalName());
        if (decoded == name.getLocalName()) {
            return name;
        } else {
            return new QName(name.getNamespaceURI(), decoded.toString());
        }
    }

    /**
     * Decodes the <code>name</code>.
     * @param name the <code>String</code> to decode.
     * @return the decoded <code>String</code>.
     */
    public static String decode(String name) {
        // quick check
        if (name.indexOf("_x") < 0) {
            // not encoded
            return name;
        }
        StringBuffer decoded = new StringBuffer();
        Matcher m = ENCODE_PATTERN.matcher(name);
        while (m.find()) {
            m.appendReplacement(decoded, Character.toString((char) Integer.parseInt(m.group().substring(2, 6), 16)));
        }
        m.appendTail(decoded);
        return decoded.toString();
    }

    //-------------------------< internal >-------------------------------------

    /**
     * Encodes the character <code>c</code> as a String in the following form:
     * <code>"_x" + hex value of c + "_"</code>. Where the hex value has always
     * four digits with possibly leading zeros.
     * <p/>
     * Example: ' ' (the space character) is encoded to: _x0020_
     * @param c the character to encode
     * @param b the encoded character is appended to <code>StringBuffer</code>
     *  <code>b</code>.
     */
    private static void encode(char c, StringBuffer b) {
        b.append("_x");
        String hex = Integer.toHexString(c);
        b.append(PADDING, 0, 4 - hex.length());
        b.append(hex);
        b.append("_");
    }

}
