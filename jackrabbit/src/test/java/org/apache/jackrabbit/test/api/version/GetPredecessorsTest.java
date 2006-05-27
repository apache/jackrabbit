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

import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

/**
 * <code>GetPredecessorsTest</code>  provides test methods covering {@link
 * javax.jcr.version.Version#getPredecessors()}.
 *
 * @test
 * @sources GetPredecessorsTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.GetPredecessorsTest
 * @keywords versioning
 */
public class GetPredecessorsTest extends AbstractVersionTest {

    /**
     * Returns the predecessor versions of this version. This corresponds to
     * returning all the nt:version nodes whose jcr:successors property includes
     * a reference to the nt:version node that represents this version. A
     * RepositoryException is thrown if an error occurs.
     */
    public void testGetPredecessors() throws RepositoryException {
        // create a new version
        versionableNode.checkout();
        Version version = versionableNode.checkin();

        assertTrue("Version should have at minimum one predecessor version.", version.getPredecessors().length > 0);
    }
}