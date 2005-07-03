Overview
-----------------
JCR-commands contains a set of Apache Commons Chain commands
that perform operations in a JCR compliant repository. 
This might be useful for handling the repository either 
declaratively or programatically. 

Since there are no dependencies among commands, you can plug 
custom commands and combine them in any order as far as the 
sequence of actions is allowed in the specification 
(http://www.jcp.org/en/jsr/detail?id=170).

For more information about using Commons Chain please refer 
to http://jakarta.apache.org/commons/chain/

Benchmarking JCR
------------------
You will find a plugin for jmeter under "/jmeter-chain" that 
will allow you to add commons chain commands to 
jmeter in order to prepare custom test plans.

Instructions:
------------------
1 - Install Jmeter.
    see http://jakarta.apache.org/jmeter/.
2 - build jmeter-chain-xx.jar and place it under "lib/ext" [1][2]. 
3 - build jcr-commands-xx.jar and place it under "lib/ext". [2].
4 - Put jcr jar files under "lib" [2]
5 - Put your jcr implementation and all its dependencies under "/lib" [2]

You'll find an simple testplan example under 
"/benchmarking".

[1] See /jmeter-chain/README.txt
[2] The path is relative to the JMeter installation folder.
   