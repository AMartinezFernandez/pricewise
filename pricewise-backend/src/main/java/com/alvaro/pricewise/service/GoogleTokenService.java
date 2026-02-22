package com.alvaro.pricewise.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alvaro.pricewise.exception.BadRequestException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleTokenService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenService(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    /**
     * Valida un Google ID token y devuelve el payload con email, name, etc.
     * @throws BadRequestException si el token es invalido o esta expirado
     */
    public GoogleIdToken.Payload verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new BadRequestException("Token de Google invalido o expirado");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new BadRequestException("El email de Google no esta verificado");
            }

            log.debug("Google token validado para: {}", payload.getEmail());
            return payload;

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validando token de Google: {}", e.getMessage());
            throw new BadRequestException("Error al validar el token de Google");
        }
    }
}
