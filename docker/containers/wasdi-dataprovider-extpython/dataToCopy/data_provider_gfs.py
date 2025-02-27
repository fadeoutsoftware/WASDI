import sys
import json
import os
import re
import logging
import time
from datetime import datetime, timedelta
import boto3
from botocore import UNSIGNED
from botocore.config import Config

s_sDataProviderName = 'GFS_NRT'
s_sPlatform = 'GFS'

s_sEndPointUrl = "https://noaa-gfs-bdp-pds.s3.amazonaws.com"
s_sBucketName = "noaa-gfs-bdp-pds"
s_sBaseUrl = "https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?"

def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""


def getValidDates(sStartDate, sEndDate):
    asStartDateTime = sStartDate.split("T")
    asEndDateTime = sEndDate.split("T")

    oStartDate = datetime.strptime(asStartDateTime[0], '%Y-%m-%d')
    oEndDate = datetime.strptime(asEndDateTime[0], '%Y-%m-%d')

    oToday = datetime.today()

    oOneDayStep = timedelta(days=1)
    oValidModelsRange = timedelta(days=9)

    oLastValidDate = oToday - oValidModelsRange

    oActualDate = oStartDate

    aoValidDates = []

    while oActualDate <= oEndDate:
        if oActualDate >= oLastValidDate:
            aoValidDates.append(oActualDate)
        oActualDate = oActualDate + oOneDayStep

    return aoValidDates

def executeCount(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeCount: input file not found')

    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeCount: error reading the input file: {sInputFilePath}, {oEx}')
        sys.exit(1)

    if aoInputQuery is None:
        logging.warning(f'executeCount: input file {sInputFilePath} is None')
        sys.exit(1)

    sStartDate = aoInputQuery["startFromDate"]
    sEndDate = aoInputQuery["endToDate"]
    sVariable = aoInputQuery["productType"]
    sLevel = aoInputQuery["productLevel"]
    sModelRun = aoInputQuery["filters"]["modelRun"]
    sForecastTime = aoInputQuery["filters"]["forecastTime"]

    aoValidDates = getValidDates(sStartDate, sEndDate)

    iRunMultiplier = 1
    if sModelRun == "ALL":
        iRunMultiplier = 4

    iForecastTimeMultiplier = 1
    if sForecastTime == "ALL":
        iForecastTimeMultiplier = 210

    iValidDates = len(aoValidDates)

    #oS3Client = boto3.client('s3', config=Config(signature_version=UNSIGNED))
    #oPaginator = oS3Client.get_paginator('list_objects')
    #oResult = oPaginator.paginate(Bucket=s_sBucketName, Prefix="gfs.", Delimiter='/')
    #for oPrefix in oResult.search('CommonPrefixes'):
    #    print(oPrefix.get('Prefix'))

    aoReturnObject = {}
    # TODO: Make the query and return the total count
    iResultCount = iValidDates * iForecastTimeMultiplier * iRunMultiplier

    aoReturnObject["count"] = iResultCount

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnObject, oFile)
    except Exception as oEx:
        logging.warning(f'executeCount: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)

def executeAndRetrieve(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeAndRetrieve: input file not found')

    aoInputQuery = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeAndRetrieve: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputQuery is None:
        logging.warning(f'executeAndRetrieve: input file: {sInputFilePath} is None')
        sys.exit(1)

    sStartDate = aoInputQuery["startFromDate"]
    sEndDate = aoInputQuery["endToDate"]
    sVariable = aoInputQuery["productType"]
    sLevel = aoInputQuery["productLevel"]
    sModelRun = aoInputQuery["filters"]["modelRun"]
    sForecastTime = aoInputQuery["filters"]["forecastTime"]

    aoValidDates = getValidDates(sStartDate, sEndDate)
    aoModelRuns = []

    if sModelRun == "ALL":
        aoModelRuns.append("00")
        aoModelRuns.append("06")
        aoModelRuns.append("12")
        aoModelRuns.append("18")
    else:
        aoModelRuns.append(sModelRun)

    aoForecastTime = []
    if sForecastTime == "ALL":
        aoForecastTime.append("anl")
        iStep = 0
        while iStep <= 384:
            sStep = "f" + str(iStep).zfill(3)
            aoForecastTime.append(sStep)
            if iStep>120:
                iStep = iStep + 3
            else:
                iStep = iStep + 1
    else:
        aoForecastTime.append(sForecastTime)


    aoReturnList = []

    # Make the query, get the result and convert to QueryResultViewModels
    for oDate in aoValidDates:
        for sModel in aoModelRuns:
            for sTime in aoForecastTime:
                oResult = {}
                sDate = oDate.strftime("%Y-%m-%d")
                oResult["title"] = "GFS_"+sDate+"_t" + sModel + "_" + sTime + "_" + sVariable
                oResult["id"] = "gfs." + sDate + "_" + "gfs.t"+sModel+"z.pgrb2.0p25."+sTime
                oResult["link"] = sDate +"_" + sModel + "_" + sTime + "_" + sVariable
                oResult["summary"] = "GFS Model date: " + sDate + " ran at: " + sModel + " forecast time: " + sTime + " Variable: " + sVariable
                oResult["provider"] = s_sDataProviderName
                oResult["platform"] = s_sPlatform

                aoReturnList.append(oResult)

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnList, oFile)
    except Exception as oEx:
        logging.warning(f'executeAndRetrieve: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeDownloadFile: input file not found')

    aoInputParameters = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputParameters = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputParameters is None:
        logging.warning(f'executeDownloadFile: there was an error reading the input file: {sWasdiConfigFilePath}')
        sys.exit(1)

    if stringIsNullOrEmpty(sWasdiConfigFilePath):
        logging.warning(f'executeDownloadFile: data provider configuration is None or empty string: {sWasdiConfigFilePath}')
        sys.exit(1)

    aoDataProviderConfig = None
    try:
        with open(sWasdiConfigFilePath) as oWasdiConfigJsonFile:
            aoDataProviderConfig = json.load(oWasdiConfigJsonFile)
    except Exception as oEx:
        logging.warning(f'executeDownloadFile: error reading the wasdiConfig file: {sWasdiConfigFilePath}, {oEx}')
        sys.exit(1)

    if aoDataProviderConfig is None:
        logging.warning(f'executeDownloadFile:  wasdiConfig file is None: {sWasdiConfigFilePath}')
        
    sTargetFolder = aoInputParameters.get("downloadDirectory", "")
    sTargetFileName = aoInputParameters.get("downloadFileName", "")
    iMaxRetry = aoInputParameters.get("maxRetry", 1)

    # find the configuration for the data provider
    oDataProviderConfig = None

    aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
    for oProvider in aoWasdiDataProviders:
        if oProvider.get('name', "") == s_sDataProviderName:
            oDataProviderConfig = oProvider
            break

    if oDataProviderConfig is None:
        logging.warning(f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
        sys.exit(1)

    # TODO: Download the file in sTargetFolder + sTargetFileName
    sDownloadedFilePath = sTargetFolder + sTargetFileName

    oRes = {
        'outputFile': sDownloadedFilePath
    }

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
            logging.debug(f"path to the downloaded file written in the output file")
    except Exception as oEx:
        logging.error(f'executeDownloadFile: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


def getFileName(sInputFilePath, sOutputFilePath):
    if not os.path.isfile(sInputFilePath):
        logging.warning('getFileName: input file not found')

    aoInputQuery = None
    try:
        with open(sInputFilePath) as oJsonFile:
            aoInputQuery = json.load(oJsonFile)
    except Exception as oEx:
        logging.error(f'getFileName: error reading the input file: {sInputFilePath}, {oEx}')
        return sys.exit(-1)

    if aoInputQuery is None:
        logging.warning(f'getFileName: input file: {sInputFilePath} is None')
        sys.exit(1)
        
    sUrl = aoInputQuery.get("url","")
    # TODO: Extract the file name from the Url
    sFileName = ""
    
    oRes = {
        'fileName': sFileName
    }    

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
    except Exception as oEx:
        logging.warning(f'getFileName: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)


if __name__ == '__main__':
    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    sOperation = None
    sInputFile = None
    sOutputFile = None

    # let's read the arguments
    asArgs = sys.argv

    try:
        if asArgs is None or len(asArgs) < 5:
            logging.error("__main__: no arguments passed to the data provider")
            sys.exit(1)

        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sWasdiConfigFile = asArgs[4]

        logging.debug('__main__: WASDI GFS Data Provider ' + sOperation)

        # first argument asArgs[0] is the name of the file - we are not interested in it
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + sInputFile)
        logging.debug('__main__: output file: ' + sOutputFile)
        logging.debug('__main__: wasdi config path: ' + sWasdiConfigFile)

    except Exception as oE:
        logging.error('__main__: Exception ' + str(oE))
        sys.exit(1)
    
    if sOperation == "0":
        logging.debug('__main__: chosen operation is EXECUTE AND RETRIEVE')
        executeAndRetrieve(sInputFile, sOutputFile)
    elif sOperation == "1":
        logging.debug('__main__: chosen operation is EXECUTE COUNT')
        executeCount(sInputFile, sOutputFile)
    elif sOperation == "2":
        logging.debug('__main__: chosen operation is DOWNLOAD')
        executeDownloadFile(sInputFile, sOutputFile, sWasdiConfigFile)
    elif sOperation == "3":
        logging.debug('__main__: chosen operation is GET FILE NAME')
        getFileName(sInputFile, sOutputFile)
    else:
        logging.debug('__main__: unknown operation. Script will exit')
        sys.exit(1)

    sys.exit(0)
