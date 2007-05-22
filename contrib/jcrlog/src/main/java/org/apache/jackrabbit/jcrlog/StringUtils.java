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
package org.apache.jackrabbit.jcrlog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Credentials;
import javax.jcr.ItemVisitor;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventListener;

/**
 * Some String manipulations / formatting functions used by this tool.
 *
 * @author Thomas Mueller
 *
 */
public class StringUtils {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS Z");

    static String[] arraySplit(String s, char separatorChar) {
        if (s == null) {
            return null;
        }
        if (s.length() == 0) {
            return new String[0];
        }
        ArrayList list = new ArrayList();
        StringBuffer buff = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == separatorChar) {
                list.add(buff.toString());
                buff.setLength(0);
            } else if (c == '\\' && i < s.length() - 1) {
                buff.append(s.charAt(++i));
            } else {
                buff.append(c);
            }
        }
        list.add(buff.toString());
        String[] array = new String[list.size()];
        list.toArray(array);
        return array;
    }

    private static String addAsterix(String s, int index) {
        if (s != null && index < s.length()) {
            s = s.substring(0, index) + "[*]" + s.substring(index);
        }
        return s;
    }

    private static IllegalArgumentException getFormatException(String s, int i) {
        return new IllegalArgumentException("String format error: "
                + addAsterix(s, i));
    }

    /**
     * Parse a a Java string.
     *
     * @param s the string formatted as a Java string
     * @return the parsed string
     */
    public static String javaDecode(String s) {
        StringBuffer buff = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                break;
            } else if (c == '\\') {
                if (i >= s.length()) {
                    throw getFormatException(s, s.length() - 1);
                }
                c = s.charAt(++i);
                switch (c) {
                case 't':
                    buff.append('\t');
                    break;
                case 'r':
                    buff.append('\r');
                    break;
                case 'n':
                    buff.append('\n');
                    break;
                case 'b':
                    buff.append('\b');
                    break;
                case 'f':
                    buff.append('\f');
                    break;
                case '"':
                    buff.append('"');
                    break;
                case '\\':
                    buff.append('\\');
                    break;
                case 'u': {
                    try {
                        c = (char) (Integer.parseInt(s.substring(i + 1, i + 5),
                                16));
                    } catch (NumberFormatException e) {
                        throw getFormatException(s, i);
                    }
                    i += 4;
                    buff.append(c);
                    break;
                }
                default:
                    if (c >= '0' && c <= '9') {
                        try {
                            c = (char) (Integer.parseInt(s.substring(i, i + 3),
                                    8));
                        } catch (NumberFormatException e) {
                            throw getFormatException(s, i);
                        }
                        i += 2;
                        buff.append(c);
                    } else {
                        throw getFormatException(s, i);
                    }
                }
            } else {
                buff.append(c);
            }
        }
        return buff.toString();
    }

    private static String javaEncode(String s) {
        StringBuffer buff = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            // case '\b':
            // // BS backspace
            // // not supported in properties files
            // buff.append("\\b");
            // break;
            case '\t':
                // HT horizontal tab
                buff.append("\\t");
                break;
            case '\n':
                // LF linefeed
                buff.append("\\n");
                break;
            case '\f':
                // FF form feed
                buff.append("\\f");
                break;
            case '\r':
                // CR carriage return
                buff.append("\\r");
                break;
            case '"':
                // double quote
                buff.append("\\\"");
                break;
            case '\\':
                // backslash
                buff.append("\\\\");
                break;
            default:
                int ch = (c & 0xffff);
                if (ch >= ' ' && (ch < 0x80)) {
                    buff.append(c);
                    // not supported in properties files
                    // } else if(ch < 0xff) {
                    // buff.append("\\");
                    // // make sure it's three characters (0x200 is octal
                    // 1000)
                    // buff.append(Integer.toOctalString(0x200 |
                    // ch).substring(1));
                } else {
                    buff.append("\\u");
                    // make sure it's four characters
                    buff.append(Integer.toHexString(0x10000 | ch).substring(1));
                }
            }
        }
        return buff.toString();
    }

    static String quoteString(String result) {
        if (result == null) {
            return "null";
        }
        return "\"" + javaEncode(result) + "\"";
    }

    static String quoteArray(Class clazz, String name, Object[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer buff = new StringBuffer("new ");
        buff.append(name);
        buff.append("[]{");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buff.append(", ");
            }
            buff.append(quote(clazz, array[i]));
        }
        buff.append("}");
        return buff.toString();
    }

    private static String quoteLongArray(long[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer buff = new StringBuffer("new long[]{");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buff.append(", ");
            }
            buff.append(array[i] + "L");
        }
        buff.append("}");
        return buff.toString();
    }

    private static String quoteStringArray(String[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer buff = new StringBuffer("new String[]{");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                buff.append(", ");
            }
            buff.append(quoteString(array[i]));
        }
        buff.append("}");
        return buff.toString();
    }

    static String quoteCredentials(Credentials credentials) {
        if (credentials == null) {
            return null;
        } else if (credentials instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) credentials;
            String user = sc.getUserID();
            // TODO log attribute names
            // String[] attributeNames = sc.getAttributeNames();
            // TODO option to log the password
            // String pwd = "- hidden -"; // new String(sc.getPassword());
            return JcrUtils.CLASSNAME + ".createSimpleCredentials("
                    + quoteString(user) + ")";
        } else {
            // TODO serialize credentials
            return "?" + credentials.getClass().getName() + ":"
                    + credentials.toString() + "?";
        }
    }

    static String quoteArgs(Class[] argClasses, Object[] args) {
        if (args == null) {
            return "";
        }
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                buff.append(", ");
            }
            buff.append(StringUtils.quote(argClasses[i], args[i]));
        }
        return buff.toString();
    }

    /**
     * Format an object as Java source code.
     *
     * @param o the object
     * @return the formatted string
     */
    public static String quoteSimple(Object o) {
        if (o instanceof String) {
            return quoteString((String) o);
        } else if (o.getClass().isArray()) {
            if (o instanceof String[]) {
                return quoteStringArray((String[]) o);
            } else if (o instanceof long[]) {
                return quoteLongArray((long[]) o);
            } else {
                return null;
            }
        } else if (o instanceof Integer) {
            return o.toString();
        } else if (o instanceof Long) {
            return o.toString() + "L";
        } else if (o instanceof Boolean) {
            return o.toString();
        } else if (o instanceof Double) {
            return o.toString() + "d";
        } else if (o instanceof Calendar) {
            return quoteCalendar((Calendar) o);
        }
        return null;
    }

    /**
     * Format an object as Java source code.
     *
     * @param clazz the class
     * @param o the object
     * @return the formatted string
     */
    public static String quote(Class clazz, Object o) {
        if (o == null) {
            if (clazz == void.class) {
                return "";
            }
            if (clazz.isArray()) {
                return "(" + clazz.getComponentType().getName() + "[])null";
            } else {
                return "(" + clazz.getName() + ")null";
            }
        } else if (o instanceof Credentials) {
            return quoteCredentials((Credentials) o);
        } else if (o instanceof String) {
            return quoteString((String) o);
        } else if (o.getClass().isArray()) {
            if (o instanceof String[]) {
                return quoteStringArray((String[]) o);
            } else if (o instanceof long[]) {
                return quoteLongArray((long[]) o);
            } else {
                Class ct = o.getClass().getComponentType();
                LogObject.InterfaceDef idef = LogObject.getInterface(ct);
                if (idef == null) {
                    throw new Error("unsupported class: " + o.getClass());
                }
                return quoteArray(idef.interfaceClass, idef.className,
                        (Object[]) o);
            }
        } else if (o instanceof Integer) {
            if (clazz == int.class) {
                return o.toString();
            } else {
                // TODO doesn't work yet in player
                return "new Integer(" + o.toString() + ")";
            }
        } else if (o instanceof Long) {
            if (clazz == long.class) {
                return o.toString() + "L";
            } else {
                // TODO doesn't work yet in player
                return "new Long(" + o.toString() + ")";
            }
        } else if (o instanceof Boolean) {
            if (clazz == boolean.class) {
                return o.toString();
            } else {
                // TODO doesn't work yet in player
                return "Boolean.valueOf(" + o.toString() + ")";
            }
        } else if (o instanceof EventListener) {
            return JcrUtils.CLASSNAME
                    + ".createDoNothingEventListener()";
        } else if (o instanceof ItemVisitor) {
            return JcrUtils.CLASSNAME
                    + ".createDoNothingItemVisitor()";
        } else if (o instanceof Proxy) {
            LogObject obj = (LogObject) Proxy.getInvocationHandler(o);
            return obj.getObjectName();
        } else if (o instanceof LogObject) {
            return ((LogObject) o).getObjectName();
        } else if (o instanceof Double) {
            return o.toString() + "d";
        } else if (o instanceof Calendar) {
            return quoteCalendar((Calendar) o);
        } else if (o instanceof OutputStream) {
            return quoteOutputStream((OutputStream) o);
        } else if (o instanceof InputStream) {
            return quoteInputStream((InputStream) o);
        } else if (o instanceof org.xml.sax.ContentHandler) {
            return JcrUtils.CLASSNAME
            + ".createDoNothingEventListener()";
        } else {
            StringBuffer buff = new StringBuffer();
            try {
                String s = serializeToString(o);
                buff.append("/* UNKNOWN OBJECT: ");
                buff.append(o.toString());
                buff.append(" */ (");
                buff.append(o.getClass().getName());
                buff.append(")");
                buff.append(JcrUtils.CLASSNAME);
                buff.append(".deserializeFromString(\"");
                buff.append(s);
                buff.append("\")");
            } catch (Exception e) {
                buff.append("/* UNKNOWN UNSERIALIZABLE OBJECT: ");
                buff.append(o.toString());
                buff.append(" (");
                buff.append(o.getClass().getName());
                buff.append(") */ null");
            }
            return buff.toString();
        }
    }

    private static String quoteOutputStream(OutputStream stream) {
        StringBuffer buff = new StringBuffer(JcrUtils.CLASSNAME);
        buff.append(".getOutputStream()");
        return buff.toString();
    }

    private static String quoteInputStream(InputStream stream) {
        StringBuffer buff = new StringBuffer(JcrUtils.CLASSNAME);
        buff.append(".getInputStream(\"");
        buff.append(stream.toString());
        buff.append("\")");
        return buff.toString();
    }

    private static String quoteCalendar(Calendar cal) {
        StringBuffer buff = new StringBuffer(JcrUtils.CLASSNAME);
        buff.append(".getCalendar(\"");
        buff.append(DATE_FORMAT.format(cal.getTime()));
        buff.append("\")");
        return buff.toString();
    }

    static Calendar parseCalendar(String s) {
        Date d;
        try {
            d = DATE_FORMAT.parse(s);
        } catch (ParseException e) {
            throw new Error("Can not parse date: " + s);
        }
        // TODO calendar: timezones are not set here
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return cal;
    }

    static String convertBytesToString(byte[] value) {
        StringBuffer buff = new StringBuffer(value.length * 2);
        for (int i = 0; value != null && i < value.length; i++) {
            int c = value[i] & 0xff;
            buff.append(Integer.toHexString((c >> 4) & 0xf));
            buff.append(Integer.toHexString(c & 0xf));
        }
        return buff.toString();
    }

    static byte[] convertStringToBytes(String s) throws IllegalArgumentException, NumberFormatException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex String with odd number of characters: " + s);
        }
        len /= 2;
        byte[] buff = new byte[len];
        for (int i = 0; i < len; i++) {
            String t = s.substring(i + i, i + i + 2);
            buff[i] = (byte) Integer.parseInt(t, 16);
        }
        return buff;
    }

    static String serializeToString(Object obj) throws IOException {
        byte[] bytes = serialize(obj);
        return convertBytesToString(bytes);
    }

    private static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    /**
     * Check if this string looks like a UUID.
     *
     * @param id
     * @return if the id is formatted correctly
     */
    public static boolean isUUID(String id) {
        // example: "deab5a60-01d3-472f-bc86-4d2bdeb06470"
        if (id == null || id.length() != 36) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            switch(i) {
            case 8:
            case 13:
            case 18:
            case 23:
                if (ch != '-') {
                    return false;
                }
                break;
            default:
                if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f') && (ch < 'A' || ch > 'F')) {
                    return false;
                }
            }
        }
        return true;
    }

}
