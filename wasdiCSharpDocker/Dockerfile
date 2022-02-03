FROM ubuntu:20.04

MAINTAINER WASDI <info@wasdi.cloud>


# Define argument used to create a user and a group in the container
# Values are provided with the docker build command
ARG USR_NAME
ARG USR_ID
ARG GRP_NAME
ARG GRP_ID
ARG USR_HOME="/home/wasdi"
ARG PIP_INSTALL_WASDI_ARGUMENTS=""


# Create environment variables:
#   - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#   - VIRTUAL_ENV: to define the path to the venv we will use
ENV DEBIAN_FRONTEND="noninteractive"
ENV VIRTUAL_ENV="$USR_HOME/venv"


# Create a user, create a group and change rights on /home
RUN groupadd --gid $GRP_ID --non-unique $USR_NAME && \
    useradd --create-home --home-dir $USR_HOME --uid $USR_ID --gid $GRP_ID --non-unique --shell /bin/bash $USR_NAME && \
    chmod 755 /home


# Set the workdir to $USR_HOME
WORKDIR "$USR_HOME"


# Update the system and install packages
RUN apt-get update && \
    apt-get upgrade --assume-yes --no-install-recommends && \
    apt-get install --assume-yes --no-install-recommends \
        apt-transport-https \
        ca-certificates \
        gpg \
        python3 \
        python3-dev \
        python3-gdal \
        python3-pip \
        python3-venv \
        wget && \
    wget --quiet --output-document=/tmp/microsoft.asc http://packages.microsoft.com/keys/microsoft.asc && \
    gpg --dearmor --output /etc/apt/trusted.gpg.d/microsoft.asc.gpg /tmp/microsoft.asc && \
    chown root:root /etc/apt/trusted.gpg.d/microsoft.asc.gpg && \
    chmod 644 /etc/apt/trusted.gpg.d/microsoft.asc.gpg && \
    wget --quiet --output-document=/etc/apt/sources.list.d/microsoft-prod.list http://packages.microsoft.com/config/ubuntu/20.04/prod.list && \
    apt-get update && \
    apt-get install --assume-yes --no-install-recommends aspnetcore-runtime-6.0


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Create a virtual environment
RUN python3 -m venv $VIRTUAL_ENV


# Override the environment PATH variable to be able to use binaries provided by our virtual environment
# From here, all pip3 commands are executed in our virtual environment because of this setting
ENV PATH="$VIRTUAL_ENV/bin:$PATH"


# Create a directory to host the application
RUN mkdir app && \
    chmod 755 app && \
    chown $USR_NAME:$GRP_NAME app


# [IN OUR VENV] Install needed Python libraries
RUN pip3 install flask \
    gunicorn \
    requests \
    numpy \
    pandas \
    rasterio \
    wheel


# [IN OUR VENV] Install the wasdi library: can come from TEST according to the value of $PIP_INSTALL_WASDI_ARGUMENTS
RUN pip3 install $PIP_INSTALL_WASDI_ARGUMENTS wasdi


# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/app/.


# Remove files which are not necessary
RUN rm -f $USR_HOME/app/Dockerfile


# From now, all commands executed are executed as root
USER root


# Change rights
RUN find $USR_HOME/app -type f -exec chmod 644 {} \; && \
    find $USR_HOME/app -type d -exec chmod 755 {} \;


# Install user's packages:
#   - only if $USR_HOME/packages.txt exists
#   - we have to become root using USER instruction
RUN test -f $USR_HOME/packages.txt && apt-get install -y `cat $USR_HOME/packages.txt | tr "\n" " "`; exit 0


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Install user's Python libraries:
#   - only if $USR_HOME/pip.txt exists
RUN test -f $USR_HOME/pip.txt && pip3 install `cat $USR_HOME/pip.txt | tr "\n" " "`; exit 0


# Start our webserver
WORKDIR "$USR_HOME/app"
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
