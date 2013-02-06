package org.usergrid.services.assets.data;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.Asset;
import org.usergrid.utils.StringUtils;

import com.google.common.base.Preconditions;

/**
 * @author zznate
 */
public class AssetUtils {
  private static Logger logger = LoggerFactory.getLogger(AssetUtils.class);

  public static final String CONTENT_TYPE = "content-type";
  public static final String CONTENT_LENGTH = "content-length";
  public static final String CONTENT_DISPOSITION = "content-disposition";
  public static final String E_TAG = "etag";
  public static final String CHECKSUM = "checksum";

  /**
   * Returns the key for the bucket in the following form:
   * [appId]/[{@link org.usergrid.persistence.entities.Asset#getPath()}
   * @param appId
   * @param asset
   * @return
   */
  public static String buildAssetKey(UUID appId, Asset asset) {
    Preconditions.checkArgument(asset.getUuid() != null, "The asset provided to buildAssetKey had a null UUID");
    Preconditions.checkArgument(appId !=null, "The appId provided to buildAssetKey was null");
    return appId.toString().concat("/").concat(asset.getUuid().toString());
  }

  /**
   * Attempt to parse the Date from a Date-based header, primarily If-Modified-Since
   * @param headerValue
   * @return
   */
  public static Date fromIfModifiedSince(String headerValue) {
    Date moded = null;
    if (!StringUtils.isEmpty(headerValue )) {
      try {
        moded = DateUtils.parseDate(headerValue, DEFAULT_PATTERNS);
      } catch (ParseException pe) {
        logger.error("Could not parse date format from If-Modified-Since header: " + headerValue);
      }
    }
    return moded;
  }

  /***
   * The following yanked from org.apache.http.impl.cookie.DateUtils, Apache 2.0 License
   ***/

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1123 format.
   */
  public static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in RFC 1036 format.
   */
  public static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";

  /**
   * Date format pattern used to parse HTTP date headers in ANSI C
   * <code>asctime()</code> format.
   */
  public static final String PATTERN_ASCTIME = "EEE MMM d HH:mm:ss yyyy";

  public static final String[] DEFAULT_PATTERNS = new String[] {
      PATTERN_RFC1036,
      PATTERN_RFC1123,
      PATTERN_ASCTIME
  };
}
