README - Jackrabbit WebApp
===============================================================================

Description
-------------------------------------------------------------------------------

The Jackrabbit WebApp provides servlets used to access a jackrabbit
repository:

- RepositoryAccessServlet.java
- LoggingServlet.java
- RepositoryStartupServlet.java

In addition, the project contains 2 different WebDAV servlets:

- SimpleWebdavServlet.java
  Adds webdav support (DAV 1,2) to your jackrabbit repository.
  
- JCRWebdavServerServlet.java
  A servlet used to remove JSR170 calls via webDAV. 
  IMPORTANT: Please note, that this servlet is not intended to provide 
  common webdav support to the repository. Instead the primary goal is to 
  remote JSR170 calls.
  For the corresponding client see -> contrib/spi (work in progress).


Requirements
-------------------------------------------------------------------------------

This project has a dependency to the following jackrabbit projects:

- jackrabbit core          (used for repository access)
- jackrabbit jcr-commons   (utilities)
- jackrabbit jcr-rmi       (used for repository access)
- jackrabbit webdav        (used for webdav servlets)
- jackrabbit jcr-server    (used for webdav servlets)

Make sure you have all those libraries installed before building
this project. If this is not the case, go back to the root
project and launch

> cd jackrabbit
> maven
> cd modules/commons
> maven jar:install

Also go to the jcr-rmi project and launch

> cd jcr-rmi
> maven jar:install

The webdav and the server project:

> cd webdav
> maven

> cd jcr-server
> maven

which will build and copy Jackrabbit into ~/.maven/repository/org.apache.jackrabbit/jars

Finally build this project:

> cd webapp
> maven



Common Questions
-------------------------------------------------------------------------------



License (see also LICENSE.txt)
-------------------------------------------------------------------------------

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

