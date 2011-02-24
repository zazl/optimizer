#!/bin/sh

CURRENT_DIR=`pwd`
export CURRENT_DIR
apache-ant-1.8.2/bin/ant -lib org.ant4eclipse_1.0.0.M4/org.ant4eclipse_1.0.0.M4.jar -lib org.ant4eclipse_1.0.0.M4/libs -Dbasedir=$CURRENT_DIR -f ../zazl/org.dojotoolkit.optimizer.feature/scripts/optimizerbuild.xml


