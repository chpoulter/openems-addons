# openems-backend-metadata-ldap
OpenEMS Backend implementation for LDAP Metadata.

Allows authentication, authorization and edge configuration through a LDAP server.

Attention: with OpenEMS 2026.2.0 authentication API has changed and was not adopted here.
Likely this will compile but not work. As I do not use the backend anymore, I stopped
maintaining this code beyond compiling.

[https://github.com/chpoulter/openems-addons](https://github.com/chpoulter/openems-addons "https://github.com/chpoulter/openems-addons")

## Build

Just build with maven clean package and you'll get a jar file that can be added to the felix load folder.

You will need a local maven repository containing OpenEMS bundles. Use maven-repo to generate one.

## LDAP example structure

```
dc=test
 +-- ou=ems
 |    +-- ou=edges [defines edges, configuration: Edges ou]
 |    |    +-- cn=edge0
 |    |    |   objectclass: device
 |    |    |   description: Test Edge 0
 |    |    |   serialnumber: ThisIsTheApiKey1
 |    |    |   owner: cn=edgegroup0,ou=groups,ou=ems,dc=test  [members of this group will have the defined role for this edge]
 |    |    |
 |    |    +-- cn=edge1
 |    |    .   objectclass: device
 |    |    .   description: Test Edge 1
 |    |    .   serialnumber: ThisIsTheApiKey2
 |    |        owner: cn=edgegroup0,ou=groups,ou=ems,dc=test  [members of these groups will have the defined role for this edge]
 |    |        owner: cn=edgegroup1,ou=groups,ou=ems,dc=test
 |    |
 |    +-- ou=groups [groups users with a role to a group, configuration: Edges group ou]
 |         +-- cn=edgegroup0
 |         |   objectclass: groupOfNames
 |         |   businesscategory: OWNER                        [the role a user will get]
 |         |   member: uid=username2,ou=users,dc=test
 |         |   member: uid=username3,ou=users,dc=test
 |         |   member: uid=username4,ou=users,dc=test
 |         |
 |         +-- cn=edgegroup1
 |         .   objectclass: groupOfNames
 |         .   businesscategory: INSTALLER                    [the role a user will get]
 |         .   member: uid=username5,ou=users,dc=test
 |
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
     |   objectclass: person
     |   objectclass: organizationalPerson
     |   objectclass: inetOrgPerson
     |   objectclass: uidObject
     |   (memberOf: cn=openems_admin)
     |   cn: firstname lastname
     |   displayname: firstname lastname
     |   givenname: firstname
     |   sn: lastname
     |   street: street housenumber
     |   postalcode: postalcode
     |   l: city
     |   st: state
     |   telephonenumber: telephon number
     |   mail: mail@mail.mail
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
