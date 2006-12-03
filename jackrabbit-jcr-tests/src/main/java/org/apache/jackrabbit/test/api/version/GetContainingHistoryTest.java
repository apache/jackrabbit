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
package org.apache.jackrabbit.test.api.version;

import javax.jcr.version.Version;
import javax.jcr.RepositoryException;

/**
 * <code>GetContainingHistoryTest</code> provides test methods covering {@link
 * javax.jcr.version.Version#getContainingHistory()}.
 *
 * @test
 * @sources GetContainingHistoryTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.GetContainingHistoryTest
 * @keywords versioning
 */
public class GetContainingHistoryTest extends AbstractVersionTest {

    /**
     * Tests if {@link javax.jcr.version.Version#getContainingHistory()} returns
     * the correct VersionHistory instance.
     */
    public void testGetContainingHistory() throws RepositoryException {
        // create version
        versionableNode.checkout();
        Version version = versionableNode.checkin();

        assertTrue("Method getContainingHistory() must return the same VersionHistory " +
                   "as getVersionHistory() of the corresponding Node.",
                   versionableNode.getVersionHistory().isSame(version.getContainingHistory()));
    }
}