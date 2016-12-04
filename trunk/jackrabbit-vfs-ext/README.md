#Welcome to Jackrabbit Commons VFS Extension

This is the Commons-VFS Extension component of the Apache Jackrabbit project.

## Build Instructions

To build the latest SNAPSHOT versions of all the components
included here, run the following command with Maven 3:

       mvn clean install

## Unit Test Instructions

### Testing with the default local file system

By default, the unit tests use the local file system as backend storage.
You can run the unit tests with the default temporary local file system like the following:

        mvn clean test

### Testing with WebDAV file system

You can run the unit tests with WebDAV backend file system like the following:

        mvn clean test -Dconfig=src/test/resources/vfs-webdav.properties

*Tip*: You can install/run WsgiDAV server (http://wsgidav.readthedocs.io/en/latest/) like the following:

        wsgidav --host=0.0.0.0 --port=8888 --root=/tmp/davroot

### Testing with SFTP file system

You can run the unit tests with WebDAV backend file system like the following:

        mvn clean test -Dconfig=src/test/resources/vfs-sftp.properties

## Configuration Instructions

### With local file system

          <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VfsDataStore">
            <!-- VFSDataStore specific parameters -->
            <param name="baseFolderUri" value="file://${rep.home}/vfsds" />
            <param name="asyncWritePoolSize" value="10" />
            <!-- CachingDataStore specific parameters -->
            <param name="secret" value="123456789"/>
          </DataStore>

### With WebDAV file system

          <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VFSDataStore">
            <param name="config" value="${catalina.base}/conf/vfs2-datastore.properties" />
            <!-- VFSDataStore specific parameters -->
            <param name="asyncWritePoolSize" value="10" />
            <!-- CachingDataStore specific parameters -->
            <param name="secret" value="123456789"/>
          </DataStore>

vfs2-datastore.properties:

```
        baseFolderUri = webdav://tester:secret@localhost:8888/vfsds
        # Properties to build org.apache.commons.vfs2.FileSystemOptions at runtime when resolving the base folder.
        # Any properties, name of which is starting with 'fso.', are used to build FileSystemOptions
        # after removing the 'fso.' prefix. See VFS2 documentation for the detail.
        fso.http.maxTotalConnections = 200
        fso.http.maxConnectionsPerHost = 200
        fso.http.preemptiveAuth = false
```

### With SFTP file system

          <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VFSDataStore">
            <param name="config" value="${catalina.base}/conf/vfs2-datastore.properties" />
            <!-- VFSDataStore specific parameters -->
            <param name="asyncWritePoolSize" value="10" />
            <!-- CachingDataStore specific parameters -->
            <param name="secret" value="123456789"/>
          </DataStore>

vfs2-datastore.properties:

```
        baseFolderUri = sftp://tester:secret@localhost/vfsds
        # Properties to build org.apache.commons.vfs2.FileSystemOptions at runtime when resolving the base folder.
        # Any properties, name of which is starting with 'fso.', are used to build FileSystemOptions
        # after removing the 'fso.' prefix. See VFS2 documentation for the detail.
```

License
-------

(see the top-level [LICENSE.txt](../LICENSE.txt) for full license details)

Collective work: Copyright 2012 The Apache Software Foundation.

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
