#!/bin/bash
#export NETLOGO=/N/u/chenpeng/BigRed2/netlogo-5.2.0
NETLOGO=/u/chenpeng/netlogo-5.2.0

export CLASSPATH=$CLASSPATH:dist/lib/MonzeABM.jar:lib/opencsv-2.3.jar:$NETLOGO/NetLogo.jar

$JAVA_HOME/bin/java -Xmx1g -cp $CLASSPATH controller.ZambiaMultiThreadSimulator simulator.properties.txt 0
