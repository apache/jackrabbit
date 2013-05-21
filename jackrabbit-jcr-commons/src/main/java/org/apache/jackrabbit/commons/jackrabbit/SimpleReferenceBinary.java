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
package org.apache.jackrabbit.commons.jackrabbit;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.api.ReferenceBinary;
import org.apache.jackrabbit.api.ReferenceBinaryException;

public class SimpleReferenceBinary implements ReferenceBinary {

    private final String reference;

    public SimpleReferenceBinary(String reference) {
        this.reference = reference;
    }

    //---------------------------------------------------< ReferenceBinary >--

    @Override
    public String getReference() {
        return reference;
    }

    //------------------------------------------------------------< Binary >--

    @Override
    public InputStream getStream() throws RepositoryException {
        throw new ReferenceBinaryException(
                "Broken binary reference: " + reference);
    }

    @Override
    public int read(byte[] b, long position)
            throws IOException, RepositoryException {
        throw new ReferenceBinaryException(
                "Broken binary reference: " + reference);
    }

    @Override
    public long getSize() throws RepositoryException {
        throw new ReferenceBinaryException(
                "Broken binary reference: " + reference);
    }

    @Override
    public void dispose() {
        // ignore
    }

}
