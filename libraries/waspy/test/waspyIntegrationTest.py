'''
Unit Test class
A config file is required in the current directory
The test case are executed in the same order as they are implemented on this file.
The scenario tested follows the tutorial of wasdi till test number 7, after that
some more functionalities are tested.
The execution MUST be done with all tests in order to work correctly.
The single execution of some test will fail because the state i which they need to be is a result
of previous test.
'''
import shutil
import string
import random
import unittest
from datetime import date, timedelta
import logging

import wasdi

from shutil import copyfile

unittest.TestLoader.sortTestMethodsUsing = None


class WaspyIntegrationTests(unittest.TestCase):
    '''
    This workspace name is used for creation and deletion of temporary empty workspaces
    To checks products list and other test related to non-empty WS, this test set use the
    workspace initialized a the beginning of each test.
    To test everything please create a non empty ws and set it in config.json for initialization
    '''

    # todo create unique workspace name each time
    # m_sWorkspaceName = ""
    # m_iWorkspaceNameLen = 64

    # todo test writing username and password from stdin
    # def test_InitBroken(self):
    #     bInitOk = wasdi.init("./nofile.txt");
    #     self.assertFalse(bInitOk)

    @classmethod
    def setUpClass(cls):
        cls.m_iWorkspaceNameLen = 128
        print(cls.m_iWorkspaceNameLen)
        cls.m_sWorkspaceName = cls.randomString(cls.m_iWorkspaceNameLen)

        cls.sTestFile1Name = "S2A_MSIL1C_20201008T102031_N0209_R065_T32TMR_20201008T123525"
        cls.sTestFile2Name = "S2B_MSIL1C_20201013T101909_N0209_R065_T32TMR_20201018T165151"

        wasdi.init("./config.json")

        cls.readBoundingBox(cls)

        URL = "https://test.wasdi.net/wasdiwebserver/rest"
        print("swapping Base url to " + URL)
        wasdi.setBaseUrl(URL)
        wasdi.setWorkspaceBaseUrl("https://test.wasdi.net/wasdiwebserver/rest")
        wasdi.getWorkspaceBaseUrl()

        # cls.clearWorkspaces(cls)

    @classmethod
    def randomString(cls, size=6, chars=string.ascii_uppercase + string.digits):
        sRandom = ''.join(random.choice(chars) for _ in range(size))
        return sRandom

    def clearWorkspaces(self):
        aoWorkspaces = wasdi.getWorkspaces()
        for oWorkspace in aoWorkspaces:
            try:
                print(f"clearWorkspaces: {oWorkspace['workspaceName']}")
                if oWorkspace['workspaceName'] == self.m_sWorkspaceName:
                    print(f"clearWorkspaces: FOUND!")
                    sReadId = oWorkspace['workspaceId']
                    bDeleted = wasdi.deleteWorkspace(sReadId)
                    if not bDeleted:
                        print(f'clearWorkspaces: could not delete workspace: {sReadId}')
                    bDone = False
            except Exception as oE:
                print(f'clearWorkspaces: {type(oE)}: {oE}, skipping iteration')

    @classmethod
    def clearProducts(cls):
        asProducts = wasdi.getProductsByActiveWorkspace()
        for sProduct in asProducts:
            wasdi.deleteProduct(sProduct)

    def readBoundingBox(self):
        sBbox = wasdi.getParameter("test.bounding.box")
        try:
            asBbox = sBbox.split(",")
            self.dULLat = asBbox[0]
            self.dULLon = asBbox[1]
            self.dLRLat = asBbox[2]
            self.dLRLon = asBbox[3]
        except Exception as oE:
            print(f'readBoundingBox: {type(oE)}: {oE}')

    def test_01_createWorkspace(self):
        print('test_01_createWorkspace')
        sCreatedWorkspaceId = wasdi.createWorkspace(self.m_sWorkspaceName)
        sFoundWorkspaceId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
        self.assertEqual(sCreatedWorkspaceId, sFoundWorkspaceId)

    def test_02_openWorkspace(self):
        print('test_02_openWorkspace')
        sActiveWorkspaceId = wasdi.openWorkspace(self.m_sWorkspaceName)
        sFoundWorkspaceId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)

        self.assertEqual(sActiveWorkspaceId, wasdi.getActiveWorkspaceId())
        self.assertEqual(sActiveWorkspaceId, sFoundWorkspaceId)

    def test_03_searchEOImages(self):
        print('test_03_searchEOImages')
        aoSearchResult = wasdi.searchEOImages(
            wasdi.getParameter('platform'),
            wasdi.getParameter('date'),
            wasdi.getParameter('date'),
            self.dULLat,
            self.dULLon,
            self.dLRLat,
            self.dLRLon,
            wasdi.getParameter('product.type'),
            None,  # iOrbitNumber
            None,  # sSensorOperationalMode
            wasdi.getParameter('max.cloud')
        )

        asNames = [aoItem['title'] for aoItem in aoSearchResult]

        self.assertTrue(wasdi.getParameter('file1.name') in asNames)
        self.assertTrue(wasdi.getParameter('file1.name') in asNames)

    def test_04_importProductListWithMaps(self):

        logging.info("importProductListWithMaps")

        images = wasdi.searchEOImages(self.sPlatform,
                                      self.sDateFrom,
                                      self.sDateTo,
                                      self.dULLat,
                                      self.dULLon,
                                      self.dLRLat,
                                      self.dLRLon,
                                      self.sProductType,
                                      self.iOrbitNumber,
                                      self.sSensorOperationalMode,
                                      self.sCloudCoverage)

        alreadyExistingImages = wasdi.getProductsByActiveWorkspace()





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
        logging.info("Test - addFileToWasdi")
        # copy file from resources folder
        shutil.copy("./resources/images/lux1.tif",
                    "./lux1.out.tif")
        status = wasdi.addFileToWASDI("lux1.tif")
        self.assertEqual("DONE", status)

        status = wasdi.addFileToWASDI("lux2.tif")
        self.assertEqual("DONE", status)

        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertFalse(aoImageList.__contains__("lux1.tif"))
        self.assertFalse(aoImageList.__contains__("lux2.tif"))

        return

    def test_08_mosaic(self):
        logging.info("Test - mosaic")
        asInput = ["lux1.tif", "lux2.tif"]
        sOutputFile = "mosaic.tif"

        status = wasdi.mosaic(asInput, sOutputFile)
        self.assertEqual("DONE", status)

        aoImageList = wasdi.getProductsByActiveWorkspace()
        self.assertFalse(aoImageList.__contains__("lux1.tif"))
        self.assertFalse(aoImageList.__contains__("lux2.tif"))
        self.assertFalse(aoImageList.__contains__("mosaic.tif"))

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

        self.assertTrue(availableImages.__contains__("lux1.tif"));
        self.assertTrue(availableImages.__contains__("lux2.tif"));
        self.assertTrue(availableImages.__contains__("mosaic.tif"));
        self.assertTrue(availableImages.__contains__("subset1.tif"));
        self.assertTrue(availableImages.__contains__("subset2.tif"));

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
    WaspyIntegrationTests.initTest();
    unittest.main()
