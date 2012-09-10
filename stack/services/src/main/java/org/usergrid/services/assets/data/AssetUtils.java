package org.usergrid.services.assets.data;

import com.google.common.base.Preconditions;
import org.usergrid.persistence.entities.Asset;

import java.util.UUID;

/**
 * @author zznate
 */
public class AssetUtils {

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
}
