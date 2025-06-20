package com.mc_host.api.service.panel;

import com.mc_host.api.client.PterodactylUserClient;
import com.mc_host.api.controller.panel.FileResource;
import com.mc_host.api.model.provisioning.Context;
import com.mc_host.api.model.resource.pterodactyl.PterodactylServer;
import com.mc_host.api.model.resource.pterodactyl.file.FileObject;
import com.mc_host.api.model.resource.pterodactyl.file.SignedUrl;
import com.mc_host.api.repository.GameServerRepository;
import com.mc_host.api.repository.ServerExecutionContextRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService implements FileResource {

    private final PterodactylUserClient pterodactylClient;
    private final ServerExecutionContextRepository serverExecutionContextRepository;
    private final GameServerRepository gameServerRepository;

    @Override
    public ResponseEntity<List<FileObject>> listFiles(String userId, String subscriptionId, String directory) {
        var serverUid = getServerUid(subscriptionId);
        var files = pterodactylClient.listFiles(serverUid, directory);
        return ResponseEntity.ok(files);
    }

    @Override
    public ResponseEntity<String> getFileContents(String userId, String subscriptionId, String file) {
        var serverUid = getServerUid(subscriptionId);
        var contents = pterodactylClient.getFileContents(serverUid, file);
        return ResponseEntity.ok(contents);
    }

    @Override
    public ResponseEntity<SignedUrl> getFileDownloadLink(String userId, String subscriptionId, String file) {
        var serverUid = getServerUid(subscriptionId);
        var signedUrl = pterodactylClient.getFileDownloadLink(serverUid, file);
        return ResponseEntity.ok(signedUrl);
    }

    @Override
    public ResponseEntity<Void> uploadFile(String userId, String subscriptionId, MultipartFile file) throws IOException, InterruptedException {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.uploadFile(serverUid, file);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> renameFiles(String userId, String subscriptionId, RenameRequest request) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.renameFiles(serverUid, request.root(), request.files());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> copyFile(String userId, String subscriptionId, CopyFileRequest request) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.copyFile(serverUid, request.location());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> writeFile(String userId, String subscriptionId, String file, String content) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.writeFile(serverUid, file, content);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<FileObject> compressFiles(String userId, String subscriptionId, CompressRequest request) {
        var serverUid = getServerUid(subscriptionId);
        var fileObject = pterodactylClient.compressFiles(serverUid, request.root(), request.files());
        return ResponseEntity.ok(fileObject);
    }

    @Override
    public ResponseEntity<Void> decompressFile(String userId, String subscriptionId, DecompressRequest request) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.decompressFile(serverUid, request.root(), request.file());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteFiles(String userId, String subscriptionId, DeleteRequest request) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.deleteFiles(serverUid, request.root(), request.files());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> createFolder(String userId, String subscriptionId, CreateFolderRequest request) {
        var serverUid = getServerUid(subscriptionId);
        pterodactylClient.createFolder(serverUid, request.root(), request.name());
        return ResponseEntity.noContent().build();
    }

    private String getServerUid(String subscriptionId) {
        Long serverId = serverExecutionContextRepository.selectSubscription(subscriptionId)
            .map(Context::getPterodactylServerId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
        return gameServerRepository.selectPterodactylServer(serverId)
            .map(PterodactylServer::pterodactylServerUid)
            .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404)));
    }
}