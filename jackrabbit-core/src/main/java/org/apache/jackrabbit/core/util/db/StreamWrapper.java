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
package org.apache.jackrabbit.core.util.db;

import java.io.InputStream;

public class StreamWrapper {

    private final InputStream stream;
    private final long size;

    /**
     * Creates a wrapper for the given InputStream that can
     * safely be passed as a parameter to the {@link ConnectionHelper#exec(String, Object...)},
     * {@link ConnectionHelper#exec(String, Object[], boolean, int)} and
     * {@link ConnectionHelper#update(String, Object[])} methods.
     *
     * @param in the InputStream to wrap
     * @param size the size of the input stream
     */
    public StreamWrapper(InputStream in, long size) {
        this.stream = in;
        this.size = size;
    }
    
    public InputStream getStream() {
        return stream;
    }
    
    public long getSize() {
        return size;
    }
}
