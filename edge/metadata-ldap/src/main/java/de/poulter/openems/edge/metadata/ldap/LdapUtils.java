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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class LdapUtils {

    private static final Logger log = LoggerFactory.getLogger(LdapUtils.class);

    public static SearchControls createSearchControls(String... attributeNames) {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributeNames);

        return searchControls;
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String defaultValue,
        String distinguishedName
    ) throws NamingException {
        return extractValueFromAttributes(attributes, attributeId, defaultValue, distinguishedName, true);
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String defaultValue,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        String value = extractValueFromAttributes(attributes, attributeId, distinguishedName, required);

        if (value == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ", using default value " + defaultValue + ".");
            }

            value = defaultValue;
        }

        return value;
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName
    ) throws NamingException {
        return extractValueFromAttributes(attributes, attributeId, distinguishedName, true);
    }

    public static String extractValueFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        Attribute attribute = attributes.get(attributeId);

        if (attribute == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ".");
            }
            return null;
        }
        return (String) attribute.get();
    }

    public static List<String> extractValuesFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName
    ) throws NamingException {
        return extractValuesFromAttributes(attributes, attributeId, distinguishedName, true);
    }

    public static List<String> extractValuesFromAttributes(
        Attributes attributes,
        String attributeId,
        String distinguishedName,
        boolean required
    ) throws NamingException {
        Attribute attribute = attributes.get(attributeId);

        if (attribute == null) {
            if (required) {
                log.info(attributeId + " attribute not found for dn " + distinguishedName + ".");
            }

            return null;
        }

        List<String> values = new ArrayList<>();
        NamingEnumeration<?> namingEnumeration = attribute.getAll();

        while (namingEnumeration.hasMore()) {
            String value = (String) namingEnumeration.next();
            if (value != null) {
                values.add(value);
            }
        }

        return values;
    }

    public static Role extractGlobalRole(Attributes attributes, String attributeId, String distinguishedName, Map<String, Role> roleMap) throws NamingException {

        if (roleMap == null || roleMap.isEmpty()) {
            return null;
        }

        final List<String> values = LdapUtils.extractValuesFromAttributes(attributes, attributeId, distinguishedName);

        if (values == null || values.isEmpty()) {
            return null;
        }

        Role role = values.stream()
            .filter(roleKey -> {
                if (roleKey == null) {
                    log.error("Role key is null.");
                    return false;
                }

                if (roleMap.containsKey(roleKey)) {
                    return true;

                } else {
                    log.error("No role " + roleKey + " found.");
                    return false;

                }
            })
            .map(roleKey -> roleMap.get(roleKey))
            .sorted((a,b) -> Integer.compare(a.getLevel(), b.getLevel()))
            .findFirst()
            .orElse(null)
            ;

        return role;
    }

    public static Language extractLanguage(Attributes attributes, String attributeId, String distinguishedName) throws NamingException {
        final String languageString = LdapUtils.extractValueFromAttributes(attributes, attributeId, distinguishedName, false);

        Language language = null;
        try {
            language = Language.from(Optional.ofNullable(languageString));

        } catch (OpenemsException ex) {
            log.info("language not found for " + distinguishedName + ", using " + languageString + ".");
        }

        if (language == null) {
            log.info("language attribute not found for " + distinguishedName + ", using " + languageString + ".");
            language = Language.DEFAULT;
        } 

        return language;
    }

}
