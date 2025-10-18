package org.spacehub.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;

@Service
public class JanusService {

  private final String janusUrl = "http://localhost:8088/janus";

  private final RestTemplate restTemplate = new RestTemplate();

  public String createSession() {
    Map<String, String> request = Map.of(
      "janus", "create",
      "transaction", UUID.randomUUID().toString()
    );
    ResponseEntity<JsonNode> response = restTemplate.postForEntity(janusUrl, request, JsonNode.class);
    if (response.getBody() != null && response.getBody().has("data")) {
      return response.getBody().get("data").get("id").asText();
    }
    throw new RuntimeException("Failed to create Janus session");
  }

  public String attachAudioBridgePlugin(String sessionId) {
    Map<String, String> request = Map.of(
      "janus", "attach",
      "plugin", "janus.plugin.audiobridge",
      "transaction", UUID.randomUUID().toString()
    );
    String sessionUrl = janusUrl + "/" + sessionId;
    ResponseEntity<JsonNode> response = restTemplate.postForEntity(sessionUrl, request, JsonNode.class);
    if (response.getBody() != null && response.getBody().has("data")) {
      return response.getBody().get("data").get("id").asText();
    }
    throw new RuntimeException("Failed to attach AudioBridge plugin");
  }

  public void createAudioRoom(String sessionId, String handleId, int roomId) {
    Map<String, Object> body = Map.of(
      "request", "create",
      "room", roomId,
      "description", "SpaceHub Voice Room",
      "private", false
    );

    Map<String, Object> request = Map.of(
      "janus", "message",
      "transaction", UUID.randomUUID().toString(),
      "body", body
    );

    String handleUrl = String.format("%s/%s/%s", janusUrl, sessionId, handleId);
    restTemplate.postForEntity(handleUrl, request, JsonNode.class);
  }
}
