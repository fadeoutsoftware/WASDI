package it.fadeout.wasdi.keycloak.event;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;

import java.io.IOException;
import java.util.Map;


class WASDIEventListenerProvider implements EventListenerProvider {

    private final OkHttpClient client = new OkHttpClient();
    //public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    public void onEvent(Event oEvent) {
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        // Log
        Log(oEvent);
        if (oEvent.getType().equals(EventType.VERIFY_EMAIL)) {
            String sUsername = oEvent.getDetails().get("username");
            if (sUsername == null) {
                System.out.println("[WARNING] UserId parsed to 'null', assign empty string instead");
                sUsername = "";
            }
            System.out.println("Verified email for the user " + sUsername);

            StringBuilder oStringBuilder = new StringBuilder();
            oStringBuilder.append("{ \"userId\" : ");
            oStringBuilder.append("\"");
            oStringBuilder.append(sUsername);
            oStringBuilder.append("\"");
            oStringBuilder.append("\n}");

            System.out.println("String to be posted :");
            System.out.println(oStringBuilder.toString());

            okhttp3.RequestBody oBody = RequestBody.create(JSON, oStringBuilder.toString());
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://www.wasdi.net/wasdiwebserver/rest/auth/register")
                    .post(oBody)
                    .build();
            try (Response oResponse = client.newCall(request).execute()) {
                if (!oResponse.isSuccessful()) throw new IOException("Response isn't successfull ! ");
                System.out.println(" WASDI response from POST " + oResponse.body().string());
            } catch (IOException e) {
            	System.out.println("ERROR: " + e.toString());
            }
        }

    }

    private void Log(Event event) {
        if (event != null) {
            if (!event.getType().equals(EventType.INTROSPECT_TOKEN)) {
                System.out.println(" EVENT -----------" + event.getType());
                System.out.println("USER ID    ----" + event.getUserId());
                System.out.println("SESSION ID ----" + event.getSessionId());
            }

            if (event.getDetails() != null) {
                printMapDetails(event.getDetails());
            }
        }
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean b) {

    }

    @Override
    public void close() {

    }

    void printMapDetails(Map<String, String> details) {
        for (Map.Entry<String, String> row : details.entrySet()) {
            String key = row.getKey();
            String value = row.getValue();
            System.out.println(key + ": " + value);
        }
    }
}
