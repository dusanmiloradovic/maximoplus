#!/bin/bash
MXENAME=${MXENAME:-maximodocker}

if [ -f "maximo.properties" ]; then
    echo "starting with maximo.properties"
    java -Dmxe.name=$MXENAME --illegal-access=warn --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -DcorsAllowed=* -cp .:"maximoplus_jars/*":"lib/*":"maximo_jars/*" -Dmaximo.properties=maximo.properties -DnreplPort=55633 maximoplus.core
else
    echo "starting without maximo.properties"
    java -Dmxe.name=$MXENAME --illegal-access=warn --add-opens java.base/java.io=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED -DcorsAllowed=* -cp .:"maximoplus_jars/*":"lib/*":"maximo_jars/*" -DnreplPort=55633 maximoplus.core
fi
