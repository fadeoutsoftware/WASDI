import logging
import sys
import time
import os
import json

import requests
from lxml import html
from urllib.parse import urlparse

s_sDataProviderName = 'ESA'

s_sESA_IDENTIY_MANAGEMENT_URL = "eoiam-idp.eo.esa.int"


def authenticateAccess(sDownloadLink, sUsername, sPassword):

    """
    TODO:
    Download link: e' quello che dobbiamo leggere dagli input in qualche modo
    Username = da leggere dalla configurazione wasdiConfig.json
    Password = da leggere dalla configurazione wasdiConfig.json
    """
    try:
        oHostingMachine = urlparse(
            "https://esar-ds.eo.esa.int/oads/data/SAR_IMP_1P/SAR_IMP_1PNESA20090902_100035_00000015A150_00122_75126_0000.E2").netloc

        # requesting access to the machine storing the products
        oAccessResponse = requests.get(f"https://{oHostingMachine}/oads/access/login", proxies={})

        #extracting the cookies from the response
        oCookies = oAccessResponse.cookies
        oRedirectResponseList = oAccessResponse.history
        for oRedirectResponse in oRedirectResponseList:
            oCookies = requests.cookies.merge_cookies(oCookies, oRedirectResponse.cookies)
        oTree = html.fromstring(oAccessResponse.content)
        # extracting the sessionDataKey from the the response
        sSessionDataKey = oTree.findall(".//input[@name = 'sessionDataKey']")[0].attrib["value"]
        # defining the payload to send to Authentication platform
        oPostRequestData = {
            "tocommonauth": "true",
            "username": sUsername,
            "password": sPassword,
            "sessionDataKey": sSessionDataKey
        }
        # making the request to Authentication platform
        oAuthenticationResponse = requests.post(
            url=f"https://{s_sESA_IDENTIY_MANAGEMENT_URL}/samlsso",
            data=oPostRequestData,
            cookies=oCookies,
            proxies={}
        )
        # parsing the response from Authentication platform
        oTree = html.fromstring(oAuthenticationResponse.content)
        # extracting the variables needed to redirect from a successful authentication to OADS
        sRelayState = oTree.findall(".//input[@name='RelayState']")[0].attrib["value"]
        sSAMLResponse = oTree.findall(".//input[@name='SAMLResponse']")[0].attrib["value"]
        sSAMLRedirectUrl = oTree.findall(".//form[@method='post']")[0].attrib["action"]

        # redirect back to the machine storing the products
        oPostData = {
            "RelayState": sRelayState,
            "SAMLResponse": sSAMLResponse
        }
        oRedirectResponse = requests.post(
            url=sSAMLRedirectUrl,
            data=oPostData,
            proxies={}
        )
        oCookies2 = oRedirectResponse.cookies
        oRedirectResponseList = oRedirectResponse.history
        for oRedirectResponse in oRedirectResponseList:
            oCookies2 = requests.cookies.merge_cookies(oCookies2, oRedirectResponse.cookies)
        return oCookies2
    except Exception as oEx:
        logging.error(f"[ERROR] authenticateAccess: {oEx}")
    return None


def  executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    try:

        if not os.path.isfile(sInputFilePath) or not os.path.isfile(sOutputFilePath):
            logging.warning('executeDownloadFile: input or output file not found')
            sys.exit(1)
        try:
            with open(sInputFilePath) as oJsonFile:
                aoInputParameters = json.load(oJsonFile)
        except Exception as oEx:
            logging.error(f'executeDownloadFile: error reading the input file: {sInputFilePath}, {oEx}')
            return sys.exit(1)

        if aoInputParameters is None:
            logging.warning(f'executeDownloadFile: there was an error reading the input file: {sWasdiConfigFilePath}')
            sys.exit(1)

        if stringIsNullOrEmpty(sWasdiConfigFilePath):
            logging.warning(
                f'executeDownloadFile: data provider configuration is None or empty string: {sWasdiConfigFilePath}')
            sys.exit(1)

        try:
            with open(sWasdiConfigFilePath) as oWasdiConfigJsonFile:
                aoDataProviderConfig = json.load(oWasdiConfigJsonFile)
        except Exception as oEx:
            logging.warning(f'executeDownloadFile: error reading the wasdiConfig file: {sWasdiConfigFilePath}, {oEx}')
            sys.exit(1)

        if aoDataProviderConfig is None:
            logging.warning(f'executeDownloadFile: wasdiConfig file is None: {sWasdiConfigFilePath}')
            sys.exit(1)

        # find the configuration for the data provider
        aoESADataProviderConfig = None

        aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders', [])
        for oProvider in aoWasdiDataProviders:
            if oProvider['name'] == s_sDataProviderName:
                aoESADataProviderConfig = oProvider
                break

        if aoESADataProviderConfig is None:
            logging.warning(
                f"executeDownloadFile: no configuration found for {s_sDataProviderName}. Impossible to continue")
            sys.exit(1)

        sUsername = aoESADataProviderConfig['user']
        sPassword = aoESADataProviderConfig['password']

        sDownloadLink = "https://esar-ds.eo.esa.int/oads/data/SAR_IMP_1P/SAR_IMP_1PNESA20090902_100035_00000015A150_00122_75126_0000.E2" # TODO


        oAuthCookies = authenticateAccess(sDownloadLink, sUsername, sPassword)

        if oAuthCookies is None:
            logging.warning(f"[WARN] executeDownloadFile: the auth cookies are null, can not proceed with download")
            return None

        iMaxRetries = 5 # TODO: how many?
        sFileName = "SAR_IMP_1PNESA20090902_100035_00000015A150_00122_75126_0000.E2" # TODO: change this
        logging.info(f"[INFO] executeDownloadFile: requesting {sDownloadLink}")

        for iAttempt in range(iMaxRetries):
            try:
                logging.info(f"[INFO] executeDownloadFile: download attempt number {str(iAttempt)}")
                oDownloadResponse = requests.get(
                    sDownloadLink,
                    cookies=oAuthCookies,
                    proxies={},
                    stream=True
                )
                oDownloadResponse.raise_for_status() # Raise an error for bad responses (4xx and 5xx)

                # downloading the product
                sDownloadFilePath = "try_download" # TODO: add the path to the download file
                with open(sDownloadFilePath, "wb") as oDownloadFile:
                    for oChunk in oDownloadResponse:
                        oDownloadFile.write(oChunk)
                logging.info(f"[INFO] executeDownloadFile: product downloaded at {sDownloadFilePath}")
                break
            except requests.exceptions.RequestException as oEx:
                logging.error(f"[ERROR] executeDownloadFile: failed to download the file {sFileName}. Error: {oEx}")
                time.sleep(2)

    except Exception as oEx:
        logging.error(f"[ERROR] executeDownloadFile: error {oEx}")

    return None

def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""

if __name__ == '__main__':
    # quello che viene ricevuto e' gia' il download link nella sua versione "raw", niente altro da fare

    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    sOperation = None
    sInputFile = None
    sOutputFile = None

    asArgs = sys.argv

    try:
        sOperation = "2"
        sInputFile = "C:/temp/8318dbb9-2d22-4fee-9381-f4721c5910ac"
        sOutputFile = "C:/temp/89bc0c86-40fd-4cad-860a-07a804a889cd"
        sWasdiConfigFile = "C:/temp/wasdi/wasdiLocalTESTConfig.json"

        """
        if asArgs is None or len(asArgs) < 1:
            logging.error("__main__: no arguments passed to the data provider")
            sys.exit(1)

        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sWasdiConfigFile = asArgs[4]
        """

        # first argument asArgs[0] is the name of the file - we are not interested in it
        logging.debug('__main__: operation ' + sOperation)
        logging.debug('__main__: input file ' + str(sInputFile))
        logging.debug('__main__: output file: ' + str(sOutputFile))
        logging.debug('__main__: wasdi config path: ' + str(sWasdiConfigFile))

    except Exception as oE:
        logging.error('__main__: Exception ' + str(oE))
        sys.exit(1)

    if sOperation == "0":
        logging.debug('__main__: operation EXECUTE AND RETRIEVE not implemented. Script will exit')
        sys.exit(1)
    elif sOperation == "1":
        logging.debug('__main__: operation is EXECUTE COUNT not implemented. Script will exit')
        sys.exit(1)
    elif sOperation == "2":
        logging.debug('__main__: chosen operation is DOWNLOAD')
        executeDownloadFile(sInputFile, sOutputFile, sWasdiConfigFile)
    else:
        logging.debug('__main__: unknown operation. Script will exit')
        sys.exit(1)

    sys.exit(0)
