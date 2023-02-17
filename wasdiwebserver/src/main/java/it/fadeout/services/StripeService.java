package it.fadeout.services;

import java.util.Date;
import java.util.List;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.model.checkout.Session.CustomerDetails;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;

public class StripeService {

	public StripePaymentDetail retrieveStripePaymentDetail(String sCheckoutSessionId) {
		StripePaymentDetail oStripePaymentDetail = new StripePaymentDetail();

		try {
			Stripe.apiKey = WasdiConfig.Current.stripe.apiKey;

			Session oSession = Session.retrieve(sCheckoutSessionId);

			oStripePaymentDetail.setClientReferenceId(oSession.getClientReferenceId());

			CustomerDetails oCustomerDetails = oSession.getCustomerDetails();
			String sCustomerName = oCustomerDetails.getName();
			String sCustomerEmail = oCustomerDetails.getEmail();

			String sPaymentIntentId = oSession.getPaymentIntent();
			PaymentIntent oPaymentIntent = PaymentIntent.retrieve(sPaymentIntentId);
			String sPaymentStatus = oPaymentIntent.getStatus();
			String sPaymentCurrency = oPaymentIntent.getCurrency();
			Long lPaymentAmountInCents = oPaymentIntent.getAmount();

			oStripePaymentDetail.setPaymentIntentId(sPaymentIntentId);
			oStripePaymentDetail.setCustomerName(sCustomerName);
			oStripePaymentDetail.setCustomerEmail(sCustomerEmail);
			oStripePaymentDetail.setPaymentStatus(sPaymentStatus);
			oStripePaymentDetail.setPaymentCurrency(sPaymentCurrency);
			oStripePaymentDetail.setPaymentAmountInCents(lPaymentAmountInCents);

			String sInvoiceId = oPaymentIntent.getInvoice();

			if (sInvoiceId != null) {
				Invoice oInvoice = Invoice.retrieve(sInvoiceId);

				if (oInvoice != null) {
					InvoiceLineItemCollection oInvoiceLineItemCollection = oInvoice.getLines();
					List<InvoiceLineItem> oInvoiceLineItemData = oInvoiceLineItemCollection.getData();
					InvoiceLineItem oInvoiceLineItem = oInvoiceLineItemData.get(0);
					String sProductDescription = oInvoiceLineItem.getDescription();
					Long lPaymentDateInSeconds = oInvoiceLineItem.getPeriod().getStart();
					Date oDate = new Date(lPaymentDateInSeconds * 1_000);

					String sInvoicePdfUrl = oInvoice.getInvoicePdf();

					oStripePaymentDetail.setInvoiceId(sInvoiceId);
					oStripePaymentDetail.setProductDescription(sProductDescription);
					oStripePaymentDetail.setPaymentDateInSeconds(lPaymentDateInSeconds);
					oStripePaymentDetail.setDate(oDate);
					oStripePaymentDetail.setInvoicePdfUrl(sInvoicePdfUrl);
				}
			}
		} catch (StripeException oEx) {
			WasdiLog.debugLog("StripeService.confirmation: " + oEx);
		}

		return oStripePaymentDetail;
	}

}
