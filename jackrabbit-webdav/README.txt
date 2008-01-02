====================================
Welcome to Jackrabbit WebDAV Library
====================================

This is the WebDAV Library component of the Apache Jackrabbit project.
This component provides interfaces and common utility classes used for
building a WebDAV server or client. The following RFC have been integrated:

    * RFC 2518 (WebDAV - HTTP Extensions for Distributed Authoring)
    * RFC 3253 (DeltaV - Versioning Extensions to WebDAV)
    * RFC 3648 (Ordered Collections Protocol)
    * RFC 3744 (Access Control Protocol)
    * DAV Searching and Locating  (DASL)

In addition this library defines (unspecified)

    * Observation
    * Bundling multiple request with extensions to locking

See "TODO.txt" for the current status and open issues.

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
package the jackrabbit-commons jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

This component has a dependency to the Jackrabbit JCR Commons library.
If you are using the latest Jackrabbit source code, you need to first
build and install the JCR Commons library in your local Maven 2 repository
before building this component.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout
http://svn.apache.org/repos/asf/jackrabbit/trunk/jackrabbit-api

See the Subversion documentation for other source control features.


Common Questions
================

Q: Which WebDAV features are supported?
A: DAV1,2, DeltaV, Ordering, Access Control, Search

Q: This this WebDAV libarary provide a full dav server?
A: This library only defines interfaces, utilities and common
   classes used for a dav server/client.
   A JCR specific implementation can be found in the 'jcr-server'
   and the 'webapp' project.

Q: How do a get a deployable Jackrabbit installation with WebDAV and
   optional RMI support?
A: The 'webdav' project only serves as library. In order to access
   a Jackrabbit repository please follow the instructions present
   with the 'webapp' project.

Q: Does the WebDAV library has dependency to JSR170
A: No, the library can be used as generic webdav library in any
   other project. There exists a dependency to the jackrabbit-commons
   library for utility classes only.
