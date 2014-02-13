package org.apache.usergrid.persistence.geo;


import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.geo.model.Point;

import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.Schema.INDEX_CONNECTIONS;
import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;


/**
 * Class for loading connection data
 *
 * @author tnine
 */
public class ConnectionGeoSearch extends GeoIndexSearcher {

    private final UUID connectionId;


    public ConnectionGeoSearch( EntityManager entityManager, IndexBucketLocator locator, CassandraService cass,
                                UUID connectionId ) {
        super( entityManager, locator, cass );

        this.connectionId = connectionId;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.apache.usergrid.persistence.query.ir.result.GeoIterator.GeoIndexSearcher
     * #doSearch()
     */
    @Override
    protected TreeSet<HColumn<ByteBuffer, ByteBuffer>> doSearch( List<String> geoCells, UUID startId, Point searchPoint,
                                                                 String propertyName, int pageSize ) throws Exception {

        return query( key( connectionId, INDEX_CONNECTIONS, propertyName ), geoCells, searchPoint, startId, pageSize );
    }
}
