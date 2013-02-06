package org.usergrid.services.assets.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.usergrid.persistence.entities.Asset;

/**
 * A binary store implementation using the local file system
 *
 * @author zznate
 */
public class LocalFileBinaryStore implements BinaryStore {

  private String reposLocation = FileUtils.getTempDirectoryPath();

  /**
   * Control where to store the file repository. In the system's temp dir
   * by default.
   * @param reposLocation
   */
  public void setReposLocation(String reposLocation) {
    this.reposLocation = reposLocation;
  }

  public String getReposLocation() {
    return reposLocation;
  }

  /**
   * Common method of contructing the file object based on the configured repos
   * and {@link org.usergrid.persistence.entities.Asset#getPath()}
   * @param asset
   * @return
   */
  private File path(UUID appId, Asset asset) {
    return new File(reposLocation, AssetUtils.buildAssetKey(appId, asset));
  }

  @Override
  public void write(UUID appId, Asset asset, InputStream inputStream) {

    File file = path(appId, asset);
    try {

      FileUtils.copyInputStreamToFile(inputStream, file);

      long size = FileUtils.sizeOf(file);

      asset.setProperty("content-length",size);

    } catch (IOException e) {
      e.printStackTrace();
    }
    // if we were successful, write the mime type
    if ( file.exists() ) {
      AssetMimeHandler.get().getMimeType(asset, file);
    }
  }

  @Override
  public InputStream read(UUID appId, Asset asset) {
    return read(appId, asset, 0, FileUtils.ONE_MB * 5);
  }

  @Override
  public InputStream read(UUID appId, Asset asset, long offset, long length) {
    try {
      return new BufferedInputStream(FileUtils.openInputStream(path(appId, asset)));
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    // TODO throw typed exception
    return null;
  }

  /**
   * Deletes the asset if it is a file. Does nothing if
   * {@link org.usergrid.persistence.entities.Asset#getPath()}
   * represents a directory.
   *
   * @param asset
   */
  @Override
  public void delete(UUID appId, Asset asset) {
    File file = path(appId, asset);
    if ( file.exists() && !file.isDirectory() ) {
      FileUtils.deleteQuietly(file);
    }
  }

}
