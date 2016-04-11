#!/bin/bash
set -e

if [ "$EUID" -ne 0 ]; then
    echo "This script uses functionality which requires root privileges"
    exit 1
fi

TMPFS_PATH="tmp/"
WORK_PATH_FLAG="--work-path ${TMPFS_PATH}"
DEBUG_FLAG="--debug"
FLAGS="${DEBUG_FLAG} ${WORK_PATH_FLAG}"

# Create a tmpfs to build into
if [ ! -d "${TMPFS_PATH}" ]; then
    mkdir -p "${TMPFS_PATH}"
fi
mount -t tmpfs -o size=512M,mode=0755 tmpfs ${TMPFS_PATH}

# Start the build with an empty ACI
acbuild ${FLAGS} begin

# In the event of the script exiting, end the build.
# NB: Calling 'end' deletes the current build context.
acbuildEnd() {
    export EXIT=$?
    acbuild ${FLAGS} end && \
        umount ${TMPFS_PATH} && \
        rmdir ${TMPFS_PATH} && \
        exit $EXIT
}
trap acbuildEnd EXIT

# Name the ACI
acbuild ${FLAGS} set-name org.crimeminister/api-revuecinema

# Store the version as a label
acbuild ${FLAGS} label add version "0.0.1-SNAPSHOT"

# Based on alpine
acbuild ${FLAGS} dep add quay.io/coreos/alpine-sh

# Install openjdk8-jre
acbuild ${FLAGS} copy appc/etc/apk/repositories /etc/apk/repositories
acbuild ${FLAGS} run -- apk add --update openjdk8-jre

# Have the app listen on port 8000
acbuild ${FLAGS} environment add PORT 8000

# Add a port for http traffic on port 8000
acbuild ${FLAGS} port add http tcp 8000

# Copy the app to the ACI
acbuild ${FLAGS} copy target/revuecinema-0.0.1-SNAPSHOT-standalone.jar /srv/revuecinema.jar

# Run nodejs with the app
acbuild ${FLAGS} set-exec -- /usr/bin/java -jar /srv/revuecinema.jar

# Write the result
acbuild ${FLAGS} write --overwrite api-revuecinema-0.0.1-SNAPSHOT-linux-amd64.aci
