package org.apache.usergrid.java.client.utils;

import static java.net.URLEncoder.encode;
import static org.apache.usergrid.java.client.utils.ObjectUtils.isEmpty;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.usergrid.java.client.exception.ClientException;

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
