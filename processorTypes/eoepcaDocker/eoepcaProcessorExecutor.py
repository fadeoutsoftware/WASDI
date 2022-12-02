'''
Created on 24 Nov 2022

@author: p.campanella
'''

import wasdi
import os
import zipfile
import sys
import urllib.parse
import json
import traceback
import requests
def _unzip(sAttachmentName, sPath):
    """
    Unzips a file

    :param sAttachmentName: filename to unzip

    :param sPath: both the path where the file is and where it must be unzipped
    :return: None
    """
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

def executeProcessor():

    sProcessId = wasdi.getProcId()
    # First of all be sure to be in the right path
    #sProcessorDirPath = os.path.dirname(os.path.realpath(__file__))
    #os.chdir(sProcessorDirPath)

    if sProcessId is None:
        sProcessId = "test"

    if sProcessId == "":
        sProcessId = ""

    print("[" + sProcessId+ "] wasdi.executeProcessor: processor folder set")
    
    #Init Wasdi
    print("[" + sProcessId+ "] wasdi.executeProcessor: init waspy lib")
    wasdi.setIsOnServer(False)
    wasdi.setIsOnExternalServer(True)
    wasdi.setDownloadActive(True)
    wasdi.setUploadActive(True)

    sForceStatus = 'ERROR'

    #Run the processor
    try:
        import myProcessor        
        wasdi.wasdiLog("wasdi.executeProcessor RUN " + sProcessId)
        myProcessor.run()
        wasdi.wasdiLog("wasdi.executeProcessor Done")
        
        sForceStatus = 'DONE'
        
    except Exception as oEx:
        wasdi.wasdiLog("wasdi.executeProcessor EXCEPTION")
        wasdi.wasdiLog(repr(oEx))
        wasdi.wasdiLog(traceback.format_exc())
    except:
        wasdi.wasdiLog("wasdi.executeProcessor generic EXCEPTION")
    finally:
        sFinalStatus = wasdi.getProcessStatus(sProcessId)
        
        if sFinalStatus != 'STOPPED' and sFinalStatus != 'DONE' and sFinalStatus != 'ERROR':
            wasdi.wasdiLog("wasdi.executeProcessor Process finished. Forcing status to " + sForceStatus)
            wasdi.updateProcessStatus(sProcessId, sForceStatus, 100)

    return

if __name__ == '__main__':
    sWriteDir = os.environ['WASDI_OUTPUT']

    print("eoepcaProcessorExecutor: Write Dir " + sWriteDir)

    if not sWriteDir.endswith("/"):
        sWriteDir = sWriteDir + "/"

    wasdi.setBasePath(sWriteDir)

    sConfigPath = sWriteDir + "config.json"
    print("eoepcaProcessorExecutor: config path: " + sConfigPath)

    wasdi.init(sConfigPath)

    print("called init, staring processor")
    executeProcessor()
