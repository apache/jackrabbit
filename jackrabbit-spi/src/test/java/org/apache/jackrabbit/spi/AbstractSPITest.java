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
package org.apache.jackrabbit.spi;

import junit.framework.TestCase;

import javax.jcr.RepositoryException;

/** <code>AbstractSPITest</code>... */
public class AbstractSPITest extends TestCase {

    /**
     * Helper object to access repository service transparently
     */
    public static Helper helper = new Helper();

    /**
     * SessionInfo with superuser permission
     */
    protected SessionInfo sessionInfo;

    /**
     * Returns the value of the configuration property with <code>propName</code>.
     * The sequence how configuration properties are read is the following:
     * <ol>
     * <li><code>org.apache.jackrabbit.spi.&lt;testClassName>.&lt;testCaseName>.&lt;propName></code></li>
     * <li><code>org.apache.jackrabbit.spi.&lt;testClassName>.&lt;propName></code></li>
     * <li><code>org.apache.jackrabbit.spi.&lt;packageName>.&lt;propName></code></li>
     * <li><code>org.apache.jackrabbit.spi.&lt;propName></code></li>
     * </ol>
     * Where:
     * <ul>
     * <li><code>&lt;testClassName></code> is the name of the test class without package prefix.</li>
     * <li><code>&lt;testMethodName></code> is the name of the test method</li>
     * <li><code>&lt;packageName></code> is the name of the package of the test class.
     * </ul>
     * @param propName the propName of the configuration property.
     * @return the value of the property or <code>null</code> if the property
     *  does not exist.
     * @throws RepositoryException if an error occurs while reading from
     *  the configuration.
     */
    public String getProperty(String propName) throws RepositoryException {
        String testCaseName = getName();
        String testClassName = getClass().getName();
        String testPackName = "";
        int idx;
        if ((idx = testClassName.lastIndexOf('.')) > -1) {
            testPackName = testClassName.substring(testClassName.lastIndexOf('.', idx - 1) + 1, idx);
            testClassName = testClassName.substring(idx + 1);
        }

        // 1) test case specific property first
        String value = helper.getProperty(RepositoryServiceStub.PROP_PREFIX + "."
                + testClassName + "." + testCaseName + "." + propName);
        if (value != null) {
            return value;
        }

        // 2) check test class property
        value = helper.getProperty(RepositoryServiceStub.PROP_PREFIX + "."
                + testClassName + "." + propName);
        if (value != null) {
            return value;
        }

        // 3) check package property
        value = helper.getProperty(RepositoryServiceStub.PROP_PREFIX + "."
                + testPackName + "." + propName);
        if (value != null) {
            return value;
        }

        // finally try global property
        return helper.getProperty(RepositoryServiceStub.PROP_PREFIX + "." + propName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        sessionInfo = helper.getAdminSessionInfo();
    }

    @Override
    protected void tearDown() throws Exception {
        if (sessionInfo != null) {
            helper.getRepositoryService().dispose(sessionInfo);
        }
        super.tearDown();
    }
}