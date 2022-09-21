#!/bin/bash

## SET PARAMETER ##
sCommandAwk="awk"
sCommandBash="bash"
sCommandDocker="docker"
sCommandDockerCompose="docker-compose"
sCommandEcho="echo"
sCommandRealpath="realpath"
sCommandTr="tr"

${sCommandEcho} "[INFO] Searching the directory of the container..."
sContainerDirectory="$(${sCommandRealpath} -e $(dirname ${0})/..)"

if [[ -n "${sContainerDirectory}" ]]
then
    ${sCommandEcho} "[INFO] OK"
else
    ${sCommandEcho} "[ERROR] Unable to find the directory"
    exit 1
fi

${sCommandEcho} "[INFO] Loading general variable"

if [[ -f "${sContainerDirectory}/var/general_common.env" ]]
then
    source ${sContainerDirectory}/var/general_common.env
    ${sCommandEcho} "[INFO] OK"
else
    ${sCommandEcho} "[ERROR] The file doesn't exist"
fi

${sCommandEcho} "[INFO] Loading bash variable"

if [[ -f "${sContainerDirectory}/var/bash_common.env" ]]
then
    source ${sContainerDirectory}/var/bash_common.env
    ${sCommandEcho} "[INFO] OK"
else
    ${sCommandEcho} "[ERROR] The file doesn't exist"
fi

sScriptDockerBuild="${sContainerDirectory}/tool/dockerBuild.sh"
sScriptDockerPush="${sContainerDirectory}/tool/dockerPush.sh"
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


## SCRIPT ##
${sCommandEcho} "[INFO] Searching if we have containers started..."
listRunningContainer="$(${sCommandDocker} ps | ${sCommandAwk} -v sContainerName="${sContainerName}:" '( $2 ~ sContainerName ) { print $NF }' | ${sCommandTr} "\n" " ")"

if [[ -n "${listRunningContainer}" ]]
then
    ${sCommandEcho} "[INFO] OK: we have running containers"
else
    ${sCommandEcho} "[INFO] OK: there is no container started"
    exit 0
fi

${sCommandEcho} "[INFO] Building the new image '${sContainerName}:${sContainerVersion}'..."
${sCommandBash} ${sScriptDockerBuild}

if [[ ${?} -eq 0 ]]
then
    ${sCommandEcho} "[INFO] OK"
else
    ${sCommandEcho} "[ERROR] Unable to build the image"
    exit 1
fi

${sCommandEcho} "[INFO] Publishing the new image '${sContainerName}:${sContainerVersion}'..."
${sCommandBash} ${sScriptDockerPush}

if [[ ${?} -eq 0 ]]
then
    ${sCommandEcho} "[INFO] OK"
else
    ${sCommandEcho} "[ERROR] Unable to publish the image"
    exit 1
fi

for sCurrentRunningContainer in ${listRunningContainer}
do
    ${sCommandEcho} "[INFO] [${sCurrentRunningContainer}] Searching the notebook ID..."
    sCurrentNotebookId="$(${sCommandEcho} ${sCurrentRunningContainer} | sed 's/nb_//g')"

    if [[ -n "${sCurrentNotebookId}" ]]
    then
        ${sCommandEcho} "[INFO] [${sCurrentRunningContainer}] OK: ${sCurrentNotebookId}"
    else
        ${sCommandEcho} "[ERROR] [${sCurrentRunningContainer}] Unable to find the notebook ID: we bypass this container"
        continue
    fi

    ${sCommandEcho} "[INFO] [${sCurrentRunningContainer}] Updating the container..."
    ${sCommandDockerCompose} --file ${sContainerDirectory}/docker-compose_${sCurrentNotebookId}.yml --project-name \"${sCurrentNotebookId}\" --env-file ${sContainerDirectory}/var/general_common.env up --detach

    if [[ ${?} -eq 0 ]]
    then
        ${sCommandEcho} "[INFO] [${sCurrentRunningContainer}] OK"
    else
        ${sCommandEcho} "[ERROR] [${sCurrentRunningContainer}] Unable to update the container"
        continue
    fi
done
## /SCRIPT ##
