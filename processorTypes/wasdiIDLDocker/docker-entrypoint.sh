#!/bin/bash

if [[ -n "${WASDI_IDL_LICENSE_SERVER_URL}" ]]
then
    echo "${WASDI_IDL_LICENSE_SERVER_URL}" > /usr/local/harris/license/o_licenseserverurl.txt
fi

sudo -u appwasdi python3 /usr/bin/gunicorn --workers 1 --max-requests 1 --timeout 3600 --bind 0.0.0.0:5000 wasdiProcessorServer:app
