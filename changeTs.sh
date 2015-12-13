#!/bin/bash

source conf.sh

echo "ts $1" | socat - UDP-DATAGRAM:${MULTICAST_GROUP}:${MULTICAST_PORT},broadcast