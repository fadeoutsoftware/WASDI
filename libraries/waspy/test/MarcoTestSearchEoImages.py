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


class TestSearch(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        wasdi.init("./config.json")
        wasdi.setBaseUrl("https://test.wasdi.net/wasdiwebserver/rest")

    def test_searchProduct(self, sPlatform="S2",
                           sDateFrom="2021-05-14",
                           sDateTo="2021-05-21",
                           oProvider="LSA",
                           oBoundingBox="46.69,12.44,45.60,13.93"):
        eo_images = wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, None, None, None, None, None, None, None, None,
                                         oProvider, oBoundingBox)
        self.assertIsNotNone(eo_images)
        self.assertTrue(len(eo_images) > 0)