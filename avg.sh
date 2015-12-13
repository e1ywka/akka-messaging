#!/bin/bash

source conf.sh

socat - UDP4-LISTEN:4447,fork &
PID=$!

sleep 3s

if [ -n "${PID}" ]; then
    echo "avg" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast
    kill ${PID}
fi

