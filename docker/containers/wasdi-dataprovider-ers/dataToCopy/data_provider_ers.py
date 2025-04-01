import logging
import sys
import time
import os
import json

import requests
from lxml import html
from urllib.parse import urlparse
import xml.etree.ElementTree as ET

s_sDataProviderName = 'ESA'

s_sESA_IDENTIY_MANAGEMENT_URL = "eoiam-idp.eo.esa.int"


def authenticateAccess(sDownloadLink, sUsername, sPassword):
    try:
        oHostingMachine = urlparse(sDownloadLink).netloc

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

def parseProductToCheckForRetryTime(sDownloadedFilePath):
    sResponseCode = None
    iRetryTime = None

    try:
        oTree = ET.parse(sDownloadedFilePath)
        oRoot = oTree.getroot()

        # Extract the namespace
        sNamespace = "{" + oRoot.tag.split("}")[0].strip("{") + "}"

        # Find the <ResponseCode> tag
        oResponseCodeTag = oRoot.find(f"{sNamespace}ResponseCode")
        if oResponseCodeTag is not None:
            oResponseCodeTag.text.strip()
        else:
            logging.info(f"[INFO] parseProductToCheckForRetryTime: tag ResponseCode not found")

        # Find the <RetryAfter> tag
        oRetryAfterTag = oRoot.find(f"{sNamespace}RetryAfter")
        if oRetryAfterTag is not None:
            try:
                iRetryTime = int(oRetryAfterTag.text.strip())
            except ValueError as oEx:
                logging.error(f"[ERROR] parseProductToCheckForRetryTime: "
                              f"impossible to parse retry time as an integer {oEx}")
        else:
            logging.info(f"[INFO] parseProductToCheckForRetryTime: tag RetryAfter not found")

    except (ET.ParseError, FileNotFoundError, IOError):
        print("[ERROR] parseProductToCheckForRetryTime: not an XML file")
        return None

    return (sResponseCode, iRetryTime)


def executeDownloadFile(sInputFilePath, sOutputFilePath, sWasdiConfigFilePath):
    try:
        if stringIsNullOrEmpty(sInputFilePath) or stringIsNullOrEmpty(sOutputFilePath):
            logging.warning('executeDownloadFile: input or output file paths are None or empty')
            sys.exit(1)

        if not os.path.isfile(sInputFilePath) or not os.path.isfile(sOutputFilePath):
            logging.warning('executeDownloadFile: input or output file not found')
            sys.exit(1)

        if stringIsNullOrEmpty(sWasdiConfigFilePath):
            logging.warning(
                f'executeDownloadFile: config file path is a None or empty string: {sWasdiConfigFilePath}')
            sys.exit(1)

        if not os.path.isfile(sWasdiConfigFilePath):
            logging.warning(f'executeDownloadFile: config file not found at path {sWasdiConfigFilePath}')

        # reading the input parameters to get the info about the product to download
        try:
            with open(sInputFilePath) as oJsonFile:
                aoInputParameters = json.load(oJsonFile)
        except Exception as oEx:
            logging.error(f'executeDownloadFile: error reading the input file: {sInputFilePath}, {oEx}')
            return sys.exit(1)

        if aoInputParameters is None:
            logging.warning(f'executeDownloadFile: there was an error reading the input file: {sWasdiConfigFilePath}')
            sys.exit(1)

        sDownloadDirectoryPath = aoInputParameters['downloadDirectory']
        sDownloadFileName = aoInputParameters['downloadFileName']
        sDownloadUrl = aoInputParameters['url']
        iMaxRetries = aoInputParameters['maxRetry']

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

        aoWasdiDataProviders = aoDataProviderConfig.get('dataProviders')
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

        oAuthCookies = authenticateAccess(sDownloadUrl, sUsername, sPassword)

        if oAuthCookies is None:
            logging.warning(f"[WARN] executeDownloadFile: the auth cookies are null, can not proceed with download")
            return None

        logging.info(f"[INFO] executeDownloadFile: requesting {sDownloadUrl}")

        sDownloadedProductPath = ""
        for iAttempt in range(iMaxRetries):
            try:
                logging.info(f"[INFO] executeDownloadFile: download attempt number {str(iAttempt)}")
                oDownloadResponse = requests.get(
                    sDownloadUrl,
                    cookies=oAuthCookies,
                    proxies={},
                    stream=True
                )
                oDownloadResponse.raise_for_status()  # Raise an error for bad responses (4xx and 5xx)

                # downloading the product
                sDownloadFilePath = os.path.join(sDownloadDirectoryPath, sDownloadFileName)
                with open(sDownloadFilePath, "wb") as oDownloadFile:
                    for oChunk in oDownloadResponse:
                        oDownloadFile.write(oChunk)
                sDownloadedProductPath = sDownloadFilePath
                logging.info(f"[INFO] executeDownloadFile: product downloaded at {sDownloadFilePath}")

                # sometimes the ERS product is not immediately available for the download. In that case,
                # the data provider might return a file with the same name of the product, but xml content inside.
                # The xml contains information about the waiting time before retrying the download.
                # Consequently, we need to make sure that the downloaded file is actually the ERS product
                oRetryTime = parseProductToCheckForRetryTime(sDownloadFilePath)

                if oRetryTime is None:
                    # the file looks like the final ERS product, we can exit the loop
                    logging.info(f"[INFO] executeDownloadFile: the downloaded file should be the actual product")
                    break

                # at this point, the file should have been identified as an XML. We get its main content
                # to set the download in sleep
                logging.info(f"[INFO] executeDownloadFile: the content of the downloaded file seems an XML file")

                sDownloadStatus = oRetryTime[0]
                iRetryTime = oRetryTime[1]

                logging.info(f"[INFO] executeDownloadFile: download status received {sDownloadStatus}, "
                             f"with retry time {iRetryTime}.")

                os.remove(sDownloadFilePath)

                if iRetryTime is None:
                    iRetryTime = 30

                logging.info(f"[INFO] executeDownloadFile: XML file deleted. "
                             f"Going to sleep for {iRetryTime} seconds before retrying the download")

                time.sleep(iRetryTime)

            except requests.exceptions.RequestException as oEx:
                logging.error(f"[ERROR] executeDownloadFile: "
                              f"failed to download the file {sDownloadFileName}. Error: {oEx}")
                time.sleep(2)

        oRes = {
            'outputFile': sDownloadedProductPath
        }

        with open(sOutputFilePath, 'w') as oFile:
            json.dump(oRes, oFile)
            logging.debug(f"[INFO] executeDownloadFile: path to the downloaded file written in the output file {sOutputFilePath}")

    except Exception as oEx:
        logging.error(f"[ERROR] executeDownloadFile: error {oEx}")

    return None


def stringIsNullOrEmpty(sString):
    return sString is None or sString == ""


if __name__ == '__main__':

    logging.basicConfig(encoding='utf-8', format='[%(levelname)s] %(message)s', level=logging.DEBUG)

    sOperation = None
    sInputFile = None
    sOutputFile = None

    asArgs = sys.argv

    try:
        if asArgs is None or len(asArgs) < 1:
            logging.error("__main__: no arguments passed to the data provider")
            sys.exit(1)

        sOperation = asArgs[1]
        sInputFile = asArgs[2]
        sOutputFile = asArgs[3]
        sWasdiConfigFile = asArgs[4]

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
