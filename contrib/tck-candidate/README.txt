This contribution builds a Web Application that starts a
Jackrabbit repository and registers it in JNDI.

On startup the repository is initialized with namespaces,
node types and test content that is required to run the
JSR-170 TCK on Jackrabbit.

In order to run the TCK with this war file follow these steps:

1) delete folder <tck-install>/bin/tck-webapp/jackrabbit
2) delete folder <tck-install>/bin/tck-webapp/webapps/test-candidate
3) copy target/tck-candidate.war to <tck-install>/bin/tck-webapp/webapps
4) start <tck-install>/bin/tck-webapp/server.bat