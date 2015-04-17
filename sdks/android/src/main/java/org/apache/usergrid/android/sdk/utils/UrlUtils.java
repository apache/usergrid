/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.usergrid.android.sdk.utils;

import static org.apache.usergrid.android.sdk.utils.ObjectUtils.isEmpty;
import static java.net.URLEncoder.encode;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.usergrid.android.sdk.exception.ClientException;

public class UrlUtils {

   
    public static URL url(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            throw new ClientException("Incorrect URL format", e);
        }
    }

    public static URL url(URL u, String s) {
        try {
            return new URL(u, s);
        } catch (MalformedURLException e) {
            throw new ClientException("Incorrect URL format", e);
        }
    }

    public static String path(Object... segments) {
        String path = "";
        boolean first = true;
        for (Object segment : segments) {
            if (segment instanceof Object[]) {
                segment = path((Object[]) segment);
            }
            if (!isEmpty(segment)) {
                if (first) {
                    path = segment.toString();
                    first = false;
                } else {
                    if (!path.endsWith("/")) {
                        path += "/";
                    }
                    path += segment.toString();
                }
            }
        }
        return path;
    }

    @SuppressWarnings("rawtypes")
    public static String encodeParams(Map<String, Object> params) {
        if (params == null) {
            return "";
        }
        boolean first = true;
        StringBuilder results = new StringBuilder();
        for (Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof List) {
                for (Object o : (List) entry.getValue()) {
                    if (!isEmpty(o)) {
                        if (!first) {
                            results.append('&');
                        }
                        first = false;
                        results.append(entry.getKey());
                        results.append("=");
                        try {
                            results.append(encode(o.toString(), "UTF-8"));
                        } catch (UnsupportedEncodingException e) {
                            throw new ClientException("Unknown encoding", e);
                        }
                    }
                }
            } else if (!isEmpty(entry.getValue())) {
                if (!first) {
                    results.append('&');
                }
                first = false;
                results.append(entry.getKey());
                results.append("=");
                try {
                    results.append(encode(entry.getValue().toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new ClientException("Unsupported string encoding", e);
                }
            }
        }
        return results.toString();
    }

    public static String addQueryParams(String url, Map<String, Object> params) {
        if (params == null) {
            return url;
        }
        if (!url.contains("?")) {
            url += "?";
        }
        url += encodeParams(params);
        return url;
    }

}
