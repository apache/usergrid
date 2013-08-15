package org.usergrid.persistence.query.ir;

import org.usergrid.persistence.Identifier;

/**
 * Class to represent a UUID based Identifier query
 * @author tnine
 */
public class EmailIdentifierNode extends QueryNode{

  private final Identifier identifier;

  public EmailIdentifierNode(Identifier identifier) {
    this.identifier = identifier;
  }

  @Override
  public void visit(NodeVisitor visitor) throws Exception {
    visitor.visit(this);
  }

  public Identifier getIdentifier() {
    return identifier;
  }
}
