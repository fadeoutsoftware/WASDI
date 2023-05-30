#!/bin/bash

sWorkingDirectory="${1}"

if [[ -z "${sWorkingDirectory}" ]]
then
    echo "[ERROR] Directory in which to work is not provided."
    echo
    echo "Usage: ${0} <directory>"
    exit 1
fi

if [[ ! -d "${sWorkingDirectory}" ]]
then
    echo "[ERROR] '${sWorkingDirectory}' doesn't exists"
    exit 1
fi

iReturnCode=0

find ${sWorkingDirectory} -type f -name "*.scss" ! -name "_*.scss" | while read sCurrentScssFile
do
    sCurrentCssFile="${sCurrentScssFile/.scss/.css}"
    sass ${sCurrentScssFile} ${sCurrentCssFile}

    if [[ ${?} -gt 0 ]]
    then
        echo "[ERROR] Error with '${sCurrentScssFile}'"
        iReturnCode=1
    fi

done

exit ${iReturnCode}
