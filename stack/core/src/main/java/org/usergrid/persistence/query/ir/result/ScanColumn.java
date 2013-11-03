package org.usergrid.persistence.query.ir.result;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 *
 * An interface that represents a column
 * @author: tnine
 *
 */
public interface ScanColumn{

  /**
   * Get the uuid from the column
   * @return
   */
  public UUID getUUID();

  /**
   * Get the cursor value of this column
   * @return
   */
  public ByteBuffer getCursorValue();

}
