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
package org.apache.jackrabbit.ocm;

import junit.framework.Test;
import junit.framework.TestSuite;


/**
 * OCM suite definition. Bundles together all independent and package level test suites.
 * 
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 */
public class AllTests {

    public static Test suite() throws Exception {
        return new RepositoryLifecycleTestSetup(buildSuite());
    }

    public static Test buildSuite() throws Exception {
        TestSuite suite= new TestSuite("Jackrabbit OCM Tests");
        suite.addTest(org.apache.jackrabbit.ocm.mapper.AllTests.buildSuite());     
        suite.addTest(org.apache.jackrabbit.ocm.manager.atomic.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.auto.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.basic.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.beanconverter.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.collectionconverter.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.inheritance.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.interfaces.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.jcrnodetype.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.manager.lock.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.manager.proxy.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.query.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.manager.uuid.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.manager.version.AllTests.buildSuite());        
        suite.addTest(org.apache.jackrabbit.ocm.querymanager.AllTests.buildSuite());
        suite.addTest(org.apache.jackrabbit.ocm.repository.AllTests.buildSuite());
        
        return suite;
    }
}

