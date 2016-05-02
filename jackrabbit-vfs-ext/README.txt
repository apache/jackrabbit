====================================================
Welcome to Jackrabbit Commons VFS Extension
====================================================

This is the Commons-VFS Extension component of the Apache Jackrabbit project.

====================================================
Build Instructions
====================================================
To build the latest SNAPSHOT versions of all the components
included here, run the following command with Maven 3:

    mvn clean install

====================================================
Unit Test Instructions
====================================================

1. Testing with the default local file system

    By default, the unit tests use the local file system as backend storage.
    You can run the unit tests with the default temporary local file system like the following:

        mvn clean test

        or

        mvn clean test -Dconfig=src/test/resources/vfs.properties

2. Testing with WebDAV file system

    You can run the unit tests with WebDAV backend file system like the following:

        mvn clean test -Dconfig=src/test/resources/vfs-webdav.properties

    Tip: You can install/run WsgiDAV server (http://wsgidav.readthedocs.io/en/latest/) like the following:

        wsgidav --host=0.0.0.0 --port=8888 --root=/tmp/davroot

3. Testing with HDFS file system

    TODO

====================================================
Configuration Instructions
====================================================

    <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VfsDataStore">
        <param name="secret" value="123456" />
        <param name="config" value="${rep.home}/vfs.properties"/>
    </DataStore>

====================================================
VFS specific configuration properties
====================================================

VFS specific configuration (e.g, ${rep.home}/vfs.properties) can have the following properites:

    # VFS Backend base folder URI. e.g, vfsBaseFolderUri=file://${rep.home}/vfsds
    vfsBaseFolderUri=
    # Asynchronous writing pool size to the backend. 10 by default.
    asyncWritePoolSize=10
