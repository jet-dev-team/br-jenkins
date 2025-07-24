ARG JENKINS_VERSION
FROM jenkins/jenkins:${JENKINS_VERSION}-lts-jdk21

USER root

# Install Python 3, pip, and venv
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-pip python3-venv \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Create a virtual environment and install ansible-core
RUN python3 -m venv /opt/ansible-venv \
    && . /opt/ansible-venv/bin/activate \
    && pip install --upgrade pip \
    && pip install ansible \
    && deactivate

# Make sure the Ansible venv binaries are in the PATH
ENV PATH="/opt/ansible-venv/bin:${PATH}"

USER jenkins
