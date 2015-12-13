#!/bin/bash

source conf.sh

STOP_COUNT=$1

if [ -n "${STOP_COUNT}" ]; then
    echo "stop ${STOP_COUNT}" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast
else
    echo "stop" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast
fi