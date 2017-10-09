/*
 * Copyright (C) 2014 Pivotal Software, Inc. All rights reserved.
 */
package io.pivotal.android.push.backend.api;

public interface PCFPushRegistrationListener {
    void onPCFPushRegistrationSuccess(String pcfPushDeviceRegistrationId);
    void onPCFPushRegistrationFailed(String reason);
}
