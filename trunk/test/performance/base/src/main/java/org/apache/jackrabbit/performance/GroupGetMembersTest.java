/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.jackrabbit.performance;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;

import java.security.Principal;
import java.util.Iterator;

public class GroupGetMembersTest extends AbstractTest {

    private static final int USER_COUNT = getScale(2000);

    private static final Principal GROUP_PRINCIPAL = new Principal() {
        public String getName() {
            return "test_group";
        }
    };

    private UserManager userMgr;
    private Group group;

    @Override
    protected void beforeSuite() throws Exception {
        userMgr = ((JackrabbitSession) loginWriter()).getUserManager();
        group = userMgr.createGroup(GROUP_PRINCIPAL);
        for (int k = 0; k < USER_COUNT; k++) {
            group.addMember(userMgr.createUser("user_" + k, "pass"));
        }
    }

    @Override
    protected void runTest() throws Exception {
        Iterator<?> members = group.getMembers();

        // Iterate half way through the list should show differences
        // between lazy and eager iterators returned from getMembers
        for (int k = 0; k < USER_COUNT/2; k++) {
            members.next();
        }
    }

    @Override
    protected void afterSuite() throws Exception {
        for (int k = 0; k < USER_COUNT; k++) {
            userMgr.getAuthorizable("user_" + k).remove();
        }
        group.remove();
    }

}
