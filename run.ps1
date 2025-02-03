# Build and Run Spring Boot Project
$ErrorActionPreference = "Stop"

Write-Host "Building and running Spring Boot application..." -ForegroundColor Green

try {
    # Load environment variables from .env file
    Write-Host "Loading environment variables from .env file..." -ForegroundColor Cyan
    if (Test-Path ".env") {
        Get-Content ".env" | ForEach-Object {
            if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
                $name = $matches[1].Trim()
                $value = $matches[2].Trim()
                # Remove surrounding quotes if they exist
                $value = $value -replace '^[''"]|[''"]$'
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
                Write-Host "Loaded: $name" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "Warning: .env file not found" -ForegroundColor Yellow
    }

    # Clean and package the application
    Write-Host "Building the application with Maven..." -ForegroundColor Cyan
    mvn clean package -DskipTests

    if ($LASTEXITCODE -eq 0) {
        # Run the application
        Write-Host "Starting the Spring Boot application..." -ForegroundColor Cyan
        mvn spring-boot:run
    } else {
        Write-Host "Maven build failed!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}