FROM alpine:latest

WORKDIR /app

RUN apk add --no-cache \
    jq \
    curl \
    bash

COPY start.sh /app/start.sh

RUN chmod +x /app/start.sh

ENTRYPOINT ["/app/start.sh"]
