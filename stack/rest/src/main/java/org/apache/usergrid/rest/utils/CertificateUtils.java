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
package org.apache.usergrid.rest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by michaelarusso on 1/11/16.
 */
public class CertificateUtils {

    private static final Logger logger = LoggerFactory.getLogger(CertificateUtils.class);

    private static final String APPLE_TOPIC_OID = "1.2.840.113635.100.6.3.6";

    public static Map<String, Object> getCertAtrributes(InputStream stream, String certPassword){

        if(certPassword == null){
            certPassword = ""; // if there is no password, pass in empty string
        }

        Map<String,Object> attributes = new HashMap<>(1);
        try{
            KeyStore p12 = KeyStore.getInstance("pkcs12");
            p12.load(stream, certPassword.toCharArray());
            Enumeration aliases = p12.aliases();
            while(aliases.hasMoreElements()){
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) p12.getCertificate(alias);
                attributes.put("subject", cert.getSubjectDN().toString());
                attributes.put("validFrom", cert.getNotBefore());
                attributes.put("validFromTimestamp", cert.getNotBefore().getTime());
                attributes.put("validTo", cert.getNotAfter());
                attributes.put("validToTimestamp", cert.getNotAfter().getTime());
                attributes.put("issuer", cert.getIssuerDN().toString());
                attributes.put("subjectAlternativeNames", cert.getSubjectAlternativeNames());

                // Apple uses a specific extension OID for their universal cert structure and push topics
                Map<String,Object> extensions = new HashMap<>(1);
                if(cert.getExtensionValue(APPLE_TOPIC_OID) != null){
                    extensions.put(APPLE_TOPIC_OID, cert.getExtensionValue(APPLE_TOPIC_OID));
                }
                attributes.put("extensions", extensions);

                attributes.put("extendedKeyUsages", cert.getExtendedKeyUsage());
                attributes.put("version", cert.getVersion());
                attributes.put("signatureAlgorithm", cert.getSigAlgName());
                attributes.put("serialNumber", cert.getSerialNumber());
                attributes.put("basicConstraints", cert.getBasicConstraints());

            }
        }catch (Exception e){
            String message = "Unable load certificate details.  Possible invalid p12 file.";
            throw new RuntimeException(message, e);
        }

        return attributes;
    }


    public static boolean isValid(Map<String, Object> certAttributes) {

        if(certAttributes == null || certAttributes.isEmpty()){
            return false;
        }

        // check to make sure the certificate is not expired
            final long currentTime = System.currentTimeMillis();
            final long validToTimestamp = (long) certAttributes.get("validToTimestamp");

            // if the current time doesn't fall into the certs
            if(currentTime > validToTimestamp){
                logger.error("Certificate is not valid due to time.  Cert Valid To: {}", validToTimestamp);
                return false;
            }

        return true;
    }

}
