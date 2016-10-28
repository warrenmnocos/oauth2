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
import java.util.Objects;
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
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
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
        ClientDetails clientDetails = clientDetailsService.loadClientByClientId(authentication.getOAuth2Request().getClientId());
        return applicationOAuth2AccessTokenRepository.findByOauth2AuthenticationKey(authenticationKeyGenerator.extractKey(authentication))
                .map(applicationOAuth2Access -> Optional.of(applicationOAuth2Access)
                .filter(ApplicationOAuth2AccessToken::isExpired)
                .map(expiredApplicationOAuth2Access -> {
                    // Remove expired token, should be replaced
                    applicationOAuth2AccessTokenRepository.delete(applicationOAuth2Access);
                    return expiredApplicationOAuth2Access;
                })
                .orElseGet(() -> {
                    // If not expired, re-store the access token in case the authentication has changed
                    applicationOAuth2Access.setSerializedOAuth2Authentication(SerializationUtils.serialize(authentication));
                    applicationOAuth2AccessTokenRepository.save(applicationOAuth2Access);
                    return applicationOAuth2Access;
                }))
                .flatMap(Optional::ofNullable)
                .map(applicationOAuth2Access -> {
                    // Only create a new refresh token if there wasn't an existing one
                    // associated with an expired access token.
                    // Clients might be holding existing refresh tokens, so we re-use it in
                    // the case that the old access token
                    // expired.
//                    OAuth2RefreshToken oauth2RefreshToken = Optional.ofNullable(applicationOAuth2Access.getRefreshToken())
//                            .filter(oAuth2RefreshToken -> oAuth2RefreshToken instanceof ExpiringOAuth2RefreshToken)
//                            .map(oAuth2RefreshToken -> (ExpiringOAuth2RefreshToken) oAuth2RefreshToken)
//                            .orElse(createRefreshToken(clientDetails));
                    return applicationOAuth2Access;
                })
                .orElseGet(() -> {
                    return null;
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

    protected OAuth2RefreshToken createRefreshToken(ClientDetails clientDetails) {
        return Optional.of(clientDetails)
                .filter(theClientDetails -> theClientDetails.getAuthorizedGrantTypes().contains("refresh_token"))
                .map(ClientDetails::getRefreshTokenValiditySeconds)
                .filter(Objects::nonNull)
                .map(refreshTokenValiditySeconds -> {
                    ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken;
                    applicationOAuth2RefreshToken = new ApplicationOAuth2RefreshToken();
                    for (boolean exists = true; exists;) {
                        String refreshTokenValue = UUID.randomUUID().toString();
                        exists = applicationOAuth2RefreshTokenRepository.findByValue(refreshTokenValue)
                                .map(oAuth2RefreshToken -> true)
                                .orElseGet(() -> {
                                    applicationOAuth2RefreshToken.setValue(refreshTokenValue);
                                    return false;
                                });
                    }
                    applicationOAuth2RefreshToken.setExpiration(new Date(System.currentTimeMillis() + (refreshTokenValiditySeconds * 1000L)));
                    return applicationOAuth2RefreshToken;
                })
                .orElse(null);
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
