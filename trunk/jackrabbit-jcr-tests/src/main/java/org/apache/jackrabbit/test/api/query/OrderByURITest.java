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
package org.apache.jackrabbit.test.api.query;

import javax.jcr.RepositoryException;
import javax.jcr.PropertyType;

/**
 * <code>OrderByURITest</code> tests order by queries with URI properties.
 */
public class OrderByURITest extends AbstractOrderByTest {

    private static final String BASE_URI = "http://example.com/";

    public void testURI() throws RepositoryException {
        populate(new String[]{BASE_URI + "a", BASE_URI + "b", BASE_URI + "c", BASE_URI + "d"}, PropertyType.URI);
        checkOrder(new String[]{nodeName1, nodeName2, nodeName3, nodeName4});
    }
}
