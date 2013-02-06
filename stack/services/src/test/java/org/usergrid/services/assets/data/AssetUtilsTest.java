package org.usergrid.services.assets.data;

import static junit.framework.Assert.assertEquals;

import java.util.UUID;

import org.junit.Test;
import org.usergrid.persistence.entities.Asset;

/**
 * @author zznate
 */
public class AssetUtilsTest {

  private static UUID appId = new UUID(0,1);

  @Test
  public void buildPathOk() {
    Asset asset = new Asset();
    asset.setPath("path/to/file");
    asset.setUuid(UUID.randomUUID());

    String path = AssetUtils.buildAssetKey(appId, asset);

    assertEquals(73, path.length());
    assertEquals(appId.toString(), path.substring(0,36));
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyErrorsOkAssetId() {
    Asset asset = new Asset();
    AssetUtils.buildAssetKey(appId, asset);
  }

  @Test(expected = IllegalArgumentException.class)
  public void verifyErrorsOkNullAppId() {
    Asset asset = new Asset();
    asset.setUuid(UUID.randomUUID());
    AssetUtils.buildAssetKey(null, asset);
  }
}
