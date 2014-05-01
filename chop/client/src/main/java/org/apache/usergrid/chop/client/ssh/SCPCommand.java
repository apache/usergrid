package org.apache.usergrid.chop.client.ssh;


public class SCPCommand implements Command {

    private String sourceFilePath;
    private String destinationFilePath;


    public SCPCommand( String sourceFilePath, String destinationFilePath ) {
        this.sourceFilePath = sourceFilePath;
        this.destinationFilePath = destinationFilePath;
    }


    public String getSourceFilePath() {
        return sourceFilePath;
    }


    public String getDestinationFilePath() {
        return destinationFilePath;
    }


    @Override
    public CommandType getType() {
        return CommandType.SCP;
    }


    @Override
    public String getDescription() {
        return new StringBuilder()
                .append( "scp " )
                .append( sourceFilePath )
                .append( " to " )
                .append( destinationFilePath )
                .toString();
    }
}
