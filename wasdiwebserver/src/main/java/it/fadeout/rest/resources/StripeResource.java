package it.fadeout.rest.resources;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import wasdi.shared.config.StripeProductConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.data.SubscriptionRepository;
import wasdi.shared.rabbit.Send;
import wasdi.shared.utils.PermissionsUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.ErrorResponse;
import wasdi.shared.viewmodels.SuccessResponse;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;
import wasdi.shared.viewmodels.organizations.SubscriptionType;

@Path("/stripe")
public class StripeResource {

	private static final String MSG_ERROR_INVALID_SESSION = "MSG_ERROR_INVALID_SESSION";

	@GET
	@Path("/paymentUrl")
	public Response getStripePaymentUrl(@HeaderParam("x-session-token") String sSessionId,
			@QueryParam("subscription") String sSubscriptionId, @QueryParam("workspace") String sWorkspaceId) {
		WasdiLog.debugLog("StripeResource.getStripePaymentUrl( " + "Subscription: " + sSubscriptionId + ", "
				+ "Workspace: " + sWorkspaceId + ")");

		User oUser = Wasdi.getUserFromSession(sSessionId);

		if (oUser == null) {
			WasdiLog.debugLog("StripeResource.getStripePaymentUrl: invalid session");
			return Response.status(Status.UNAUTHORIZED).entity(new ErrorResponse(MSG_ERROR_INVALID_SESSION)).build();
		}

		try {
			// Domain Check
			if (Utils.isNullOrEmpty(sSubscriptionId)) {
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("Invalid subscriptionId.")).build();
			}

			if (!PermissionsUtils.canUserAccessSubscription(oUser.getUserId(), sSubscriptionId)) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: user cannot access subscription info, aborting");
				return Response.status(Status.FORBIDDEN).entity(new ErrorResponse("The user cannot access the subscription info.")).build();
			}

			WasdiLog.debugLog("StripeResource.getStripePaymentUrl: read subscriptions " + sSubscriptionId);

			// Create repo
			SubscriptionRepository oSubscriptionRepository = new SubscriptionRepository();

			// Get requested subscription
			Subscription oSubscription = oSubscriptionRepository.getSubscriptionById(sSubscriptionId);

			if (oSubscription == null) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: subscription does not exist");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription cannot be found.")).build();
			}

			String sSubscriptionType = oSubscription.getType();

			if (Utils.isNullOrEmpty(sSubscriptionType)) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription does not have a valid type.")).build();
			}

			List<StripeProductConfig> aoProductConfigList = WasdiConfig.Current.stripe.products;

			Map<String, String> aoProductConfigMap = aoProductConfigList.stream()
					.collect(Collectors.toMap(t -> t.id, t -> t.url));

			SubscriptionType oSubscriptionType = SubscriptionType.get(sSubscriptionType);

			if (oSubscriptionType == null) {
				WasdiLog.debugLog("StripeResource.getStripePaymentUrl: the subscription does not have a valid type, aborting");
				return Response.status(Status.BAD_REQUEST).entity(new ErrorResponse("The subscription does not have a valid type.")).build();
			} else {
				String sBaseUrl = aoProductConfigMap.get(sSubscriptionType);

				if (Utils.isNullOrEmpty(sBaseUrl)) {
					WasdiLog.debugLog("StripeResource.getStripePaymentUrl: the wasdiConfig.json file doed not contain a valid configuration for the subscription");
					return Response.serverError().build();
				} else {
					String sUrl = sBaseUrl + "?client_reference_id=" + sSubscriptionId;

					if (!Utils.isNullOrEmpty(sWorkspaceId)) {
						sUrl += "_" + sWorkspaceId;
					}

					return Response.ok(new SuccessResponse(sUrl)).build();
				}
			}
		} catch (Exception oEx) {
			return Response.serverError().build();
		}
	}

	@GET
	@Path("/confirmation/{CHECKOUT_SESSION_ID}")
	public Response confirmation(@PathParam("CHECKOUT_SESSION_ID") String sCheckoutSessionId) {
		WasdiLog.debugLog("StripeResource.confirmation( sCheckoutSessionId: " + sCheckoutSessionId + ")");

		if (Utils.isNullOrEmpty(sCheckoutSessionId)) {
			WasdiLog.debugLog("StripeResource.confirmation: Stripe returned a null CHECKOUT_SESSION_ID, aborting");

			return null;
		}

		StripePaymentDetail oStripePaymentDetail = retrieveStripePaymentDetail(sCheckoutSessionId);

		String sClientReferenceId = oStripePaymentDetail.getClientReferenceId();

		if (oStripePaymentDetail == null || Utils.isNullOrEmpty(sClientReferenceId)) {
			WasdiLog.debugLog("StripeResource.confirmation: Stripe returned an invalid result, aborting");

			return null;
		}

		String sSubscriptionId = null;
		String sWorkspaceId = null;

		if (sClientReferenceId.contains("_")) {
			String[] asClientReferenceId = sClientReferenceId.split("_");
			sSubscriptionId = asClientReferenceId[0];
			sWorkspaceId = asClientReferenceId[1];
		} else {
			sSubscriptionId = sClientReferenceId;
		}

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

				if (!Utils.isNullOrEmpty(sWorkspaceId)) {
					sendRabbitMessage(sWorkspaceId, oStripePaymentDetail);
				}
			}
		}

		String sHtmlContent = "<script type=\"text/javascript\">\r\n" + 
				"setTimeout(\r\n" + 
				"function ( )\r\n" + 
				"{\r\n" + 
				"  self.close();\r\n" + 
				"}, 1000 );\r\n" + 
				"</script>";
		
		return Response.ok(new SuccessResponse(sHtmlContent)).build();
	}

	private void sendRabbitMessage(String sWorkspaceId, StripePaymentDetail oStripePaymentDetail) {
		try {
			// Search for exchange name
			String sExchange = WasdiConfig.Current.rabbit.exchange;

			// Set default if is empty
			if (Utils.isNullOrEmpty(sExchange)) {
				sExchange = "amq.topic";
			}

			// Send the Asynch Message to the clients
			Send oSendToRabbit = new Send(sExchange);
			oSendToRabbit.SendRabbitMessage(true, "SUBSCRIPTION", sWorkspaceId, oStripePaymentDetail, sWorkspaceId);
			oSendToRabbit.Free();
		} catch (Exception oEx) {
			WasdiLog.debugLog("StripeResource.sendRabbitMessage: exception sending asynch notification");
		}
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
