package com.mc_host.api.model.panel.request.transfer;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MultipartFile implementation that streams from a temporary file
 */
public class TempFileMultipartFile implements MultipartFile {
	private final String name;
	private final String originalFilename;
	private final Path tempFile;

	public TempFileMultipartFile(String name, String originalFilename, Path tempFile) {
		this.name = name;
		this.originalFilename = originalFilename;
		this.tempFile = tempFile;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	@Override
	public String getContentType() {
		return "application/gzip";
	}

	@Override
	public boolean isEmpty() {
		try {
			return Files.size(tempFile) == 0;
		} catch (IOException e) {
			return true;
		}
	}

	@Override
	public long getSize() {
		try {
			return Files.size(tempFile);
		} catch (IOException e) {
			return 0;
		}
	}

	@Override
	public byte[] getBytes() throws IOException {
		// avoid this for large files - defeats the purpose of streaming
		return Files.readAllBytes(tempFile);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Files.newInputStream(tempFile);
	}

	@Override
	public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
		Files.copy(tempFile, dest.toPath());
	}
}