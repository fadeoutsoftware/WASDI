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
	print("wasdiProcessorServer Started - ProcId = " + processId, flush=True)

	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))
	os.chdir(dir_path)

	sLocalPath = '/home/appwasdi/application'

	try:
		# Copy updated files from processor folder to the docker
		copy_tree("/wasdi", sLocalPath, update=1)
		print("[" + processId+ "] wasdiProcessorServer: processors files updated", flush=True)

		# It looks impossible to move a file from host to container. So ENVI installation files 
		# are in the processor folder and copied to the docker. After the first run, delete it
		# The launcher will delete the ones in the host folder
		# But in the first run files are still there: here to save space we delete it
		if os.path.exists(sLocalPath + "/envi552-linux.tar"):
			os.remove(sLocalPath + "/envi552-linux.tar")
		if os.path.exists(sLocalPath + "/install.sh"):
			os.remove(sLocalPath + "/install.sh")
		if os.path.exists(sLocalPath + "/o_licenseserverurl.txt"):
			os.remove(sLocalPath + "/o_licenseserverurl.txt")
	except:
		print("[" + processId+ "] wasdiProcessorServer: Unexpected error ", repr(sys.exc_info()[0]), flush=True)

	# Check if this is a help request
	if processId == '--help':
		print("[" + processId+ "] wasdiProcessorServer Help Request: calling processor Help", flush=True)

		sHelp = ""

		#Try to get help from the processor
		try:
			sHelpFileName = ""

			if os.path.isfile("readme.md"):
				sHelpFileName = "readme.md"
			elif os.path.isfile("README.md"):
				sHelpFileName = "README.md"
			elif os.path.isfile("README.MD"):
				sHelpFileName = "README.MD"
			elif os.path.isfile("readme.MD"):
				sHelpFileName = "readme.MD"
			elif os.path.isfile("help.md"):
				sHelpFileName = "help.md"
			elif os.path.isfile("help.MD"):
				sHelpFileName = "help.MD"
			elif os.path.isfile("HELP.MD"):
				sHelpFileName = "HELP.MD"
			if os.path.isfile("readme.txt"):
				sHelpFileName = "readme.txt"
			elif os.path.isfile("README.txt"):
				sHelpFileName = "README.txt"
			elif os.path.isfile("README.TXT"):
				sHelpFileName = "README.TXT"
			elif os.path.isfile("readme.TXT"):
				sHelpFileName = "readme.TXT"
			elif os.path.isfile("help.txt"):
				sHelpFileName = "help.txt"
			elif os.path.isfile("help.TXT"):
				sHelpFileName = "help.TXT"
			elif os.path.isfile("HELP.TXT"):
				sHelpFileName = "HELP.TXT"

			if not sHelpFileName == "":
				with open(sHelpFileName, 'r') as oHelpFile:
					sHelp = oHelpFile.read()

		except AttributeError:
			print("[" + processId+ "] wasdiProcessorServer Help not available")
			sHelp = "No help available. Just try."

		# Return the available help
		return jsonify({'help': sHelp})

	# Check if this is a lib update request
	if processId == '--wasdiupdate':
		#Try to update the lib
		try:
			print("[" + processId+ "] Copy updated lib", flush=True)
			#oProcess = subprocess.Popen(["pip", "install", "--upgrade", "wasdi"])
			#print("pip upgrade done")
		except Exception as oEx:
			print("[" + processId+ "] wasdi.executeProcessor EXCEPTION", flush=True)
			print("[" + processId+ "] " + repr(oEx), flush=True)
			print("[" + processId+ "] " + traceback.format_exc(), flush=True)
		except:
			print("[" + processId+ "] wasdi.executeProcessor generic EXCEPTION", flush=True)

		# Return the result of the update
		return jsonify({'update': '1'})	

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

	#Force User Session Workspace and myProcId from the Query Params
	if (request.args.get('user') is not None):
		parameters['user'] = request.args.get('user')
	else:
		print("[" + processId+ "] USER arg not available", flush=True)

	if (request.args.get('sessionid') is not None):
		parameters['sessionid'] = request.args.get('sessionid')
	else:
		print("[" + processId+ "] SESSION arg not available", flush=True)

	if (request.args.get('workspaceid') is not None):
		parameters['workspaceid'] = request.args.get('workspaceid')
		print("[" + processId + "] WORKSPACE arg " + request.args.get('workspaceid') , flush=True)
	else:
		print("[" + processId+ "] WORKSPACE arg not available", flush=True)

	#Try to get the user
	try:
		sUser = parameters['user']
		wasdi.setUser(sUser)
		print("[" + processId+ "] wasdiProcessorServer User available in params. Got " + sUser, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer user not available in parameters.", flush=True)

	#Try to get the password
	try:
		sPassword = parameters['password']
		wasdi.setPassword(sPassword)
		print("[" + processId+ "] wasdiProcessorServer Pw available in params", flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer password not available in parameters.", flush=True)

	#Try to get the session id
	try:
		sSessionId = parameters['sessionid']
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

	#Try to get the base url
	try:
		sBaseUrl = parameters['baseurl']
		wasdi.setBaseUrl(sBaseUrl)
		print("[" + processId+ "] wasdiProcessorServer Base Url in params " + sBaseUrl, flush=True)
	except:
		print("[" + processId+ "] wasdiProcessorServer Using default base url", flush=True)

	#Try to get the workspace id
	sWorkspaceId = ""
	try:
		sWorkspaceId = parameters['workspaceid']
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

	wasdi.openWorkspaceById(sWorkspaceId)
	#Run the processor
	try:
		sConfigFilePath = sLocalPath + processId + '.config'
		sParamFilePath = sLocalPath +  processId + '.params'

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
		oConfigFile.write('BASEURL=' + wasdi.getBaseUrl() + '\r\n')
		oConfigFile.close()

		#Write Params:
		oParamsFile = open(sParamFilePath, 'w+')
		for sKey in parameters:
			oParamsFile.write(sKey + '=' + str(parameters[sKey]) + '\r\n')
		oParamsFile.close()

		oProcess = subprocess.Popen([os.path.join(sLocalPath, 'runProcessor.sh'), '--session-id', sSessionId, '--process-object-id', processId, '--pid-file', '/tmp/pid_idl_' + processId])
		wasdi.wasdiLog('wasdiProcessorServer Process Started with local pid %(runProcessorProcessPid)s' %{'runProcessorProcessPid': str(oProcess.pid)})

		idlProcessPid = None
		wasdi.wasdiLog('Get the PID of the real IDL process')
		getPidOutput = subprocess.run([os.path.join(sLocalPath, 'getPid.sh'), '--pid-file', '/tmp/pid_idl_' + processId, '--wait-pid-file', '--wait-timeout', '300'], stdout=subprocess.PIPE)
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
