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
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.map.JsonSerializable;
import org.codehaus.jackson.map.JsonSerializableWithType;

import com.sun.jersey.api.json.JSONWithPadding;
import com.sun.jersey.spi.MessageBodyWorkers;


/**
 * A writer that will redirect requests for "text/html" to "application/json" if the value
 * returned by the resource is an instance of JSONWithPadding
 *
 * @author tnine
 *
 */
@Provider
@Produces( MediaType.TEXT_HTML )
public class TextToJsonSwapWriter implements MessageBodyWriter<JSONWithPadding> {


    private static final MediaType JSON_MEDIA_TYPE = MediaType.APPLICATION_JSON_TYPE;

    @Context
    MessageBodyWorkers bodyWorker;


    @Override
    public boolean isWriteable( final Class<?> type, final Type genericType, final Annotation[] annotations,
                                final MediaType mediaType ) {

        //this should only map no media type, or text/html requests with json responses

        final boolean mediaTypeCorrect = mediaType == null || MediaType.TEXT_HTML_TYPE.equals( mediaType );

        if(!mediaTypeCorrect){
            return false;
        }


        final boolean serializableAnnotation = type.getAnnotation( XmlRootElement.class ) != null;


        final boolean jsonSerializable = JsonSerializableWithType.class.isAssignableFrom( type );


        return serializableAnnotation || jsonSerializable;
    }


    @Override
    public long getSize( final JSONWithPadding jsonWithPadding, final Class<?> type, final Type genericType,
                         final Annotation[] annotations, final MediaType mediaType ) {
        return -1;
    }


    @Override
    public void writeTo( final JSONWithPadding jsonWithPadding, final Class<?> type, final Type genericType,
                         final Annotation[] annotations, final MediaType mediaType,
                         final MultivaluedMap<String, Object> httpHeaders, final OutputStream entityStream )
            throws IOException, WebApplicationException {


        Object jsonEntity = jsonWithPadding.getJsonSource();
        Type entityGenericType = jsonEntity.getClass();
        Class<?> entityType = jsonEntity.getClass();

        final boolean genericEntityUsed = jsonEntity instanceof GenericEntity;

        if ( genericEntityUsed ) {
            GenericEntity ge = ( GenericEntity ) jsonEntity;
            jsonEntity = ge.getEntity();
            entityGenericType = ge.getType();
            entityType = ge.getRawType();
        }


        //replace the text/html content type with application Json
        httpHeaders.remove( HttpHeaders.CONTENT_TYPE);

        httpHeaders.putSingle( HttpHeaders.CONTENT_TYPE,  JSON_MEDIA_TYPE);




        MessageBodyWriter bw = bodyWorker.getMessageBodyWriter( entityType, entityGenericType, annotations, JSON_MEDIA_TYPE );

        if ( bw == null ) {

            throw new RuntimeException( "Couldn't find the serailziation writer for json type");
        }




        bw.writeTo( jsonEntity, entityType, entityGenericType, annotations, JSON_MEDIA_TYPE, httpHeaders,
                entityStream );


    }
}
