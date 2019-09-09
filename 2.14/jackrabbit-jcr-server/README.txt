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

Things to do
============

-------------------------------------------------------------------
TODO 'jcr' server implementation
-------------------------------------------------------------------

general 

- undo incomplete changes in case of exception
- multistatus fuer lock, copy, move, delete wherever required.
- DAV:supported-live-property-set
- timeout: remove expired locks/subscriptions
- improve definition methods/compliance-class
- OPTIONS to *-request-uri (according to RFC 2616)


lock

- implement session-scoped locks. this includes:
  > uncommenting supported-locks entry
  > build caching mechanism for session in case of session-scoped locks.
  > retrieval of cached sessions (currently not possible from IfHeader).
  > open issue in JCR: scope of lock cannot be retrieved.

- JCR lock-token currently not checked for compliance with RFC2518. If the
  token is modified accordingly, setting the lock-token to the subsequent
  session (currently in the WebdavRequestImpl) must be aware of that change....

- transaction locks
  - lock returned upon lock-discovery 
  - remove after timeout (>> releasing cached sessions)
  - define reasonable timeout or make timeout configurable
  - createLock must respect existing locks in the subtree, for lock is always deep.
  - repository transactions ('global') are only possible with jackrabbit, where
  the session represents the XAResource itself.
  since j2ee explicitely requires any usertransaction to be completed
  upon the end of the servletes service method.
  general review necessary....
  
 
observation

- make sure all expired subscriptions are removed.
- subscription: reasonable default/max timeout make it configurable...

versioning

- Additional VERSION-CONTROL Semantics with workspace not implemented.
- BaseLine/Activity not respected yet (see jsr283)
