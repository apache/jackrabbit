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

package org.apache.jackrabbit.aws.ext.ds;

import org.apache.commons.io.input.ProxyInputStream;
import org.apache.jackrabbit.core.data.BackendResourceAbortable;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
 * S3 Backend based <code>InputStream</code> wrapper to implement {@link BackendResourceAbortable}.
 */
class S3BackendResourceAbortableInputStream extends ProxyInputStream implements BackendResourceAbortable {

    /**
     * Underlying backend {@link S3ObjectInputStream} instance.
     */
    private final S3ObjectInputStream s3input;

    /**
     * Construct a {@link BackendResourceAbortable} input stream with the given backend {@link S3ObjectInputStream}.
     * @param s3input
     */
    S3BackendResourceAbortableInputStream(final S3ObjectInputStream s3input) {
        super(s3input);
        this.s3input = s3input;
    }

    @Override
    public void abort() {
        s3input.abort();
    }

}
