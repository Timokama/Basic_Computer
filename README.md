# Basic Computer - Computer Science Learning Platform

A pure Java web application for accessing computer science study materials including notes, videos, sample questions, and exams. Payment is handled via M-Pesa integration.

## Features

- **User Authentication**: Login and Registration system
- **M-Pesa Payment Integration**: Subscribe via M-Pesa STK Push
- **Content Management**: Notes, Videos, Sample Questions, Exams & Answers
- **Multiple Subscription Plans**: Monthly, Quarterly, Yearly

## Requirements

1. **Java Development Kit (JDK)**: JDK 8 or higher
2. **PostgreSQL Database**: PostgreSQL 12 or higher
3. **PostgreSQL JDBC Driver**: postgresql-42.7.8.jar

## Database Setup

1. Install PostgreSQL
2. Create a new database named `basiccomputer`:
   ```sql
   CREATE DATABASE basiccomputer;
   ```
3. The application will automatically create the required tables on first run.

## Setup Instructions

### 1. Download PostgreSQL JDBC Driver

Download `postgresql-42.7.8.jar` from:
https://jdbc.postgresql.org/download/postgresql-42.7.8.jar

Place it in the `lib` folder.

### 2. Configure Database Connection

Edit `src/com/basiccomputer/db/Database.java` and update the following:
```java
private static final String URL = "jdbc:postgresql://localhost:5432/basiccomputer";
private static final String USER = "postgres";      // Your PostgreSQL username
private static final String PASSWORD = "postgres";  // Your PostgreSQL password
```

### 3. Configure M-Pesa (Optional - for production)

Edit `src/com/basiccomputer/handler/MpesaStkPushHandler.java` and update:
- CONSUMER_KEY
- CONSUMER_SECRET
- SHORTCODE
- PASSKEY

### 4. Run the Application

```batch
run.bat
```

The server will start on `http://localhost:8082`

## Network Access

For local network access (other devices on same WiFi):
- Find your local IP address (run `ipconfig` on Windows)
- Access via `http://[YOUR_IP]:8082`

For global internet access:
- Use port forwarding on your router, OR
- Use a tunneling service like ngrok: `ngrok http 8082`

## Project Structure

```
basic-computer/
├── src/
│   └── com/
│       └── basiccomputer/
│           ├── Main.java              # Main server entry point
│           ├── db/
│           │   └── Database.java      # Database operations
│           └── handler/
│               ├── RootHandler.java       # Home page
│               ├── LoginHandler.java      # Login page
│               ├── RegisterHandler.java  # Registration page
│               ├── LogoutHandler.java     # Logout
│               ├── DashboardHandler.java  # User dashboard
│               ├── ContentHandler.java   # Content listing
│               ├── PaymentHandler.java   # Subscription/payment
│               ├── MpesaCallbackHandler.java   # M-Pesa callback
│               └── MpesaStkPushHandler.java    # STK push
├── lib/
│   └── postgresql-42.7.8.jar    # PostgreSQL JDBC Driver
├── run.bat                      # Build and run script
└── README.md                    # This file
```

## Usage Flow

1. **Home Page**: Visit `http://localhost:8082`
2. **Register**: Create a new account
3. **Login**: Log in with your credentials
4. **Dashboard**: Access the main dashboard
5. **Subscribe**: Choose a subscription plan and pay with M-Pesa
6. **Content**: Access notes, videos, sample questions, and exams

## Subscription Plans

- **Monthly**: KSh 500/month
- **Quarterly**: KSh 1,200/3 months (save KSh 300)
- **Yearly**: KSh 4,000/year (save KSh 2,000)

## Content Categories

- Programming
- Data Structures
- Algorithms
- Databases
- Web Development
- Operating Systems
- Computer Networks
- Software Engineering
- Artificial Intelligence
- Computer Architecture
- Mathematics
- MS Word
- MS Excel
- MS Access Database
- MS PowerPoint
- MS Publisher
- Internet and Email
- Emerging Trends