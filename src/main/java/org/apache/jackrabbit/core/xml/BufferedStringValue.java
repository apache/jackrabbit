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
package org.apache.jackrabbit.core.xml;

import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.apache.jackrabbit.value.ValueHelper;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;

/**
 * <code>BufferedStringValue</code> represents an appendable
 * serialized value that is either buffered in-memory or backed
 * by a temporary file if its size exceeds a certain limit.
 * <p/>
 * <b>Important:</b> Note that in order to free resources
 * <code>{@link #dispose()}</code> should be called as soon as
 * <code>BufferedStringValue</code> instance is not used anymore.
 */
class BufferedStringValue implements TextValue {

    private static Logger log = LoggerFactory.getLogger(BufferedStringValue.class);

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

    private final NamePathResolver nsContext;

    /**
     * Constructs a new empty <code>BufferedStringValue</code>.
     * @param nsContext
     */
    protected BufferedStringValue(NamePathResolver nsContext) {
        buffer = new char[0x2000];
        bufferPos = 0;
        tmpFile = null;
        writer = null;
        this.nsContext = nsContext;
    }

    /**
     * Returns the length of the serialized value.
     *
     * @return the length of the serialized value
     * @throws IOException if an I/O error occurs
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
     * Retrieves the serialized value.
     *
     * @return the serialized value
     * @throws IOException if an I/O error occurs
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
     * Returns a <code>Reader</code> for reading the serialized value.
     *
     * @return a <code>Reader</code> for reading the serialized value.
     * @throws IOException if an I/O error occurs
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

    /**
     * Append a portion of an array of characters.
     *
     * @param chars  the characters to be appended
     * @param start  the index of the first character to append
     * @param length the number of characters to append
     * @throws IOException if an I/O error occurs
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
                    int bufferSize =
                            BUFFER_INCREMENT * (((bufferPos + length) / BUFFER_INCREMENT) + 1);
                    char[] newBuffer = new char[bufferSize];
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
     * Close this value. Once a value has been closed,
     * further append() invocations will cause an IOException to be thrown.
     *
     * @throws IOException if an I/O error occurs
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

    //--------------------------------------------------------< TextValue >

    public Value getValue(int targetType, NamePathResolver resolver)
            throws ValueFormatException, RepositoryException {
        try {
            if (targetType == PropertyType.NAME
                    || targetType == PropertyType.PATH) {
                // NAME and PATH require special treatment because
                // they depend on the current namespace context
                // of the xml document

                // convert serialized value to InternalValue using
                // current namespace context of xml document
                InternalValue ival =
                    InternalValue.create(ValueHelper.convert(
                            retrieve(), targetType, ValueFactoryImpl.getInstance()), nsContext);
                // convert InternalValue to Value using this
                // session's namespace mappings
                return ival.toJCRValue(resolver);
            } else if (targetType == PropertyType.BINARY) {
                if (length() < 0x10000) {
                    // < 65kb: deserialize BINARY type using String
                    return ValueHelper.deserialize(retrieve(), targetType, false, ValueFactoryImpl.getInstance());
                } else {
                    // >= 65kb: deserialize BINARY type using Reader
                    Reader reader = reader();
                    try {
                        return ValueHelper.deserialize(reader, targetType, false, ValueFactoryImpl.getInstance());
                    } finally {
                        reader.close();
                    }
                }
            } else {
                // all other types
                return ValueHelper.deserialize(retrieve(), targetType, false, ValueFactoryImpl.getInstance());
            }
        } catch (IOException e) {
            String msg = "failed to retrieve serialized value";
            log.debug(msg, e);
            throw new RepositoryException(msg, e);
        }
    }

    public InternalValue getInternalValue(int type)
            throws ValueFormatException, RepositoryException {
        try {
            if (type == PropertyType.BINARY) {
                // base64 encoded BINARY type;
                // decode using Reader
                if (length() < 0x10000) {
                    // < 65kb: deserialize BINARY type in memory
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    Base64.decode(retrieve(), baos);
                    // no need to close ByteArrayOutputStream
                    //baos.close();
                    return InternalValue.create(baos.toByteArray());
                } else {
                    // >= 65kb: deserialize BINARY type
                    // using Reader and temporay file
                    if (InternalValue.USE_DATA_STORE) {
                        Base64ReaderInputStream in = new Base64ReaderInputStream(reader());
                        return InternalValue.createTemporary(in);
                    }
                    TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                    File tmpFile = fileFactory.createTransientFile("bin", null, null);
                    FileOutputStream out = new FileOutputStream(tmpFile);
                    Reader reader = reader();
                    try {
                        Base64.decode(reader, out);
                    } finally {
                        reader.close();
                        out.close();
                    }
                    return InternalValue.create(tmpFile);
                }
            } else {
                // convert serialized value to InternalValue using
                // current namespace context of xml document
                return InternalValue.create(ValueHelper.convert(
                        retrieve(), type, ValueFactoryImpl.getInstance()), nsContext);
            }
        } catch (IOException e) {
            throw new RepositoryException("Error accessing property value", e);
        }
    }
    
    private static class Base64ReaderInputStream extends InputStream {

        private static final int BUFFER_SIZE = 1024;
        private final char[] chars;
        private final ByteArrayOutputStream out;
        private final Reader reader;
        private int pos;
        private int remaining;
        private byte[] buffer;

        public Base64ReaderInputStream(Reader reader) {
            chars = new char[BUFFER_SIZE];
            this.reader = reader;
            out = new ByteArrayOutputStream(BUFFER_SIZE);
        }

        private void fillBuffer() throws IOException {
            int len = reader.read(chars, 0, BUFFER_SIZE);
            if (len < 0) {
                remaining = -1;
                return;
            }
            Base64.decode(chars, 0, len, out);
            buffer = out.toByteArray();
            pos = 0;
            remaining = buffer.length;
            out.reset();
        }

        public int read() throws IOException {
            if (remaining == 0) {
                fillBuffer();
            }
            if (remaining < 0) {
                return -1;
            }
            remaining--;
            return buffer[pos++] & 0xff;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        if (buffer != null) {
            buffer = null;
            bufferPos = 0;
        } else if (tmpFile != null) {
            try {
                writer.close();
                tmpFile.delete();
                tmpFile = null;
                writer = null;
            } catch (IOException e) {
                log.warn("Problem disposing property value", e);
            }
        } else {
            log.warn("this instance has already been disposed");
        }
    }
}
