package org.apache.usergrid.persistence.collection.mvcc.stage;


import java.util.UUID;

import org.apache.usergrid.persistence.collection.mvcc.entity.MvccEntity;
import org.apache.usergrid.persistence.collection.util.EntityUtils;
import org.apache.usergrid.persistence.model.entity.Entity;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;

import com.google.common.base.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** Helper class for generating MvccEntities and Entities
 * @author tnine */
public class TestEntityGenerator {


    /**
     * Return an MvccEntityMock with valid inputs from the supplied entity
     * @param entity
     * @return
     */
    public static MvccEntity fromEntity(Entity entity){

        final MvccEntity mvccEntity = mock(MvccEntity.class);
        when(mvccEntity.getId()).thenReturn( entity.getId());
        when(mvccEntity.getVersion()).thenReturn( entity.getVersion() );
        when( mvccEntity.getEntity() ).thenReturn( Optional.of( entity ) );

        return mvccEntity;
    }


    /**
     * Generate a valid entity
     * @return
     * @throws IllegalAccessException
     */
    public static Entity generateEntity() throws IllegalAccessException {
            final Entity entity = new Entity( "test" );
            final UUID version = UUIDGenerator.newTimeUUID();

            EntityUtils.setVersion( entity, version );

            return entity;
        }
}
