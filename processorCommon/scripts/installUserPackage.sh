#!/bin/bash

# MAINTAINER: WASDI SARL
# VERSION: 1.4


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
    echo -e "\t- octave package: you have to provide '--package-octave' on the command line"
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
    local sFileToParse="${1}"
    # sed 1: replace line that contains only space by an empty line
    # grep 1: remove empty line
    echo "$(cat ${sFileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | wc -l)"
    return 0
}

function getGreatestValue() {
    local aValues=( "${@}" )
    local sCurrentValue
    local sValueToReturn=0

    for sCurrentValue in "${aValues[@]}"
    do
        if [[ "${sCurrentValue}" -gt ${sValueToReturn} ]]
        then
            sValueToReturn=${sCurrentValue}
        fi
    done

    return ${sValueToReturn}
}

function installPackageApt() {
    local sFileToParse="${sApplicationDirectory}/packages.txt"
    local iReturnCode=0

    echo "[INFO] Check if the file '${sFileToParse}' exists..."

    if [[ -f "${sFileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Convert the file in the Unix format..."
    dos2unix ${sFileToParse}
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi

    echo "[INFO] Check if the file '${sFileToParse}' contains something..."
    iNumberOfLine=$(getNumberOfLine "${sFileToParse}")

    if [[ ${iNumberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${iNumberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Update the local APT cache..."
    apt-get update
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi

    echo "[INFO] Install package..."
    apt-get install --assume-yes --no-install-recommends $(cat ${sFileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | tr "\n" " ")
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi
}

function cleanCacheApt() {
    if [[ -d "/var/lib/apt/lists/" ]]
    then
        rm --force --recursive /var/lib/apt/lists/*
        return ${?}
    fi

    return 0
}

function installPackageConda() {
    local sFileToParse="${sApplicationDirectory}/env.yml"
    local iReturnCode=0

    echo "[INFO] Check if the file '${sFileToParse}' exists..."

    if [[ -f "${sFileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Convert the file in the Unix format..."
    dos2unix ${sFileToParse}
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi

    echo "[INFO] Check if the file '${sFileToParse}' contains something..."
    iNumberOfLine=$(getNumberOfLine "${sFileToParse}")

    if [[ ${iNumberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${iNumberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install package..."
    conda env update --quiet --file ${sFileToParse}
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi
}

function cleanCacheConda() {
    conda clean --all --force-pkgs-dirs --yes
    return ${?}
}

function installPackagePip() {
    local sFileToParse="${sApplicationDirectory}/pip.txt"
    local iReturnCode=0

    echo "[INFO] Check if the file '${sFileToParse}' exists..."

    if [[ -f "${sFileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Convert the file in the Unix format..."
    dos2unix ${sFileToParse}
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi

    echo "[INFO] Check if the file '${sFileToParse}' contains something..."
    iNumberOfLine=$(getNumberOfLine "${sFileToParse}")

    if [[ ${iNumberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${iNumberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install package..."
    pip3 install --no-cache-dir --no-compile $(cat ${sFileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | tr "\n" " ")
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi
}

function cleanCachePip() {
    find ${sHomeDirectory} -xdev -type d -name __pycache__ -exec rm --recursive --force {} +
    return ${?}
}

function installPackageOctave() {
    local sFileToParse="${sApplicationDirectory}/octave-packages.txt"
    local iReturnCode=0

    echo "[INFO] Check if the file '${sFileToParse}' exists..."

    if [[ -f "${sFileToParse}" ]]
    then
        echo -e "[INFO] The file exists: we continue"
    else
        echo -e "[INFO] The file doesn't exists: we stop now"
        return 0
    fi

    echo "[INFO] Convert the file in the Unix format..."
    dos2unix ${sFileToParse}
    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
    fi

    echo "[INFO] Check if the file '${sFileToParse}' contains something..."
    iNumberOfLine=$(getNumberOfLine "${sFileToParse}")

    if [[ ${iNumberOfLine} -gt 0 ]]
    then
        echo -e "[INFO] There is ${iNumberOfLine} line so we continue"
    else
        echo -e "[INFO] There is no line: we stop now"
        return 0
    fi

    echo "[INFO] Install packages..."
    # octave --eval \"pkg install -nodeps -forge $(cat ${sFileToParse} | sed 's/^[ \t]*$//' | grep -vE "^$" | tr "\n" " ")\"
    # going from list installation to list of eval 
    cat ${sFileToParse} | grep -vE "([^;]*;){2,}[^;]*" | grep -vE "^$|^#" | while read sCurrentLine
    do
        echo "Executing command octave --eval \"pkg install ${sCurrentLine}\""
        octave --eval \"pkg install ${sCurrentLine}\"
    done

    iReturnCode=${?}

    if [[ ${iReturnCode} -eq 0 ]]
    then
        echo -e "[INFO] OK"
        return 0
    else
        echo -e "[ERROR] Please analyse the command output"
        return ${iReturnCode}
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

        --application-directory)
            shift

            if [[ -n "${1}" ]]
            then
                sApplicationDirectory="${1}"
            fi

            shift
        ;;

        --home-directory)
            shift

            if [[ -n "${1}" ]]
            then
                sHomeDirectory="${1}"
            fi

            shift
        ;;

        --package-apt)
            sInstallUserPackageApt="true"
            shift
        ;;

        --package-conda)
            sInstallUserPackageConda="true"
            shift
        ;;

        --package-pip)
            sInstallUserPackagePip="true"
            shift
        ;;

        --package-octave)
            sInstallUserPackageOctave="true"
            shift
        ;;

        --failure-is-ok)
            sFailureIsOk="true"
            shift
        ;;
    esac
done
#### ARGUMENT MANAGEMENT ####




#### CONTROL ####
if [[ -z "${sInstallUserPackageApt}" && -z "${sInstallUserPackageConda}" && -z "${sInstallUserPackagePip}" ]]
then
    echo "[INFO] You didn't specificy which package to install so we stop now."
    exit 0
fi

if [[ -z "${sApplicationDirectory}" ]]
then
    echo "[ERROR] Please specify the application directory in which user files are copied."
    errorDetected="true"
fi

if [[ -n "${sApplicationDirectory}" && ! -d "${sApplicationDirectory}" ]]
then
    echo "[INFO] The directory '${sApplicationDirectory}' doesn't exists."
    errorDetected="true"
fi

if [[ -z "${sHomeDirectory}" ]]
then
    echo "[ERROR] Please specify the home directory in which we have everything (application, virtual environment, etc)."
    errorDetected="true"
fi

if [[ -n "${sHomeDirectory}" && ! -d "${sHomeDirectory}" ]]
then
    echo "[INFO] The directory '${sHomeDirectory}' doesn't exists."
    errorDetected="true"
fi

if [[ "${errorDetected,,}" == "true" ]]
then
    showHelp 1
fi
#### /CONTROL ####




#### SCRIPT ####
if [[ "${sInstallUserPackageApt,,}" == "true" ]]
then
    echo "==== APT ===="
    installPackageApt
    iReturnCodeInstallPackageApt=${?}

    cleanCacheApt

    if [[ "${sFailureIsOk}" == "true" ]]
    then
        iReturnCodeInstallPackageApt=0
    fi

    echo "==== /APT ===="
fi

if [[ "${sInstallUserPackageConda,,}" == "true" ]]
then
    echo "==== CONDA ===="
    installPackageConda
    iReturnCodeInstallPackageConda=${?}

    cleanCacheConda

    if [[ "${sFailureIsOk}" == "true" ]]
    then
        iReturnCodeInstallPackageConda=0
    fi

    echo "==== /CONDA ===="
fi

if [[ "${sInstallUserPackagePip,,}" == "true" ]]
then
    echo "==== PIP ===="
    installPackagePip
    iReturnCodeInstallPackagePip=${?}

    cleanCachePip

    if [[ "${sFailureIsOk}" == "true" ]]
    then
        iReturnCodeInstallPackagePip=0
    fi

    echo "==== /PIP ===="
fi

if [[ "${sInstallUserPackageOctave,,}" == "true" ]]
then
    echo "==== OCTAVE ===="
    installPackageOctave
    iReturnCodeInstallPackageOctave=${?}

    if [[ "${sFailureIsOk}" == "true" ]]
    then
        iReturnCodeInstallPackageOctave=0
    fi

    echo "==== /OCTAVE ===="
fi


iReturnCodeList=(
    "${iReturnCodeInstallPackageApt}"
    "${iReturnCodeInstallPackageConda}"
    "${iReturnCodeInstallPackagePip}"
    "${iReturnCodeInstallPackageOctave}"
)
getGreatestValue "${iReturnCodeList[@]}"
exit ${?}
#### /SCRIPT ####
