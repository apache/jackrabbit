=======================================================================
Jackrabbit JCA 1.0 Resource Adapter
=======================================================================

Overview
--------

This package includes a JCA resource adapter for Jackrabbit. It's
following the JCA 1.0 specification and can be deployed on a wide
range of application servers. 

Jackrabbit is embedded into the JCA package and is started when
first accessed by the client. If re-deployed, the old repository
configuration will shutdown to minimize alot of opened inactive
repository instances.

Building
--------

To build the resource archive (RAR), use the rar:rar goal in
maven.

  maven rar:rar
  

Deployment
----------

Example deployment configurations are located under ./deploy
directory. 

License (see also LICENSE.txt)
==============================

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
