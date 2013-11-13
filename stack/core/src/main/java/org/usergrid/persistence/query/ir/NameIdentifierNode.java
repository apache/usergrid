package org.usergrid.persistence.query.ir;


/**
 * Class to represent a UUID based Identifier query
 *
 * @author tnine
 */
public class NameIdentifierNode extends QueryNode {

    private final String name;


    public NameIdentifierNode( String name ) {
        this.name = name;
    }


    @Override
    public void visit( NodeVisitor visitor ) throws Exception {
        visitor.visit( this );
    }


    public String getName() {
        return name;
    }
}
