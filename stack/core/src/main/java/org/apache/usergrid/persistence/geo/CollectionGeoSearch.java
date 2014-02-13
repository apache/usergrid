package org.apache.usergrid.persistence.geo;


import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.IndexBucketLocator;
import org.apache.usergrid.persistence.cassandra.CassandraService;
import org.apache.usergrid.persistence.geo.model.Point;

import me.prettyprint.hector.api.beans.HColumn;

import static org.apache.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;


/**
 * Class for loading collection search data
 *
 * @author tnine
 */
public class CollectionGeoSearch extends GeoIndexSearcher {

    private final String collectionName;
    private final EntityRef headEntity;


    public CollectionGeoSearch( EntityManager entityManager, IndexBucketLocator locator, CassandraService cass,
                                EntityRef headEntity, String collectionName ) {
        super( entityManager, locator, cass );
        this.collectionName = collectionName;
        this.headEntity = headEntity;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.apache.usergrid.persistence.query.ir.result.GeoIterator.GeoIndexSearcher
     * #doSearch()
     */
    @Override
    protected TreeSet<HColumn<ByteBuffer, ByteBuffer>> doSearch( List<String> geoCells, UUID startId, Point searchPoint,
                                                                 String propertyName, int pageSize ) throws Exception {

        return query( key( headEntity.getUuid(), collectionName, propertyName ), geoCells, searchPoint, startId,
                pageSize );
    }
}
