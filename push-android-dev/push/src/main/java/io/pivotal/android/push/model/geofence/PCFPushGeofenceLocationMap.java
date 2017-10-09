package io.pivotal.android.push.model.geofence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PCFPushGeofenceLocationMap extends HashMap<String, PCFPushGeofenceLocation> {

    public interface Filter {
        boolean filterItem(PCFPushGeofenceData item, PCFPushGeofenceLocation location);
    }

    public static class LocationEntry {
        private final long geofenceId;
        private final long locationId;
        private final PCFPushGeofenceLocation location;

        public LocationEntry(long geofenceId, long locationId, PCFPushGeofenceLocation location) {
            this.geofenceId = geofenceId;
            this.locationId = locationId;
            this.location = location;
        }

        public long getGeofenceId() {
            return geofenceId;
        }

        public long getLocationId() {
            return locationId;
        }

        public PCFPushGeofenceLocation getLocation() {
            return location;
        }

        /**
         * Returns the geofence data object associated with this particular geofence location
         *
         * @param list A list of known PCFPushGeofenceData objects
         *
         * @return the PCFPusgGeofenceData object that matches the geofence ID in this location.
         */
        public PCFPushGeofenceData getGeofenceData(PCFPushGeofenceDataList list) {
            return list.get(geofenceId);
        }
    }

    public int addAll(PCFPushGeofenceDataList list) {
        if (list == null) {
            return 0;
        }

        int itemsAdded = 0;
        for (final PCFPushGeofenceData geofence : list) {
            if (geofence != null && geofence.getLocations() != null) {
                for (final PCFPushGeofenceLocation location : geofence.getLocations()) {
                    final String id = getAndroidRequestId(geofence.getId(), location.getId());
                    this.put(id, location);
                    itemsAdded += 1;
                }
            }
        }
        return itemsAdded;
    }

    public int addFiltered(PCFPushGeofenceDataList list, Filter filter) {

        if (list == null || filter == null) {
            return 0;
        }

        int itemsAdded = 0;
        for (final PCFPushGeofenceData geofence : list) {
            if (geofence.getLocations() != null) {
                for (final PCFPushGeofenceLocation location : geofence.getLocations()) {
                    if (filter.filterItem(geofence, location)) {
                        final String id = getAndroidRequestId(geofence.getId(), location.getId());
                        this.put(id, location);
                        itemsAdded += 1;
                    }
                }
            }
        }
        return itemsAdded;
    }

    public void putLocation(PCFPushGeofenceData geofence, int locationIndex) {
        final PCFPushGeofenceLocation location = geofence.getLocations().get(locationIndex);
        putLocation(geofence, location);
    }

    public void putLocation(PCFPushGeofenceData geofence, PCFPushGeofenceLocation location) {
        final String androidRequestId = PCFPushGeofenceLocationMap.getAndroidRequestId(geofence.getId(), location.getId());
        put(androidRequestId, location);
    }

    public Set<LocationEntry> locationEntrySet() {
        final Set<LocationEntry> locationEntries = new HashSet<>();
        for (final Entry<String, PCFPushGeofenceLocation> entry : entrySet()) {
            final long geofenceId = getGeofenceId(entry.getKey());
            final long locationId = getLocationId(entry.getKey());
            final LocationEntry locationEntry = new LocationEntry(geofenceId, locationId, entry.getValue());
            locationEntries.add(locationEntry);
        }
        return locationEntries;
    }

    public static long getGeofenceId(String key) {
//        TODO - there is a possibility of an ArrayIndexOutOfBoundsException here if the key is malformed or null - make this code less brittle, please.
        return Long.parseLong(key.split("_")[1]);
    }

    public static long getLocationId(String key) {
//        TODO - there is a possibility of an ArrayIndexOutOfBoundsException here if the key is malformed or null - make this code less brittle, please.
        return Long.parseLong(key.split("_")[2]);
    }

    public static String getAndroidRequestId(LocationEntry entry) {
        return getAndroidRequestId(entry.getGeofenceId(), entry.getLocationId());
    }

    public static String getAndroidRequestId(long geofenceId, long locationId) {
        return String.format("PCF_%d_%d", geofenceId, locationId);
    }
}
