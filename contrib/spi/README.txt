===============================================================================
Welcome to Apache Jackrabbit  - SPI
===============================================================================

License (see also LICENSE.txt)
===============================================================================

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



Introduction
===============================================================================

This contribution within the Jackrabbit project contains example 
implementations for Jackrabbit SPI:

- SPI2JCR : implements the SPI on top of JCR (e.g. Jackrabbit)
- SPI2DAV : SPI implementation on top of WebDAV connecting to a Jackrabbit Jcr-Server.

- SPI-RMI : rmi layer

In addition the contrib contains

- JCR2SPI : implementation of the JSR170 API covering the transient layer.


This means that the current environment contains the following layers

  JCR -> SPI -> JCR
  JCR -> SPI -> WebDAV -> JCR


Requirements
===============================================================================

This project assumes that you have already successfully compiled and 
installed the Jackrabbit SPI, SPI commons and JCR commons module as well 
as the Jackrabbit Jcr-Server into your maven repository. If this is not the case, 
go back to your jackrabbit directory and build it.
NOTE: Jackrabbit is built using Maven2.

> cd ../../
> mvn install

-> installs all required jar files.

> cd ../contrib/spi
> mvn install
