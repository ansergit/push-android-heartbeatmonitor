package io.pivotal.android.push.model.analytics;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import io.pivotal.android.push.database.Database;

public class AnalyticsEvent implements Parcelable {

    public static class Columns {
        public static final String RECEIPT_ID = "receiptId";
        public static final String EVENT_TYPE = "eventType";
        public static final String EVENT_TIME = "eventTime";
        public static final String DEVICE_UUID = "deviceUuid";
        public static final String GEOFENCE_ID = "geofenceId";
        public static final String LOCATION_ID = "locationId";
        public static final String STATUS = "status";
        public static final String SDK_VERSION = "sdkVersion";
        public static final String PLATFORM_TYPE = "platformType";
        public static final String PLATFORM_UUID = "platformUuid";
    }

    public static class Status {
        public static final int NOT_POSTED = 0;
        public static final int POSTING = 1;
        public static final int POSTED = 2;
        public static final int POSTING_ERROR = 3;
    }

    public static String statusString(int status) {
        switch (status) {
            case Status.NOT_POSTED:
                return "Not posted";
            case Status.POSTING:
                return "Posting";
            case Status.POSTED:
                return "Posted";
            case Status.POSTING_ERROR:
                return "Error";
        }
        return "?";
    }

    private transient int status;
    private transient int id;

    @SerializedName(Columns.RECEIPT_ID)
    private String receiptId;

    @SerializedName(Columns.EVENT_TYPE)
    private String eventType;

    @SerializedName(Columns.EVENT_TIME)
    private String eventTime;

    @SerializedName(Columns.DEVICE_UUID)
    private String deviceUuid;

    @SerializedName(Columns.GEOFENCE_ID)
    private String geofenceId;

    @SerializedName(Columns.LOCATION_ID)
    private String locationId;

    @SerializedName(Columns.SDK_VERSION)
    private String sdkVersion;

    @SerializedName(Columns.PLATFORM_TYPE)
    private String platformType;

    @SerializedName(Columns.PLATFORM_UUID)
    private String platformUuid;

    public AnalyticsEvent() {
    }

    // Construct from cursor
    public AnalyticsEvent(Cursor cursor) {
        int columnIndex;

        columnIndex = cursor.getColumnIndex(BaseColumns._ID);
        if (columnIndex >= 0) {
            setId(cursor.getInt(columnIndex));
        } else {
            setId(0);
        }

        columnIndex = cursor.getColumnIndex(Columns.STATUS);
        if (columnIndex >= 0) {
            setStatus(cursor.getInt(columnIndex));
        } else {
            setStatus(Status.NOT_POSTED);
        }

        columnIndex = cursor.getColumnIndex(Columns.RECEIPT_ID);
        if (columnIndex >= 0) {
            setReceiptId(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.EVENT_TYPE);
        if (columnIndex >= 0) {
            setEventType(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.EVENT_TIME);
        if (columnIndex >= 0) {
            setEventTime(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.DEVICE_UUID);
        if (columnIndex >= 0) {
            setDeviceUuid(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.GEOFENCE_ID);
        if (columnIndex >= 0) {
            setGeofenceId(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.LOCATION_ID);
        if (columnIndex >= 0) {
            setLocationId(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.SDK_VERSION);
        if (columnIndex >= 0) {
            setSdkVersion(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.PLATFORM_TYPE);
        if (columnIndex >= 0) {
            setPlatformType(cursor.getString(columnIndex));
        }

        columnIndex = cursor.getColumnIndex(Columns.PLATFORM_UUID);
        if (columnIndex >= 0) {
            setPlatformUuid(cursor.getString(columnIndex));
        }
    }

    // Copy constructor
    public AnalyticsEvent(AnalyticsEvent source) {
        this.status = source.status;
        this.id = source.id;
        this.receiptId = source.receiptId;
        this.eventType = source.eventType;
        this.eventTime = source.eventTime;
        this.deviceUuid = source.deviceUuid;
        this.geofenceId = source.geofenceId;
        this.locationId = source.locationId;
        this.sdkVersion = source.sdkVersion;
        this.platformType = source.platformType;
        this.platformUuid = source.platformUuid;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        if (status != Status.NOT_POSTED && status != Status.POSTING && status != Status.POSTED && status != Status.POSTING_ERROR) {
            throw new IllegalArgumentException("Illegal event status: " + status);
        }
        this.status = status;
    }

    public String getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(String receiptId) {
        this.receiptId = receiptId;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getGeofenceId() {
        return geofenceId;
    }

    public void setGeofenceId(String geofenceId) {
        this.geofenceId = geofenceId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String time) {
        this.eventTime = time;
    }

    public void setEventTime(Date time) {
        if (time != null) {
            this.eventTime = String.format("%d", time.getTime() / 1000L);
        } else {
            this.eventTime = null;
        }
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnalyticsEvent that = (AnalyticsEvent) o;

        if (status != that.status) return false;
        if (receiptId != null ? !receiptId.equals(that.receiptId) : that.receiptId != null)
            return false;
        if (eventType != null ? !eventType.equals(that.eventType) : that.eventType != null)
            return false;
        if (eventTime != null ? !eventTime.equals(that.eventTime) : that.eventTime != null)
            return false;
        if (deviceUuid != null ? !deviceUuid.equals(that.deviceUuid) : that.deviceUuid != null)
            return false;
        if (geofenceId != null ? !geofenceId.equals(that.geofenceId) : that.geofenceId != null)
            return false;
        if (locationId != null ? !locationId.equals(that.locationId) : that.locationId != null)
            return false;
        if (sdkVersion != null ? !sdkVersion.equals(that.sdkVersion) : that.sdkVersion != null)
            return false;
        if (platformType != null ? !platformType.equals(that.platformType) : that.platformType != null)
            return false;
        return !(platformUuid != null ? !platformUuid.equals(that.platformUuid) : that.platformUuid != null);

    }

    @Override
    public int hashCode() {
        int result = status;
        result = 31 * result + (receiptId != null ? receiptId.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventTime != null ? eventTime.hashCode() : 0);
        result = 31 * result + (deviceUuid != null ? deviceUuid.hashCode() : 0);
        result = 31 * result + (geofenceId != null ? geofenceId.hashCode() : 0);
        result = 31 * result + (locationId != null ? locationId.hashCode() : 0);
        result = 31 * result + (sdkVersion != null ? sdkVersion.hashCode() : 0);
        result = 31 * result + (platformType != null ? platformType.hashCode() : 0);
        result = 31 * result + (platformUuid != null ? platformUuid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "AnalyticsEvent{" +
                "receiptId='" + receiptId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", deviceUuid='" + deviceUuid + '\'' +
                ", geofenceId='" + geofenceId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", sdkVersion='" + sdkVersion + '\'' +
                ", platformType='" + platformType + '\'' +
                ", platformUuid='" + platformUuid + '\'' +
                '}';
    }

// JSON helpers

    public static List<AnalyticsEvent> jsonStringToList(String str) {
        final Gson gson = new Gson();
        final Type type = getTypeToken();
        return gson.fromJson(str, type);
    }

    public static String listToJsonString(List<AnalyticsEvent> list) {
        if (list == null) {
            return null;
        } else {
            final Gson gson = new Gson();
            final Type type = getTypeToken();
            return gson.toJson(list, type);
        }
    }

    private static Type getTypeToken() {
        return new TypeToken<List<AnalyticsEvent>>() {}.getType();
    }

    // Database helpers

    public ContentValues getContentValues(int databaseVersion) {
        // NOTE - do not save the 'id' field to the ContentValues. Let the database
        // figure out the 'id' itself.
        final ContentValues cv = new ContentValues();
        cv.put(Columns.RECEIPT_ID, getReceiptId());
        cv.put(Columns.EVENT_TIME, getEventTime());
        cv.put(Columns.EVENT_TYPE, getEventType());
        cv.put(Columns.DEVICE_UUID, getDeviceUuid());
        cv.put(Columns.GEOFENCE_ID, getGeofenceId());
        cv.put(Columns.LOCATION_ID, getLocationId());
        cv.put(Columns.STATUS, getStatus());

        if (databaseVersion >= 2) {
            cv.put(Columns.SDK_VERSION, getSdkVersion());
        }

        if (databaseVersion >= 3) {
            cv.put(Columns.PLATFORM_TYPE, getPlatformType());
            cv.put(Columns.PLATFORM_UUID, getPlatformUuid());
        }

        return cv;
    }

    public static String getCreateTableSqlStatement(int databaseVersion) {
        final StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE IF NOT EXISTS ");
        sb.append('\'');
        sb.append(Database.EVENTS_TABLE_NAME);
        sb.append("\' ('");
        sb.append(BaseColumns._ID);
        sb.append("' INTEGER PRIMARY KEY AUTOINCREMENT, '");
        sb.append(Columns.RECEIPT_ID);
        sb.append("' TEXT, '");
        sb.append(Columns.EVENT_TYPE);
        sb.append("' TEXT, '");
        sb.append(Columns.EVENT_TIME);
        sb.append("' TEXT, '");
        sb.append(Columns.DEVICE_UUID);
        sb.append("' TEXT, '");
        sb.append(Columns.GEOFENCE_ID);
        sb.append("' TEXT, '");
        sb.append(Columns.LOCATION_ID);
        sb.append("' TEXT, '");
        if (databaseVersion >= 2) {
            sb.append(Columns.SDK_VERSION);
            sb.append("' TEXT, '");
        }
        if (databaseVersion >= 3) {
            sb.append(Columns.PLATFORM_TYPE);
            sb.append("' TEXT, '");
            sb.append(Columns.PLATFORM_UUID);
            sb.append("' TEXT, '");
        }
        sb.append(Columns.STATUS);
        sb.append("' INT);");
        return sb.toString();
    }

    public static String getDropTableSqlStatement() {
        return "DROP TABLE IF EXISTS '" + Database.EVENTS_TABLE_NAME + "';";
    }

    public static List<String> getDatabaseMigrationCommands(int oldVersion, int newVersion) {

        final List<String> upgradeStatements = new LinkedList<>();

        if (oldVersion > newVersion) {
            return null;

        } else if (oldVersion == newVersion) {
            return upgradeStatements;

        } else if (oldVersion == 1 && newVersion == 2) {

            upgradeStatements.addAll(AnalyticsEvent.getMigrateVersion1ToVersion2Statement());

        } else if (oldVersion == 2 && newVersion == 3) {

            upgradeStatements.addAll(AnalyticsEvent.getMigrateVersion2ToVersion3Statement());

        } else if (oldVersion == 1 && newVersion == 3) {

            upgradeStatements.addAll(AnalyticsEvent.getMigrateVersion1ToVersion2Statement());
            upgradeStatements.addAll(AnalyticsEvent.getMigrateVersion2ToVersion3Statement());
        }

        return upgradeStatements;
    }

    private static List<String> getMigrateVersion1ToVersion2Statement() {
        return Arrays.asList("ALTER TABLE '" + Database.EVENTS_TABLE_NAME + "' " + "ADD COLUMN '" + Columns.SDK_VERSION + "' TEXT;");
    }

    private static List<String> getMigrateVersion2ToVersion3Statement() {
        return Arrays.asList("ALTER TABLE '" + Database.EVENTS_TABLE_NAME + "' " + "ADD COLUMN '" + Columns.PLATFORM_TYPE + "' TEXT;",
                "ALTER TABLE '" + Database.EVENTS_TABLE_NAME + "' " + "ADD COLUMN '" + Columns.PLATFORM_UUID + "' TEXT;");
    }

    public static int getRowIdFromCursor(final Cursor cursor) {
        final int idColumn = cursor.getColumnIndex(BaseColumns._ID);
        if (idColumn < 0) {
            throw new IllegalArgumentException("No " + BaseColumns._ID + " in cursor");
        }
        final int id = cursor.getInt(idColumn);
        return id;
    }

    // Parcelable stuff

    public static final Parcelable.Creator<AnalyticsEvent> CREATOR = new Parcelable.Creator<AnalyticsEvent>() {

        public AnalyticsEvent createFromParcel(Parcel in) {
            return new AnalyticsEvent(in);
        }

        public AnalyticsEvent[] newArray(int size) {
            return new AnalyticsEvent[size];
        }
    };

    private AnalyticsEvent(Parcel in) {
        id = in.readInt();
        status = in.readInt();
        eventType = in.readString();
        eventTime = in.readString();
        deviceUuid = in.readString();
        receiptId = in.readString();
        geofenceId = in.readString();
        locationId = in.readString();
        sdkVersion = in.readString();
        platformType = in.readString();
        platformUuid = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeInt(status);
        out.writeString(eventType);
        out.writeString(eventTime);
        out.writeString(deviceUuid);
        out.writeString(receiptId);
        out.writeString(geofenceId);
        out.writeString(locationId);
        out.writeString(sdkVersion);
        out.writeString(platformType);
        out.writeString(platformUuid);
    }
}
