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
package org.apache.jackrabbit.core.query.lucene;

import java.io.IOException;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.index.Payload;

/**
 * <code>SingletonTokenStream</code> implements a token stream that wraps a
 * single value with a given property type. The property type is stored as a
 * payload on the single returned token.
 */
public final class SingletonTokenStream extends TokenStream {

    /**
     * The string value of the token.
     */
    private String value;

    /**
     * The payload of the token.
     */
    private Payload payload;

    /**
     * The term attribute of the current token
     */
    private TermAttribute termAttribute;

    /**
     * The payload attribute of the current token
     */
    private PayloadAttribute payloadAttribute;

    private boolean consumed = false;

    /**
     * Creates a new SingleTokenStream with the given value and payload.
     * 
     * @param value
     *            the string value that will be returned with the token.
     * @param payload
     *            the payload that will be attached to this token
     */
    public SingletonTokenStream(String value, Payload payload) {
        this.value = value;
        this.payload = payload;
        termAttribute = (TermAttribute) addAttribute(TermAttribute.class);
        payloadAttribute = (PayloadAttribute) addAttribute(PayloadAttribute.class);
    }

    /**
     * Creates a new SingleTokenStream with the given value and a property
     * <code>type</code>.
     * 
     * @param value
     *            the string value that will be returned with the token.
     * @param type
     *            the JCR property type.
     */
    public SingletonTokenStream(String value, int type) {
        this(value, new Payload(new PropertyMetaData(type).toByteArray()));
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (consumed) {
            return false;
        }
        clearAttributes();
        termAttribute.setTermBuffer(value);
        payloadAttribute.setPayload(payload);
        consumed = true;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        consumed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        consumed = true;
        value = null;
        payload = null;
        payloadAttribute = null;
        termAttribute = null;
    }
}
