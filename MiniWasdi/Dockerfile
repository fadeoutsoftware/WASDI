# --- Stage 1: Build/Setup ---
FROM ubuntu:24.04

USER root

# Avoid prompts
ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="1"
# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="1"
# This variable is setted to set the default editor
ENV EDITOR="nano"
# Default Path of the configuration file
ENV WASDI_CONFIG_FILE="/data/wasdi/wasdiConfig.json"
ENV WASDI_RUN_APPLICATION=""
ENV WASDI_PARAMS=""
ENV WASDI_WORKSPACE=""
ENV WASDI_WORKSPACE_ID=""

# RUN generic to create user, group, directories, etc
RUN groupadd --gid 2042 --non-unique appwasdi && \
    useradd --create-home --home-dir /home/appwasdi --uid 2042 --gid 2042 --non-unique --shell /bin/bash appwasdi && \
    mkdir --parents \
        /home/appwasdi/.ssh \
        /opt/wasdi && \
    chown appwasdi:appwasdi \
        /home/appwasdi/.ssh && \
    chown appwasdi:appwasdi \
        /opt/wasdi && \
    chmod 700 \
        /home/appwasdi/.ssh && \
    chmod 755 \
        /opt/wasdi

# Install only runtime dependencies (no build/dev packages)
RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates \
    openjdk-21-jre-headless \
    gdal-bin \
    libgdal-dev \
    python3-full \
    python3-pip \
    python3-dev \
    g++ \
    curl \
    && rm -rf /var/lib/apt/lists/* /var/cache/apt/* /tmp/* /var/tmp/*

# Install Python WASDI library used by miniWasdi.py
RUN python3 -m pip install --no-cache-dir wasdi --break-system-packages
    

# Install 'uv' for high-speed venv management
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

# Create runtime directories as root, then hand over ownership to appwasdi
RUN mkdir -p \
    /data/wasdi/install/processors \
    /data/wasdi/install/workflows \
    /data/wasdi/processors \
    /data/wasdi/params \
    /data/wasdi/metadata \
    /data/wasdi/tmp \
    /data/wasdi/appwasdi.home/.snap \
    /data/wasdi/appwasdi.home/.snap/etc \
    /opt/wasdi/launcher \
    /opt/wasdi/scheduler \
    /opt/wasdi/dockertemplate/local_python312 \
    /var/log/wasdi \
    /etc/wasdi && \
    chown -R appwasdi:appwasdi \
    /data/wasdi \
    /opt/wasdi \
    /var/log/wasdi \
    /etc/wasdi

USER appwasdi

#RUN uv python install 3.12

# Prepare WASDI Directories
WORKDIR /opt/wasdi

# Copy your artifacts
# Assuming webserver.war is your webserver build
COPY --chown=appwasdi:appwasdi ./dataToCopy/wasdi-mini-server.jar /opt/wasdi/wasdi-mini-server.jar
COPY --chown=appwasdi:appwasdi ./dataToCopy/launcher-1.0.jar /opt/wasdi/launcher/launcher.jar
COPY --chown=appwasdi:appwasdi ./dataToCopy/scheduler-1.0.jar /opt/wasdi/scheduler/scheduler.jar
COPY --chown=appwasdi:appwasdi ./dataToCopy/wasdi.sh /opt/wasdi/wasdi.sh
COPY --chown=appwasdi:appwasdi ./dataToCopy/miniWasdi.py /opt/wasdi/miniWasdi.py
COPY --chown=appwasdi:appwasdi ./dataToCopy/processors/local_python312/* /opt/wasdi/dockertemplate/local_python312/
COPY --chown=appwasdi:appwasdi ./dataToCopy/config/* /etc/wasdi/

# Copy JNI libs
COPY --chown=appwasdi:appwasdi ./dataToCopy/linux-x86-64/* /opt/wasdi/launcher/linux-x86-64/
ENV LD_LIBRARY_PATH="/opt/wasdi/launcher/linux-x86-64/"


# Entrypoint Script
USER root
RUN chmod +x /opt/wasdi/wasdi.sh
USER appwasdi

#EXPOSE 8080
ENTRYPOINT ["/opt/wasdi/wasdi.sh"]