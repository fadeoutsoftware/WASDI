#!/bin/bash

#### FUNCTION ####
function showHelp() {
    local _exitCode="${1}"

    echo
    echo "------"
    echo "This script install user packages."
    echo "It can install:"
    echo -e "\t- system package for Debian / Ubuntu: you have to provide '--package-apt' on the command line"
    echo -e "\t- conda package: you have to provide '--package-conda' on the command line"
    echo -e "\t- pip package: you have to provide '--package-pip' on the command line"
    echo
    echo "Usage: /bin/bash ${0} --home-directory <directory> [--package-apt] [--package-conda] [--package-pip] [--failure-is-ok]"
    echo
    echo "Return code:"
    echo -e "\t- if no --package-* argument are provided: the script exit with a return code 0"
    echo -e "\t- if at least one --package-* argument is provided:"
    echo -e "\t\t- if the package file to read is missing: the script exit with a return code 0"
    echo -e "\t\t- if the pakcage file to read is present, the script read it and tries to install packages:"
    echo -e "\t\t\t- in case of success, it exit with a return code 0"
    echo -e "\t\t\t- in case of failure:"
    echo -e "\t\t\t\t- if --failure-is-ok is provided: it exit with a return code 0"
    echo -e "\t\t\t\t- if --failure-is-ok is not provided: it exit with the return code of the command which failed"

    if [[ -n "${_exitCode}" ]]
    then
        exit ${_exitCode}
    fi
}

function getNumberOfLine() {
    local fileToParse="${1}"
    # sed 1: replace line that contains only space by an empty line
    # grep 1: remove empty line
    echo "$(cat ${fileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | wc -l)"
    return 0
}

function getGreatestValue() {
    local valueList=( "${@}" )
    local currentValue
    local valueToReturn=0

    for currentValue in "${valueList[@]}"
    do
        if [[ "${currentValue}" -gt ${valueToReturn} ]]
        then
            valueToReturn=${currentValue}
        fi
    done

    return ${valueToReturn}
}

function installPackageApt() {
    local fileToParse="${homeDirectory}/packages.txt"
    local returnCode=0

    echo "[INFO] Check if the file '${fileToParse}' exists..."

    if [[ -f "${fileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Check if the file '${fileToParse}' contains something..."
    numberOfLine=$(getNumberOfLine "${fileToParse}")

    if [[ ${numberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${numberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install package..."
    apt-get install --assume-yes --no-install-recommends $(cat ${fileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | tr "\n" " ")
    returnCode=${?}

    if [[ ${returnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the installation output"
        return ${returnCode}
    fi
}

function installPackageConda() {
    local fileToParse="${homeDirectory}/env.yml"
    local returnCode=0

    echo "[INFO] Check if the file '${fileToParse}' exists..."

    if [[ -f "${fileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Check if the file '${fileToParse}' contains something..."
    numberOfLine=$(getNumberOfLine "${fileToParse}")

    if [[ ${numberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${numberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install package..."
    conda env update --file ${fileToParse}
    returnCode=${?}

    if [[ ${returnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the installation output"
        return ${returnCode}
    fi
}

function installPackagePip() {
    local fileToParse="${homeDirectory}/pip.txt"
    local returnCode=0

    echo "[INFO] Check if the file '${fileToParse}' exists..."

    if [[ -f "${fileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Check if the file '${fileToParse}' contains something..."
    numberOfLine=$(getNumberOfLine "${fileToParse}")

    if [[ ${numberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${numberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install package..."
    pip3 install $(cat ${fileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | tr "\n" " ")
    returnCode=${?}

    if [[ ${returnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the installation output"
        return ${returnCode}
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

        --home-directory)
            shift

            if [[ -n "${1}" ]]
            then
                homeDirectory="${1}"
            fi

            shift
        ;;

        --package-apt)
            installUserPackageApt="true"
            shift
        ;;

        --package-conda)
            installUserPackageConda="true"
            shift
        ;;

        --package-pip)
            installUserPackagePip="true"
            shift
        ;;

        --failure-is-ok)
            failureIsOk="true"
            shift
        ;;
    esac
done
#### ARGUMENT MANAGEMENT ####




#### CONTROL ####
if [[ -z "${installUserPackageApt}" && -z "${installUserPackageConda}" && -z "${installUserPackagePip}" ]]
then
    echo "[INFO] You didn't specificy which package to install so we stop now."
    exit 0
fi

if [[ -z "${homeDirectory}" ]]
then
    echo "[ERROR] Please specify the home directory in which user files are copied."
    errorDetected="true"
fi

if [[ -n "${homeDirectory}" && ! -d "${homeDirectory}" ]]
then
    echo "[INFO] The directory '${homeDirectory}' doesn't exists."
    errorDetected="true"
fi

if [[ "${errorDetected,,}" == "true" ]]
then
    showHelp 1
fi
#### /CONTROL ####




#### SCRIPT ####
if [[ "${installUserPackageApt,,}" == "true" ]]
then
    echo "==== APT ===="
    installPackageApt
    returnCodeApt=${?}

    if [[ "${failureIsOk}" == "true" ]]
    then
        returnCodeApt=0
    fi

    echo "==== /APT ===="
fi

if [[ "${installUserPackageConda,,}" == "true" ]]
then
    echo "==== CONDA ===="
    installPackageConda
    returnCodeConda=${?}

    if [[ "${failureIsOk}" == "true" ]]
    then
        returnCodeConda=0
    fi

    echo "==== /CONDA ===="
fi

if [[ "${installUserPackagePip,,}" == "true" ]]
then
    echo "==== PIP ===="
    installPackagePip
    returnCodePip=${?}

    if [[ "${failureIsOk}" == "true" ]]
    then
        returnCodePip=0
    fi

    echo "==== /PIP ===="
fi


returnCodeList=(
    "${returnCodeApt}"
    "${returnCodeConda}"
    "${returnCodePip}"
)
getGreatestValue "${returnCodeList[@]}"
exit ${?}
#### /SCRIPT ####
