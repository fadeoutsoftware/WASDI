#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import json
import urllib.parse
import subprocess
import traceback
from distutils.dir_util import copy_tree
from os.path import sys

app = Flask(__name__)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	
	print("wasdiProcessorServer Started - ProcId = " + processId)
	
	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))	
	os.chdir(dir_path)
	print("wasdiProcessorServer: processor folder set")
	
	try:
		# Copy updated files from processor folder to the docker
		copy_tree("/wasdi", "/home/wasdi", update=1)
		print("wasdiProcessorServer: processors files updated")
	except:
		print("wasdiProcessorServer: Unexpected error ", repr(sys.exc_info()[0]))
	
	# Check if this is a help request
	if processId == '--help':
		
		print("wasdiProcessorServer Help Request: calling processor Help")
		
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
			print("wasdiProcessorServer Help not available")
			sHelp = "No help available. Just try."
		
		print("wasdiProcessorServer return received help " + sHelp)
		# Return the available help			
		return jsonify({'help': sHelp})
	
	# Check if this is a lib update request
	if processId == '--wasdiupdate':
		#Try to update the lib
		try:
			
			print("Copy updated lib")
			#oProcess = subprocess.Popen(["pip", "install", "--upgrade", "wasdi"])
			#print("pip upgrade done")
		except Exception as oEx:
			print("wasdi.executeProcessor EXCEPTION")
			print(repr(oEx))
			print(traceback.format_exc())
		except:
			print("wasdi.executeProcessor generic EXCEPTION")			
		
		# Return the result of the update
		return jsonify({'update': '1'})	
	
	# Check if this is a lib update request
	if processId.startswith('--kill'):
		#Try to update the lib
		try:
			
			asKillParts = processId.split("_")
			
			#TODO safety check
			print("Killing subprocess")
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			print("Subprocess killed")
		except Exception as oEx:
			print("wasdi.executeProcessor EXCEPTION")
			print(repr(oEx))
			print(traceback.format_exc())
		except:
			print("wasdi.executeProcessor generic EXCEPTION")			
		
		# Return the result of the update
		return jsonify({'kill': '1'})		
	
	print("wasdiProcessorServer run request")
	
	# This is not a help request but a run request.
	
	# Copy request json in the parameters array
	parameters = request.json
	
		
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


	#Try to get the base url
	try:
		sBaseUrl = parameters['baseurl']
		wasdi.setBaseUrl(sBaseUrl)
		print("wasdiProcessorServer Base Url in params " + sBaseUrl)
	except:
		print('wasdiProcessorServer Using default base url')		

	
	#Init Wasdi
	print("wasdiProcessorServer: init waspy lib")
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)
	
	if wasdi.init() == False:
		print("wasdiProcessorServer: init FAILED")
		return jsonify({'processId': 'ERROR', 'processorEngineVersion':'2'})
	
	#Run the processor
	try:
		wasdi.wasdiLog("wasdiProcessorServer RUN " + processId)
		
		sLocalPath = "/home/wasdi/"
		sConfigFilePath = sLocalPath + "config.properties"
		sParamFilePath = sLocalPath + "param.properties"
		
		# Write Config file:
		oConfigFile = open(sConfigFilePath, "w+")
		oConfigFile.write("")
		
		oConfigFile.write("BASEPATH=" + wasdi.getBasePath() +"\r\n");
		oConfigFile.write("USER=" + sUser+"\r\n");
		oConfigFile.write("WORKSPACEID=" + sWorkspaceId +"\r\n");
		oConfigFile.write("SESSIONID="+sSessionId+"\r\n");
		oConfigFile.write("ISONSERVER=1"+"\r\n");
		oConfigFile.write("DOWNLOADACTIVE=0"+"\r\n");
		oConfigFile.write("UPLOADACTIVE=0"+"\r\n");
		oConfigFile.write("VERBOSE=0"+"\r\n");
		oConfigFile.write("PARAMETERSFILEPATH=" + sParamFilePath+"\r\n");
		oConfigFile.write("MYPROCID="+ processId +"\r\n");
		oConfigFile.close()
		
		
		#Write Params:
		oParamsFile = open(sParamFilePath, "w+")
		for sKey in parameters:
			oParamsFile.write(sKey+"=" + str(parameters[sKey])+"\r\n")
		oParamsFile.close()
		
		oProcess = subprocess.Popen(["octave", "myProcessor.m", sConfigFilePath])
		
		wasdi.wasdiLog("wasdiProcessorServer Process Started with local pid "  + str(oProcess.pid))
		#Update the server with the subprocess pid
		wasdi.setSubPid(processId, int(oProcess.pid))
		
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