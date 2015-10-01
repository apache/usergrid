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
package org.apache.usergrid.rest.filters;


import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;
import org.apache.usergrid.management.ManagementService;
import org.apache.usergrid.persistence.EntityManager;
import org.apache.usergrid.persistence.EntityManagerFactory;
import org.apache.usergrid.services.ServiceManagerFactory;
import org.apache.usergrid.system.UsergridSystemMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.*;
import javax.ws.rs.core.Context;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


// TODO: Metering for Jersey 2
@Resource
@PreMatching
@Component
public class MeteringFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {

    }
//
//    @Context
//    protected HttpServletRequest httpServletRequest;
//
//    EntityManagerFactory emf;
//    ServiceManagerFactory smf;
//    Properties properties;
//    ManagementService management;
//    UsergridSystemMonitor usergridSystemMonitor;
//    final Counter activeRequests;
//    final Timer requestTimer;
//
//    private static final Logger logger = LoggerFactory.getLogger( MeteringFilter.class );
//
//
//    public MeteringFilter() {
//        logger.info( "MeteringFilter installed" );
//        this.activeRequests = Metrics.newCounter( MeteringFilter.class, "activeRequests" );
//        this.requestTimer =
//                Metrics.newTimer( MeteringFilter.class, "requests", TimeUnit.MILLISECONDS, TimeUnit.SECONDS );
//    }
//
//
//    @Autowired
//    public void setEntityManagerFactory( EntityManagerFactory emf ) {
//        this.emf = emf;
//    }
//
//
//    @Autowired
//    public void setServiceManagerFactory( ServiceManagerFactory smf ) {
//        this.smf = smf;
//    }
//
//
//    @Autowired
//    @Qualifier("properties")
//    public void setProperties( Properties properties ) {
//        this.properties = properties;
//    }
//
//
//    @Autowired
//    public void setManagementService( ManagementService management ) {
//        this.management = management;
//    }
//
//
//    @Autowired
//    public void setUsergridSystemMonitor( UsergridSystemMonitor usergridSystemMonitor ) {
//        this.usergridSystemMonitor = usergridSystemMonitor;
//    }
//
//
//    @Override
//    public void filter(ContainerRequestContext creq) throws IOException {
//
//        try {
//            activeRequests.inc();
//            creq.setEntityStream( new InputStreamAdapter( creq.getEntityStream() ) );
//            httpServletRequest.setAttribute( "application.request.timetamp", System.currentTimeMillis() );
//            httpServletRequest.setAttribute( "application.request.requestTimer", requestTimer.time() );
//        }
//        catch ( Exception e ) {
//            logger.error( "Unable to capture request", e );
//        }
//    }
//
//    public void countDataWritten( long written ) {
//        TimerContext timer = ( TimerContext ) httpServletRequest.getAttribute( "application.request.requestTimer" );
//        try {
//            UUID applicationId = ( UUID ) httpServletRequest.getAttribute( "applicationId" );
//            Long timestamp = ( Long ) httpServletRequest.getAttribute( "application.request.timetamp" );
//            long time;
//            if ( ( timestamp != null ) && ( timestamp > 0 ) ) {
//                time = System.currentTimeMillis() - timestamp;
//            }
//            else {
//                time = -1;
//            }
//            usergridSystemMonitor.maybeLogPayload( time, "path", httpServletRequest.getRequestURI(), "applicationId",
//                    applicationId );
//            if ( applicationId != null ) {
//
//                Map<String, Long> counters = new HashMap<String, Long>();
//
//
//                if ( time > 0 ) {
//                    logger.trace( "Application: {}, spent {} milliseconds of CPU time", applicationId, time );
//                    counters.put( "application.request.time", time );
//                }
//
//                Long read = ( Long ) httpServletRequest.getAttribute( "application.request.upload" );
//                if ( ( read != null ) && ( read > 0 ) ) {
//                    logger.trace( "Application: {}, received {} bytes", applicationId, written );
//                    counters.put( "application.request.upload", read );
//                }
//
//                if ( written > 0 ) {
//                    logger.trace( "Application: {}, sending {} bytes", applicationId, written );
//                    counters.put( "application.request.download", written );
//                }
//
//                if ( emf != null ) {
//                    EntityManager em = emf.getEntityManager( applicationId );
//                    em.incrementAggregateCounters( null, null, null, counters );
//                }
//                else {
//                    logger.error( "No EntityManagerFactory configured" );
//                }
//            }
//        }
//        catch ( Exception e ) {
//            logger.error( "Unable to capture output", e );
//        }
//        finally {
//            if ( timer != null ) {
//                timer.stop();
//            }
//            activeRequests.dec();
//        }
//    }
//
//
//    public void countDataRead( long read ) {
//        try {
//            if ( read > 0 ) {
//                httpServletRequest.setAttribute( "application.request.upload", read );
//            }
//        }
//        catch ( Exception e ) {
//            logger.error( "Unable to capture input", e );
//        }
//    }
//
//
//    private final class InputStreamAdapter extends FilterInputStream {
//
//        long total = 0;
//
//
//        protected InputStreamAdapter( InputStream in ) {
//            super( in );
//        }
//
//
//        @Override
//        public int available() throws IOException {
//            int i = super.available();
//            return i;
//        }
//
//
//        @Override
//        public int read() throws IOException {
//            int b = super.read();
//            if ( b != -1 ) {
//                total++;
//            }
//            else {
//                countDataRead( total );
//                total = 0;
//            }
//            return b;
//        }
//
//
//        @Override
//        public int read( byte[] b, int off, int len ) throws IOException {
//            int l = super.read( b, off, len );
//            if ( l != -1 ) {
//                total += l;
//            }
//            if ( ( l == -1 ) || ( l < len ) ) {
//                countDataRead( total );
//                total = 0;
//            }
//            return l;
//        }
//
//
//        @Override
//        public int read( byte[] b ) throws IOException {
//            int l = super.read( b );
//            if ( l != -1 ) {
//                total += l;
//            }
//            if ( ( l == -1 ) || ( l < b.length ) ) {
//                countDataRead( total );
//                total = 0;
//            }
//            return l;
//        }
//
//
//        @Override
//        public void close() throws IOException {
//            super.close();
//            countDataRead( total );
//        }
//    }
//
//
//    private final class ContainerResponseWriterAdapter implements ContainerResponseWriter {
//
//        private final ContainerResponseWriter crw;
//        private OutputStreamAdapter out = null;
//
//
//        ContainerResponseWriterAdapter( ContainerResponseWriter crw ) {
//            this.crw = crw;
//        }
//
//
//        @Override
//        public OutputStream writeResponseStatusAndHeaders(
//            long contentLength, ContainerResponse response) throws ContainerException {
//
//            // logger.info("Wrapping output stream");
//            OutputStream o = crw.writeResponseStatusAndHeaders( contentLength, response );
//
//            if ( out == null ) {
//                out = new OutputStreamAdapter( o );
//            }
//
//            return out;
//        }
//
//        @Override
//        public boolean suspend(long l, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
//            return crw.suspend( l, timeUnit, timeoutHandler );
//        }
//
//        @Override
//        public void setSuspendTimeout(long l, TimeUnit timeUnit) throws IllegalStateException {
//            crw.setSuspendTimeout( l, timeUnit );
//        }
//
//        @Override
//        public void commit() {
//            crw.commit();
//        }
//
//        @Override
//        public void failure(Throwable throwable) {
//            crw.failure( throwable );
//        }
//
//        @Override
//        public boolean enableResponseBuffering() {
//            return false;
//        }
//
//
//        private final class OutputStreamAdapter extends FilterOutputStream {
//
//            long total = 0;
//
//
//            public OutputStreamAdapter( OutputStream out ) {
//                super( out );
//            }
//
//
//            public long getTotal() {
//                return total;
//            }
//
//
//            @Override
//            public void write( byte[] b, int off, int len ) throws IOException {
//                out.write( b, off, len );
//                total += len;
//            }
//
//
//            @Override
//            public void write( byte[] b ) throws IOException {
//                out.write( b );
//                total += b.length;
//            }
//
//
//            @Override
//            public void write( int b ) throws IOException {
//                out.write( b );
//                total += 1;
//            }
//        }
//    }
//
//    @Override
//    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
//        try {
//            res.setEntityStream( res.getEntityStream() );
//        }
//        catch ( Exception e ) {
//            logger.error( "Unable to capture response", e );
//        }
//    }
}
