package org.usergrid.persistence.cassandra;

import static me.prettyprint.hector.api.factory.HFactory.createColumn;
import static org.usergrid.persistence.Schema.DICTIONARY_LOCATIONS;
import static org.usergrid.persistence.cassandra.CassandraPersistenceUtils.key;
import static org.usergrid.utils.ClassUtils.cast;
import static org.usergrid.utils.ConversionUtils.bytebuffer;
import static org.usergrid.utils.UUIDUtils.getTimestampInMicros;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import me.prettyprint.cassandra.serializers.ByteBufferSerializer;
import me.prettyprint.cassandra.serializers.DoubleSerializer;
import me.prettyprint.cassandra.serializers.UUIDSerializer;
import me.prettyprint.hector.api.beans.DynamicComposite;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;

import org.usergrid.persistence.EntityRef;
import org.usergrid.persistence.Results;
import org.usergrid.persistence.Results.Level;
import org.usergrid.utils.UUIDUtils;

import com.beoui.geocell.GeocellManager;
import com.beoui.geocell.GeocellQueryEngine;
import com.beoui.geocell.annotations.Latitude;
import com.beoui.geocell.annotations.Longitude;
import com.beoui.geocell.model.GeocellQuery;
import com.beoui.geocell.model.Point;

public class GeoIndexManager {

	public static class LocationIndexEntry implements EntityRef {

		private UUID uuid;

		private String type;

		private UUID timestampUuid;

		@Latitude
		private double latitude;

		@Longitude
		private double longitude;

		public LocationIndexEntry() {
		}

		public LocationIndexEntry(UUID uuid, UUID timestampUuid,
				double latitude, double longitude) {
			this.uuid = uuid;
			this.timestampUuid = timestampUuid;
			this.latitude = latitude;
			this.longitude = longitude;
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

	}

	EntityManagerImpl em;
	CassandraService cass;

	public GeoIndexManager(EntityManagerImpl em) {
		this.em = em;
		cass = em.cass;
	}

	public static List<LocationIndexEntry> getLocationIndexEntries(
			List<HColumn<ByteBuffer, ByteBuffer>> columns) {
		List<LocationIndexEntry> entries = new ArrayList<LocationIndexEntry>();
		if (columns != null) {
			LocationIndexEntry prevEntry = null;
			for (HColumn<ByteBuffer, ByteBuffer> column : columns) {
				DynamicComposite composite = DynamicComposite
						.fromByteBuffer(column.getName());
				UUID uuid = composite.get(0, UUIDSerializer.get());
				UUID timestampUuid = composite.get(1, UUIDSerializer.get());
				composite = DynamicComposite.fromByteBuffer(column.getValue());
				Double latitude = composite.get(0, DoubleSerializer.get());
				Double longitude = composite.get(1, DoubleSerializer.get());
				if ((prevEntry != null) && uuid.equals(prevEntry.getUuid())) {
					prevEntry.setLatitude(latitude);
					prevEntry.setLongitude(longitude);
				} else {
					prevEntry = new LocationIndexEntry(uuid, timestampUuid,
							latitude, longitude);
					entries.add(prevEntry);
				}
			}
		}
		return entries;
	}

	public static Mutator<ByteBuffer> addLocationEntryToMutator(
			Mutator<ByteBuffer> m, Object key, LocationIndexEntry entry,
			UUID timestampUuid) {

		HColumn<ByteBuffer, ByteBuffer> column = createColumn(
				DynamicComposite.toByteBuffer(entry.getUuid(), timestampUuid),
				DynamicComposite.toByteBuffer(entry.getLatitude(),
						entry.getLongitude()),
				getTimestampInMicros(timestampUuid),
				ByteBufferSerializer.get(), ByteBufferSerializer.get());
		m.addInsertion(bytebuffer(key),
				ApplicationCF.ENTITY_PROPERTIES.toString(), column);

		return m;
	}

	public static List<LocationIndexEntry> mergeLocationEntries(
			List<LocationIndexEntry> list, List<LocationIndexEntry>... lists) {
		if ((lists == null) || (lists.length == 0)) {
			return list;
		}
		LinkedHashMap<UUID, LocationIndexEntry> merge = new LinkedHashMap<UUID, LocationIndexEntry>();
		for (LocationIndexEntry loc : list) {
			merge.put(loc.getUuid(), loc);
		}
		for (List<LocationIndexEntry> l : lists) {
			for (LocationIndexEntry loc : l) {
				if (merge.containsKey(loc.getUuid())) {
					if (UUIDUtils.compare(loc.getTimestampUuid(),
							merge.get(loc.getUuid()).getTimestampUuid()) > 0) {
						merge.put(loc.getUuid(), loc);
					}
				}
			}
		}
		return new ArrayList<LocationIndexEntry>(merge.values());
	}

	@SuppressWarnings("unchecked")
	public List<LocationIndexEntry> query(Object key,
			List<String> curGeocellsUnique, UUID startResult, int count,
			boolean reversed) {
		List<LocationIndexEntry> list = new ArrayList<LocationIndexEntry>();

		for (String geoCell : curGeocellsUnique) {
			List<HColumn<ByteBuffer, ByteBuffer>> columns;
			try {
				columns = cass.getColumns(
						cass.getApplicationKeyspace(em.applicationId),
						ApplicationCF.ENTITY_INDEX, key(key, geoCell),
						startResult, null, count, reversed);
				List<LocationIndexEntry> entries = getLocationIndexEntries(columns);
				list = mergeLocationEntries(list, entries);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return list;
	}

	public Results proximitySearchCollection(final EntityRef headEntity,
			final String collectionName, final String propertyName,
			Point center, int maxResults, double maxDistance,
			final UUID startResult, final int count, final boolean reversed,
			Level level) throws Exception {

		GeocellQueryEngine gqe = new GeocellQueryEngine() {
			@SuppressWarnings("unchecked")
			@Override
			public <T> List<T> query(GeocellQuery baseQuery,
					List<String> curGeocellsUnique, Class<T> entityClass) {
				return (List<T>) GeoIndexManager.this.query(
						key(headEntity.getUuid(), DICTIONARY_LOCATIONS,
								collectionName, propertyName),
						curGeocellsUnique, startResult, count, reversed);
			}
		};

		List<LocationIndexEntry> locations = null;

		GeocellQuery baseQuery = new GeocellQuery();
		try {
			locations = GeocellManager.proximitySearch(center, maxResults,
					maxDistance, LocationIndexEntry.class, baseQuery, gqe,
					GeocellManager.MAX_GEOCELL_RESOLUTION);
		} catch (Exception e) {
			e.printStackTrace();
		}

		@SuppressWarnings("unchecked")
		Results results = Results
				.fromRefList((List<EntityRef>) cast(locations));
		results = em.loadEntities(results, level, count);
		return results;
	}

}
