package wasdi.shared.viewmodels.organizations;

import java.util.Date;

public class StripePaymentDetail {

	private String clientReferenceId;

	private String customerName;
	private String customerEmail;

	private String paymentIntentId;
	private String paymentStatus;
	private String paymentCurrency;
	private Long paymentAmountInCents;

	private String invoiceId;
	private String productDescription;
	private Long paymentDateInSeconds;
	private Date date;
	private String invoicePdfUrl;
	
	public String getClientReferenceId() {
		return clientReferenceId;
	}
	public void setClientReferenceId(String clientReferenceId) {
		this.clientReferenceId = clientReferenceId;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public String getCustomerEmail() {
		return customerEmail;
	}
	public void setCustomerEmail(String customerEmail) {
		this.customerEmail = customerEmail;
	}
	public String getPaymentIntentId() {
		return paymentIntentId;
	}
	public void setPaymentIntentId(String paymentIntentId) {
		this.paymentIntentId = paymentIntentId;
	}
	public String getPaymentStatus() {
		return paymentStatus;
	}
	public void setPaymentStatus(String paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	public String getPaymentCurrency() {
		return paymentCurrency;
	}
	public void setPaymentCurrency(String paymentCurrency) {
		this.paymentCurrency = paymentCurrency;
	}
	public Long getPaymentAmountInCents() {
		return paymentAmountInCents;
	}
	public void setPaymentAmountInCents(Long paymentAmountInCents) {
		this.paymentAmountInCents = paymentAmountInCents;
	}
	public String getInvoiceId() {
		return invoiceId;
	}
	public void setInvoiceId(String invoiceId) {
		this.invoiceId = invoiceId;
	}
	public String getProductDescription() {
		return productDescription;
	}
	public void setProductDescription(String productDescription) {
		this.productDescription = productDescription;
	}
	public Long getPaymentDateInSeconds() {
		return paymentDateInSeconds;
	}
	public void setPaymentDateInSeconds(Long paymentDateInSeconds) {
		this.paymentDateInSeconds = paymentDateInSeconds;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getInvoicePdfUrl() {
		return invoicePdfUrl;
	}
	public void setInvoicePdfUrl(String invoicePdfUrl) {
		this.invoicePdfUrl = invoicePdfUrl;
	}

}
