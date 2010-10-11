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

The results are stored as jackrabbit*/target/*.txt report files and can
be combined into an HTML report by running the following command on a
(Unix) system where gnuplot is installed.

    sh plot.sh

Selecting which tests to run
----------------------------

The -Donly command line parameter allows you to specify a regexp for
selecting which performance test cases to run. To run a single test
case, use a command like this:

    mvn clean install -Donly=ConcurrentReadTest

To run all concurrency tests, use:

    mvn clean install -Donly=Concurrent.*Test

Selecting which repository versions/configurations to test
----------------------------------------------------------

The -Drepo command line parameter allows you to specify a regexp for
selecting the repository versions and configurations against which the
performance tests are run. The default setting selects only the official
release versions:

    mvn clean install -Drepo=\d\.\d

To run the tests against all included configurations, use:

    mvn clean install -Drepo=.*

Using a profiler
----------------

To enable a profiler, use the -Dagentlib= command line pameter:

    mvn clean install -Dagentlib=hprof=cpu=samples,depth=10


