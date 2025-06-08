package com.mc_host.api.service.panel;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.mc_host.api.controller.panel.FileController;
import com.mc_host.api.model.resource.pterodactyl.request.CompressFilesRequest;
import com.mc_host.api.model.resource.pterodactyl.request.CopyFileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.DecompressFileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.DeleteFilesRequest;
import com.mc_host.api.model.resource.pterodactyl.request.FileRequest;
import com.mc_host.api.model.resource.pterodactyl.request.GetDirectoryRequest;
import com.mc_host.api.model.resource.pterodactyl.request.NewDirectoryRequest;
import com.mc_host.api.model.resource.pterodactyl.request.RenameRequest;

public class FileService implements FileController {

    @Override
    public ResponseEntity<List<String>> getFiles(String userId, String subscriptionId, GetDirectoryRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFiles'");
    }

    @Override
    public ResponseEntity<String> getFileContents(String userId, String subscriptionId, FileRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileContents'");
    }

    @Override
    public ResponseEntity<String> getFileDownloadLink(String userId, String subscriptionId, FileRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileDownloadLink'");
    }

    @Override
    public ResponseEntity<String> getFileUploadLink(String userId, String subscriptionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getFileUploadLink'");
    }

    @Override
    public ResponseEntity<Void> renameFiles(String userId, String subscriptionId, RenameRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'renameFiles'");
    }

    @Override
    public ResponseEntity<Void> createDirectory(String userId, String subscriptionId, NewDirectoryRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createDirectory'");
    }

    @Override
    public ResponseEntity<Void> copyFile(String userId, String subscriptionId, CopyFileRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'copyFile'");
    }

    @Override
    public ResponseEntity<Void> writeFile(String userId, String subscriptionId, String content) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeFile'");
    }

    @Override
    public ResponseEntity<Void> deleteFile(String userId, String subscriptionId, DeleteFilesRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteFile'");
    }

    @Override
    public ResponseEntity<Void> compressFiles(String userId, String subscriptionId, CompressFilesRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'compressFiles'");
    }

    @Override
    public ResponseEntity<Void> decompressFile(String userId, String subscriptionId, DecompressFileRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'decompressFile'");
    }
    
}
