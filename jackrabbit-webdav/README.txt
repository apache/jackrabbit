README - Jackrabbit WebDAV Library
===============================================================================

Description
-------------------------------------------------------------------------------

This WebDAV library provides interfaces and common utility classes used
for building a WebDAV server or client.

The following RFC have been integrated so far:

- RFC 2518 (WebDAV - HTTP Extensions for Distributed Authoring)
- RFC 3253 (DeltaV - Versioning Extensions to WebDAV)
- RFC 3648 (Ordered Collections Protocol)
- RFC 3744 (Access Control Protocol)
- DAV Searching and Locating  (DASL)

In addition this library defines (unspecified)
- Observation
- Bundling multiple request with extensions to locking

For open issues see -> TODO.txt


Requirements
-------------------------------------------------------------------------------

This project has a dependency to the jackrabbit commons library.
To build the jackrabbit commons:

> cd ../jackrabbit
> maven
> cd modules/commons
> maven jar:install

this will build and copy the required jar file to

~/.maven/repository/org.apache.jackrabbit/jars


Common Questions
-------------------------------------------------------------------------------

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
   Dependency to JSR170 is left to the server implementations.
   -> see jcr-server project.

Q: Where do I find documentation?
A: There isn't much in the way of documentation about these
   things, but that will hopefully improve over time. Please feel
   free to add to our documentation wiki at
   http://wiki.apache.org/jackrabbit/


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

