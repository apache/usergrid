package org.usergrid.persistence.query.ir.result;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


/** Simple utility to convert Scan Columns collections to lists */
public class ScanColumnTransformer {

    public static List<UUID> getIds( Collection<ScanColumn> cols ) {

        List<UUID> ids = new ArrayList<UUID>( cols.size() );

        for ( ScanColumn col : cols ) {
            ids.add( col.getUUID() );
        }

        return ids;
    }
}
