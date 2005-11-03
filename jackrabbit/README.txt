=======================================================================
Welcome to Apache Jackrabbit  <http://incubator.apache.org/jackrabbit/>
=======================================================================

License (see also LICENSE.txt)
==============================

Copyright 2004-2005 The Apache Software Foundation or its licensors,
                    as applicable.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


Getting Started
===============

Apache Jackrabbit is an effort undergoing incubation at the
Apache Software Foundation. Incubation is required of all newly
accepted projects until a further review indicates that the
infrastructure, communications, and decision making process
have stabilized in a manner consistent with other successful
ASF projects. While incubation status is not necessarily a
reflection of the completeness or stability of the code, it
does indicate that the project has yet to be fully endorsed
by the ASF.  The incubation status is recorded at

   http://incubator.apache.org/projects/jackrabbit.html

Mailing Lists
-------------

To get involved with the Jackrabbit project, start by having a
look at our website (link at top of page) and join our mailing
lists by sending an empty message to

   jackrabbit-dev-subscribe     :at: incubator.apache.org
and
   jackrabbit-commits-subscribe :at: incubator.apache.org

and the dev mailing list archives can be found at

   http://incubator.apache.org/mail/jackrabbit-dev/


Downloading
-----------

The Jackrabbit source code is available via Subversion at

   https://svn.apache.org/repos/asf/incubator/jackrabbit/trunk

and anonymous access is available at

   http://svn.apache.org/repos/asf/incubator/jackrabbit/trunk

or with viewcvs at

   http://svn.apache.org/viewcvs/incubator/jackrabbit/trunk/

The Jackrabbit main project is located in the "jacrabbit" subdirectory
and the "contrib" subdirectory contains various additional modules and
contributed projects.

To checkout the main Jackrabbit source tree, run

   svn checkout http://svn.apache.org/repos/asf/incubator/jackrabbit/trunk/jackrabbit

Once you have a copy of the source code tree, you can use Apache Maven

   http://maven.apache.org/

to build the project.  After installing Maven 1.0, be sure to download the
latest release of the Ant plugin (1.8.1 or later) using a command like

   maven plugin:download -DgroupId=maven \
         -DartifactId=maven-ant-plugin -Dversion=1.8.1

before running one of the maven commands listed at

   http://maven.apache.org/start/use.html

to build the Jackrabbit project and/or documentation.

NOTE: JDK 1.5 users need to download xalan.jar and place it in
$MAVEN_HOME/lib/endorsed (or $JRE/lib/endorsed if using Maven 1.1)
to build the Jackrabbit sources. The reason for this workaround is
explained in

   http://issues.apache.org/jira/browse/JCR-46

Credits
=======

who                     what
--------------------    -----------------------------------------------
Roy Fielding            incubation
Stefan Guggisberg       core, data model, persistence, nodetypes, misc.
David Nuescheler        architecture, api
Dominique Pfister       transactions
Peeter Piegaze          api
Tim Reilly              mavenize
Marcel Reutegger        observation, query
Tobias Strasser         versioning


Changes
=======

See <http://incubator.apache.org/jackrabbit/changelog-report.html>
