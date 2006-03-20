README - JCR Server
-------------------

WebDAV library and WebDAV based JCR client/server connection facility.

Requirements
------------

This project assumes that you have already successfully compiled and 
installed the parent project Jackrabbit and the contrib project jcr-rmi
into your maven repository. If this is not the case, go back to the root
project and launch

  commons:  maven jar:install

which will build and copy Jackrabbit into ~/.maven/repository/org.apache.jackrabbit/jars

Also go to the contrib project jcr-rmi and launch

  maven jar:install

which will build and copy jcr-rmi to ~/.maven/repository/org.apache.jackrabbit/jars

After building all dependencies one can build the actual server webapp

  cd webdav
  maven jar:install

  cd ../server
  maven jar:install

  cd ../client
  maven jar:install

  cd ../webapp
  maven

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

