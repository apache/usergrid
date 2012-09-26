package org.usergrid.services.assets.data;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Module;
import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jclouds.blobstore.AsyncBlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.options.GetOptions;
import org.jclouds.blobstore.options.PutOptions;
import org.jclouds.http.config.JavaUrlHttpCommandExecutorServiceModule;
import org.jclouds.logging.log4j.config.Log4JLoggingModule;
import org.jclouds.netty.config.NettyPayloadModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.Asset;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author zznate
 */
public class S3BinaryStore implements BinaryStore {

  final static Iterable<? extends Module> MODULES =
          ImmutableSet.of(new JavaUrlHttpCommandExecutorServiceModule(), new Log4JLoggingModule(), new NettyPayloadModule());

  private Logger logger = LoggerFactory.getLogger(S3BinaryStore.class);

  private final String secretKey;
  private final String accessId;
  private static String S3_PROVIDER = "s3";

  private Properties overrides;
  private final BlobStoreContext context;

  private static final long FIVE_MB = (FileUtils.ONE_MB * 5);

  private String bucketName = "usergrid-test";

  public S3BinaryStore(String accessId, String secretKey, String bucketName) {
    this.accessId = accessId;
    this.secretKey = secretKey;
    this.bucketName = bucketName;
    overrides = new Properties();
    //overrides.setProperty("jclouds.mpu.parallel.degree", threadcount);
    overrides.setProperty(S3_PROVIDER + ".identity", accessId);
    overrides.setProperty(S3_PROVIDER + ".credential", secretKey);

    context = new BlobStoreContextFactory().createContext("s3", MODULES, overrides);

    // Create Container (the bucket in s3)
    try {
      AsyncBlobStore blobStore = context.getAsyncBlobStore(); // it can be changed to sync
      // BlobStore (returns false if it already exists)
      ListenableFuture<Boolean> createContainer = blobStore.createContainerInLocation(null, bucketName);
      createContainer.get();

    } catch(Exception ex) {
      logger.error("Could not start binary service: {}", ex.getMessage());
      throw new RuntimeException(ex);
    }

  }

  public void destroy() {
    context.close();
  }

  @Override
  public void write(UUID appId, Asset asset, InputStream inputStream) {
    try {
      AsyncBlobStore blobStore = context.getAsyncBlobStore();
      // Add a Blob
      // objectname will be in the form of org/app/UUID
      // #payload can take an input stream
      // TODO toggle to largeAsset ~ > 5mb: IOUtils#copyLarge to a tmp file
      // TODO do this via ListenableFuture on dedicated thread pool

      //byte[] data = IOUtils.toByteArray(inputStream);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      long copied = IOUtils.copyLarge(inputStream, baos, 0, FIVE_MB);
      BlobBuilder.PayloadBlobBuilder bb = null;

      // If we are bigger than 5mb, dump to a tmp file and upload from there
      if ( copied == FIVE_MB ) {
        File f;
        f = File.createTempFile(asset.getUuid().toString(), "tmp");
        f.deleteOnExit();
        OutputStream os = null;
        try {
          os = new BufferedOutputStream(new FileOutputStream(f.getAbsolutePath()));

          copied = IOUtils.copyLarge(inputStream, os, 0, (FileUtils.ONE_GB * 5));

          bb = blobStore.blobBuilder(AssetUtils.buildAssetKey(appId, asset))
                  .payload(f)
                  .calculateMD5()
                  .contentType(AssetMimeHandler.get().getMimeType(asset, f));
        } catch (Exception ex) {
          ex.printStackTrace();
        } finally {
          IOUtils.closeQuietly(os);
          if ( f != null && f.exists() ) {
            f.delete();
          }
        }
      } else {
        byte[] data = baos.toByteArray();
        copied = data.length;
        bb = blobStore.blobBuilder(AssetUtils.buildAssetKey(appId, asset))
                .payload(data)
                .calculateMD5()
                .contentType(AssetMimeHandler.get().getMimeType(asset, data));
      }

      asset.setProperty(AssetUtils.CONTENT_LENGTH,copied);

      if ( asset.getProperty(AssetUtils.CONTENT_DISPOSITION) != null ) {
        bb.contentDisposition(asset.getProperty(AssetUtils.CONTENT_DISPOSITION).toString());
      }
      Blob blob = bb.build();

      String md5sum = Hex.encodeHexString(blob.getMetadata().getContentMetadata().getContentMD5());
      asset.setProperty(AssetUtils.CHECKSUM,md5sum);
      // containername?
      ListenableFuture<String> futureETag = blobStore.putBlob(bucketName, blob, PutOptions.Builder.multipart());
      // move update of properties into: futureETag.addListener();

      // asynchronously wait for the upload if we are not doing a large file
      if ( copied < FIVE_MB ) {
        String eTag = futureETag.get();
        asset.setProperty(AssetUtils.E_TAG,eTag);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }



  @Override
  public InputStream read(UUID appId, Asset asset, long offset, long length) {
    AsyncBlobStore blobStore = context.getAsyncBlobStore();
    ListenableFuture<Blob> blobFuture;
    try {
      if ( offset == 0 && length == FIVE_MB ) {
        // missing file will throw: org.jclouds.aws.AWSResponseException:
        blobFuture = blobStore.getBlob(bucketName,AssetUtils.buildAssetKey(appId, asset));
      } else {
        GetOptions options = GetOptions.Builder.range(offset, length);
        blobFuture = blobStore.getBlob(bucketName,AssetUtils.buildAssetKey(appId, asset), options);
      }
      return blobFuture.get().getPayload().getInput();
      //return blob.getPayload().
      //return null;
    } catch (Exception ex) {
      // TODO throw typed exception
      ex.printStackTrace();
    }
    return null;
  }

  @Override
  public InputStream read(UUID appId, Asset asset) {
    return read(appId, asset,0,FIVE_MB);
  }

  @Override
  public void delete(UUID appId, Asset asset) {

    AsyncBlobStore blobStore = context.getAsyncBlobStore();

    blobStore.removeBlob(bucketName, AssetUtils.buildAssetKey(appId, asset));

  }
}

