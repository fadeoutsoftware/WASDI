FROM ubuntu:20.04


MAINTAINER WASDI <info@wasdi.cloud>


# Define arguments
# Values not setted here are provided with the docker build command
ARG GRP_ID
ARG GRP_NAME
ARG PIP_INSTALL_WASDI_ARGUMENTS=""
ARG USR_ID
ARG USR_HOME="/home/wasdi"
ARG USR_NAME
ARG MINICONDA_SRC_URL="https://repo.anaconda.com/miniconda/Miniconda3-py38_4.12.0-Linux-x86_64.sh"
ARG MINICONDA_TEMPORARY_SCRIPT="Miniconda3.sh"


# Create environment variables:
#   - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#   - DEFAULT_HOME: used by supervisord
#   - PATH: to use binaries from our future virtual environment as default
#   - PYTHONDONTWRITEBYTECODE: to stop Python to write pyc files
#   - PYTHONUNBUFFERED: to force Python to unbuffer the stdin and stdout
ENV DEBIAN_FRONTEND="noninteractive"
ENV DEFAULT_HOME="$USR_HOME"
ENV PATH="$USR_HOME/miniconda/bin:$PATH"
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
        ca-certificates \
        supervisor \
        wget && \
    mkdir -p /var/log/supervisor && \
    mkdir -p /var/run/supervisor && \
    chown $USR_NAME:$GRP_NAME /var/log/supervisor /var/run/supervisor && \
    chmod 644 /etc/supervisor/supervisord.conf


# Install the supervisor configuration file
COPY --chown=root:root etc_supervisor_supervisord.conf /etc/supervisor/supervisord.conf


# Set the workdir to $USR_HOME
WORKDIR "$USR_HOME"


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# Download miniconda
# Install miniconda
# Update the conda environment
RUN wget --quiet --output-document=$USR_HOME/$MINICONDA_TEMPORARY_SCRIPT $MINICONDA_SRC_URL && \
    bash $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT -b -p $USR_HOME/miniconda && \
    rm -f $USR_HOME/$MINICONDA_TEMPORARY_SCRIPT && \
    conda update --yes --all


# Copy our files
COPY --chown=$USR_NAME:$GRP_NAME ./ $USR_HOME/


# From now, all commands executed are executed as root
USER root


# Install user's packages:
#   - only if $USR_HOME/packages.txt exists
#   - we have to become root using USER instruction
## DISABLE FOR THE MOMENT ##
# RUN bash $USR_HOME/tool/installUserPackage.sh --home-directory $USR_HOME --package-apt
## /DISABLE FOR THE MOMENT ##


# From now, all commands executed are executed with the user $USR_NAME who must not be root
USER $USR_NAME


# [IN OUR CONDA ENVIRONMENT] Install user's Conda libraries:
#   - only if $USR_HOME/env.yml exists
## DISABLE FOR THE MOMENT ##
# RUN bash $USR_HOME/tool/installUserPackage.sh --home-directory $USR_HOME --package-conda
## /DISABLE FOR THE MOMENT ##


# [IN OUR CONDA ENVIRONMENT] Install Python libraries
RUN pip3 install flask \
        gunicorn \
        requests && \
    pip3 install $PIP_INSTALL_WASDI_ARGUMENTS wasdi


# From now, all commands executed are executed as root
USER root


# Change rights in the working directory
# Clean caches to reduce the image size
# Install user package
RUN chmod -R 755 $USR_HOME/ && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    find $USR_HOME -follow -type f -name '*.a' -delete && \
    find $USR_HOME -follow -type f -name '*.pyc' -delete && \
    find $USR_HOME -follow -type f -name '*.js.map' -delete && \
    rm -f etc_supervisor_supervisord.conf && \
    conda clean --all --force-pkgs-dirs --yes


# Start our webserver
CMD ["/usr/bin/supervisord", "--configuration", "/etc/supervisor/supervisord.conf"]
