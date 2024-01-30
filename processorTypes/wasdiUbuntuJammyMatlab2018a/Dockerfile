FROM ubuntu:22.04


MAINTAINER "WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# This environment variable is setted to avoid packages to ask a confirmation
ENV DEBIAN_FRONTEND="noninteractive"

# This variable is setted to make Python stop creating files '.pyc'
ENV PYTHONDONTWRITEBYTECODE="False"

# This variable is setted to force the 'print' method of Python to flush message directly
ENV PYTHONUNBUFFERED="True"

# This variable is setted to use binaries from our virtual environment
ENV PATH="{{ sWasdiSystemUserHome }}/venv/bin:${PATH}"


# RUN generic to create user, group, directories, etc
RUN groupadd --gid {{ sWasdiSystemGroupId }} --non-unique {{ sWasdiSystemGroupName }} && \
    useradd --create-home --home-dir {{ sWasdiSystemUserHome }} --uid {{ sWasdiSystemUserId }} --gid {{ sWasdiSystemGroupId }} --non-unique --shell /bin/bash {{ sWasdiSystemUserName }}


# RUN generic to update the system and install common packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    apt-get update && \
    apt-get install \
        ca-certificates && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy main restricted universe multiverse"           > /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-updates main restricted universe multiverse"  >> /etc/apt/sources.list && \
    echo "deb https://mirror.ubuntu.ikoula.com/ jammy-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get dist-upgrade && \
    apt-get install \
    apt-get install \
        curl \
        git \
        gunicorn \
        nano \
        python3 \
        python3-flask \
        python3-requests \
        python3-pip \
        python3-venv \
        python3-wheel \
        unzip \
        wget && \
    rm --recursive --force /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm --recursive --force {} +


# From now, all commands executed are executed as {{ sWasdiSystemUserName }}
USER {{ sWasdiSystemUserName }}


# Create the application directory
# Create the virtual environment
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/application && \
    python3 -m venv --system-site-packages {{ sWasdiSystemUserHome }}/venv


# Set the workdir to {{ sWasdiSystemUserHome }}/application
WORKDIR {{ sWasdiSystemUserHome }}/application


# Install the matlab runtime
RUN mkdir --parents \
        {{ sWasdiSystemUserHome }}/matlab \
        /tmp/matlab && \
    wget --no-verbose --show-progress --progress=bar:force:noscroll https://ssd.mathworks.com/supportfiles/downloads/R2018a/deployment_files/R2018a/installers/glnxa64/MCR_R2018a_glnxa64_installer.zip --output-document /tmp/matlab/matlab.zip && \
    unzip /tmp/matlab/matlab.zip -d /tmp/matlab && \
    /tmp/matlab/install -mode silent -agreeToLicense yes -outputFile /tmp/matlab/install_log.txt -agreeToLicense yes -outputFile install_log.txt -destinationFolder {{ sWasdiSystemUserHome }}/matlab/ && \
    rm -rf \
        /tmp/matlab \
        /tmp/mathworks_*

ENV LD_LIBRARY_PATH="{{ sWasdiSystemUserHome }}/matlab/v94/runtime/glnxa64:{{ sWasdiSystemUserHome }}/matlab/v94/bin/glnxa64:{{ sWasdiSystemUserHome }}/matlab/v94/sys/os/glnxa64:{{ sWasdiSystemUserHome }}/matlab/v94/extern/bin/glnxa64"
