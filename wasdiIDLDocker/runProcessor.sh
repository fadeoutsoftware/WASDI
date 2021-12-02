#!/bin/bash
script_file="/home/wasdi/call_idl.pro"
echo "executing WASDI idl script..."
i=0
sStatus="RUNNING"
# read session ID from config.properties
sSessionId=`awk -F"=" '$1=="SESSIONID"{print $2}' config.properties`
sSessionId=`echo $sSessionId | sed 's/\\r//g'`
echo "Session ID is: "$sSessionId
sProcessObjId=`awk -F"=" '$1=="MYPROCID"{print $2}' config.properties`
sProcessObjId=`echo $sProcessObjId | sed 's/\\r//g'`
echo "Process Obj Id is: "$sProcessObjId
iDuration=-1
while (( i < 5 ))
do
	sStatus=$(curl -k --location --request GET "https://172.17.0.1/wasdiwebserver/rest/process/getstatusbyid?processObjId=${sProcessObjId}" --header "x-session-token: ${sSessionId}")
	if [[ "$sStatus" == "DONE" ]] || [[ "$sStatus" == "ERROR" ]] || [[ "$sStatus" == "STOPPED" ]]
	then
		break
	fi
	echo "[${sProcessObjId}] Trying to launch script: "$i
	start=`date +%s`
	umask 000; /usr/local/bin/idl ${script_file}
	end=`date +%s`
	iDuration=$((end-start))
	echo "[${sProcessObjId}] Execution apparently took "$iDuration" seconds"
	((i++))
	#echo "Getting proces status"
	# get status
	sStatus=$(curl -k --location --request GET "https://172.17.0.1/wasdiwebserver/rest/process/getstatusbyid?processObjId=${sProcessObjId}" --header "x-session-token: ${sSessionId}")
	if [[ "RUNNING" == $sStatus ]]
	then
		echo "[${sProcessObjId}] Retrying after 1 m sleep"
		sleep 1m
	else
		#echo "Done"
		break
	fi
done
echo "[${sProcessObjId}] After loop, status is now "$sStatus
# status now must be either DONE or ERROR or STOPPED
sResult='to be initialized'
if [ "$sStatus" != "DONE" ] && [ "$sStatus" != "ERROR" ] && [ "$sStatus" != "STOPPED" ]
then
	echo "[${sProcessObjId}] Something did not work, forcing status to ERROR, sorry"
	# todo force status = ERROR
	sResult=$(curl -k --location --request GET "https://172.17.0.1/wasdiwebserver/rest/process/updatebyid?sProcessId=${sProcessObjId}&status=ERROR&perc=-1" --header "x-session-token: ${sSessionId}")
	#echo $sResult
else
	echo "[${sProcessObjId}] IDL Processor done!"
fi

