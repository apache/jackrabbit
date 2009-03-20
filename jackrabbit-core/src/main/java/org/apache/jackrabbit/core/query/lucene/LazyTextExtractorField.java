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

import org.apache.lucene.document.AbstractField;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.TokenStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.Reader;
import java.io.IOException;

/**
 * <code>LazyTextExtractorField</code> implements a Lucene field with a String
 * value that is lazily initialized from a given {@link Reader}. In addition
 * this class provides a method to find out whether the purpose of the reader
 * is to extract text and whether the extraction process is already finished.
 *
 * @see #isExtractorFinished()
 */
public class LazyTextExtractorField extends AbstractField {

    /**
     * The serial version UID.
     */
    private static final long serialVersionUID = -2707986404659820071L;

    /**
     * The logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(LazyTextExtractorField.class);

    /**
     * The reader from where to read the text extract.
     */
    private final Reader reader;

    /**
     * The extract as obtained lazily from {@link #reader}.
     */
    private String extract;

    /**
     * Creates a new <code>LazyTextExtractorField</code> with the given
     * <code>name</code>.
     *
     * @param name the name of the field.
     * @param reader the reader where to obtain the string from.
     * @param store when set <code>true</code> the string value is stored in the
     *          index.
     * @param withOffsets when set <code>true</code> a term vector with offsets
     *          is written into the index.
     */
    public LazyTextExtractorField(String name,
                                  Reader reader,
                                  boolean store,
                                  boolean withOffsets) {
        super(name,
                store ? Field.Store.YES : Field.Store.NO,
                Field.Index.ANALYZED,
                withOffsets ? Field.TermVector.WITH_OFFSETS : Field.TermVector.NO);
        this.reader = reader;
    }

    /**
     * @return the string value of this field.
     */
    public String stringValue() {
        if (extract == null) {
            StringBuffer textExtract = new StringBuffer();
            char[] buffer = new char[1024];
            int len;
            try {
                while ((len = reader.read(buffer)) > -1) {
                    textExtract.append(buffer, 0, len);
                }
            } catch (IOException e) {
                log.warn("Exception reading value for field: "
                        + e.getMessage());
                log.debug("Dump:", e);
            } finally {
                IOUtils.closeQuietly(reader);
            }
            extract = textExtract.toString();
        }
        return extract;
    }

    /**
     * @return always <code>null</code>.
     */
    public Reader readerValue() {
        return null;
    }

    /**
     * @return always <code>null</code>.
     */
    public byte[] binaryValue() {
        return null;
    }

    /**
     * @return always <code>null</code>.
     */
    public TokenStream tokenStreamValue() {
        return null;
    }

    /**
     * @return <code>true</code> if the underlying reader is ready to provide
     *          extracted text.
     */
    public boolean isExtractorFinished() {
        if (reader instanceof TextExtractorReader) {
            return ((TextExtractorReader) reader).isExtractorFinished();
        }
        return true;
    }

    /**
     * Disposes this field and closes the underlying reader.
     *
     * @throws IOException if an error occurs while closing the reader.
     */
    public void dispose() throws IOException {
        reader.close();
    }
}
