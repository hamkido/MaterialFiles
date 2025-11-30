FROM cimg/android:2024.01

USER root

# Install ninja-build
RUN apt-get update && \
    apt-get install -y ninja-build && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV ANDROID_HOME=/home/circleci/android-sdk
ENV ANDROID_SDK_ROOT=/home/circleci/android-sdk
ENV GRADLE_USER_HOME=/opt/gradle-home

# Accept licenses and install required Android SDK components
RUN mkdir -p ${ANDROID_HOME}/licenses && \
    echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" > ${ANDROID_HOME}/licenses/android-sdk-license && \
    echo "84831b9409646a918e30573bab4c9c91346d8abd" >> ${ANDROID_HOME}/licenses/android-sdk-license && \
    echo "d975f751698a77b662f1254ddbeed3901e976f5a" >> ${ANDROID_HOME}/licenses/android-sdk-license && \
    sdkmanager "ndk;28.1.13356709" "build-tools;36.0.0" "platforms;android-36"

# Copy gradle wrapper files and pre-download Gradle
COPY gradle/ /tmp/project/gradle/
COPY gradlew /tmp/project/
RUN cd /tmp/project && chmod +x gradlew && ./gradlew --version && rm -rf /tmp/project

WORKDIR /project
