package com.mc_host.api.controller.api.subscriptions.panel;

import com.mc_host.api.auth.ValidatedSubscription;
import com.mc_host.api.model.resource.pterodactyl.file.FileObject;
import com.mc_host.api.model.resource.pterodactyl.file.SignedUrl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("api/panel/user/subscription/{subscriptionId}/file")
public interface FileController {

    @GetMapping("list")
    ResponseEntity<List<FileObject>> listFiles(
        @ValidatedSubscription String subscriptionId,
        @RequestParam String directory
    );

    @GetMapping("contents")
    ResponseEntity<String> getFileContents(
        @ValidatedSubscription String subscriptionId,
        @RequestParam String file
    );

    @GetMapping("download")
    ResponseEntity<SignedUrl> getFileDownloadLink(
        @ValidatedSubscription String subscriptionId,
        @RequestParam String file
    );

    @PostMapping("upload")
    ResponseEntity<Void> uploadFile(
        @ValidatedSubscription String subscriptionId,
        @RequestParam("files") MultipartFile file
    ) throws IOException, InterruptedException;

    @PutMapping("rename")
    ResponseEntity<Void> renameFiles(
        @ValidatedSubscription String subscriptionId,
        @RequestBody RenameRequest request
    );

    @PostMapping("copy")
    ResponseEntity<Void> copyFile(
        @ValidatedSubscription String subscriptionId,
        @RequestBody CopyFileRequest request
    );

    @PostMapping("write")
    ResponseEntity<Void> writeFile(
        @ValidatedSubscription String subscriptionId,
        @RequestParam String file,
        @RequestBody String content
    );

    @PostMapping("compress")
    ResponseEntity<FileObject> compressFiles(
        @ValidatedSubscription String subscriptionId,
        @RequestBody CompressRequest request
    );

    @PostMapping("decompress")
    ResponseEntity<Void> decompressFile(
        @ValidatedSubscription String subscriptionId,
        @RequestBody DecompressRequest request
    );

    @PostMapping("delete")
    ResponseEntity<Void> deleteFiles(
        @ValidatedSubscription String subscriptionId,
        @RequestBody DeleteRequest request
    );

    @PostMapping("create-folder")
    ResponseEntity<Void> createFolder(
        @ValidatedSubscription String subscriptionId,
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