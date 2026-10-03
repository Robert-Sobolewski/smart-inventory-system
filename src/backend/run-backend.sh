#!/usr/bin/env bash
# run-backend.sh - uruchamianie i restart Spring Boot backendu
# Uzytkowanie:
#   ./run-backend.sh start    # normalny start (mvn spring-boot:run)
#   ./run-backend.sh stop     # zabija proces Java z portem 8888
#   ./run-backend.sh restart  # stop + start

set -e
cd "$(dirname "$0")"

PORT=8888

find_pid() {
    netstat -ano | grep ":$PORT " | grep LISTENING | awk '{print $5}' | head -1
}

stop_backend() {
    local pid=$(find_pid)
    if [ -n "$pid" ]; then
        echo "Znajdzione PID: $pid -> zabijam..."
        # taskkill to natywny program Windows -> pojedyncze slaski (//F nie dziala w MSYS)
        taskkill /F /PID "$pid"
        sleep 2
    else
        echo "Brak procesu na porcie $PORT (nic do robienia)."
    fi
}

# mvn (bash script) zawodzi pod MSYS (path conversion zlamuje glob boot/plexus-classworlds-*.jar),
# wiec uzywamy wersji batch, jesli jest dostepna.
if command -v mvn.cmd >/dev/null 2>&1; then
    MVN=mvn.cmd
else
    MVN=mvn
fi

start_backend() {
    stop_backend
    echo "Startuje backend ($MVN spring-boot:run)..."
    $MVN spring-boot:run
}

case "$1" in
    start)   start_backend ;;
    stop)    stop_backend ;;
    restart) start_backend ;;
    *)       echo "Uzytkowanie: $0 {start|stop|restart}" ; exit 1 ;;
esac
