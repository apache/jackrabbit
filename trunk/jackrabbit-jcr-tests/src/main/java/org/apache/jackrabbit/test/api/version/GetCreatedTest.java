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

import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

/**
 * <code>GetCreatedTest</code> provides test methods covering {@link
 * javax.jcr.version.Version#getCreated()}.
 *
 * @test
 * @sources GetCreatedTest.java
 * @executeClass org.apache.jackrabbit.test.api.version.GetCreatedTest
 * @keywords versioning
 */
public class GetCreatedTest extends AbstractVersionTest {

    /**
     * Returns the date this version was created.
     */
    public void testGetCreated() throws RepositoryException {

        // create version
        versionableNode.checkout();
        Version version = versionableNode.checkin();

        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.SECOND, 1);

        assertTrue("Method getCreated() should return a creation date before current date.", version.getCreated().before(now));
    }


    /**
     * Returns the date this version was created. This corresponds to the value
     * of the jcr:created property in the nt:version node that represents this
     * version.
     */
    public void testGetCreatedCheckAgainstProperty() throws RepositoryException {

        // create version
        versionableNode.checkout();
        Version version = versionableNode.checkin();

        Calendar calGetCreated = version.getCreated();
        Calendar calCreatedProp = version.getProperty(jcrCreated).getValue().getDate();

        assertEquals("Method getCreated() should return value of the jcr:created property.", calGetCreated, calCreatedProp);
    }

}