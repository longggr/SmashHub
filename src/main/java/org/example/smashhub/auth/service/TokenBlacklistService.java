package org.example.smashhub.auth.service;

import java.util.Date;

public interface TokenBlacklistService {
    void blacklist(String jti, Date expiryTime);
    boolean isBlacklisted(String jti);
}
