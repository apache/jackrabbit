=========================
Welcome to Jackrabbit SPI
=========================

This is the SPI component of the Apache Jackrabbit project.

The SPI defines a layer within a JSR-170 implementation that separates
the transient space from the persistent layer. The main goals were:

(1) Defined support of a client/server architecture
A flat SPI-API lends itself to protocol mappings to protocols 
like WebDAV, SOAP or others in a straightforward yet meaningful way.

(2) Implementation Support
Drawing the boundaries between the repository client and the
repository server allows repository implementation to implement
only the "server" portion and leverage existing generic 
(opensource) clients for things like the "transient space" etc.
This should ease the implementation of the JSR-170 api.
