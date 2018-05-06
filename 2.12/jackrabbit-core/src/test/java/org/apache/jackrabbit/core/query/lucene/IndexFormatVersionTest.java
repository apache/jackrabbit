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
package org.apache.jackrabbit.core.query.lucene;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.core.query.AbstractIndexingTest;

/**
 * <code>IndexFormatVersionTest</code> checks if the various index format
 * versions are correctly read from the index.
 */
public class IndexFormatVersionTest extends AbstractIndexingTest {

    public void testVersionOne() throws RepositoryException {
        checkIndexFormatVersion("index-format-v1", IndexFormatVersion.V1);
    }

    public void testVersionTwo() throws RepositoryException {
        checkIndexFormatVersion("index-format-v2", IndexFormatVersion.V2);
    }

    public void testVersionThree() throws RepositoryException {
        checkIndexFormatVersion("index-format-v3", IndexFormatVersion.V3);
    }

    private void checkIndexFormatVersion(String wspName,
                                         IndexFormatVersion version)
            throws RepositoryException {
        Session session = getHelper().getSuperuserSession(wspName);
        try {
            SearchIndex index = getSearchIndex(session);
            assertEquals("Wrong index format", version.getVersion(),
                    index.getIndexFormatVersion().getVersion());
        } finally {
            session.logout();
        }
    }
}
