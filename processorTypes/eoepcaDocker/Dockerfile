FROM ubuntu:20.04


LABEL org.opencontainers.image.authors="WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# Define arguments
# Values not setted here are provided with the docker build command
ARG sContainerGroupId="1000"
ARG sContainerGroupName="wasdi"
ARG sContainerUserId="1000"
ARG sContainerUserName="wasdi"
ARG sContainerUserHomeDirectory="/home/${sContainerUserName}"
ARG PIP_INSTALL_WASDI_ARGUMENTS=""


# This environment variable is setted to avoid packages to ask a confirmation
ENV DEBIAN_FRONTEND="noninteractive"

# This variable is setted to be able to use the binaries of the virtual environment
ENV PATH="${sContainerUserHomeDirectory}/venv/bin:$PATH"

# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="False"

# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="True"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid ${sContainerGroupId} --non-unique ${sContainerGroupName} && \
    useradd --create-home --home-dir ${sContainerUserHomeDirectory} --uid ${sContainerUserId} --gid ${sContainerGroupId} --non-unique --shell /bin/bash ${sContainerUserName} && \
    mkdir --parents ${sContainerUserHomeDirectory}/application && \
    chown --recursive ${sContainerUserName}:${sContainerGroupName} ${sContainerUserHomeDirectory}/ && \
    chmod 755 ${sContainerUserHomeDirectory}/


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
        dos2unix \
        gdal-bin \
        git \
        python3 \
        python3-boto3 \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# Set the workdir
WORKDIR ${sContainerUserHomeDirectory}/application


# From now, all commands executed are executed with the user ${sContainerUserName}
USER ${sContainerUserName}


# Create a virtual environment
# [IN OUR VENV] Install needed Python libraries
# Please note:
#   - 'wasdi' is installed independently to be able to set the repository
#   - there is no cache: I suppose because it is too much dynamic
RUN python3 -m venv --system-site-packages $sContainerUserHomeDirectory/venv && \
    pip3 install --no-cache-dir --no-compile ${PIP_INSTALL_WASDI_ARGUMENTS} wasdi && \
    pip3 install --no-cache-dir --no-compile \
        numpy


# Copy:
#   - the tool to install package
#   - the application files
COPY --chown=${sContainerUserName}:${sContainerGroupName} ./ ${sContainerUserHomeDirectory}/application/


# From now, all commands executed are executed with the user 'root'
USER root


# Install user's packages only if $sContainerUserHomeDirectory/application/packages.txt exists
RUN bash ${sContainerUserHomeDirectory}/application/installUserPackage.sh \
        --application-directory ${sContainerUserHomeDirectory}/application \
        --home-directory ${sContainerUserHomeDirectory} \
        --package-apt


# From now, all commands executed are executed with the user ${sContainerUserName}
USER ${sContainerUserName}


# Install user's Python libraries only if $sContainerUserHomeDirectory/application/pip.txt exists
RUN bash ${sContainerUserHomeDirectory}/application/installUserPackage.sh \
        --application-directory ${sContainerUserHomeDirectory}/application \
        --home-directory ${sContainerUserHomeDirectory} \
        --package-pip


# Execute the processor
CMD [ \
    "python3", \
    "eoepcaProcessorExecutor.py" \
]
