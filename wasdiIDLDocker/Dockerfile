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
        curl \
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
        python3 \
        python3-dev \
        python3-gdal \
        python3-pip \
        python3-venv \
        software-properties-common \
        wget \
        zlib1g-dev


# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/


# Change rights
RUN chmod -R 755 $USR_HOME/


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Create a virtual environment
RUN python3 -m venv $VIRTUAL_ENV


# Unpack the tar in the $USR_HOME directory
RUN tar --directory $USR_HOME/ -xf $USR_HOME/envi552-linux.tar


# Overwrite install script with the one which is close to the Dockerfile
COPY --chown=$USR_NAME:$GRP_NAME ./install.sh $USR_HOME/


# From now, all commands executed are executed as root
USER root


# Make it executable and run the installation
RUN bash $USR_HOME/install.sh s


# Set the license server url
COPY ./o_licenseserverurl.txt /usr/local/harris/license/o_licenseserverurl.txt


# Clean envi installation files
CMD rm -rf ./install \
    ./silent \
    $USR_HOME/envi552-linux.tar \
    $USR_HOME/o_licenseserverurl.txt \
    $USR_HOME/install.sh


# Make the folder for idl and let others write
RUN mkdir /.idl && \
     chmod 777 /.idl


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Override the environment PATH variable to be able to use binaries provided by our virtual environment
# From here, all pip3 commands are executed in our virtual environment because of this setting
ENV PATH="$VIRTUAL_ENV/bin:$PATH"


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


# [IN OUR VENV] Install needed Python GDAL library
RUN export CPLUS_INCLUDE_PATH=/usr/include/gdal && \
    export C_INCLUDE_PATH=/usr/include/gdal && \
    pip3 install --global-option=build_ext --global-option="-I/usr/include/gdal" GDAL==`gdal-config --version`


# Start our webserver
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
