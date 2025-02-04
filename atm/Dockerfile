FROM alpine:latest

WORKDIR /app

RUN apk add --no-cache \
    jq \
    curl \
    bash

COPY start.sh /app/start.sh

RUN chmod +x /app/start.sh

# Default to a shell instead of running the script
CMD ["sh"]
