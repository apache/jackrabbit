README - JCR examples
---------------------

=======================================================================
Welcome to Jackrabbit Deployment Test
=======================================================================

License (see also LICENSE.txt)
==============================

Collective work: Copyright 2006 The Apache Software Foundation.

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

The Jackrabbit Deployment Test is a webapp designed to demonstrate and
test the configuration of two styles of Jackrabbit deployment:

- Model 1, Embedded: This style of deployment embeds a Jackrabbit
                     repository within a web application.
- Model 2, Shared:   This style of deployment makes a Jackrabbit repository
                     available to all web applications in the application
                     server.

Prerequisites
-------------

In order to build the web application and create the documentation that
accompanies it, you will need to have Maven (version 1.x should work, but
1.0.2 is known to work) installed on your computer. If you want to run
the application, you will also need a working installation of Tomcat 5.5.x
(version 5.5.15 is known to work).

Follow the links for these two programs in the "More Information" section
at the end of this document to learn how to download and install them.

Requirements
------------

This project assumes that you have already successfully compiled and 
installed the parent project Jackrabbit into your maven repository.
If this is not the case, go back to the root project and launch

  maven jar:install

which will build and copy Jackrabbit into ~/.maven/repository/jackrabbit/jars


Building the web application
----------------------------

Maven's "war" goal will build the web application .war file for the project.
From the root directory of the projecct, run maven with the following
command:

    maven war

The web application will be created at

    target/jackrabbit_deployment_test.war

After you have followed the configuration instructions in the project's
documentation site, copy this file into Tomcat's webapps/ directory to
install it into the application server. Once installed, you should be able
to access it at

    http://localhost:8080/jackrabbit_deployment_test/index.html

This assumes you have left Tomcat's port configured at 8080. If you changed
the port, you should replace "8080" in the URL above with the port you
assigned to Tomcat.

More Information
================

Jackrabbit:         http://jackrabbit.apache.org/
Tomcat:             http://tomcat.apache.org
Maven:              http://maven.apache.org
Maven 1.x Download: http://maven.apache.org/maven-1.x/start/download.html

Credits
=======

who                     what
--------------------    -----------------------------------------------
Mark Slater             initial development and documentation
