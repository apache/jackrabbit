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
package org.apache.jackrabbit.core.data;

import java.io.InputStream;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Test that adding an entry works even if the the entry already exists and is still open for reading.
 *
 * This is a problem for the FileDataStore on Windows, see JCR-2872.
 */
public class WriteWhileReadingTest extends AbstractJCRTest {

    private static final int STREAM_LENGTH = 32 * 1024;

    public void test() throws Exception {
        Node root = superuser.getRootNode();
        ValueFactory vf = superuser.getValueFactory();

        // store a binary in the data store
        root.setProperty("p1", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();

        // read from the binary, but don't close the file
        Value v1 = root.getProperty("p1").getValue();
        InputStream in = v1.getBinary().getStream();
        in.read();

        // store the same content at a different place -
        // this will change the last modified date of the file
        root.setProperty("p2", vf.createBinary(new RandomInputStream(1, STREAM_LENGTH)));
        superuser.save();

        in.close();
    }

}
