# openems-edge-pvinverter-solaredge-se9k
OpenEMS Edge PvInverter SolarEdge SE9K

Applies to SolarEdge SE9K (and probably other types of this series).

Implemented Natures:
- ElectricityMeter
- ManagedSymmetricPvInverter

[https://github.com/chpoulter/openems-addons](https://github.com/chpoulter/openems-addons "https://github.com/chpoulter/openems-addons")

## Build

Just build with maven clean package and you'll get a jar file that can be added to the felix load
folder. 

You will need a local maven repository containing OpenEMS bundles. Use maven-repo to generate one.

## License

Copyright © 2025 Christian Poulter <devel(at)poulter.de>

The OpenEMS PvInverter SolarEdge Se9k bundle is released under the GNU AFFERO GENERAL PUBLIC LICENSE Version 3 license, for more information, check the LICENSE file.

It makes use of third party libraries, for more information check the LICENSE-3RD-PARTY file.

It is intended to be run in an [OpenEMS instance](https://github.com/OpenEMS/openems "OpenEMS instance"), licenses can be found at [https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0](https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0 "https://github.com/OpenEMS/openems/blob/develop/LICENSE-EPL-2.0")
