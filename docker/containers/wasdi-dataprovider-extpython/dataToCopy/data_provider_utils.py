import requests
import logging
import os
import json

from DownloadFileViewModel import DownloadFileViewModel
from FileNameViewModel import FileNameViewModel
from QueryViewModel import QueryViewModel


class DataProviderUtils:

    def __init__(self, **kwargs):
        pass

    @staticmethod
    def downloadFile(sUrl, sPath):
        """
        Download a file

        :param sUrl):
        :return: None
        """
        oResponse = None

        try:
            oResponse = requests.get(sUrl, stream=True)
        except Exception as oEx:
            logging.error("DataProviderUtils.downloadFile: there was an error contacting the url " + str(oEx))

        if oResponse is None:
            return False

        if oResponse.status_code == 200:
            logging.debug('DataProviderUtils.downloadFile: got ok result, downloading')
            sAttachmentName = None

            if os.path.exists(os.path.dirname(sPath)) == False:
                try:
                    os.makedirs(os.path.dirname(sPath))
                except:  # Guard against race condition
                    logging.error('DataProviderUtils.downloadFile: cannot create File Path, aborting' +
                          '  ******************************************************************************')
                    return False

            logging.debug('DataProviderUtils.downloadFile: downloading local file ' + sPath)

            with open(sPath, 'wb') as oFile:
                for oChunk in oResponse:
                    # _log('.')
                    oFile.write(oChunk)
            logging.debug('DataProviderUtils.downloadFile: download done, new file locally available ' + sPath)

        else:
            logging.error('DataProviderUtils.downloadFile: download error, server code: ' + str(oResponse.status_code) +
                  '  ******************************************************************************')
            return False

        return True

    @staticmethod
    def stringIsNullOrEmpty(sString):
        return sString is None or sString == ""

    @staticmethod
    def getDataProviderConfig(sWasdiConfigFilePath, sDataProviderName):
        if DataProviderUtils.stringIsNullOrEmpty(sWasdiConfigFilePath):
            logging.warning(f'DataProviderUtils.getDataProviderConfig: data provider configuration is None or empty string: {sWasdiConfigFilePath}')
            return None

        aoDataProviderConfig = None
        try:
            with open(sWasdiConfigFilePath) as oWasdiConfigJsonFile:
                aoDataProviderConfig = json.load(oWasdiConfigJsonFile)
        except Exception as oEx:
            logging.warning(f'DataProviderUtils.getDataProviderConfig: error reading the wasdiConfig file: {sWasdiConfigFilePath}, {oEx}')
            return None

        if aoDataProviderConfig is None:
            logging.warning(f'DataProviderUtils.getDataProviderConfig:  wasdiConfig file is None: {sWasdiConfigFilePath}')
            return None

        # find the configuration for the data provider
        oDataProviderConfig = None

        aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
        for oProvider in aoWasdiDataProviders:
            if oProvider.get('name', "") == sDataProviderName:
                oDataProviderConfig = oProvider
                break
        return oDataProviderConfig

    @staticmethod
    def getQueryViewModel(sInputFilePath):
        if not os.path.isfile(sInputFilePath):
            logging.warning('DataProviderUtils.getQueryViewModel: input file not found')
            return None

        try:
            with open(sInputFilePath) as oJsonFile:
                aoInputQuery = json.load(oJsonFile)
        except Exception as oEx:
            logging.error(f'DataProviderUtils.getQueryViewModel: error reading the input file: {sInputFilePath}, {oEx}')
            return None

        if aoInputQuery is None:
            logging.warning(f'DataProviderUtils.getQueryViewModel: input file {sInputFilePath} is None')
            return None

        oViewModel = QueryViewModel(**aoInputQuery)

        return oViewModel


    @staticmethod
    def getDownloadFileViewModel(sInputFilePath):
        if not os.path.isfile(sInputFilePath):
            logging.warning('DataProviderUtils.getDownloadFileViewModel: input file not found')

        aoInputParameters = None
        try:
            with open(sInputFilePath) as oJsonFile:
                aoInputParameters = json.load(oJsonFile)
        except Exception as oEx:
            logging.error(f'DataProviderUtils.getDownloadFileViewModel: error reading the input file: {sInputFilePath}, {oEx}')
            return None

        if aoInputParameters is None:
            return None

        oViewModel = DownloadFileViewModel(**aoInputParameters)

        return oViewModel

    @staticmethod
    def getFileNameViewModel(sInputFilePath):
        if not os.path.isfile(sInputFilePath):
            logging.warning('DataProviderUtils.getFileNameViewModel: input file not found')

        aoInputParameters = None
        try:
            with open(sInputFilePath) as oJsonFile:
                aoInputParameters = json.load(oJsonFile)
        except Exception as oEx:
            logging.error(f'DataProviderUtils.getFileNameViewModel: error reading the input file: {sInputFilePath}, {oEx}')
            return None

        if aoInputParameters is None:
            return None

        oViewModel = FileNameViewModel(**aoInputParameters)

        return oViewModel


