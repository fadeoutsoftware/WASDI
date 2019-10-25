package wasdi.shared.parameters;

import wasdi.shared.LauncherOperations;

/**
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
		
		if (sOperationType.equals(LauncherOperations.APPLYORBIT.name())) {
			oParam = new ApplyOrbitParameter();
		}
		else if (sOperationType.equals(LauncherOperations.CALIBRATE.name())) {
			oParam = new CalibratorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DELETEPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DEPLOYPROCESSOR.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DOWNLOAD.name())) {
			oParam = new DownloadFileParameter();
		}
		else if (sOperationType.equals(LauncherOperations.DOWNLOADANDPUBLISH.name())) {
			oParam = new DownloadFileParameter();
		}
		else if (sOperationType.equals(LauncherOperations.FILTER.name())) {
			oParam = new FilterParameter();
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
		else if (sOperationType.equals(LauncherOperations.MULTILOOKING.name())) {
			oParam = new MultilookingParameter();
		}
		else if (sOperationType.equals(LauncherOperations.MULTISUBSET.name())) {
			oParam = new MultiSubsetParameter();
		}
		else if (sOperationType.equals(LauncherOperations.NDVI.name())) {
			oParam = new NDVIParameter();
		}
		else if (sOperationType.equals(LauncherOperations.PUBLISH.name())) {
			oParam = new PublishParameters();
		}
		else if (sOperationType.equals(LauncherOperations.PUBLISHBAND.name())) {
			oParam = new PublishBandParameter();
		}
		else if (sOperationType.equals(LauncherOperations.RASTERGEOMETRICRESAMPLE.name())) {
			oParam = new RasterGeometricResampleParameter();
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
		else if (sOperationType.equals(LauncherOperations.TERRAIN.name())) {
			oParam = new RangeDopplerGeocodingParameter();
		}
		else if (sOperationType.equals(LauncherOperations.UPDATEPROCESSES.name())) {
			oParam = new ProcessorParameter();
		}
		else if (sOperationType.equals(LauncherOperations.WPS.name())) {
			oParam = new WpsParameters();
		}
		
		return oParam;
	}
}
