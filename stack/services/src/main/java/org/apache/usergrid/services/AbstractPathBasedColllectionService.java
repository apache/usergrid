/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.usergrid.services;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.persistence.EntityRef;
import org.apache.usergrid.persistence.Query;
import org.apache.usergrid.persistence.Schema;
import org.apache.usergrid.services.ServiceParameter.IdParameter;
import org.apache.usergrid.services.ServiceParameter.NameParameter;
import org.apache.usergrid.services.ServiceParameter.QueryParameter;
import org.apache.usergrid.services.exceptions.ServiceInvocationException;
import static org.apache.usergrid.services.ServiceParameter.filter;
import static org.apache.usergrid.services.ServiceParameter.mergeQueries;
import static org.apache.usergrid.utils.InflectionUtils.pluralize;
import static org.apache.usergrid.utils.ListUtils.dequeueCopy;
import static org.apache.usergrid.utils.ListUtils.isEmpty;


public class AbstractPathBasedColllectionService extends AbstractCollectionService {

    private static final Logger logger = LoggerFactory.getLogger( AbstractPathBasedColllectionService.class );


    public AbstractPathBasedColllectionService() {
        super();
    }


    @Override
    public ServiceContext getContext( ServiceAction action, ServiceRequest request, ServiceResults previousResults,
                                      ServicePayload payload ) throws Exception {

        EntityRef owner = request.getOwner();
        String collectionName = "application".equals( owner.getType() ) ? pluralize( getServiceInfo().getItemType() ) :
                                getServiceInfo().getCollectionName();

        EntityRef pathEntity = null;
        List<ServiceParameter> parameters = filter( request.getParameters(), replaceParameters );
        ServiceParameter first_parameter = null;
        if ( !isEmpty( parameters ) ) {
            first_parameter = parameters.get( 0 );

            if ( first_parameter instanceof NameParameter ) {

                if ( hasServiceMetadata( first_parameter.getName() ) ) {
                    return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                            parameters, payload ).withServiceMetadata( first_parameter.getName() );
                }
                else if ( hasServiceCommand( first_parameter.getName() ) ) {
                    return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                            parameters, payload ).withServiceCommand( first_parameter.getName() );
                }

                List<String> aliases = new ArrayList<String>();
                String alias = "";
                String slash = "";
                for ( ServiceParameter parameter : parameters ) {
                    if ( parameter instanceof NameParameter ) {
                        String name = parameter.getName();
                        if ( ( entityDictionaries != null ) && ( entityDictionaries
                                .contains( new EntityDictionaryEntry( name ) ) ) ) {
                            break;
                        }
                        if ( Schema.getDefaultSchema().hasCollection( getServiceInfo().getItemType(), name ) ) {
                            break;
                        }
                        alias += slash + name;
                        aliases.add( alias );
                        slash = "/";
                    }
                    else {
                        break;
                    }
                }
                if ( !isEmpty( aliases ) ) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("Found {} potential paths", aliases.size());
                    }
                    Map<String, EntityRef> aliasedEntities = em.getAlias( getEntityType(), aliases );
                    for ( int i = aliases.size() - 1; i >= 0; i-- ) {
                        alias = aliases.get( i );
                        pathEntity = aliasedEntities.get( alias );
                        if ( pathEntity != null ) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Found entity {} of type {} for alias {}",
                                        pathEntity.getUuid(), pathEntity.getType(), alias);
                            }
                            parameters = parameters.subList( i + 1, parameters.size() );
                            first_parameter = new IdParameter( pathEntity.getUuid() );
                            // if (!isEmpty(parameters)) {
                            // first_parameter = parameters.get(0);
                            // }
                            break;
                        }
                    }
                }
            }

            if ( pathEntity == null ) {
                parameters = dequeueCopy( parameters );
            }
        }

        Query query = null;
        if ( first_parameter instanceof QueryParameter ) {
            query = first_parameter.getQuery();
        }
        parameters = mergeQueries( query, parameters );

        if ( first_parameter instanceof IdParameter ) {
            UUID id = first_parameter.getId();
            return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                    Query.fromUUID( id ), parameters, payload );
        }
        else if ( first_parameter instanceof NameParameter ) {
            String name = first_parameter.getName();
            return new ServiceContext( this, action, request, previousResults, owner, collectionName,
                    Query.fromIdentifier( name ), parameters, payload );
        }
        else if ( query != null ) {
            return new ServiceContext( this, action, request, previousResults, owner, collectionName, query, parameters,
                    payload );
        }
        else if ( first_parameter == null ) {
            return new ServiceContext( this, action, request, previousResults, owner, collectionName, null, null,
                    payload );
        }

        throw new ServiceInvocationException( request, "No parameter found" );
    }
}
