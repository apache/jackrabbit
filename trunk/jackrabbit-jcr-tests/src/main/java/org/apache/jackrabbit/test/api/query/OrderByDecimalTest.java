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

import java.math.BigDecimal;

import javax.jcr.RepositoryException;

/**
 * <code>OrderByDecimalTest</code> tests order by queries with decimal properties.
 */
public class OrderByDecimalTest extends AbstractOrderByTest {

    public void testDecimal() throws RepositoryException {
        populate(new BigDecimal[]{new BigDecimal(0), new BigDecimal(-1), new BigDecimal(1), new BigDecimal(5)});
        checkOrder(new String[]{nodeName2, nodeName1, nodeName3, nodeName4});
    }
}
