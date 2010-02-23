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
package org.apache.jackrabbit.spi2davex;

import org.apache.jackrabbit.spi.AbstractSPITest;
import org.apache.jackrabbit.spi.RepositoryService;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.namespace.AbstractNamespaceResolver;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

/**
 * <code>ConnectionTest</code>...
 */
public class ConnectionTest extends AbstractSPITest {

    private NamePathResolver resolver;
    private RepositoryService rs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        rs = helper.getRepositoryService();
                NamespaceResolver nsResolver = new AbstractNamespaceResolver() {
            public String getURI(String prefix) {
                return ("jcr".equals(prefix)) ? "http://www.jcp.org/jcr/1.0" : prefix;
            }
            public String getPrefix(String uri) {
                return ("http://www.jcp.org/jcr/1.0".equals(uri)) ? "jcr" : uri;
            }
        };
        resolver = new DefaultNamePathResolver(nsResolver);
    }

    public void testObtainWithNullWorkspaceName() throws RepositoryException, LoginException {
        SessionInfo sInfo = rs.obtain(new SimpleCredentials("admin", "admin".toCharArray()), null);
        try {
            assertEquals("default",sInfo.getWorkspaceName());
        } finally {
            rs.dispose(sInfo);
        }
    }
}
