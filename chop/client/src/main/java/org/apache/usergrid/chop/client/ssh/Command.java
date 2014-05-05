package org.apache.usergrid.chop.client.ssh;


public interface Command {

    public String getDescription();

    public CommandType getType();
}