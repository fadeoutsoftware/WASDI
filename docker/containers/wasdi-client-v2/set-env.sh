#!/bin/sh
# set-env.sh
# This script is executed inside the Docker container at startup.
# It generates the /var/www/html/assets/assets/environment.js file
# based on Docker environment variables prefixed with WASDI_.

# Define the path where your Angular app expects the environment.js file
# This typically matches the path in your index.html: <script src="/assets/environment.js"></script>
OUTPUT_FILE="/var/www/html/assets/environment.js"

echo "Generating runtime environment file: ${OUTPUT_FILE}"

# Start with the basic structure for window.__env
echo "window.__env = window.__env || {};" > "$OUTPUT_FILE"

# Set default value for the env variables if not defined
if [ -z "$WASDI_BASE_URL" ]; then
  WASDI_BASE_URL="https://www.wasdi.net"
  echo "  - WASDI_BASE_URL not set, using default: $WASDI_BASE_URL"
fi

if [ -z "$WASDI_WEBSTOMP_URL" ]; then
  WASDI_WEBSTOMP_URL="wss://main01.wasdi.net"
  echo "  - WASDI_WEBSTOMP_URL not set, using default: $WASDI_WEBSTOMP_URL"
fi

if [ -z "$WASDI_CESIUM_TOKEN" ]; then
  WASDI_CESIUM_TOKEN=""
  echo "  - WASDI_CESIUM_TOKEN not set, using default"
fi

if [ -z "$RABBIT_USER" ]; then
  RABBIT_USER="wasdi"
  echo "  - RABBIT_USER not set, using default: $RABBIT_USER"
fi

if [ -z "$RABBIT_PASSWORD" ]; then
  RABBIT_PASSWORD=""
  echo "  - RABBIT_USER not set, using default"
fi



# Set the specific URL using WASDI_BASE_URL environment variable
echo "window.__env.url = '${WASDI_BASE_URL}/wasdiwebserver/';" >> "$OUTPUT_FILE"
echo "window.__env.webstompUrl = '${WASDI_WEBSTOMP_URL}/rabbit-stomp/ws';" >> "$OUTPUT_FILE"
echo "window.__env.wmsUrl = '${WASDI_BASE_URL}/geoserver/ows?'" >> "$OUTPUT_FILE"
echo "window.__env.authUrl = '${WASDI_BASE_URL}/auth/realms/wasdi'" >> "$OUTPUT_FILE"
echo "window.__env.keycloakJs = '${WASDI_BASE_URL}/auth/js/keycloak.js'" >> "$OUTPUT_FILE"
echo "window.__env.baseurl = '${WASDI_BASE_URL}/'" >> "$OUTPUT_FILE"
echo "window.__env.RABBIT_USER = '${RABBIT_USER}'" >> "$OUTPUT_FILE"
echo "window.__env.RABBIT_PASSWORD = '${RABBIT_PASSWORD}'" >> "$OUTPUT_FILE"
echo "window.__env.CESIUM_BASE_URL = '${WASDI_BASE_URL}/assets/cesium/'" >> "$OUTPUT_FILE"
echo "window.__env.cesiumToken = '${WASDI_CESIUM_TOKEN}'" >> "$OUTPUT_FILE"

echo "window.__env.production = true;" >> "$OUTPUT_FILE"


echo "Finished generating environment file."
echo "--- Content of ${OUTPUT_FILE} ---"
cat "$OUTPUT_FILE"
echo "-----------------------------------"

# Execute the command passed to the script (e.g., 'nginx -g "daemon off;"')
# This is essential to ensure the main container process runs.
exec "$@"