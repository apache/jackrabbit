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
package org.apache.jackrabbit.webdav.jcr;

import junit.framework.TestCase;

import javax.jcr.lock.LockException;
import javax.jcr.RepositoryException;

/** <code>JcrDavExceptionTest</code>... */
public class JcrDavExceptionTest extends TestCase {

    public void testDerivedException() {
        RepositoryException re = new DerievedRepositoryException();

        // creating JcrDavException from the derived exception must not throw
        // NPE (see issue https://issues.apache.org/jira/browse/JCR-1678)
        JcrDavException jde = new JcrDavException(re);

        // error code must be the same as for LockException
        assertEquals(new JcrDavException(new LockException()).getErrorCode(),
                     jde.getErrorCode());
    }

    public void testNullException() {
        try {
            new JcrDavException(null);
            fail("Should throw NPE");
        } catch (NullPointerException e) {
            // as documented in the javadoc
        }
    }

    /**
     * Derived exception that does not extend from RepositoryException, which
     * returns the 'default' error code.
     */
    private static final class DerievedRepositoryException extends LockException {
    }
}