/*
 * Copyright 2004-2005 The Apache Software Foundation or its licensors,
 *                     as applicable.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.test.rmi;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that contains all JCR-RMI test cases.
 */
public class TestAll {

    /** This class cannot be instantiated. */
    private TestAll() {
    }

    /**
     * Returns the JCR-RMI test suite.
     *
     * @return JCR-RMI test suite
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("Tests for org.apache.jackrabbit.rmi");
        //$JUnit-BEGIN$
        suite.addTestSuite(RemoteAdapterTest.class);
        //$JUnit-END$
        return suite;
    }

}
