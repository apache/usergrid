package org.usergrid.services.assets.data;

import org.usergrid.persistence.entities.Asset;

import java.io.InputStream;

/**
 * @author zznate
 */
public interface BinaryStore {
  void write(Asset asset, InputStream inputStream);

  InputStream read(Asset asset);

  InputStream read(Asset asset, long offset, long length);

  void delete(Asset asset);
}
