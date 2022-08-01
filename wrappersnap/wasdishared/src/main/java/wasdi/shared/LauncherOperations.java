package wasdi.shared;

/**
 * List of all the Launcher Operations.
 * Is used also as message code of rabbit messages.
 * 
 * Created by p.campanella on 14/10/2016.
 */
public enum LauncherOperations {
	INGEST,
    DOWNLOAD,
    SHARE,
    FTPUPLOAD,
    PUBLISHBAND,
    UPDATEPROCESSES,
    GRAPH,
    DEPLOYPROCESSOR,
    RUNPROCESSOR,
    RUNIDL,
    RUNMATLAB,
    MOSAIC,
    SUBSET,
    MULTISUBSET,
    REGRID,
    DELETEPROCESSOR,
    INFO,
    COPYTOSFTP,
    REDEPLOYPROCESSOR,
    LIBRARYUPDATE,
    ENVIRONMENTUPDATE,
    KILLPROCESSTREE,
    READMETADATA,
    SEN2COR
}
