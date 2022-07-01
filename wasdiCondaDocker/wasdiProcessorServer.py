#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import json
import urllib.parse
import re
import traceback
import subprocess
from distutils.dir_util import copy_tree
from os.path import sys

app = Flask(__name__)


@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):

	print("wasdiProcessorServer Started - ProcId = " + processId)

	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))
	os.chdir(dir_path)

	try:
		# Copy updated files from processor folder to the docker
		copy_tree("/wasdi", "/home/wasdi", update=1)
		#print("wasdiProcessorServer: processors files updated")
	except:
		print("[" + processId+ "] wasdiProcessorServer: Unexpected error Updating Files: ", repr(sys.exc_info()[0]))

	# Check if this is a help request
	if processId == '--help':

		print("[" + processId+ "] wasdiProcessorServer Help Request: calling processor Help")

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

			sHelp = ""
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
			print("[" + processId+ "] Calling pip upgrade")
			oProcess = subprocess.Popen(["pip", "install", "--upgrade", "wasdi"])
			print("[" + processId+ "] pip upgrade done")
		except Exception as oEx:
			print(f'[{processId}] wasdi.executeProcessor EXCEPTION: {type(oEx)}: {oEx}')
			print(repr(oEx))
			print(traceback.format_exc())
		except:
			# todo catch BaseException or something
			print("[" + processId+ "] wasdi.executeProcessor generic EXCEPTION while updating\n" + traceback.format_exc())

		# Return the result of the update
		return jsonify({'update': '1'})


	# Check if this is a lib update request
	if processId.startswith('--kill'):
		#Try to update the lib
		try:
			asKillParts = processId.split("_")

			#TODO safety check or something
			print("[" + processId+ "] Killing subprocess")
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			print("[" + processId+ "] Subprocess killed")
		except Exception as oEx:
			print(f'[{processId}] wasdi.executeProcessor EXCEPTION ({type(oEx)}: {oEx})')
			print("[" + processId+ "] " + repr(oEx))
			print("[" + processId+ "] " + traceback.format_exc())
		except:
			# todo catch BaseException or something
			print("[" + processId + "] wasdi.executeProcessor generic EXCEPTION while killing\n" + traceback.format_exc())

		# Return the result of the update
		return jsonify({'kill': '1'})

	print("[" + processId+ "] wasdiProcessorServer run request")

	# This is not a help request but a run request.

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
			print("[" + processId+ "] wasdiProcessorServer no Embedded Params available")

		print("[" + processId+ "] wasdiProcessorServer Added Embedded Params")
	except:
		print("[" + processId+ "] wasdiProcessorServer Error in reading params.json")

	#Force User Session Workspace and myProcId from the Query Params
	if (request.args.get('user') is not None):
		parameters['user'] = request.args.get('user')
	else:
		print("[" + processId+ "] USER arg not available")

	if (request.args.get('sessionid') is not None):
		parameters['sessionid'] = request.args.get('sessionid')
	else:
		print("[" + processId+ "] SESSION arg not available")

	if (request.args.get('workspaceid') is not None):
		parameters['workspaceid'] = request.args.get('workspaceid')
	else:
		print("[" + processId+ "] WORKSPACE arg not available")

	#Try to get the user
	try:
		sUser = parameters['user']
		wasdi.setUser(sUser)
		print("[" + processId+ "] wasdiProcessorServer User available in params. Got " + sUser)
	except:
		print("[" + processId+ "] wasdiProcessorServer user not available in parameters.")

	#Try to get the password
	try:
		sPassword = parameters['password']
		wasdi.setPassword(sPassword)
		print("[" + processId+ "] wasdiProcessorServer Pw available in params")
	except:
		print("[" + processId+ "] wasdiProcessorServer password not available in parameters.")

	#Try to get the session id
	try:
		sSessionId = parameters['sessionid']
		wasdi.setSessionId(sSessionId)
		print("[" + processId+ "] wasdiProcessorServer Session available in params " + sSessionId)
	except:
		print("[" + processId+ "] wasdiProcessorServer Session not available in parameters.")

	#Try to set the proc id
	try:
		wasdi.setProcId(processId)
		#print("[" + processId+ "] wasdiProcessorServer set Proc Id " + processId)
	except:
		print("[" + processId+ "] wasdiProcessorServer Proc Id not available")

	#Try to get the workspace id
	try:
		sWorkspaceId = parameters['workspaceid']
		wasdi.openWorkspaceById(sWorkspaceId)
		print("[" + processId+ "] wasdiProcessorServer Workspace Id available in params " + sWorkspaceId)
	except:
		print("[" + processId+ "] wasdiProcessorServer Workspace Id not available in parameters.")

	#Init Wasdi
	#print("wasdiProcessorServer: init waspy lib")
	wasdi.setIsOnServer(True)
	wasdi.setDownloadActive(False)

	if wasdi.init() == False:
		print("[" + processId+ "] wasdiProcessorServer: init FAILED")

	#Run the processor
	try:
		wasdi.wasdiLog("wasdiProcessorServer RUN " + processId)

		#Run the processor in a separate process
		sStringParams = json.dumps(parameters)

		sEncodeParameters = urllib.parse.quote(sStringParams, safe='')
		oProcess = subprocess.Popen(["python", "wasdiProcessorExecutor.py", sEncodeParameters, processId])

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


@app.route('/packageManager/listPackages/')
def pm_list_packages():
	print('/packageManager/listPackages/')

	command: str = 'conda list'

	output: str = __execute_conda_command_and_get_output(command)
	dependencies: list = __parse_list_command_output(output)

	return json.dumps(dependencies), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/getPackage/<name>/')
def pm_get_package(name: str):
	print('/packageManager/getPackage/' + name)

	command: str = 'conda list ' + name

	output: str = __execute_conda_command_and_get_output(command)

	if 'Name' in output \
			and name in output:

		dependencies: list = __parse_list_command_output(output)

		if len(dependencies) > 0:
			info: dict = dependencies[0]

			return json.dumps(info), 200, {'Content-Type': 'application/json'}

	return json.dumps({'error': 'Not found'}), 404, {'Content-Type': 'application/json'}


@app.route('/packageManager/addPackage/<name>/', defaults={'version': ''})
@app.route('/packageManager/addPackage/<name>/<version>/')
def pm_add_package(name: str, version: str):
	print('/packageManager/addPackage/' + name + '/' + version)

	command: str = 'conda install ' + name
	if version != '':
		command = command + '=' + version

	output: str = __execute_conda_command_and_get_output(command)

#	if 'Successfully' in output:
#		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
#	else:
#		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'})

	return json.dumps({'output': output}), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/upgradePackage/<name>/', defaults={'version': ''})
@app.route('/packageManager/upgradePackage/<name>/<version>/')
def pm_upgrade_package(name: str, version: str):
	print('/packageManager/upgradePackage/' + name + '/' + version)

	command: str = 'conda install ' + name
	if version != '':
		command = command + '=' + version

	output: str = __execute_conda_command_and_get_output(command)

#	if 'Successfully' in output:
#		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
#	else:
#		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'})

	return json.dumps({'output': output}), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/removePackage/<name>/')
def pm_remove_package(name: str):
	print('/packageManager/removePackage/' + name)

	command: str = 'conda remove ' + name

	output: str = __execute_conda_command_and_get_output(command)

#	if 'Successfully' in output:
#		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
#	else:
#		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'})

	return json.dumps({'output': output}), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/packageVersions/<name>/')
def pm_package_versions(name: str):
	print('/packageManager/packageVersions/' + name)

	return json.dumps({'output': 'Not Implemented'}), 501, {'Content-Type': 'application/json'}


@app.route('/packageManager/managerVersion/')
def pm_manager_version():
	print('/packageManager/managerVersion/')

	command: str = 'conda -V'

	version: dict = __get_version(command)

	return json.dumps(version), 200, {'Content-Type': 'application/json'}


def __execute_conda_command_and_get_output(command: str) -> str:
	print('__execute_conda_command_and_get_output: ' + command)

	cpe = subprocess.run(command + ' > tmp', shell=True, capture_output=True)

	sOutput = open('tmp', 'r').read()
	os.remove('tmp')

	stderr: str = cpe.stderr.decode("utf-8")

	if stderr != '':
		if sOutput == '':
			sOutput = stderr
		else:
			sOutput += stderr

	return sOutput


def __get_version(command: str) -> dict:
	print('__get_version')
	output: str = __execute_conda_command_and_get_output(command)
	return __version_string_2_dictionary(__extract_version_from_output(output))


def __extract_version_from_output(output: str) -> str:
	result: str = output.splitlines()[0].split('conda ', 1)[1]

	return result


def __extract_versions_from_output(output: str) -> str:
	start: str = '\\(from versions: '
	end: str = '\\)'

	result: str = __extract_substring_limited_by(output, start, end)

	return result


def __extract_substring_limited_by(full_string: str, start: str, end: str) -> str:
	return re.search('%s(.*)%s' % (start, end), full_string).group(1)


def __version_string_2_dictionary(version: str) -> dict:
	asVersion: list = version.split('.')
	oVersion: dict = {
		"version": version,
		"major": asVersion[0],
		"minor": asVersion[1],
		"patch": asVersion[2]
	}

	return oVersion


def __parse_show_command_output(output: str) -> dict:
	info: dict = {}

	asLines: list = output.splitlines()

	for sLine in asLines:
		asTokens: list = sLine.split(': ')
		info[asTokens[0]] = asTokens[1]

	return info


def __parse_list_command_output(output: str) -> list:
	asLines: list = output.splitlines()

	sHeader: str = asLines[2]
	sHeader = sHeader.replace('#', '')
	asHeaders: list = sHeader.split()

	for i in range(len(asHeaders)):
		asHeaders[i] = asHeaders[i].lower()

	aoDependencies: list = []

	for sLine in asLines[3:]:
		asColumns = sLine.split()

		if len(asColumns) == 3:
			aoDependencies.append({
				asHeaders[0]: asColumns[0],
				asHeaders[1]: asColumns[1],
				asHeaders[2]: asColumns[2]})
		elif len(asColumns) == 4:
			aoDependencies.append({
				asHeaders[0]: asColumns[0],
				asHeaders[1]: asColumns[1],
				asHeaders[2]: asColumns[2],
				asHeaders[3]: asColumns[3]})

	return aoDependencies


if __name__ == '__main__':
	app.run(host='0.0.0.0', debug=False, use_reloader=False)
