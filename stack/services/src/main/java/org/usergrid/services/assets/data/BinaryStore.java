package org.usergrid.services.assets.data;

import org.usergrid.persistence.entities.Asset;

import java.io.InputStream;
import java.util.UUID;

/**
 * @author zznate
 */
public interface BinaryStore {
  void write(UUID appId, Asset asset, InputStream inputStream);

  InputStream read(UUID appId, Asset asset);

  InputStream read(UUID appId, Asset asset, long offset, long length);

  void delete(UUID appId, Asset asset);
}
