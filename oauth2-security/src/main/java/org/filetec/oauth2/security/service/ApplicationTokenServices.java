/*
 * Copyright 2016 Filetec Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.filetec.oauth2.security.service;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import org.filetec.oauth2.security.model.ApplicationOAuth2AccessToken;
import org.filetec.oauth2.security.model.ApplicationOAuth2RefreshToken;
import org.filetec.oauth2.security.repository.ApplicationOAuth2AccessTokenRepository;
import org.filetec.oauth2.security.repository.ApplicationOAuth2RefreshTokenRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidGrantException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.common.util.SerializationUtils;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.ConsumerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author warren.nocos
 */
@Service
@Primary
@Transactional(readOnly = true)
public class ApplicationTokenServices implements AuthorizationServerTokenServices,
        ResourceServerTokenServices, ConsumerTokenServices {

    protected final ApplicationOAuth2AccessTokenRepository applicationOAuth2AccessTokenRepository;

    protected final ApplicationOAuth2RefreshTokenRepository applicationOAuth2RefreshTokenRepository;

    protected final ClientDetailsService clientDetailsService;

    protected final TokenEnhancer accessTokenEnhancer;

    protected final AuthenticationManager authenticationManager;

    protected final AuthenticationKeyGenerator authenticationKeyGenerator;

    @Inject
    public ApplicationTokenServices(ApplicationOAuth2AccessTokenRepository applicationOAuth2AccessTokenRepository,
            ApplicationOAuth2RefreshTokenRepository applicationOAuth2RefreshTokenRepository,
            ClientDetailsService clientDetailsService, TokenEnhancer accessTokenEnhancer,
            AuthenticationManager authenticationManager) {
        this.applicationOAuth2AccessTokenRepository = applicationOAuth2AccessTokenRepository;
        this.applicationOAuth2RefreshTokenRepository = applicationOAuth2RefreshTokenRepository;
        this.clientDetailsService = clientDetailsService;
        this.accessTokenEnhancer = accessTokenEnhancer;
        this.authenticationManager = authenticationManager;
        authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();
    }

    @Override
    @Transactional
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
        return applicationOAuth2AccessTokenRepository.findByOauth2AuthenticationKey(authenticationKeyGenerator.extractKey(authentication))
                .map(applicationOAuth2Access -> Optional.of(applicationOAuth2Access)
                .filter(theApplicationOAuth2Access -> !theApplicationOAuth2Access.isExpired())
                // If not expired, re-store the access token in case the authentication has changed
                .map(nonExpiredApplicationOAuth2Access -> {
                    nonExpiredApplicationOAuth2Access.setSerializedOAuth2Authentication(SerializationUtils.serialize(authentication));
                    applicationOAuth2AccessTokenRepository.save(nonExpiredApplicationOAuth2Access);
                    return nonExpiredApplicationOAuth2Access.getApplicationOAuth2RefreshToken();
                })
                // Remove expired token, should be replaced
                .orElseGet(() -> {
                    ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken = applicationOAuth2Access.getApplicationOAuth2RefreshToken();
                    applicationOAuth2RefreshToken.setApplicationOAuth2AccessToken(null);
                    applicationOAuth2AccessTokenRepository.delete(applicationOAuth2Access);
                    return applicationOAuth2RefreshToken;
                }))
                .map(applicationOAuth2RefreshToken -> Optional.of(applicationOAuth2RefreshToken)
                .filter(theApplicationOAuth2RefreshToken -> !theApplicationOAuth2RefreshToken.isExpired())
                .map(ApplicationOAuth2RefreshToken::getApplicationOAuth2AccessToken)
                // But the refresh token itself might need to be re-issued if it has
                // expired.
                .orElseGet(() -> {
                    ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authentication.getOAuth2Request().getClientId());
                    ApplicationOAuth2RefreshToken newApplicationOAuth2RefreshToken = createApplicationOAuth2RefreshToken(clientDetails);
                    Optional.ofNullable(applicationOAuth2RefreshToken.getApplicationOAuth2AccessToken())
                            .ifPresent(applicationOAuth2AccessToken -> {
                                applicationOAuth2AccessToken.setApplicationOAuth2RefreshToken(newApplicationOAuth2RefreshToken);
                                newApplicationOAuth2RefreshToken.setApplicationOAuth2AccessToken(applicationOAuth2AccessToken);
                            });
                    return newApplicationOAuth2RefreshToken.getApplicationOAuth2AccessToken();
                }))
                // Only create a new refresh token if there wasn't an existing one
                // associated with an expired access token.
                // Clients might be holding existing refresh tokens, so we re-use it in
                // the case that the old access token
                // expired.
                .orElseGet(() -> {
                    ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authentication.getOAuth2Request().getClientId());
                    return createApplicationOAuth2AccessToken(clientDetails, authentication, createApplicationOAuth2RefreshToken(clientDetails));
                });
    }

    @Override
    @Transactional(noRollbackFor = {InvalidTokenException.class, InvalidGrantException.class})
    public OAuth2AccessToken refreshAccessToken(String refreshToken, TokenRequest tokenRequest) throws AuthenticationException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OAuth2AccessToken getAccessToken(OAuth2Authentication authentication) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean revokeToken(String tokenValue) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    protected ApplicationOAuth2RefreshToken createApplicationOAuth2RefreshToken(ClientDetails clientDetails) {
        return Optional.of(clientDetails)
                .filter(theClientDetails -> theClientDetails.getAuthorizedGrantTypes().contains("refresh_token"))
                .map(theClientDetails -> Optional.ofNullable(theClientDetails.getRefreshTokenValiditySeconds())
                // default 30 days
                .orElse(60 * 60 * 24 * 30))
                .map(refreshTokenValiditySeconds -> {
                    ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken;
                    applicationOAuth2RefreshToken = new ApplicationOAuth2RefreshToken();
                    for (boolean exists = true; exists;) {
                        String hashedRefreshTokenValue = hashObject(UUID.randomUUID().toString());
                        exists = applicationOAuth2RefreshTokenRepository.findByValue(hashedRefreshTokenValue)
                                .map(oAuth2RefreshToken -> true)
                                .orElseGet(() -> {
                                    applicationOAuth2RefreshToken.setValue(hashedRefreshTokenValue);
                                    return false;
                                });
                    }
                    applicationOAuth2RefreshToken.setExpiration(new Date(System.currentTimeMillis() + (refreshTokenValiditySeconds * 1000L)));
                    return applicationOAuth2RefreshToken;
                })
                .orElse(null);
    }

    protected ApplicationOAuth2AccessToken createApplicationOAuth2AccessToken(ClientDetails clientDetails,
            OAuth2Authentication authentication, ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken) {
        ApplicationOAuth2AccessToken applicationOAuth2AccessToken;
        applicationOAuth2AccessToken = new ApplicationOAuth2AccessToken();
        Integer accessTokenValiditySeconds = clientDetails.getAccessTokenValiditySeconds();
        // default 12 hours.
        applicationOAuth2AccessToken.setExpiresIn(accessTokenValiditySeconds == null ? 60 * 60 * 12 : accessTokenValiditySeconds);
        applicationOAuth2AccessToken.setScope(authentication.getOAuth2Request().getScope());
        applicationOAuth2AccessToken.setOauth2AuthenticationKey(authenticationKeyGenerator.extractKey(authentication));
        applicationOAuth2AccessToken.setSerializedOAuth2Authentication(SerializationUtils.serialize(authentication));
        applicationOAuth2AccessToken.setApplicationOAuth2RefreshToken(applicationOAuth2RefreshToken);
        applicationOAuth2RefreshToken.setApplicationOAuth2AccessToken(applicationOAuth2AccessToken);
        return applicationOAuth2AccessToken;
    }

    protected String hashObject(Object object) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).");
        }
        try {
            byte[] bytes = digest.digest(object.toString().getBytes("UTF-8"));
            return String.format("%032x", new BigInteger(1, bytes));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).");
        }
    }

}
