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
                Write-Host "Loaded: $name=$value" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "Warning: .env file not found" -ForegroundColor Yellow
    }

    # Clean and package the application
    Write-Host "Building the application with Maven..." -ForegroundColor Cyan
    mvn clean package -DskipTests

    if ($LASTEXITCODE -eq 0) {
        # Start Stripe listener in background
        Write-Host "Starting Stripe webhook listener..." -ForegroundColor Cyan
        $stripeJob = Start-Job -ScriptBlock {
            stripe listen --forward-to localhost:8080/api/stripe/webhook
        }
        
        # Give stripe a moment to start
        Start-Sleep -Seconds 2
        
        # Run the application
        Write-Host "Starting the Spring Boot application..." -ForegroundColor Cyan
        mvn spring-boot:run
        
        # Clean up stripe listener when app exits
        Stop-Job $stripeJob -ErrorAction SilentlyContinue
        Remove-Job $stripeJob -ErrorAction SilentlyContinue
    } else {
        Write-Host "Maven build failed!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
    exit 1
}