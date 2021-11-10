'''
Unit Test class
A config file is required in the current directory
'''
import string
import random
import unittest
from datetime import date, timedelta

import wasdi

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
        cls.m_iWorkspaceNameLen = 512
        cls.m_sWorkspaceName = cls.randomString(cls.m_iWorkspaceNameLen)

        wasdi.init("./config.json")
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

    def test_01_createWorkspace(self):
        sCreatedWorkspaceId = wasdi.createWorkspace(self.m_sWorkspaceName)
        sFoundWorkspaceId = wasdi.getWorkspaceIdByName(self.m_sWorkspaceName)
        self.assertEquals(sCreatedWorkspaceId, sFoundWorkspaceId)

 




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
