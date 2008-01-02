==========================
Welcome to Jackrabbit Site
==========================

This project contains the source of the Jackrabbit web site
http://jackrabbit.apache.org/. 

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


Building and publishing the site
================================

The Apache Jackrabbit web site at http://jackrabbit.apache.org/ is
generated with Maven 2, committed to the source repository, and finally
updated to the live web server.

To edit and publish the contents of the site you first need to check out the
Jackrabbit trunk as well as the Jackrabbit site directory:

    svn checkout https://svn.apache.org/repos/asf/jackrabbit/trunk jackrabbit
    svn checkout https://svn.apache.org/repos/asf/jackrabbit/site

To make changes, first edit the source content under
"jackrabbit/jackrabbit-site/src/site" and then generate the HTML versions
using Maven.

Here is the process for generating the site content after the above
checkouts have been done and changes made:

    $ svn update jackrabbit site             # Get the latest changes
    $ cd jackrabbit/jackrabbit-site
    $ mvn site

You can review the site by pointing your browser to target/site/index.html
inside the jackrabbit/jackrabbit-site directory.

If the site looks good, you can publish the changes like this:

    $ cp -f -r target/site/* ../../site
    $ cd ../../site
    $ find . -name '*.html" | xargs perl -i -pe 's/\r\n/\n/'
                                             # Fix line endings
    $ svn status | egrep '^\?'               # Check for new files
    $ svn add ...                            # If new files are included
    $ svn diff | less                        # Check sanity
    $ svn commit -m 'site: Updated site'

Note the CRLF fix, it is needed because of some of the Maven 2 xdoc
processing introduces Windows line breaks in the generated html.

Once the updated site has been committed, the checked out version on
people.apache.org needs to be updated:

    $ umask 002; svn update /www/jackrabbit.apache.org

The contents of /www/jackrabbit.apache.org are automatically
synchronized to the actual public web server every few hours.

Note that the above process is only sufficient for changes to existing
files and new files.  When source files are deleted or moved under
jackrabbit, their corresponding docs files have to be separately
deleted or moved within the jackrabbit-site tree (otherwise, the old
generated file will just sit there and stagnate).
