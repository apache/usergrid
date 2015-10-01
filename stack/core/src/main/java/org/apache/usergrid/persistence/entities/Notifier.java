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
package org.apache.usergrid.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.usergrid.persistence.TypedEntity;
import org.apache.usergrid.persistence.annotations.EntityProperty;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

/**
 * The entity class for representing Notifiers.
 */
@XmlRootElement
public class Notifier extends TypedEntity {

    public static final String ENTITY_TYPE = "notifier";

    public Notifier() {
    }

    public Notifier(UUID id) {
        uuid = id;
    }

    @EntityProperty(aliasProperty = true, unique = true, basic = true)
    protected String name;

    @EntityProperty(required = true)
    protected String provider;

    @EntityProperty
    protected String environment;

    // Apple APNs
    @EntityProperty(indexed = false, includedInExport = false, encrypted = true)
    protected byte[] p12Certificate;

    // Apple APNs
    @EntityProperty(indexed = false, includedInExport = false, encrypted = true)
    protected String certificatePassword;

    // Google GCM and Windows WNS
    @EntityProperty(indexed = false, includedInExport = false, encrypted = true)
    protected String apiKey;

    //Windows WNS sid
    @EntityProperty(indexed = false, includedInExport = false, encrypted = true)
    protected String sid;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {  this.sid = sid; }

    //Windows WNS logging
    @EntityProperty(indexed = false, includedInExport = false, encrypted = true)
    protected boolean logging = true;

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public boolean getLogging() {  return logging;  }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    @Override
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    @JsonIgnore
    public boolean isProduction() {
        return !"development".equals(environment);
    }

    @JsonIgnore
    public byte[] getP12Certificate() {
        return p12Certificate;
    }

    public void setP12Certificate(byte[] p12Certificate) {
        this.p12Certificate = p12Certificate;
    }

    @JsonIgnore
    public InputStream getP12CertificateStream() {
        byte[] cert = getP12Certificate();
        return cert != null ? new ByteArrayInputStream(cert) : null;
    }

    @JsonIgnore
    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    @JsonIgnore
    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }


}
