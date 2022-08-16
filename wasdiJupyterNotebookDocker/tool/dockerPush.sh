#!/bin/bash

## LOG MANAGEMENT ##
iCurrentPid=${$}
exec 1> >(logger --id=${iCurrentPid} --stderr --tag docker-push) 2>&1
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
echo "[INFO] Checking if we have repository"

if [[ -z "${listDockerRemoteRepository}" || "${#listDockerRemoteRepository[@]}" -eq 0 ]]
then
    echo "[INFO] No repository was provided: nothing to do"
    exit 0
else
    echo "[INFO] We have repository: we can continue operations"
fi

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


## PUSH THE IMAGE TO A REMOTE REPOSITORY ##
for sCurrentDockerRemoteRepository in ${listDockerRemoteRepository}
do
    # the login is a blocking instruction
    docker login https://${sCurrentDockerRemoteRepository}/
    iReturnCode=${?}
    calculReturnCode ${iReturnCode}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        # for the push, in case of failure,
        # we accept to continue until the end
        # but we exit with a code different than 0
        docker image push ${sCurrentDockerRemoteRepository}/${sContainerName}:${sContainerVersion}
        iReturnCode=${?}
        calculReturnCode ${iReturnCode}

        docker image push ${sCurrentDockerRemoteRepository}/${sContainerName}:latest
        iReturnCode=${?}
        calculReturnCode ${iReturnCode}
    fi
done
## /PUSH THE IMAGE TO A REMOTE REPOSITORY ##


## EXIT ##
calculReturnCode
exit ${?}
## /EXIT ##
