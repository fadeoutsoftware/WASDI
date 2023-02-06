package wasdi.shared.viewmodels.organizations;

import java.util.Date;

import lombok.Data;

@Data
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

}
