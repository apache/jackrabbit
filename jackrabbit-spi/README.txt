=========================
Welcome to Jackrabbit SPI
=========================

This is the SPI component of the Apache Jackrabbit project.

The SPI defines a layer within a JSR-170 implementation that separates
the transient space from the persistent layer. The main goals were:

(1) Defined support of a client/server architecture
A flat SPI-API lends itself to protocol mappings to protocols 
like WebDAV, SOAP or others in a straightforward yet meaningful way.

(2) Implementation Support
Drawing the boundaries between the repository client and the
repository server allows repository implementation to implement
only the "server" portion and leverage existing generic 
(opensource) clients for things like the "transient space" etc.
This should ease the implementation of the JSR-170 api.

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
package the jacrabbit-spi jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this compoment is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/jackrabbit/trunk/jackrabbit-spi

See the Subversion documentation for other source control features.
