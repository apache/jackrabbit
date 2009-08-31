=============================================================
Welcome to Apache Jackrabbit  <http://jackrabbit.apache.org/>
=============================================================

Apache Jackrabbit is a fully conforming implementation of the
Content Repository for Java Technology API (JCR). A content repository
is a hierarchical content store with support for structured and
unstructured content, full text search, versioning, transactions,
observation, and more.

This is the Jackrabbit version used as the Reference Implementation (RI)
of JSR 283.

Apache Jackrabbit is a project of the Apache Software Foundation.

Building Jackrabbit
===================

To build this version of Jackrabbit, you first need to install the
JCR 2.0 API jar in your local Maven repository:

    mvn install:install-file -Dfile=jcr-2.0.jar \
        -DgroupId=javax.jcr -DartifactId=jcr -Dversion=2.0 \
        -Dpackaging=jar

Note that the JCR 2.0 API jar will also be made available from the central
Maven repository shortly after JSR 283 is final.

Once you've installed the jar, you can build Jackrabbit like this:

    mvn clean install

You need Maven 2.0.9 (or higher) with Java 5 (or higher) for the build.
For more instructions, please see the documentation at:

   http://jackrabbit.apache.org/building-jackrabbit.html

