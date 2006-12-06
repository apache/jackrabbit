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
package org.apache.jackrabbit.value;

import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.uuid.UUID;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.TransientFileFactory;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;
import javax.jcr.Property;
import javax.jcr.ValueFormatException;
import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Arrays;

/**
* <code>QValue</code> represents the qualified format of a property value.
 * <p/>
 * The following table specifies the qualified format for every property type:
 * <pre>
 * <table>
 * <tr><b>PropertyType</b><td></td><td><b>Internal Format</b></td></tr>
 * <tr>STRING<td></td><td>String</td></tr>
 * <tr>LONG<td></td><td>Long</td></tr>
 * <tr>DOUBLE<td></td><td>Double</td></tr>
 * <tr>DATE<td></td><td>Calendar</td></tr>
 * <tr>BOOLEAN<td></td><td>Boolean</td></tr>
 * <tr>NAME<td></td><td>QName</td></tr>
 * <tr>PATH<td></td><td>Path</td></tr>
 * <tr>BINARY<td></td><td>BLOBFileValue</td></tr>
 * <tr>REFERENCE<td></td><td>String</td></tr>
 * </table>
 * </pre>
 */
public class QValue {

    public static final QValue[] EMPTY_ARRAY = new QValue[0];

    public static final QValue BOOLEAN_TRUE = create(true);
    public static final QValue BOOLEAN_FALSE = create(false);

    /**
     * the default encoding
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    private final Object val;
    private final int type;

    //----------------------------------------------------< Factory methods >---
    /**
     * @param value
     * @return
     */
    public static QValue create(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(value, PropertyType.STRING);
    }

    /**
     * @param values
     * @return
     */
    public static QValue[] create(String[] values) {
        QValue[] ret = new QValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = create(values[i]);
        }
        return ret;
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(String value, int type) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        switch (type) {
            case PropertyType.BOOLEAN:
                return new QValue(Boolean.valueOf(value).booleanValue());
            case PropertyType.DATE:
                return new QValue(ISO8601.parse(value));
            case PropertyType.DOUBLE:
                return new QValue(Double.valueOf(value).doubleValue());
            case PropertyType.LONG:
                return new QValue(Long.valueOf(value).longValue());
            case PropertyType.REFERENCE:
                // NOTE: references are not forced to represent a UUID object
                return new QValue(value, PropertyType.REFERENCE);
            case PropertyType.PATH:
                return new QValue(Path.valueOf(value));
            case PropertyType.NAME:
                return new QValue(QName.valueOf(value));
            case PropertyType.STRING:
                return new QValue(value, PropertyType.STRING);
            case PropertyType.BINARY:
                throw new IllegalArgumentException("this method does not support the type PropertyType.BINARY");
            default:
                throw new IllegalArgumentException("illegal type");
        }
    }

    /**
     * @param values
     * @return
     */
    public static QValue[] create(String[] values, int type) {
        QValue[] ret = new QValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = QValue.create(values[i], type);
        }
        return ret;
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(long value) {
        return new QValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(double value) {
        return new QValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(Calendar value) {
        return new QValue(ISO8601.format(value), PropertyType.DATE);
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(boolean value) {
        return new QValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static QValue create(InputStream value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @param type
     * @return
     * @throws IOException
     */
    public static QValue create(InputStream value, int type) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        switch (type) {
            case PropertyType.BINARY:
                return new QValue(new BLOBFileValue(value));

            case PropertyType.BOOLEAN:
            case PropertyType.DATE:
            case PropertyType.DOUBLE:
            case PropertyType.LONG:
            case PropertyType.REFERENCE:
            case PropertyType.PATH:
            case PropertyType.NAME:
            case PropertyType.STRING:
                // convert stream value to String
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = value.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                    byte[] data = out.toByteArray();
                    String text = new String(data, DEFAULT_ENCODING);
                    return create(text, type);

                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(DEFAULT_ENCODING + " not supported on this platform: " + e.getMessage());
                } catch (IOException e) {
                    throw new IllegalArgumentException("conversion from stream to string failed: " + e.getMessage());
                } finally {
                    value.close();
                }
            default:
                throw new IllegalArgumentException("illegal type");
        }
    }

    /**
     * @param values
     * @return
     */
    public static QValue[] create(InputStream[] values, int type) throws IOException {
        QValue[] ret = new QValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = QValue.create(values[i], type);
        }
        return ret;
    }

    /**
     * @param value
     * @param temp
     * @return
     * @throws IOException
     */
    public static QValue create(InputStream value, boolean temp) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(new BLOBFileValue(value, temp));
    }

    /**
     * @param value
     * @return
     * @throws IOException
     */
    public static QValue create(File value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(new BLOBFileValue(value));
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(QName value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(value);
    }

    /**
     * @param values
     * @return
     */
    public static QValue[] create(QName[] values) {
        QValue[] ret = new QValue[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = create(values[i]);
        }
        return ret;
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(Path value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(value);
    }

    /**
     * @param value
     * @return
     */
    public static QValue create(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValue(value);
    }

    //--------------------------------------------------------------------------
    /**
     * @return
     */
    public int getType() {
        return type;
    }

    /**
     *
     * @return
     */
    public String getString() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            return ((BLOBFileValue) val).getString();
        } else {
            return val.toString();
        }
    }

    /**
     *
     * @return
     * @throws RepositoryException
     */
    public InputStream getStream() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            return ((BLOBFileValue) val).getStream();
        } else {
            try {
                // convert via string
                return new ByteArrayInputStream(getString().getBytes(DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(DEFAULT_ENCODING + " is not supported encoding on this platform", e);
            }
        }
    }

    /**
     * Frees temporarily allocated resources such as temporary file, buffer, etc.
     */
    public void discard() {
        if (type == PropertyType.BINARY) {
            ((BLOBFileValue) val).discard();
        }
        // else: nothing to do.
    }

    /**
     * Returns the length of the internal value.<br>
     * NOTE: for {@link PropertyType#NAME} and {@link PropertyType#PATH} the
     * length of the internal value must not be used for indicating the length
     * of a property such as retrieved by calling {@link Property#getLength()}
     * and {@link Property#getLengths()}.
     *
     * @return length of the internal value.
     */
    public long getLength() throws RepositoryException {
        if (type == PropertyType.BINARY) {
            return ((BLOBFileValue) val).getLength();
        } else {
            return getString().length();
        }
    }

    /**
     * @return
     * @throws RepositoryException
     */
    public QValue createCopy() throws RepositoryException {
        switch (type) {
            case PropertyType.BINARY:
                try {
                    InputStream stream = ((BLOBFileValue) val).getStream();
                    try {
                        return new QValue(new BLOBFileValue(stream));
                    } finally {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                } catch (IOException ioe) {
                    throw new RepositoryException("Failed to copy binary value", ioe);
                }
            case PropertyType.BOOLEAN:
                return new QValue(((Boolean) val).booleanValue());
            case PropertyType.DATE:
                return new QValue((String) val, PropertyType.DATE);
            case PropertyType.DOUBLE:
                return new QValue(((Double) val).doubleValue());
            case PropertyType.LONG:
                return new QValue(((Long) val).longValue());
            case PropertyType.REFERENCE:
                return new QValue((String) val, PropertyType.REFERENCE);
            case PropertyType.PATH:
                return new QValue((Path) val);
            case PropertyType.NAME:
                return new QValue((QName) val);
            case PropertyType.STRING:
                return new QValue((String) val, PropertyType.STRING);
            default:
                throw new RepositoryException("Illegal internal value type");
        }
    }

    //-------------------------------------------------------------< Object >---
    /**
     * Returns the string representation of this internal value. If this is a
     * <i>binary</i> value then the path of its backing file will be returned.
     *
     * @return string representation of this internal value
     */
    public String toString() {
        return val.toString();
    }

    /**
     *
     * @param obj
     * @return
     * @see Object#equals(Object)
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof QValue) {
            QValue other = (QValue) obj;
            return val.equals(other.val) && type == other.type;
        }
        return false;
    }

    /**
     * @return the hashCode of the internal value object.
     * @see Object#hashCode()
     */
    public int hashCode() {
        return val.hashCode();
    }

    //-----------------------------------------------------< implementation >---
    private QValue(String value, int type) {
        val = value;
        this.type = type;
    }

    private QValue(QName value) {
        val = value;
        type = PropertyType.NAME;
    }

    private QValue(long value) {
        val = new Long(value);
        type = PropertyType.LONG;
    }

    private QValue(double value) {
        val = new Double(value);
        type = PropertyType.DOUBLE;
    }

    private QValue(Calendar value) {
        val = ISO8601.format(value);
        type = PropertyType.DATE;
    }

    private QValue(boolean value) {
        val = new Boolean(value);
        type = PropertyType.BOOLEAN;
    }

    private QValue(BLOBFileValue value) {
        val = value;
        type = PropertyType.BINARY;
    }

    private QValue(Path value) {
        val = value;
        type = PropertyType.PATH;
    }

    private QValue(UUID value) {
        // NOTE: reference value must not represent a UUID object
        val = value.toString();
        type = PropertyType.REFERENCE;
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * <code>BLOBFileValue</code> represents a binary <code>Value</code> which is
     * backed by a resource or byte[]. Unlike <code>BinaryValue</code> it has no
     * state, i.e. the <code>getStream()</code> method always returns a fresh
     * <code>InputStream</code> instance.
     */
    private static class BLOBFileValue {
        /**
         * empty array
         */
        private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

        /**
         * max size for keeping tmp data in memory
         */
        private static final int MAX_BUFFER_SIZE = 0x10000;

        /**
         * underlying file
         */
        private final File file;

        /**
         * flag indicating if this instance represents a <i>temporary</i> value
         * whose dynamically allocated resources can be explicitly freed on
         * {@link #discard()}.
         */
        private final boolean temp;

        /**
         * Buffer for small-sized data
         */
        private byte[] buffer = EMPTY_BYTE_ARRAY;

        /**
         * Converted text
         */
        private String text = null;

        /**
         * Creates a new <code>BLOBFileValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The new instance represents a <i>temporary</i> value whose dynamically
         * allocated resources will be freed explicitly on {@link #discard()}.
         *
         * @param in stream to be represented as a <code>BLOBFileValue</code> instance
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private BLOBFileValue(InputStream in) throws IOException {
            this(in, true);
        }

        /**
         * Creates a new <code>BLOBFileValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The <code>temp</code> parameter governs whether dynamically allocated
         * resources will be freed explicitly on {@link #discard()}. Note that any
         * dynamically allocated resources (temp file/buffer) will be freed
         * implicitly once this instance has been gc'ed.
         *
         * @param in stream to be represented as a <code>BLOBFileValue</code> instance
         * @param temp flag indicating whether this instance represents a
         *             <i>temporary</i> value whose resources can be explicitly freed
         *             on {@link #discard()}.
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private BLOBFileValue(InputStream in, boolean temp) throws IOException {
            byte[] spoolBuffer = new byte[0x2000];
            int read;
            int len = 0;
            OutputStream out = null;
            File spoolFile = null;
            try {
                while ((read = in.read(spoolBuffer)) > 0) {
                    if (out != null) {
                        // spool to temp file
                        out.write(spoolBuffer, 0, read);
                        len += read;
                    } else if (len + read > MAX_BUFFER_SIZE) {
                        // threshold for keeping data in memory exceeded;
                        // create temp file and spool buffer contents
                        TransientFileFactory fileFactory = TransientFileFactory.getInstance();
                        spoolFile = fileFactory.createTransientFile("bin", null, null);
                        out = new FileOutputStream(spoolFile);
                        out.write(buffer, 0, len);
                        out.write(spoolBuffer, 0, read);
                        buffer = null;
                        len += read;
                    } else {
                        // reallocate new buffer and spool old buffer contents
                        byte[] newBuffer = new byte[len + read];
                        System.arraycopy(buffer, 0, newBuffer, 0, len);
                        System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                        buffer = newBuffer;
                        len += read;
                    }
                }
            } finally {
                if (out != null) {
                    out.close();
                }
            }

            // init vars
            file = spoolFile;
            this.temp = temp;
            // buffer is EMPTY_BYTE_ARRAY (default value)
        }

        /**
         * Creates a new <code>BLOBFileValue</code> instance from a
         * <code>byte[]</code> array.
         *
         * @param bytes byte array to be represented as a <code>BLOBFileValue</code>
         *              instance
         */
        private BLOBFileValue(byte[] bytes) {
            buffer = bytes;
            file = null;
            // this instance is not backed by a temporarily allocated buffer
            temp = false;
        }

        /**
         * Creates a new <code>BLOBFileValue</code> instance from a <code>File</code>.
         *
         * @param file file to be represented as a <code>BLOBFileValue</code> instance
         * @throws IOException if the file can not be read
         */
        private BLOBFileValue(File file) throws IOException {
            String path = file.getCanonicalPath();
            if (!file.isFile()) {
                throw new IOException(path + ": the specified file does not exist");
            }
            if (!file.canRead()) {
                throw new IOException(path + ": the specified file can not be read");
            }
            // this instance is backed by a 'real' file
            this.file = file;
            // this instance is not backed by temporarily allocated resource/buffer
            temp = false;
            // buffer is EMPTY_BYTE_ARRAY (default value)
        }

        /**
         * Returns the length of this <code>BLOBFileValue</code>.
         *
         * @return The length, in bytes, of this <code>BLOBFileValue</code>,
         *         or -1L if the length can't be determined.
         */
        private long getLength() {
            if (file != null) {
                // this instance is backed by a 'real' file
                if (file.exists()) {
                    return file.length();
                } else {
                    return -1;
                }
            } else {
                // this instance is backed by an in-memory buffer
                return buffer.length;
            }
        }

        /**
         * Frees temporarily allocated resources such as temporary file, buffer, etc.
         * If this <code>BLOBFileValue</code> is backed by a persistent resource
         * calling this method will have no effect.
         */
        private void discard() {
            if (!temp) {
                // do nothing if this instance is not backed by temporarily
                // allocated resource/buffer
                return;
            }
            if (file != null) {
                // this instance is backed by a temp file
                file.delete();
            } else if (buffer != null) {
                // this instance is backed by an in-memory buffer
                buffer = EMPTY_BYTE_ARRAY;
            }
        }

        /**
         *
         * @return
         * @throws ValueFormatException
         * @throws IllegalStateException
         * @throws RepositoryException
         */
        private String getString()
            throws ValueFormatException, IllegalStateException,
            RepositoryException {
            if (text == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    spool(out);
                    byte[] data = out.toByteArray();
                    text = new String(data, DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    throw new RepositoryException(DEFAULT_ENCODING
                        + " not supported on this platform", e);
                } catch (IOException e) {
                    throw new ValueFormatException("conversion from stream to string failed", e);
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            return text;
        }

        /**
         *
         * @return
         * @throws IllegalStateException
         * @throws RepositoryException
         */
        private InputStream getStream()
            throws IllegalStateException, RepositoryException {
            // always return a 'fresh' stream
            if (file != null) {
                // this instance is backed by a 'real' file
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException fnfe) {
                    throw new RepositoryException("file backing binary value not found",
                        fnfe);
                }
            } else {
                return new ByteArrayInputStream(buffer);
            }
        }

        /**
         * Spools the contents of this <code>BLOBFileValue</code> to the given
         * output stream.
         *
         * @param out output stream
         * @throws RepositoryException if the input stream for this
         *                             <code>BLOBFileValue</code> could not be obtained
         * @throws IOException         if an error occurs while while spooling
         */
        private void spool(OutputStream out) throws RepositoryException, IOException {
            InputStream in;
            if (file != null) {
                // this instance is backed by a 'real' file
                try {
                    in = new FileInputStream(file);
                } catch (FileNotFoundException fnfe) {
                    throw new RepositoryException("file backing binary value not found",
                        fnfe);
                }
            } else {
                // this instance is backed by an in-memory buffer
                in = new ByteArrayInputStream(buffer);
            }
            try {
                byte[] buffer = new byte[0x2000];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {
                }
            }
        }

        //-----------------------------------------------< java.lang.Object >---
        /**
         * Returns a string representation of this <code>BLOBFileValue</code>
         * instance. The string representation of a resource backed value is
         * the path of the underlying resource. If this instance is backed by an
         * in-memory buffer the generic object string representation of the byte
         * array will be used instead.
         *
         * @return A string representation of this <code>BLOBFileValue</code> instance.
         */
        public String toString() {
            if (file != null) {
                // this instance is backed by a 'real' file
                return file.toString();
            } else {
                // this instance is backed by an in-memory buffer
                return buffer.toString();
            }
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BLOBFileValue) {
                BLOBFileValue other = (BLOBFileValue) obj;
                return ((file == null ? other.file == null : file.equals(other.file))
                    && Arrays.equals(buffer, other.buffer));
            }
            return false;
        }

        /**
         * Returns zero to satisfy the Object equals/hashCode contract.
         * This class is mutable and not meant to be used as a hash key.
         *
         * @return always zero
         * @see Object#hashCode()
         */
        public int hashCode() {
            return 0;
        }
    }
}