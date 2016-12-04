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
package org.apache.jackrabbit.performance;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class BigFileWriteTest extends AbstractTest {

    private static final int FILE_SIZE = 100;

    private Session session;

    private Node file;

    public void beforeSuite() throws RepositoryException {
        failOnRepositoryVersions("1.4", "1.5", "1.6");

        session = loginWriter();
    }

    public void runTest() throws RepositoryException {
        file = session.getRootNode().addNode(
                "BigFileWriteTest", "nt:file");
        Node content = file.addNode("jcr:content", "nt:resource");
        content.setProperty("jcr:mimeType", "application/octet-stream");
        content.setProperty("jcr:lastModified", Calendar.getInstance());
        content.setProperty(
                "jcr:data", new TestInputStream(FILE_SIZE * 1024 * 1024));
        session.save();
    }

    public void afterTest() throws RepositoryException {
        file.remove();
        session.save();
    }

}
