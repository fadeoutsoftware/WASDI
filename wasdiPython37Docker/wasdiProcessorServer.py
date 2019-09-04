#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import myProcessor
import json
import threading
import urllib.parse
import subprocess

app = Flask(__name__)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	
	print("wasdiProcessorServer Started - ProcId = " + processId)
	
	# Check if this is a help request
	if processId == '--help':
		
		print("wasdiProcessorServer Help Request: calling procesor Help")
		
		#Try to get help from the processor
		try:
			sHelp = myProcessor.WasdiHelp()
		except AttributeError:
			print("wasdiProcessorServer Help not available")
			sHelp = "No help available. Just try."
		
		print("wasdiProcessorServer return received help " + sHelp)
		# Return the available help			
		return jsonify({'help': sHelp})
	
	print("wasdiProcessorServer run request")
	
	# This is not a help request but a run request. First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))	
	os.chdir(dir_path)
	
	print("wasdiProcessorServer: processor folder set")
	
	# Copy request json in the parameters array
	parameters = request.json
			
	#Add, if present, embedded params
	try:
		if (os.path.isfile('params.json')):
			with open('params.json') as oFile:
				oEmbeddedParams = json.load(oFile)
				
				for sKey in oEmbeddedParams:
					if (not (sKey in parameters)):
						parameters[sKey] = oEmbeddedParams[sKey] 
		else:			
			print("wasdiProcessorServer no Embedded Params available")
			
		print("wasdiProcessorServer Added Embedded Params")
	except:
		print('wasdiProcessorServer Error in reading params.json')
	
	#Force User Session Workspace and myProcId from the Query Params
	if (request.args.get('user') is not None):
		parameters['user'] = request.args.get('user')
	else:
		print('USER arg not available')

	if (request.args.get('sessionid') is not None):
		parameters['sessionid'] = request.args.get('sessionid')
	else:
		print('SESSION arg not available')
		
	if (request.args.get('workspaceid') is not None):
		parameters['workspaceid'] = request.args.get('workspaceid')
	else:
		print('WORKSPACE arg not available')
	
	#Try to get the user
	try:
		sUser = parameters['user']
		wasdi.setUser(sUser)
		print("wasdiProcessorServer User available in params. Got " + sUser)
	except:
		print('wasdiProcessorServer user not available in parameters.')
		
	#Try to get the password
	try:
		sPassword = parameters['password']
		wasdi.setPassword(sPassword)
		print("wasdiProcessorServer Pw available in params")
	except:
		print('wasdiProcessorServer password not available in parameters.')
		
	#Try to get the session id
	try:
		sSessionId = parameters['sessionid']
		wasdi.setSessionId(sSessionId)
		print("wasdiProcessorServer Session available in params " + sSessionId)
	except:
		print('wasdiProcessorServer Session not available in parameters.')		
	
	#Try to set the proc id
	try:
		wasdi.setProcId(processId)
		print("wasdiProcessorServer set Proc Id " + processId)
	except:
		print('wasdiProcessorServer Proc Id not available')
		
	#Try to get the workspace id
	try:
		sWorkspaceId = parameters['workspaceid']
		wasdi.openWorkspaceById(sWorkspaceId)
		print("wasdiProcessorServer Workspace Id available in params " + sWorkspaceId)
	except:
		print('wasdiProcessorServer Workspace Id not available in parameters.')		
	
	#Init Wasdi
	print("wasdiProcessorServer: init waspy lib")
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)
	
	if wasdi.init() == False:
		print("wasdiProcessorServer: init FAILED")
	
	#Run the processor
	try:
		wasdi.wasdiLog("wasdiProcessorServer RUN " + processId)
				
		#Run the processor in a separate process
		sStringParams = json.dumps(parameters)
		
		sEncodeParameters = urllib.parse.quote(sStringParams, safe='')
		oProcess = subprocess.Popen(["python", "wasdiProcessorExecutor.py", sEncodeParameters, processId])
		
		wasdi.wasdiLog("wasdiProcessorServer Process Started with local pid "  + str(oProcess.pid))
		
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