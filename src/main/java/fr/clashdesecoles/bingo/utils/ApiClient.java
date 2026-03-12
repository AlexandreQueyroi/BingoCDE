package fr.clashdesecoles.bingo.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ApiClient {
    
    private final String baseUrl;
    private final String apiKey;
    private final int timeout;
    private final Gson gson;
    private final Logger logger;
    
    public ApiClient(String baseUrl, String apiKey, int timeout, Logger logger) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.gson = new Gson();
        this.logger = logger;
    }
    
    public JsonElement get(String endpoint) {
        return request("GET", endpoint, null);
    }
    
    public JsonElement post(String endpoint, JsonObject body) {
        return request("POST", endpoint, body);
    }
    
    public JsonElement patch(String endpoint, JsonObject body) {
        return request("PATCH", endpoint, body);
    }
    
    public JsonElement delete(String endpoint) {
        return request("DELETE", endpoint, null);
    }

    private JsonElement request(String method, String endpoint, JsonObject body) {
        HttpURLConnection connection = null;
        
        try {
            String urlStr = baseUrl + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
            URL url = new URL(urlStr);
            
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            
            if (body != null && (method.equals("POST") || method.equals("PATCH"))) {
                connection.setDoOutput(true);
                String jsonBody = gson.toJson(body);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            int responseCode = connection.getResponseCode();
            
            java.io.InputStream rawStream = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();
            
            StringBuilder response = new StringBuilder();
            if (rawStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(rawStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }
            }
            
            String responseStr = response.toString();
            
            if (responseCode >= 200 && responseCode < 300) {
                if (responseStr.isEmpty()) {
                    return new JsonObject();
                }
                return JsonParser.parseString(responseStr);
            } else {
                logger.warning("API Error (" + responseCode + "): " + responseStr);
                return null;
            }
            
        } catch (Exception e) {
            logger.warning("API Request failed: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
