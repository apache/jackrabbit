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
package org.apache.jackrabbit.test.api.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class which allows to check whether the #close() method has been
 * called on this stream.
 */
public class InputStreamWrapper extends FilterInputStream {

    private boolean closed;

    public InputStreamWrapper(InputStream in) {
        super(in);
        closed = false;
    }

    public void close() throws IOException {
        closed = true;
        super.close();
    }

    public boolean isClosed() {
        return closed;
    }
}
