package io.pivotal.android.push.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.test.AndroidTestCase;
import android.test.mock.MockContext;

import com.google.android.gms.location.Geofence;

import io.pivotal.android.push.prefs.PushPreferencesFCM;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.pivotal.android.push.analytics.AnalyticsEventLogger;
import io.pivotal.android.push.geofence.GeofenceEngine;
import io.pivotal.android.push.geofence.GeofencePersistentStore;
import io.pivotal.android.push.model.geofence.PCFPushGeofenceDataList;
import io.pivotal.android.push.model.geofence.PCFPushGeofenceLocationMap;
import io.pivotal.android.push.util.GeofenceHelper;
import io.pivotal.android.push.util.ModelUtil;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class GeofenceServiceTest2 extends AndroidTestCase {
    private static final Geofence GEOFENCE_1 = makeGeofence(-43.5,   61.5, 150.0f, "PCF_5_99",  Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_2 = makeGeofence( 53.5,  -91.5, 120.0f, "PCF_11_66", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_3 = makeGeofence( 53.5,  -91.5, 120.0f, "PCF_44_66", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_4 = makeGeofence( 55.5,  -94.5, 100.0f, "PCF_44_82", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_5 = makeGeofence( 63.5,  -61.5, 130.0f, "PCF_49_97", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_6 = makeGeofence( 73.5,  -61.5, 140.0f, "PCF_49_99", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);
    private static final Geofence GEOFENCE_7 = makeGeofence( 63.5,  -61.5, 160.0f, "PCF_51_53", Geofence.GEOFENCE_TRANSITION_EXIT,  Geofence.NEVER_EXPIRE);

    private final List<Geofence> GEOFENCE_LIST_ENTER = Arrays.asList(GEOFENCE_1);
    private final List<Geofence> GEOFENCE_LIST_EXIT_1 = Arrays.asList(GEOFENCE_2);
    private final List<Geofence> GEOFENCE_LIST_EXIT_2 = Arrays.asList(GEOFENCE_2, GEOFENCE_3, GEOFENCE_4, GEOFENCE_5, GEOFENCE_6, GEOFENCE_7);
    private PCFPushGeofenceDataList GEOFENCE_DATA_LIST;

    private GeofenceHelper helper;
    private GeofencePersistentStore store;
    private GeofenceEngine engine;
    private AnalyticsEventLogger eventLogger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("dexmaker.dexcache", mContext.getCacheDir().getPath());
        helper = mock(GeofenceHelper.class);
        store = mock(GeofencePersistentStore.class);
        engine = mock(GeofenceEngine.class);
        eventLogger = mock(AnalyticsEventLogger.class);
        GEOFENCE_DATA_LIST = ModelUtil.getPCFPushGeofenceDataList(getContext(),
            "geofence_five_items.json");
    }

    public void testHandleNullIntent() throws InterruptedException {
        final FakeGeofenceService service = startService(FakeGeofenceService.class);
        service.onHandleIntent(null);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verifyZeroInteractions(helper);
        verifyZeroInteractions(store);
        verifyZeroInteractions(eventLogger);
    }

    public void testHandleEmptyIntent() throws InterruptedException {
        final Intent intent = new Intent(getContext(), FakeGeofenceService.class);
        final FakeGeofenceService service = startService(FakeGeofenceService.class);
        when(helper.isGeofencingEvent()).thenReturn(false);
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verifyZeroInteractions(store);
        verifyZeroInteractions(eventLogger);
    }

    public void testReceivesGeofenceEnterEvent() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_ENTER);
        final FakeGeofenceService service = startService(FakeGeofenceService.class, getContext(), getPreferences(true));
        final PCFPushGeofenceLocationMap expectedLocationsToClear = new PCFPushGeofenceLocationMap();
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(5L), 0);
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_ENTER);
        when(store.getGeofenceData(5L)).thenReturn(GEOFENCE_DATA_LIST.get(5L));
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(1);
        service.assertTimesGeofenceExited(0);
        service.assertGeofenceEnteredContainsMessage("tacos");
        service.onDestroy();
        verify(engine, times(1)).clearLocations(eq(expectedLocationsToClear));
        verify(store, times(1)).getGeofenceData(eq(5L));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("5"), eq("99"));
        verifyNoMoreInteractions(engine);
        verifyNoMoreInteractions(store);
        verifyNoMoreInteractions(eventLogger);
    }

    public void testReceivesGeofenceEnterEventWithGeofencesDisabled() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_ENTER);
        final FakeContext context = new FakeContext(getContext());
        final FakeGeofenceService service = startService(FakeGeofenceService.class, context, getPreferences(false));
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verifyZeroInteractions(engine);
        verifyZeroInteractions(store);
        verifyZeroInteractions(eventLogger);
    }

    public void testReceivesGeofenceEnterEventForMissingGeofenceData() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_ENTER);
        final FakeGeofenceService service = startService(FakeGeofenceService.class, getContext(), getPreferences(true));
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_ENTER);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_ENTER);
        when(store.getGeofenceData(5L)).thenReturn(null);
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verify(store, times(1)).getGeofenceData(eq(5L));
        verifyZeroInteractions(engine);
        verifyNoMoreInteractions(store);
        verifyZeroInteractions(eventLogger);
    }

    public void testReceivesGeofenceExitEvent() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_EXIT);
        final FakeGeofenceService service = startService(FakeGeofenceService.class, getContext(), getPreferences(true));
        final PCFPushGeofenceLocationMap expectedLocationsToClear = new PCFPushGeofenceLocationMap();
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(11L), 0);
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_EXIT_1);
        when(store.getGeofenceData(11L)).thenReturn(GEOFENCE_DATA_LIST.get(11L));
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(1);
        service.assertGeofenceExitedContainsMessage("pizzas");
        service.onDestroy();
        verify(engine, times(1)).clearLocations(eq(expectedLocationsToClear));
        verify(store, times(1)).getGeofenceData(eq(11L));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("11"), eq("66"));
        verifyNoMoreInteractions(store);
        verifyNoMoreInteractions(engine);
        verifyNoMoreInteractions(eventLogger);
    }

    public void testReceivesGeofenceExitEventWithGeofencesDisabled() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_EXIT);
        final FakeContext context = new FakeContext(getContext());
        final FakeGeofenceService service = startService(FakeGeofenceService.class, context, getPreferences(false));
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_EXIT_1);
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verifyZeroInteractions(engine);
        verifyZeroInteractions(store);
        verifyZeroInteractions(eventLogger);
    }

    public void testReceivesGeofenceExitEventForMissingGeofenceData() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_EXIT);
        final FakeGeofenceService service = startService(FakeGeofenceService.class, getContext(), getPreferences(true));
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_EXIT_1);
        when(store.getGeofenceData(11L)).thenReturn(null);
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(0);
        service.onDestroy();
        verify(store, times(1)).getGeofenceData(eq(11L));
        verifyNoMoreInteractions(store);
        verifyZeroInteractions(engine);
        verifyZeroInteractions(eventLogger);
    }

    public void testReceivesGeofenceExitEventMultipleWithEmptySubscribedTags() throws Exception {
        final Intent intent = createGeofenceTransitionEventIntent(getContext(), Geofence.GEOFENCE_TRANSITION_EXIT);
        final FakeGeofenceService service = startService(FakeGeofenceService.class, getContext(), getPreferences(true));
        final PCFPushGeofenceLocationMap expectedLocationsToClear = new PCFPushGeofenceLocationMap();
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(11L), 0);
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(44L), 0);
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(44L), 1);
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(49L), 0);
        expectedLocationsToClear.putLocation(GEOFENCE_DATA_LIST.get(49L), 1);
        setupMultipleEvents();
        service.onHandleIntent(intent);
        service.assertTimesGeofenceEntered(0);
        service.assertTimesGeofenceExited(5);
        service.assertGeofenceExitedContainsMessage("pizzas");
        service.assertGeofenceExitedContainsMessage("eat all this great stuff");
        service.assertGeofenceExitedContainsMessage("gelato");
        service.onDestroy();
        verifyMultipleEvents();
        verify(engine, times(1)).clearLocations(eq(expectedLocationsToClear));
        verifyNoMoreInteractions(engine);
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("11"), eq("66"));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("44"), eq("66"));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("44"), eq("82"));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("49"), eq("97"));
        verify(eventLogger, times(1)).logGeofenceTriggered(eq("49"), eq("99"));
        verifyNoMoreInteractions(eventLogger);
    }

    private void setupMultipleEvents() {
        when(helper.isGeofencingEvent()).thenReturn(true);
        when(helper.getGeofenceTransition()).thenReturn(Geofence.GEOFENCE_TRANSITION_EXIT);
        when(helper.getGeofences()).thenReturn(GEOFENCE_LIST_EXIT_2);
        when(store.getGeofenceData(11L)).thenReturn(GEOFENCE_DATA_LIST.get(11L));
        when(store.getGeofenceData(44L)).thenReturn(GEOFENCE_DATA_LIST.get(44L));
        when(store.getGeofenceData(49L)).thenReturn(GEOFENCE_DATA_LIST.get(49L));
        when(store.getGeofenceData(51L)).thenReturn(GEOFENCE_DATA_LIST.get(51L));
    }

    private void verifyMultipleEvents() {
        verify(store, times(1)).getGeofenceData(eq(11L));
        verify(store, times(2)).getGeofenceData(eq(44L));
        verify(store, times(2)).getGeofenceData(eq(49L));
        verify(store, times(1)).getGeofenceData(eq(51L));
        verifyNoMoreInteractions(store);
    }

    private Intent createGeofenceTransitionEventIntent(Context context, int transition) throws IOException {
        final Intent intent = new Intent(context, FakeGeofenceService.class);
        intent.putExtra("com.google.android.location.intent.extra.transition", transition);
        return intent;
    }

    private static Geofence makeGeofence(double latitude, double longitude, float radius, String requestId, int transition, long duration) {
        return new Geofence.Builder()
                .setCircularRegion(latitude, longitude, radius)
                .setRequestId(requestId)
                .setTransitionTypes(transition)
                .setExpirationDuration(duration)
                .build();
    }

    private PushPreferencesFCM getPreferences(boolean areGeofencesEnabled, String... tags) {
        final Set<String> set;
        if (tags != null) {
            set = new HashSet<>(Arrays.asList(tags));
        } else {
            set = null;
        }
        PushPreferencesFCM pushPreferences = mock(PushPreferencesFCM.class);
        when(pushPreferences.areGeofencesEnabled()).thenReturn(areGeofencesEnabled);
        when(pushPreferences.getTags()).thenReturn(set);
        return pushPreferences;
    }

    private <T extends FakeGeofenceService> T startService(final Class<T> klass) {
        return startService(klass, getContext(), getPreferences(false));
    }

    private <T extends FakeGeofenceService> T startService(final Class<T> klass, final Context context, final PushPreferencesFCM preferences) {
        try {
            final Object object = klass.newInstance();
            final T service = klass.cast(object);
            service.setGeofenceHelper(helper);
            service.setGeofencePersistentStore(store);
            service.setGeofenceEngine(engine);
            service.setEventLogger(eventLogger);
            service.setPushPreferences(preferences);
            service.attachBaseContext(context);
            service.onCreate();
            return service;
        } catch (Exception e) {
            return null;
        }
    }

    private final class FakeContext extends MockContext {

        private final Context context;
        private Intent startedServiceIntent;

        private FakeContext(Context context) {
            this.context = context;
        }

        @Override
        public Context getApplicationContext() {
            return this;
        }

        @Override
        public String getPackageName() {
            return context.getPackageName();
        }

        @Override
        public ComponentName startService(Intent service) {
            startedServiceIntent = service;
            return service.getComponent();
        }

        @Override
        public ClassLoader getClassLoader() {
            return context.getClassLoader();
        }

        public Intent getStartedServiceIntent() {
            return startedServiceIntent;
        }
    }

    private static final class FakeGeofenceService extends GeofenceService {

        private List<String> enteredGeofencesMessages = new ArrayList<>();
        private List<String> exitedGeofencesMessages = new ArrayList<>();
        private int timesEnteredGeofences = 0;
        private int timesExitedGeofences = 0;

        public FakeGeofenceService() {
            super();
        }

        @Override
        public void attachBaseContext(final Context base) {
            super.attachBaseContext(base);
        }

        @Override
        public void onGeofenceEnter(Bundle payload) {
            timesEnteredGeofences += 1;
            if (payload != null) {
                enteredGeofencesMessages.add(payload.getString("message"));
            }
        }

        @Override
        public void onGeofenceExit(Bundle payload) {
            timesExitedGeofences += 1;
            if (payload != null) {
                exitedGeofencesMessages.add(payload.getString("message"));
            }
        }

        public void assertTimesGeofenceEntered(final int expected) {
            assertEquals(expected, timesEnteredGeofences);
        }

        public void assertTimesGeofenceExited(final int expected) {
            assertEquals(expected, timesExitedGeofences);
        }

        public void assertGeofenceEnteredContainsMessage(final String expectedMessage) {
            assertTrue(enteredGeofencesMessages.contains(expectedMessage));
        }

        public void assertGeofenceExitedContainsMessage(final String expectedMessage) {
            assertTrue(exitedGeofencesMessages.contains(expectedMessage));
        }
    }
}