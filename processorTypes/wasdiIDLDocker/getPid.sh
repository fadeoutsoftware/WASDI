#!/bin/bash

#### FUNCTION ####
function showHelp() {
    local _exitCode="${1}"

    echo
    echo "------"
    echo "Usage: /bin/bash ${0} --pid-file <path to the PID file> [--wait-pid-file] [--wait-timeout <duration in seconds>]"
    echo
    echo "If not specified, the default wait timeout is 60 seconds"

    if [[ -n "${_exitCode}" ]]
    then
        exit ${_exitCode}
    fi
}
#### /FUNCTION ####




#### ARGUMENT MANAGEMENT ####
while test ${#} -gt 0
do
    case "${1}" in
        -h|--help)
            showHelp 0
        ;;

        -p|--pid-file)
            shift

            if [[ -n "${1}" ]]
            then
                pidFile="${1}"
            fi

            shift
        ;;

        --wait-pid-file)
            shift
            waitForPidFile="true"
        ;;

        --wait-timeout)
            shift

            if [[ -n "${1}" ]]
            then
                waitTimeout="${1}"
            fi

            shift
        ;;
    esac
done
#### /ARGUMENT MANAGEMENT ####




#### DEFAULT VALUE ####
if [[ -z "${waitTimeout}" ]]
then
    waitTimeout=60
fi


if [[ -z "${waitForPidFile}" ]]
then
    waitForPidFile="false"
fi
#### /DEFAULT VALUE ####




#### CONTROL ####
if [[ -z "${pidFile}" ]]
then
    echo "[ERROR] The PID file to monitor must be precised."
    echo
    showHelp 1
fi

numberRegex='^[1-9][0-9]{0,}$'

if ! [[ ${waitTimeout} =~ ${numberRegex} ]]
then
    echo "[ERROR] The wait timeout must be a positive integer."
    echo
    showHelp 1
fi
#### /CONTROL ####




#### PARAMETER ####
startDate="$(date +%s)"
#### PARAMETER ####




#### SCRIPT ####
while [[ true ]]
do
    if [[ -f "${pidFile}" ]]
    then
        cat ${pidFile}
        exit 0
    else
        # if we have to wait, we wait for the timeout duration
        if [[ "${waitForPidFile}" == "true" ]]
        then
            currentDate="$(date +%s)"
            startDateTimeout=$(( ${startDate} + ${waitTimeout} ))

            # if the timeout is expired, we exit now
            if [[ ${currentDate} -gt ${startDateTimeout} ]]
            then
                exit 1
            fi
        # if we don't have to wait, we exit directly
        else
            exit 1
        fi
    fi

    sleep 1s
done

exit 0
#### /SCRIPT ####
