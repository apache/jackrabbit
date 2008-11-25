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
package org.apache.jackrabbit.extractor;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringReader;

/**
 * Text extractor for Rich Text Format (RTF)
 */
public class RTFTextExtractor extends AbstractTextExtractor {

    /**
     * Logger instance.
     */
    private static final Logger logger =
        LoggerFactory.getLogger(RTFTextExtractor.class);

    /**
     * Creates a new <code>RTFTextExtractor</code> instance.
     */
    public RTFTextExtractor() {
        super(new String[]{"application/rtf", "text/rtf"});
    }

    //-------------------------------------------------------< TextExtractor >

    /**
     * {@inheritDoc}
     */
    public Reader extractText(InputStream stream,
                              String type,
                              String encoding) throws IOException {

        try {
            RTFEditorKit rek = new RTFEditorKit();
            DefaultStyledDocument doc = new DefaultStyledDocument();
            rek.read(stream, doc, 0);
            String text = doc.getText(0, doc.getLength());
            return new StringReader(text);
        } catch (Throwable t) {
            logger.warn("Failed to extract RTF text content", t);
            return new StringReader("");
        } finally {
            stream.close();
        }
    }
}
