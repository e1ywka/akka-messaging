#!/bin/bash

source conf.sh

COUNT=$1
NODEID=1

if [ -z "${COUNT}" ]; then
    echo "Error: specify node count"
    exit 1
fi

if [ -a "cluster.pid" ]; then
    echo "Error: cluster is running"
    exit 1
fi

while [ ${NODEID} -le ${COUNT} ]
do
    REMOTE=$(( 2552 + ${NODEID} ))
    ${JAVA} -Xms128m -Xmx128m \
    -Dconfig.file=akka-messaging.conf \
    -Dakka.remote.netty.tcp.port=${REMOTE} \
    -Dlogback.configurationFile=./logback.xml -DLOG_FILE_ID=${NODEID} \
    -jar target/scala-2.11/akka-messaging-assembly-1.0.jar ${NODEID} ${COUNT} &>/dev/null &

    echo $! >> cluster.pid
    (( NODEID++ ))
done
