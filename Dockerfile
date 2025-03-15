ARG TAG=2025.3.0

### git
FROM alpine/git AS git

RUN git clone https://github.com/OpenEMS/openems.git /openems
WORKDIR /openems
RUN git checkout 2025.3.0

### gradle: build
FROM gradle:jdk21 AS gradle

COPY --from=git /openems /openems

WORKDIR /openems

RUN ./gradlew build

RUN mkdir -p /jars
RUN find -type f -name "io.openems.*.jar" -exec cp {} ../jars ';'

### bash: build script
FROM bash AS bash
ARG TAG
ARG BUNDLES

RUN echo zzzzz $BUNDLES

COPY --from=gradle /jars /jars

RUN echo "#!/bin/bash" > /mvndeploy.sh
    
WORKDIR /jars

RUN mkdir -p /jars2

RUN for f in io.openems.common.jar \
             io.openems.edge.common.jar \
             io.openems.edge.bridge.modbus.jar \
             io.openems.edge.meter.api.jar \
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
#RUN cat /mvndeploy.sh
RUN /mvndeploy.sh  