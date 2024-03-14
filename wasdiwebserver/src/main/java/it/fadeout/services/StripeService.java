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

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.HttpUtils;
import wasdi.shared.utils.Utils;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.HttpCallResponse;
import wasdi.shared.viewmodels.organizations.StripePaymentDetail;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
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
	
	
	public String createAppProduct(String sName, String sDescription, Float fPrice) {
		
		if (Utils.isNullOrEmpty(sName)) {
			WasdiLog.warnLog("StripeService.createAppProduct: can't create product on Stripe. Produc name is null or empty");
			return null;
		}
		
		WasdiLog.debugLog("StripeService.createAppProduct: creating product on stripe of app name: " + sName + ", with price: " + fPrice);
		
		String sUrl = s_sSTRIPE_BASE_URL;
		if (!sUrl.endsWith("/")) sUrl += "/";
		sUrl += "products?";
		
		// creating parameters with encoding
		List<String> asParameters = new ArrayList<>();
		try {	
			asParameters.add("name=" + URLEncoder.encode(sName, StandardCharsets.UTF_8.toString()));
			
			if (!Utils.isNullOrEmpty(sDescription))
				asParameters.add("description=" + URLEncoder.encode(sDescription, StandardCharsets.UTF_8.toString()));
				
			asParameters.add("tax_code=" + URLEncoder.encode("txcd_10000000", StandardCharsets.UTF_8.toString())); // same tax code as subscriptions			
		} catch (UnsupportedEncodingException oEx) {
			WasdiLog.errorLog("StripeService.createAppProduct: can't create product on Stripe. Error in encoding the request parameters", oEx);
			return null;
		}
		
		String sAllParams = String.join("&", asParameters);
		sUrl += sAllParams;
				
		Map<String, String> asHeaders = getStripeAuthHeader();
				
		HttpCallResponse oHttpResponse = HttpUtils.httpPost(sUrl, "", asHeaders);
		int iResponseCode = oHttpResponse.getResponseCode();
		
		if (iResponseCode >= 200 && iResponseCode <= 299) {
			try {
				JSONObject oJsonResponseBody = new JSONObject(oHttpResponse.getResponseBody());
				String sProductId = oJsonResponseBody.optString("id", null);
				WasdiLog.debugLog("StripeService.createAppProduct: product created on Stripe with id: " + sProductId);
				return sProductId;
			} catch (Exception oEx) {
				WasdiLog.errorLog("StripeService.createAppProduct: there was an error reading the json response ", oEx);
				return null;
			}
		} else {
			WasdiLog.errorLog("StripeService.createAppProduct: Can't create product on Stripe. Error while sending the request to Stripe.");
			return null;
		}

	}
	
	
	public Map<String, String> getStripeAuthHeader() {
		Map<String, String> asHeaders = new HashMap<>();
		
		byte [] yAuthEncBytes = Base64.getEncoder().encode("sk_test_51Mg8UfKhhULxWbPP4GUh8PwQCkmmgWdBSZljuVceiHta1PVDsehJNCqN8Lfxv9iUsAkTF7wG11YNDmQClfvcU91Y00h0VUntGk".getBytes());
		// byte [] yAuthEncBytes = Base64.getEncoder().encode(WasdiConfig.Current.stripe.apiKey.getBytes());
        String sAuthStringEnc = new String(yAuthEncBytes);
        
        asHeaders.put("Authorization", "Basic " + sAuthStringEnc);
        
        return asHeaders;
	}

}
