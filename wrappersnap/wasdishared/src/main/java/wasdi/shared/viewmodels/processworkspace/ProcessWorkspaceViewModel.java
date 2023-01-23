package wasdi.shared.viewmodels.processworkspace;

import wasdi.shared.business.ProcessWorkspace;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;

/**
 * Represents a Process Workspace
 * 
 * Created by s.adamo on 31/01/2017.
 */
public class ProcessWorkspaceViewModel {

    private String productName;
    private String operationType;
    private String operationSubType;
	private String operationDate;
    private String operationStartDate;
    private String operationEndDate;
    private String lastChangeDate;
	private String userId;
    private String fileSize;
    private String status;
    private int progressPerc;
    private String processObjId;
    private int pid;
    private String payload;
    private String workspaceId;

    public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getOperationDate() {
        return operationDate;
    }

    public void setOperationDate(String operationDate) {
        this.operationDate = operationDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize = fileSize;
    }


    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getProcessObjId() {
		return processObjId;
	}

    public void setProcessObjId(String processObjId) {
		this.processObjId = processObjId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getProgressPerc() {
		return progressPerc;
	}

	public void setProgressPerc(int progressPerc) {
		this.progressPerc = progressPerc;
	}

	public String getOperationEndDate() {
		return operationEndDate;
	}

	public String getOperationStartDate() {
		return operationStartDate;
	}

	public void setOperationStartDate(String operationStartDate) {
		this.operationStartDate = operationStartDate;
	}

	public void setOperationEndDate(String operationEndDate) {
		this.operationEndDate = operationEndDate;
	}
    
    public String getLastChangeDate() {
		return lastChangeDate;
	}

	public void setLastChangeDate(String lastChangeDate) {
		this.lastChangeDate = lastChangeDate;
	}

    public String getOperationSubType() {
		return operationSubType;
	}

	public void setOperationSubType(String operationSubType) {
		this.operationSubType = operationSubType;
	}

	public String getWorkspaceId() {
		return workspaceId;
	}

	public void setWorkspaceId(String workspaceId) {
		this.workspaceId = workspaceId;
	}
	 
	public static ProcessWorkspaceViewModel buildProcessWorkspaceViewModel(ProcessWorkspace oProcess) {
		//WasdiLog.debugLog("ProcessWorkspaceResource.buildProcessWorkspaceViewModel");
		ProcessWorkspaceViewModel oViewModel = new ProcessWorkspaceViewModel();
		try {
			// Set the start date: beeing introduced later, for compatibility, if not present use the Operation Date
			if (!Utils.isNullOrEmpty(oProcess.getOperationStartTimestamp())) {
				oViewModel.setOperationStartDate(Utils.getFormatDate(oProcess.getOperationStartTimestamp()));
			}
			else {
				oViewModel.setOperationStartDate(Utils.getFormatDate(oProcess.getOperationTimestamp()));
			}
			
			if (!Utils.isNullOrEmpty(oProcess.getLastStateChangeTimestamp())) {
				oViewModel.setLastChangeDate(Utils.getFormatDate(oProcess.getLastStateChangeTimestamp()));
			}
			
			oViewModel.setOperationDate(Utils.getFormatDate(oProcess.getOperationTimestamp()));
			oViewModel.setOperationEndDate(Utils.getFormatDate(oProcess.getOperationEndTimestamp()));
			oViewModel.setOperationType(oProcess.getOperationType());
			if (!Utils.isNullOrEmpty(oProcess.getOperationSubType())) {
				oViewModel.setOperationSubType(oProcess.getOperationSubType());
			}
			else {
				oViewModel.setOperationSubType("");
			}
			
			oViewModel.setProductName(oProcess.getProductName());
			oViewModel.setUserId(oProcess.getUserId());
			oViewModel.setFileSize(oProcess.getFileSize() == null ? "" : oProcess.getFileSize());
			oViewModel.setPid(oProcess.getPid());
			oViewModel.setStatus(oProcess.getStatus());
			oViewModel.setProgressPerc(oProcess.getProgressPerc());
			oViewModel.setProcessObjId(oProcess.getProcessObjId());
			oViewModel.setPayload(oProcess.getPayload());
			
			oViewModel.setWorkspaceId(oProcess.getWorkspaceId());
			
		} catch (Exception oEx) {
			WasdiLog.debugLog("ProcessWorkspaceResource.buildProcessWorkspaceViewModel: " + oEx);
		}
		return oViewModel;
	}	

}
