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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.persistence.check.ConsistencyReport;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.apache.jackrabbit.test.LogPrintWriter;

public class ConsistencyCheck extends AbstractJCRTest {

    private LogPrintWriter log = new LogPrintWriter(logger);

    // Why are we running these twice?
    public void testDo1() throws Exception {
        runCheck();
    }

    // ...because AbstractJCRTests iterates through multiple test repositories.
    // this way, we should check at least two of them. Yes, this is a hack.
    public void testDo2() throws Exception {
        runCheck();
    }

    private void runCheck() throws Exception {
        log.print("running consistency check on repository "
                + getHelper().getRepository());

        ConsistencyReport rep = TestHelper.checkConsistency(testRootNode.getSession(), false, null);
        assertEquals("Found broken nodes in repository: " + rep, 0, rep.getItems().size());

        rep = TestHelper.checkVersionStoreConsistency(testRootNode.getSession(), false, null);
        assertEquals("Found broken nodes in version storage: " + rep, 0, rep.getItems().size());
    }
}
