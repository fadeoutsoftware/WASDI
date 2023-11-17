#!flask/bin/python

from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import json
import subprocess
import traceback
from distutils.dir_util import copy_tree
from os.path import sys

app = Flask(__name__)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	print("wasdiProcessorServer Started v.2.1.2 - ProcId = " + processId, flush=True)

	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))
	os.chdir(dir_path)

	sLocalPath = '/home/appwasdi/application'

	# Check if this is a lib update request
	if processId.startswith('--kill'):
		#Try to update the lib
		try:
			asKillParts = processId.split("_")

			#TODO safety check
			print("[" + processId+ "] Killing subprocess", flush=True)
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			print("[" + processId+ "] Subprocess killed", flush=True)
		except Exception as oEx:
			print("[" + processId+ "] wasdi.executeProcessor EXCEPTION", flush=True)
			print("[" + processId+ "] " + repr(oEx), flush=True)
			print("[" + processId+ "] " + traceback.format_exc(), flush=True)
		except:
			print("[" + processId+ "] wasdi.executeProcessor generic EXCEPTION", flush=True)

		# Return the result of the update
		return jsonify({'kill': '1'})

	print("[" + processId+ "] wasdiProcessorServer run request", flush=True)

	# This is not a help request but a run request.
	# Copy request json in the parameters array
	parameters = request.json

	#Try to get the user
	try:
		sUser = request.args.get('user')
		wasdi.setUser(sUser)
		print("[" + processId+ "] wasdiProcessorServer User available in params. Got " + sUser, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer user not available in parameters.", flush=True)

	#Try to get the session id
	try:
		sSessionId = request.args.get('sessionid')
		wasdi.setSessionId(sSessionId)
		print("[" + processId+ "] wasdiProcessorServer Session available in params " + sSessionId, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer Session not available in parameters.", flush=True)

	#Try to set the proc id
	try:
		wasdi.setProcId(processId)
		print("[" + processId+ "] wasdiProcessorServer set Proc Id " + processId, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer Proc Id not available", flush=True)

	#Try to get the workspace id
	sWorkspaceId = ""
	try:
		sWorkspaceId = request.args.get('workspaceid')
		wasdi.setActiveWorkspaceId(sWorkspaceId)
		print("[" + processId + "] wasdiProcessorServer got Workspace Id " + sWorkspaceId, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer Workspace Id not available in parameters.", flush=True)


	#Init Wasdi
	print("[" + processId+ "] wasdiProcessorServer: init waspy lib", flush=True)
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)

	if not wasdi.init():
		print("[" + processId+ "] wasdiProcessorServer: init FAILED", flush=True)
		return jsonify({'processId': 'ERROR', 'processorEngineVersion':'2'})

	print("[" + processId + "] wasdiProcessorServer: opening workspace", flush=True)
	wasdi.openWorkspaceById(sWorkspaceId)
	#Run the processor
	try:
		sConfigFilePath = sLocalPath + '/' + processId + '.config'
		sParamFilePath = sLocalPath +  '/' + processId + '.params'

		print("[" + processId + "] wasdiProcessorServer: creating the config file " + sConfigFilePath, flush=True)
		# Write Config file:
		oConfigFile = open(sConfigFilePath, 'w+')
		oConfigFile.write('')

		oConfigFile.write('BASEPATH=' + wasdi.getBasePath() + '\r\n')
		oConfigFile.write('USER=' + sUser + '\r\n')
		oConfigFile.write('WORKSPACEID=' + sWorkspaceId + '\r\n')
		oConfigFile.write('SESSIONID=' + sSessionId + '\r\n')
		oConfigFile.write('ISONSERVER=1' + '\r\n')
		oConfigFile.write('DOWNLOADACTIVE=0\r\n')
		oConfigFile.write('UPLOADACTIVE=0\r\n')
		oConfigFile.write('VERBOSE=0\r\n')
		oConfigFile.write('PARAMETERSFILEPATH=' + sParamFilePath + '\r\n')
		oConfigFile.write('MYPROCID=' + processId + '\r\n')
		sBaseUrl = wasdi.getBaseUrl()

		# To idl we need to pass only the name of the server
		asUrlParts = sBaseUrl.split("/")
		if asUrlParts is not None:
			if len(asUrlParts)>=3:
				sBaseUrl = asUrlParts[2]

		oConfigFile.write('BASEURL=' + sBaseUrl + '\r\n')
		oConfigFile.close()

		print("[" + processId + "] wasdiProcessorServer: creating the paramas file "+ sParamFilePath, flush=True)

		#Write Params:
		oParamsFile = open(sParamFilePath, 'w+')
		for sKey in parameters:
			oParamsFile.write(sKey + '=' + str(parameters[sKey]) + '\r\n')
		oParamsFile.close()

		oProcess = subprocess.Popen(['bash', os.path.join(sLocalPath, 'runProcessor.sh'), '--session-id', sSessionId, '--process-object-id', processId, '--pid-file', '/tmp/pid_idl_' + processId])
		wasdi.wasdiLog('wasdiProcessorServer Process Started with local pid %(runProcessorProcessPid)s' %{'runProcessorProcessPid': str(oProcess.pid)})

		idlProcessPid = None
		wasdi.wasdiLog('Get the PID of the real IDL process')
		getPidOutput = subprocess.run(['bash', os.path.join(sLocalPath, 'getPid.sh'), '--pid-file', '/tmp/pid_idl_' + processId, '--wait-pid-file', '--wait-timeout', '300'], stdout=subprocess.PIPE)
		idlProcessPid = getPidOutput.stdout.decode('UTF-8')
		wasdi.wasdiLog('PID: %(idlProcessPid)s' %{'idlProcessPid': idlProcessPid})

		if idlProcessPid is not None:
			#Update the server with the subprocess pid
			wasdi.setSubPid(processId, int(idlProcessPid))

	except Exception as oEx:
		wasdi.wasdiLog("wasdiProcessorServer EXCEPTION")
		wasdi.wasdiLog(repr(oEx))
		wasdi.updateProcessStatus(processId, "ERROR", 100)

	return jsonify({'processId': processId, 'processorEngineVersion':'2'})

@app.route('/hello', methods=['GET'])
def hello():
	print("wasdiProcessoServer Hello request")
	return jsonify({'hello': 'hello waspi'})

if __name__ == '__main__':
	app.run(host='0.0.0.0', debug=False, use_reloader=False)
