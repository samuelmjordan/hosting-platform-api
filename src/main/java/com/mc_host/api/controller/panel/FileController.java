package com.mc_host.api.controller.panel;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mc_host.api.model.resource.pterodactyl.request.GetDirectoryRequest;
import com.mc_host.api.model.resource.pterodactyl.request.NewDirectoryRequest;
import com.mc_host.api.model.resource.pterodactyl.request.CompressFilesRequest;
import com.mc_host.api.model.resource.pterodactyl.request.CopyFileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.DecompressFileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.DeleteFilesRequest;
import com.mc_host.api.model.resource.pterodactyl.request.FileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.RenameRequest;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/file")
public interface FileController {

    @GetMapping("/directory")
    public ResponseEntity<List<String>> getFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody GetDirectoryRequest request
    );

    @GetMapping("/contents")
    public ResponseEntity<String> getFileContents(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody FileRequest request
    );

    @GetMapping("/download")
    public ResponseEntity<String> getFileDownloadLink(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody FileRequest request
    );

    @GetMapping("/upload") 
    public ResponseEntity<String> getFileUploadLink(
        @PathVariable String userId,
        @PathVariable String subscriptionId
    );

    @PutMapping("/rename")
    public ResponseEntity<Void> renameFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody RenameRequest request 
    );

    @PostMapping("/directory")
    public ResponseEntity<Void> createDirectory(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody NewDirectoryRequest request 
    );

    @PostMapping("/copy")
    public ResponseEntity<Void> copyFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody CopyFileRequest request 
    );

    @PostMapping("/write")
    public ResponseEntity<Void> writeFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody String content 
    );

    @PostMapping("/delete")
    public ResponseEntity<Void> deleteFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody DeleteFilesRequest request 
    );

    @PostMapping("/compress")
    public ResponseEntity<Void> compressFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody CompressFilesRequest request 
    );

    @PostMapping("/decompress")
    public ResponseEntity<Void> decompressFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody DecompressFileRequest request 
    );

}
