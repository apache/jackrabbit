Overview
-----------------
JMeter plugin for running Commons Chain commands.
It includes the following classes:
 - org.apache.jmeter.protocol.java.config.gui.ChainConfigGui
 - org.apache.jmeter.protocol.java.control.gui.ChainTestSamplerGui
 - org.apache.jmeter.protocol.java.sampler.ChainSampler
 - org.apache.jmeter.protocol.java.test.SleepChainTest

Dependencies not included in JMeter
-----------------
- commons-chain 
- commons-beanutils
 
Installation
-----------------
 - Build the jar file and place it under /lib/ext.
 - Build a jar file with your custom Chain commands
 - Add the following properties to /org/apache/jmeter/resources/messages.properties
   in order to see the Chain labels correctly.
   1 - chain_config_title=Chain configuration
   2 - chain_request=Chain request

