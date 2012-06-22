/*******************************************************************************
 * Copyright 2012 Apigee Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.usergrid.rest.filters;

import static org.junit.Assert.assertEquals;
import static org.usergrid.utils.MapUtils.hashMap;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.usergrid.rest.AbstractRestTest;
import org.usergrid.utils.JsonUtils;

/**
 * @author tnine
 * 
 */
public class ContentTypeResourceTest extends AbstractRestTest {

    /**
     * Creates a simple entity of type game. Does not set the content type. The
     * type should be set to json to match the body
     * @throws  
     * @throws Exception 
     */
    @Test
    public void correctHeaders() throws Exception {
        Map<String, String> data = hashMap("name", "Solitaire");

        String json = JsonUtils.mapToFormattedJsonString(data);

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost(super.getBaseURI().getHost(), super.getBaseURI().getPort());
  
        
        HttpPost post = new HttpPost("/test-organization/test-app/games");
        post.setEntity(new StringEntity(json));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ access_token);
        post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        

        HttpResponse rsp = client.execute(host, post);

        printResponse(rsp);

        assertEquals(200, rsp.getStatusLine().getStatusCode());

    }
    
    /**
     * Creates a simple entity of type game. Does not set the accept header. The
     * type should be set to json to match the body
     * @throws  
     * @throws Exception 
     */
    @Test
    public void contentTypeJson() throws Exception {
        Map<String, String> data = hashMap("name", "Solitaire");

        String json = JsonUtils.mapToFormattedJsonString(data);

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost(super.getBaseURI().getHost(), super.getBaseURI().getPort());
  
        
        HttpPost post = new HttpPost("/test-organization/test-app/games");
        post.setEntity(new StringEntity(json));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ access_token);
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        
        

        HttpResponse rsp = client.execute(host, post);

        printResponse(rsp);

        //should change content type was set
        assertEquals(200, rsp.getStatusLine().getStatusCode());

    }
    
    /**
     * Creates a simple entity of type game. Does not set the content type. The
     * type should be set to json to match the body
     * @throws  
     * @throws Exception 
     */
    @Test
    public void textHtmlContentType() throws Exception {
        Map<String, String> data = hashMap("name", "Solitaire");

        String json = JsonUtils.mapToFormattedJsonString(data);

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost(super.getBaseURI().getHost(), super.getBaseURI().getPort());
  
        
        HttpPost post = new HttpPost("/test-organization/test-app/games");
        post.setEntity(new StringEntity(json));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ access_token);
        post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML);
         
        

        HttpResponse rsp = client.execute(host, post);

        printResponse(rsp);

        //should be an error, no content type was set
        assertEquals(200, rsp.getStatusLine().getStatusCode());

    }
    

    /**
     * Creates a simple entity of type game. Does not set the content type or accept. The
     * type should be set to json to match the body
     * @throws  
     * @throws Exception 
     */
    @Test
    public void missingBoth() throws Exception {
        Map<String, String> data = hashMap("name", "Solitaire");

        String json = JsonUtils.mapToFormattedJsonString(data);

        DefaultHttpClient client = new DefaultHttpClient();

        HttpHost host = new HttpHost(super.getBaseURI().getHost(), super.getBaseURI().getPort());
  
        
        HttpPost post = new HttpPost("/test-organization/test-app/games");
        post.setEntity(new StringEntity(json));
        post.setHeader(HttpHeaders.AUTHORIZATION, "Bearer "+ access_token);
         
        

        HttpResponse rsp = client.execute(host, post);

        printResponse(rsp);

        assertEquals(200, rsp.getStatusLine().getStatusCode());

    }
    
    private void printResponse(HttpResponse rsp) throws ParseException, IOException{
        HttpEntity entity = rsp.getEntity();
        
        System.out.println("----------------------------------------");
        System.out.println(rsp.getStatusLine());
      
        Header[] headers = rsp.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            System.out.println(headers[i]);
        }
        System.out.println("----------------------------------------");

        if (entity != null) {
            System.out.println(EntityUtils.toString(entity));
        }
        
    }
}
