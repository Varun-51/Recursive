package com.recursive.application;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tracks whether the user accepted the license. Persistence of the
 * acceptance state is an infrastructure concern; this service only owns
 * the in-session fact.
 */
public class LicenseService {

    private final AtomicBoolean accepted = new AtomicBoolean();

    public boolean isAccepted() {
        return accepted.get();
    }

    public void accept() {
        accepted.set(true);
    }
}
