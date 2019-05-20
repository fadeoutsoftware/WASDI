import wasdi
import os


print(os.getcwd())

bInitOk = False
# wasdi.__loadConfig('./config.json')
# wasdi.m_sSessionId = '04ff2155-0166-4ea0-b662-114e9717337d'
# wasdi.setUser('c.nattero@fadeout.it')
# bInitOk = wasdi.init()

bInitOk = wasdi.init('./config.json')


# 	"BASEPATH": "./",

def searchEO():
    aoSearchResult = wasdi.searchEOImages(
        sPlatform="S1",
        sDateFrom="2019-05-06",
        sDateTo="2019-05-13",
        # upper left
        dULLat="45.706179285330855",
        dULLon="3.999710083007813",
        # lower right
        dLRLat="41.541477666790286",
        dLRLon="16.128616333007816",
        sProductType=None,
        iOrbitNumber=None,
        sSensorOperationalMode=None,
        sCloudCoverage=None
    )
    print(aoSearchResult)
    return aoSearchResult


def importProduct(sProductName):
    print(sProductName)
    sImported = wasdi.importProduct(asProduct=sProductName)
    print(sImported)
    return sImported


if bInitOk:
    print('[INFO] waspy: init ok')

    wasdi.uploadFile('./a/deeeply\\nested/ciccio\\pasticcio.txt')

    sFileName = 'thisIsAFalseFileNameDoNeverMind'
    bExists = wasdi.__fileExistsOnWasdi(sFileName)
    assert(bExists is False)

    # test download no unzip
    # sFileName = 'S1B_IW_RAW__0SDV_20190506T052631_20190506T052703_016119_01E53A_D2AD.zip'
    # bExists = wasdi.__fileExistsOnWasdi(sFileName)
    # if bExists:
    #     wasdi.downloadFile(sFileName)
    #     bExists = wasdi.__fileExistsOnWasdi(sFileName)
    #     assert (bExists is True)

    # test download + unzip
    sFileName = 'S1A_EW_GRDM_1SSH_20190509T004543_20190509T004646_027143_030F49_B737_ApplyOrbit.dim'
    bExists = wasdi.__fileExistsOnWasdi(sFileName)
    if bExists:
        wasdi.downloadFile(sFileName)
        bExists = wasdi.__fileExistsOnWasdi(sFileName)
        assert (bExists is True)

    aoSearchResult = searchEO()
    oSelected = aoSearchResult[0]
    sImported = importProduct(oSelected)


else:
    print('[ERROR] cannot init waspy')
