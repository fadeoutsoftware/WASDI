#!/usr/bin/python3
#-*- coding: utf-8 -*-


import argparse
import hashlib
import inspect
import json
import logging
import os
import shutil
import sys
import traceback

from jinja2 import Template, StrictUndefined




'''
This class is used to format
exceptions received.
'''
class EngineException(object):
    def __init__(self, **kwargs):
        self.oLogging = kwargs['oLogging']

        self.oLogging.error(
            'Script: %(errorScript)s'
            %{
                'errorScript': kwargs['dictError']['inspectError'][0][1]
            }
        )

        self.oLogging.error(
            'Line (approximately): %(errorInLine)s'
            %{
                'errorInLine': kwargs['dictError']['inspectError'][0][2]
            }
        )

        self.oLogging.error(
            'Method: %(errorFunctionName)s'
            %{
                'errorFunctionName': kwargs['dictError']['inspectError'][0][3]
            }
        )

        self.oLogging.error(
            'Type of error: %(errorType)s'
            %{
                'errorType': kwargs['dictError']['sysError'][0]
            }
        )

        self.oLogging.error(
            'Error message: %(errorMessage)s'
            %{
                'errorMessage': kwargs['dictError']['sysError'][1]
            }
        )

        tracebackIndex = 1
        for currentTracebackMessage in traceback.format_tb(kwargs['dictError']['sysError'][2]):
            self.oLogging.error(
                'Traceback - Line %(tracebackIndex)s: %(traceBackMessage)s'
                %{
                    'traceBackMessage': currentTracebackMessage.strip(),
                    'tracebackIndex': tracebackIndex
                }
            )

            tracebackIndex += 1

        if 'iExitCode' in kwargs and kwargs['iExitCode'] >= 0:
            sys.exit(kwargs['iExitCode'])




'''
This class is used to compare the checksum of 2 files.

Parameters mandatory:
    - file1: the first file used in the comparison
    - file2: the second file used in the comparison

Properties available for user:
    - checksum1: the checksum of the file1
    - checksum2: the checksum of the file2
'''
class FileChecksum(object):

    def __init__(self, **kwargs):
        self.file1 = kwargs['file1']
        self.file2 = kwargs['file2']
        self.checksum1 = None
        self.checksum2 = None




    '''
    Calculate the checksum of self.file1 + self.file2 and return:
      - True: if checksums are the same
      - False: if checksums are different
      - None: if one of both file doesn't exist
    '''
    def compare(self):
        try:
            self.checksum1 = self.getChecksumSha256(self.file1)
        except FileNotFoundError:
            self.checksum1 = None

        try:
            self.checksum2 = self.getChecksumSha256(self.file2)
        except FileNotFoundError:
            self.checksum2 = None

        if self.checksum1 == None or self.checksum2 == None:
            return None

        return self.checksum1 == self.checksum2




    '''
    Return the sha256 checksum of the given file
    '''
    def getChecksumSha256(self, file):
        bufferSize = 65536
        currentHashlib = hashlib.sha256()

        with open(file, 'rb') as fileDescriptor:
            while True:
                data = fileDescriptor.read(bufferSize)

                if not data:
                    break

                currentHashlib.update(data)

        return currentHashlib.hexdigest()




'''
This class is used to move files.


Mandatory properties to pass:
    - src: the source file to move
    - dest: the full path of the final location
    (with the name of the target file)


Optionnal properties to pass:
    - overwriteIfDestExists: a boolean to say if we can
    overwrite (or not) the target file if exists (default = False)
    - onlyIfDifference:
        - True: file is moved only if checksums
        between source and target are different
        - False: we don't control the checksums
'''
class FileMove(object):

    def __init__(self, **kwargs):
        self.oLogging = kwargs['oLogging']
        self.src = kwargs['src']
        self.dest = kwargs['dest']
        self.overwriteIfDestExists = kwargs['overwriteIfDestExists'] if 'overwriteIfDestExists' in kwargs else False
        self.onlyIfDifference = kwargs['onlyIfDifference'] if 'onlyIfDifference' in kwargs else False


    def run(self):
        returnSourceDoesntExist = False
        returnTargetExists = False
        returnMoveDone = False


        ## TEST ON SOURCE FILE ##
        self.oLogging.info(
            'Checking if \'%(src)s\' exists'
            %{
                'src': self.src,
            }
        )

        if os.path.exists(self.src):
            self.oLogging.info('OK')
        else:
            self.oLogging.error('The source file does not exist')
            returnSourceDoesntExist = True
            return returnMoveDone, returnSourceDoesntExist, returnTargetExists
        ## /TEST ON SOURCE FILE ##


        ## TEST ON TARGET FILE ##
        self.oLogging.info(
            'Checking if \'%(dest)s\' exists'
            %{
                'dest': self.dest,
            }
        )

        if os.path.exists(self.dest):
            if self.overwriteIfDestExists:
                self.oLogging.info('OK: the target file already exists but I can overwrite it')
            else:
                self.oLogging.error('The target file already exists and I can\'t overwrite it')
                returnTargetExists = True
                return returnMoveDone, returnSourceDoesntExist, returnTargetExists
        else:
            # we disable the checksum analysis
            self.onlyIfDifference = False
            self.oLogging.info('OK: the target file does not exist')
        ## /TEST ON TARGET FILE ##


        ## CONTROL THE DIFFERENCE ##
        checksumIdentical = False

        if self.onlyIfDifference and self.overwriteIfDestExists:
            self.oLogging.info('Calculate the checksum of files')

            objFileChecksum = FileChecksum(
                file1 = self.src,
                file2 = self.dest
            )

            checksumIdentical = objFileChecksum.compare()

            if checksumIdentical == True:
                self.oLogging.info('We have the same checksum')
            else:
                self.oLogging.info('Checksums are different')
        else:
            checksumIdentical = False
        ## /CONTROL THE DIFFERENCE ##


        ## MOVE ##
        self.oLogging.info(
            'Moving \'%(src)s\' to \'%(dest)s\''
            %{
                'src': self.src,
                'dest': self.dest
            }
        )

        if checksumIdentical == False:
            try:
                shutil.move(self.src, self.dest)
                returnMoveDone = True
                self.oLogging.info('OK')
            except:
                EngineException(
                    oLogging = self.oLogging,
                    dictError = {
                        'inspectError': inspect.stack(),
                        'sysError': sys.exc_info()
                    }
                )
        else:
            returnMoveDone = True
            self.oLogging.info('Checksum are the same: the move is skipped')
        ## /MOVE ##


        return returnMoveDone, returnSourceDoesntExist, returnTargetExists




'''
This class is an abstraction for logging.
It is used to manage all log messages.
'''
class Logging(object):
    def __init__(self):
        self.oLogging = logging.getLogger('wasdi-system-resource')
        self.oLogging.setLevel(logging.INFO)

        ## ADD LOG ON SCREEN ##
        oLoggingFormatter = logging.Formatter('%(asctime)s | %(levelname)-8s | %(message)s')

        streamTty = logging.StreamHandler()
        streamTty.setFormatter(oLoggingFormatter)

        self.oLogging.addHandler(streamTty)


    def debug(self, message):
        self.oLogging.debug(message)


    def error(self, message):
        self.oLogging.error(message)


    def info(self, message):
        self.oLogging.info(message)


    def setLevel(self, level):
        self.oLogging.setLevel(level)




'''
This class is the main code to execute
to render a Jinja template.
'''
class renderTemplate(object):
    def __init__(self, **kwargs):
        self.oLogging = kwargs['oLogging']
        self.listCommandLineArguments = [
            {
                'argument': '--json-inline',
                'type': str,
                'required': True,
                'dest': 'dictDataInlineFromCommandLine',
                'action': 'store',
                'help': ' \
                    Datas to use to fill the template. \
                '
            },
            {
                'argument': '--template',
                'type': str,
                'required': True,
                'dest': 'sSourceTemplate',
                'action': 'store',
                'help': ' \
                    Template file to use as source. \
                '
            },
            {
                'argument': '--rendered-file',
                'type': str,
                'required': True,
                'dest': 'sTargetFile',
                'action': 'store',
                'help': ' \
                    Full path to the targeted file when the template will be rendered. \
                '
            },
            {
                'argument': '--strict',
                'required': False,
                'dest': 'bStrictMode',
                'action': 'store_true',
                'help': ' \
                    With this parameter, we enable the strict mode. It means, if a variable is used in a Jinja template but not \
                    given on the command line, we will stop with an error. \
                '
            }
        ]
        self.listCommandLineArgumentParsed = None


    '''
    The main function called by the
    toolbox engine.
    '''
    def execute(self):
        try:
            ## GET CLI ARGS ##
            dictDataInlineFromCommandLine = self.listCommandLineArgumentParsed.dictDataInlineFromCommandLine
            bStrictMode = self.listCommandLineArgumentParsed.bStrictMode
            sTargetFile = self.listCommandLineArgumentParsed.sTargetFile
            sSourceTemplate = self.listCommandLineArgumentParsed.sSourceTemplate
            ## /GET CLI ARGS ##


            ## SCRIPT ##
            self.oLogging.info('Convert the JSON data from the command line into a dictionnary')

            try:
                dataForTemplate = json.loads(dictDataInlineFromCommandLine)
                self.oLogging.info('OK')
            except:
                self.oLogging.error('Unable to convert datas: please verify the given values')

                EngineException(
                    oLogging = self.oLogging,
                    dictError = {
                        'inspectError': inspect.stack(),
                        'sysError': sys.exc_info()
                    },
                    iExitCode = 1
                )

            self.oLogging.info('Determine where the temporary file will be saved')

            try:
                sTemporaryFile = os.path.join('/tmp/', os.path.basename(sTargetFile))
                self.oLogging.info(
                    'OK: %(sTemporaryFile)s'
                    %{
                        'sTemporaryFile': sTemporaryFile
                    }
                )
            except:
                self.oLogging.error('We can\'t continue')

                EngineException(
                    oLogging = self.oLogging,
                    dictError = {
                        'inspectError': inspect.stack(),
                        'sysError': sys.exc_info()
                    },
                    iExitCode = 1
                )

            self.oLogging.info(
                'Render the template \'%(sSourceTemplate)s\' in the temporary file \'%(sTemporaryFile)s\''
                %{
                    'sSourceTemplate': sSourceTemplate,
                    'sTemporaryFile': sTemporaryFile
                }
            )

            try:
                with open(sSourceTemplate) as currentTemplate:
                    with open(sTemporaryFile, 'w') as fdTemporaryFile:
                        if bStrictMode:
                            fdTemporaryFile.write(Template(currentTemplate.read(), undefined=StrictUndefined).render(**dataForTemplate))
                        else:
                            fdTemporaryFile.write(Template(currentTemplate.read()).render(**dataForTemplate))

                self.oLogging.info('OK')
            except FileNotFoundError as oException:
                self.oLogging.error('The template does not exist')
                sys.exit(1)
            except:
                self.oLogging.error('Unable to render the template')

                EngineException(
                    oLogging = self.oLogging,
                    dictError = {
                        'inspectError': inspect.stack(),
                        'sysError': sys.exc_info()
                    },
                    iExitCode = 1
                )


            ## MOVE THE FILE ##
            oFileMove = FileMove(
                oLogging = self.oLogging,
                src = sTemporaryFile,
                dest = sTargetFile,
                overwriteIfDestExists = True,
                onlyIfDifference = True
            )

            returnMoveDone, returnSourceDoesntExist, returnTargetExists = oFileMove.run()
            ## /MOVE THE FILE ##


            self.oLogging.info(
                'Remove \'%(sTemporaryFile)s\''
                %{
                    'sTemporaryFile': sTemporaryFile
                }
            )

            if os.path.exists(sTemporaryFile):
                try:
                    os.remove(sTemporaryFile)
                    self.oLogging.info('OK')
                except:
                    self.oLogging.error('Unable to remove the file')
                    EngineException(
                        oLogging = self.oLogging,
                        dictError = {
                            'inspectError': inspect.stack(),
                            'sysError': sys.exc_info()
                        },
                        iExitCode = 1
                    )
            else:
                self.oLogging.info('The file doesn\'t exist: nothing to do')

            sys.exit(0)
            ## /SCRIPT ##
        except (SystemExit) as currentExitCode:
            sys.exit(currentExitCode)
        except:
            EngineException(
                oLogging = self.oLogging,
                dictError = {
                   'inspectError': inspect.stack(),
                   'sysError': sys.exc_info()
                },
                iExitCode = 1
            )




if __name__ == '__main__':
    '''
    INITIALIZE OBJECTS
    '''
    oLogging = Logging()
    oRenderTemplate = renderTemplate(
        oLogging = oLogging
    )
    oArgumentParser = argparse.ArgumentParser()
    '''
    /INITIALIZE OBJECTS
    '''


    '''
    PARSE ARGUMENTS
    '''
    for dictCurrentArgument in oRenderTemplate.listCommandLineArguments:
        if 'type' not in dictCurrentArgument:
            oArgumentParser.add_argument(
                dictCurrentArgument['argument'],
                required = dictCurrentArgument['required'],
                action = dictCurrentArgument['action'],
                help = dictCurrentArgument['help'],
                dest = dictCurrentArgument['dest']
            )
        else:
            oArgumentParser.add_argument(
                dictCurrentArgument['argument'],
                type = dictCurrentArgument['type'],
                required = dictCurrentArgument['required'],
                action = dictCurrentArgument['action'],
                help = dictCurrentArgument['help'],
                dest = dictCurrentArgument['dest']
            )

    oRenderTemplate.listCommandLineArgumentParsed = oArgumentParser.parse_args()
    '''
    /PARSE ARGUMENTS
    '''



    '''
    RENDER THE TEMPLATE
    '''
    oRenderTemplate.execute()
    '''
    /RENDER THE TEMPLATE
    '''
