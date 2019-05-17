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

if bInitOk:
    print('[INFO] waspy: init ok')

    sFileName = 'itsAFalseFileDoNeverMind'
    bExists = wasdi.__fileExistsOnWasdi(sFileName)
    assert(bExists is False)
    sFileName = 'S1A_EW_GRDM_1SSH_20190509T004543_20190509T004646_027143_030F49_B737.zip'
    bExists = wasdi.__fileExistsOnWasdi(sFileName)
    assert(bExists is True)
    # if bExists:
    #     wasdi.downloadFile(sFileName)

    # sFileName = 'S1A_EW_GRDM_1SSH_20190509T004543_20190509T004646_027143_030F49_B737_ApplyOrbit.dim'
    # bExists = wasdi.__fileExistsOnWasdi(sFileName)
    # assert (bExists is True)
    # if bExists:
    #     wasdi.downloadFile(sFileName)

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

    oSelected = aoSearchResult[0]
    print(oSelected)
    sImported = wasdi.importProduct(asProduct=oSelected)
    print(sImported)


else:
    print('[ERROR] cannot init waspy')
