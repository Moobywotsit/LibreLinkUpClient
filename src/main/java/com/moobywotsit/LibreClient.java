package com.moobywotsit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.moobywotsit.models.LibreCgmData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LibreClient {
    private final HttpClient client;

    private final String authToken;
    private final String patientId;
    private final Gson gson;

    private final String libreURINoRegion = "https://api-%s.libreview.io";
    private String libreURI;

    public LibreClient(String username, String password) throws IOException, InterruptedException {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(2)).build();
        //Use default region to start
        libreURI = String.format(libreURINoRegion, "eu");

        authToken = Login(username, password);
        patientId = getFirstConnection();
        gson = new GsonBuilder()
                .setDateFormat("M/d/YYYY hh:mm:ss a")
                .create();
    }

    public LibreCgmData getCurrent() throws IOException, InterruptedException {
        var request = createGetBuilderWithHeaders()
                .header("Authorization", String.join("", "BEARER ", authToken))
                .uri(URI.create(String.join("", libreURI, "/llu/connections/", patientId, "/graph"))).build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        var latestCgmMeasurement = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("connection").getAsJsonObject().get("glucoseMeasurement");
        return gson.fromJson(latestCgmMeasurement, LibreCgmData.class);
    }

    private HttpRequest.Builder createGetBuilderWithHeaders(){
        return addHeaders(HttpRequest.newBuilder().GET());
    }

    private HttpRequest.Builder createPostWithHeaders(HttpRequest.BodyPublisher bodyPublisher){
        return addHeaders(HttpRequest.newBuilder().POST(bodyPublisher));
    }

    private HttpRequest.Builder addHeaders(HttpRequest.Builder builder){
        return builder
                .header("content-type", "application/json")
                .header("product", "llu.android")
                .header("version", "4.7.0");
    }

    private String Login(String username, String password) throws IOException, InterruptedException {
        var response = callLoginEndpoint(username,password);

        //if redirected to another uri.
        if(JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("redirect").getAsBoolean()){
            libreURI = String.format(libreURINoRegion,
                    JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("region").getAsString());

            response = callLoginEndpoint(username,password);
        }

        return JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("authTicket").getAsJsonObject().get("token").getAsString();
    }

    private HttpResponse<String> callLoginEndpoint(String username, String password) throws IOException, InterruptedException {
        var loginBody = String.format("{ \"email\": \"%s\", \"password\": \"%s\" }", username, password);

        var request = createPostWithHeaders(HttpRequest.BodyPublishers.ofString(loginBody))
                .uri(URI.create(String.join("", libreURI, "/llu/auth/login"))).build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getFirstConnection() throws IOException, InterruptedException {
        var request = createGetBuilderWithHeaders()
                .header("Authorization", String.join("", "BEARER ", authToken))
                .uri(URI.create(String.join("", libreURI, "/llu/connections"))).build();

        var response = (client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        return JsonParser.parseString(response).getAsJsonObject().get("data").getAsJsonArray().get(0).getAsJsonObject().get("patientId").getAsString();
    }
}
