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
package org.apache.jackrabbit.spi2davex;

import org.apache.commons.httpclient.util.EncodingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * <code>StringPart</code> extending from the default HttpClient 3.x
 * StringPart and sending the name in the Content Disposition header
 * with the specified charset.
 *
 * TODO: This class can be removed once we upgrade to HttpClient 4.x (JCR-2406)
 */
class StringPart extends org.apache.commons.httpclient.methods.multipart.StringPart {

    public StringPart(String name, String value, String charset) {
        super(name, value, charset);
    }

    @Override
    protected void sendDispositionHeader(OutputStream out) throws IOException {
        out.write(CONTENT_DISPOSITION_BYTES);
        out.write(QUOTE_BYTES);
        out.write(EncodingUtil.getBytes(getName(), getCharSet()));
        out.write(QUOTE_BYTES);
    }
}