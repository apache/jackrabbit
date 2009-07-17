=============================================================
Welcome to Apache Jackrabbit  <http://jackrabbit.apache.org/>
=============================================================

Apache Jackrabbit is a fully conforming implementation of the
Content Repository for Java Technology API (JCR). A content repository
is a hierarchical content store with support for structured and
unstructured content, full text search, versioning, transactions,
observation, and more. Typical applications that use content
repositories include content management, document management,
and records management systems.

Jackrabbit is currently based on a pre-release version of the JCR 2.0 API
defined by the Java Specification Request 283 (JSR 283,
http://jcp.org/en/jsr/detail?id=283). Version 1.0 of the JCR API was
specified by JSR 170.

Apache Jackrabbit is a project of the Apache Software Foundation.

Building Jackrabbit
===================

To build Jackrabbit, you first need to download the jcr-2.0-b110.jar and
jcr-2.0-b110.xml files from https://issues.apache.org/jira/browse/JCR-1104
and install them to your local Maven repository:

    mvn install:install-file \
        -Dfile=jcr-2.0-b110.jar -DpomFile=jcr-2.0-b110.xml

Once you've installed the jar, you can build Jackrabbit like this:

    mvn clean install

You need Maven 2.0.9 (or higher) with Java 5 (or higher) for the build.
For more instructions, please see the documentation at:

   http://jackrabbit.apache.org/building-jackrabbit.html

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2009 The Apache Software Foundation.

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

Mailing Lists
-------------

To get involved with the Apache Jackrabbit project, start by having a
look at our website and joining our mailing lists. For more details about
Jackrabbit mailing lists as well as links to list archives, please see:

   http://jackrabbit.apache.org/mailing-lists.html

Downloading
-----------

The Jackrabbit source code is available via Subversion at

   https://svn.apache.org/repos/asf/jackrabbit/trunk/

or with ViewVC at

   https://svn.apache.org/viewvc/jackrabbit/trunk/

To checkout the main Jackrabbit source tree, run

   svn checkout https://svn.apache.org/repos/asf/jackrabbit/trunk jackrabbit

Credits
=======

See http://jackrabbit.apache.org/jackrabbit-team.html for the list of 
Jackrabbit committers and main contributors.
