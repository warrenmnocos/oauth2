/*
 * Copyright 2016 International Systems Research Co. (ISR).
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
package org.filetec.oauth2.security.repository;

import java.math.BigInteger;
import java.util.Optional;
import org.filetec.oauth2.security.model.AccountUserDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * This is the {@link org.springframework.data.repository.Repository} for
 * {@link AccountUserDetails}.
 *
 * @author Warren Nocos
 * @since 1.0
 * @version 1.0
 */
@Repository
public interface AccountUserDetailsRepository
        extends JpaRepository<AccountUserDetails, BigInteger> {

    /**
     * This queries for {@link AccountUserDetails} using
     * {@link AccountUserDetails#username}.
     *
     * @param username the {@link AccountUserDetails#username} of the
     * {@link AccountUserDetails}
     * @return the {@link AccountUserDetails}
     */
    Optional<AccountUserDetails> findByUsername(@Param("username") String username);

}
