#!/bin/bash
set -euo pipefail

SCHEDULER_PID=""
TOMCAT_PID=""
PYTHON_EXIT_CODE=0

export CPLUS_INCLUDE_PATH=/usr/include/gdal
export C_INCLUDE_PATH=/usr/include/gdal

cleanup() {
	echo "Stopping WASDI..."

	if [ -n "${SCHEDULER_PID}" ] && kill -0 "${SCHEDULER_PID}" 2>/dev/null; then
		kill "${SCHEDULER_PID}" 2>/dev/null || true
	fi

	if [ -n "${SERVER_PID}" ] && kill -0 "${SERVER_PID}" 2>/dev/null; then
		kill "${SERVER_PID}" 2>/dev/null || true
	fi

	wait || true

	if [ "${PYTHON_EXIT_CODE}" -ne 0 ]; then
		echo "MiniWasdi failed with exit code ${PYTHON_EXIT_CODE}."
	fi
}

trap cleanup EXIT TERM INT

SCHEDULER_JAR="/opt/wasdi/scheduler/scheduler.jar"
if [ ! -f "${SCHEDULER_JAR}" ]; then
	SCHEDULER_JAR=$(ls /opt/wasdi/scheduler/*.jar 2>/dev/null | head -n 1 || true)
fi

# 2. Start the Scheduler (Background)
echo "Starting WASDI..."
if [ -n "${SCHEDULER_JAR}" ] && [ -f "${SCHEDULER_JAR}" ]; then
	java -jar "${SCHEDULER_JAR}" --config "$WASDI_CONFIG_FILE" &
	SCHEDULER_PID=$!
else
	echo "WARNING: Scheduler jar not found in /opt/wasdi/scheduler."
fi


java --add-opens java.base/java.lang=ALL-UNNAMED \
     -jar /opt/wasdi/wasdi-mini-server.jar  &
SERVER_PID=$!

# 4. Start MiniWasdi Python application (Foreground)
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
set +e
python3 "$SCRIPT_DIR/miniWasdi.py"
PYTHON_EXIT_CODE=$?
set -e

exit $PYTHON_EXIT_CODE
