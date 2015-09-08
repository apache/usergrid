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
package org.apache.usergrid.rest;


import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.server.mvc.Viewable;



// TODO: text to JSON swapping for Jersey 2

/**
 * A writer that will redirect requests for "text/html" to MediaType.APPLICATION_JSON if the value
 * returned by the resource is an instance of JSONWithPadding
 *
 * @author tnine
 *
 */
@Provider
@Produces( MediaType.TEXT_HTML )
public class TextToJsonSwapWriter { // implements MessageBodyWriter<JSONWithPadding> {
//
//
//    private static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;
//
//    @Context
//    MessageBodyWorkers bodyWorker;
//
//
//    @Override
//    public boolean isWriteable( final Class<?> type, final Type genericType, final Annotation[] annotations,
//                                final MediaType mediaType ) {
//
//
//        // if type is Viewable them we want to return HTML, so no swap needed
//        if ( type.isAssignableFrom(Viewable.class) ) {
//            return false;
//        }
//
//        // this should only map no media type, or text/html requests with json responses
//        final boolean mediaTypeCorrect = mediaType == null || MediaType.TEXT_HTML_TYPE.equals( mediaType );
//
//        if(!mediaTypeCorrect){
//            return false;
//        }
//
//        return true;
//
//// JsonSerializableWithType no longer exists in FasterXML Jackson
////
////        final boolean serializableAnnotation = type.getAnnotation( XmlRootElement.class ) != null;
////        final boolean jsonSerializable = JsonSerializableWithType.class.isAssignableFrom( type );
////        return serializableAnnotation || jsonSerializable;
//    }
//
//
//    @Override
//    public long getSize( final JSONWithPadding jsonWithPadding, final Class<?> type, final Type genericType,
//                         final Annotation[] annotations, final MediaType mediaType ) {
//        return -1;
//    }
//
//
//    @Override
//    public void writeTo( final JSONWithPadding jsonWithPadding, final Class<?> type, final Type genericType,
//                         final Annotation[] annotations, final MediaType mediaType,
//                         final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream )
//            throws IOException, WebApplicationException {
//
//
//        Object jsonEntity = jsonWithPadding.getJsonSource();
//        Type entityGenericType = jsonEntity.getClass();
//        Class<?> entityType = jsonEntity.getClass();
//
//        final boolean genericEntityUsed = jsonEntity instanceof GenericEntity;
//
//        if ( genericEntityUsed ) {
//            GenericEntity ge = ( GenericEntity ) jsonEntity;
//            jsonEntity = ge.getEntity();
//            entityGenericType = ge.getType();
//            entityType = ge.getRawType();
//        }
//
//
//        //replace the text/html content type with application Json
//        httpHeaders.remove( HttpHeaders.CONTENT_TYPE);
//
//        httpHeaders.putSingle( HttpHeaders.CONTENT_TYPE,  JSON_MEDIA_TYPE);
//
//
//
//
//        MessageBodyWriter bw = bodyWorker.getMessageBodyWriter( entityType, entityGenericType, annotations, JSON_MEDIA_TYPE );
//
//        if ( bw == null ) {
//
//            throw new RuntimeException( "Couldn't find the serailziation writer for json type");
//        }
//
//
//
//
//        bw.writeTo( jsonEntity, entityType, entityGenericType, annotations, JSON_MEDIA_TYPE, httpHeaders,
//                entityStream );
//
//
//    }
}
