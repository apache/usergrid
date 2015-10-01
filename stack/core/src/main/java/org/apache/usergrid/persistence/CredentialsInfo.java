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
package org.apache.usergrid.persistence;


import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class CredentialsInfo implements Comparable<CredentialsInfo>,Serializable {

    boolean recoverable;
    boolean encrypted;
    String cipher;
    String key;
    String secret;
    String hashType;
    Long created;

    /**
     * A list of crypto algorithms to apply to unecrypted input for comparison. Note that cipher and hashtype should be
     * deprecated
     */
    private String[] cryptoChain;

    protected Map<String, Object> properties = new TreeMap<String, Object>( String.CASE_INSENSITIVE_ORDER );


    public CredentialsInfo() {
        created = System.currentTimeMillis();
    }


    public boolean getRecoverable() {
        return recoverable;
    }


    public void setRecoverable( boolean recoverable ) {
        this.recoverable = recoverable;
    }


    public boolean getEncrypted() {
        return encrypted;
    }


    public void setEncrypted( boolean encrypted ) {
        this.encrypted = encrypted;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getCipher() {
        return cipher;
    }


    public void setCipher( String cipher ) {
        this.cipher = cipher;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getKey() {
        return key;
    }


    public void setKey( String key ) {
        this.key = key;
    }


    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getSecret() {
        return secret;
    }


    public void setSecret( String secret ) {
        this.secret = secret;
    }


    public static String getCredentialsSecret( CredentialsInfo credentials ) {
        if ( credentials == null ) {
            return null;
        }
        return credentials.getSecret();
    }


    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }


    @JsonAnySetter
    public void setProperty( String key, Object value ) {
        properties.put( key, value );
    }


    public Object getProperty( String key ) {
        return properties.get( key );
    }


    /** @return the hashType */
    public String getHashType() {
        return hashType;
    }


    /**
     * Used for handling legacy passwords encrypted in md5 or similar.
     *
     * @param hashType the hashType to set
     */
    public void setHashType( String hashType ) {
        this.hashType = hashType;
    }


    /** @return the cryptoChain */
    public String[] getCryptoChain() {
        return cryptoChain;
    }


    /** @param cryptoChain the cryptoChain to set */
    public void setCryptoChain( String[] cryptoChain ) {
        this.cryptoChain = cryptoChain;
    }


    public Long getCreated() {
        return created;
    }


    @Override
    public int compareTo( CredentialsInfo o ) {
        if (created.equals(o.created)) {
            return 0;
        }
        if ( o.created == null ) {
            return 1;
        }
        return o.created.compareTo( created );
    }
}
