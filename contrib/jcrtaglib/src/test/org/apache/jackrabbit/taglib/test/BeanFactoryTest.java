/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.taglib.test;

import org.apache.jackrabbit.taglib.bean.SimpleBeanFactory;
import org.apache.jackrabbit.taglib.bean.SpringBeanFactory;
import org.apache.jackrabbit.taglib.traverser.PreorderTraverser;
import org.apache.jackrabbit.taglib.traverser.Traverser;

import junit.framework.TestCase;

/**
 * Bean factory test
 */
public class BeanFactoryTest extends TestCase
{
    public void testSpring() {
        SpringBeanFactory factory = new SpringBeanFactory() ;
        factory.setConfig("jcrtaglib-beans.xml") ;
        Traverser traverser  = (Traverser) factory.getBean("traverser.preorder") ;
        if (traverser==null) {
            fail("Traverser not found") ;
        }
    }
    
    public void testSimple() {
        SimpleBeanFactory factory = new SimpleBeanFactory() ;
        Traverser traverser = (Traverser) factory.getBean(PreorderTraverser.class.getName()) ;
        if (traverser==null) {
            fail("Traverser not found") ;
        }
    }

}
