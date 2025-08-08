# MC Host API

A comprehensive Minecraft hosting platform backend API built with Spring Boot. This system provides automated server provisioning, payment processing, and infrastructure management for Minecraft game servers.

## 🚀 Features

### Core Functionality
- **Automated Server Provisioning** - Streamlined deployment of Minecraft servers with multiple game types
- **Multi-Provider Infrastructure** - Integration with Hetzner Cloud & Robot for scalable hosting
- **Payment Processing** - Complete Stripe integration for subscriptions and billing
- **User Authentication** - Clerk-based user management system
- **Real-time Console Access** - WebSocket-based server console management
- **File Management** - SFTP and web-based file operations
- **Server Monitoring** - Prometheus metrics and health monitoring

### Supported Game Types
- **Vanilla Minecraft** - Official server software
- **Paper** - High-performance server with plugin support  
- **Fabric** - Modding platform with lightweight performance
- **Forge** - Traditional modding framework
- **BungeeCord** - Proxy server for network setups
- **Bedrock Edition** - Cross-platform Minecraft support
- **Modpacks** - CurseForge and Tekkit integration

### Infrastructure Management
- **Dynamic Node Allocation** - Automatic server provisioning on Hetzner infrastructure
- **DNS Management** - Automated Cloudflare DNS record creation
- **Server Creation** - Automatic pterodactyl server management

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.4.1 with Java 21
- **Database**: PostgreSQL with Flyway migrations
- **Authentication**: Clerk (OAuth2)
- **Payments**: Stripe API integration
- **Infrastructure**: Hetzner Cloud & Robot APIs
- **DNS**: Cloudflare API
- **Game Panel**: Pterodactyl Panel integration
- **Monitoring**: Prometheus, Grafana, Sentry
- **Security**: Spring Security with JWT validation

## 🏗 Architecture

### Key Components

#### Provisioning System
The core provisioning system uses a step-based workflow engine:
- **ServerExecutor** - Orchestrates multi-step server deployment
- **Step-based Processing** - Modular deployment stages (allocation, configuration, installation)
- **Context Management** - Persistent state tracking throughout provisioning

#### API Structure
- **REST Controllers** - Clean separation of concerns
  - User management (`/api/user`)
  - Subscription management (`/api/subscriptions`)
  - Server panel operations (`/api/subscriptions/panel`)
- **WebSocket Support** - Real-time console access
- **Webhook Handlers** - Stripe and Clerk event processing

#### Data Layer
- **Repository Pattern** - Clean data access abstraction
- **Flyway Migrations** - Version-controlled database schema
- **Connection Pooling** - Optimized database performance with HikariCP

## 🚦 Getting Started

### Prerequisites
- Java 21+
- PostgreSQL 12+
- Maven 3.6+

### Environment Variables
```bash
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DATABASE=mchost
POSTGRES_USER=your_user
POSTGRES_PASSWORD=your_password

# External Services
STRIPE_SECRET_KEY=sk_test_...
CLERK_SECRET_KEY=sk_...
CLOUDFLARE_SECRET_KEY=...
HETZNER_SECRET_KEY=...
PANEL_HOST=https://your-panel.com
PANEL_SECRET_KEY=ptla_...

# Infrastructure
CLOUD_DOMAIN=your-cloud-domain.com
INFRA_DOMAIN=your-infra-domain.com
```

### Running Locally

1. **Start Dependencies**
   ```bash
   docker compose up postgres prometheus grafana
   ```

2. **Run Application**
   ```bash
   ./mvnw spring-boot:run
   ```
   Or use the platform-specific scripts:
   ```bash
   # Linux/macOS
   ./run.sh
   
   # Windows
   ./run.ps1
   ```

4. **Webhooks**
   - Try pointing your clerk webhooks at ```/clerk/webhook```. If running locally you can use cloudflare tunnels.
   - The run script will start the stripe listener, but this still needs to be configured in stripe to point to ```/stripe/webhook```

4. **Access Services**
   - API: http://localhost:8080
   - Grafana: http://localhost:3001 (admin/admin)
   - Prometheus: http://localhost:9090

## 📊 Monitoring

The application includes comprehensive monitoring:
- **Health Endpoints** - Spring Actuator health checks
- **Prometheus Metrics** - Custom metrics for business logic
- **Grafana Dashboards** - Pre-configured monitoring dashboards
- **Sentry Integration** - Error tracking and performance monitoring

## 🔒 Security Features

- **JWT Token Validation** - Secure API access
- **CORS Configuration** - Controlled cross-origin requests
- **Input Validation** - Request validation and sanitization
- **Secret Management** - Environment-based configuration

## 📈 Scalability

- **Async Processing** - Non-blocking operations for provisioning
- **Job Queue System** - Background task processing
- **Connection Pooling** - Optimized database connections
- **Multi-node Support** - Horizontal scaling capabilities

## 🔮 Roadmap

- [ ] Dedicated Hetzner server support
- [ ] Enhanced backup system implementation
- [ ] Improved API error handling
- [ ] CurseForge modpack integration

---