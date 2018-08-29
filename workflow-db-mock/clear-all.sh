#!/bin/bash
# This script will drop and recreate all caches and databases, USE WITH CARE!
#
# amchavan, 24-Jul-2018

ALL_DATABASES="msgq ngas pipeline-reports status-entities products-metadata delivery-status"
BASE_URL="http://localhost:5984"
CREDS="-u admin:admin"

if [ -z "$DRW_EU_CACHE" -o -z "$DRW_JAO_CACHE" ] ; then
	echo "Env variables DRW_EU_CACHE and/or DRW_JAO_CACHE are not defined, exiting"
	exit
fi

for database in ${ALL_DATABASES}; do
	echo "Recreating ${database}"
	curl ${CREDS} -X DELETE ${BASE_URL}/${database}
	curl ${CREDS} -X PUT    ${BASE_URL}/${database}
done

echo "Recreating ${DRW_EU_CACHE} ${DRW_JAO_CACHE}"
rm -rf   ${DRW_EU_CACHE} ${DRW_JAO_CACHE}
mkdir -p ${DRW_EU_CACHE} ${DRW_JAO_CACHE}