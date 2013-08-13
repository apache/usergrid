package org.usergrid.persistence.geo;

import static org.apache.commons.lang.math.NumberUtils.toDouble;
import static org.usergrid.utils.StringUtils.stringOrSubstringAfterLast;
import static org.usergrid.utils.StringUtils.stringOrSubstringBeforeFirst;

import java.util.UUID;

import me.prettyprint.hector.api.beans.DynamicComposite;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.geo.model.Point;
import org.usergrid.utils.UUIDUtils;


public class EntityLocationRef implements EntityRef {

  private UUID uuid;

  private String type;

  private UUID timestampUuid = UUIDUtils.newTimeUUID();

  private double latitude;

  private double longitude;

  private double distance;

  public EntityLocationRef() {
  }

  public EntityLocationRef(EntityRef entity, double latitude, double longitude) {
    this(entity.getType(), entity.getUuid(), latitude, longitude);
  }

  public EntityLocationRef(String type, UUID uuid, double latitude, double longitude) {
    this.type = type;
    this.uuid = uuid;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public EntityLocationRef(EntityRef entity, UUID timestampUuid, double latitude, double longitude) {
    this(entity.getType(), entity.getUuid(), timestampUuid, latitude, longitude);
  }

  public EntityLocationRef(String type, UUID uuid, UUID timestampUuid, double latitude, double longitude) {
    this.type = type;
    this.uuid = uuid;
    this.timestampUuid = timestampUuid;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public EntityLocationRef(EntityRef entity, UUID timestampUuid, String coord) {
    this.type = entity.getType();
    this.uuid = entity.getUuid();
    this.timestampUuid = timestampUuid;
    this.latitude = toDouble(stringOrSubstringBeforeFirst(coord, ','));
    this.longitude = toDouble(stringOrSubstringAfterLast(coord, ','));
  }

  @Override
  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public UUID getTimestampUuid() {
    return timestampUuid;
  }

  public void setTimestampUuid(UUID timestampUuid) {
    this.timestampUuid = timestampUuid;
  }

  public double getLatitude() {
    return latitude;
  }

  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  public Point getPoint() {
    return new Point(latitude, longitude);
  }

  public DynamicComposite getColumnName() {
    return new DynamicComposite(uuid, type, timestampUuid);
  }

  public DynamicComposite getColumnValue() {
    return new DynamicComposite(latitude, longitude);
  }

  public long getTimestampInMicros() {
    return UUIDUtils.getTimestampInMicros(timestampUuid);
  }

  public long getTimestampInMillis() {
    return UUIDUtils.getTimestampInMillis(timestampUuid);
  }

  public double getDistance() {
    return distance;
  }

  /**
   * Calculate, set and return the distance from this location to the point specified
   * @param point
   * @return
   */
  public double calcDistance(Point point) {
    distance = GeocellUtils.distance(getPoint(), point);
    return distance;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    EntityLocationRef other = (EntityLocationRef) obj;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    if (uuid == null) {
      if (other.uuid != null)
        return false;
    } else if (!uuid.equals(other.uuid))
      return false;
    return true;
  }

}
