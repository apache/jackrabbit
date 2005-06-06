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
package org.apache.jackrabbit.value;

import org.apache.jackrabbit.util.Base64;
import org.apache.jackrabbit.util.Text;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.ValueFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.ByteArrayInputStream;

/**
 * The <code>ValueHelper</code> class provides several <code>Value</code>
 * related utility methods.
 */
public class ValueHelper {

    /**
     * Empty private constructor (avoid instanciation).
     */
    private ValueHelper() {
    }

    /**
     * Serializes the given value to a <code>String</code>. The serialization
     * format is the same as used by Document & System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link javax.jcr.Value#getString()}</code> will be used.
     *
     * @param value the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     * as <code>"_x0020_"</code> within he output string.
     * @return a string representation of the given value.
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static String serialize(Value value, boolean encodeBlanks)
            throws IllegalStateException, RepositoryException {
        StringWriter writer = new StringWriter();
        try {
            serialize(value, encodeBlanks, writer);
        } catch (IOException ioe) {
            throw new RepositoryException("failed to serialize value",
                    ioe);
        }
        return writer.toString();
    }

    /**
     * Outputs the serialized value to a <code>Writer</code>. The serialization
     * format is the same as used by Document & System View XML, i.e.
     * binary values will be Base64-encoded whereas for all others
     * <code>{@link javax.jcr.Value#getString()}</code> will be used for
     * serialization.
     *
     * @param value        the value to be serialized
     * @param encodeBlanks if <code>true</code> space characters will be encoded
     *                     as <code>"_x0020_"</code> within he output string.
     * @param writer       writer to output the encoded data
     * @throws IllegalStateException if the given value is in an illegal state
     * @throws IOException           if an i/o error occured during the
     *                               serialization
     * @throws RepositoryException   if an error occured during the serialization.
     */
    public static void serialize(Value value, boolean encodeBlanks,
                                 Writer writer)
            throws IllegalStateException, IOException, RepositoryException {
        if (value.getType() == PropertyType.BINARY) {
            // binary data, base64 encoding required;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain space characters
            InputStream in = value.getStream();
            try {
                Base64.encode(in, writer);
                // no need to close StringWriter
                //writer.close();
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } else {
            String textVal = value.getString();
            if (encodeBlanks) {
                // enocde blanks in string
                textVal = Text.replace(textVal, " ", "_x0020_");
            }
            writer.write(textVal);
        }
    }

    /**
     * Deserializes the given string to a <code>Value</code> of the given type.
     *
     * @param factory the factory used to create the {@link Value} object of the
     * specified type.
     * @param value string to be deserialized
     * @param type type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code> character
     * sequences will be decoded to single space characters each.
     * @return the deserialized <code>Value</code>
     * @throws ValueFormatException if the string data is not of the required
     * format
     * @throws RepositoryException  if an error occured during the deserialization.
     */
    public static Value deserialize(ValueFactory factory, String value, int type,
                                    boolean decodeBlanks)
            throws ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                Base64.decode(value, baos);
                // no need to close ByteArrayOutputStream
                //baos.close();
            } catch (IOException ioe) {
                throw new RepositoryException("failed to decode binary value",
                        ioe);
            }
            return factory.createValue(new ByteArrayInputStream(baos.toByteArray()));
        } else {
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return factory.createValue(value, type);
        }
    }

    /**
     * Deserializes the string data read from the given reader to a
     * <code>Value</code> of the given type.
     *
     * @param factory the factory used to create the {@link Value} object of the
     * specified type.
     * @param reader reader for the string data to be deserialized
     * @param type type of value
     * @param decodeBlanks if <code>true</code> <code>"_x0020_"</code> character
     * sequences will be decoded to single space characters each.
     * @return the deserialized <code>Value</code>
     * @throws IOException if an i/o error occured during the serialization
     * @throws ValueFormatException if the string data is not of the required
     * format
     * @throws RepositoryException  if an error occured during the deserialization.
     */
    public static Value deserialize(ValueFactory factory, Reader reader, int type,
                                    boolean decodeBlanks)
            throws IOException, ValueFormatException, RepositoryException {
        if (type == PropertyType.BINARY) {
            // base64 encoded binary value;
            // the encodeBlanks flag can be ignored since base64-encoded
            // data cannot contain encoded space characters
/*
            // @todo decode to temp file and pass FileInputStream to BinaryValue constructor
            File tmpFile = File.createTempFile("bin", null);
            FileOutputStream out = new FileOutputStream(tmpFile);
            tmpFile.deleteOnExit();
            Base64.decode(reader, out);
            out.close();
            return new BinaryValue(new FileInputStream(tmpFile));
*/
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64.decode(reader, baos);
            // no need to close ByteArrayOutputStream
            return factory.createValue(new ByteArrayInputStream(baos.toByteArray()));
        } else {
            char[] chunk = new char[8192];
            int read;
            StringBuffer buf = new StringBuffer();
            while ((read = reader.read(chunk)) > -1) {
                buf.append(chunk, 0, read);
            }
            String value = buf.toString();
            if (decodeBlanks) {
                // decode encoded blanks in value
                value = Text.replace(value, "_x0020_", " ");
            }
            return factory.createValue(value, type);
        }
    }
}
