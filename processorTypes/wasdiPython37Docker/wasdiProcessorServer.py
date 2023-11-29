#!flask/bin/python
from flask import Flask
from flask import jsonify
from flask import request
import os
import wasdi
import json
import urllib.parse
import re
import subprocess
import traceback
from distutils.dir_util import copy_tree

app = Flask(__name__)

m_sProcId = ""

def log(sLogString):
	print("[" + m_sProcId + "] wasdiProcessorServer PythonPip3.7 local build - Engine v.2.0.0 - " + sLogString)

@app.route('/run/<string:processId>', methods=['POST'])
def run(processId):
	global m_sProcId
	m_sProcId = processId

	log("Started")

	# First of all be sure to be in the right path
	dir_path = os.path.dirname(os.path.realpath(__file__))
	os.chdir(dir_path)

	log("Processor folder set")

	try:
		# Copy updated files from processor folder to the docker
		copy_tree("/wasdi", "/home/wasdi", update=1)
		log("processors files updated")
	except:
		log("error while copying\n" + traceback.format_exc())

	# Check if this is a lib update request
	if processId == '--wasdiupdate':
		# Try to update the lib
		try:
			log("Calling pip upgrade")
			oProcess = subprocess.Popen(["pip", "install", "--upgrade", "wasdi"])
			log("pip upgrade done")
		except Exception as oEx:
			log('EXCEPTION:')
			log(repr(oEx))
			log(traceback.format_exc())
		except:
			# todo catch BaseException or something
			log("generic EXCEPTION while updating\n" + traceback.format_exc())

		# Return the result of the update
		return jsonify({'update': '1'})

	# Check if this is a lib update request
	if processId.startswith('--kill'):
		# Try to update the lib
		try:
			asKillParts = processId.split("_")

			# TODO safety check or something
			log("Killing subprocess")
			oProcess = subprocess.Popen(["kill", "-9", asKillParts[1]])
			log("Subprocess killed")
		except Exception as oEx:
			log('EXCEPTION:')
			log(repr(oEx))
			log(traceback.format_exc())
		except:
			# todo catch BaseException or something
			log("generic EXCEPTION while killing\n" + traceback.format_exc())

		# Return the result of the update
		return jsonify({'kill': '1'})

	print("[" + processId + "] wasdiProcessorServer run request")

	# This is not a help request but a run request.

	# Copy request json in the parameters array
	log("Run request")

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

	# Run the processor
	try:
		wasdi.wasdiLog("wasdiProcessorServer RUN " + processId)

		# Run the processor in a separate process
		sStringParams = json.dumps(aoParameters)

		sEncodeParameters = urllib.parse.quote(sStringParams, safe='')
		oProcess = subprocess.Popen(["python", "wasdiProcessorExecutor.py", sEncodeParameters, processId])

		wasdi.wasdiLog("wasdiProcessorServer Process Started with local pid " + str(oProcess.pid))
		# Update the server with the subprocess pid
		wasdi.setSubPid(processId, int(oProcess.pid))

	except Exception as oEx:
		wasdi.wasdiLog(f"wasdiProcessorServer EXCEPTION: {type(oEx)}: {oEx}")
		wasdi.wasdiLog(repr(oEx))
		wasdi.updateProcessStatus(processId, "ERROR", 100)

	return jsonify({'processId': processId, 'processorEngineVersion': '2'})


@app.route('/hello', methods=['GET'])
def hello():
	print("wasdiProcessoServer Hello request")
	return jsonify({'hello': 'hello waspi'})


@app.route('/packageManager/listPackages/', defaults={'flag': ''})
@app.route('/packageManager/listPackages/<flag>/')
def pm_list_packages(flag: str):
	print('/packageManager/listPackages/' + flag)

	command: str = 'pip list'
	if flag != '':
		command = command + ' -' + flag

	output: str = __execute_pip_command_and_get_output(command)
	dependencies: list = __parse_list_command_output(output)

	return json.dumps(dependencies), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/getPackage/<name>/')
def pm_get_package(name: str):
	print('/packageManager/getPackage/' + name)

	command: str = 'pip show ' + name

	output: str = __execute_pip_command_and_get_output(command)
	info: dict = __parse_show_command_output(output)

	if 'Name' in output \
			and name in output:
		return json.dumps(info), 200, {'Content-Type': 'application/json'}
	else:
		return json.dumps(info), 404, {'Content-Type': 'application/json'}


@app.route('/packageManager/executeCommand', methods=['POST'])
def pm_execute_command():
	command: str = request.form.get('command')
	print('/packageManager/executeCommand | command: ' + command)

	output: str = __execute_pip_command_and_get_output(command)

	return json.dumps({'output': output}), 200, {'Content-Type': 'application/json'}


@app.route('/packageManager/addPackage/<name>/', defaults={'version': ''})
@app.route('/packageManager/addPackage/<name>/<version>/')
def pm_add_package(name: str, version: str):
	print('/packageManager/addPackage/' + name + '/' + version)

	command: str = 'pip install ' + name
	if version != '':
		command = command + '==' + version

	output: str = __execute_pip_command_and_get_output(command)

	if 'Successfully' in output:
		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
	else:
		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'}


@app.route('/packageManager/upgradePackage/<name>/', defaults={'version': ''})
@app.route('/packageManager/upgradePackage/<name>/<version>/')
def pm_upgrade_package(name: str, version: str):
	print('/packageManager/upgradePackage/' + name + '/' + version)

	command: str = 'pip install -U ' + name
	if version != '':
		command = command + '==' + version

	output: str = __execute_pip_command_and_get_output(command)

	if 'Successfully' in output:
		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
	else:
		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'}


@app.route('/packageManager/removePackage/<name>/')
def pm_remove_package(name: str):
	print('/packageManager/removePackage/' + name)

	command: str = 'pip uninstall ' + name + ' -y'

	output: str = __execute_pip_command_and_get_output(command)

	if 'Successfully' in output:
		return json.dumps({'success': output}), 200, {'Content-Type': 'application/json'}
	else:
		return json.dumps({'error': output}), 409, {'Content-Type': 'application/json'}


@app.route('/packageManager/packageVersions/<name>/')
def pm_package_versions(name: str):
	print('/packageManager/packageVersions/' + name)

	command: str = 'pip -V'

	version: dict = __get_version(command)

	version_major: int = int(version['major'])
	version_minor: int = int(version['minor'])

	new_command: str = 'pip'
	if version_major >= 21:
		if version_minor >= 2:
			new_command = 'pip index versions ' + name
		elif version_minor >= 1:
			new_command = 'pip install ' + name + '== '
		else:
			new_command = 'pip install --use-deprecated=legacy-resolver ' + name + '=='
	elif version_major >= 20:
		if version_minor >= 3:
			new_command = 'pip install --use-deprecated=legacy-resolver ' + name + '=='
		else:
			new_command = 'pip install ' + name + '== '
	elif version_major >= 9:
		new_command = 'pip install ' + name + '== '

	output: str = __execute_pip_command_and_get_output(new_command)
	versions_string: str = __extract_versions_from_output(output)
	versions_list: list = versions_string.split(', ')

	if 'none' not in output:
		return json.dumps(versions_list), 200, {'Content-Type': 'application/json'}
	else:
		return json.dumps(versions_list), 404, {'Content-Type': 'application/json'}


@app.route('/packageManager/managerVersion/')
def pm_manager_version():
	print('/packageManager/managerVersion/')

	command: str = 'pip -V'

	version: dict = __get_version(command)

	return json.dumps(version), 200, {'Content-Type': 'application/json'}


def __execute_pip_command_and_get_output(command: str) -> str:
	print('__execute_pip_command_and_get_output: ' + command)

	oPipProcess = subprocess.run(command + ' > tmp', shell=True, capture_output=True)

	sOutput = open('tmp', 'r').read()
	os.remove('tmp')

	stderr: str = oPipProcess.stderr.decode("utf-8")

	if stderr != '':
		if sOutput == '':
			sOutput = stderr
		else:
			sOutput += stderr

	return sOutput


def __get_version(command: str) -> dict:
	print('__get_version')
	output: str = __execute_pip_command_and_get_output(command)
	return __version_string_2_dictionary(__extract_version_from_output(output))


def __extract_version_from_output(output: str) -> str:
	start: str = 'pip '
	end: str = ' from '

	return __extract_substring_limited_by(output, start, end)


def __extract_versions_from_output(output: str) -> str:
	start: str = '\\(from versions: '
	end: str = '\\)'

	return __extract_substring_limited_by(output, start, end)


def __extract_substring_limited_by(full_string: str, start: str, end: str) -> str:
	return re.search('%s(.*)%s' % (start, end), full_string).group(1)


def __version_string_2_dictionary(version: str) -> dict:
	asVersion: list = version.split('.')
	oVersion: dict = {
		"name": "pip",
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

	sHeader: str = asLines[0]
	asHeaders: list = sHeader.split()

	for i in range(len(asHeaders)):
		asHeaders[i] = asHeaders[i].lower()

	aoDependencies: list = []

	for sLine in asLines[2:]:
		asColumns = sLine.split()

		if len(asHeaders) == 2:
			aoDependencies.append({
				"manager": "pip",
				asHeaders[0]: asColumns[0],
				asHeaders[1]: asColumns[1]})
		elif len(asHeaders) == 4:
			aoDependencies.append({
				"manager": "pip",
				asHeaders[0]: asColumns[0],
				asHeaders[1]: asColumns[1],
				asHeaders[2]: asColumns[2],
				asHeaders[3]: asColumns[3]})

	return aoDependencies


if __name__ == '__main__':
	app.run(host='0.0.0.0', debug=False, use_reloader=False)
