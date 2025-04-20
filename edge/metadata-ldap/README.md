# openems-edge-metadata-ldap
OpenEMS Edge implementation for LDAP user service.

Allows authentication and authorization through a LDAP server for edges.

[https://github.com/chpoulter/openems-addons](https://github.com/chpoulter/openems-addons "https://github.com/chpoulter/openems-addons")

## Build

Just build with maven clean package and you'll get a jar file that can be added to the felix load folder.

You will need a local maven repository containing OpenEMS bundles. Use maven-repo to generate one.

The included patch has to be applied to OpenEMS code, as the edge only has a password field for login.

## LDAP example structure

```
dc=test
 +-- ou=groups [defines the global role of the users]
 |    +-- cn=openems_admin
 |    |   objectclass: groupOfNames
 |    |   member: uid=username1,ou=users,dc=test
 |    |
 |    +-- cn=openems_guest
 |    |   objectclass: groupOfNames
 |    |   member: uid=username2,ou=users,dc=test
 |    |   member: uid=username3,ou=users,dc=test
 |    |
 |    +-- cn=openems_installer
 |    |   objectclass: groupOfNames
 |    |   member: uid=username4,ou=users,dc=test
 |    |
 |    +-- cn=openems_owner
 |    .   objectclass: groupOfNames
 |    .   member: uid=username5,ou=users,dc=test
 |    .   member: uid=username6,ou=users,dc=test
 |
 +-- ou=users [defines users, configuration: Users ou]
     +-- uid=username1
     |   objectclass: inetOrgPerson
     |   (memberOf: cn=openems_admin)
     |   displayname: firstname lastname
     |   preferredlanguage: de
     |   userPassword: XXX
     |
     +-- uid=username2
     |   ...
     |
     +-- uid=username3
     |   ...
     .
     .
     .

```

*Note:* In my LDAP the memberOf overlay is installed. It creates and updates the memberOf attribute automatically. If you do not have this overlay (which is the default), you need to maintain this attribute on your own, meaning you have to add and maintain it by hand.

I have been running my LDAP for ages, but afaik memberOf overlay is deprecated for some time now. Unfortunately I did not have time to think of alternatives yet ... dynlist overlay might be a good try.

## License

Copyright © 2025 Christian Poulter <devel(at)poulter.de>

The OpenEMS Edge Meter B+G E-TECH DS100 Bundle is released under the GNU AFFERO GENERAL PUBLIC LICENSE Version 3 license, for more information, check the LICENSE file.

It makes use of third party libraries, for more information check the LICENSE-3RD-PARTY file.

It is intended to be run in an [OpenEMS instance](https://github.com/OpenEMS/openems "OpenEMS instance"), licenses can be found at [https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0](https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0 "https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0")
