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
package org.apache.jackrabbit.core.query.lucene.fulltext;

import java.io.IOException;

/**
 * Modified version of <code>org.apache.lucene.queryParser.FastCharStream</code>
 * based on a <code>String</code> input.
 * <p/>
 * An efficient implementation of JavaCC's CharStream interface. <p>Note that
 * this does not do line-number counting, but instead keeps track of the
 * character position of the token in the input, as required by Lucene's {@link
 * org.apache.lucene.analysis.Token} API.
 */
public final class FastCharStream implements CharStream {

    /**
     * Next char to read.
     */
    private int position;

    /**
     * Offset in String for current token.
     */
    private int tokenStart;

    /**
     * The input String.
     */
    private String input;

    /**
     * Constructs from a String.
     */
    public FastCharStream(String input) {
        this.input = input;
    }

    /**
     * @inheritDoc
     */
    public char readChar() throws IOException {
        if (position >= input.length()) {
            throw new IOException("read past eof");
        }
        return input.charAt(position++);
    }

    /**
     * @inheritDoc
     */
    public char BeginToken() throws IOException {
        tokenStart = position;
        return readChar();
    }

    /**
     * @inheritDoc
     */
    public void backup(int amount) {
        position -= amount;
    }

    /**
     * @inheritDoc
     */
    public String GetImage() {
        return input.substring(tokenStart, position);
    }

    /**
     * @inheritDoc
     */
    public char[] GetSuffix(int len) {
        char[] value = new char[len];
        for (int i = 0; i < len; i++) {
            value[i] = input.charAt(position - len + i);
        }
        return value;
    }

    /**
     * @inheritDoc
     */
    public void Done() {
    }

    /**
     * @inheritDoc
     */
    public int getColumn() {
        return position;
    }

    /**
     * @inheritDoc
     */
    public int getLine() {
        return 1;
    }

    /**
     * @inheritDoc
     */
    public int getEndColumn() {
        return position;
    }

    /**
     * @inheritDoc
     */
    public int getEndLine() {
        return 1;
    }

    /**
     * @inheritDoc
     */
    public int getBeginColumn() {
        return tokenStart;
    }

    /**
     * @inheritDoc
     */
    public int getBeginLine() {
        return 1;
    }
}
