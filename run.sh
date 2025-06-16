#!/bin/bash
set -e

echo -e "\033[32mBuilding and running Spring Boot application...\033[0m"

# Load environment variables from .env file
echo -e "\033[36mLoading environment variables from .env file...\033[0m"
if [ -f ".env" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        # Strip carriage returns
        line=$(echo "$line" | tr -d '\r')
        
        # Skip comments and empty lines
        if [[ $line =~ ^[[:space:]]*# ]] || [[ -z "$line" ]]; then
            continue
        fi
        
        # Only process lines that contain =
        if [[ $line == *"="* ]]; then
            name="${line%%=*}"
            value="${line#*=}"
            
            # Trim whitespace from name
            name=$(echo "$name" | xargs)
            
            # Remove surrounding quotes
            if [[ $value =~ ^\".*\"$ ]]; then
                value="${value:1:-1}"
            elif [[ $value =~ ^\'.*\'$ ]]; then
                value="${value:1:-1}"
            fi
            
            # Only export if name is valid
            if [[ $name =~ ^[a-zA-Z_][a-zA-Z0-9_]*$ ]]; then
                export "$name"="$value"
                echo -e "\033[90mLoaded: $name\033[0m"
            fi
        fi
    done < .env
else
    echo -e "\033[33mWarning: .env file not found\033[0m"
fi

# Clean and package the application
echo -e "\033[36mBuilding the application with Maven...\033[0m"
mvn clean package -DskipTests

# Docker up
docker compose up -d

# Start stripe webhook listener
stripe listen --forward-to localhost:8080/api/stripe/webhook > webhook.log 2>&1 &
echo -e "\033[36mStripe Webhook listener started with pid $!...\033[0m"

if [ $? -eq 0 ]; then
    # Run the application
    echo -e "\033[36mStarting the Spring Boot application...\033[0m"
    mvn spring-boot:run
else
    echo -e "\033[31mMaven build failed!\033[0m"
    exit 1
fi
