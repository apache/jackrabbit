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
package org.apache.jackrabbit.spi.commons.value;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.QValue;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.apache.jackrabbit.uuid.UUID;

/**
 * <code>QValueFactoryImpl</code>...
 */
public final class QValueFactoryImpl implements QValueFactory {

    private static final QValueFactory INSTANCE = new QValueFactoryImpl();

    private static final PathFactory PATH_FACTORY = PathFactoryImpl.getInstance();
    private static final NameFactory NAME_FACTORY = NameFactoryImpl.getInstance();

    /**
     * the default encoding
     */
    private static final String DEFAULT_ENCODING = "UTF-8";

    private QValueFactoryImpl() {
    }

    public static QValueFactory getInstance() {
        return INSTANCE;
    }

    //------------------------------------------------------< QValueFactory >---
    /**
     * @see QValueFactory#create(String, int)
     */
    public QValue create(String value, int type) throws RepositoryException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }

        try {
            switch (type) {
                case PropertyType.BOOLEAN:
                    return (Boolean.valueOf(value).booleanValue()) ?
                            QValueImpl.TRUE :
                            QValueImpl.FALSE;
                case PropertyType.DATE: {
                        Calendar cal = ISO8601.parse(value);
                        if (cal == null) {
                            throw new ValueFormatException("not a valid date: " + value);
                        }
                        return new DateQValue(cal);
                    }
                case PropertyType.DOUBLE:
                    return new QValueImpl(Double.valueOf(value));
                case PropertyType.LONG:
                    return new QValueImpl(Long.valueOf(value));
                case PropertyType.PATH:
                    return new QValueImpl(PATH_FACTORY.create(value));
                case PropertyType.NAME:
                    return new QValueImpl(NAME_FACTORY.create(value));
                case PropertyType.STRING:
                case PropertyType.REFERENCE:
                    return new QValueImpl(value, type);
                case PropertyType.BINARY:
                    return new BinaryQValue(value.getBytes(DEFAULT_ENCODING));
                // default: invalid type specified -> see below.
            }
        } catch (IllegalArgumentException ex) {
            // given String value cannot be converted to Long/Double/Path/Name
            throw new ValueFormatException(ex);
        } catch (UnsupportedEncodingException ex) {
            throw new RepositoryException(ex);
        }

        // invalid type specified:
        throw new IllegalArgumentException("illegal type " + type);
    }

    /**
     * @see QValueFactory#create(Calendar)
     */
    public QValue create(Calendar value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        // Calendar is not constant, must create a clone
        return new DateQValue((Calendar) value.clone());
    }

    /**
     * @see QValueFactory#create(double)
     */
    public QValue create(double value) {
        return new QValueImpl(new Double(value));
    }

    /**
     * @see QValueFactory#create(long)
     */
    public QValue create(long value) {
        return new QValueImpl(new Long(value));
    }

    /**
     * @see QValueFactory#create(boolean)
     */
    public QValue create(boolean value) {
        if (value) {
            return QValueImpl.TRUE;
        } else {
            return QValueImpl.FALSE;
        }
    }

    /**
     * @see QValueFactory#create(Name)
     */
    public QValue create(Name value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValueImpl(value);
    }

    /**
     * @see QValueFactory#create(Path)
     */
    public QValue create(Path value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new QValueImpl(value);
    }

    /**
     * @see QValueFactory#create(byte[])
     */
    public QValue create(byte[] value) {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#create(InputStream)
     */
    public QValue create(InputStream value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#create(File)
     */
    public QValue create(File value) throws IOException {
        if (value == null) {
            throw new IllegalArgumentException("Cannot create QValue from null value.");
        }
        return new BinaryQValue(value);
    }

    /**
     * @see QValueFactory#computeAutoValues(QPropertyDefinition)
     */
    public QValue[] computeAutoValues(QPropertyDefinition propertyDefinition) throws RepositoryException {
        Name nodeType = propertyDefinition.getDeclaringNodeType();
        Name name = propertyDefinition.getName();

        if (NameConstants.NT_HIERARCHYNODE.equals(nodeType) && NameConstants.JCR_CREATED.equals(name)) {
            return new QValue[] { create(Calendar.getInstance()) };
        } else if (NameConstants.NT_RESOURCE.equals(nodeType) && NameConstants.JCR_LASTMODIFIED.equals(name)) {
            return new QValue[] { create(Calendar.getInstance()) };
        } else if (NameConstants.MIX_REFERENCEABLE.equals(nodeType) && NameConstants.JCR_UUID.equals(name)) {
            return new QValue[] { create(UUID.randomUUID().toString(), PropertyType.STRING) };
        } else {
            throw new RepositoryException("createFromDefinition not implemented for: " + name);
        }
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * <code>QValue</code> implementation for all valid <code>PropertyType</code>s
     * except for BINARY.
     * @see QValueFactoryImpl.BinaryQValue
     */
    private static class QValueImpl implements QValue, Serializable {

        private static final QValue TRUE = new QValueImpl(Boolean.TRUE);

        private static final QValue FALSE = new QValueImpl(Boolean.FALSE);

        private final Object val;
        private final int type;

        private QValueImpl(Object value, int type) {
            val = value;
            this.type = type;
        }

        private QValueImpl(String value, int type) {
            if (!(type == PropertyType.STRING || type == PropertyType.REFERENCE)) {
                throw new IllegalArgumentException();
            }
            val = value;
            this.type = type;
        }

        private QValueImpl(Long value) {
            val = value;
            type = PropertyType.LONG;
        }

        private QValueImpl(Double value) {
            val = value;
            type = PropertyType.DOUBLE;
        }

        private QValueImpl(Boolean value) {
            val = value;
            type = PropertyType.BOOLEAN;
        }

        private QValueImpl(Name value) {
            val = value;
            type = PropertyType.NAME;
        }

        private QValueImpl(Path value) {
            val = value;
            type = PropertyType.PATH;
        }

        //---------------------------------------------------------< QValue >---
        /**
         * @see QValue#getType()
         */
        public int getType() {
            return type;
        }

        /**
         * @see QValue#getLength()
         */
        public long getLength() throws RepositoryException {
            return getString().length();
        }

        /**
         * @see QValue#getString()
         */
        public String getString() throws RepositoryException {
            return val.toString();
        }

        /**
         * @see QValue#getStream()
         */
        public InputStream getStream() throws RepositoryException {
            try {
                // convert via string
                return new ByteArrayInputStream(getString().getBytes(QValueFactoryImpl.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                throw new RepositoryException(QValueFactoryImpl.DEFAULT_ENCODING + " is not supported encoding on this platform", e);
            }
        }

        /**
         * @see QValue#getName()
         */
        public Name getName() throws RepositoryException {
            if (type == PropertyType.NAME) {
                return (Name) val;
            } else {
                try {
                    return NAME_FACTORY.create(getString());
                } catch (IllegalArgumentException e) {
                    throw new ValueFormatException("not a valid Name value: " + getString(), e);
                }
            }
        }

        /**
         * @see QValue#getCalendar()
         */
        public Calendar getCalendar() throws RepositoryException {
            if (type == PropertyType.DATE) {
                return (Calendar) ((Calendar) val).clone();
            } else if (type == PropertyType.DOUBLE) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                cal.setTimeInMillis(((Double) val).longValue());
                return cal;
            } else if (type == PropertyType.LONG) {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+00:00"));
                cal.setTimeInMillis(((Long) val).longValue());
                return cal;
            } else {
                Calendar cal = ISO8601.parse(getString());
                if (cal == null) {
                    throw new ValueFormatException("not a date string: " + getString());
                }
                else {
                    return cal;
                }
            }
        }

        /**
         * @see QValue#getDouble()
         */
        public double getDouble() throws RepositoryException {
            if (type == PropertyType.DOUBLE) {
                return ((Double) val).doubleValue();
            } else if (type == PropertyType.DATE) {
                return ((Calendar) val).getTimeInMillis();
            } else {
                try {
                    return Double.parseDouble(getString());
                } catch (NumberFormatException ex) {
                    throw new ValueFormatException("not a double: " + getString(), ex);
                }
            }
        }

        /**
         * @see QValue#getLong()
         */
        public long getLong() throws RepositoryException {
            if (type == PropertyType.LONG) {
                return ((Long) val).longValue();
            } else if (type == PropertyType.DOUBLE) {
                return ((Double) val).longValue();
            } else if (type == PropertyType.DATE) {
                return ((Calendar) val).getTimeInMillis();
            } else {
                try {
                    return Long.parseLong(getString());
                } catch (NumberFormatException ex) {
                    throw new ValueFormatException("not a long: " + getString(), ex);
                }
            }
        }

        /**
         * @throws RepositoryException
         * @see QValue#getBoolean()
         */
        public boolean getBoolean() throws RepositoryException {
            if (type == PropertyType.BOOLEAN) {
                return ((Boolean) val).booleanValue();
            } else {
                return Boolean.valueOf(getString()).booleanValue();
            }
        }

        /**
         * @see QValue#getPath()
         */
        public Path getPath() throws RepositoryException {
            if (type == PropertyType.PATH) {
                return (Path) val;
            } else {
                try {
                    return PATH_FACTORY.create(getString());
                } catch (IllegalArgumentException e) {
                    throw new ValueFormatException("not a valid Path: " + getString(), e);
                }
            }
        }

        /**
         * @see QValue#discard()
         */
        public void discard() {
            // nothing to do
        }

        //---------------------------------------------------------< Object >---
        /**
         * Returns the string representation of this internal value.
         *
         * @return string representation of this internal value
         */
        public String toString() {
            return val.toString();
        }

        /**
         *
         * @param obj
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof QValueImpl) {
                QValueImpl other = (QValueImpl) obj;
                return type == other.type && val.equals(other.val);
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

    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * Extension for values of type {@link PropertyType#DATE}.
     */
    private static class DateQValue extends QValueImpl {

        private final String formattedStr;

        private DateQValue(Calendar value) {
            super(value, PropertyType.DATE);
            formattedStr = ISO8601.format(value);
        }

        /**
         * @return The formatted String of the internal Calendar value.
         * @throws RepositoryException
         * @see QValue#getString()
         * @see ISO8601#format(Calendar)
         */
        public String getString() throws RepositoryException {
            return formattedStr;
        }

        /**
         * @param obj
         * @return true if the given Object is a <code>DateQValue</code> with an
         * equal String representation.
         * @see Object#equals(Object)
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof DateQValue) {
                DateQValue other = (DateQValue) obj;
                return formattedStr.equals(other.formattedStr);
            }
            return false;
        }

        /**
         * @return the hashCode of the formatted String of the Calender value.
         * @see Object#hashCode()
         */
        public int hashCode() {
            return formattedStr.hashCode();
        }
    }

    //--------------------------------------------------------< Inner Class >---
    /**
     * <code>BinaryQValue</code> represents a binary <code>Value</code> which is
     * backed by a resource or byte[]. Unlike <code>BinaryValue</code> it has no
     * state, i.e. the <code>getStream()</code> method always returns a fresh
     * <code>InputStream</code> instance.
     */
    private static class BinaryQValue implements QValue, Serializable {
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
        private transient File file;

        /**
         * flag indicating if this instance represents a <i>temporary</i> value
         * whose dynamically allocated resources can be explicitly freed on
         * {@link #discard()}.
         */
        private transient boolean temp;

        /**
         * Buffer for small-sized data
         */
        private byte[] buffer = BinaryQValue.EMPTY_BYTE_ARRAY;

        /**
         * Converted text
         */
        private transient String text = null;

        /**
         * Creates a new <code>BinaryQValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The new instance represents a <i>temporary</i> value whose dynamically
         * allocated resources will be freed explicitly on {@link #discard()}.
         *
         * @param in stream to be represented as a <code>BinaryQValue</code> instance
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private BinaryQValue(InputStream in) throws IOException {
            this(in, true);
        }

        /**
         * Creates a new <code>BinaryQValue</code> instance from an
         * <code>InputStream</code>. The contents of the stream is spooled
         * to a temporary file or to a byte buffer if its size is smaller than
         * {@link #MAX_BUFFER_SIZE}.
         * <p/>
         * The <code>temp</code> parameter governs whether dynamically allocated
         * resources will be freed explicitly on {@link #discard()}. Note that any
         * dynamically allocated resources (temp file/buffer) will be freed
         * implicitly once this instance has been gc'ed.
         *
         * @param in stream to be represented as a <code>BinaryQValue</code> instance
         * @param temp flag indicating whether this instance represents a
         *             <i>temporary</i> value whose resources can be explicitly freed
         *             on {@link #discard()}.
         * @throws IOException if an error occurs while reading from the stream or
         *                     writing to the temporary file
         */
        private BinaryQValue(InputStream in, boolean temp) throws IOException {
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
                    } else if (len + read > BinaryQValue.MAX_BUFFER_SIZE) {
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
                in.close();
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
         * Creates a new <code>BinaryQValue</code> instance from a
         * <code>byte[]</code> array.
         *
         * @param bytes byte array to be represented as a <code>BinaryQValue</code>
         *              instance
         */
        private BinaryQValue(byte[] bytes) {
            buffer = bytes;
            file = null;
            // this instance is not backed by a temporarily allocated buffer
            temp = false;
        }

        /**
         * Creates a new <code>BinaryQValue</code> instance from a <code>File</code>.
         *
         * @param file file to be represented as a <code>BinaryQValue</code> instance
         * @throws IOException if the file can not be read
         */
        private BinaryQValue(File file) throws IOException {
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

        //---------------------------------------------------------< QValue >---
        /**
         * @see QValue#getType()
         */
        public int getType() {
            return PropertyType.BINARY;
        }

        /**
         * Returns the length of this <code>BinaryQValue</code>.
         *
         * @return The length, in bytes, of this <code>BinaryQValue</code>,
         *         or -1L if the length can't be determined.
         * @see QValue#getLength()
         */
        public long getLength() {
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
         * @see QValue#getString()
         */
        public String getString() throws RepositoryException {
            if (text == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    spool(out);
                    byte[] data = out.toByteArray();
                    text = new String(data, QValueFactoryImpl.DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException e) {
                    throw new RepositoryException(QValueFactoryImpl.DEFAULT_ENCODING
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
         * @see QValue#getStream()
         */
        public InputStream getStream() throws RepositoryException {
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
         * @see QValue#getName()
         */
        public Name getName() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        /**
         * @see QValue#getCalendar()
         */
        public Calendar getCalendar() throws RepositoryException {
             Calendar cal = ISO8601.parse(getString());
             if (cal == null) {
                 throw new ValueFormatException("not a date string: " + getString());
             } else {
                 return cal;
             }
        }

        /**
         * @see QValue#getDouble()
         */
        public double getDouble() throws RepositoryException {
            try {
                return Double.parseDouble(getString());
            } catch (NumberFormatException ex) {
                throw new ValueFormatException(ex);
            }
        }

        /**
         * @see QValue#getLong()
         */
        public long getLong() throws RepositoryException {
            try {
                return Long.parseLong(getString());
            } catch (NumberFormatException ex) {
                throw new ValueFormatException(ex);
            }
        }

        /**
         * @see QValue#getBoolean()
         */
        public boolean getBoolean() throws RepositoryException {
            return Boolean.valueOf(getString()).booleanValue();
        }

        /**
         * @see QValue#getPath()
         */
        public Path getPath() throws RepositoryException {
            throw new UnsupportedOperationException();
        }

        /**
         * Frees temporarily allocated resources such as temporary file, buffer, etc.
         * If this <code>BinaryQValue</code> is backed by a persistent resource
         * calling this method will have no effect.
         * @see QValue#discard()
         */
        public void discard() {
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

        //-----------------------------------------------< java.lang.Object >---
        /**
         * Returns a string representation of this <code>BinaryQValue</code>
         * instance. The string representation of a resource backed value is
         * the path of the underlying resource. If this instance is backed by an
         * in-memory buffer the generic object string representation of the byte
         * array will be used instead.
         *
         * @return A string representation of this <code>BinaryQValue</code> instance.
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
            if (obj instanceof BinaryQValue) {
                BinaryQValue other = (BinaryQValue) obj;
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

        //----------------------------------------------------------------------
        /**
         * Spools the contents of this <code>BinaryQValue</code> to the given
         * output stream.
         *
         * @param out output stream
         * @throws RepositoryException if the input stream for this
         *                             <code>BinaryQValue</code> could not be obtained
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

        //-----------------------------< Serializable >-------------------------

        private void writeObject(ObjectOutputStream out)
                throws IOException {
            out.defaultWriteObject();
            // write hasFile marker
            out.writeBoolean(file != null);
            // then write file if necessary
            if (file != null) {
                byte[] buffer = new byte[4096];
                int bytes;
                InputStream stream = new FileInputStream(file);
                while ((bytes = stream.read(buffer)) >= 0) {
                    // Write a segment of the input stream
                    if (bytes > 0) {
                        // just to ensure that no 0 is written
                        out.writeInt(bytes);
                        out.write(buffer, 0, bytes);
                    }
                }
                // Write the end of stream marker
                out.writeInt(0);
                // close stream
                stream.close();
            }
        }

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            boolean hasFile = in.readBoolean();
            if (hasFile) {
                file = File.createTempFile("binary-qvalue", "bin");

                OutputStream out = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                for (int bytes = in.readInt(); bytes > 0; bytes = in.readInt()) {
                    if (buffer.length < bytes) {
                        buffer = new byte[bytes];
                    }
                    in.readFully(buffer, 0, bytes);
                    out.write(buffer, 0, bytes);
                }
                out.close();
            }
            // deserialized value is always temp
            temp = true;
        }

    }

}
