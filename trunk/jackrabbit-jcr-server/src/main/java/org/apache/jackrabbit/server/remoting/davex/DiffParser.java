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
package org.apache.jackrabbit.server.remoting.davex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/** <code>DiffParser</code>... */
class DiffParser {

    // TODO: review again: currently all line-sep. chars before an diff-char are
    // TODO: ignored unless they are escaped in way the handler understands (e.g.
    // TODO: JSON does: \\r for \r).
    // TODO: in contrast line sep. at the end of the string are treated as value.
    // TODO: ... similar: line sep. following by non-diff symbol.

    private final DiffHandler handler;

    private static final int EOF = -1;

    private static final char SYMBOL_ADD_NODE = '+';
    private static final char SYMBOL_MOVE = '>';
    private static final char SYMBOL_REMOVE = '-';
    private static final char SYMBOL_SET_PROPERTY = '^';

    private static final int STATE_START_LINE = 0;
    private static final int STATE_START_TARGET = 1;
    private static final int STATE_TARGET = 2;
    private static final int STATE_START_VALUE = 3;
    private static final int STATE_VALUE = 4;

    /**
     *
     * @param handler
     */
    public DiffParser(DiffHandler handler) {
        this.handler = handler;
    }

    public void parse(String str) throws IOException, DiffException {
        parse(new BufferedReader(new StringReader(str)));
    }

    public void parse(InputStream input, String charSetName) throws IOException, DiffException {
        parse(new BufferedReader(new InputStreamReader(input, charSetName)));
    }

    public void parse(Reader reader) throws IOException, DiffException {
        int action = -1;
        String path = null;

        StringBuffer lineSeparator = null;
        StringBuffer bf = null;

        int state = STATE_START_LINE;
        int next = reader.read();

        while (next != EOF) {
            switch (state) {
                case STATE_START_LINE:
                    if (isSymbol(next)) {
                        // notify the last action read
                        if (action > -1) {
                            informAction(action, path, bf);
                        }
                        // ... and  start recording the next action
                        action = next;
                        bf = null;
                        lineSeparator = null;
                        state = STATE_START_TARGET;
                    } else if (isLineSeparator(next)) {
                        // still line-separator -> append c to the lineSeparator
                        // buffer and keep state set to STATE_START_LINE
                        if (lineSeparator == null) {
                            throw new DiffException("Invalid start of new line.");
                        } else {
                            lineSeparator.append((char) next);
                        }
                    } else if (lineSeparator != null && bf != null) {
                        // append the collected return/linefeed chars as part
                        // of the value read and continued reading value.
                        bf.append(lineSeparator);
                        bf.append((char) next);
                        lineSeparator = null;
                        state = STATE_VALUE;
                    } else {
                        throw new DiffException("Invalid start of new line.");
                    }
                    break;

                case STATE_START_TARGET:
                    if (Character.isWhitespace((char) next) || next == ':') {
                        throw new DiffException("Invalid start of target path '" + next + "'");
                    }
                    bf = new StringBuffer();
                    bf.append((char) next);
                    state = STATE_TARGET;
                    break;

                case STATE_TARGET:
                    if (Character.isWhitespace((char) next) && endsWithDelim(bf)) {
                        // a sequence of 'wsp:wsp' indicates the delimiter between
                        // the target path and the diff value.
                        path = bf.substring(0, bf.lastIndexOf(":")).trim();
                        state = STATE_START_VALUE;
                        // reset buffer
                        bf = null;
                    } else {
                        // continue reading the path into the buffer.
                        bf.append((char) next);
                    }
                    break;

                case STATE_START_VALUE:
                    if (isLineSeparator(next)) {
                        lineSeparator = new StringBuffer();
                        lineSeparator.append((char) next);
                        bf = new StringBuffer();
                        state = STATE_START_LINE;
                    } else {
                        bf = new StringBuffer();
                        bf.append((char) next);
                        state = STATE_VALUE;
                    }
                    break;

                case STATE_VALUE:
                    if (isLineSeparator(next)) {
                        lineSeparator = new StringBuffer();
                        lineSeparator.append((char) next);
                        state = STATE_START_LINE;
                    } else {
                        bf.append((char) next);
                        // keep state set to STATE_VALUE
                    }
                    break;

            }
            // read the next character.
            next = reader.read();
        }

        // a diff ending after a command or within the target is invalid.
        if (state == STATE_START_TARGET || state == STATE_TARGET) {
            throw new DiffException("Invalid end of DIFF string: missing separator and value.");
        }
        if (state == STATE_START_VALUE ) {
            // line separator AND buffer must be null
            if (!(lineSeparator == null && bf == null)) {
                throw new DiffException("Invalid end of DIFF string.");
            }
        }

        // append eventual remaining line-separators to the value
        if (lineSeparator != null) {
            bf.append(lineSeparator);
        }
        // notify the last action read
        informAction(action, path, bf);
    }

    private void informAction(int action, String path, StringBuffer diffVal) throws DiffException {
        if (path == null) {
            throw new DiffException("Missing path for action " + action + "(diffValue = '"+ diffVal +"')");
        }
        String value = (diffVal == null) ? null : diffVal.toString();
        switch (action) {
            case SYMBOL_ADD_NODE:
                handler.addNode(path, value);
                break;
            case SYMBOL_SET_PROPERTY:
                handler.setProperty(path, value);
                break;
            case SYMBOL_MOVE:
                handler.move(path, value);
                break;
            case SYMBOL_REMOVE:
                handler.remove(path, value);
                break;
            default:
                throw new DiffException("Invalid action " + action);
        }
    }

    private static boolean isSymbol(int c) {
        return c == SYMBOL_ADD_NODE || c == SYMBOL_SET_PROPERTY || c == SYMBOL_MOVE || c == SYMBOL_REMOVE;
    }

    private static boolean isLineSeparator(int c) {
        return c == '\n' || c == '\r';

    }
    private static boolean endsWithDelim(StringBuffer bf) {
        if (bf.length() < 2) {
            return false;
        } else {
            return ':' == bf.charAt(bf.length()-1) && Character.isWhitespace(bf.charAt(bf.length()-2));
        }
    }
}
