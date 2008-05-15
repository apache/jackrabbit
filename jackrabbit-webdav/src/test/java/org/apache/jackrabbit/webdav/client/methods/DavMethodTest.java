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
package org.apache.jackrabbit.webdav.client.methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.jackrabbit.webdav.DavException;
import org.w3c.dom.Document;
import junit.framework.TestCase;

import java.io.IOException;

/** <code>DavMethodTest</code>... */
public class DavMethodTest extends TestCase {

    private static Logger log = LoggerFactory.getLogger(DavMethodTest.class);

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetReponseException() throws IOException {
        DavMethod m = new TestDavMethod();
        DavException e = m.getResponseException();

        assertFalse(e.hasErrorCondition());
    }


    private class TestDavMethod extends DavMethodBase {
        private TestDavMethod() {
            super("test");
        }
        public String getName() {
            return "test";
        }
        public int getStatusCode() {
            return 404;
        }
        public String getStatusText() {
            return "404";
        }
        public Document getResponseBodyAsDocument() throws IOException {
            throw new IOException();
        }
        public void checkSuccess() throws DavException, IOException {
            throw new DavException(404);
        }
        public boolean succeeded() {
            return false;
        }
        protected void checkUsed() {}
        protected boolean isSuccess(int statusCode) {
            return false;
        }
        protected boolean getSuccess() {
            return false;
        }
    }
}