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
package org.apache.jackrabbit.core.security.authorization.combined;

import org.apache.jackrabbit.core.security.authorization.AbstractPolicyTemplateTest;
import org.apache.jackrabbit.core.security.authorization.PolicyTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * <code>PolicyTemplateImplTest</code>...
 */
public class PolicyTemplateImplTest extends AbstractPolicyTemplateTest {

    private static Logger log = LoggerFactory.getLogger(PolicyTemplateImplTest.class);

    private String testPath = "/rep:accessControl/users/test";

    protected String getTestPath() {
        return testPath;
    }

    protected PolicyTemplate createEmptyTemplate(String testPath) {
        return new PolicyTemplateImpl(Collections.EMPTY_LIST, testPrincipal, testPath);
    }

    public void testGetPrincipal() {
        PolicyTemplateImpl pt = (PolicyTemplateImpl) createEmptyTemplate(testPath);
        assertEquals(testPrincipal, pt.getPrincipal());
    }
}