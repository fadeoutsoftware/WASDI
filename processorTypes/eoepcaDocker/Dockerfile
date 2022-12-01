FROM ubuntu:20.04


MAINTAINER "WASDI SARL"


# From now, all commands executed are executed with the user 'root'
USER root


# Define arguments
# Values not setted here are provided with the docker build command
ARG sContainerGroupId="1000"
ARG sContainerGroupName="wasdi"
ARG sContainerUserId="1000"
ARG sContainerUserHomeDirectory="/home/$sContainerUserName"
ARG sContainerUserName="wasdi"
ARG PIP_INSTALL_WASDI_ARGUMENTS=""


# Create environment variables:
#   - SYSTEM (name are imposed):
#     - DEBIAN_FRONTEND: to avoid packages to ask a confirmation
#     - PATH: to use binaries from our future virtual environment as default
#     - PYTHONDONTWRITEBYTECODE: to stop Python to write pyc files
#     - PYTHONUNBUFFERED: to force 'print' to flush message directly
ENV DEBIAN_FRONTEND="noninteractive"
ENV PATH="$sContainerUserHomeDirectory/venv/bin:$PATH"
ENV PYTHONDONTWRITEBYTECODE="False"
ENV PYTHONUNBUFFERED="True"


# Big task 1:
#   - Create a group
#   - Create a user
#   - Change rights on /home
#   - Configure the APT command
#   - Update the repository cache
#   - Update the system
#   - Install needed packages
RUN echo "APT::Install-Suggests \"0\";" > /etc/apt/apt.conf.d/99local && \
    echo "APT::Install-Recommends \"0\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Get::Assume-Yes \"true\";" >> /etc/apt/apt.conf.d/99local && \
    echo "APT::Keep-Downloaded-Packages \"false\";" >> /etc/apt/apt.conf.d/99local && \
    echo "Dpkg::Options::--force-confold;" >> /etc/apt/apt.conf.d/99local && \
    echo "deb http://lu.archive.ubuntu.com/ubuntu/ focal main restricted" > /etc/apt/sources.list && \
    apt-get update && \
    apt-get install ca-certificates && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt focal main restricted universe multiverse" > /etc/apt/sources.list && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt focal-updates main restricted universe multiverse" >> /etc/apt/sources.list && \
    echo "deb mirror://mirrors.ubuntu.com/mirrors.txt focal-security main restricted universe multiverse" >> /etc/apt/sources.list && \
    apt-get update && \
    apt-get upgrade && \
    apt-get install \
        gdal-bin \
        git \
        python3 \
        python3-gdal \
        python3-requests \
        python3-pip \
        python3-venv && \
    rm -rf /var/lib/apt/lists/* && \
    find / -xdev -type d -name __pycache__ -exec rm -rf {} +


# Big task 2:
#   - create the internal group
#   - create the internal user
#   - create all directories
#   - create a sudo configuration to become root with tomcat
RUN groupadd --gid $sContainerGroupId --non-unique $sContainerGroupName && \
    useradd --create-home --home-dir $sContainerUserHomeDirectory --uid $sContainerUserId --gid $sContainerGroupId --non-unique --shell /bin/bash $sContainerUserName && \
    mkdir -p $sContainerUserHomeDirectory/application && \
    chown -R $sContainerUserName:$sContainerGroupName $sContainerUserHomeDirectory/ && \
    chmod -R 755 $sContainerUserHomeDirectory/


# Set the workdir to $sContainerUserHomeDirectory
WORKDIR $sContainerUserHomeDirectory/application


# From now, all commands executed are executed with the user $sContainerUserName who must not be root
USER $sContainerUserName


# Create a virtual environment
# [IN OUR VENV] Install needed Python libraries
# Please note:
#   - 'wasdi' is installed independently to be able to set the repository
#   - there is no cache: I suppose because it is too much dynamic
RUN python3 -m venv --system-site-packages $sContainerUserHomeDirectory/venv && \
    pip3 install --no-cache-dir --no-compile $PIP_INSTALL_WASDI_ARGUMENTS wasdi && \
    pip3 install --no-cache-dir --no-compile \
        numpy


# Copy:
#   - the tool to install package
#   - the application files
COPY --chown=$sContainerUserName:$sContainerGroupName ./ $sContainerUserHomeDirectory/application/


# Install user's Python libraries only if $sContainerUserHomeDirectory/application/pip.txt exists
RUN bash $sContainerUserHomeDirectory/application/installUserPackage.sh --home-directory $sContainerUserHomeDirectory/application --package-pip


# From now, all commands executed are executed with the user 'root'
USER root


# Install user's packages only if $sContainerUserHomeDirectory/application/packages.txt exists
RUN bash $sContainerUserHomeDirectory/application/installUserPackage.sh --home-directory $sContainerUserHomeDirectory/application --package-apt


# From now, all commands executed are executed with the user $sContainerUserName who must not be root
USER $sContainerUserName


# Execute the processor
CMD ["python3", "eoepcaProcessorExecutor.py"]
