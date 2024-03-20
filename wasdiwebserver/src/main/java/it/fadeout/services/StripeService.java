package it.fadeout.services;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.InvoiceLineItemCollection;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.model.checkout.Session.CustomerDetails;

import wasdi.shared.config.StripeProductConfig;
import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class StripeService {

	private static final String s_sSTRIPE_BASE_URL = "https://api.stripe.com/v1";

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

	public String createProductAppWithOnDemandPrice(String sName, String sDescription, Float fPrice) {

		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.warnLog(
					"StripeService.createProductAppWithOnDemandPrice: can't create product on Stripe. Produc name is null or empty");
			return null;
		}

		WasdiLog.debugLog("StripeService.createProductAppWithOnDemandPrice: creating product on stripe of app name: "
				+ sName + ", with price: " + fPrice);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "products?";

		// request parameters
		List<String> asParameters = new ArrayList<>();
		try {
			// product name
			asParameters.add("name=" + URLEncoder.encode(sName, StandardCharsets.UTF_8.toString()));

			// description
			if (!Utils.isNullOrEmpty(sDescription))
				asParameters.add("description=" + URLEncoder.encode(sDescription, StandardCharsets.UTF_8.toString()));

			// tax code - same of subscriptions (General - Electronically Supplied Services)
			asParameters.add("tax_code=" + URLEncoder.encode("txcd_10000000", StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException oEx) {
			WasdiLog.errorLog(
					"StripeService.createProductAppWithOnDemandPrice: can't create product on Stripe. Error in encoding the request parameters",
					oEx);
			return null;
		}

		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;

		// headers
		Map<String, String> asHeaders = getStripeAuthHeader();

		// metadata
		String sMetadata = "metadata[productType]=processor";

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, sMetadata, asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sProductId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sProductId)) {
					WasdiLog.warnLog(
							"StripeService.createProductAppWithOnDemandPrice: product id not found in the Stripe response. An error might have occured");
					return null;
				}

				WasdiLog.debugLog("StripeService.createProductAppWithOnDemandPrice: product created on Stripe with id: "
						+ sProductId);

				String sPriceId = createProductOnDemandPrice(sProductId, fPrice);

				if (Utils.isNullOrEmpty(sPriceId)) {
					WasdiLog.warnLog(
							"StripeService.createAppProduct: error creating the price for product " + sProductId);
					// TODO: at this point, I probably need to remove the product from Stripe
					return null;
				}

				WasdiLog.debugLog("StripeService.createProductAppWithOnDemandPrice: created price " + sPriceId
						+ " for product " + sProductId);

				return sProductId;
			} catch (Exception oEx) {
				WasdiLog.errorLog(
						"StripeService.createProductAppWithOnDemandPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			WasdiLog.errorLog(
					"StripeService.createProductAppWithOnDemandPrice: Can't create product on Stripe. Error while sending the request to Stripe.");
			return null;
		}
	}

	public String deactivateProduct(String sProductId) {

		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.warnLog(
					"StripeService.deactivateProduct: product id is null or empty. Impossible to deactivate product on Stripe");
			return null;
		}

		// first we deactivate all the active on demand prices associated to the product
		List<String> asPrices = getActiveOnDemandPricesId(sProductId);

		if (asPrices != null && asPrices.size() > 0) {

			for (String sPriceId : asPrices) {
				String sDeactivatedPrice = deactivatePrice(sPriceId);

				if (Utils.isNullOrEmpty(sDeactivatedPrice))
					WasdiLog.warnLog(
							"StripeService.deactivateProduct: there was an error deactivating price " + sPriceId);
				else
					WasdiLog.debugLog("StripeService.deactivateProduct: price " + sPriceId + " was deactivated");
			}
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "products/" + sProductId + "?active=false";

		Map<String, String> oMapHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", oMapHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJSONResponse = new JSONObject(oHttpResponse.getResponseBody());
				boolean bIsActive = oJSONResponse.optBoolean("active");
				String sResponseProductId = oJSONResponse.optString("id", null);

				if (!sResponseProductId.equals(sProductId)) {
					WasdiLog.warnLog("StripeService.deactivateProduct: product id " + sResponseProductId
							+ " from Stripe does not match requested product id " + sProductId);
					return null;
				}

				if (bIsActive) {
					WasdiLog.warnLog(
							"StripeService.deactivateProduct: product " + sProductId + " is still active on Stripe");
					return null;
				}
				return sProductId;

			} catch (Exception oEx) {
				WasdiLog.errorLog("StripeService.deactivateProduct: error reading the response from Stripe", oEx);
				return null;
			}
		} else {
			WasdiLog.warnLog("StripeService.deactivateProduct: received an error code from Stripe");
			return null;
		}

	}

	public List<String> getActiveOnDemandPricesId(String sProductId) {
		// TODO: decide if I just return the list of price ids or some data structure
		// more relevant

		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.warnLog("StripeService.getActiveOnDemandPriceId: product id is null or empty. ");
			return null;
		}

		WasdiLog.debugLog(
				"StripeService.getActiveOnDemandPriceId: get active on demand price for product " + sProductId);

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices/search";

		String sQuery = "";

		try {
			sQuery = URLEncoder.encode(("product:\'" + sProductId + "\'"), StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException oEx) {
			WasdiLog.errorLog("StripeService.getActiveOnDemandPriceId: error in encoding the request parameters", oEx);
			return null;
		}

		sUrl += "?query=" + sQuery;

		Map<String, String> asStripeHeaders = getStripeAuthHeader();
		HttpCallResponse oHttpResponse = HttpUtils.httpGet(sUrl, asStripeHeaders);

		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {

				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				JSONArray aoProductPrices = oJsonResponseBody.optJSONArray("data");

				if (aoProductPrices == null) {
					WasdiLog.warnLog("StripeService.getActiveOnDemandPriceId: prices array is null for product id "
							+ sProductId);
				}

				if (aoProductPrices.length() == 0) {
					WasdiLog.warnLog("StripeService.getActiveOnDemandPriceId: prices array is empty for product id "
							+ sProductId);
				}

				List<String> asActivePricesIds = new ArrayList<>();

				for (int i = 0; i < aoProductPrices.length(); i++) {
					JSONObject oJsonPrice = aoProductPrices.getJSONObject(i);

					if (oJsonPrice.optBoolean("active", false)) {
						String sPriceId = oJsonPrice.optString("id", null);
						if (!Utils.isNullOrEmpty(sPriceId))
							asActivePricesIds.add(sPriceId);
					}
				}

				return asActivePricesIds;

			} catch (Exception oEx) {
				WasdiLog.errorLog("StripeService.getActiveOnDemandPriceId: exception reading the Stripe response ",
						oEx);
				return null;
			}
		} else {
			WasdiLog.errorLog("StripeService.getActiveOnDemandPriceId: error while sending the request to Stripe");
			return null;
		}
	}

	public String createProductOnDemandPrice(String sProductId, Float fPrice) {

		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.errorLog(
					"StripeService.createProductPrice: product id is null or empty. Impossible to add the price to the product.");
			return null;
		}

		if (fPrice < 0) {
			WasdiLog.errorLog("StripeService.createProductPrice: price can not be a negativa value");
			return null;
		}

		List<String> asParameters = new ArrayList<>();

		int iPriceInCents = (int) (fPrice * 100);

		try {
			asParameters.add("currency=eur");
			asParameters.add("product=" + URLEncoder.encode(sProductId, StandardCharsets.UTF_8.toString()));
			asParameters.add("unit_amount="
					+ URLEncoder.encode(Integer.toString(iPriceInCents), StandardCharsets.UTF_8.toString()));
		} catch (UnsupportedEncodingException oEx) {
			WasdiLog.errorLog(
					"StripeService.createProductPrice: can't create product on Stripe. Error in encoding the request parameters",
					oEx);
			return null;
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices?";

		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;

		Map<String, String> asHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sPriceId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sPriceId)) {
					WasdiLog.warnLog("StripeService.createProductPrice: no price id found after creation on stripe");
					// TODO: in this case I need probably to remove the price from Stripe
					return null;
				}
				WasdiLog.debugLog("StripeService.createProductPrice: created price in Stripe with id: " + sPriceId);

				// one last check that the price is associated to the correct product (it should
				// always be the case)
				String sProductIdFromPrice = oJsonResponseBody.optString("product", null);

				if (!sProductIdFromPrice.equals(sProductId)) {
					WasdiLog.warnLog(
							"StripeService.createProductPrice: the product id of the price and the expected product id are not matching");
					// TODO: in this case I need probably to remove the price from Stripe
					return null;
				}

				return sPriceId;
			} catch (Exception oEx) {
				WasdiLog.errorLog("StripeService.createProductPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			WasdiLog.errorLog(
					"StripeService.createProductPrice: Can't create product on Stripe. Error while sending the request to Stripe");
			return null;
		}
	}
	
	public String deactivateOnDemandPrice(String sProductId) {
		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.warnLog("StripeService.deactivateOnDemandPrice: product id is null or empty.");
			return null;
		}
		
		List<String> asOnDemandPrices = getActiveOnDemandPricesId(sProductId);
		
		if (asOnDemandPrices == null || asOnDemandPrices.size() == 0) {
			WasdiLog.warnLog("StripeService.deactivateOnDemandPrice: no active prices found for product " + sProductId);
			return null;
		}
		
		if (asOnDemandPrices.size() > 1) {
			WasdiLog.warnLog("StripeService.updateOnDemandPrice: more than one active prices found for product " + sProductId + ". Can not decide the one to deactivate");
			return null;
		}
		
		return deactivatePrice(asOnDemandPrices.get(0));
	}

	public String deactivatePrice(String sPriceId) {
		if (Utils.isNullOrEmpty(sPriceId)) {
			WasdiLog.errorLog(
					"StripeService.deactivateProductPrice: price id is null or empty. Impossible to deactivate the price");
			return null;
		}

		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/"))
			sUrl += "/";
		sUrl += "prices/" + sPriceId + "?" + "active=false";

		Map<String, String> asHeaders = getStripeAuthHeader();

		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();

		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sStripePriceId = oJsonResponseBody.optString("id", null);

				if (Utils.isNullOrEmpty(sStripePriceId)) {
					WasdiLog.warnLog(
							"StripeService.deactivateProductPrice: no price id found after creation on stripe");
					// TODO: in this case I need probably to remove the price from Stripe
					return null;
				}

				if (!sPriceId.equals(sStripePriceId)) {
					// this case should never happen
					WasdiLog.warnLog(
							"StripeService.deactivateProductPrice: the required price id and the returned price id are different");
					return null;
				}

				// one last check that the price is associated to the correct product (it should
				// always be the case)
				String sProductIdFromPrice = oJsonResponseBody.optString("product", null);

				WasdiLog.debugLog("StripeService.deactivateProductPrice: price in Stripe with id: " + sStripePriceId
						+ " has been deactivated");

				return sStripePriceId;

			} catch (Exception oEx) {
				WasdiLog.errorLog("StripeService.deactivateProductPrice: there was an error reading the json response ",
						oEx);
				return null;
			}
		} else {
			WasdiLog.errorLog(
					"StripeService.deactivateProductPrice: Can't deactivate price. Error while sending the request to Stripe");
			return null;
		}

	}
	
	public String updateOnDemandPrice(String sProductId, float fNewPrice) {
		
		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.warnLog("StripeService.updateOnDemandPrice: product id is null or empty.");
			return null;
		}
		
		List<String> asOnDemandPrices = getActiveOnDemandPricesId(sProductId);
		
		if (asOnDemandPrices == null || asOnDemandPrices.size() == 0) {
			WasdiLog.warnLog("StripeService.updateOnDemandPrice: no active prices found for product " + sProductId);
			return null;
		}
		
		if (asOnDemandPrices.size() > 1) {
			WasdiLog.warnLog("StripeService.updateOnDemandPrice: more than one active prices found for product " + sProductId + ". Can not decide the one to update");
			return null;
		}
		
		return updateOnDemandPrice(sProductId, asOnDemandPrices.get(0), fNewPrice);
		
	}

	public String updateOnDemandPrice(String sProductId, String sPriceId, float fNewPrice) {
		// TODO: adjust this so that we retrieve directly here the priceId
		if (Utils.isNullOrEmpty(sProductId)) {
			WasdiLog.warnLog(
					"StripeService.updateAppPrice: product id is null or empty. Impossible to update app price");
			return null;
		}

		if (Utils.isNullOrEmpty(sPriceId)) {
			WasdiLog.warnLog("StripeService.updateAppPrice: price id is null or empty. Impossible to update app price");
			return null;
		}

		if (fNewPrice < 0) {
			WasdiLog.errorLog("StripeService.updateAppPrice: price can not be a negativa value");
			return null;
		}

		// to update a price, we need first to deactivate the current price, then create
		// a new one
		String sOldPriceId = deactivatePrice(sPriceId);

		if (Utils.isNullOrEmpty(sOldPriceId)) {
			WasdiLog.warnLog(
					"StripeService.updateAppPrice: no old price retrieved. The product won't be updated with the new price");
			return null;
		}

		WasdiLog.debugLog("StripeService.updateAppPrice: old price with id " + sPriceId
				+ " has been deactivated for the product " + sProductId);

		String sNewPriceId = createProductOnDemandPrice(sProductId, fNewPrice);

		if (Utils.isNullOrEmpty(sNewPriceId)) {
			WasdiLog.warnLog("StripeService.updateAppPrice: new price id is null or empty");
			return null;
		}

		WasdiLog.debugLog("StripeService.updateAppPrice: new price with id " + sNewPriceId
				+ " has been created for the product " + sProductId);

		return sNewPriceId;
	}

	public Map<String, String> getStripeAuthHeader() {
		Map<String, String> asHeaders = new HashMap<>();

		byte[] yAuthEncBytes = Base64.getEncoder().encode(WasdiConfig.Current.stripe.apiKey.getBytes());
		String sAuthStringEnc = new String(yAuthEncBytes);

		asHeaders.put("Authorization", "Basic " + sAuthStringEnc);

		return asHeaders;
	}

	public void listProducts() {
		String sUrl = s_sSTRIPE_BASE_URL + "/products";

		Map<String, String> headers = getStripeAuthHeader();

		HttpCallResponse oResponse = HttpUtils.httpGet(sUrl, headers);

		if (oResponse.getResponseCode() >= 200 && oResponse.getResponseCode() < 300) {
			System.out.println(oResponse.getResponseBody());
		} else {
			System.out.println("error code: " + oResponse.getResponseCode());
		}

	}

	public static void main(String[] args) throws Exception {

		StripeService stripeService = new StripeService();
		WasdiConfig.readConfig("C:/temp/wasdi/wasdiLocalTESTConfig.json");
//		String isAppCreated = stripeService.createProductAppWithOnDemandPrice("Test App with Metadata", "This is an app with metadata", 50.00f); // prod_PlWwdzKlNEtHSc
//		System.out.println(isAppCreated);

		String sProdId = stripeService.deactivateProduct("prod_PlUsE22AA6eMga");
		System.out.println("Deleted product: " + sProdId);
		stripeService.listProducts();

//        List<String> sPriceIds = stripeService.getActiveOnDemandPriceId("prod_PlWwdzKlNEtHSc"); 
//        for (String sId : sPriceIds)
//        	System.out.println(sId);
//		
//        String sNewPriceId = stripeService.updateOnDemandPrice("prod_PlUsE22AA6eMga", "price_1OvxsKKhhULxWbPP4MxyE6yB", 100.00f);
//        System.out.println("New price id: " + sNewPriceId);  // price_1Ovz08KhhULxWbPPlIxgVpWp

//        sPriceIds = stripeService.getPriceIdFromProductId("prod_PlUsE22AA6eMga"); // price_1OvxsKKhhULxWbPP4MxyE6yB
//        for (String sId : sPriceIds)
//        	System.out.println(sId);

	}

}
