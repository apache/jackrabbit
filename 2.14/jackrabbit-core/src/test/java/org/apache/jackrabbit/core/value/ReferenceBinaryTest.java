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
package org.apache.jackrabbit.core.value;

import javax.jcr.Binary;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.api.ReferenceBinary;
import org.apache.jackrabbit.commons.jackrabbit.SimpleReferenceBinary;
import org.apache.jackrabbit.core.data.RandomInputStream;
import org.apache.jackrabbit.test.AbstractJCRTest;

/**
 * Testcase for JCR-3534.
 */
public class ReferenceBinaryTest extends AbstractJCRTest {

    private static final int STREAM_LENGTH = 256 * 1024;

    public void testReferenceBinaryExchangeWithSharedRepository() throws Exception {

        Session firstSession = superuser;

        // create a binary
        Binary b = vf.createBinary(new RandomInputStream(1, STREAM_LENGTH));

        ReferenceBinary referenceBinary = null;
        if (b instanceof ReferenceBinary) {
            referenceBinary = (ReferenceBinary) b;
        }

        assertNotNull(referenceBinary);

        assertNotNull(referenceBinary.getReference());

        // in the current test the message is exchanged via repository which is shared as well
        // put the reference message value in a property on a node
        String newNode = "sample_" + System.nanoTime();
        firstSession.getRootNode().addNode(newNode).setProperty("reference", referenceBinary.getReference());

        // save the first session
        firstSession.save();

        // get a second session over the same repository / ds
        Session secondSession = getHelper().getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));

        // read the binary referenced by the referencing binary
        String reference = secondSession.getRootNode().getNode(newNode).getProperty("reference").getString();

        ReferenceBinary ref = new SimpleReferenceBinary(reference);

        assertEquals(b, secondSession.getValueFactory().createValue(ref).getBinary());

    }

}