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
package org.apache.jackrabbit.jcr2spi.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <code>TargetImportHandler</code> serves as the base class for the concrete
 * classes <code>{@link DocViewImportHandler}</code> and
 * <code>{@link SysViewImportHandler}</code>.
 */
abstract class TargetImportHandler extends DefaultHandler {

    private static Logger log = LoggerFactory.getLogger(TargetImportHandler.class);

    protected final Importer importer;
    protected final NamePathResolver resolver;

    protected TargetImportHandler(Importer importer, NamePathResolver resolver) {
        this.importer = importer;
        this.resolver = resolver;
    }

    /**
     * Disposes all instances of <code>AppendableValue</code> contained in the
     * given property info's value array.
     *
     * @param prop property info
     */
    protected void disposePropertyValues(Importer.PropInfo prop) {
        Importer.TextValue[] vals = prop.getValues();
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof AppendableValue) {
                try {
                    ((AppendableValue) vals[i]).dispose();
                } catch (IOException ioe) {
                    log.warn("error while disposing temporary value", ioe);
                    // fall through...
                }
            }
        }
    }

    //-------------------------------------------------------< ContentHandler >

    /**
     * Initializes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document starts.
     *
     * @throws SAXException if the importer can not be initialized
     * @see DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        try {
            importer.start();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    /**
     * Closes the underlying {@link Importer} instance. This method
     * is called by the XML parser when the XML document ends.
     *
     * @throws SAXException if the importer can not be closed
     * @see DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        try {
            importer.end();
        } catch (RepositoryException re) {
            throw new SAXException(re);
        }
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>AppendableValue</code> represents a serialized value that is
     * appendable.
     * <p>
     * <b>Important:</b> Note that in order to free resources
     * <code>{@link #dispose()}</code> should be called as soon as an
     * <code>AppendableValue</code> object is not used anymore.
     */
    public interface AppendableValue extends Importer.TextValue {
        /**
         * Append a portion of an array of characters.
         *
         * @param chars  the characters to be appended
         * @param start  the index of the first character to append
         * @param length the number of characters to append
         * @throws IOException if an I/O error occurs
         */
        void append(char[] chars, int start, int length)
                throws IOException;

        /**
         * Close this value. Once a value has been closed,
         * further append() invocations will cause an IOException to be thrown.
         *
         * @throws IOException if an I/O error occurs
         */
        void close() throws IOException;

        /**
         * Dispose this value, i.e. free all bound resources. Once a value has
         * been disposed, further method invocations will cause an IOException
         * to be thrown.
         *
         * @throws IOException if an I/O error occurs
         */
        void dispose() throws IOException;
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
     * <code>BufferedStringValue</code> represents an appendable
     * serialized value that is either buffered in-memory or backed
     * by a temporary file if its size exceeds a certain limit.
     * <p>
     * <b>Important:</b> Note that in order to free resources
     * <code>{@link #dispose()}</code> should be called as soon as
     * <code>BufferedStringValue</code> instance is not used anymore.
     */
    protected class BufferedStringValue implements AppendableValue {

        /**
         * max size for buffering data in memory
         */
        private static final int MAX_BUFFER_SIZE = 0x10000;
        /**
         * size of increment if capacity buffer needs to be enlarged
         */
        private static final int BUFFER_INCREMENT = 0x2000;
        /**
         * in-memory buffer
         */
        private char[] buffer;
        /**
         * current position within buffer (size of actual data in buffer)
         */
        private int bufferPos;

        /**
         * backing temporary file created when size of data exceeds
         * MAX_BUFFER_SIZE
         */
        private File tmpFile;
        /**
         * writer used to write to tmpFile; writer & tmpFile are always
         * instantiated together, i.e. they are either both null or both not null.
         */
        private Writer writer;

        /**
         * Constructs a new empty <code>BufferedStringValue</code>.
         */
        protected BufferedStringValue() {
            buffer = new char[0x2000];
            bufferPos = 0;
            tmpFile = null;
            writer = null;
        }

        //--------------------------------------------------------< TextValue >
        /**
         * {@inheritDoc}
         */
        public long length() throws IOException {
            if (buffer != null) {
                return bufferPos;
            } else if (tmpFile != null) {
                // flush writer first
                writer.flush();
                return tmpFile.length();
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }

        /**
         * {@inheritDoc}
         */
        public String retrieve() throws IOException {
            if (buffer != null) {
                return new String(buffer, 0, bufferPos);
            } else if (tmpFile != null) {
                // flush writer first
                writer.flush();
                if (tmpFile.length() > Integer.MAX_VALUE) {
                    throw new IOException("size of value is too big, use reader()");
                }
                StringBuffer sb = new StringBuffer((int) tmpFile.length());
                char[] chunk = new char[0x2000];
                int read;
                Reader reader = new FileReader(tmpFile);
                try {
                    while ((read = reader.read(chunk)) > -1) {
                        sb.append(chunk, 0, read);
                    }
                } finally {
                    reader.close();
                }
                return sb.toString();
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }

        /**
         * {@inheritDoc}
         */
        public Reader reader() throws IOException {
            if (buffer != null) {
                return new StringReader(new String(buffer, 0, bufferPos));
            } else if (tmpFile != null) {
                // flush writer first
                writer.flush();
                return new FileReader(tmpFile);
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }

        //--------------------------------------------------< AppendableValue >
        /**
         * {@inheritDoc}
         */
        public void append(char[] chars, int start, int length)
                throws IOException {
            if (buffer != null) {
                if (bufferPos + length > MAX_BUFFER_SIZE) {
                    // threshold for keeping data in memory exceeded;
                    // create temp file and spool buffer contents
                    TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                    tmpFile = fileFactory.createTransientFile("txt", null, null);
                    final FileOutputStream fout = new FileOutputStream(tmpFile);
                    writer = new OutputStreamWriter(fout) {
                        @Override
                        public void flush() throws IOException {
                            // flush this writer
                            super.flush();
                            // force synchronization with underlying file
                            fout.getFD().sync();
                        }
                    };
                    writer.write(buffer, 0, bufferPos);
                    writer.write(chars, start, length);
                    // reset fields
                    buffer = null;
                    bufferPos = 0;
                } else {
                    if (bufferPos + length > buffer.length) {
                        // reallocate new buffer and spool old buffer contents
                        char[] newBuffer = new char[ bufferPos + length + BUFFER_INCREMENT];
                        System.arraycopy(buffer, 0, newBuffer, 0, bufferPos);
                        buffer = newBuffer;
                    }
                    System.arraycopy(chars, start, buffer, bufferPos, length);
                    bufferPos += length;
                }
            } else if (tmpFile != null) {
                writer.write(chars, start, length);
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }

        /**
         * {@inheritDoc}
         */
        public void close() throws IOException {
            if (buffer != null) {
                // nop
            } else if (tmpFile != null) {
                writer.close();
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }

        /**
         * {@inheritDoc}
         */
        public void dispose() throws IOException {
            if (buffer != null) {
                buffer = null;
                bufferPos = 0;
            } else if (tmpFile != null) {
                writer.close();
                tmpFile.delete();
                tmpFile = null;
                writer = null;
            } else {
                throw new IOException("this instance has already been disposed");
            }
        }
    }
}
