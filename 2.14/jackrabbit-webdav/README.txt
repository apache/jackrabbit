====================================
Welcome to Jackrabbit WebDAV Library
====================================

This is the WebDAV Library component of the Apache Jackrabbit project.
This component provides interfaces and common utility classes used for
building a WebDAV server or client. The following RFC have been integrated:

    * RFC 2518 (WebDAV - HTTP Extensions for Distributed Authoring)
    * RFC 3253 (DeltaV - Versioning Extensions to WebDAV)
    * RFC 3648 (Ordered Collections Protocol)
    * RFC 3744 (Access Control Protocol)
    * DAV Searching and Locating  (DASL)
    * Binding Extensions to Web Distributed Authoring and Versioning (WebDAV) (experimental)

In addition this library defines (unspecified)

    * Observation
    * Bundling multiple request with extensions to locking

Common Questions
================

Q: Which WebDAV features are supported?
A: DAV 1, 2, DeltaV, Ordering, Access Control, Search, Bind

Q: This this WebDAV library provide a full dav server?
A: This library only defines interfaces, utilities and common
   classes used for a dav server/client.
   A JCR specific implementation can be found in the 'jcr-server'
   and the 'webapp' project.

Q: How do a get a deployable Jackrabbit installation with WebDAV and
   optional RMI support?
A: The 'webdav' project only serves as library. In order to access
   a Jackrabbit repository please follow the instructions present
   with the 'webapp' project.

Q: Does the WebDAV library has dependency to JSR170
A: No, the library can be used as generic webdav library in any
   other project. There exists a dependency to the jackrabbit-commons
   library for utility classes only.

Things to do
============

-------------------------------------------------------------------
todo webdav/version
-------------------------------------------------------------------

- review: compliance to deltaV
- reconsider feature-sets (see also JCR-394)
- CHECKOUT may contain request body (working-resource, activity, checkout-in-place)
- CHECKIN may contain request body (working-resource, checkout-in-place)
- VERSION-CONTROL may contain request body (workspace f.)
- BASELINE: creation of Baseline resources is not yet supported
  (TODO within AbstractWebDAVServlet)

-------------------------------------------------------------------
todo webdav/ordering
-------------------------------------------------------------------

- respect Position header with creation of new collection members by
  PUT, COPY, MKCOL requests

-------------------------------------------------------------------
todo webdav/search
-------------------------------------------------------------------

- SearchResource should extend DavResource
- Library misses support for the DAV:basicsearch

-------------------------------------------------------------------
todo webdav/transaction
-------------------------------------------------------------------

- review naming of the lock scopes. 'global','local' are not correct in
  this context.
- j2ee explicitely requires any usertransaction to be completed
  upon the end of the servletes service method.
  general review necessary.
