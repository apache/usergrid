package org.usergrid.persistence.query.ir.result;

import org.usergrid.persistence.Results;
import java.util.List;
import java.util.UUID;

public class IDLoader implements ResultsLoader {

  public IDLoader() {
  }

  /* (non-Javadoc)
   * @see org.usergrid.persistence.query.ir.result.ResultsLoader#getResults(java.util.List)
   */
  @Override
  public Results getResults(List<UUID> entityIds) throws Exception {
    Results r = new Results();
    r.setIds(entityIds);
    return r;
  }
}
