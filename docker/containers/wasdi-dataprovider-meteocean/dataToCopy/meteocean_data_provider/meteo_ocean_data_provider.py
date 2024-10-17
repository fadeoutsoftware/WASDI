"""
First assumptions:
- using the local FS for getting the files --> then we will need to replace if with the access to the S3 bucker
- we miss the API service
"""

import json
import logging
import xarray as xr
import numpy as np

logging.basicConfig(format='%(levelname)s:%(message)s', level=logging.INFO)


def isStringNullOrEmpty(sString):
    return sString is None or sString == ''


def isBoudningBoxValid(dNorth, dSouth, dWest, dEast):
    return isLatitudeValid(dNorth, dSouth) and isLongitudeValid(dEast, dWest)


def isLatitudeValid(dNorth, sSouth):
    if not -90 <= dNorth <= 90:
        return False

    if not -90 <= sSouth <= 90:
        return False

    return sSouth <= dNorth


def isLongitudeValid(dEast, dWest):
    if not -180 < dEast < 180:
        return False

    if not -180 < dWest < 180:
        return False

    return dWest <= dEast


def isInputValid(aoInputMap):
    # - check that the data provider is valid
    sPlatformName = aoInputMap.get('platformName')
    logging.info("isInputValid. platform name: " + sPlatformName)
    if isStringNullOrEmpty(sPlatformName):
        logging.warning("isInputValid. platform name is null or empty")
        return False

    # - check that the dataset (productType) is valid
    # TODO: should I also check for acceptable values?
    sDataset = aoInputMap.get('productType')
    logging.info("isInputValid. dataset " + sDataset)
    if isStringNullOrEmpty(sDataset):
        logging.warning("isInputValid. dataset is null or empty")
        return False

    # - check that the variable (productLevel) is valid
    # TODO: should I also check for acceptable values?
    sVariable = aoInputMap.get('productLevel')
    logging.info("isInputValid. product level " + sVariable)
    if isStringNullOrEmpty(sVariable):
        logging.warning("isInputValid. variable is null or empty")
        return False

    # - check that the case (sensorMode) is valid
    # TODO: should I also check for acceptable values?
    sCase = aoInputMap.get('sensorMode')
    logging.info("isInputValid. case " + sCase)
    if isStringNullOrEmpty(sCase):
        logging.warning("isInputValid. case is null or empty")
        return False

    # - check that the coordinates are valid
    dNorth = aoInputMap.get('north')
    dSouth = aoInputMap.get('south')
    dWest = aoInputMap.get('west')
    dEast = aoInputMap.get('east')
    if dNorth is None or dSouth is None or dWest is None or dEast is None:
        logging.warning("isInputValid. some coordinates are missing")
        return False

    if not isBoudningBoxValid(dNorth, dSouth, dWest, dEast):
        logging.warning("isInputValid. bounding box not valid")
        return False

    return True
    
def countResults(aoInputMap):
    iCount = -1

    # preliminary controls. Check the inputs for validation
    if not isInputValid(aoInputMap):
        logging.warning("countResults. some of the input parameters are not valid")
        return iCount

    # if the inputs are valid, we can proceed to check if there are data in the bounding box specified in the query

    # so far we get the name of the local dataset, but it will need to be replaced with the access to the S3 bucket
    # TODO: replace with the access to the bucket

    sDatasetFolderPath = "C:/.../wave_dataset"

    # get the name of the file to access
    # TODO: we try with HINDCAST, then we extend to other models

    sFileName = ""
    sDataset = aoInputMap.get('productType')
    sVariable = aoInputMap.get('productLevel')
    sCase = aoInputMap.get('sensorMode')
    dNorth = aoInputMap.get('north')
    dSouth = aoInputMap.get('south')
    dWest = aoInputMap.get('west')
    dEast = aoInputMap.get('east')

    if sDataset.lower() == 'hindcast':
        logging.info("countResults. Selected dataset: hindcast")
        sFileName += 'hindcast_' + sVariable + '_1979_2005__' + sCase + ".nc"
        sProductFilePath = sDatasetFolderPath + '/' + sFileName
        logging.info("countResults. product path: " + sProductFilePath)

        oHindcastDataset = xr.open_dataset(sProductFilePath, engine='h5netcdf')
        oDatasetVariableData = oHindcastDataset[sVariable]

        bIsInBoundingBox = (oDatasetVariableData['longitude'] >= dWest) & (oDatasetVariableData['longitude'] <= dEast) \
                           & (oDatasetVariableData['latitude'] >= dSouth) & (oDatasetVariableData['latitude'] >= dNorth)

        oValuesInBoundingBox = oDatasetVariableData.where(bIsInBoundingBox, drop=True)

        if np.any(~np.isnan(oValuesInBoundingBox)):
            logging.info("countResults. some values in the selected bounding box")
            return 1
        else:
            logging.info("countResults. no data in the selected bounding box")
            return 0

        """
        oNewDataset = oValuesInBoundingBox.to_dataset()

        oNewDataset.to_netcdf("C:/Users/valentina.leone/Desktop/WORK/Return/subsample.nc", engine="h5netcdf")
        """

    return iCount

def executeAndRetrieve(aoInputMap):
    aoResults = None

    # preliminary controls. Check the inputs for validation
    if not isInputValid(aoInputMap):
        logging.warning("countResults. some of the input parameters are not valid")
        return None

        # if the inputs are valid, we can proceed to check if there are data in the bounding box specified in the query

        # so far we get the name of the local dataset, but it will need to be replaced with the access to the S3 bucket
        # TODO: replace with the access to the bucket

        sDatasetFolderPath = "C:/.../wave_dataset"

        # get the name of the file to access
        # TODO: we try with HINDCAST, then we extend to other models

        sFileName = ""
        sDataset = aoInputMap.get('productType')
        sVariable = aoInputMap.get('productLevel')
        sCase = aoInputMap.get('sensorMode')
        dNorth = aoInputMap.get('north')
        dSouth = aoInputMap.get('south')
        dWest = aoInputMap.get('west')
        dEast = aoInputMap.get('east')

        if sDataset.lower() == 'hindcast':
            logging.info("countResults. Selected dataset: hindcast")
            sFileName += 'hindcast_' + sVariable + '_1979_2005__' + sCase + ".nc"
            sProductFilePath = sDatasetFolderPath + '/' + sFileName
            logging.info("countResults. product path: " + sProductFilePath)

            oHindcastDataset = xr.open_dataset(sProductFilePath, engine='h5netcdf')
            oDatasetVariableData = oHindcastDataset[sVariable]

            bIsInBoundingBox = (oDatasetVariableData['longitude'] >= dWest) & (
                        oDatasetVariableData['longitude'] <= dEast) \
                               & (oDatasetVariableData['latitude'] >= dSouth) & (
                                           oDatasetVariableData['latitude'] >= dNorth)

            oValuesInBoundingBox = oDatasetVariableData.where(bIsInBoundingBox, drop=True)

            if np.any(~np.isnan(oValuesInBoundingBox)):
                logging.info("countResults. some values in the selected bounding box")

            else:
                logging.info("countResults. no data in the selected bounding box")
                return 0

def createQueryResultViewModel(sDataset, sOriginalFileName, sVariable, sCase, dNorth, dSouth, dWest, dEast):
    sTitle = f"{sDataset}_{sVariable}_{sCase}_{formatDecimal(dWest)}W_{formatDecimal(dNorth)}N_{formatDecimal(dEast)}E_{formatDecimal(dSouth)}S.nc"
    oResultVM = dict()
    oResultVM['preview'] = None
    oResultVM['title'] = sTitle
    oResultVM['summary'] = None
    oResultVM['id'] = sTitle
    oResultVM['link'] = f"{sOriginalFileName},{sVariable},{sCase},{dNorth},{dSouth},{dWest},{dEast}"
    oResultVM['footprint'] = f"POLYGON (({dWest} {dSouth}, {dWest} {dNorth}, {dEast} {dNorth}, {dEast} {dSouth}, {dWest} {dSouth}))"
    oResultVM['provider'] = "MeteOcean"
    oResultVM['volumeName'] = None
    oResultVM['volumePath'] = None
    oResultVM['properties'] = None

    return oResultVM

def formatDecimal(dValue):
    sSign = 'P' if dValue >= 0 else 'N'
    dAbsValue = abs(dValue)
    sIntegerPart, sDecimalPart = str(dAbsValue).split('.')
    sIntegerPart = sIntegerPart.zfill(3)
    sDecimalPart = sDecimalPart.ljust(4, '0')
    formatted_value = f"{sSign}{sIntegerPart}.{sDecimalPart}"

    return formatted_value

if __name__ == '__main__':
    # let's read the input from a file. It will be something else then
    sInputFileName = "C:/.../test_input_meteocean"
    with open(sInputFileName, 'r') as oJsonInputFile:
        aoInputMap = json.load(oJsonInputFile)

    iResultsCount = countResults(aoInputMap)
    print("Count of results: " + str(iResultsCount))