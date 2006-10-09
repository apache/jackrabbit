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
package org.apache.jackrabbit.core.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.IOException;

/**
 * <code>LazyReader</code> implement an utility that allows an implementing
 * class to lazy initialize an actual reader.
 */
public abstract class LazyReader extends Reader {

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(LazyReader.class);

    /**
     * Implements a reader that acts as if reading from an empty file.
     */
    private static final Reader NULL_READER = new Reader() {
        public void close() {
        }

        public int read(char cbuf[], int off, int len) {
            return -1;
        }
    };

    /**
     * The actual reader, set by concrete sub class.
     */
    protected Reader delegate;

    /**
     * Implementation must set the actual reader {@link #delegate} when
     * this method is called.
     *
     * @throws IOException if an error occurs.
     */
    protected abstract void initializeReader() throws IOException;

    /**
     * Closes the underlying reader.
     *
     * @throws IOException if an exception occurs while closing the underlying
     *                     reader.
     */
    public void close() throws IOException {
        if (delegate != null) {
            delegate.close();
        }
    }

    /**
     * @inheritDoc
     */
    public int read(char cbuf[], int off, int len) throws IOException {
        if (delegate == null) {
            try {
                // try to initialize reader
                initializeReader();
            } catch (Throwable t) {
                log.warn("exception initializing reader " +
                        getClass().getName() + ": " + t);
                log.debug("Dump: ", t);
                // assign null reader
                delegate = NULL_READER;
            }
        }
        // be suspicious
        if (delegate == null) {
            delegate = NULL_READER;
        }
        return delegate.read(cbuf, off, len);
    }
}
