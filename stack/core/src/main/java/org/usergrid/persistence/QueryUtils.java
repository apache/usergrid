package org.usergrid.persistence;

import org.usergrid.utils.ListUtils;

import java.util.List;
import java.util.Map;

/**
 * Utilities to deal with query extraction and generation
 *
 * @author zznate
 */
public class QueryUtils {

    public static final String PARAM_QL = "ql";
    public static final String PARAM_Q = "q";
    public static final String PARAM_QUERY = "query";

    public static String queryStrFrom(Map<String,List<String>> params) {
        if ( params.containsKey(PARAM_QL)) {
            return ListUtils.first(params.get(PARAM_QL));
        } else if ( params.containsKey(PARAM_Q)) {
            return ListUtils.first(params.get(PARAM_Q));
        } else if ( params.containsKey(PARAM_QUERY)) {
            return ListUtils.first(params.get(PARAM_QUERY));
        }
        return null;
    }
}
