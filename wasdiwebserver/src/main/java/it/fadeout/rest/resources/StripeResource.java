package it.fadeout.rest.resources;

import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.model.checkout.Session.CustomerDetails;

import it.fadeout.Wasdi;
import wasdi.shared.business.Subscription;
import wasdi.shared.business.User;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;
import wasdi.shared.viewmodels.organizations.SubscriptionType;

@Path("/stripe")
public class StripeResource {

	@GET
	@Path("/paymentUrl")
	public PrimitiveResult getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId) {
		WasdiLog.debugLog("StripeResource.getStripePaymentUrl( Subscription: " + sSubscriptionId + ")");

		PrimitiveResult oResult = new PrimitiveResult();
		oResult.setBoolValue(false);

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("StripeResource.getStripePaymentUrl: invalid session");
			oResult.setStringValue("Invalid session.");
			return oResult;
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				oResult.setStringValue("Invalid subscription Id.");
				return oResult;
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: user cannot access subscription info, aborting");
				oResult.setStringValue("User cannot access subscription info, aborting.");
				return oResult;
			}

			WasdiLog.debugLog("StripeResource.getStripePaymentUrl: read subscriptions " + sSubscriptionId);

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: subscription does not exist");
				oResult.setStringValue("No subscription with the subscription Id " + sSubscriptionId + " exists.");
				return oResult;
			}

			String sSubscriptionType = oSubscription.getType();

			if (Utils.isNullOrEmpty(sSubscriptionType)) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				oResult.setStringValue("The subscription does not have a valid type, aborting.");
				return oResult;
			}

			if (sSubscriptionType.equals(SubscriptionType.OneDayStandard.name())) {
				oResult.setBoolValue(true);
				oResult.setStringValue("https://buy.stripe.com/test_6oEaGo7gY3Ve83m8wy" + "?client_reference_id=" + sSubscriptionId);
			} else if (sSubscriptionType.equals(SubscriptionType.OneWeekStandard.name())) {
				oResult.setBoolValue(true);
				oResult.setStringValue("https://buy.stripe.com/test_5kAg0I0SAajC97q5kn" + "?client_reference_id=" + sSubscriptionId);
			}
		} catch (Exception oEx) {
			WasdiLog.debugLog("StripeResource.getStripePaymentUrl: " + oEx);
		}

		return oResult;
	}

	@GET
	@Path("/confirmation/{CHECKOUT_SESSION_ID}")
	public StripePaymentDetail confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		WasdiLog.debugLog("StripeResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			WasdiLog.debugLog("StripeResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");

			return null;
		}

		StripePaymentDetail oStripePaymentDetail = retrieveStripePaymentDetail(sCheckoutSessionId);

		if (oStripePaymentDetail == null || Utils.isNullOrEmpty(oStripePaymentDetail.getClientReferenceId())) {
			WasdiLog.debugLog("StripeResource.confirmation: Stripe returned an invalid result, aborting");

			return null;
		}

		String sSubscriptionId = oStripePaymentDetail.getClientReferenceId();

		WasdiLog.debugLog("StripeResource.confirmation( sSubscriptionId: " + sSubscriptionId + ")");

		if (oStripePaymentDetail != null) {

			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("StripeResource.confirmation: subscription does not exist");
			} else {
				oSubscription.setBuyDate(Utils.nowInMillis());
				oSubscription.setBuySuccess(true);

				oSubscriptionRepository.updateSubscription(oSubscription);
			}
		}

		return oStripePaymentDetail;
	}

	private StripePaymentDetail retrieveStripePaymentDetail(String sCheckoutSessionId) {
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
			WasdiLog.debugLog("StripeResource.confirmation: " + oEx);
		}

		return oStripePaymentDetail;
	}

}
