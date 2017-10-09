package io.pivotal.android.push.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;

import io.pivotal.android.push.model.geofence.PCFPushGeofenceData;
import io.pivotal.android.push.model.geofence.PCFPushGeofenceDataList;

public class GsonUtil {

    public static Gson getGson() {
        return getBuilder().create();
    }

    public static Gson getGsonAndSerializeNulls() {
        return getBuilder().serializeNulls().create();
    }

    private static GsonBuilder getBuilder() {

        final Type longSparseArrayType = new TypeToken<PCFPushGeofenceDataList>(){}.getType();

        return new GsonBuilder()
                .registerTypeAdapter(longSparseArrayType, new PCFPushGeofenceDataListTypeAdapter())
                .registerTypeAdapter(Date.class, dateDeserializer)
                .registerTypeAdapter(Date.class, dateSerializer);
    }

    private final static JsonDeserializer<Date> dateDeserializer = new JsonDeserializer<Date>() {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null) {
                return null;
            }
            return new Date(json.getAsLong());
        }
    };

    private final static JsonSerializer<Date> dateSerializer = new JsonSerializer<Date>() {

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) {
                return null;
            }
            return new JsonPrimitive(src.getTime());
        }
    };

    private static class PCFPushGeofenceDataListTypeAdapter extends TypeAdapter<PCFPushGeofenceDataList> {

        final Gson gson;

        public PCFPushGeofenceDataListTypeAdapter() {

            this.gson = new GsonBuilder()
                    .registerTypeAdapter(Date.class, dateSerializer)
                    .registerTypeAdapter(Date.class, dateDeserializer)
                    .create();
        }

        @Override
        public void write(JsonWriter out, PCFPushGeofenceDataList array) throws IOException {
            if (array == null) {
                out.nullValue();
                return;
            }

            final Type dataType = new TypeToken<PCFPushGeofenceData>(){}.getType();

            out.beginArray();

            for (int i = 0; i < array.size(); i += 1) {
                final long key = array.keyAt(i);
                final PCFPushGeofenceData item = array.get(key);
                gson.toJson(gson.toJsonTree(item, dataType), out);
            }

            out.endArray();
        }

        @Override
        public PCFPushGeofenceDataList read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (in.peek() != JsonToken.BEGIN_ARRAY) {
                throw new IOException("Parsing PCFPushGeofenceDataList. Expected JsonToken.BEGIN_ARRAY");
            }
            final PCFPushGeofenceDataList result = new PCFPushGeofenceDataList();
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                final PCFPushGeofenceData item = gson.fromJson(in, PCFPushGeofenceData.class);
                final long key = item.getId();
                result.put(key, item);
            }
            in.endArray();
            return result;
        }
    }
}
