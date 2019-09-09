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
package org.apache.jackrabbit.commons.json;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Stack;

/**
 * <code>JsonParser</code> parses and validates the JSON object passed upon 
 * {@link #parse(String)} or {@link #parse(InputStream, String)} and notifies
 * the specified <code>JsonHandler</code>
 */
public class JsonParser {

    private static final String NULL = "null";
    private static final int EOF = -1;

    private static final int KEY_START = 1;
    private static final int VALUE_START = 2;
    private static final int VALUE = 4;

    private static final Integer OBJECT = new Integer(8);
    private static final Integer ARRAY = new Integer(32);

    /* the handler */
    private final JsonHandler handler;

    /**
     * Create a new <code>JSONParser</code> with the specified <code>JSONHandler</code>.
     *
     * @param jsonHandler A <code>JSONHandler</code>
     */
    public JsonParser(JsonHandler jsonHandler) {
        handler = jsonHandler;
    }

    /**
     *
     * @param str String to be parsed
     * @throws IOException If an error occurs.
     */
    public void parse(String str) throws IOException {
        parse(new BufferedReader(new StringReader(str)));
    }

    /**
     *
     * @param input InputStream to be parsed.
     * @param charSetName Name of the charset to be used.
     * @throws IOException If an error occurs.
     */
    public void parse(InputStream input, String charSetName) throws IOException {
        parse(new BufferedReader(new InputStreamReader(input, charSetName)));
    }

    /**
     *
     * @param reader The reader
     * @throws IOException If an error occurs.
     */
    public void parse(Reader reader) throws IOException {

        //StringBuffer key = new StringBuffer();
        StringBuffer value = new StringBuffer();

        int state;
        Stack complexVStack = new Stack();

        int next = reader.read();
        if (next == '{') {
            handler.object();
            complexVStack.push(OBJECT);
            state = KEY_START;
            next = readIgnoreWhitespace(reader);
        } else {
            throw new IOException("JSON object must start with a '{'");
        }


        while (next != EOF) {
            switch (state) {

                case KEY_START:
                    if (next == '"') {
                        String key = nextString(reader, '\"');
                        next = readIgnoreWhitespace(reader);
                        if (next == ':') {
                            handler.key(key);
                            state = VALUE_START;
                        } else {
                            throw new IOException("Key-Value pairs must be separated by ':'");
                        }
                        next = readIgnoreWhitespace(reader);
                    } else if (next == '}') {
                        // empty object
                        state = VALUE;
                    } else {
                        throw new IOException("Key must be in String format (double quotes)");
                    }
                    break;

                case VALUE_START:
                    if (next == '[') {
                        handler.array();
                        complexVStack.push(ARRAY);
                        // status still value_start
                        next = readIgnoreWhitespace(reader);
                    } else if (next == '{') {
                        handler.object();
                        complexVStack.push(OBJECT);
                        state = KEY_START;
                        next = readIgnoreWhitespace(reader);
                    } else if (next == '\"') {
                        handler.value(nextString(reader, '\"'));
                        next = readIgnoreWhitespace(reader);
                        if (!(next == ',' || next == ']' || next == '}')) {
                            throw new IOException("Invalid json format");
                        }
                    } else {
                        // start of boolean/long/double/null value
                        // will be notified as key-value pair
                        state = VALUE;
                    }
                    break;

                case VALUE:
                    if (next == '"') {
                        throw new IOException("Invalid json format");
                    } else if (next == ',') {
                        state = (complexVStack.peek() == OBJECT) ? KEY_START : VALUE_START;
                        value = resetValue(value);
                        next = readIgnoreWhitespace(reader);
                    } else if (next == ']') {
                        if (complexVStack.pop() != ARRAY) {
                            throw new IOException("Invalid json format: Unexpected array termination.");
                        }
                        value = resetValue(value);
                        handler.endArray();

                        next = readIgnoreWhitespace(reader);
                        if (!(next == ',' || next == '}' || next == ']')) {
                            throw new IOException("Invalid json format");
                        }
                    } else if (next == '}') {
                        if (complexVStack.pop() != OBJECT) {
                            throw new IOException("Invalid json format: Unexpected object termination.");
                        }
                        value = resetValue(value);
                        handler.endObject();

                        next = readIgnoreWhitespace(reader);
                        if (!(next == ',' || next == '}' || next == ']' || next == EOF)) {
                            throw new IOException("Invalid json format");
                        }
                    } else {
                        // simple value
                        value.append((char) next);
                        next = reader.read();
                    }
                    break;
            }
        }

        // EOF reached -> minimal validation check
        if (value.length() != 0) {
            throw new IOException("Invalid json format");
        }
    }

    /**
     * Return the characters up to the next close quote character.
     * Backslash processing is done. The formal JSON format does not
     * allow strings in single quotes, but an implementation is allowed to
     * accept them.
     *
     * @param r The reader.
     * @param quote The quoting character, either
     *      <code>"</code>&nbsp;<small>(double quote)</small> or
     *      <code>'</code>&nbsp;<small>(single quote)</small>.
     * @return      A String.
     * @throws IOException Unterminated string.
     */
    private static String nextString(Reader r, char quote) throws IOException {
        int c;
        StringBuffer sb = new StringBuffer();
        for (;;) {
            c = r.read();
            switch (c) {
            case EOF:
            case '\n':
            case '\r':
                throw new IOException("Unterminated string");
            case '\\':
                c = r.read();
                switch (c) {
                case 'b':
                    sb.append('\b');
                    break;
                case 't':
                    sb.append('\t');
                    break;
                case 'n':
                    sb.append('\n');
                    break;
                case 'f':
                    sb.append('\f');
                    break;
                case 'r':
                    sb.append('\r');
                    break;
                case 'u':
                    sb.append((char)Integer.parseInt(next(r, 4), 16));
                    break;
                case 'x' :
                    sb.append((char) Integer.parseInt(next(r, 2), 16));
                    break;
                default:
                    sb.append((char) c);
                }
                break;
            default:
                if (c == quote) {
                    return sb.toString();
                }
                sb.append((char) c);
            }
        }
    }

    private static String next(Reader r, int n) throws IOException {
        StringBuffer b = new StringBuffer(n);
        while (n-- > 0) {
            int c = r.read();
            if (c < 0) {
                throw new EOFException();
            }
            b.append((char) c);
        }
        return b.toString();
    }

    /**
     * Get the next char in the string, skipping whitespace.
     *
     * @param reader The reader
     * @return A character, or -1 if there are no more characters.
     * @throws IOException If an error occurs.
     */
    private static int readIgnoreWhitespace(Reader reader) throws IOException {
        int next;
        do {
            next = reader.read();
        } while (next == ' ' || next == '\n' || next == '\r' || next == '\t');
        return next;
    }

    private StringBuffer resetValue(StringBuffer value) throws IOException {
        if (value != null && value.length() > 0) {
            String v = value.toString();
            if (NULL.equals(v)) {
                handler.value(null);
            } else if (v.equalsIgnoreCase("true")) {
                handler.value(true);
            } else if (v.equalsIgnoreCase("false")) {
                handler.value(false);
            } else if (v.indexOf('.') > -1) {
                double d = Double.parseDouble(v);
                handler.value(d);
            } else {
                long l = Long.parseLong(v);
                handler.value(l);
            }
        }
        return new StringBuffer();
    }
}