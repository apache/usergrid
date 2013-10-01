package org.usergrid.services.assets.data;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.usergrid.persistence.Entity;
import org.usergrid.persistence.entities.Asset;

public interface BinaryStore {
  void write(UUID appId, Entity entity, InputStream inputStream) throws IOException;

  InputStream read(UUID appId, Entity entity) throws IOException;

  InputStream read(UUID appId, Entity entity, long offset, long length) throws IOException;

  void delete(UUID appId, Entity entity);
}
