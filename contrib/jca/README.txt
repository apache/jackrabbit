=======================================================================
Jackrabbit JCA 1.0 Resource Adapter
=======================================================================

Overview
--------

This package includes a JCA resource adapter for Jackrabbit. It's
following the JCA 1.0 specification and can be deployed on a wide
range of application servers. 

Jackrabbit is embedded into the JCA package and is started when
first accessed by the client. If re-deployed, the old repository
configuration will shutdown to minimize alot of opened inactive
repository instances.


Building
--------

To build the resource archive (RAR), use the rar:rar goal in
maven.

  maven rar:rar
  

Deployment
----------

Example deployment configurations are located under ./deploy
directory. 

