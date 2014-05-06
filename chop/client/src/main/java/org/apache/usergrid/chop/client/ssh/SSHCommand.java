package org.apache.usergrid.chop.client.ssh;


public class SSHCommand implements Command {

    private String command;

    public SSHCommand( String command ) {
        this.command = command;
    }


    public String getCommand() {
        return command;
    }


    @Override
    public String getDescription() {
        return new StringBuilder()
                .append( "ssh " )
                .append( command )
                .toString();
    }


    @Override
    public CommandType getType() {
        return CommandType.SSH;
    }
}
