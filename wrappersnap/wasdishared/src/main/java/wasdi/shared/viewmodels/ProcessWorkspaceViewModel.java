package wasdi.shared.viewmodels;

/**
 * Created by s.adamo on 31/01/2017.
 */
public class ProcessWorkspaceViewModel {

    private String productName;
    private String operationType;
    private String operationDate;
    private String userId;

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
}
