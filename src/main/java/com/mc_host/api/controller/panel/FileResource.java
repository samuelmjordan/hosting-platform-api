package com.mc_host.api.controller.panel;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.mc_host.api.model.resource.pterodactyl.file.FileObject;
import com.mc_host.api.model.resource.pterodactyl.file.SignedUrl;

import java.util.List;

@RestController
@RequestMapping("/api/panel/user/{userId}/subscription/{subscriptionId}/file")
public interface FileResource {

    @GetMapping("/list")
    ResponseEntity<List<FileObject>> listFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestParam String directory
    );

    @GetMapping("/contents")
    ResponseEntity<String> getFileContents(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestParam String file
    );

    @GetMapping("/download")
    ResponseEntity<SignedUrl> getFileDownloadLink(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestParam String file
    );

    @PostMapping("/upload")
    ResponseEntity<Void> uploadFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestParam("files") MultipartFile file
    );

    @PutMapping("/rename")
    ResponseEntity<Void> renameFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody RenameRequest request
    );

    @PostMapping("/copy")
    ResponseEntity<Void> copyFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody CopyFileRequest request
    );

    @PostMapping("/write")
    ResponseEntity<Void> writeFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestParam String file,
        @RequestBody String content
    );

    @PostMapping("/compress")
    ResponseEntity<FileObject> compressFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody CompressRequest request
    );

    @PostMapping("/decompress")
    ResponseEntity<Void> decompressFile(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody DecompressRequest request
    );

    @PostMapping("/delete")
    ResponseEntity<Void> deleteFiles(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody DeleteRequest request
    );

    @PostMapping("/create-folder")
    ResponseEntity<Void> createFolder(
        @PathVariable String userId,
        @PathVariable String subscriptionId,
        @RequestBody CreateFolderRequest request
    );

    // Request DTOs
    record RenameRequest(String root, List<RenameItem> files) {}
    record RenameItem(String from, String to) {}
    record CopyFileRequest(String location) {}
    record CompressRequest(String root, List<String> files) {}
    record DecompressRequest(String root, String file) {}
    record DeleteRequest(String root, List<String> files) {}
    record CreateFolderRequest(String root, String name) {}
}