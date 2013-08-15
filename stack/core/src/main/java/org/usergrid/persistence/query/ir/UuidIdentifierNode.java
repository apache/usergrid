package org.usergrid.persistence.query.ir;

import java.util.UUID;

/**
 * Class to represent a UUID based Identifier query
 * @author tnine
 */
public class UuidIdentifierNode extends QueryNode{


  private final UUID uuid;

  public UuidIdentifierNode(UUID uuid) {
    this.uuid = uuid;
  }

  @Override
  public void visit(NodeVisitor visitor) throws Exception {
    visitor.visit(this);
  }

  public UUID getUuid() {
    return uuid;
  }
}
