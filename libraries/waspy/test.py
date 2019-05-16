import wasdi
import os

print(os.getcwd())

bInitOk = False
bInitOk = wasdi.init('./config.json')



if bInitOk:
    print('[INFO] waspy: init ok')

    wasdi.printStatus()

    bExists = wasdi.__fileExistsOnWasdi('itsAFalseFileDoNeverMind')
    assert(bExists is False)
    bExists = wasdi.__fileExistsOnWasdi('S1A_EW_GRDM_1SSH_20190509T004543_20190509T004646_027143_030F49_B737.zip')
    assert(bExists is True)

    '''
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
    print(repr(aoSearchResult))
    '''



else:
    print('[ERROR] cannot init waspy')


