'''
Unit Test class
A config file is required in the current directory
'''

import unittest
from datetime import date, timedelta

import wasdi


class testWaspy(unittest.TestCase):
    '''
    This workspace name is used for creation and deletion of temporary empty workspaces
    To checks products list and other test related to non-empty WS, this test set use the
    workspace initialized a the beginning of each test.
    To test everything please create a non empty ws and set it in config.json for initialization
    '''
    CONST_WORKSPACE_NAME = "OS2TIhDJyRlOGgzV"

    '''
    TODO fix wasdi.init ! if config file doesn't exist return false !
    def test_InitBroken(self):
        self.assertFalse(wasdi.init("./nofile.txt"))
    '''

    def setUp(self):
        # init library
        wasdi.init("./config.json")
        wasdi.setBaseUrl("https://test.wasdi.net/wasdiwebserver/rest")
        wasdi.setWorkspaceBaseUrl("https://test.wasdi.net/wasdiwebserver/rest")

    def test_createAndDeleteWorkspace(self):
        '''
        Creates and delete a workspace by using its Id !
        returns the id itself, if deleted
        '''
        self.assertIsNotNone(wasdi.createWorkspace(self.CONST_WORKSPACE_NAME))
        wsId = wasdi.getWorkspaceIdByName(self.CONST_WORKSPACE_NAME)
        self.assertEqual(True, wasdi.deleteWorkspace(wsId))

    def test_ListProduct(self):
        workspace_id = wasdi.getActiveWorkspaceId()
        leng = len(wasdi.getProductsByActiveWorkspace())
        self.assertTrue(leng > 0);

    def test_PrinStatus(self):
        wasdi.printStatus()

    def test_SetVerbose(self):
        wasdi.setVerbose(True)
        self.assertTrue(wasdi.getVerbose())

    def test_hello(self):
        self.assertTrue(wasdi.hello().__contains__("Hello Wasdi"))

    def test_GetListOfWorkspace(self):
        '''assume that, at least, the testing user has a workspace'''
        self.assertTrue(len(wasdi.getWorkspaces()) > 0)

    def test_getWorkspaceOwnerByName(self):
        '''
        Creates a workspace checks its owner and delete the workspace by using its Id !
        '''
        oOwner = wasdi.getUser();
        self.assertIsNotNone(wasdi.createWorkspace(self.CONST_WORKSPACE_NAME))
        wsId = wasdi.getWorkspaceIdByName(self.CONST_WORKSPACE_NAME)
        self.assertEqual(wasdi.getWorkspaceOwnerByName(self.CONST_WORKSPACE_NAME), oOwner)
        wasdi.deleteWorkspace(wsId)

    def test_getWorkspaceOwnerById(self):
        '''
        Creates a workspace checks its owner and delete the workspace by using its Id !
        '''
        oOwner = wasdi.getUser();
        self.assertIsNotNone(wasdi.createWorkspace(self.CONST_WORKSPACE_NAME))
        wsId = wasdi.getWorkspaceIdByName(self.CONST_WORKSPACE_NAME)
        self.assertEqual(wasdi.getWorkspaceOwnerByWsId(wsId), oOwner)
        wasdi.deleteWorkspace(wsId)

    def test_openWorkspaceById(self):
        '''
        Creates a workspace and use it, the delete it
        '''
        oOwner = wasdi.getUser();
        self.assertIsNotNone(wasdi.createWorkspace(self.CONST_WORKSPACE_NAME))
        wsId = wasdi.getWorkspaceIdByName(self.CONST_WORKSPACE_NAME)
        self.assertEqual(wasdi.openWorkspaceById(wsId), wsId)
        wasdi.deleteWorkspace(wsId)

    def test_openWorkspaceByName(self):
        '''
        Creates a workspace and use it, the delete it
        '''
        oOwner = wasdi.getUser();
        self.assertIsNotNone(wasdi.createWorkspace(self.CONST_WORKSPACE_NAME))
        wsId = wasdi.getWorkspaceIdByName(self.CONST_WORKSPACE_NAME)
        self.assertEqual(wasdi.openWorkspace(self.CONST_WORKSPACE_NAME), wsId)
        wasdi.deleteWorkspace(wsId)

    def test_getProductListByWorkspace(self):
        self.assertTrue(len(wasdi.getProductsByActiveWorkspace()) > 0)

    def test_fileExistOnWasdi(self):
        '''Warning the setup for this test is not fully automated'''
        on_wasdi = wasdi.fileExistsOnWasdi("S1B_IW_RAW__0SDV_20211018T170617_20211018T170649_029193_037BE3_D53A")
        self.assertTrue(on_wasdi != False)

    def test_waitProcess(self):
        '''Warning the setup for this test is not fully automated'''
        paramsDict = {"name": "WASDI"}
        processID = wasdi.executeProcessor("hellowasdiworld", paramsDict)
        if processID is None or processID == "":
            sStatus = "somethingisbroken"
        else:
            sStatus = wasdi.waitProcess(processID)
        self.assertTrue(sStatus in {"DONE", "STOPPED", "ERROR"})

    def test_importProduct(self):
        '''
        This test checks the capabilities of WASDI to gather
        images. This implies the search and the download of at least
        one image on a 10 day timespan
        '''

        oFromDate = date.today() - timedelta(days=15)
        sFromDate = oFromDate.strftime('%Y-%m-%d')

        oToDate = date.today() - timedelta(days=5)
        sToDate = oToDate.strftime('%Y-%m-%d')

        eo_images = wasdi.searchEOImages("S2", sFromDate, sToDate,
                                             None, None, None, None, None, None, None, None,
                                             "LSA", "46.69,12.44,45.60,13.93")
        self.assertIsNotNone(eo_images)
        self.assertTrue(len(eo_images) > 0)
        # In case assertion isn't violated
        productName = eo_images[0].get("title")
        if(len(eo_images) > 0):
            wasdi.importProduct(eo_images[0], "LSA")
            # The file should be available in the active workspace
            self.assertTrue(wasdi.getProductsByActiveWorkspace().__contains__(productName))





    def test_setProcessPayload(self):
        '''Warning the setup for this test is not fully automated.
        It requires the sharing of the hellowasdiworld application '''
        pid = wasdi.executeProcessor("hellowasdiworld", "{\"name\": \"WASDI\"}")
        self.assertTrue(wasdi.setProcessPayload(pid, "{ \"name\" : \"TestPayload\"}") != "")
        sStatus = wasdi.waitProcess(pid)

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
    testWaspy.initTest();
    unittest.main()
