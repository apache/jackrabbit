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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.jcr.RepositoryException;

import org.xml.sax.InputSource;

/**
 * Input source that clones existing input source. After cloning the existing
 * input source should not be used anymore. To make more copies call
 * {@link #cloneInputSource()} method.
 */
public class ClonedInputSource extends InputSource {
    private final char[] characterArray;
    private final byte[] byteArray;

    /**
     * Clone existing input source.
     *
     * @param input
     * @throws RepositoryException
     */
    public ClonedInputSource(InputSource input) throws RepositoryException {

        if (input == null) {
            throw new IllegalArgumentException(
                    "Argument 'input' may not be null.");
        }

        characterArray = read(input.getCharacterStream());
        byteArray = read(input.getByteStream());

        setEncoding(input.getEncoding());
        setPublicId(input.getPublicId());
        setSystemId(input.getSystemId());
        if (characterArray != null) {
            setCharacterStream(new CharArrayReader(characterArray));
        }
        if (byteArray != null) {
            setByteStream(new ByteArrayInputStream(byteArray));
        }
    }

    private ClonedInputSource(char[] characterArray, byte[] byteArray) {
        super();
        this.characterArray = characterArray;
        this.byteArray = byteArray;
    }

    public char[] getCharacterArray() {
        return characterArray;
    }

    public byte[] getByteArray() {
        return byteArray;
    }

    /**
     * Make a clone if this input source. The input source being cloned is still
     * valid after cloning.
     *
     * @return input source clone.
     */
    public ClonedInputSource cloneInputSource() {

        ClonedInputSource res = new ClonedInputSource(characterArray, byteArray);

        res.setEncoding(getEncoding());
        res.setPublicId(getPublicId());
        res.setSystemId(getSystemId());

        if (byteArray != null) {
            res.setByteStream(new ByteArrayInputStream(byteArray));
        }
        if (characterArray != null) {
            res.setCharacterStream(new CharArrayReader(characterArray));
        }

        return res;
    }

    private static byte[] read(InputStream stream) throws RepositoryException {
        if (stream != null) {
            try {
                final int bufferSize = Math.min(stream.available(), 4096);
                ByteArrayOutputStream s = new ByteArrayOutputStream(bufferSize);

                byte[] buffer = new byte[bufferSize];
                while (true) {
                    int numRead = stream.read(buffer);
                    if (numRead > 0) {
                        s.write(buffer, 0, numRead);
                    }
                    if (numRead != bufferSize) {
                        break;
                    }
                }

                return s.toByteArray();
            } catch (IOException e) {
                throw new RepositoryException(e);
            } finally {
                try {
                    stream.close();
                } catch (IOException ignore) {

                }
            }
        } else {
            return null;
        }
    }

    private static char[] read(Reader reader) throws RepositoryException {
        if (reader != null) {
            try {
                final int bufferSize = 4096;
                CharArrayWriter w = new CharArrayWriter(bufferSize);

                char[] buffer = new char[bufferSize];
                while (true) {
                    int numRead = reader.read(buffer);
                    if (numRead > 0) {
                        w.write(buffer, 0, numRead);
                    }
                    if (numRead != bufferSize) {
                        break;
                    }
                }
                return w.toCharArray();
            } catch (IOException e) {
                throw new RepositoryException(e);
            } finally {
                try {
                    reader.close();
                } catch (IOException ignore) {

                }
            }
        } else {
            return null;
        }

    }
}