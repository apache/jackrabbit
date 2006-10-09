
===========================================================================
Jackrabbit NTDoc - Node Type Documentation System
===========================================================================

Overview
--------

This package holds the NTDoc tool. It is a node type documentation tool
for Jackrabbit that is able to parse both CND and XML node type definitions.
Documentation output is generated in HTML and is very similar to JavaDoc.

Building
--------

To build the NTDoc package using Maven 2, use the package target. Write the
following on the command line:

  mvn package

Running
-------

The jar file is created as an executable jar. To run the tool to get usage
information, type the following:

  java -jar jackrabbit-ntdoc-1.0.jar

To generate node type documentation for both buildtin types and my custom
types, type the following:

  java -jar jackrabbit-ntdoc-1.0.jar -d output -t "My Docs"
    nodetypes/buildin.cnd nodetypes/mycustom.cnd

NOTE: The tool will not generate any links between node types in different
files if not both files are specified.

License (see also LICENSE.txt)
------------------------------

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



  
