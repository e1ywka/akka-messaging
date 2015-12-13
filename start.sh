#!/bin/bash

source conf.sh

START_COUNT=$1

if [ -n "${START_COUNT}" ]; then
    echo "start ${START_COUNT}" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast
else
    echo "start" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast
fi