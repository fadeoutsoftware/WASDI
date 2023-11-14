FROM ubuntu:20.04

MAINTAINER WASDI <info@wasdi.cloud>


# Define argument used to create a user and a group in the container
# Values are provided with the docker build command
ARG USR_NAME
ARG USR_ID
ARG GRP_NAME
ARG GRP_ID
ARG USR_HOME="/home/wasdi"
ARG PIP_INSTALL_WASDI_ARGUMENTS


# Create environment variables:
#   - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#   - DEFAULT_HOME: used by supervisord
#   - PATH: to use binaries from our future virtual environment as default
#   - PYTHONDONTWRITEBYTECODE: to stop Python to write pyc files
#   - PYTHONUNBUFFERED: to force 'print' to flush message directly
ENV DEBIAN_FRONTEND="noninteractive"
ENV DEFAULT_HOME="$USR_HOME"
ENV VIRTUAL_ENV=$USR_HOME/venv
ENV PATH="$VIRTUAL_ENV/bin:$PATH"
ENV PYTHONDONTWRITEBYTECODE="False"
ENV PYTHONUNBUFFERED="True"


# Create a group
# Create a user
# Change rights on /home
# Update the repository cache
# Update the system
# Install needed packages
# Create directories for supervisor
# Change owner and group for supervisor directories
# Change right for supervisor configuration file
RUN groupadd --gid $GRP_ID --non-unique $USR_NAME && \
    useradd --create-home --home-dir $USR_HOME --uid $USR_ID --gid $GRP_ID --non-unique --shell /bin/bash $USR_NAME && \
    chmod 755 /home && \
    apt-get update && \
    apt-get upgrade --option Dpkg::Options::="--force-confold" --assume-yes --no-install-recommends && \
    apt-get install --option Dpkg::Options::="--force-confold" --assume-yes --no-install-recommends \
        build-essential \
        curl \
        git \
        gdal-bin \
        libffi-dev \
        libgdal-dev \
        libpq-dev \
        libssl-dev \
        libxml2-dev \
        libxslt1-dev \
        locate \
        nano \
        nodejs \
        openjdk-8-jre \
        python3 \
        python3-dev \
        python3-gdal \
        python3-pip \
        python3-venv \
        software-properties-common \
        supervisor \
        wget \
        zlib1g-dev && \
    mkdir -p /var/log/supervisor && \
    mkdir -p /var/run/supervisor && \
    chown $USR_NAME:$GRP_NAME /var/log/supervisor /var/run/supervisor


# Set the workdir to $USR_HOME
WORKDIR "$USR_HOME"


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Create a virtual environment
# [IN OUR VENV] Install needed Python libraries
# Please note:
#   - 'wheel' is installed first else we have a problem to build GDAL
#   - 'wasdi' is installed independently to be able to set the repository
#   - 'GDAL' is installed after because it is a compiled package which depends of wheel
#   - there is no cache: I suppose because it is too much dynamic
RUN python3 -m venv $VIRTUAL_ENV && \
    pip3 install --no-cache-dir wheel && \
    pip3 install --no-cache-dir \
        jupyterlab \
        numpy \
        pandas \
        rasterio \
        requests && \
    pip3 install --no-cache-dir $PIP_INSTALL_WASDI_ARGUMENTS wasdi && \
    pip3 install --no-cache-dir GDAL==`gdal-config --version`


# From now, all commands executed are executed as root
USER root


# Change rights in the working directory
# Clean caches to reduce the image size
RUN chmod -R 755 $USR_HOME/ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    find $USR_HOME -follow -type f -name '*.a' -delete && \
    find $USR_HOME -follow -type f -name '*.pyc' -delete && \
    find $USR_HOME -follow -type f -name '*.js.map' -delete


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Start our webserver
CMD ["/usr/bin/supervisord", "--configuration", "/etc/supervisor/supervisord.conf"]
