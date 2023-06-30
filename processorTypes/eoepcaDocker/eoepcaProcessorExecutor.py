'''
Created on 24 Nov 2022

@author: p.campanella
'''

import json
import os
import requests
import shutil
import sys
import traceback
import urllib.parse
import wasdi
import zipfile

from libs.WasdiLogging import WasdiLogging
'''
DISABLE FOR THE MOMENT
from libs.WasdiS3 import WasdiS3
'''


def _unzip(sAttachmentName, sPath):
    '''
    Unzips a file

    :param sAttachmentName: filename to unzip

    :param sPath: both the path where the file is and where it must be unzipped
    :return: None
    '''
    wasdi.wasdiLog('[INFO] waspy._unzip( ' + sAttachmentName + ', ' + sPath + ' )')
    if sPath is None:
        print('[ERROR] waspy._unzip: path is None' +
              '  ******************************************************************************')
        return
    if sAttachmentName is None:
        print('[ERROR] waspy._unzip: attachment to unzip is None' +
              '  ******************************************************************************')
        return

    try:
        sZipFilePath = os.path.join(sPath, sAttachmentName)
        zip_ref = zipfile.ZipFile(sZipFilePath, 'r')
        zip_ref.extractall(sPath)
        zip_ref.close()
    except:
        print('[ERROR] waspy._unzip: failed unzipping' +
              '  ******************************************************************************')

    return

def _getStandardHeaders():
    """
    Get the standard headers for a WASDI API Call, setting also the session token

    :return: dictionary of headers to add to the REST API
    """
    asHeaders = {'Content-Type': 'application/json', 'x-session-token': wasdi.getSessionId()}
    return asHeaders


def _downloadProcessor(sFileName):
    """
    Download a file from WASDI

    :param sFileName: file to download
    :return: None
    """

    wasdi.wasdiLog('[INFO] waspy._downloadProcessor( ' + sFileName + ' )')

    asHeaders = _getStandardHeaders()
    payload = {'filename': sFileName}

    sUrl = wasdi.getBaseUrl()
    sUrl += '/processors/downloadprocessor?'
    sUrl += 'processorId='
    sUrl += sFileName

    wasdi.wasdiLog('[INFO] waspy._downloadProcessor: send request to configured url ' + sUrl)

    try:
        oResponse = requests.get(sUrl, headers=asHeaders, params=payload, stream=True, timeout=wasdi.geRequestsTimeout())
    except Exception as oEx:
        wasdi.wasdiLog("[ERROR] there was an error contacting the API " + str(oEx))

    if (oResponse is not None) and (oResponse.status_code == 200):
        wasdi.wasdiLog('[INFO] waspy._downloadProcessor: got ok result, downloading')
        sAttachmentName = None
        asResponseHeaders = oResponse.headers
        if asResponseHeaders is not None:
            if 'Content-Disposition' in asResponseHeaders:
                sContentDisposition = asResponseHeaders['Content-Disposition']
                sAttachmentName = sContentDisposition.split('filename=')[1]
                bLoop = True
                while bLoop is True:
                    if sAttachmentName[0] == '.':
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[0] == '/') or (sAttachmentName[0] == '\\'):
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[-1] == '/') or (sAttachmentName[-1] == '\\'):
                        sAttachmentName = sAttachmentName[:-1]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[0] == '\"') or (sAttachmentName[0] == '\''):
                        sAttachmentName = sAttachmentName[1:]
                        bLoop = True
                    else:
                        bLoop = False
                    if (sAttachmentName[-1] == '\"') or (sAttachmentName[-1] == '\''):
                        sAttachmentName = sAttachmentName[:-1]
                        bLoop = True
                    else:
                        bLoop = False
        sSavePath = wasdi.getSavePath()
        sSavePath = os.path.join(sSavePath, sAttachmentName)

        if os.path.exists(os.path.dirname(sSavePath)) == False:
            try:
                os.makedirs(os.path.dirname(sSavePath))
            except:  # Guard against race condition
                print('[ERROR] waspy._downloadProcessor: cannot create File Path, aborting' +
                      '  ******************************************************************************')
                return

        wasdi.wasdiLog('[INFO] waspy._downloadProcessor: downloading local file ' + sSavePath)

        with open(sSavePath, 'wb') as oFile:
            for oChunk in oResponse:
                # _log('.')
                oFile.write(oChunk)
        wasdi.wasdiLog('[INFO] waspy._downloadProcessor: download done, new file locally available ' + sSavePath)

        if (sAttachmentName is not None) and \
                sAttachmentName.lower().endswith('.zip'):
            sPath = wasdi.getSavePath()
            _unzip(sAttachmentName, sPath)

    else:
        print('[ERROR] waspy._downloadProcessor: download error, server code: ' + str(oResponse.status_code) +
              '  ******************************************************************************')

    return


def getEnvironmentVariable(sKey, **kwargs):
    try:
        return os.environ[sKey]
    except KeyError:
        return kwargs['sDefault']


def isS3CanBeActivated(aoS3Configuration):
    if aoS3Configuration['sAccessKey']       == '' \
        or aoS3Configuration['sBucketName']  == '' \
        or aoS3Configuration['sEndpointUrl'] == '' \
        or aoS3Configuration['sRegionName']  == '' \
        or aoS3Configuration['sSecretKey']   == '':
        return False

    return True


def executeProcessor(aoS3Configuration):
    sProcessId = wasdi.getProcId()

    # First of all be sure to be in the right path
    #sProcessorDirPath = os.path.dirname(os.path.realpath(__file__))
    #os.chdir(sProcessorDirPath)

    if sProcessId is None or sProcessId.strip() == '':
        sProcessId = 'test'

    # Get the logging object
    oLogging = WasdiLogging(
        sLoggerName = 'wasdiProcessor'
    )

    # Add a prefix to all logs we will produce
    oLogging.sPrefixDefault = '[' + sProcessId + ']'
    oLogging.info('wasdi.executeProcessor: processor folder set')

    # Init Wasdi
    oLogging.info('wasdi.executeProcessor: init waspy lib')
    wasdi.setIsOnServer(False)
    wasdi.setIsOnExternalServer(True)
    wasdi.setDownloadActive(True)
    wasdi.setUploadActive(True)

    sForceStatus = 'ERROR'

    # Run the processor
    try:
        import myProcessor

        wasdi.wasdiLog('wasdi.executeProcessor RUN ' + sProcessId)
        myProcessor.run()
        wasdi.wasdiLog('wasdi.executeProcessor Done')

        '''
        DISABLE FOR THE MOMENT
        if isS3CanBeActivated(aoS3Configuration):
            # Init S3
            oWasdiS3 = WasdiS3(**aoS3Configuration)

            # Push files
            asProducts = wasdi.getProductsByActiveWorkspace()

            for sFile in asProducts:
                sFullPath = wasdi.getPath(sFile)
                oWasdiS3.uploadFile(sFullPath)
        '''

        '''
        CREATE THE DIRECTORY TO WRITE IN S3
        '''
        sObjectStorageDirectory = os.path.realpath(
            os.path.join(
                os.environ['WASDI_OUTPUT'],
                's3_results'
            )
        )

        if not os.path.isdir(sObjectStorageDirectory):
            wasdi.wasdiLog('Create the directory: %s' %(sObjectStorageDirectory))
            os.mkdir(sObjectStorageDirectory)
            wasdi.wasdiLog('The directory %s is created' %(sObjectStorageDirectory))


        '''
        PUBLISH FILES PRODUCED IN THE S3 BUCKET
        '''
        asProducts = wasdi.getProductsByActiveWorkspace()

        for sFile in asProducts:
            wasdi.wasdiLog('Copy the file %s in the S3 bucket' %(sFile))
            shutil.copy(
                wasdi.getPath(sFile),
                os.path.join(
                    sObjectStorageDirectory,
                    sFile
                )
            )

            wasdi.wasdiLog('The file %s is written' %(sFile))

        sForceStatus = 'DONE'
    except Exception as oEx:
        wasdi.wasdiLog('wasdi.executeProcessor EXCEPTION')
        wasdi.wasdiLog(repr(oEx))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog('wasdi.executeProcessor generic EXCEPTION')
    finally:
        sFinalStatus = wasdi.getProcessStatus(sProcessId)

        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog('wasdi.executeProcessor Process finished. Forcing status to ' + sForceStatus)
            wasdi.updateProcessStatus(sProcessId, sForceStatus, 100)

    return

if __name__ == '__main__':
    '''
    Initialize a named logger
    to be able to retrieve it later
    thanks to the Singleton
    '''
    sLoggerName = 'wasdiProcessor'
    oLogging = WasdiLogging(
        sLoggerName = sLoggerName,
        aoLogOnScreen = {
            'bEnable': True
        }
    )


    '''
    Configure the directory in
    which we can write
    '''
    oLogging.info('Configure the directory in which we can write')

    sWriteDir = os.path.realpath(
        os.environ['WASDI_OUTPUT']
    ) + os.sep

    wasdi.setBasePath(sWriteDir)
    oLogging.info('OK: %s' %(sWriteDir))


    '''
    Set the path to the configuration file to parse
    '''
    oLogging.info('Create the path to the configuration file')
    sConfigPath = os.path.join(sWriteDir, 'config.json')
    oLogging.info('OK: %s' %(sConfigPath))


    '''
    Check if the configuration file exists
    '''
    oLogging.info('Check if \'%s\' exists' %(sConfigPath))

    if os.path.exists(sConfigPath):
        oLogging.info('OK')
    else:
        if getEnvironmentVariable('S_INTERACTIVE', sDefault = '') == '':
            oLogging.error('The file does not exist: we stop now')
            sys.exit(1)
        else:
            oLogging.error('The file does not exist but we continue as we are in the interactive mode')


    '''
    Initialize the WASDI library
    '''
    oLogging.info('Initialize the WASDI engine')

    try:
        wasdi.init(sConfigPath)
    except Exception as oException:
        oLogging.error('Unable to initialize the WASDI engine: we stop now')
        oLogging.error('Exception: %s' %(repr(oException)))
        oLogging.error(traceback.format_exc())
        sys.exit(1)


    '''
    Prepare the S3 configuration
    '''
    aoS3Configuration = {
        'sLoggerName'  : sLoggerName,
        'sAccessKey'   : getEnvironmentVariable('S_S3_ACCESS_KEY',   sDefault = ''),
        'sBucketName'  : getEnvironmentVariable('S_S3_BUCKET_NAME',  sDefault = ''),
        'sEndpointUrl' : getEnvironmentVariable('S_S3_ENDPOINT_URL', sDefault = ''),
        'sRegionName'  : getEnvironmentVariable('S_S3_REGION_NAME',  sDefault = ''),
        'sSecretKey'   : getEnvironmentVariable('S_S3_SECRET_KEY',   sDefault = ''),
    }


    '''
    Start the processor
    '''
    oLogging.info('Start the processor')

    try:
        executeProcessor(aoS3Configuration)
        oLogging.info('The processor was executed successfully')
    except Exception as oException:
        oLogging.error('The processor failed')
        oLogging.error('Exception: %s' %(repr(oException)))
        oLogging.error(traceback.format_exc())
        sys.exit(1)
