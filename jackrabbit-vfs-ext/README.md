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

### Testing with HDFS file system

TODO

## Configuration Instructions

### With local file system

        <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VfsDataStore">
            <!-- VFSDataStore specific parameters -->
            <param name="baseFolderUri" value="file://${rep.home}/vfsds" />
            <param name="asyncWritePoolSize" value="10" />
            <!-- CachingDataStore specific parameters -->
            <param name="secret" value="123456789"/>
            <param name="minRecordLength " value="16384"/>
            <param name="cacheSize" value="68719476736"/>
            <param name="cachePurgeTrigFactor" value="0.95d"/>
            <param name="cachePurgeResizeFactor" value="0.85d"/>
            <param name="continueOnAsyncUploadFailure" value="false"/>
            <param name="concurrentUploadsThreads" value="10"/>
            <param name="asyncUploadLimit" value="100"/>
            <param name="uploadRetries" value="3"/>
        </DataStore>

### With WebDAV file system

        <DataStore class="org.apache.jackrabbit.vfs.ext.ds.VFSDataStore">
            <!-- VFSDataStore specific parameters -->
            <param name="baseFolderUri" value="webdav://localhost:8888/vfsds" />
            <param name="asyncWritePoolSize" value="10" />
            <param name="fileSystemOptionsPropertiesInString"
                   value="http.maxTotalConnections = 200&#13;http.maxConnectionsPerHost = 200" />
            <!-- CachingDataStore specific parameters -->
            <param name="secret" value="123456789"/>
            <param name="minRecordLength " value="16384"/>
            <param name="cacheSize" value="68719476736"/>
            <param name="cachePurgeTrigFactor" value="0.95d"/>
            <param name="cachePurgeResizeFactor" value="0.85d"/>
            <param name="continueOnAsyncUploadFailure" value="false"/>
            <param name="concurrentUploadsThreads" value="10"/>
            <param name="asyncUploadLimit" value="100"/>
            <param name="uploadRetries" value="3"/>
        </DataStore>
