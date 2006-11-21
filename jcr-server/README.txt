README - Jackrabbit JCR Server
===============================================================================

This project contains WebDAV based JCR server implementations.
Currently 2 different server implementations are provided:


1) WebDAV server ('simple')
-------------------------------------------------------------------------------

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
-------------------------------------------------------------------------------

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
   - http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip">JCR_Webdav_Protocol.zip


Requirements
--------------------------------------------------------------------------------

This project assumes that you have already successfully compiled and 
installed the Jackrabbit 

- jcr-commons module and the 
- webdav project

into your maven repository. If this is not the case, go back to the root
project and build the corresponding projects.


License (see also LICENSE.txt)
--------------------------------------------------------------------------------

Collective work: Copyright 2006 The Apache Software Foundation.

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

