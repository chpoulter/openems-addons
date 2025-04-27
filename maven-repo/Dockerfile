#
#    OpenEMS maven repo
#
#    Written by Christian Poulter.
#    Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU Affero General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
# 
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU Affero General Public License for more details.
# 
#    You should have received a copy of the GNU Affero General Public License
#    along with this program.  If not, see <https://www.gnu.org/licenses/>.
#
#    SPDX-License-Identifier: AGPL-3.0-or-later
#

ARG TAG=2025.3.0

### git
FROM alpine/git AS git

RUN git clone https://github.com/OpenEMS/openems.git /openems
WORKDIR /openems
#RUN git checkout 2025.4.0
RUN git checkout develop

### gradle: build
FROM gradle:jdk21 AS gradle

COPY patches /tmp/patches
COPY --from=git /openems /openems
WORKDIR /openems

RUN for i in /tmp/patches/*.patch; do echo "Apply patch $i"; git apply -v $i; done
RUN ./gradlew build

RUN mkdir -p /jars
RUN find -type f -name "io.openems.*.jar" -exec cp {} ../jars ';'

### bash: build script
FROM bash AS bash
ARG TAG
ARG BUNDLES

COPY --from=gradle /jars /jars
WORKDIR /jars
RUN ls -la > /alljarslist.txt

RUN mkdir -p /jars2
RUN echo "#!/bin/bash" > /mvndeploy.sh

RUN for f in io.openems.common.jar \
             io.openems.backend.common.jar \
             io.openems.edge.bridge.modbus.jar \
             io.openems.edge.common.jar \
             io.openems.edge.controller.api.jar \
             io.openems.edge.io.api.jar \
             io.openems.edge.meter.api.jar \
             io.openems.edge.pvinverter.api.jar \
             io.openems.edge.pvinverter.sunspec.jar \
             io.openems.edge.evcs.api.jar \
             io.openems.edge.evcs.cluster.jar \
             io.openems.edge.ess.api.jar \
             io.openems.edge.timedata.api.jar \
   ; do cp -v $f /jars2; done

WORKDIR /jars2

RUN for f in *.jar; do \
      echo mv /source/source-$f.jar /source/${f:0:-4}-source.jar >> /mvndeploy.sh; \
      echo mvn deploy:deploy-file \
       -DgroupId=io.openems \
       -DartifactId=${f:11:-4} \
       -Dversion=$TAG \
       -Durl=file:/openems-maven-repo \
       -DrepositoryId=openems-maven-repo \
       -DupdateReleaseInfo=true \
       -Dfile=/jars2/$f >> /mvndeploy.sh \
       -Dsources=/source/${f:0:-4}-source.jar; \     
   done

### maven: deploy
FROM maven 

COPY --from=bash /alljarslist.txt /alljarslist.txt
COPY --from=bash /jars2 /jars2
COPY --from=bash /mvndeploy.sh /mvndeploy.sh

WORKDIR /jars2

RUN mkdir -p /source
RUN for f in *.jar; do \
    echo "Building source for ${f}";\
    mkdir -p /src; \
    cd /src; \
    jar -f /jars2/$f -x; \
    jar -f /source/source-$f.jar -c -M -C "OSGI-OPT/src" .; \
    rm -r /src; \
  done

RUN ls -la /jars2
RUN ls -la /source

RUN chmod 755 /mvndeploy.sh
RUN /mvndeploy.sh


