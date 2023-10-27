#!/bin/bash

# This script is used to delete all files:
#   - in a specified directory
#   - which match a specified pattern
#   - keeping the last X files (X is a number you have to specify)
#
# Example:
#   - I have this:
#     - /tmp/myExample01.txt
#     - /tmp/myExample02.txt
#     - /tmp/myExample03.txt
#     - /tmp/myExample04.txt
#     - /tmp/myExample05.txt
#   - I want to keep the 3 most recent file
#   - I execute the script like this: bash ${0} '/tmp' 'myExample??.txt' 3
#   -> the script will delete /tmp/myExample01.txt and /tmp/myExample02.txt


function showHelp() {
    echo "Usage: bash ${0} <directory> <file pattern> <number of instance to keep|default = 3>"
    echo
    echo "Example:"
    echo "# bash ${0} {{ sWasdiLauncherRootDirectoryPath }} {{ sWasdiLauncherTargetFileName }}.?????????? 5"
}

sDirectory="${1}"
sFilePattern="${2}"
iNumberOfInstanceToKeepTemporary=${3}

if [[ -z "${sDirectory}" ]]
then
    echo "[ERROR] The directory was not specified."
    errorDetected=1
fi

if [[ -z "${sFilePattern}" ]]
then
    echo "[ERROR] The file pattern was not specified."
    errorDetected=1
fi

if [[ -n "${sDirectory}" && ! -d "${sDirectory}" ]]
then
    echo "[INFO] The directory '${sDirectory}' doesn't exists: nothing to do"
    errorDetected=1
fi

if [[ -z "${iNumberOfInstanceToKeepTemporary}" ]]
then
    iNumberOfInstanceToKeepTemporary=3
fi

if [[ ${iNumberOfInstanceToKeepTemporary} -le 0 ]]
then
    echo "[ERROR] 'numberOfInstanceToKeep' must be a positive number"
    errorDetected=1
fi

iNumberOfInstanceToKeep=$(( ${iNumberOfInstanceToKeepTemporary} + 1 ))

if [[ "${errorDetected}" == "1" ]]
then
    echo
    echo
    showHelp
    exit 1
fi

echo "[INFO] Search how many file match the pattern '${sDirectory}/${sFilePattern}'..."
iNumberOfFile="$(ls -1A ${sDirectory}/${sFilePattern} 2> /dev/null | wc -l)"
echo -e "\t-> we found ${iNumberOfFile} files"

echo
echo "[INFO] Compare the number of file that match the pattern and the number of file to keep..."

if [[ ${iNumberOfFile} -lt ${iNumberOfInstanceToKeep} ]]
then
    echo -e "\t-> we want to keep ${iNumberOfInstanceToKeepTemporary} files: we don't have enough files"
    exit 0
else
    echo -e "\t-> OK"
fi

echo
echo "[INFO] List all file that match the pattern..."
ls -1tp ${sDirectory}/${sFilePattern}

echo
echo "[INFO] Remove file keeping the last ${iNumberOfInstanceToKeepTemporary} files..."
ls -1tp ${sDirectory}/${sFilePattern} | tail -n+${iNumberOfInstanceToKeep} | while read currentFile
do
    echo -e "\t- ${currentFile}"
    rm -f "${currentFile}"

    if [[ ${?} -eq 0 && ! -f "${currentFile}" ]]
    then
        echo -e "\t\t-> OK"
    else
        echo -e "\t\t-> ERROR"
    fi
done
