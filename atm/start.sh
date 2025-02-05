#!/bin/bash

API_BASE_URL="http://wallet-service:8080"
SESSION_FILE="session.txt"
LOGIN_SESSION_FILE="session_login.txt"
SECRET_KEY="my-secret-key"
REFERENCE_ID=$(date +%s)
ACCOUNT_ID=""
ACCOUNT_NAME=""

start_session() {
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v1/session" -H "Content-Type: application/json" -H "secret_key: $SECRET_KEY" -w "%{http_code}" -o response.json)
    HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
    RESPONSE_BODY=$(<response.json)

    if [[ "$HTTP_STATUS" -ge 400 ]]; then
        ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
        echo "Error: HTTP $HTTP_STATUS - $ERROR_MESSAGE"
        echo "Response file saved: response.json"
        return 1
    fi

    API_KEY=$(echo "$RESPONSE_BODY" | jq -r '.api_key')
    APPLICATION_ID=$(echo "$RESPONSE_BODY" | jq -r '.application_id')
    echo "$API_KEY $APPLICATION_ID" > "$SESSION_FILE"
}

stop_session() {
    if [ -f "$SESSION_FILE" ]; then
        read API_KEY APPLICATION_ID < "$SESSION_FILE"
        RESPONSE=$(curl -s -X PUT "$API_BASE_URL/v1/session/deactivate" -H "Content-Type: application/json" -H "secret_key: $SECRET_KEY" -d "{\"application_id\": \"$APPLICATION_ID\", \"api_key\": \"$API_KEY\"}" -w "%{http_code}" -o response.json)
        HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
        RESPONSE_BODY=$(<response.json)

        if [[ "$HTTP_STATUS" -ge 400 ]]; then
            ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
            echo "Error: HTTP $HTTP_STATUS - $ERROR_MESSAGE"
            echo "Response file saved: response.json"
            return 1
        fi
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
    if [ -f "$LOGIN_SESSION_FILE" ]; then
        echo "A session is already active. Please log out first before logging in again."
        return 1
    fi

    if [ -z "$1" ]; then
        echo "Error: Username is required for login."
        return 1
    fi

    USERNAME=$1
    read API_KEY APPLICATION_ID < "$SESSION_FILE"
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v1/login" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -d "{\"account_name\": \"$USERNAME\"}" -w "%{http_code}" -o response.json)
    HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
    RESPONSE_BODY=$(<response.json)

    if [[ "$HTTP_STATUS" -ge 400 ]]; then
        ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
        echo "$ERROR_MESSAGE"
        return 1
    fi

    ACCOUNT_ID=$(echo "$RESPONSE_BODY" | jq -r '.account.account_id // empty')
    ACCOUNT_NAME=$(echo "$RESPONSE_BODY" | jq -r '.account.name // empty')
    if [[ -n "$ACCOUNT_ID" && -n "$ACCOUNT_NAME" ]]; then
        echo "$API_KEY $APPLICATION_ID $ACCOUNT_ID $ACCOUNT_NAME" > "$LOGIN_SESSION_FILE"
        echo "Hello, $ACCOUNT_NAME!"
        parse_transaction_response "$RESPONSE_BODY"
        echo "Your balance is \$$(echo "$RESPONSE_BODY" | jq -r '.account.balance // 0')"
    else
        echo "Login failed or invalid response."
    fi
}

validate_amount() {
    if ! [[ "$1" =~ ^[0-9]+(\.[0-9]+)?$ ]]; then
        echo "Error: Amount must be a valid number."
        return 1
    fi
}

deposit() {
    if [ -z "$1" ]; then
        echo "Error: Amount is required for deposit."
        return 1
    fi
    validate_amount "$1" || return 1

    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    AMOUNT=$1
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"DEPOSIT\"}" -w "%{http_code}" -o response.json)
    HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
    RESPONSE_BODY=$(<response.json)

    if [[ "$HTTP_STATUS" -ge 400 ]]; then
        ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
        echo "$ERROR_MESSAGE"
        return 1
    fi

    parse_transaction_response "$RESPONSE_BODY"
    echo "Your balance is \$$(echo "$RESPONSE_BODY" | jq -r '.account.balance // 0')"
}

withdraw() {
    if [ -z "$1" ]; then
        echo "Error: Amount is required for withdrawal."
        return 1
    fi
    validate_amount "$1" || return 1

    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    AMOUNT=$1
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"WITHDRAW\"}" -w "%{http_code}" -o response.json)
    HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
    RESPONSE_BODY=$(<response.json)

    if [[ "$HTTP_STATUS" -ge 400 ]]; then
        ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
        echo "$ERROR_MESSAGE"
        return 1
    fi

    parse_transaction_response "$RESPONSE_BODY"
    echo "Your balance is \$$(echo "$RESPONSE_BODY" | jq -r '.account.balance // 0')"
}

transfer() {
    if [ -z "$1" ]; then
        echo "Error: Target account is required for transfer."
        return 1
    fi
    if [ -z "$2" ]; then
        echo "Error: Amount is required for transfer."
        return 1
    fi
    validate_amount "$2" || return 1

    read API_KEY APPLICATION_ID ACCOUNT_ID ACCOUNT_NAME < "$LOGIN_SESSION_FILE"
    TARGET=$1
    AMOUNT=$2
    RESPONSE=$(curl -s -X POST "$API_BASE_URL/v2/transactions" -H "Content-Type: application/json" -H "application-id: $APPLICATION_ID" -H "api-key: $API_KEY" -H "account-id: $ACCOUNT_ID" -d "{\"reference_id\": \"$REFERENCE_ID\", \"amount\": $AMOUNT, \"transaction_type\": \"TRANSFER\", \"recipient\": \"$TARGET\"}" -w "%{http_code}" -o response.json)
    HTTP_STATUS=$(tail -n1 <<< "$RESPONSE")
    RESPONSE_BODY=$(<response.json)

    if [[ "$HTTP_STATUS" -ge 400 ]]; then
        ERROR_MESSAGE=$(echo "$RESPONSE_BODY" | jq -r '.message')
        echo "$ERROR_MESSAGE"
        return 1
    fi

    parse_transaction_response "$RESPONSE_BODY"
    echo "Your balance is \$$(echo "$RESPONSE_BODY" | jq -r '.account.balance // 0')"
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
    if [ -f "$LOGIN_SESSION_FILE" ]; then
        echo "Please log out first before exit."
        return 1
    fi
    rm -f "$SESSION_FILE" "$LOGIN_SESSION_FILE"  # Remove session files
    trap - EXIT  # Remove the EXIT trap to avoid duplicate messages
    STOP_RESPONSE=$(stop_session 2>&1)
    if [[ ! "$STOP_RESPONSE" =~ "No active Session found" ]]; then
        echo "Session cleared!"
    fi
    echo "Goodbye!"
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
