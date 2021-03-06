#!/bin/bash

 BUILD_NUMBER=$1 
 INCLUDE=$2
 EXCLUDE=$3
 SUITE_NAME=$4
 MAJOR_VERSION=$5
 MINOR_VERSION=$6
 SUITE_ID=$7
 SUITE_NUMBER=$8
 BYON_MACHINES=$9
 SUPPORTED_CLOUDS=${10}
 EC2_REGION=${11}
 SUITE_WORK_DIR=${12}; export SUITE_WORK_DIR
 SUITE_DEPLOY_DIR=${13}; export SUITE_DEPLOY_DIR
 EXT_JAVA_OPTIONS="${EXT_JAVA_OPTIONS} -Dcom.gs.work=${SUITE_WORK_DIR} -Dcom.gs.deploy=${SUITE_DEPLOY_DIR}"; export EXT_JAVA_OPTIONS 
 

echo clouds=$SUPPORTED_CLOUDS

mkdir ${BUILD_DIR}/../${SUITE_NAME}
cd ${BUILD_DIR}/../SGTest

mvn test -e -X -U -P tgrid-sgtest-cloudify \
-Dsgtest.cloud.enabled=false \
-Dsgtest.buildNumber=${BUILD_NUMBER} \
-Dcloudify.home=${BUILD_DIR} \
-Dincludes=${INCLUDE} \
-Dexcludes=${EXCLUDE} \
-Djava.security.policy=policy/policy.all \
-Djava.awt.headless=true \
-Dsgtest.suiteName=${SUITE_NAME} \
-Dsgtest.suiteId=${SUITE_ID} \
-Dsgtest.summary.dir=${BUILD_DIR}/../${SUITE_NAME} \
-Dsgtest.numOfSuites=${SUITE_NUMBER} \
-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger \
-Dcom.gs.logging.level.config=true \
-Djava.util.logging.config.file=/export/tgrid/sgtest3.0-cloudify/bin/..//logging/sgtest_logging.properties \
-Dsgtest.buildFolder=../ \
-Dsgtest.url=http://192.168.9.121:8087/sgtest3.0-cloudify/ \
-Dcom.gs.work=${SUITE_WORK_DIR} \
-Dcom.gs.deploy=${SUITE_DEPLOY_DIR} \
-Dec2.region=${EC2_REGION} \
-DipList=${BYON_MACHINES} \
-Dsupported-clouds=${SUPPORTED_CLOUDS}

#return java exit code. 
exit $?