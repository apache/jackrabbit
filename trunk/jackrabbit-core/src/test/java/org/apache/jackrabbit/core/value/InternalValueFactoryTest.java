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
package org.apache.jackrabbit.core.value;

import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.spi.QValueFactoryTest;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <code>InternalValueFactoryTest</code>...
 */
public class InternalValueFactoryTest extends QValueFactoryTest {

    /**
     * logger instance
     */
    private static final Logger log = LoggerFactory.getLogger(InternalValueFactoryTest.class);

    protected void setUp() throws Exception {
        factory = InternalValueFactory.getInstance();
        rootPath = PathFactoryImpl.getInstance().getRootPath();
        testName = NameFactoryImpl.getInstance().create(Name.NS_JCR_URI, "data");
        reference = NodeId.randomId().toString();
    }

    protected void tearDown() throws Exception {
    }
}