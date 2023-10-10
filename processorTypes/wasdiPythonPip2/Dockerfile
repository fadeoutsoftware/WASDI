FROM ubuntu:22.04


MAINTAINER "WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# Define arguments
# Values not setted here are provided with the docker build command
# Retro-compatibility
ARG USR_NAME
ARG USR_ID
ARG GRP_NAME
ARG GRP_ID
ARG USR_HOME="/home/wasdi"
ARG PIP_INSTALL_WASDI_ARGUMENTS
# New naming convention
ARG sContainerGroupId="${GRP_ID}"
ARG sContainerGroupName="${GRP_NAME}"
ARG sContainerPipWasdiArguments="${PIP_INSTALL_WASDI_ARGUMENTS}"
ARG sContainerUserId="${USR_ID}"
ARG sContainerUserName="${USR_NAME}"
ARG sContainerUserHomeDirectory="${USR_HOME}"
ARG sUbuntuDistributionName="jammy"

# Create environment variables:
#   - SYSTEM (name are imposed):
#     - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#     - PATH: to use binaries from our future virtual environment as default
#     - PYTHONDONTWRITEBYTECODE: to stop Python to write pyc files
#     - PYTHONUNBUFFERED: to force 'print' to flush message directly
ENV DEBIAN_FRONTEND="noninteractive"
ENV PATH="${sContainerUserHomeDirectory}/venv/bin:$PATH"
ENV PYTHONDONTWRITEBYTECODE="False"
ENV PYTHONUNBUFFERED="True"


# Many actions:
#   - Configure the APT command
#   - Update the repository cache
#   - Update the system
#   - Install needed packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install ca-certificates && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt ${sUbuntuDistributionName} main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt ${sUbuntuDistributionName}-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt ${sUbuntuDistributionName}-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get upgrade && \
    apt-get install \
        gdal-bin \
        git \
        gunicorn \
        nano \
        python3 \
        python3-flask \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv \
        python3-wheel \
        sudo && \
    rm -rf /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm -rf {} +


# Many actions:
#   - create the internal group
#   - create the internal user
#   - create all directories
#   - create a sudo configuration to become root with ${sContainerUserName}
RUN groupadd --gid ${sContainerGroupId} --non-unique ${sContainerGroupName} && \
    useradd --create-home --home-dir ${sContainerUserHomeDirectory} --uid ${sContainerUserId} --gid ${sContainerGroupId} --non-unique --shell /bin/bash ${sContainerUserName} && \
    mkdir -p ${sContainerUserHomeDirectory}/application && \
    chown -R ${sContainerUserName}:${sContainerGroupName} ${sContainerUserHomeDirectory}/ && \
    chmod -R 755 ${sContainerUserHomeDirectory}/ && \
    echo -e "${sContainerUserName} ALL=(ALL:ALL) NOPASSWD:/usr/bin/apt\n${sContainerUserName} ALL=(ALL:ALL) NOPASSWD:/usr/bin/apt-get" > /etc/sudoers.d/01_wasdi && \
    chmod 644 /etc/sudoers.d/01_wasdi


# Set the workdir to ${sContainerUserHomeDirectory}
WORKDIR ${sContainerUserHomeDirectory}/application


# From now, all commands executed are executed with the user ${sContainerUserName} who must not be root
USER ${sContainerUserName}


# Copy:
#   - the tool to install package
#   - the application files
COPY --chown=${sContainerUserName}:${sContainerGroupName} ./ ${sContainerUserHomeDirectory}/application/


# Create a virtual environment
# [IN OUR VENV] Install needed Python libraries
# Please note 'wasdi' is installed independently to be able to set the repository
RUN python3 -m venv --system-site-packages ${sContainerUserHomeDirectory}/venv && \
    pip3 install --no-cache-dir --no-compile ${sContainerPipWasdiArguments} wasdi && \
    pip3 install --no-cache-dir --no-compile \
        numpy


# Install EO expert libraries only if ${sContainerUserHomeDirectory}/application/pip.txt exists
RUN bash ${sContainerUserHomeDirectory}/application/installUserPackage.sh --home-directory ${sContainerUserHomeDirectory}/application --package-pip --venv-directory ${sContainerUserHomeDirectory}/venv


# From now, all commands executed are executed with the user 'root'
USER root


# Install EO expert packages only if ${sContainerUserHomeDirectory}/application/packages.txt exists
RUN bash ${sContainerUserHomeDirectory}/application/installUserPackage.sh --home-directory ${sContainerUserHomeDirectory}/application --package-apt


# From now, all commands executed are executed with the user ${sContainerUserName} who must not be root
USER ${sContainerUserName}


# Start the container using the python3 binary in the venv
CMD ["python3", "/usr/bin/gunicorn", "--workers", "1", "--max-requests", "1", "--timeout", "3600", "--bind", "0.0.0.0:5000", "wasdiProcessorServer:app"]
