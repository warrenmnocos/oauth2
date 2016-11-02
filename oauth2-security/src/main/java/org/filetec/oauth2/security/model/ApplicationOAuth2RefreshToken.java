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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import org.springframework.security.oauth2.common.ExpiringOAuth2RefreshToken;

/**
 *
 * @author warren.nocos
 */
@Entity
@Table(name = "application_oauth2_refresh_token")
public class ApplicationOAuth2RefreshToken implements ExpiringOAuth2RefreshToken, Serializable {

    @Id
    @Column(nullable = false,
            name = "id")
    @TableGenerator(initialValue = 1,
            name = "application_oauth2_refresh_token_id_generator",
            pkColumnName = "table_name",
            pkColumnValue = "application_oauth2_refresh_token",
            table = "id_generator",
            valueColumnName = "available_id")
    @GeneratedValue(generator = "application_oauth2_refresh_token_id_generator",
            strategy = GenerationType.TABLE)
    protected BigInteger id;

    @Column(name = "value",
            nullable = false,
            unique = true)
    @NotNull
    protected String value;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiration",
            nullable = false)
    @NotNull
    protected Date expiration;

    @OneToOne(fetch = FetchType.LAZY,
            optional = false,
            mappedBy = "applicationOAuth2RefreshToken",
            targetEntity = ApplicationOAuth2AccessToken.class)
    @NotNull
    protected ApplicationOAuth2AccessToken applicationOAuth2AccessToken;

    public BigInteger getId() {
        return id;
    }

    public void setId(BigInteger id) {
        this.id = id;
    }

    @Override
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Date getExpiration() {
        return expiration;
    }

    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }
    
    public boolean isExpired() {
        return expiration != null && expiration.getTime() < System.currentTimeMillis();
    }

    public ApplicationOAuth2AccessToken getApplicationOAuth2AccessToken() {
        return applicationOAuth2AccessToken;
    }

    public void setApplicationOAuth2AccessToken(ApplicationOAuth2AccessToken applicationOAuth2AccessToken) {
        this.applicationOAuth2AccessToken = applicationOAuth2AccessToken;
    }

}
