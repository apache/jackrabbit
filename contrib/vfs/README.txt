For testing you can use tmp provider. This provider will delete 
all the files and folders on repository shutdown.

The providers configuration file (providers.xml) must be in the classpath.

repository.xml Example:
<FileSystem class="org.apache.jackrabbit.core.fs.vfs.VFSFileSystem">
	<!-- full path to the base folder. -->
    <param name="path" value="/repository"/>
    <!-- provider prefix -->
    <param name="prefix" value="tmp"/>
	<!-- providers configuration file name -->
    <param name="config" value="providers.xml"/>
</FileSystem>
