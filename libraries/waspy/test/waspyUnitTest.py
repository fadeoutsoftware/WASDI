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

    def test_searchProduct(self, sPlatform="S2",
                           sDateFrom="2021-05-14",
                           sDateTo="2021-05-21",
                           oProvider="AUTO",
                           oBoundingBox="46.69,12.44,45.60,13.93"):
        eo_images = ['dummy', 'more dummy than before']
        # eo_images = wasdi.searchEOImages(sPlatform, sDateFrom, sDateTo, None, None, None, None, None, None, None, None,
        #                                  oProvider, oBoundingBox)
        self.assertIsNotNone(eo_images)
        self.assertTrue(len(eo_images) > 0)

    def test_SetVerbose(self):
        wasdi.setVerbose(True)
        self.assertTrue(wasdi.getVerbose())

    def test_waitProcesses_wrongInputTypeExitsWithEmptyList(self):
        aoWrongList = [['a', 'b', 'c'], ['d', 'e', 'f']]
        asReturnList = wasdi.waitProcesses(aoWrongList)
        self.assertEqual(len(asReturnList), 0)

    def test_waitProcesses_wrongInputListExitsWithEmptyList(self):
        asWrongList = ['a', 'b', 'c']
        asReturnList = wasdi.waitProcesses(asWrongList)
        self.assertEqual(len(asReturnList), 0)

    # the following code contains a processObjId which belongs to specific users.
    # Make sure you can see the job, or add your own instead
    # def test_waitProcesses_mixedWrongInputListExitsWithEmptyList(self):
    #     asWrongList = ['a', 'b', 'a2d07cab-1800-4687-953b-d4c82732575a']
    #     asReturnList = wasdi.waitProcesses(asWrongList)
    #     self.assertEqual(len(asReturnList), 1)
