package org.usergrid.services.assets.data;

import java.io.*;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.usergrid.persistence.Entity;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import org.usergrid.persistence.EntityManager;
import org.usergrid.persistence.EntityManagerFactory;

public class S3BinaryStore implements BinaryStore {

  private static final Iterable<? extends Module> MODULES =
      ImmutableSet.of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(),
          new NettyPayloadModule());

  private static final Logger LOG = LoggerFactory.getLogger(S3BinaryStore.class);
  private static final long FIVE_MB = (FileUtils.ONE_MB * 5);

  private final BlobStoreContext context;
  private String bucketName;
  private ExecutorService executor = Executors.newFixedThreadPool(10);

  @Autowired
  private EntityManagerFactory emf;

  public S3BinaryStore(String accessId, String secretKey, String bucketName) {
    context = ContextBuilder.newBuilder("aws-s3")
        .credentials(accessId, secretKey)
        .modules(MODULES)
        .buildView(BlobStoreContext.class);

    // Create Container (the bucket in s3)
    this.bucketName = bucketName;
    BlobStore blobStore = context.getBlobStore();
    blobStore.createContainerInLocation(null, bucketName);
  }

  public void destroy() {
    context.close();
  }

  @Override
  public void write(final UUID appId, final Entity entity, InputStream inputStream) throws IOException {
    final BlobStore blobStore = context.getBlobStore();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    long copied = IOUtils.copyLarge(inputStream, baos, 0, FIVE_MB);
    BlobBuilder.PayloadBlobBuilder bb = null;

    if (copied == FIVE_MB) { // If bigger than 5mb, dump to a tmp file and upload from there

      File f = File.createTempFile(entity.getUuid().toString(), "tmp");
      f.deleteOnExit();
      OutputStream os = null;
      try {
        os = new BufferedOutputStream(new FileOutputStream(f.getAbsolutePath()));

        copied = IOUtils.copyLarge(inputStream, os, 0, (FileUtils.ONE_GB * 5));

        bb = blobStore.blobBuilder(AssetUtils.buildAssetKey(appId, entity))
            .payload(f)
            .calculateMD5()
            .contentType(AssetMimeHandler.get().getMimeType(entity, f));

      } finally {
        IOUtils.closeQuietly(os);
        if (f.exists()) { f.delete(); }
      }

    } else { // smaller than 5mb

      byte[] data = baos.toByteArray();
      copied = data.length;
      bb = blobStore.blobBuilder(AssetUtils.buildAssetKey(appId, entity))
          .payload(data)
          .calculateMD5()
          .contentType(AssetMimeHandler.get().getMimeType(entity, data));
    }

    final Map<String, Object> fileMetadata = AssetUtils.getFileMetadata(entity);
    fileMetadata.put(AssetUtils.CONTENT_LENGTH, copied);

    if (fileMetadata.get(AssetUtils.CONTENT_DISPOSITION) != null) {
      bb.contentDisposition(fileMetadata.get(AssetUtils.CONTENT_DISPOSITION).toString());
    }
    final Blob blob = bb.build();

    String md5sum = Hex.encodeHexString(blob.getMetadata().getContentMetadata().getContentMD5());
    fileMetadata.put(AssetUtils.CHECKSUM, md5sum);

    if (copied < FIVE_MB) { // synchronously upload for small files

      String eTag = blobStore.putBlob(bucketName, blob, PutOptions.Builder.multipart());
      fileMetadata.put(AssetUtils.E_TAG, eTag);

    } else { // asynchronously for large files

      executor.execute(new Runnable() {
        @Override
        public void run() {
          String eTag = blobStore.putBlob(bucketName, blob, PutOptions.Builder.multipart());
          fileMetadata.put(AssetUtils.E_TAG, eTag);
          EntityManager em = emf.getEntityManager(appId);
          try {
            em.update(entity);
          } catch (Exception e) {
            LOG.error("error updating entity", e);
          }
        }
      });
    }
  }

  @Override
  public InputStream read(UUID appId, Entity entity, long offset, long length) {
    BlobStore blobStore = context.getBlobStore();
    Blob blob;
    if (offset == 0 && length == FIVE_MB) {
      blob = blobStore.getBlob(bucketName, AssetUtils.buildAssetKey(appId, entity));
    } else {
      GetOptions options = GetOptions.Builder.range(offset, length);
      blob = blobStore.getBlob(bucketName, AssetUtils.buildAssetKey(appId, entity), options);
    }
    return blob.getPayload().getInput();
  }

  @Override
  public InputStream read(UUID appId, Entity entity) {
    return read(appId, entity, 0, FIVE_MB);
  }

  @Override
  public void delete(UUID appId, Entity entity) {
    BlobStore blobStore = context.getBlobStore();
    blobStore.removeBlob(bucketName, AssetUtils.buildAssetKey(appId, entity));
  }
}

