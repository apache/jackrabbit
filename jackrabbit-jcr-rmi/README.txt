===================================================
Apache Jackrabbit JCR-RMI
<http://jackrabbit.apache.org/commons/jcr-rmi.html>
===================================================

JCR-RMI is a transparent Remote Method Invocation (RMI) layer for JCR.
The layer makes it possible to remotely access JCR content repositories
and is compatible with all JCR implementations.

JCR-RMI is a part of the JCR Commons subproject of Apache Jackrabbit.
Jackrabbit is a project of the Apache Software Foundation.

Build instructions
==================

To build JCR-RMI, run the following command in this directory:

    mvn clean install

You need Java 5 (or higher) and Maven 2.0.9 (or higher) to do this.

After the build finishes successfully, you can find the compiled JCR-RMI
component in ./target/. The component will also have been installed
in your local Maven repository.

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2010 The Apache Software Foundation.

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
