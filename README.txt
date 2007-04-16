================================
Welcome to Jackrabbit JCR Server
================================

This is the JCR Server component of the Apache Jackrabbit project.
This component contains two WebDAV based JCR server implementations:

   1) WebDAV server ('simple')

      DAV1,2 compliant WebDAV server implementation to access a
      JSR170 repository.
  
      Futher information such as configuration as well as the
      SimpleWebdavServlet itself may be found in the 'webapp' project.
  
      Packages:
      - org.apache.jackrabbit.server         = server
      - org.apache.jackrabbit.server.io      = import/export
      - org.apache.jackrabbit.webdav.simple  = dav-resource implementation + config.

      Servlet (webapp project):
      - org.apache.jackrabbit.j2ee.SimpleWebdavServlet.java 
  

   2) 'jcr' server:

      Server used to remove JSR170 calls via WebDAV.
      No particular effort to be compliant to WebDAV related RFCs.
   
      The 'client' counterpart of this server is under development and
      can be found within the <jackrabbit>/contrib/spi contribution.
   
      Packages:
      - org.apache.jackrabbit.server     = server
      - org.apache.jackrabbit.server.jcr = jcr-server specific server part
      - org.apache.jackrabbit.webdav.jcr = dav-resources, reports, properties   
   
      Servlet (webapp project):
      - org.apache.jackrabbit.j2ee.JCRServerServlet.java
           
      Further reading:
      - http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip

See the Apache Jackrabbit web site (http://jackrabbit.apache.org/)
for documentation and other information. You are welcome to join the
Jackrabbit mailing lists (http://jackrabbit.apache.org/mail-lists.html)
to discuss this compoment and to use the Jackrabbit issue tracker
(http://issues.apache.org/jira/browse/JCR) to report issues or request
new features.

Apache Jackrabbit is a project of the Apache Software Foundation
(http://www.apache.org).


License (see also LICENSE.txt)
==============================

Collective work: Copyright 2007 The Apache Software Foundation.

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Getting Started
===============

This compoment uses a Maven 2 (http://maven.apache.org/) build
environment. If you have Maven 2 installed, you can compile and
package the jacrabbit-jcr-server jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

This compoment depends on the Jackrabbit WebDAV and JCR Commons libraries.
If you are using the latest Jackrabbit source code, you need to first
build these libraries and install them in your local Maven 2 repository
before building this component.

The latest source code for this compoment is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/jackrabbit/trunk/jackrabbit-jcr-server

See the Subversion documentation for other source control features.
