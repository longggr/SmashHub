package org.example.smashhub.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import org.example.smashhub.auth.dto.request.IntrospectRequest;
import org.example.smashhub.auth.dto.response.IntrospectResponse;
import org.example.smashhub.user.entity.User;

import java.text.ParseException;

public interface JwtService {
    String generateToken(User user);
    void invalidateToken(String token);
    IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException;
    SignedJWT verifyToken(String token) throws ParseException, JOSEException;
}
