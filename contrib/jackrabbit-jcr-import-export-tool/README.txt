================================================
Welcome to the Jackrabbit JCR Import/Export tool
================================================

This is the Jackrabbit JCR Import/Export tool which provides
command-line functionality to import and export JCR content in a
persistence-layer independent way.

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
package the jacrabbit-api jar using the following command:

    mvn package

See the Maven 2 documentation for other build features.

The latest source code for this compoment is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/jackrabbit/trunk/contrib/jackrabbit-jcr-import-export-tool

See the Subversion documentation for other source control features.


Installation
============

After building, the created jar file in the target/ directory can
be "installed" by unpacking it's lib directory which contains libraries.

  jar -xf jackrabbit-jcr-import-export-tool-<version>.jar lib/


Exporting repository content
============================

Options (all required):
 -c filename  : specifies the filename of the source repository configuration (repository.xml)
 -d directory : specifies the source repository home directory
 -nt          : specifies to include the nodetypes in the export
 -ns          : specifies to include the namespace mappings in the export
 -e filename [path list] : specifies the name of the zip file to which the exported content will be written,
                           and also a list of absolute paths of nodes that will be exported

To export the /myroot node and all namespace mappings and custom nodetypes:

  java -Xmx512M -jar JcrImportExportTool-<version>.jar -c repository.xml -d repository/ -ns -nt -e export.zip /myroot


Importing repository content
============================
Options (all required):
 -c filename  : specifies the filename of the target repository configuration (repository.xml)
 -d directory : specifies the target repository home directory
 -nt          : specifies to include the nodetypes in the import
 -ns          : specifies to include the namespace mappings in the import
 -i filename  : specifies the name of the zip file from which the content will be read

To import the content in the previously generated export.zip in another repository:

  java -Xmx512M -jar JcrImportExportTool-<version>.jar -c repository2.xml -d repository2/ -ns -nt -i export.zip


Open issues
===========

Think about using Workspace.importXML method to avoid out-of-memory errors. This
might, however, give reference constraint violations when multiple nodes are imported.
