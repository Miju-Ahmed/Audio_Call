# VoiceCast — Mass Voice Calling & Broadcast System

A scalable telephony-based voice communication system that allows an administrator to initiate bulk voice calls (500+ users) to Bangladeshi phone numbers. End users receive a normal phone call and are connected to a voice broadcast or conference — **no app or internet required**.

## Architecture

- **Backend:** Java Spring Boot 3.2 + Twilio SDK
- **Frontend:** React 19 (Vite) with premium dark-mode UI
- **Database:** PostgreSQL
- **Real-time:** WebSocket (STOMP) for live call monitoring
- **Telephony:** Twilio Programmable Voice (swappable via provider interface)

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- PostgreSQL 14+
- Twilio Account (Account SID, Auth Token, Phone Number)

### 1. Database Setup
```sql
CREATE DATABASE voice_broadcast;
```

### 2. Backend
```bash
cd backend

# Configure environment variables
export TWILIO_ACCOUNT_SID=your_account_sid
export TWILIO_AUTH_TOKEN=your_auth_token
export TWILIO_PHONE_NUMBER=+1234567890
export WEBHOOK_BASE_URL=https://your-public-url.com  # Use ngrok for dev

# Run
./mvnw spring-boot:run
```
Backend runs on `http://localhost:8080`

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`

### 4. Webhook Setup (Development)
Twilio needs to reach your backend via a public URL:
```bash
# Install ngrok
ngrok http 8080

# Copy the HTTPS URL and set it as WEBHOOK_BASE_URL
```

## Features

| Feature | Description |
|---------|-------------|
| **Bulk Calling** | Batched outbound calls (50/batch) with configurable concurrency |
| **Auto-Retry** | Failed/busy/no-answer calls retried up to 3 times |
| **Broadcast Mode** | Admin speaks, all listeners are muted |
| **Interactive Mode** | Moderated conference with mute/unmute controls |
| **Live Monitoring** | Real-time WebSocket updates on call status |
| **CSV Upload** | Bulk import phone numbers from CSV files |
| **Call Logs** | Full history with export to CSV |

## API Endpoints

### Phone Numbers
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/phone-numbers` | List (paginated, filterable) |
| POST | `/api/phone-numbers` | Add single number |
| POST | `/api/phone-numbers/upload` | Bulk CSV upload |
| PUT | `/api/phone-numbers/{id}` | Update |
| DELETE | `/api/phone-numbers/{id}` | Delete |

### Sessions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/sessions` | List all sessions |
| POST | `/api/sessions` | Create session |
| POST | `/api/sessions/{id}/start` | Start calling |
| POST | `/api/sessions/{id}/stop` | Stop session |
| GET | `/api/sessions/{id}/stats` | Live statistics |
| GET | `/api/sessions/{id}/logs` | Call logs |

### Webhooks (Twilio)
| Endpoint | Purpose |
|----------|---------|
| `/webhook/voice` | TwiML for conference join |
| `/webhook/status` | Call status callbacks |
| `/webhook/conference-status` | Conference events |

## Configuration

Key settings in `backend/src/main/resources/application.yml`:

```yaml
app:
  call:
    batch-size: 50          # Calls per batch
    batch-delay-ms: 2000    # Delay between batches
    max-concurrent: 200     # Max concurrent calls
    retry-max: 3            # Max retry attempts
    retry-delay-seconds: 60 # Seconds between retry cycles
```

## BTRC Compliance Notes

- Obtain user consent before adding numbers
- Implement opt-out/DND mechanisms
- Route calls through authorized IGW operators
- Maintain call logs for regulatory audits
