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

import javax.inject.Inject;
import org.filetec.oauth2.security.repository.ApplicationClientDetailsRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.stereotype.Service;

/**
 *
 * @author warren.nocos
 */
@Service
@Primary
public class ApplicationClientDetailsService implements ClientDetailsService {

    protected ApplicationClientDetailsRepository applicationClientDetailsRepository;

    @Inject
    public ApplicationClientDetailsService(ApplicationClientDetailsRepository applicationClientDetailsRepository) {
        this.applicationClientDetailsRepository = applicationClientDetailsRepository;
    }

    @Override
    public ClientDetails loadClientByClientId(String clientId) throws ClientRegistrationException {
        return applicationClientDetailsRepository.findByClientId(clientId)
                .orElseThrow(() -> new ClientRegistrationException("Bad credentials"));
    }

}
