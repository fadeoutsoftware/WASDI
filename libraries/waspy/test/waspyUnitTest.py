'''
Unit Test class
A config file is required in the current directory
'''

import unittest
import wasdi

'''class searchParameters:
    sPlatform = "S1"  # S1|S2|VIIRS|L8|ENVI
    sDateFrom = "2021-05-14"
    sDateTo = "2021-05-21"
    fULLat = ""
    fULLon = ""
    fLRLat = ""
    fLRLon = ""
    sProductType = ""
    iOrbitNumber = ""
    sSensorOperationalMode = ""
    sCloudCoverage = ""
    sProvider = "LSA"  # LSA|ONDA|CREODIAS|SOBLOO|VIIRS|SENTINEL
    oBoundingBox = "46.69,12.44,45.60,13.93"

    def __init__(self):
        self.sPlatform = "S1"
'''


class MarcoTestSearchEOImages(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        wasdi.init("./resources/config.json")

    def test_SetVerbose(self):
        wasdi.setVerbose(True)
        self.assertTrue(wasdi.getVerbose())



    # def test_getProductDetailsByWorkspaceId_wrongId(self):
    #     sId = 'bibillo'
    #     wasdi.openWorkspaceById(sId)
    #     aoReturnList = wasdi.getDetailedProductsByWorkspaceId()
    #     self.assertEqual(len(aoReturnList), 0)


