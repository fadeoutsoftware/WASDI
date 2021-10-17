package wasdi.shared.parameters;

import wasdi.shared.LauncherOperations;

/**
 * Base Parameter.
 * 
 * Parameters are a set of class that represents the input needed by each Launcher Operation
 * 
 * Each Operation is triggered from an API; this API collects the data needed by the launcher to execute the operation.
 * All these info are stored in a parameter.
 * 
 * Parameters are stored in the file system using the SerializationUtils.
 * 
 * Almost each operation has its own parameter, even if some operations can share the same param.
 * 
 * Created by s.adamo on 16/03/2017.
 */
public class BaseParameter {

    /**
     * UserId
     */
    private String userId;

    /**
     * Workspace
     */
    private String workspace;
    
    /**
     * Workspace Onwer
     */
    private String workspaceOwnerId;

    /**
     * Is workspace
     */
    private String exchange;
    
    /**
     * Is ObjectId of the process
     */
    private String processObjId;

	/**
	 * sessionID of the user that launched the operation that needs this parameter
	 */
	private String sessionID;

    public String getProcessObjId() {
        return processObjId;
    }

    public void setProcessObjId(String processObjId) {
        this.processObjId = processObjId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

	public String getWorkspaceOwnerId() {
		return workspaceOwnerId;
	}

	public void setWorkspaceOwnerId(String workspaceOwnerId) {
		this.workspaceOwnerId = workspaceOwnerId;
	}
	
	public static BaseParameter getParameterFromOperationType(String sOperationType) {
		BaseParameter oParam = null;
		
		if (sOperationType.equals(LauncherOperations.DELETEPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DEPLOYPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DOWNLOAD.name())) {
			oParam = new DownloadFileParameter();
		}
		else if (sOperationType.equals(LauncherOperations.FTPUPLOAD.name())) {
			oParam = new FtpUploadParameters();
		}
		else if (sOperationType.equals(LauncherOperations.GRAPH.name())) {
			oParam = new GraphParameter();
		}
		else if (sOperationType.equals(LauncherOperations.INGEST.name())) {
			oParam = new IngestFileParameter();
		}
		else if (sOperationType.equals(LauncherOperations.MOSAIC.name())) {
			oParam = new MosaicParameter();
		}
		else if (sOperationType.equals(LauncherOperations.MULTISUBSET.name())) {
			oParam = new MultiSubsetParameter();
		}
		else if (sOperationType.equals(LauncherOperations.PUBLISHBAND.name())) {
			oParam = new PublishBandParameter();
		}
		else if (sOperationType.equals(LauncherOperations.REGRID.name())) {
			oParam = new RegridParameter();
		}
		else if (sOperationType.equals(LauncherOperations.RUNIDL.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.RUNMATLAB.name())) {
			oParam = new MATLABProcParameters();
		}
		else if (sOperationType.equals(LauncherOperations.RUNPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.SUBSET.name())) {
			oParam = new SubsetParameter();
		}
		else if (sOperationType.equals(LauncherOperations.UPDATEPROCESSES.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.REDEPLOYPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.COPYTOSFTP.name())) {
			oParam = new IngestFileParameter();
		}
		else if (sOperationType.equals(LauncherOperations.LIBRARYUPDATE.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.KILLPROCESSTREE.name())) {
			oParam = new KillProcessTreeParameter();
		}
		else if (sOperationType.equals(LauncherOperations.READMETADATA.name())) {
			oParam = new ReadMetadataParameter();
		}
		
		return oParam;
	}
	
	
	public String getSessionID() {
		return sessionID;
	}
	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}
}
