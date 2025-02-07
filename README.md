# finease
FinEase is a modern, efficient platform designed to streamline financial operations like money transfers, credit, and debit management. Built for reliability and scalability, FinEase ensures seamless handling of transactions, making financial processes effortless and accessible.
## Features

- **Spring Boot Wallet Service**:
    - Authentication using Spring Security context.
    - RESTful APIs for managing accounts, transactions, and sessions.
    - Unit tests for key functionalities.
    - Centralized logging for monitoring and debugging.
- **ATM CLI Application**:
    - Bash script-based CLI for simulating ATM interactions.
    - Supports login, deposit, withdrawal, transfer, and logout operations.
    - Error handling for invalid operations.
- **Database**:
    - PostgreSQL database with four tables:
        1. `session`
        2. `account`
        3. `transaction`
        4. `owed_transaction`
- **Docker Compose**:
    - Multi-container setup including:
        - Wallet Service
        - ATM CLI
        - PostgreSQL database.

## Prerequisites

- Docker and Docker Compose installed on your system.
- Java 21 for building the Spring Boot service using Maven.
- Bash shell for running the ATM CLI.

## Running the Project

### 1. Build the Wallet Service
Before running the Docker containers, ensure the Wallet Service JAR is built using Maven:

```bash
mvn clean install
```


### 2. Start the Services

Use the following commands to manage the Docker Compose environment:

- To start the services and build the containers:

```bash
docker-compose up --build -d
```

- To rebuild containers without using cache:

```bash
docker-compose build --no-cache
```

- To stop and remove all running containers:

```bash
docker-compose down
```

Once the containers are running, the services include:

- **ATM Service**
- **Wallet Service** (Spring Boot application)
- **PostgreSQL Database**

```bash
docker-compose up --build
```

This will start the following containers:

- **ATM Service**
- **Wallet Service** (Spring Boot application)
- **PostgreSQL Database**

### 3. Testing the ATM CLI

After starting the services, execute the ATM CLI from the container:

```bash
docker exec -it atm-service /app/start.sh
```

####  Sample Commands:

```bash
$ login bob
Hello, bob!
Your balance is $0

$ deposit 10
Your balance is $10.00

$ logout
Goodbye, bob!

$ login alice
Hello, alice!
Your balance is $0

$ deposit 5
Your balance is $5.00

$ transfer bob 10
Transferred $5.00 to bob
Owed $5.00 to bob
Your balance is $0

$ logout
Goodbye, alice!

$ login bob
Hello, bob!
Owed $5.00 by alice
Your balance is $15.00

$ transfer alice 10
Transferred $10.00 to alice
Owed $5.00 to alice
Your balance is $5.00

$ deposit 10
Your balance is $10.00

$ transfer alice 5
Transferred $5.00 to alice
Owed $5.00 to alice
Your balance is $5.00

$ logout
Goodbye, bob!
```

### 4. Testing with HTTP Requests

Example HTTP requests are provided for manual testing. Below are sample requests:

#### Create Account

```http
POST http://localhost:8080/v1/login
Content-Type: application/json
application-id: zMrC1R0MyaRAyRxRrTPZlA
api-key: 18673025e2954863875913d2e88588b080777e9038ef483e83413773f518df55

{
  "account_name": "alice"
}
```

#### Create Session

```http
POST http://localhost:8080/v1/session
Content-Type: application/json
secret_key: my-secret-key
```

#### Deactivate Session

```http
PUT http://localhost:8080/v1/session/deactivate
Content-Type: application/json
secret_key: my-secret-key

{
  "client_name": "Valid Client",
  "application_id": "_VqE4FpW-ADs9T_ktFBAbg",
  "api_key": "390391c916b2452f8b6b4c42615b5366ac19ca31bf2c487cb6d66ffb27f13d43"
}
```

#### Transaction API

```http
POST http://localhost:8080/v2/transactions
Content-Type: application/json
application-id: zMrC1R0MyaRAyRxRrTPZlA
api-key: 18673025e2954863875913d2e88588b080777e9038ef483e83413773f518df55
account-id: RFID1000000022

{
  "reference_id": "1234546789",
  "amount": 1000.00,
  "transaction_type": "TRANSFER",
  "recipient": "alice"
}
```

### 5. Logs

Logs for the ATM Service are stored in the container under `/app/activity.log`. You can view them with:

```bash
docker exec -it atm-service cat /app/activity.log
```

Logs for the Wallet Service can be viewed using:

```bash
docker logs wallet-service
```

