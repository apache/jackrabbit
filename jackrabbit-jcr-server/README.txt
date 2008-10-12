================================
Welcome to Jackrabbit JCR Server
================================

This is the JCR Server component of the Apache Jackrabbit project.
This component contains two WebDAV based JCR server implementations:

   1) WebDAV server ('simple')

      DAV1,2 compliant WebDAV server implementation to access a
      JSR170 repository.
  
      Futher information such as configuration as well as the
      SimpleWebdavServlet itself may be found in the 'webapp' project.
  
      Packages:
      - org.apache.jackrabbit.server         = server
      - org.apache.jackrabbit.server.io      = import/export
      - org.apache.jackrabbit.webdav.simple  = dav-resource implementation + config.

      Servlet (webapp project):
      - org.apache.jackrabbit.j2ee.SimpleWebdavServlet.java 
  

   2) 'jcr' server:

      Server used to remove JSR170 calls via WebDAV.
      No particular effort to be compliant to WebDAV related RFCs.
   
      The 'client' counterpart of this server is under development and
      can be found within the <jackrabbit>/contrib/spi contribution.
   
      Packages:
      - org.apache.jackrabbit.server     = server
      - org.apache.jackrabbit.server.jcr = jcr-server specific server part
      - org.apache.jackrabbit.webdav.jcr = dav-resources, reports, properties   
   
      Servlet (webapp project):
      - org.apache.jackrabbit.j2ee.JCRServerServlet.java
           
      Further reading:
      - http://www.day.com/jsr170/server/JCR_Webdav_Protocol.zip
