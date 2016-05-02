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

    # VFS Backend base folder URI. e.g, vfsBackendUri=file://${rep.home}/vfsds
    vfsBackendUri=

