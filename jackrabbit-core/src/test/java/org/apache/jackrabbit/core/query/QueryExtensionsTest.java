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
package org.apache.jackrabbit.core.query;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;

/**
 * Tests for newer query extensions defined in Oak (verifying they either work
 * as described, or predictably fail so fallbacks can be used).
 */

public class QueryExtensionsTest extends AbstractQueryTest {

    // see https://issues.apache.org/jira/browse/OAK-9625
    public void testFirst() throws RepositoryException {
        try {
            executeSQL2Query("select [jcr:path] from [nt:base] where first([jcr:mixinTypes]) >= ''\"");
            fail("InvalidQueryException expected");
        } catch (InvalidQueryException expected) {
        }
    }
}
