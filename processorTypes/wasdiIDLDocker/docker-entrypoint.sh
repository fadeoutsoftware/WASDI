#!/bin/bash

if [[ -n "${WASDI_IDL_LICENSE_SERVER_URL}" ]]
then
    echo "${WASDI_IDL_LICENSE_SERVER_URL}" > /usr/local/harris/license/o_licenseserverurl.txt
fi

gunicorn -w 1 --max-requests 1 -t 3600 -b 0.0.0.0:5000 wasdiProcessorServer:app