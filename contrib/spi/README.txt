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

This contribution within the Jackrabbit project contains a Service Provider
Interface (SPI) and an example implementation:

- JCR2SPI : implementation of the JSR170 API covering the transient layer
            
- SPI2DAV : SPI implementation on top of WebDAV currently connecting to
            a Jackrabbit Jcr-Server.
            
This means that the current environment contains the following layers

  JCR -> SPI -> WebDAV -> JCR
           
In a next step the JCR layer at the end of the chain will be replaced
by another SPI implementation, avoiding the extra costs caused by 
having JCR (including transient space and Session management) again:

  JCR -> SPI -> SPI-Implementation
  
 

Aim of the SPI
===============================================================================

The goal of JSR-170 was to specify an API that is easy to use and intuitive 
for a java "application" developer. This means that the API is designed to 
cover the needs of the "consumer" of the API.
Having an additional SPI layer would allow to cover network protocol mappings 
in an abstract manner and also lower the bar for a JCR implementation.

The SPI is layered underneith the transient space, and is not designed
to be used by the application programmer directly. 
Instead it is implemented by a "repository server" (still missing) and used 
by a "repository client" (JCR2SPI) which exposes the JCR API to the
application programmer.

The original design goal was to define a mostly flat, non-"object oriented" 
API. The reasons for this are:

(1) Defined support of a client/server architecture
A flat SPI-API lends itself to protocol mappings to protocols 
like WebDAV, SOAP or others in a straightforward yet meaningful way.
An SPI allows that a repository client and a repository server 
can interact without explicitely specifying protocol mappings.

(2) Implementation Support
Drawing the boundaries between the repository client and the
repository server allows repository implementation to implement
only the "server" portion and leverage existing generic 
(opensource) clients for things like the "transient space" etc.
This should ease the implementation of the JSR-170 api.
 


Requirements
===============================================================================

This project assumes that you have already successfully compiled and 
installed the Jackrabbit commons module as well as the Jackrabbit
Jcr-Server into your maven repository. If this is not the case, go back
to your jackrabbit directory and build it.
NOTE: Jackrabbit is built using Maven2.

> cd ../../
> mvn install

-> installs all required jar files.

> cd ../contrib/spi
> maven



Limitations
===============================================================================

The SPI contribution is still under development and not yet tested.
This applies to the API as well as to the implementations.
  




