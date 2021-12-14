#!/usr/bin/env python
# -*- coding: iso-8859-15 -*-

# courtesy of Linda Cademartori and Carola Lo Monaco, 2021

"""
The goal is to test searchEOImages function of wasdi library using mock and unittest.

Last Update : 04/09/2021
Tested with : Python 3.8
Created on 7 May 2021
"""

import unittest
from unittest.mock import Mock
from unittest.mock import patch

import requests

import wasdi
from SearchEOImagesVars import SearchEOImagesVars

__author__ = "Linda Cademartori, Carola Lo Monaco"


class TestSearchEOImages(unittest.TestCase):

    def testSearchEoImages_allParametersAreCorrect_responseIsMeaningful(self):
        """
        Test for searchEOImages() in the case that all parameters are correct
        """
        wasdi.setVerbose(False)
        self.assertEqual(
            wasdi.searchEOImages(
                sPlatform="S1", sDateFrom="2021-03-16", sDateTo="2021-03-16",
                fULLat=49.41145140497633, fULLon=1.854752483603064,
                fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                sProductType="GRD", sProvider="CREODIAS"
            ),
            SearchEOImagesVars().S1_2021_03_16_2021_03_16
        )

    def testSearchEOImages_sPlatform_None(self):
        """
        Test for searchEOImages() in the case that sPlatform = None

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(
            wasdi.searchEOImages(
                sPlatform=None, sDateFrom="2021-03-16", sDateTo="2021-03-16",
                fULLat=49.41145140497633, fULLon=1.854752483603064,
                fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                sProductType="GRD", sProvider="CREODIAS"
            ),
            []
        )

    def testSearchEOImages_sPlatform_S3(self):
        """
        Test for searchEOImages in the case that sPlatform is Wrong (different from S1 and S2)

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S3', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType="GRD", sProvider="CREODIAS"), [])

    """sProductType"""

    def testSearchEOImages_sProductType_None(self):
        """
        Test for searchEOImages() in the case that sProductType = None

        :return: ok if searchEOImages() returns Vars().S1_2021_03_16_2021_03_16_sProductType_None
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType=None, sProvider="CREODIAS"),
                         SearchEOImagesVars().S1_2021_03_16_2021_03_16_sProductType_None)

    def testSearchEOImages_sProductType_pippo(self):
        """
        Test for searchEOImages() in the case that sProductType is wrong

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType='pippo', sProvider="CREODIAS"), [])

    def testSearchEOImages_sDateFrom_None(self):
        """
        Test for searchEOImages() in the case that sDateFrom = None

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom=None, sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType='GRD', sProvider="CREODIAS"), [])

    def testSearchEOImages_sDateFrom_wrong_format(self):
        """
        Test for searchEOImages() in the case that sDateFrom is in a wrong format (different from YYYY-MM-DD)

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="16-03-2021", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType='GRD', sProvider="CREODIAS"), [])

    def testSearchEOImages_sDateTo_None(self):
        """
        Test for searchEOImages() in the case that sDateTo = None

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo=None,
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType='GRD', sProvider="CREODIAS"), [])

    def testSearchEOImages_sDateTo_wrong_format(self):
        """
        Test for searchEOImages() in the case that sDateTo is in a wrong format (different from YYYY-MM-DD)

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="16-03-2021",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType='GRD', sProvider="CREODIAS"), [])

    def testSearchEOImages_sProvider_None_LSA(self):
        """
        Test for searchEOImages() in the case that sProvider = None. By default is chosen ONDA

        :return: ok if searchEOImages() returns Vars().S1_2021_03_16_2021_03_16_LSA
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType="GRD", sProvider=None),
                         SearchEOImagesVars().S1_2021_03_16_2021_03_16_LSA)

    def testSearchEOImages_sProvider_pippo(self):
        """
        Test for searchEOImages() in the case that sProvider is wrong

        :return: ok if searchEOImages() returns an empty array
        """
        wasdi.setVerbose(False)
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType="GRD", sProvider="pippo"), [])

    """searchEOImagesAfterPost"""

    @patch.object(requests, 'post')
    def testSearchEoImagesAfterPost(self, mock_request_post):
        """
        Test for searchEOImages() mocking the request.post function

        :param mock_request_post: is the Mock object for post
        :return: ok if searchEOImages() returns Vars().S1_2021_03_16_2021_03_16_After_post
        """

        def res():
            r = Mock()
            r.status_code.return_value = 200
            r.json.return_value = SearchEOImagesVars().S1_2021_03_16_2021_03_16_oResponseJson
            return r

        wasdi.setVerbose(False)
        mock_request_post.return_value = res()
        self.assertEqual(wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                                              fULLat=49.41145140497633, fULLon=1.854752483603064,
                                              fLRLat=48.51693720209968, fLRLon=3.045217797594174,
                                              sProductType="GRD", sProvider="CREODIAS"),
                         SearchEOImagesVars().S1_2021_03_16_2021_03_16_After_post)

    @patch('requests.post')
    def testPost(self, post):
        """
        Test for request.post in searchEOImages(): verify that the post receives the correct parameters

        :param post: is the Mock object for post
        :return: ok if post is called with the correct parameters
        """
        wasdi.setVerbose(False)
        wasdi.searchEOImages(sPlatform='S1', sDateFrom="2021-03-16", sDateTo="2021-03-16",
                             fULLat=49.41145140497633, fULLon=1.854752483603064, fLRLat=48.51693720209968,
                             fLRLon=3.045217797594174,
                             sProductType="GRD", sProvider="CREODIAS")
        post.assert_called_with(
            SearchEOImagesVars().sUrl_S1_2021_03_16_2021_03_16,
            data=SearchEOImagesVars().sQueryBody_S1_2021_03_16_2021_03_16,
            headers=wasdi._getStandardHeaders())


if __name__ == '__main__':
    wasdi.init('./config.json')
    unittest.main()
