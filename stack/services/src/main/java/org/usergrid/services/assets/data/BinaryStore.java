package org.usergrid.services.assets.data;

import java.io.InputStream;
import java.util.UUID;

import org.usergrid.persistence.entities.Asset;

/**
 * @author zznate
 */
public interface BinaryStore {
  void write(UUID appId, Asset asset, InputStream inputStream);

  InputStream read(UUID appId, Asset asset);

  InputStream read(UUID appId, Asset asset, long offset, long length);

  void delete(UUID appId, Asset asset);
}
