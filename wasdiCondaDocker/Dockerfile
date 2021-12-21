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
ARG MINICONDA_SRC_URL="https://repo.anaconda.com/miniconda/Miniconda3-py38_4.10.3-Linux-x86_64.sh"
ARG MINICONDA_TEMPORARY_SCRIPT="Miniconda3.sh"


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
        ca-certificates \
        curl \
        git \
        nano \
        wget

# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Install miniconda
RUN wget --quiet --output-document=$USR_HOME/$MINICONDA_TEMPORARY_SCRIPT $MINICONDA_SRC_URL && \
    bash $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT -b -p $USR_HOME/miniconda && \
    rm -f $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT


# Override the environment PATH variable to be able to use binaries provided by our conda environment
# From here, all pip3 commands are executed in our conda environment because of this setting
ENV PATH="$USR_HOME/miniconda/bin:$PATH"


# [IN OUR CONDA ENVIRONMENT] Update conda + install packages using conda
RUN conda config --set channel_priority strict && \
    conda update --yes --all && \
    conda install --yes --channel conda-forge gdal && \
    pip3 install flask \
      gunicorn \
      requests && \
    pip3 install $PIP_INSTALL_WASDI_ARGUMENTS wasdi

# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/


# From now, all commands executed are executed as root
USER root


# Install user's packages:
#   - only if $USR_HOME/packages.txt exists
#   - we have to become root using USER instruction
RUN test -f $USR_HOME/packages.txt && apt-get install --assume-yes --no-install-recommends `cat $USR_HOME/packages.txt | tr "\n" " "`; exit 0


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# [IN OUR CONDA ENVIRONMENT] Install user's Conda libraries:
#   - only if $USR_HOME/env.yml exists
RUN test -f $USR_HOME/env.yml && conda env update --file $USR_HOME/env.yml; exit 0


# From now, all commands executed are executed as root
USER root


# Clean cache to reduce the image size
RUN rm -rf /var/lib/apt/lists/* && \
    conda clean --all --force-pkgs-dirs --yes && \
    find $USR_HOME -follow -type f -name '*.a' -delete && \
    find $USR_HOME -follow -type f -name '*.pyc' -delete && \
    find $USR_HOME -follow -type f -name '*.js.map' -delete


# Start our webserver
RUN chmod 777 ./runServer.sh
CMD ./runServer.sh
