---------------------------------
Jackrabbit Performance Test Suite
---------------------------------

This directory contains a simple performance test suite that covers all
major and minor Jackrabbit releases since 1.0. Use the following command
to run this test suite:

    mvn clean install

Note that the test suite will take more than an hour to complete, and to
avoid distorting the results you should avoid putting any extra load on
the computer while the test suite is running.

The results are stored as jackrabbit*/target/*.txt report files.

Selecting which tests to run
----------------------------

The -Donly= command line parameter allows you to specify a regexp for
selecting which performance test cases to run. To run a single test
case, use a command like this:

    mvn clean install -Donly=ConcurrentReadTest

To run all concurrency tests, use:

    mvn clean install -Donly=Concurrent.*Test

Using a profiler
----------------

To enable a profiler, use the -Dagentlib= command line pameter:

    mvn clean install -Dagentlib=hprof=cpu=samples,depth=10


