# Zookeeper Dockerfile
FROM confluentinc/cp-zookeeper:latest

# Ensure we are using root user
USER root

# Update package lists
RUN yum -y update

# Install Java
RUN yum -y install java-11-openjdk

# Install nano
RUN yum -y install nano

# Clean up to reduce image size
RUN yum clean all
