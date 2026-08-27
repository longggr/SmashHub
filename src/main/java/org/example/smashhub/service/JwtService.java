package org.example.smashhub.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.example.smashhub.dto.request.IntrospectRequest;
import org.example.smashhub.dto.response.IntrospectResponse;
import org.example.smashhub.entity.User;
import org.example.smashhub.exception.AppException;
import org.example.smashhub.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtService {
    TokenBlacklistService tokenBlacklistService;
    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;
    public String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("cms.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }


    public void invalidateToken(String token){
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String jit = signedJWT.getJWTClaimsSet().getJWTID();
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            tokenBlacklistService.blacklist(jit,expiryTime);
        } catch (ParseException e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (user.getRole() != null)
            stringJoiner.add("ROLE_" + user.getRole().getName());
        return stringJoiner.toString();
    }

    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        String token = request.getToken();
        boolean isValid = true;
        try {
            verifyToken(token);
        }catch (AppException e){
            isValid = false;
        }
        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    public SignedJWT verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        boolean verified = signedJWT.verify(verifier);

        if(!verified||!expiryTime.after(new Date())){
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        if (tokenBlacklistService.isBlacklisted(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        return signedJWT;
    }
}
