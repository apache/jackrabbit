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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.Constants;
import org.apache.jackrabbit.core.NamespaceResolver;
import org.apache.log4j.Logger;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * <code>TargetImportHandler</code> serves as the base class for the concrete
 * classes <code>{@link DocViewImportHandler}</code> and
 * <code>{@link SysViewImportHandler}</code>.
 */
abstract class TargetImportHandler extends DefaultHandler implements Constants {

    private static Logger log = Logger.getLogger(TargetImportHandler.class);

    protected final Importer importer;
    protected final NamespaceResolver nsContext;

    protected TargetImportHandler(Importer importer,
                                  NamespaceResolver nsContext) {
        this.importer = importer;
        this.nsContext = nsContext;
    }

    protected void disposePropertyValues(Importer.PropInfo prop) {
        Importer.TextValue[] vals = prop.getValues();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof AppendableValue) {
                try {
                    ((AppendableValue) vals[i]).dispose();
                } catch (IOException ioe) {
                    log.warn("error while disposing temporary value appender",
                            ioe);
                }
            }
        }
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>AppendableValue</code> represents a serialized value that is
     * appendable.
     */
    public interface AppendableValue extends Importer.TextValue {
        public void append(char[] chars, int start, int length)
                throws IllegalStateException, IOException;

        /**
         * @throws IOException
         */
        public void close() throws IOException;

        /**
         * @throws IOException
         */
        public void dispose() throws IOException;
    }

    /**
     * <code>StringValue</code> represents an immutable serialized value.
     */
    protected class StringValue implements Importer.TextValue {

        private final String value;

        /**
         * Constructs a new <code>StringValue</code> representing the given
         * value.
         *
         * @param value
         */
        protected StringValue(String value) {
            this.value = value;
        }

        //--------------------------------------------------------< TextValue >
        /**
         * {@inheritDoc}
         */
        public long length() {
            return value.length();
        }

        /**
         * {@inheritDoc}
         */
        public String retrieve() {
            return value;
        }

        /**
         * {@inheritDoc}
         */
        public Reader reader() {
            return new StringReader(value);
        }
    }

    /**
     * <code>StringBufferValue</code> represents an appendable serialized value
     * that is internally backed by a <code>StringBuffer</code>.
     */
    protected class StringBufferValue implements AppendableValue {

        private final StringBuffer buffer;

        /**
         * Constructs a new empty <code>StringBufferValue</code>.
         */
        protected StringBufferValue() {
            buffer = new StringBuffer();
        }

        //--------------------------------------------------------< TextValue >
        /**
         * {@inheritDoc}
         */
        public long length() {
            return buffer.length();
        }

        /**
         * {@inheritDoc}
         */
        public String retrieve() {
            return buffer.toString();
        }

        /**
         * {@inheritDoc}
         */
        public Reader reader() {
            return new StringReader(buffer.toString());
        }

        //--------------------------------------------------< AppendableValue >
        /**
         * {@inheritDoc}
         */
        public void append(char[] chars, int start, int length) {
            buffer.append(chars, start, length);
        }

        /**
         * {@inheritDoc}
         */
        public void close() {
            // nop
        }

        /**
         * {@inheritDoc}
         */
        public void dispose() {
            buffer.setLength(0);
        }
    }

    /**
     * <code>CLOBValue</code> represents an appendable serialized value
     * that is internally backed by a temporary file.
     */
    protected class CLOBValue implements AppendableValue {

        private File tmpFile;
        private Writer writer;

        protected CLOBValue() throws IOException {
            tmpFile = File.createTempFile("bin", null);
            writer = new FileWriter(tmpFile);
        }

        //--------------------------------------------------------< TextValue >
        /**
         * {@inheritDoc}
         */
        public long length() throws IllegalStateException, IOException {
            if (tmpFile == null) {
                throw new IllegalStateException();
            }
            return tmpFile.length();
        }

        /**
         * {@inheritDoc}
         */
        public String retrieve() throws IllegalStateException, IOException {
            Reader reader = reader();
            char[] chunk = new char[8192];
            int read;
            StringBuffer buf = new StringBuffer();
            while ((read = reader.read(chunk)) > -1) {
                buf.append(chunk, 0, read);
            }
            return buf.toString();
        }

        /**
         * {@inheritDoc}
         */
        public Reader reader() throws IllegalStateException, IOException {
            if (tmpFile == null) {
                throw new IllegalStateException();
            }
            return new FileReader(tmpFile);
        }

        //--------------------------------------------------< AppendableValue >
        /**
         * {@inheritDoc}
         */
        public void append(char[] chars, int start, int length)
                throws IllegalStateException, IOException {
            if (writer == null) {
                throw new IllegalStateException();
            }
            writer.write(chars, start, length);
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            if (writer != null) {
                writer.close();
                writer = null;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void dispose() throws IOException {
            close();
            if (tmpFile != null) {
                tmpFile.delete();
                tmpFile = null;
            }
        }
    }
}
