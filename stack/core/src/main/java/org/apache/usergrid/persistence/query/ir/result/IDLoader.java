package org.apache.usergrid.persistence.query.ir.result;


import java.util.List;

import org.apache.usergrid.persistence.Results;


public class IDLoader implements ResultsLoader {

    public IDLoader() {
    }


    /* (non-Javadoc)
     * @see org.apache.usergrid.persistence.query.ir.result.ResultsLoader#getResults(java.util.List)
     */
    @Override
    public Results getResults( List<ScanColumn> entityIds ) throws Exception {
        Results r = new Results();
        r.setIds( ScanColumnTransformer.getIds( entityIds ) );
        return r;
    }
}
