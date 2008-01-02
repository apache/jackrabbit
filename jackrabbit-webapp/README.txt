=====================================
Welcome to Jackrabbit Web Application
=====================================

This is the Web Application component of the Apache Jackrabbit project.
This component provides servlets used to access a Jackrabbit repository:

    * RepositoryAccessServlet.java
    * LoggingServlet.java
    * RepositoryStartupServlet.java

In addition, the project contains 2 different WebDAV servlets:

    * SimpleWebdavServlet.java
      Adds webdav support (DAV 1,2) to your jackrabbit repository.
  
    * JCRWebdavServerServlet.java
      A servlet used to remove JSR170 calls via webDAV. 
      IMPORTANT: Please note, that this servlet is not intended to provide 
      common webdav support to the repository. Instead the primary goal is to 
      remote JSR170 calls.
      For the corresponding client see -> contrib/spi (work in progress).

See the Apache Jackrabbit web site (http://jackrabbit.apache.org/)
for documentation and other information. You are welcome to join the
Jackrabbit mailing lists (http://jackrabbit.apache.org/mail-lists.html)
to discuss this component and to use the Jackrabbit issue tracker
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

This component uses a Maven 2 (http://maven.apache.org/) build
environment. If you have Maven 2 installed, you can compile and
package the jackrabbit-webapp war using the following command:

    mvn package

See the Maven 2 documentation for other build features.

This component depends on the following other Jackrabbit components:

    * jackrabbit-core          (used for repository access)
    * jackrabbit-jcr-commons   (utilities)
    * jackrabbit-jcr-rmi       (used for remote access)
    * jackrabbit-webdav        (used for webdav servlets)
    * jackrabbit-jcr-server    (used for webdav servlets)

If you are using the latest Jackrabbit source code, you need to first
build these components and install them in your local Maven 2 repository
before building this component.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/jackrabbit/trunk/jackrabbit-webapp

See the Subversion documentation for other source control features.
