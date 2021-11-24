FROM ubuntu:18.04

MAINTAINER WASDI <info@wasdi.cloud>


# Define argument used to create a user and a group in the container
# Values are provided with the docker build command
ARG USR_NAME
ARG USR_ID
ARG GRP_NAME
ARG GRP_ID
ARG USR_HOME="/home/wasdi"


# Create environment variables:
#   - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#   - VIRTUAL_ENV: to define the path to the venv we will use
ENV DEBIAN_FRONTEND="noninteractive"
ENV VIRTUAL_ENV=$USR_HOME/venv


# Create a user, create a group and change rights on /home
RUN groupadd --gid $GRP_ID --non-unique $USR_NAME && \
    useradd --create-home --home-dir $USR_HOME --uid $USR_ID --gid $GRP_ID --non-unique --shell /bin/bash $USR_NAME && \
    chmod 755 /home


# Set the workdir to $USR_HOME
WORKDIR "$USR_HOME"


# Update the system and install packages
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y \
        build-essential \
        gdal-bin \
        libffi-dev \
        libgdal-dev \
        libpq-dev \
        libssl-dev \
        libxml2-dev \
        libxslt1-dev \
        locate \
        nano \
        openjdk-8-jre \
        python \
        python-dev \
        python-gdal \
        python-pip \
        software-properties-common \
        virtualenv \
        wget \
        zlib1g-dev


# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/


# Change rights
RUN chmod 644 $USR_HOME/*


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Create a virtual environment
RUN virtualenv --python=python2 $VIRTUAL_ENV


# Override the environment PATH variable to be able to use binaries provided by our virtual environment
# From here, all pip2 commands are executed in our virtual environment because of this setting
ENV PATH="$VIRTUAL_ENV/bin:$PATH"


# [IN OUR VENV] Install needed Python libraries
RUN pip2 install flask \
    gunicorn \
    requests \
    numpy \
    pandas \
    rasterio \
    wasdi \
    wheel


# [IN OUR VENV] Install needed Python GDAL library
RUN export CPLUS_INCLUDE_PATH=/usr/include/gdal && \
    export C_INCLUDE_PATH=/usr/include/gdal
RUN pip2 install --global-option=build_ext --global-option="-I/usr/include/gdal" GDAL==`gdal-config --version`


# From now, all commands executed are executed as root
USER root


# Install user's packages:
#   - only if $USR_HOME/packages.txt exists
#   - we have to become root using USER instruction
RUN test -f $USR_HOME/packages.txt && apt-get install -y `cat $USR_HOME/packages.txt | tr "\n" " "`; exit 0


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Install user's Python libraries:
#   - only if $USR_HOME/pip.txt exists
RUN test -f $USR_HOME/pip.txt && pip2 install `cat $USR_HOME/pip.txt | tr "\n" " "`; exit 0


# Start our webserver
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
