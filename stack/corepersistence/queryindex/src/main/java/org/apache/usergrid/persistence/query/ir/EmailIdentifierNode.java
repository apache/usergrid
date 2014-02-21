package org.apache.usergrid.persistence.query.ir;


/**
 * Class to represent a UUID based Identifier query
 *
 * @author tnine
 */
public class EmailIdentifierNode extends QueryNode {

    private final String identifier;


    public EmailIdentifierNode( String identifier ) {
        this.identifier = identifier;
    }


    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    public String getIdentifier() {
        return identifier;
    }
}
