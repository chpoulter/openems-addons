/*
 *   OpenEMS Edge Metadata LDAP bundle
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2024 Christian Poulter <devel(at)poulter.de>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *   SPDX-License-Identifier: AGPL-3.0-or-later
 *
 */

package de.poulter.openems.edge.metadata.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchResult;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.user.User;
import io.openems.edge.common.user.UserService;

@Designate(
    ocd = Config.class,
    factory = false
)
@Component(
    name = "Core.User.LDAP",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE		
)
public class UserServiceImpl implements UserService {

	private final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	private LdapContextManager ldapContextManager;

    private String ldapInitialContextFactory;
    private String ldapProviderUrl;
    private String ldapSecurityPrincipalTemplate;
    private String ldapUsersOu;
    private String ldapUsersOuFilter;

    private Map<String, Role> roleMap; 

    private boolean active = false;

	@Activate
	void activate(Config config) {
        log.info("Activating Core User LDAP");

        ldapContextManager = new LdapContextManager(config);

        ldapInitialContextFactory = config.ldapInitialContextFactory();
        ldapProviderUrl = config.ldapProviderUrl();
        ldapSecurityPrincipalTemplate = config.ldapSecurityPrincipalTemplate();
        ldapUsersOu = config.ldapUsersOu();
        ldapUsersOuFilter = config.ldapUsersOuFilter();

        String ldapGroupOuGuest = config.ldapGroupOuGuest();
        String ldapGroupOuOwner = config.ldapGroupOuOwner();
        String ldapGroupOuInstaller = config.ldapGroupOuInstaller();
        String ldapGroupOuAdmin = config.ldapGroupOuAdmin();

        roleMap = new HashMap<>();
        roleMap.put(ldapGroupOuGuest, Role.GUEST);
        roleMap.put(ldapGroupOuOwner, Role.OWNER);
        roleMap.put(ldapGroupOuInstaller, Role.INSTALLER);
        roleMap.put(ldapGroupOuAdmin, Role.ADMIN);

        active = true;
	}

	@Deactivate
	void deactivate() {
	    log.info("Deactivating Core User LDAP");

	    CompletableFuture.runAsync(() -> ldapContextManager.shutdown());

	    ldapContextManager = null;

        ldapInitialContextFactory = null;
        ldapProviderUrl = null;
        ldapSecurityPrincipalTemplate = null;
        ldapUsersOu = null;
        ldapUsersOuFilter = null;

	    roleMap = null;

	    active = false;
	}

	@Override
	public final Optional<User> authenticate(String username, String credentials) {
	    log.info("Authenticating username " + username + ".");

	    if (!active) {
	        log.warn("Access denied. The ldap user service is not active.");
	        return Optional.empty();
	    }

         // authenticate user against LDAP
	    if (authenticateAgainstLdap(username, credentials)) {
	        return readUser(username);
	    }

        return Optional.empty();  
	}

	@Override
	public final Optional<User> authenticate(String password) {
	    log.warn("Access denied. The ldap user service requires an username.");

	    return Optional.empty();
	}

	private boolean validateUsername(String username) {
	    return username.matches("[\\w\\s]*");
	}

    private boolean validateCredentials(String credentials) {
        return credentials.matches("[\\w]*");
    }

    private boolean validateUsernameWithCredentials(String username, String credentials) {
        return validateUsername(username) && validateCredentials(credentials);
    }

    private boolean authenticateAgainstLdap(String username, String credentials) {
        try {
            if (!validateUsernameWithCredentials(username, credentials)) {
                log.warn("Authentication failed for user[" + username + "]: illegal characters");
                return false;
            }

            String principal = ldapSecurityPrincipalTemplate.replace("%%username%%", username);

            Properties props = new Properties();
            props.put(Context.INITIAL_CONTEXT_FACTORY, ldapInitialContextFactory);
            props.put(Context.PROVIDER_URL, ldapProviderUrl);
            props.put(Context.SECURITY_PRINCIPAL, principal);
            props.put(Context.SECURITY_CREDENTIALS, credentials);

            InitialDirContext authContext = new InitialDirContext(props);
            authContext.close();

            log.info("Authentication successful for user[" + username + "].");
            return true;

        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for user[" + username + "]: does not exists or wrong password");
            return false;

        } catch (NamingException ex) {
            log.error("Could not access LDAP.", ex);
            return false;
        }
    }

    public Optional<User> readUser(String username) {
        try {
            DirContext context = ldapContextManager.getContext();

            NamingEnumeration<SearchResult> searchResults = context.search(
                ldapUsersOu,
                ldapUsersOuFilter.replace("%%username%%", username),
                LdapUtils.createSearchControls(
                    "uid",
                    "memberOf",
                    "displayName",
                    "preferredLanguage"
                )
            );

            if (!searchResults.hasMore()) {
                log.warn("User " + username + " not found.");
                return Optional.empty();
            }

            SearchResult searchResult = searchResults.next();
            Attributes resultAttributes = searchResult.getAttributes();

            // id
            String id = LdapUtils.extractValueFromAttributes(resultAttributes, "uid", username);
            if (id == null) {
                log.warn("Missing uid for user " + username + ".");
                return Optional.empty();
            }

            // name
            final String name = LdapUtils.extractValueFromAttributes(resultAttributes, "displayName", id, username);

            // language
            Language language = LdapUtils.extractLanguage(resultAttributes, "preferredLanguage", username);

            // globalRole
            Role globalRole = LdapUtils.extractGlobalRole(resultAttributes, "memberOf", username, roleMap);
            if (globalRole == null) {
                log.error("GlobalRole could not be determined for user " + username + ".");
                return Optional.empty();
            }

            LdapUser user = new LdapUser(id, name, language, globalRole);

            log.info("User " + username + " read from LDAP: " + user);

            return Optional.of(user);

        } catch (NamingException ex) {
            log.error("Could not access LDAP.", ex);

            return Optional.empty();
        }
    }

    @Override
    public void registerAdminUser(String setupKey, String username, String password) throws OpenemsNamedException {
        throw new OpenemsNamedException(OpenemsError.GENERIC, "Please manage users using your favorite LDAP administration tool.");
    }
}