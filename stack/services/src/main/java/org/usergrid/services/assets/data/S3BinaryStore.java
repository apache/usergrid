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
  private MimeUtil2 mimeUtil;

  private static final long FIVE_MB = (FileUtils.ONE_MB * 5);


  public S3BinaryStore(String accessId, String secretKey) {
    this.accessId = accessId;
    this.secretKey = secretKey;
    overrides = new Properties();
    //overrides.setProperty("jclouds.mpu.parallel.degree", threadcount);
    overrides.setProperty(S3_PROVIDER + ".identity", accessId);
    overrides.setProperty(S3_PROVIDER + ".credential", secretKey);

    context = new BlobStoreContextFactory().createContext("s3", MODULES, overrides);

    mimeUtil = new MimeUtil2();
    mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
  }

  public void destroy() {
    context.close();
  }

  @Override
  public void write(Asset asset, InputStream inputStream) {
    // write(UUID applicationId, Asset asset, InputStream is)
    try {
      // Create Container (the bucket in s3)
      AsyncBlobStore blobStore = context.getAsyncBlobStore(); // it can be changed to sync
      // BlobStore (retruns false if it already exists)
      blobStore.createContainerInLocation(null, "usergrid-test");
      // bad name will throw ContainerNotFoundException

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

          bb = blobStore.blobBuilder(asset.getUuid().toString())
                  .payload(f)
                  .calculateMD5()
                  .contentType(getMimeType(asset, f));
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
        bb = blobStore.blobBuilder(asset.getUuid().toString())
                .payload(data)
                .calculateMD5()
                .contentType(getMimeType(asset, data));
      }

      asset.setProperty("content-length",copied);

      if ( asset.getProperty("content-disposition") != null ) {
        bb.contentDisposition(asset.getProperty("content-disposition").toString());
      }
      Blob blob = bb.build();

      String md5sum = Hex.encodeHexString(blob.getMetadata().getContentMetadata().getContentMD5());
      asset.setProperty("checksum",md5sum);
      // containername?
      ListenableFuture<String> futureETag = blobStore.putBlob("usergrid-test", blob, PutOptions.Builder.multipart());
      // move update of properties into: futureETag.addListener();

      // asynchronously wait for the upload if we are not doing a large file
      if ( copied < FIVE_MB ) {
        String eTag = futureETag.get();
        asset.setProperty("etag",eTag);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private <T> String getMimeType(Asset asset, T t) {
    String contentType = MediaType.APPLICATION_OCTET_STREAM;
    if ( asset.getProperty("content-type") != null ) {
      contentType = asset.getProperty("content-type").toString();
    } else {
      Collection col;
      if ( t instanceof byte[] ) {
        col = mimeUtil.getMimeTypes((byte[])t);
      } else if ( t instanceof File ) {
        col = mimeUtil.getMimeTypes((File)t);
      } else {
        return contentType;
      }
      if ( !col.isEmpty() ) {
        try {
          MimeType mime = ((MimeType)col.iterator().next());
          contentType = mime.toString();
          asset.setProperty("content-type",contentType);
        } catch(MimeException me) {
          logger.error("could not sniff mime type for asset {}", asset.getUuid());
        }
      }
    }
    return contentType;
  }

  @Override
  public InputStream read(Asset asset, long offset, long length) {
    AsyncBlobStore blobStore = context.getAsyncBlobStore();
    ListenableFuture<Blob> blobFuture;
    if ( offset == 0 && length == FIVE_MB ) {
      blobFuture = blobStore.getBlob("usergrid-test",asset.getUuid().toString());
    } else {
      GetOptions options = GetOptions.Builder.range(offset, length);
      blobFuture = blobStore.getBlob("usergrid-test",asset.getUuid().toString(), options);
    }
    try {
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
  public InputStream read(Asset asset) {
    return read(asset,0,FIVE_MB);
  }

  @Override
  public void delete(Asset asset) {

    AsyncBlobStore blobStore = context.getAsyncBlobStore();

    blobStore.removeBlob("usergrid-test",asset.getUuid().toString());

  }
}

