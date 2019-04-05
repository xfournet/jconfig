#!/bin/sh
set -e

echo "TRAVIS_BRANCH = $TRAVIS_BRANCH"
echo "TRAVIS_TAG = $TRAVIS_TAG"
echo "TRAVIS_PULL_REQUEST = $TRAVIS_PULL_REQUEST"

if [ "$TRAVIS_SECURE_ENV_VARS" = "true" -a "$TRAVIS_PULL_REQUEST" = "false" -a "$SONAR_TOKEN" != "" -a "$GPG_SECRET_KEYS" != "" ]; then
    echo
    echo "Build, test and generate Sonar report"
    mvn -B clean org.jacoco:jacoco-maven-plugin:prepare-agent package sonar:sonar

    if [ \("$TRAVIS_BRANCH" = "master" -o "$TRAVIS_TAG" != ""\) -a "$OSSRH_USERNAME" != "" ]; then
        echo
        echo "Build, sign with GPG and deploy to OSSRH"
        mvn -B -P release -DskipTests=true clean deploy
    else
        echo
        echo "Build and sign with GPG"
        mvn -B -P release -DskipTests=true clean verify
    fi
else
    echo
    echo "Build and verify"
    mvn -B clean verify
fi
