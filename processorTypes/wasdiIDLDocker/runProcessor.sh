#!/bin/bash

#### PARAMETER ####
sCommandIdl="/usr/local/bin/idl"
sWasdiHome="/home/appwasdi/application"
sScriptFile="${sWasdiHome}/call_idl.pro"
sStatus="RUNNING"
iDuration=-1
iLoopMax=5
#### /PARAMETER ####




#### FUNCTION ####
function showHelp() {
    local _exitCode="${1}"

    echo
    echo "------"
    echo "Usage: /bin/bash ${0} --pid-file <path to the PID file> --process-object-id <process object ID> --session-id <session ID>"

    if [[ -n "${_exitCode}" ]]
    then
        exit ${_exitCode}
    fi
}

function wasdiLog() {
    local _lineToLog="${1}"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] ${_lineToLog}"
}

function getStatus() {
    local _sProcessObjId="${1}"
    local _sSessionId="${2}"
    local _sStatus=""
    local _returnCode=0

    _sStatus="$(curl --silent --insecure --location --request GET "${WASDI_WEBSERVER_NODE_URL}/process/getstatusbyid?procws=${_sProcessObjId}" --header "x-session-token: ${_sSessionId}")"
    _returnCode=${?}
    echo "${_sStatus^^}"
    return ${_returnCode}
}

function setStatusError() {
    local _sProcessObjId="${1}"
    local _sSessionId="${2}"
    local _sResult=""
    local _returnCode=0

    _sResult="$(curl --silent --insecure --location --request GET "${WASDI_WEBSERVER_NODE_URL}/process/updatebyid?procws=${_sProcessObjId}&status=ERROR&perc=-1" --header "x-session-token: ${_sSessionId}")"
    _returnCode=${?}
    echo "${_sResult^^}"
    return ${_returnCode}
}
#### /FUNCTION ####




#### ARGUMENT MANAGEMENT ####
while test ${#} -gt 0
do
    case "${1}" in
        -h|--help)
            showHelp 0
        ;;

        --pid-file)
            shift

            if [[ -n "${1}" ]]
            then
                pidFileTemporary="${1}"
            fi

            shift
        ;;

        --process-object-id)
            shift

            if [[ -n "${1}" ]]
            then
                sProcessObjId="${1}"
            fi

            shift
        ;;

        --session-id)
            shift

            if [[ -n "${1}" ]]
            then
                sSessionId="${1}"
            fi

            shift
        ;;

        *)
            wasdiLog "The argument '${1}' is not recognized"
            errorDetected="true"
            shift
        ;;
    esac
done
#### /ARGUMENT MANAGEMENT ####




#### CONTROL ####
if [[ -z "${pidFileTemporary}" ]]
then
    wasdiLog "[ERROR] The variable '\${pidFile}' is empty"
    errorDetected="true"
else
    pidFile="$(realpath -m "${pidFileTemporary}")"
fi

if [[ -n "${pidFile}" && -d "${pidFile}" ]]
then
    wasdiLog "[ERROR] The variable '\${pidFile}' is pointing an existing directory"
    errorDetected="true"
fi

if [[ -z "${sProcessObjId}" ]]
then
    wasdiLog "[ERROR] The variable '\${sProcessObjId}' is empty"
    errorDetected="true"
fi

if [[ -z "${sSessionId}" ]]
then
    wasdiLog "[ERROR] The variable '\${sSessionId}' is empty"
    errorDetected="true"
fi

if [[ "${errorDetected}" == "true" ]]
then
    showHelp 1
fi
#### /CONTROL ####




#### SCRIPT ####
wasdiLog "---- WASDI IDL SCRIPT ----"

wasdiLog "Session ID is: ${sSessionId}"
wasdiLog "[${sProcessObjId}] Process Obj Id is: ${sProcessObjId}"

for currentLoop in $(seq 1 ${iLoopMax})
do
    wasdiLog "-- [${sProcessObjId}] LOOP ${currentLoop}/${iLoopMax} --"
    wasdiLog "[${sProcessObjId}] Control the process status"
    sStatus="$(getStatus "${sProcessObjId}" "${sSessionId}")"

    if [[ "${sStatus}" == "DONE" || "${sStatus}" == "ERROR" || "${sStatus}" == "STOPPED" ]]
    then
        wasdiLog "[${sProcessObjId}] The status is '${sStatus}': we stop now"
        wasdiLog "-- END [${sProcessObjId}] LOOP ${currentLoop}/${iLoopMax} --"
        break
    else
        wasdiLog "[${sProcessObjId}] The status is '${sStatus}': we will execute the IDL script"
    fi

    wasdiLog "[${sProcessObjId}] Executing the IDL script"
    idlScriptStart="$(date +%s)"

    # execute in background to be able to retrieve the PID directly (needed by WASDI)
    umask 000 ; ${sCommandIdl} ${sScriptFile} -args "${sProcessObjId}.config" &
    currentPid=${!}
    echo "${currentPid}" > ${pidFile}
    wasdiLog "[${sProcessObjId}] PID = ${currentPid}"
    wasdiLog "[${sProcessObjId}] Waiting for the PID '${currentPid}' to finish"

    ## loop as long as the process exists
    while [[ true ]]
    do
        ps -p ${currentPid} > /dev/null 2>&1

        if [[ ${?} -ne 0 ]]
        then
            break
        fi

        sleep 1s
    done

    idlScriptEnd="$(date +%s)"
    iDuration=$(( idlScriptEnd - idlScriptStart ))
    wasdiLog "[${sProcessObjId}] Execution tooks ${iDuration} seconds"

    wasdiLog "[${sProcessObjId}] Control the process status"
    sStatus="$(getStatus "${sProcessObjId}" "${sSessionId}")"

    if [[ "${sStatus}" == "RUNNING" ]]
    then
        wasdiLog "[${sProcessObjId}] The status is '${sStatus}': we recheck in 1 minute"
        sleep 1m
    else
        wasdiLog "[${sProcessObjId}] The status is '${sStatus}': we stop the loop"
        wasdiLog "-- END [${sProcessObjId}] LOOP ${currentLoop}/${iLoopMax} --"
        break
    fi

    wasdiLog "-- END [${sProcessObjId}] LOOP ${currentLoop}/${iLoopMax} --"
done

wasdiLog "[${sProcessObjId}] Remove the file '${pidFile}'"

if [[ "$(realpath -m ${pidFile})" != "/" ]]
then
    rm -f ${pidFile}

    if [[ ${?} -eq 0 && ! -f "${pidFile}" ]]
    then
        wasdiLog "[${sProcessObjId}] OK"
    else
        wasdiLog "[${sProcessObjId}] ERROR"
    fi
else
    wasdiLog "[${sProcessObjId}] ERROR WITH THE VARIABLE VALUE"
fi

wasdiLog "[${sProcessObjId}] Remove the file '${sWasdiHome}/${sProcessObjId}.config'"

if [[ "$(realpath -m ${sWasdiHome}/${sProcessObjId}.config)" != "/" ]]
then
    rm -f ${sWasdiHome}/${sProcessObjId}.config

    if [[ ${?} -eq 0 && ! -f "${sWasdiHome}/${sProcessObjId}.config" ]]
    then
        wasdiLog "[${sProcessObjId}] OK"
    else
        wasdiLog "[${sProcessObjId}] ERROR"
    fi
else
    wasdiLog "[${sProcessObjId}] ERROR WITH THE VARIABLE VALUE"
fi

wasdiLog "[${sProcessObjId}] Remove the file '${sWasdiHome}/${sProcessObjId}.params'"

if [[ "$(realpath -m ${sWasdiHome}/${sProcessObjId}.params)" != "/" ]]
then
    rm -f ${sWasdiHome}/${sProcessObjId}.params

    if [[ ${?} -eq 0 && ! -f "${sWasdiHome}/${sProcessObjId}.params" ]]
    then
        wasdiLog "[${sProcessObjId}] OK"
    else
        wasdiLog "[${sProcessObjId}] ERROR"
    fi
else
    wasdiLog "[${sProcessObjId}] ERROR WITH THE VARIABLE VALUE"
fi

wasdiLog "[${sProcessObjId}] After loop, status is now: '${sStatus}'"

# status now must be either DONE or ERROR or STOPPED
sResult="to be initialized"

if [[ "${sStatus}" != "DONE" && "${sStatus}" != "ERROR" && "${sStatus}" != "STOPPED" ]]
then
    wasdiLog "[${sProcessObjId}] Something did not work."
    wasdiLog "[${sProcessObjId}] Forcing status to ERROR"
    sResult="$(setStatusError "${sProcessObjId}" "${sSessionId}")"

    if [[ ${?} -eq 0 ]]
    then
        wasdiLog "[${sProcessObjId}] OK"
    else
        wasdiLog "[${sProcessObjId}] ERROR"
    fi
fi

wasdiLog "---- /WASDI IDL SCRIPT ----"
exit 0
#### /SCRIPT ####
