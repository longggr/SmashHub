package org.example.smashhub.auth.service;

public interface LoginAttemptService {
    boolean isLocked(String username);
    long getLockoutSecondsRemaining(String username);
    boolean registerFailure(String username);
    void lock(String username);
    void reset(String username);
}
