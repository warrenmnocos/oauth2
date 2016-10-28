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
package org.filetec.oauth2.security.model;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;

/**
 *
 * @author warren.nocos
 */
@Entity
@Table(name = "application_oauth2_access_token")
public class ApplicationOAuth2AccessToken implements OAuth2AccessToken, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_TOKEN_TYPE = OAuth2AccessToken.BEARER_TYPE.toLowerCase();

    @Id
    @Column(nullable = false,
            name = "id")
    @TableGenerator(initialValue = 1,
            name = "application_oauth2_access_token_id_generator",
            pkColumnName = "table_name",
            pkColumnValue = "application_oauth2_access_token",
            table = "id_generator",
            valueColumnName = "available_id")
    @GeneratedValue(generator = "application_oauth2_access_token_id_generator",
            strategy = GenerationType.TABLE)
    protected BigInteger id;

    @ElementCollection(fetch = FetchType.EAGER,
            targetClass = String.class)
    @CollectionTable(foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT),
            joinColumns = @JoinColumn(name = "application_oauth2_access_token_id",
                    nullable = false,
                    referencedColumnName = "id"),
            name = "application_oauth2_access_token_scope")
    @Column(name = "scope",
            nullable = false)
    @NotNull
    protected Set<String> scope;

    @OneToOne(cascade = CascadeType.ALL,
            targetEntity = ApplicationOAuth2RefreshToken.class)
    @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.CONSTRAINT),
            name = "application_oAuth2_refresh_token_id",
            nullable = false,
            referencedColumnName = "id")
    @NotNull
    protected ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken;

    @Column(name = "token_type",
            nullable = false)
    @NotNull
    protected String tokenType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration",
            nullable = false)
    @NotNull
    protected Date expiration;

    @Column(name = "value",
            nullable = false)
    @NotNull
    protected String value;

    @Lob
    @Column(name = "serialized_oAuth2_authentication",
            nullable = false)
    @NotNull
    protected byte[] serializedOAuth2Authentication;

    public ApplicationOAuth2AccessToken() {
        tokenType = DEFAULT_TOKEN_TYPE;
    }

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Set<String> getScope() {
        return scope;
    }

    public void setScope(Set<String> scope) {
        this.scope = scope;
    }

    @Override
    public OAuth2RefreshToken getRefreshToken() {
        return applicationOAuth2RefreshToken;
    }

    public ApplicationOAuth2RefreshToken getApplicationOAuth2RefreshToken() {
        return applicationOAuth2RefreshToken;
    }

    public void setApplicationOAuth2RefreshToken(ApplicationOAuth2RefreshToken applicationOAuth2RefreshToken) {
        this.applicationOAuth2RefreshToken = applicationOAuth2RefreshToken;
    }

    @Override
    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public boolean isExpired() {
        return expiration != null && expiration.before(new Date());
    }

    @Override
    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    @Override
    public int getExpiresIn() {
        return expiration != null ? Long.valueOf((expiration.getTime() - System.currentTimeMillis()) / 1000L)
                .intValue() : 0;
    }

    public void setExpiresIn(int expiresIn) {
        setExpiration(new Date(System.currentTimeMillis() + expiresIn));
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public byte[] getSerializedOAuth2Authentication() {
        return serializedOAuth2Authentication;
    }

    public void setSerializedOAuth2Authentication(byte[] serializedOAuth2Authentication) {
        this.serializedOAuth2Authentication = serializedOAuth2Authentication;
    }

}
