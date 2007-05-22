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
package org.apache.jackrabbit.jcrlog.doodle;

import javax.jcr.Repository;
import javax.jcr.Session;

/**
 * Scratch pad for transient tests.
 *
 * @author Thomas Mueller
 *
 */
public class MyDoodle {

    public static void main(String[] args) throws Exception {
        new MyDoodle().test();
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public void test() throws Exception {

//      11-10 14:13:28
//      > org.apache.jackrabbit.jcrlog.samples.FirstHop.main(FirstHop.java:40)
      /**/Repository rp0 = org.apache.jackrabbit.jcrlog.RepositoryFactory.open("apache/jackrabbit/transient");
//      > org.apache.jackrabbit.jcrlog.samples.FirstHop.main(FirstHop.java:41)
      /**/Session s0 = rp0.login();
//      11-10 14:13:33
//      time: 4672 ms
//      > org.apache.jackrabbit.jcrlog.samples.FirstHop.main(FirstHop.java:43)
      /**/s0.getUserID();
//      > org.apache.jackrabbit.jcrlog.samples.FirstHop.main(FirstHop.java:44)
      /**/rp0.getDescriptor("jcr.repository.name");
//      > org.apache.jackrabbit.jcrlog.samples.FirstHop.main(FirstHop.java:48)
      /**/s0.logout();
//      time: 562 ms


    }

}
