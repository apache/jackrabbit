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

The results are stored as jackrabbit*/target/performance-*.txt report files.
