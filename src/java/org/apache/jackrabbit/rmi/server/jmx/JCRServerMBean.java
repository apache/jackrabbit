package org.apache.jackrabbit.rmi.server.jmx;

public interface JCRServerMBean {

    public void start() throws Exception;

    public void stop() throws Exception;

    public String getLocalAddress();

    public void setLocalAddress(String address);

    public String getRemoteAddress();

    public void setRemoteAddress(String address);

    public String getRemoteEnvironment();

    public void setRemoteEnvironment(String remoteEnvironment);

    public String getLocalEnvironment();

    public void setLocalEnvironment(String localEnvironment);

}
