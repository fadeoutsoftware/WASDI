import wasdi
from datetime import datetime
from datetime import timedelta
import zipfile
import os
from osgeo import gdal
import numpy


def extractBands(sFile, sImageType):
    try:

        sOutputVrtFile = sFile.replace(".zip", ".vrt")
        sOutputTiffFile = sFile.replace(".zip", ".tif")

        # Get the Path
        sLocalFilePath = wasdi.getPath(sFile)
        sOutputVrtPath = wasdi.getPath(sOutputVrtFile)
        sOutputTiffPath = wasdi.getPath(sOutputTiffFile)

        # Band Names for S2 L2
        asBandsJp2 = ['B04_10m.jp2', 'B03_10m.jp2', 'B02_10m.jp2']

        if sImageType != "S2MSI2A":
            # Band Names for S2 L1
            asBandsJp2 = ['B04.jp2', 'B03.jp2', 'B02.jp2']

        with zipfile.ZipFile(sLocalFilePath, 'r') as sZipFiles:
            asZipNameList = sZipFiles.namelist()

            asBandsS2 = [name for name in asZipNameList for band in asBandsJp2 if band in name]

            asBandsZip = ['/vsizip/' + sLocalFilePath + '/' + band for band in asBandsS2]

            asOrderedZipBands = []

            for sBand in ['B04', 'B03', 'B02']:
                for sZipBand in asBandsZip:
                    if sBand in sZipBand:
                        asOrderedZipBands.append(sZipBand)
                        break

            gdal.BuildVRT(sOutputVrtPath, asOrderedZipBands, separate=True)

            # , options="-tr " + sResolution + " " + sResolution
            gdal.Translate(sOutputTiffPath, sOutputVrtPath)

            os.remove(sOutputVrtPath)

            return sOutputTiffFile
    except Exception as oEx:
        wasdi.wasdiLog(f'extractBands EXCEPTION: {repr(oEx)}')
    return ""


def stretchBandValues(sOutputTiffPath, sStretchedOutputFile):
    oDataset = gdal.Open(wasdi.getPath(sOutputTiffPath))

    if not oDataset:
        wasdi.wasdiLog("Impossible to get Dataset from " + sOutputTiffPath)
        return ""

    [iCols, iRows] = oDataset.GetRasterBand(1).ReadAsArray().shape
    oDriver = gdal.GetDriverByName("GTiff")
    oOutDataFile = oDriver.Create(wasdi.getPath(sStretchedOutputFile), iRows, iCols,
                                  oDataset.RasterCount, gdal.GDT_Byte, ['COMPRESS=LZW', 'BIGTIFF=YES'])

    # sets same geotransform as input
    oOutDataFile.SetGeoTransform(oDataset.GetGeoTransform())
    # sets same projection as input
    oOutDataFile.SetProjection(oDataset.GetProjection())

    for iBand in range(oDataset.RasterCount):
        iBand += 1
        oBand = oDataset.GetRasterBand(iBand)

        if oBand is None:
            wasdi.wasdiLog("BAND " + str(iBand) + " is None, jump")
            continue

        adBandArray = numpy.array(oBand.ReadAsArray())
        adBandArray[adBandArray > 5000] = 5000
        adBandArray = adBandArray.astype(float)
        adBandArray *= 0.051
        adBandArray = adBandArray.astype(int)

        oOutDataFile.GetRasterBand(iBand).WriteArray(adBandArray)
        oOutDataFile.GetRasterBand(iBand).SetNoDataValue(0)
        del oBand

    # saves to disk!!
    oOutDataFile.FlushCache()
    wasdi.wasdiLog("Saved " + sStretchedOutputFile)


def run():
    # STEP 1: Read "real" parameters

    sBBox = wasdi.getParameter("BBOX")
    sDate = wasdi.getParameter("DATE")
    sMaxCloud = wasdi.getParameter("MAXCLOUD", "20")
    sSearchDays = wasdi.getParameter("SEARCHDAYS", "10")

    sProvider = wasdi.getParameter("PROVIDER", "ONDA")

    # L1
    sImageType = wasdi.getParameter("IMAGETYPE", "S2MSI1C")
    # L2
    # sImageType = wasdi.getParameter("IMAGETYPE", "S2MSI2A")

    # Check the Bounding Box: is needed
    if sBBox is None:
        wasdi.wasdiLog("BBOX Parameter not set. Exit")
        wasdi.updateStatus("ERROR", 0)
        return

    # Split the bbox: it is in the format: NORTH, WEST, SOUTH, EAST
    asBBox = sBBox.split(",")

    if len(asBBox) != 4:
        wasdi.wasdiLog("BBOX Not valid. Please use LATN,LONW,LATS,LONE")
        wasdi.wasdiLog("BBOX received:" + sBBox)
        wasdi.wasdiLog("exit")
        wasdi.updateStatus("ERROR", 0)
        return

    # Ok is good, print it and convert in float
    wasdi.wasdiLog("Bounding Box: " + sBBox)

    fLatN = float(asBBox[0])
    fLonW = float(asBBox[1])
    fLatS = float(asBBox[2])
    fLonE = float(asBBox[3])

    iDaysToSearch = 10

    try:
        iDaysToSearch = int(sSearchDays)
    except Exception as oEx:
        wasdi.wasdiLog(f'Number of days to search not valid due to {repr(oEx)}, assuming 10 [' + str(sSearchDays) + "]")

    # Check the date: assume now
    oEndDay = datetime.today()

    try:
        # Try to convert the one in the params
        oEndDay = datetime.strptime(sDate, '%Y-%m-%d')
    except Exception as oEx:
        # No good: force to yesterday
        wasdi.wasdiLog(f'Date not valid due to {repr(oEx)}, assuming today')

    oTimeDelta = timedelta(days=iDaysToSearch)
    oStartDay = oEndDay - oTimeDelta
    sEndDate = oEndDay.strftime("%Y-%m-%d")
    sStartDate = oStartDay.strftime("%Y-%m-%d")

    # Print the date
    wasdi.wasdiLog("Search from " + sStartDate + " to " + sEndDate)

    # Check the cloud coverage
    sCloudCoverage = None

    if sMaxCloud is not None:
        sCloudCoverage = "[0 TO " + sMaxCloud + "]"
        wasdi.wasdiLog("Cloud Coverage " + sCloudCoverage)
    else:
        wasdi.wasdiLog("Cloud Coverage not set")

    # STEP 2: Search EO Images
    aoImages = wasdi.searchEOImages("S2", sStartDate, sEndDate, fLatN, fLonW, fLatS, fLonE, sImageType, None, None,
                                    sCloudCoverage, sProvider)

    for oImage in aoImages:
        wasdi.wasdiLog("Image Name WITHOUT Extension:" + oImage['title'])

    # STEP 3: Import EO Images in the workspace

    # Get the list of products in the workspace
    asAlreadyExistingImages = wasdi.getProductsByActiveWorkspace()

    # List of images not yet available
    aoImagesToImport = []

    # For each found image
    for oImage in aoImages:
        # Get the file Name from the search result
        sFileName = oImage["title"] + ".zip"

        # If the file name is not yet in the workspace
        if sFileName not in asAlreadyExistingImages:
            # Add it to the list of images to import
            aoImagesToImport.append(oImage)

    # If there are images to import
    if len(aoImagesToImport) > 0:
        # Trigger the import of the images
        wasdi.importProductList(aoImagesToImport, sProvider)
        wasdi.wasdiLog("Images Imported")

    # STEP 4: From the S2 image create a 8-bit RGB GeoTiff

    # Get again the list of images in the workspace:
    asAvailableImages = wasdi.getProductsByActiveWorkspace()

    # Check if we have at least one image
    if len(asAvailableImages) <= 0:
        # Nothing found
        wasdi.wasdiLog("No images available, nothing to do.")
        wasdi.updateStatus("DONE", 100)
        return

    # Take the first image
    for sGoodProduct in asAvailableImages:
        if(sGoodProduct.startswith("S2")):
            sImageToProcess= sGoodProduct
            break

    # Get the local path of the image: this is one of the key-feature of WASDI
    # The system checks if the image is available locally and, if it is not, it will download it
    sLocalImagePath = wasdi.getPath(sImageToProcess)

    sTiffFile = extractBands(sImageToProcess, sImageType)

    wasdi.wasdiLog("Generated RGB Tiff: " + sTiffFile)

    sOutputFile = sTiffFile.replace(".tif", "_rgb.tif")
    stretchBandValues(sTiffFile, sOutputFile)

    # Delete intermediate Tiff File: NOTE this has not been added to WASDI
    # so there is the need to clean only the physical file
    try:
        os.remove(wasdi.getPath(sTiffFile))
    except Exception as oEx:
        wasdi.wasdiLog(f'Error removing {sTiffFile} due to {repr(oEx)}')

    # Add the real output to the WASDI Workspace
    # NOTE: here starts the opposite path: when running locally, WASDI will upload the file to the cloud
    wasdi.addFileToWASDI(sOutputFile)

    # STEP 5: close the processor

    wasdi.wasdiLog("Created output file " + sOutputFile)

    aoPayload = {"OutputFile": sOutputFile}

    wasdi.setPayload(aoPayload)

    wasdi.updateStatus("DONE", 100)


if __name__ == '__main__':
    wasdi.init("./config.json")
    run()
