package org.usergrid.services.assets.data;

import eu.medsea.mimeutil.MimeException;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usergrid.persistence.entities.Asset;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.Collection;

/**
 * Detect the mime type of an Asset
 * @author zznate
 */
public final class AssetMimeHandler {
  private static final Logger logger = LoggerFactory.getLogger(AssetMimeHandler.class);

  private final MimeUtil2 mimeUtil;

  AssetMimeHandler() {
    mimeUtil = new MimeUtil2();
    mimeUtil.registerMimeDetector("eu.medsea.mimeutil.detector.MagicMimeMimeDetector");
  }

  private static AssetMimeHandler INSTANCE;

  public static AssetMimeHandler get() {
    if ( INSTANCE == null ) {
      INSTANCE = new AssetMimeHandler();
    }
    return INSTANCE;
  }

  /**
   * Get the Mime type of an Asset based on it's type. If the Asset already has
   * the "content-type" property set, we return that. Otherwise the mimeutil library
   * is used to do file type detection.
   *
   * @param asset
   * @param type
   * @param <T>
   * @return A string representation of the content type suitable for use in an
   * HTTP header. Eg. "image/jpeg" for a jpeg image.
   */
  public <T> String getMimeType(Asset asset, T type) {
    String contentType = MediaType.APPLICATION_OCTET_STREAM;
    if ( asset.getProperty(AssetUtils.CONTENT_TYPE) != null ) {
      contentType = asset.getProperty(AssetUtils.CONTENT_TYPE).toString();
    } else {
      Collection col;
      if ( type instanceof byte[] ) {
        col = mimeUtil.getMimeTypes((byte[])type);
      } else if ( type instanceof File) {
        col = mimeUtil.getMimeTypes((File)type);
      } else {
        return contentType;
      }
      if ( !col.isEmpty() ) {
        try {
          MimeType mime = ((MimeType)col.iterator().next());
          contentType = mime.toString();
          asset.setProperty(AssetUtils.CONTENT_TYPE,contentType);
        } catch(MimeException me) {
          logger.error("could not sniff mime type for asset {}", asset.getUuid());
        }
      }
    }
    return contentType;
  }
}
