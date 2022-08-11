#!/bin/bash

## LOG MANAGEMENT ##
exec 1> >(logger --stderr --tag docker-build) 2>&1
## /LOG MANAGEMENT ##


## FUNCTION ##
function calculReturnCode() {
    if [[ -z "${iGlobalReturnCode}" ]]
    then
        iGlobalReturnCode=0
    fi

    if [[ -n "${1}" && "${1}" -gt "${iGlobalReturnCode}" ]]
    then
        iGlobalReturnCode=${1}
    fi

    return ${iGlobalReturnCode}
}
## /FUNCTION ##


## SET PARAMETER ##
sContainerDirectory="$(realpath -e $(dirname ${0})/..)"

echo "[INFO] Loading general variable"

if [[ -f "${sContainerDirectory}/var/general_common.env" ]]
then
    source ${sContainerDirectory}/var/general_common.env
    echo "[INFO] OK"
else
    echo "[ERROR] The file doesn't exist"
fi

echo "[INFO] Loading bash variable"

if [[ -f "${sContainerDirectory}/var/bash_common.env" ]]
then
    source ${sContainerDirectory}/var/bash_common.env
    echo "[INFO] OK"
else
    echo "[ERROR] The file doesn't exist"
fi
## /SET PARAMETER ##


## CONTROL ##
echo "[INFO] Controling the variable '\${sContainerName}'"

if [[ -n "${sContainerName}" ]]
then
    echo "[INFO] OK"
else
    echo "[ERROR] The variable doesn't exist"
    bErrorDetected=true
fi

echo "[INFO] Controling the variable '\${sContainerVersion}'"

if [[ -n "${sContainerName}" ]]
then
    echo "[INFO] OK"
else
    echo "[ERROR] The variable doesn't exist"
    bErrorDetected=true
fi

if [[ ${bErrorDetected} == true ]]
then
    exit 1
fi
## /CONTROL ##


## BUILD THE IMAGE LOCALY ##
sDockerAdditionalTag=""

if [[ "${#listDockerRemoteRepository[@]}" -gt 0 ]]
then
    for sCurrentDockerRemoteRepository in ${listDockerRemoteRepository}
    do
        sDockerAdditionalTag="${sDockerAdditionalTag} --tag ${sCurrentDockerRemoteRepository}/${sContainerName}:${sContainerVersion}"
        sDockerAdditionalTag="${sDockerAdditionalTag} --tag ${sCurrentDockerRemoteRepository}/${sContainerName}:latest"
    done
fi


echo "[INFO] Building the container '${sContainerName}'"

docker build \
    --build-arg USR_NAME=tomcat \
    --build-arg USR_ID=$(id -u tomcat) \
    --build-arg GRP_NAME=tomcat \
    --build-arg GRP_ID=$(id -g tomcat) \
    ${sDockerAdditionalTag} \
    --tag ${sContainerName}:${sContainerVersion} \
    --tag ${sContainerName}:latest \
    ${sContainerDirectory}

iReturnCode=${?}
calculReturnCode ${iReturnCode}

if [[ ${iReturnCode} -eq 0 ]]
then
    echo "[INFO] OK"
else
    echo "[ERROR] Unable to build the container"
    calculReturnCode
    exit ${?}
fi
## /BUILD THE IMAGE LOCALY ##


## EXIT ##
calculReturnCode
exit ${?}
## /EXIT ##
