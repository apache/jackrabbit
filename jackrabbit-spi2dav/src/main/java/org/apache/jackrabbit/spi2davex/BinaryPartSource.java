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
package org.apache.jackrabbit.spi2davex;

import org.apache.commons.httpclient.methods.multipart.PartSource;
import org.apache.jackrabbit.spi.QValue;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <code>BinaryPartSource</code>...
 */
public class BinaryPartSource implements PartSource {

    private final QValue value;

    BinaryPartSource(QValue value) {
        this.value = value;
    }

    public long getLength() {
        try {
            return value.getLength();
        } catch (RepositoryException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public String getFileName() {
        return value.toString();
    }

    public InputStream createInputStream() throws IOException {
        try {
            return value.getStream();
        } catch (RepositoryException e) {
            throw new IOException(e.getMessage());
        }
    }
}