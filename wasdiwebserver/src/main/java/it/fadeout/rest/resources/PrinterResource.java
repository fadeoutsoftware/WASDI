package it.fadeout.rest.resources;

import java.net.URI;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import wasdi.shared.config.WasdiConfig;
import wasdi.shared.utils.log.WasdiLog;
import wasdi.shared.viewmodels.PrimitiveResult;
import wasdi.shared.viewmodels.PrinterViewModel;

@Path("/print")
public class PrinterResource {
		
    @POST
    @Path("/storemap")
    @Produces({ "application/xml", "application/json", "text/xml" })
    @Consumes(MediaType.APPLICATION_JSON)
    public Response storemap(PrinterViewModel oPrinterViewModel) {
        if(oPrinterViewModel == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        if (oPrinterViewModel.getBaseMap() == null || oPrinterViewModel.getBaseMap().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        }
        if (oPrinterViewModel.getCenter() == null || oPrinterViewModel.getCenter().getLat() == 0.0 && oPrinterViewModel.getCenter().getLng() == 0.0) { // Basic check for center
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        }
        if (oPrinterViewModel.getFormat() == null || (!oPrinterViewModel.getFormat().equalsIgnoreCase("pdf") && !oPrinterViewModel.getFormat().equalsIgnoreCase("png"))) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .build();
        }
        try {
            // Serialize the incoming PrintMapRequest object to JSON string
            ObjectMapper oObjectMapper = new ObjectMapper();
            String oPrinterBodyJson =oObjectMapper.writeValueAsString(oPrinterViewModel);
            //todo change the url and put in the config file
            // Build the HTTP POST request to the external WASDI API
            HttpRequest oExternalApiRequest = HttpRequest.newBuilder()
                    .uri(URI.create(WasdiConfig.Current.printServerAddress+"/storeMap"))
                    .header("Content-Type", MediaType.APPLICATION_JSON)
                    .POST(HttpRequest.BodyPublishers.ofString(oPrinterBodyJson))
                    .build();

            // Send the request and get the response
            HttpClient oHttpClient = HttpClient.newHttpClient();
            HttpResponse<String> externalApiResponse = oHttpClient.send(oExternalApiRequest, HttpResponse.BodyHandlers.ofString());

            // Check if the external API call was successful (e.g., 200 OK)
            if (externalApiResponse.statusCode() == 200) {
                // Parse the UUID from the external API's response body
                Map<String, String> oResponseMap = oObjectMapper.readValue(externalApiResponse.body(), Map.class);
                String sUUID = oResponseMap.get("uuid");

                if (sUUID != null && !sUUID.trim().isEmpty()) {
                    // Return the UUID to your frontend
                    return Response.ok(sUUID).build();
                } else {
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(Map.of("error", "External service did not return a valid UUID."))
                            .build();
                }
            } else {
                return Response.status(Response.Status.BAD_GATEWAY) // Indicate issue with upstream service
                        .entity(Map.of("error", "External print service failed to store map. Status: " + externalApiResponse.statusCode()))
                        .build();
            }

        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid JSON format in request body."))
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(Map.of("error", "An unexpected error occurred during print job submission."))
                    .build();
        }
    }

    @GET
    @Produces({ "application/pdf", "image/png" })
    public Response print(@QueryParam("uuid") String sUUID) {
        if (sUUID == null || sUUID.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Missing UUID parameter")
                    .build();
        }

        try {
            HttpClient oHttpClient = HttpClient.newHttpClient();

            String sExternalUrl = WasdiConfig.Current.printServerAddress+ "/print?uuid=" + URLEncoder.encode(sUUID, StandardCharsets.UTF_8);

            HttpRequest oRequest = HttpRequest.newBuilder()
                    .uri(URI.create(sExternalUrl))
                    .GET()
                    .build();

            HttpResponse<byte[]> oResponse = oHttpClient.send(oRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (oResponse.statusCode() == 200) {
                // Determine content type from headers
                String sContentType = oResponse.headers()
                        .firstValue("Content-Type")
                        .orElse("application/octet-stream");

                return Response.ok(oResponse.body(), sContentType)
                        .header("Content-Disposition", "inline; filename=\"map." + (sContentType.contains("pdf") ? "pdf" : "png") + "\"")
                        .build();
            } else {
                return Response.status(Response.Status.BAD_GATEWAY)
                        .entity("Failed to fetch map from external service. Status: " + oResponse.statusCode())
                        .build();
            }

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Unexpected error occurred: " + e.getMessage())
                    .build();
        }
    }
}
