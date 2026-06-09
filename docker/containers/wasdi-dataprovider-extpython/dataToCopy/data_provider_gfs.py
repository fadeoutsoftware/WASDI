import sys
import json
import os
import logging
from datetime import datetime, timedelta
from data_provider_utils import DataProviderUtils

s_sDataProviderName = 'GFS_NRT'
s_sPlatform = 'GFS'

s_sEndPointUrl = "https://noaa-gfs-bdp-pds.s3.amazonaws.com"
s_sBucketName = "noaa-gfs-bdp-pds"
s_sBaseUrl = "https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?"


def getDatesIntervals(sStartDate, sEndDate):
    """
    Given a start and an end date, returns two lists of date time objets:
    - the first list includes the date time objects that fall between those two dates, but only if those dates are no older than 9 days from the current date.
    - the second list includes the date time objects which are older tha 9 days
    """
    asStartDateTime = sStartDate.split("T")
    asEndDateTime = sEndDate.split("T")

    oStartDate = datetime.strptime(asStartDateTime[0], '%Y-%m-%d')
    oEndDate = datetime.strptime(asEndDateTime[0], '%Y-%m-%d')

    oToday = datetime.today()

    oOneDayStep = timedelta(days=1)
    oValidModelsRange = timedelta(days=9)

    oLastValidDate = oToday - oValidModelsRange

    oActualDate = oStartDate

    aoMostRecentDates = []
    aoPastDates = []

    while oActualDate <= oEndDate:
        if oActualDate >= oLastValidDate:
            aoMostRecentDates.append(oActualDate)
        else:
            aoPastDates.append(oActualDate)
        oActualDate = oActualDate + oOneDayStep

    return aoMostRecentDates, aoPastDates


def executeCount(sInputFilePath, sOutputFilePath):
    logging.debug("execute count")
    if not os.path.isfile(sInputFilePath):
        logging.warning('executeCount: input file not found')

    oQueryViewModel = DataProviderUtils.getQueryViewModel(sInputFilePath)

    if oQueryViewModel is None:
        logging.warning(f'executeCount: input file {sInputFilePath} is None')
        sys.exit(1)

    sStartDate = oQueryViewModel.startFromDate
    sEndDate = oQueryViewModel.endToDate
    sVariable = oQueryViewModel.productType
    sLevel = oQueryViewModel.productLevel
    sModelRun = oQueryViewModel.filters["modelRun"]
    sForecastTime = oQueryViewModel.filters["forecastTime"] #f000

    aoMostRecentDates, aoPastDates = getDatesIntervals(sStartDate, sEndDate)

    iRunMultiplier = 1
    if sModelRun == "ALL":
        iRunMultiplier = 4

    iForecastTimeMultiplier = 1
    if sForecastTime == "ALL":
        iForecastTimeMultiplier = 210

    iValidDates = len(aoMostRecentDates)

    aoReturnObject = {}

    # We compute the number of total results
    iResultCount = iValidDates * iForecastTimeMultiplier * iRunMultiplier

    if sVariable == "PRATE" and sLevel.lower() == "surfaca":
        logging.debug("executeCount: for PRATE - SURFACE we have also historical data")
        # For this variable and level we have also historical data, so we can consider also the past dates
        iResultCount = iResultCount + len(aoPastDates) * iForecastTimeMultiplier * iRunMultiplier

    aoReturnObject["count"] = iResultCount

    logging.debug(f'executeCount: result count is {iResultCount}')

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnObject, oFile)
    except Exception as oEx:
        logging.warning(f'executeCount: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)

def executeAndRetrieve(sInputFilePath, sOutputFilePath):

    oQueryViewModel = DataProviderUtils.getQueryViewModel(sInputFilePath)

    if oQueryViewModel is None:
        logging.warning(f'executeCount: input file {sInputFilePath} is None')
        sys.exit(1)

    sStartDate = oQueryViewModel.startFromDate
    sEndDate = oQueryViewModel.endToDate
    sVariable = oQueryViewModel.productType
    sLevel = oQueryViewModel.productLevel
    sModelRun = oQueryViewModel.filters["modelRun"] # run
    sForecastTime = oQueryViewModel.filters["forecastTime"] #f000

    aoMostRecentDates, aoPastDates = getDatesIntervals(sStartDate, sEndDate)
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

    # for PRATE-surface we have also historical data, so we can consider also the past dates
    if sVariable == "PRATE" and sLevel.lower() == "surface":
        for oDate in aoPastDates:
            for sModelRun in aoModelRuns:
                for sTime in aoForecastTime:
                        oResult = {}
                        sDate = oDate.strftime("%Y%m%d")
                        oResult["title"] = "GFS_"+sDate+"_" + sModelRun + "_" + sTime + "_" + sVariable + "_" + sLevel
                        oResult["id"] = "gfs." + sDate + "_" + "gfs.t"+sModelRun+"z.pgrb2.0p25."+sTime + "_" + sLevel
                        oResult["summary"] = "GFS Model date: " + sDate + " ran at: " + sModelRun + " forecast time: " + sTime + " Variable: " + sVariable + " Level: " + sLevel
                        if sModelRun == "LAST":
                            sModelRun = "18"    # the last model run for dates in the past is always 18
                        oResult["link"] = "https://noaa-gfs-bdp-pds.s3.amazonaws.com/gfs." + sDate + "/" + sModelRun + "/atmos/gfs.t" + sModelRun + "z.pgrb2.0p25." + sTime
                        oResult["provider"] = s_sDataProviderName
                        oResult["platform"] = s_sPlatform
                        aoReturnList.append(oResult)

    for oDate in aoMostRecentDates:
        for sModelRun in aoModelRuns:
            for sTime in aoForecastTime:
                oResult = {}
                sDate = oDate.strftime("%Y-%m-%d")
                oResult["title"] = "GFS_"+sDate+"_" + sModelRun + "_" + sTime + "_" + sVariable + "_" + sLevel
                oResult["id"] = "gfs." + sDate + "_" + "gfs.t"+sModelRun+"z.pgrb2.0p25."+sTime + "_" + sLevel
                oResult["link"] = sDate +"_" + sModelRun + "_" + sTime + "_" + sVariable + "_" + sLevel
                oResult["summary"] = "GFS Model date: " + sDate + " ran at: " + sModelRun + " forecast time: " + sTime + " Variable: " + sVariable + " Level: " + sLevel
                oResult["provider"] = s_sDataProviderName
                oResult["platform"] = s_sPlatform

                aoReturnList.append(oResult)

    try:
        with open(sOutputFilePath, 'w') as oFile:
            json.dump(aoReturnList, oFile)
    except Exception as oEx:
        logging.warning(f'executeAndRetrieve: error trying to write the output file {sOutputFilePath}, {oEx}')
        sys.exit(1)

def getPreviousRun(sRun):
    if sRun == "18":
        return "12"
    elif sRun == "12":
        return "06"
    elif sRun == "06":
        return "00"

    return ""

def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    oDownloadFileViewModel = DataProviderUtils.getDownloadFileViewModel(sInputFilePath)

    if oDownloadFileViewModel is None:
        logging.warning(f"executeDownloadFile: Impossible to read the Download File View Model")
        sys.exit(1)

    oDataProviderConfig = DataProviderUtils.getDataProviderConfig(sWasdiConfigFilePath, s_sDataProviderName)

    if oDataProviderConfig is None:
        logging.warning(f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
        sys.exit(1)
    sTargetFolder = oDownloadFileViewModel.downloadDirectory
    sTargetFileName = oDownloadFileViewModel.downloadFileName
    sDownloadedFilePath = sTargetFolder + sTargetFileName
    iMaxRetry = oDownloadFileViewModel.maxRetry
    sUrl = oDownloadFileViewModel.url

    if sUrl.startswith("https://"):
        bDownloaded = DataProviderUtils.downloadFile(sUrl, sDownloadedFilePath)
        if not bDownloaded:
            # we try to download the file with the same url, but removing the 'atmos' part, since that folder is not always present in the NOAA S3 bucket
                sUrl = sUrl.replace("/atmos", "")
                bDownloaded = DataProviderUtils.downloadFile(sUrl, sDownloadedFilePath)

    else:
        asUrlParts = sUrl.split("_")

        sDate = asUrlParts[0]
        sRun = asUrlParts[1]
        sForecastTime = asUrlParts[2]
        sVariable = asUrlParts[3]
        sLevel = asUrlParts[4]

        sUrl = s_sBaseUrl
        oNow = datetime.today()
        oDate = datetime.strptime(sDate,"%Y-%m-%d")

        bTestLastForToday = False

        if sRun == "LAST":
            sRun = "18"
            if oNow.date()==oDate.date():
                bTestLastForToday = True
                iHour = oNow.hour
                if iHour<6:
                    sRun = "00"
                elif iHour<12:
                    sRun = "06"
                elif iHour<18:
                    sRun = "12"
                else:
                    sRun = "18"

        sUrl += "dir=/gfs." + sDate.replace("-","") + "/" + sRun + "/atmos"
        sUrl += "&"
        sUrl += "file=gfs.t"+sRun + "z.pgrb2.0p25."+ sForecastTime
        sUrl += "&"
        sUrl += "var_"+ sVariable + "=on"
        sUrl += "&"
        sUrl += "lev_" + sLevel + "=on"

        bDownloaded = DataProviderUtils.downloadFile(sUrl, sDownloadedFilePath)

    if not bDownloaded:
        # Maybe we can search a previous run for today?
        if bTestLastForToday:
            sRun = getPreviousRun(sRun)
            if not DataProviderUtils.stringIsNullOrEmpty(sRun):
                bDownloaded = DataProviderUtils.downloadFile(sUrl, sDownloadedFilePath)


    if not bDownloaded:
        sDownloadedFilePath = ""

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
    oFileNameViewModel = DataProviderUtils.getFileNameViewModel(sInputFilePath)

    if oFileNameViewModel is None:
        logging.warning(f'getFileName: error trying to read the view model')
        sys.exit(1)

    sUrl = oFileNameViewModel.url
    
    sFileName = ""

    # Extract the file name from the Url
    if sUrl.startswith("https://"):
        try:
            # we are dealing with historical products - we extract them from the NOAA S3 bucket
            asUrlParts = sUrl.removeprefix("https://").split("/")
            sDate = asUrlParts[1].removeprefix("gfs.")
            oDateTime = datetime.strptime(sDate, "%Y%m%d")
            sRun = asUrlParts[2]
            sForecastTime = asUrlParts[-1].split(".")[-1]
            sFileName = f"GFS_{oDateTime:%Y-%m-%d}_{sRun}_{sForecastTime}_PRATE_surface.grb2"
        except:
            sFileName = "GFS_no_name.grb2"
    else:
        sFileName = "GFS_" + sUrl + ".grb2"

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
