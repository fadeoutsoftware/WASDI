
'''
Unit Integration class
A config file is required in the current directory
The test case are executed in the same order as they are implemented on this file.
The scenario tested follows the tutorial of wasdi till test number 7, after that
some more functionalities are tested.
The execution MUST be done with all tests in order to work correctly.
The single execution of some test will fail because the state i which they need to be is a result
of previous test.
'''
import os
import shutil
import string
import random
import unittest
from datetime import date, timedelta
import logging
from pathlib import Path
import wasdi
from os.path import exists
from shutil import copyfile
import json

unittest.TestLoader.sortTestMethodsUsing = None


class WaspyIntegrationTests(unittest.TestCase):
    '''
    This workspace name is used for creation and deletion of temporary empty workspaces
    To checks products list and other test related to non-empty WS, this test set use the
    workspace initialized a the beginning of each test.
    To test everything please create a non empty ws and set it in config.json for initialization
    '''

    # todo create unique workspace name each time
    m_sWorkspaceName = ""

    # m_iWorkspaceNameLen = 64

    # todo test writing username and password from stdin
    # def test_InitBroken(self):
    #     bInitOk = wasdi.init("./nofile.txt");
    #     self.assertFalse(bInitOk)

    @classmethod
    def setUpClass(cls):
        cls.m_iWorkspaceNameLen = 128
        logging.info(f'setUpClass: workspace len: {cls.m_iWorkspaceNameLen}')
        # cls.m_sWorkspaceName = cls.randomString(cls.m_iWorkspaceNameLen)
        cls.m_sWorkspaceName = "SOMERANDOMWORKSPACEJUSTFORTESTINGPYTHONLIB"
        logging.info(f'setUpClass: workspace name: {cls.m_sWorkspaceName}, of len {len(cls.m_sWorkspaceName)}')
        cls.clearWorkspaces(cls, cls.m_sWorkspaceName)

        cls.sTestFile1Name = "S2A_MSIL1C_20201008T102031_N0209_R065_T32TMR_20201008T123525"
        cls.sTestFile2Name = "S2B_MSIL1C_20201013T101909_N0209_R065_T32TMR_20201018T165151"

        # tries to init from config file, if one is provided
        wasdi.init("./resources/config.json")

        cls.readBoundingBoxString(cls)

    def clearWorkspaces(self, sWorkspaceName):
        asWorkspaces = wasdi.getWorkspaces()
        if sWorkspaceName in [aoWorkspace['workspaceName'] for aoWorkspace in asWorkspaces]:
            logging.info(f'clearWorkspaces: workspace {sWorkspaceName} found')
            sWorkspaceId = wasdi.getWorkspaceIdByName(sWorkspaceName)
            bDeleted = wasdi.deleteWorkspace(sWorkspaceId)
            logging.info(f'clearWorkspaces: deleted? {bDeleted}')

    @classmethod
    def tearDownClass(cls):
        logging.info(f'tearDownClass: workspace name: {cls.m_sWorkspaceName}')
        asWorkspaces = wasdi.getWorkspaces()
        if cls.m_sWorkspaceName in asWorkspaces:
            if wasdi.deleteWorkspace(cls.m_sWorkspaceName):
                logging.info(f'tearDownClass: workspace {cls.m_sWorkspaceName} deleted correctly')
            else:
                logging.error(f'tearDownClass: could not delete workspace {cls.m_sWorkspaceName}')

    @classmethod
    def randomString(cls, size=6, chars=string.ascii_uppercase + string.digits):
        sRandom = ''.join(random.choice(chars) for _ in range(size))
        return sRandom

    @classmethod
    def clearProducts(cls):
        asProducts = wasdi.getProductsByActiveWorkspace()
        for sProduct in asProducts:
            wasdi.deleteProduct(sProduct)

    def readBoundingBoxString(self):
        sBbox = wasdi.getParameter("bbox")
        try:
            asBbox = sBbox.split(",")
            self.dULLat = asBbox[0]
            self.dULLon = asBbox[1]
            self.dLRLat = asBbox[2]
            self.dLRLon = asBbox[3]
        except Exception as oE:
            logging.error(f'readBoundingBox: {type(oE)}: {oE}')

    def searchImages(self):
        asQuery = {
            'sPlatform': wasdi.getParameter('platform'),
            'sDateFrom': wasdi.getParameter('startDate'),
            'sDateTo': wasdi.getParameter('endDate'),
            'fULLat': self.dULLat,
            'fULLon': self.dULLon,
            'fLRLat': self.dLRLat,
            'fLRLon': self.dLRLon,
            'sProductType': wasdi.getParameter('product.type'),
            'sCloudCoverage': wasdi.getParameter('max.cloud'),
            'sProvider': wasdi.getParameter('default.provider')
        }
        aoSearchResult = wasdi.searchEOImages(**asQuery)
        return aoSearchResult



    def test_01_createWorkspace(self):
        logging.info(f'test_01_createWorkspace: {self.m_sWorkspaceName}')
        self.clearWorkspaces(self.m_sWorkspaceName)
        aoWorkspaces = wasdi.getWorkspaces()
        if self.m_sWorkspaceName not in [aoWs['workspaceName'] for aoWs in aoWorkspaces]:
            sCreatedWorkspaceId = wasdi.createWorkspace(self.m_sWorkspaceName)
            logging.info(f'test_01_createWorkspace: created with workspaceId {sCreatedWorkspaceId}')
            sFoundWorkspaceId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
            self.assertEqual(sCreatedWorkspaceId, sFoundWorkspaceId)
            asWorkspaces = wasdi.getWorkspaces()
            bFound = any(oWs['workspaceName'] == self.m_sWorkspaceName for oWs in asWorkspaces)
            self.assertTrue(bFound)
        else:
            logging.error(f'test_01_createWorkspace: ERROR: workspace {self.m_sWorkspaceName} already found, failing')
            self.assertTrue(False)

    def test_02_openWorkspace(self):
        logging.info('test_02_openWorkspace')
        sActiveWorkspaceId = wasdi.openWorkspace(self.m_sWorkspaceName)
        sFoundWorkspaceId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)

        self.assertEqual(sActiveWorkspaceId, wasdi.getActiveWorkspaceId())
        self.assertEqual(sActiveWorkspaceId, sFoundWorkspaceId)

    def test_03_searchEOImages(self):
        logging.info('test_03_searchEOImages')
        aoSearchResult = self.searchImages()

        asNames = [aoItem['title'] for aoItem in aoSearchResult]
        sFile1 = wasdi.getParameter('file1.name')
        logging.info(sFile1)
        bContained1 = sFile1 in asNames
        self.assertTrue(bContained1)
        self.assertTrue(wasdi.getParameter('file1.name') in asNames)

    def test_03_01_searchProduct(self):
        sPlatform = "S2",
        sDateFrom = "2021-05-14",
        sDateTo = "2021-05-21",
        sProvider = "AUTO",

        oBoundingBox = {
            "northEast": {
                "lat": 46.69,
                "lng": 13.93
            },
            "southWest": {
                "lat": 45.60,
                "lng": 12.44
            }
        }

        aoResults = wasdi.searchEOImages(sPlatform=sPlatform,
                                         sDateFrom=sDateFrom,
                                         sDateTo=sDateTo,
                                         fULLat=None, fULLon=None, fLRLat=None, fLRLon=None,
                                         sProductType=None, iOrbitNumber=None,
                                         sSensorOperationalMode=None, sCloudCoverage=None,
                                         sProvider=sProvider, oBoundingBox=oBoundingBox,
                                         aoParams=None, sFileName=None)
        self.assertIsNotNone(aoResults)
        self.assertTrue(len(aoResults) > 0)

    def test_04_importProductList(self):

        logging.info("importProductListWithMaps")

        aoSearchResult = self.searchImages()

        sFile1 = wasdi.getParameter('file1.name')
        sFile2 = wasdi.getParameter('file2.name')
        self.assertTrue(sFile1 in [aoItem['title'] for aoItem in aoSearchResult])
        self.assertTrue(sFile2 in [aoItem['title'] for aoItem in aoSearchResult])

        aoProductsToImport = [
            aoItem for aoItem in aoSearchResult if aoItem['title'] == sFile1 or aoItem['title'] == sFile2
        ]

        asStatuses = wasdi.importProductList(aoProductsToImport)
        self.assertListEqual(asStatuses, ['DONE', 'DONE'])
        asImagesInWorkspace = wasdi.getProductsByActiveWorkspace()
        self.assertTrue(sFile1 + '.zip' in asImagesInWorkspace)
        self.assertTrue(sFile2 + '.zip' in asImagesInWorkspace)

    def test_05_deleteImage(self):
        logging.info('test_05_deleteImage')
        sFileName = wasdi.getParameter('file1.name') + ".zip"
        sResponse = wasdi.deleteProduct(sFileName)
        bDeleted = bool(sResponse)
        self.assertTrue(bDeleted)
        asImages = wasdi.getProductsByActiveWorkspace()
        self.assertFalse(sFileName in asImages)

    def test_06_executeWorkflow(self):

        aoImageList = wasdi.getProductsByActiveWorkspace()

        availableImageName = self.sTestFile2Name + ".zip"
        self.assertTrue(aoImageList.__contains__(availableImageName))

        aoWorkflows = wasdi.getWorkflows()
        ndvi = ""
        for wf in aoWorkflows:
            if wf["name"] == "ndvi":
                ndvi = wf

        actualResponse = wasdi.executeWorkflow(availableImageName, availableImageName + "_preproc.tif", ndvi["name"])

        self.assertEquals(actualResponse, "DONE")
        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertTrue(aoImageList.__contains__(availableImageName))
        self.assertTrue(aoImageList.__contains__(availableImageName + "_preproc.tif"))

        wasdi.deleteProduct(availableImageName)
        wasdi.deleteProduct(availableImageName + "_preproc.tif")

        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertFalse(aoImageList.__contains__(availableImageName))
        self.assertFalse(aoImageList.__contains__(availableImageName + "_preproc.tif"))

    def test_07_addFileToWASDI(self):
        # logging.info("Test - addFileToWasdi")

        # copy file from resources folder
        # lux1.tif
        sResourcePath = "./resources/images/lux1.tif"
        sResourcePath = os.path.abspath(sResourcePath)
        self.assertTrue(exists(sResourcePath))
        sWasdiPath = f'{wasdi.getPath()}'
        Path(sWasdiPath).mkdir(parents=True, exist_ok=True)
        self.assertTrue(exists(sWasdiPath))
        oDest = shutil.copy(sResourcePath, sWasdiPath)
        logging.info(f'test_07_addFileToWASDI: copy result: {oDest}')

        # lux2.tif
        sResourcePath = "./resources/images/lux2.tif"
        sResourcePath = os.path.abspath(sResourcePath)
        self.assertTrue(exists(sResourcePath))
        sWasdiPath = f'{wasdi.getPath()}'
        Path(sWasdiPath).mkdir(parents=True, exist_ok=True)
        self.assertTrue(exists(sWasdiPath))
        oDest = shutil.copy(sResourcePath, sWasdiPath)
        logging.info(f'test_07_addFileToWASDI: copy result: {oDest}')

        # add file 1
        sStatus = wasdi.addFileToWASDI("lux1.tif")
        logging.info(f"status: {sStatus}")
        self.assertEqual("DONE", sStatus)

        sStatus = wasdi.addFileToWASDI("lux2.tif")
        logging.info(f"status: {sStatus}")
        self.assertEqual("DONE", sStatus)

        aoImageList = wasdi.getProductsByActiveWorkspace()

        self.assertTrue("lux1.tif" in aoImageList)
        self.assertTrue("lux2.tif" in aoImageList)

    def test_08_mosaic(self):
        logging.info("Test - mosaic")

        asProductsInWorkspace = wasdi.getProductsByActiveWorkspace()
        asInput = ["lux1.tif", "lux2.tif"]
        # check preconditions
        for sFile in asInput:
            self.assertTrue( sFile in asProductsInWorkspace)

        sOutputFile = "mosaic.tif"

        status = wasdi.mosaic(asInput, sOutputFile)
        self.assertEqual("DONE", status)

        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertTrue("lux1.tif" in aoImageList)
        self.assertTrue("lux2.tif" in aoImageList)
        self.assertTrue("mosaic.tif" in aoImageList)

    def test_09_multiSubset(self):
        logging.info("Test - multisubset")

        sInputFile = "mosaic.tif"
        asOutputFiles = ["subset1.tif", "subset2.tif"]

        adLatN = [48.9922701083264869, 48.9863412274512982]
        adLonW = [5.9689794485811358, 6.0399463560265785]
        adLatS = [48.9182489289150411, 48.9256151142448203]
        adLonE = [6.0406650082538738, 6.1136082093243793]

        status = wasdi.multiSubset(sInputFile, asOutputFiles, adLatN, adLonW, adLatS, adLonE)
        self.assertEqual("DONE", status)

        availableImages = wasdi.getProductsByActiveWorkspace()

        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertTrue("lux1.tif" in aoImageList)
        self.assertTrue("lux2.tif" in aoImageList)
        self.assertTrue("mosaic.tif" in aoImageList)
        self.assertTrue("subset1.tif" in aoImageList)
        self.assertTrue("subset2.tif" in aoImageList)

    def test_10_executeProcessor(self):
        logging.info("Test - executeProcessor")

        sProcName = "hellowasdiworld";
        asParams = {"NAME": "Tester"}
        sProcessObjId = wasdi.asynchExecuteProcessor(sProcName, asParams);
        sStatus = wasdi.waitProcess(sProcessObjId);
        self.assertEqual("DONE", sStatus);

        logging.info("payload");

        wasdi.getProcessorPayload(sProcessObjId);

        return

    def test_11_waitProcesses_wrongInputTypeExitsWithEmptyList(self):
        aoWrongList = [['a', 'b', 'c'], ['d', 'e', 'f']]
        asReturnList = wasdi.waitProcesses(aoWrongList)
        self.assertEqual(len(asReturnList), 0)

    def test_waitProcesses_mixedWrongInputListExitsWithEmptyList(self):
        sCurrentWorkspaceId =wasdi.getActiveWorkspaceId()
        sWsToOpen = wasdi.getParameter('wsWithProcsAndProds')
        wasdi.openWorkspaceById(sWsToOpen)
        sProcessObjId = wasdi.getParameter('validProcessObjId')
        asMixedList = ['a', 'b', sProcessObjId]

        asReturnList = wasdi.waitProcesses(asMixedList)

        self.assertEqual(len(asReturnList), 1)

        #reset to initial workspace
        wasdi.openWorkspaceById(sCurrentWorkspaceId)


    def test_getProductDetailsByWorkspaceId_goodId(self):
        sCurrentWorkspaceId = wasdi.getActiveWorkspaceId()
        sWsToOpen = wasdi.getParameter('wsWithProcsAndProds')
        wasdi.openWorkspaceById(sWsToOpen)

        aoReturnList = wasdi.getDetailedProductsByWorkspaceId()
        self.assertEqual(len(aoReturnList), 1)

        # reset to initial workspace
        wasdi.openWorkspaceById(sCurrentWorkspaceId)

    def test_publishBand_good(self):
        #wasdi.init("./resources/config.json")
        sCurrentWorkspaceId = wasdi.getActiveWorkspaceId()
        sWsToOpen = wasdi.getParameter('wsWithBandsToPublish')
        wasdi.openWorkspaceById(sWsToOpen)

        sFileName = wasdi.getParameter('fileName')
        sBand = wasdi.getParameter('bandName')

        oReturnedObject = wasdi.asynchPublishBand(sFileName, sBand)
        self.assertTrue("payload" in oReturnedObject)
        self.assertTrue(oReturnedObject["payload"] is not None)

        if isinstance(oReturnedObject["payload"], str):
            # then the publish band API returned a processObjId and we can wait on it
            sStatus = wasdi.waitProcess(oReturnedObject["payload"])
            self.assertEqual("DONE", sStatus)
            # let's do it again to get the JSON
            oReturnedObject = wasdi.asynchPublishBand(sFileName, sBand)

            # at this point the API should have returned a json, and we can try to see if the band is published
            self.assertTrue(isinstance(oReturnedObject["payload"], dict))
            # bandName
            self.assertTrue("bandName" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["bandName"])
            self.assertTrue(oReturnedObject["payload"]["bandName"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["bandName"]) > 0)
            # boundingBox
            self.assertTrue("boundingBox" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["boundingBox"])
            self.assertTrue(oReturnedObject["payload"]["boundingBox"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["boundingBox"]) > 0)
            # geoserverBoundingBox
            self.assertTrue("geoserverBoundingBox" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["geoserverBoundingBox"])
            self.assertTrue(oReturnedObject["payload"]["geoserverBoundingBox"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["geoserverBoundingBox"]) > 0)
            # geoserverUrl
            self.assertTrue("geoserverUrl" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["geoserverUrl"])
            self.assertTrue(oReturnedObject["payload"]["geoserverUrl"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["geoserverUrl"]) > 0)
            # productName
            self.assertTrue("productName" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["productName"])
            self.assertTrue(oReturnedObject["payload"]["productName"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["productName"]) > 0)
            # workspaceId
            self.assertTrue("workspaceId" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["workspaceId"])
            self.assertTrue(oReturnedObject["payload"]["workspaceId"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["workspaceId"]) > 0)
            # layerId
            self.assertTrue("layerId" in oReturnedObject["payload"])
            self.assertTrue(oReturnedObject["payload"]["layerId"])
            self.assertTrue(oReturnedObject["payload"]["layerId"] is not None)
            self.assertTrue(len(oReturnedObject["payload"]["layerId"]) > 0)

        print(oReturnedObject)

        # reset to initial workspace
        wasdi.openWorkspaceById(sCurrentWorkspaceId)

    def test_getLayerWMS_good(self):
        #wasdi.init("./resources/config.json")
        sCurrentWorkspaceId = wasdi.getActiveWorkspaceId()
        sWsToOpen = wasdi.getParameter('wsWithBandsToPublish')
        wasdi.openWorkspaceById(sWsToOpen)

        sProductName = wasdi.getParameter('fileName')
        sBand = wasdi.getParameter('bandName')

        oWMSResponse = json.loads(wasdi.getlayerWMS(sProductName, sBand))
        print(oWMSResponse)
        self.assertIsNotNone(oWMSResponse)
        self.assertTrue("server" in oWMSResponse)
        self.assertTrue("layerId" in oWMSResponse)


    #
    # test begin here
    #

    # def test_createAndDeleteWorkspace(self):
    #     '''
    #     Creates and delete a workspace by using its Id !
    #     returns the id itself, if deleted
    #     '''
    #     sWsIdAtCreation = wasdi.createWorkspace(self.m_sWorkspaceName)
    #     self.assertIsNotNone(sWsIdAtCreation)
    #     wsId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
    #     self.assertEqual(wsId, wasdi.getActiveWorkspaceId())
    #     asProducts = wasdi.getProductsByActiveWorkspace()
    #     self.assertEqual(0, len(asProducts))
    #     self.assertTrue(wasdi.deleteWorkspace(wsId))
    #
    # def test_ListProduct(self):
    #     self.clearProducts()
    #     self.assertEqual(0, wasdi.getProductsByActiveWorkspace())
    #
    # def test_PrintStatus(self):
    #     wasdi.printStatus()
    #
    # def test_hello(self):
    #     self.assertTrue(wasdi.hello().__contains__("Hello Wasdi"))
    #
    # def test_GetListOfWorkspace(self):
    #     '''assume that, at least, the testing user has a workspace'''
    #     self.assertTrue(len(wasdi.getWorkspaces()) > 0)
    #
    # def test_getWorkspaceOwnerByName(self):
    #     '''
    #     Creates a workspace checks its owner and delete the workspace by using its Id !
    #     '''
    #     oOwner = wasdi.getUser();
    #     self.assertIsNotNone(wasdi.createWorkspace(self.m_sWorkspaceName))
    #     wsId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
    #     self.assertEqual(wasdi.getWorkspaceOwnerByName(self.m_sWorkspaceName), oOwner)
    #     wasdi.deleteWorkspace(wsId)
    #
    # def test_getWorkspaceOwnerById(self):
    #     '''
    #     Creates a workspace checks its owner and delete the workspace by using its Id !
    #     '''
    #     oOwner = wasdi.getUser();
    #     self.assertIsNotNone(wasdi.createWorkspace(self.m_sWorkspaceName))
    #     wsId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
    #     self.assertEqual(wasdi.getWorkspaceOwnerByWsId(wsId), oOwner)
    #     wasdi.deleteWorkspace(wsId)
    #
    # def test_openWorkspaceById(self):
    #     '''
    #     Creates a workspace and use it, the delete it
    #     '''
    #     oOwner = wasdi.getUser();
    #     self.assertIsNotNone(wasdi.createWorkspace(self.m_sWorkspaceName))
    #     wsId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
    #     self.assertEqual(wasdi.openWorkspaceById(wsId), wsId)
    #     wasdi.deleteWorkspace(wsId)
    #
    # def test_openWorkspaceByName(self):
    #     '''
    #     Creates a workspace and use it, the delete it
    #     '''
    #     oOwner = wasdi.getUser();
    #     self.assertIsNotNone(wasdi.createWorkspace(self.m_sWorkspaceName))
    #     wsId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
    #     self.assertEqual(wasdi.openWorkspace(self.m_sWorkspaceName), wsId)
    #     wasdi.deleteWorkspace(wsId)
    #
    # def test_getProductListByWorkspace(self):
    #     self.assertTrue(len(wasdi.getProductsByActiveWorkspace()) > 0)
    #
    # def test_fileExistOnWasdi(self):
    #     '''Warning the setup for this test is not fully automated'''
    #     on_wasdi = wasdi.fileExistsOnWasdi("S1B_IW_RAW__0SDV_20211018T170617_20211018T170649_029193_037BE3_D53A")
    #     self.assertTrue(on_wasdi != False)
    #
    # def test_waitProcess(self):
    #     '''Warning the setup for this test is not fully automated'''
    #     paramsDict = {"name": "WASDI"}
    #     processID = wasdi.executeProcessor("hellowasdiworld", paramsDict)
    #     if processID is None or processID == "":
    #         sStatus = "somethingisbroken"
    #     else:
    #         sStatus = wasdi.waitProcess(processID)
    #     self.assertTrue(sStatus in {"DONE", "STOPPED", "ERROR"})
    #
    # def test_importProduct(self):
    #     '''
    #     This test checks the capabilities of WASDI to gather
    #     images. This implies the search and the download of at least
    #     one image on a 10 day timespan
    #     '''
    #
    #     oFromDate = date.today() - timedelta(days=15)
    #     sFromDate = oFromDate.strftime('%Y-%m-%d')
    #
    #     oToDate = date.today() - timedelta(days=5)
    #     sToDate = oToDate.strftime('%Y-%m-%d')
    #
    #     eo_images = wasdi.searchEOImages("S2", sFromDate, sToDate,
    #                                      None, None, None, None, None, None, None, None,
    #                                      "LSA", "46.69,12.44,45.60,13.93")
    #     self.assertIsNotNone(eo_images)
    #     self.assertTrue(len(eo_images) > 0)
    #     # In case assertion isn't violated
    #     productName = eo_images[0].get("title")
    #     if (len(eo_images) > 0):
    #         wasdi.importProduct(eo_images[0], "LSA")
    #         # The file should be available in the active workspace
    #         # this is problematic -> it requires a bit of time to complete so need
    #         self.assertTrue(wasdi.getProductsByActiveWorkspace().__contains__(productName))
    #
    # def test_CheckProdcutExistOnWasdi(self):
    #     aoProductList = wasdi.getProductsByActiveWorkspace()
    #     self.assertTrue("S2B_MSIL1C_20211015T101029_N0301_R022_T32TQR_20211015T121549.zip" in aoProductList)
    #
    # def test_WaitForProcessesEmptyList(self):
    #     ''' Checks that, if the list passed to the methods is empty,
    #      liveness conditions holds '''
    #     wasdiProcIDList = []  # creates and empty list of procIDs
    #     wasdi.waitProcesses(wasdiProcIDList)
    #     self.assertTrue(True);  # find a better way to check timeout with assertions
    #
    # def test_setProcessPayload(self):
    #     '''Warning the setup for this test is not fully automated.
    #     It requires the sharing of the hellowasdiworld application '''
    #     pid = wasdi.executeProcessor("hellowasdiworld", "{\"name\": \"WASDI\"}")
    #     self.assertTrue(wasdi.setProcessPayload(pid, "{ \"name\" : \"TestPayload\"}") != "")
    #     sStatus = wasdi.waitProcess(pid)

    '''def test_addFileToWasdi(self):
        wasdi.addFileToWASDI("C:\\Users\\m.menapace.FADEOUT\\Documents\\Fadeout\\WASDI\\libraries\\waspy\\test\\256x256.tif")
        self.assertTrue(wasdi.getProductsByActiveWorkspace().__contains__("C:\\Users\\m.menapace.FADEOUT\\Documents\\Fadeout\\WASDI\\libraries\\waspy\\test\\256x256.tif"))
        wasdi.deleteProduct("256x256.tif")
    '''
    '''
    def cleanUpWorkSpace(self):

        Util methods to clean up the current workspace

        # clean up workspace
        for product in wasdi.getProductsByActiveWorkspace():
            wasdi.deleteProduct(product)
    '''


if __name__ == '__main__':
    WaspyIntegrationTests.initTest()
    unittest.main()
