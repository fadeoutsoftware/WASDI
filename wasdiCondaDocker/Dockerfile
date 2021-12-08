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
ARG MINICONDA_SRC_URL="https://repo.anaconda.com/miniconda/Miniconda3-py38_4.10.3-Linux-x86_64.sh"
ARG MINICONDA_TEMPORARY_SCRIPT="Miniconda3.sh"


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
        curl \
        locate \
        nano \
        openjdk-8-jre \
        python3 \
        python3-dev \
        python3-pip \
        python3-venv \
        software-properties-common \
        wget


# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/


# Change rights
RUN chmod -R 755 $USR_HOME/


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Create a virtual environment
RUN python3 -m venv $VIRTUAL_ENV


# Install miniconda
RUN wget -O $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT $MINICONDA_SRC_URL && \
    bash $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT -b -p $USR_HOME/miniconda && \
    rm -f $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT


# Override the environment PATH variable to be able to use binaries provided by our virtual environment
# and by miniconda
# From here, all pip3 commands are executed in our virtual environment because of this setting
ENV PATH="$VIRTUAL_ENV/bin:$USR_HOME/miniconda/bin:$PATH"


# Update conda
RUN conda update --yes --all


# Install package using conda
RUN conda install --yes --channel conda-forge \
    gdal \
    opencv


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


# From now, all commands executed are executed as root
USER root


# Install user's packages:
#   - only if $USR_HOME/packages.txt exists
#   - we have to become root using USER instruction
RUN test -f $USR_HOME/packages.txt && apt-get install -y `cat $USR_HOME/packages.txt | tr "\n" " "`; exit 0


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Install user's Conda libraries:
#   - only if $USR_HOME/env.yml exists
RUN test -f $USR_HOME/env.yml && $USR_HOME/miniconda/bin/conda env update --file $USR_HOME/env.yml; exit 0


# Start our webserver
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
