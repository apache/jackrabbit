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
package org.apache.jackrabbit.webdav.jcr.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.security.Privilege;

import org.apache.jackrabbit.webdav.security.SupportedPrivilege;

public class JcrSupportedPrivilegePropertyTest extends AbstractSecurityTest {

    public void testSupportedPrivileges() throws RepositoryException {
        Set<Privilege> privs = new HashSet<Privilege>(Arrays.asList(acMgr.getSupportedPrivileges(testRoot)));
        JcrSupportedPrivilegesProperty prop = new JcrSupportedPrivilegesProperty(superuser, testRoot);
        List<SupportedPrivilege> value = prop.asDavProperty().getValue();

        if (privs.contains(acMgr.privilegeFromName(Privilege.JCR_ALL))) {
            assertEquals(1, value.size());
        }
    }

    public void testJcrAllPrivilege() throws RepositoryException {
        JcrSupportedPrivilegesProperty prop = new JcrSupportedPrivilegesProperty(superuser);
        List<SupportedPrivilege> value = prop.asDavProperty().getValue();

        assertEquals(1, value.size());
    }
}