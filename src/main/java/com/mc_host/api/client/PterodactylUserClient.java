package com.mc_host.api.client;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mc_host.api.configuration.PterodactylConfiguration;
import com.mc_host.api.controller.panel.FileController.*;
import com.mc_host.api.model.resource.pterodactyl.PowerState;
import com.mc_host.api.model.resource.pterodactyl.file.FileObject;
import com.mc_host.api.model.resource.pterodactyl.file.SignedUrl;
import com.mc_host.api.model.resource.pterodactyl.panel.WebsocketCredentials;

@Service
public class PterodactylUserClient extends BaseApiClient {

    private final PterodactylConfiguration config;

    PterodactylUserClient(
        PterodactylConfiguration config,
        HttpClient httpClient,
        ObjectMapper objectMapper
    ) {
        super(httpClient, objectMapper);
        this.config = config;
    }

    @Override
    protected String getApiBase() {
        return config.getApiBase();
    }

    @Override
    protected String getAuthorizationHeader() {
        return "Bearer " + config.getClientApiToken();
    }

    // SERVERS
    public void setPowerState(String serverUid, PowerState state) {
        var action = Map.of("signal", state.toString());
        sendRequest("POST", "/api/client/servers/" + serverUid + "/power", action);
    }
    
    public void acceptMinecraftEula(String serverUid) {
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/write?file=eula.txt", "eula=true");
    }

    public ServerStatus getServerStatus(String serverUid) {
        var resources = getServerResources(serverUid);
        return ServerStatus.valueOf(resources.attributes().current_state().toUpperCase());
    }

    public ServerResourcesResponse getServerResources(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/resources");
        return deserialize(response, ServerResourcesResponse.class);
    }

    public WebsocketCredentialsResponse getWebsocketCredentials(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/websocket");
        return deserialize(response, WebsocketCredentialsResponse.class);
    }

    public void sendConsoleCommand(String serverUid, String command) {
        var payload = Map.of("command", command);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/command", payload);
    }

    // FILE MANAGEMENT
    public List<FileObject> listFiles(String serverUid, String directory) {
        var encodedDirectory = encodeFilePath(directory);
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/files/list?directory=" + encodedDirectory);
        var listResponse = deserialize(response, FileListResponse.class);
        return listResponse.data().stream().toList();
    }

    public String getFileContents(String serverUid, String file) {
        var encodedFile = encodeFilePath(file);
        return sendRequest("GET", "/api/client/servers/" + serverUid + "/files/contents?file=" + encodedFile);
    }

    public SignedUrl getFileDownloadLink(String serverUid, String file) {
        var encodedFile = encodeFilePath(file);
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/files/download?file=" + encodedFile);
        return deserialize(response, SignedUrl.class);
    }

    public SignedUrl getFileUploadLink(String serverUid) {
        var response = sendRequest("GET", "/api/client/servers/" + serverUid + "/files/upload");
        return deserialize(response, SignedUrl.class);
    }

    public void renameFiles(String serverUid, String root, List<RenameItem> files) {
        var payload = Map.of("root", root, "files", files);
        sendRequest("PUT", "/api/client/servers/" + serverUid + "/files/rename", payload);
    }

    public void copyFile(String serverUid, String location) {
        var payload = Map.of("location", location);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/copy", payload);
    }

    public void writeFile(String serverUid, String file, String content) {
        var encodedFile = encodeFilePath(file);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/write?file=" + encodedFile, content);
    }

    public FileObject compressFiles(String serverUid, String root, List<String> files) {
        var payload = Map.of("root", root, "files", files);
        var response = sendRequest("POST", "/api/client/servers/" + serverUid + "/files/compress", payload);
        return deserialize(response, FileObject.class);
    }

    public void decompressFile(String serverUid, String root, String file) {
        var payload = Map.of("root", root, "file", file);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/decompress", payload);
    }

    public void deleteFiles(String serverUid, String root, List<String> files) {
        var payload = Map.of("root", root, "files", files);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/delete", payload);
    }

    public void createFolder(String serverUid, String root, String name) {
        var payload = Map.of("root", root, "name", name);
        sendRequest("POST", "/api/client/servers/" + serverUid + "/files/create-folder", payload);
    }

    private String encodeFilePath(String file) {
        return URLEncoder.encode(file, StandardCharsets.UTF_8);
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("failed to deserialize response", e);
        }
    }

    public enum ServerStatus {
        RUNNING, STARTING, STOPPING, STOPPED, OFFLINE
    }

    public record ServerResourcesResponse(ServerResourcesAttributes attributes) {}
    public record ServerResourcesAttributes(
        String current_state,
        boolean is_suspended,
        ResourceStats resources
    ) {}
    
    public record ResourceStats(
        long memory_bytes,
        long memory_limit_bytes,
        double cpu_absolute,
        long network_rx_bytes,
        long network_tx_bytes,
        long disk_bytes,
        String uptime
    ) {}

    public record WebsocketCredentialsResponse(WebsocketCredentials data) {}

    public record FileListResponse(String object, List<FileObject> data) {}

    public record PterodactylUserResponse(UserAttributes attributes) {}
    public record UserAttributes(
        Long id,
        String email,
        String username,
        String first_name,
        String last_name,
        String language,
        Boolean root_admin,
        Boolean use_totp,
        String created_at,
        String updated_at
    ) {}
}