#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import subprocess
import traceback

app = Flask(__name__)

m_sProcId = ""

def log(sLogString):
	print("[" + m_sProcId + "] wasdiProcessorServer Java17 Engine v.2.0.0 - " + sLogString)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	global m_sProcId
	m_sProcId = processId

	log("Started")
	
	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))	
	os.chdir(dir_path)

	sLocalPath = '/home/appwasdi/application'

	log("Processor folder set")

	# Check if this is a lib update request
	if processId.startswith('--kill'):
		#Try to update the lib
		try:
			
			asKillParts = processId.split("_")
			
			#TODO safety check
			print("[" + processId+ "] Killing subprocess")
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			print("[" + processId+ "] Subprocess killed")
		except Exception as oEx:
			print("[" + processId+ "] wasdi.executeProcessor EXCEPTION")
			print(repr(oEx))
			print(traceback.format_exc())
		except:
			print("[" + processId+ "] wasdi.executeProcessor generic EXCEPTION")			
		
		# Return the result of the update
		return jsonify({'kill': '1'})		
	
	log("Run request")
	
	# This is not a help request but a run request.
	
	# Copy request json in the parameters array
	aoParameters = request.json
	
	# Try to get the user
	try:
		sUser = request.args.get('user')
		wasdi.setUser(sUser)
		aoParameters['user'] = sUser
		log("User available in params. Got " + sUser)
	except Exception as oE:
		log("Exception reading user: " + str(repr(oE)))
	except:
		# todo catch BaseException or something
		log("user not available in parameters.\n" + traceback.format_exc())

	# Try to get the session id
	try:
		sSessionId = request.args.get('sessionid')
		wasdi.setSessionId(sSessionId)
		aoParameters['sessionid'] = sSessionId
		log("Session available in params " + sSessionId)
	except Exception as oE:
		log("Exception reading session Id: " + str(repr(oE)))
	except:
		# todo catch BaseException or something
		log("Session not available in parameters.\n" + traceback.format_exc())

	# Try to set the proc id
	try:
		wasdi.setProcId(processId)
		log("set Proc Id " + processId)
	except Exception as oE:
		log("Exception reading proc Id: " + str(repr(oE)))
	except:
		log("Proc Id not available")

	# Try to get the workspace id
	sWorkspaceId = ""
	try:
		sWorkspaceId = request.args.get('workspaceid')
		wasdi.setActiveWorkspaceId(sWorkspaceId)
		aoParameters['workspaceid'] = sWorkspaceId
		log("Workspace Id available in params " + sWorkspaceId)
	except:
		log("Workspace Id not available in parameters.")

	# Init Wasdi
	log("Call init waspy lib")
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)

	if not wasdi.init():
		log("wasdiProcessorServer: init FAILED")
		return jsonify({'processId': 'ERROR', 'processorEngineVersion': '2'})

	log("opening workspace " + sWorkspaceId)
	wasdi.openWorkspaceById(sWorkspaceId)
	
	#Run the processor
	try:
		sConfigFilePath = sLocalPath + "/config.properties"
		sParamFilePath = sLocalPath + "/param.properties"
		
		# Write Config file:
		oConfigFile = open(sConfigFilePath, "w+")
		oConfigFile.write("")
		
		oConfigFile.write("BASEPATH=" + wasdi.getBasePath() +"\r\n")
		oConfigFile.write("USER=" + sUser+"\r\n")
		oConfigFile.write("WORKSPACEID=" + sWorkspaceId +"\r\n")
		oConfigFile.write("SESSIONID="+sSessionId+"\r\n")
		oConfigFile.write("ISONSERVER=1"+"\r\n")
		oConfigFile.write("DOWNLOADACTIVE=0"+"\r\n")
		oConfigFile.write("UPLOADACTIVE=0"+"\r\n")
		oConfigFile.write("VERBOSE=0"+"\r\n")
		oConfigFile.write("PARAMETERSFILEPATH=" + sParamFilePath+"\r\n")
		oConfigFile.write("MYPROCID="+ processId +"\r\n")
		sBaseUrl = wasdi.getBaseUrl()
		oConfigFile.write('BASEURL=' + sBaseUrl + '\r\n')

		oConfigFile.close()
		
		
		#Write Params:
		oParamsFile = open(sParamFilePath, "w+")
		for sKey in aoParameters:
			oParamsFile.write(sKey+"=" + str(aoParameters[sKey])+"\r\n")
		oParamsFile.close()
		
		oProcess = subprocess.Popen(["java -jar", "myProcessor.jar", sConfigFilePath])
		
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