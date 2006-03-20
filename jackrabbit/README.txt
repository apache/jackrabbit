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

The Jackrabbit main project is located in the "jackrabbit" subdirectory
and the "contrib" subdirectory contains various additional modules and
contributed projects.

To checkout the main Jackrabbit source tree, run

   svn checkout http://svn.apache.org/repos/asf/incubator/jackrabbit/trunk/jackrabbit

Once you have a copy of the source code tree, you can use Apache Maven

   http://maven.apache.org/maven-1.x/

to build the project. You should use Maven version 1.0.2 to build Jackrabbit.
Maven 1.1 is also known to work, but Maven 2.0 is not supported. The minimal
command to build and test all the Jackrabbit sources is:

   maven

For more instructions, please see the documentation at:

   http://incubator.apache.org/jackrabbit/doc/building.html

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
Tobias Bocanegra        versioning


Changes
=======

See <http://incubator.apache.org/jackrabbit/changelog-report.html>
