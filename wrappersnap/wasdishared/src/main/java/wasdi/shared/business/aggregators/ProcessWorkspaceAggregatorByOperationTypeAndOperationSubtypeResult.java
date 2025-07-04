package wasdi.shared.business.aggregators;

public class ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult {
	
	public ProcessWorkspaceAggregatorByOperationTypeAndOperationSubtypeResult() {
		
	}
	
	private Id _id;

	private Integer count;

	private static class Id {
		
		private String operationType;
		private String operationSubType;
		private String status;
		
		public String getOperationType() {
			return operationType;
		}
		public void setOperationType(String operationType) {
			this.operationType = operationType;
		}
		public String getOperationSubType() {
			return operationSubType;
		}
		public void setOperationSubType(String operationSubType) {
			this.operationSubType = operationSubType;
		}
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}

	}

	public Id get_id() {
		return _id;
	}

	public void set_id(Id _id) {
		this._id = _id;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}
	
	public String getOperationType() {
		return _id.getOperationType();
	}
	public void setOperationType(String operationType) {
		this._id.setOperationType(operationType);
	}
	public String getOperationSubType() {
		return _id.getOperationSubType();
	}
	public void setOperationSubType(String operationSubType) {
		this._id.setOperationSubType(operationSubType);
	}
	public String getStatus() {
		return _id.getStatus();
	}
	public void setStatus(String status) {
		this._id.setStatus(status);
	}	

}
