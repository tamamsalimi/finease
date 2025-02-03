#!/bin/bash

API_BASE_URL="http://localhost:8080"
SESSION_FILE="session.txt"
LOGIN_SESSION_FILE="session_login.txt"
SECRET_KEY="my-secret-key"
REFERENCE_ID=$(date +%s)
ACCOUNT_ID=""
ACCOUNT_NAME=""

start_session() {
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v1/session" -H "Content-Type: application/json" -H "secret_key: $SECRET_KEY")
    API_KEY=$(echo "$RESPONSE" | jq -r '.api_key')
    APPLICATION_ID=$(echo "$RESPONSE" | jq -r '.application_id')
    echo "$API_KEY $APPLICATION_ID" > "$SESSION_FILE"
}

stop_session() {
    if [ -f "$SESSION_FILE" ]; then
        read API_KEY APPLICATION_ID < "$SESSION_FILE"
        curl -s -X PUT "$API_BASE_URL/v1/session/deactivate" -H "Content-Type: application/json" -H "secret_key: $SECRET_KEY" -d "{\"application_id\": \"$APPLICATION_ID\", \"api_key\": \"$API_KEY\"}"
        echo "$API_KEY $APPLICATION_ID" > "$SESSION_FILE"
    fi
}

parse_transaction_response() {
    RESPONSE=$1
    if echo "$RESPONSE" | jq -e '.results | length > 0' >/dev/null 2>&1; then
        echo "$RESPONSE" | jq -r '.results[] | select(.transaction_type == "TRANSFER") | "Transferred $" + (.amount|tostring) + " to " + .recipient_name'
    fi
    if echo "$RESPONSE" | jq -e '.owed_to | length > 0' >/dev/null 2>&1; then
        echo "$RESPONSE" | jq -r '.owed_to[] | "Owed $" + (.amount|tostring) + " to " + .account_name'
    fi
    if echo "$RESPONSE" | jq -e '.owed_by | length > 0' >/dev/null 2>&1; then
        echo "$RESPONSE" | jq -r '.owed_by[] | "Owed $" + (.amount|tostring) + " by " + .account_name'
    fi
}


login() {
    USERNAME=$1
    read API_KEY APPLICATION_ID < "$SESSION_FILE"
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v1/login" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -d "{\"account_name\": \"$USERNAME\"}")
    ACCOUNT_ID=$(echo "$RESPONSE" | jq -r '.account.account_id // empty')
    ACCOUNT_NAME=$(echo "$RESPONSE" | jq -r '.account.name // empty')
    if [[ -n "$ACCOUNT_ID" && -n "$ACCOUNT_NAME" ]]; then
        echo "$API_KEY $APPLICATION_ID $ACCOUNT_ID $ACCOUNT_NAME" > "$LOGIN_SESSION_FILE"
        echo "Hello, $ACCOUNT_NAME!"
        parse_transaction_response "$RESPONSE"
        echo "Your balance is \$$(echo "$RESPONSE" | jq -r '.account.balance // 0')"
    else
        echo "Login failed or invalid response."
    fi
}

deposit() {
    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    AMOUNT=$1
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"DEPOSIT\"}")
    parse_transaction_response "$RESPONSE"
    echo "Your balance is \$$(echo "$RESPONSE" | jq -r '.account.balance // 0')"
}

withdraw() {
    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    AMOUNT=$1
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"WITHDRAW\"}")
    parse_transaction_response "$RESPONSE"
    echo "Your balance is \$$(echo "$RESPONSE" | jq -r '.account.balance // 0')"
}

transfer() {
    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    TARGET=$1
    AMOUNT=$2
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"TRANSFER\", \"recipient\": \"$TARGET\"}")
    parse_transaction_response "$RESPONSE"
    echo "Your balance is \$$(echo "$RESPONSE" | jq -r '.account.balance // 0')"
}

logout() {
    if [ -f "$LOGIN_SESSION_FILE" ]; then
        read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
        echo "Goodbye, $ACCOUNT_NAME!"
        rm -f "$LOGIN_SESSION_FILE"
    else
        echo "No active login session."
    fi
}

exit_script() {
    trap - EXIT  # Remove the EXIT trap to avoid duplicate messages
    STOP_RESPONSE=$(stop_session 2>&1)
    if [[ ! "$STOP_RESPONSE" =~ "No active Session found" ]]; then
        echo "Session cleared. Goodbye!"
    fi
    exit 0
}

trap exit_script EXIT

start_session

echo "Welcome to the ATM CLI! Type 'exit' to quit."
while true; do
    read -p "$ " CMD ARG1 ARG2
    case "$CMD" in
        login) login "$ARG1" ;;
        deposit) deposit "$ARG1" ;;
        withdraw) withdraw "$ARG1" ;;
        transfer) transfer "$ARG1" "$ARG2" ;;
        logout) logout ;;
        exit) exit_script ;;
        *) echo "Invalid command." ;;
    esac
done
