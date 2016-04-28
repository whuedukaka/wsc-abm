#!/bin/bash

#if [ $# -ne 2 ]; then
#        echo "Usage: $0 currentMachineId totalNumOfMachines"
#        exit 1
#fi

NUM_NODES=`sort -u $PBS_NODEFILE | wc -l`
echo NUM_NODES: $NUM_NODES

CURRENT_NODE=`hostname`
CURRENT_ID=`sort -u $PBS_NODEFILE | cat -n | grep $CURRENT_NODE | awk '{print $1;}'`
CURRENT_ID=`expr $CURRENT_ID - 1`
echo CURRENT_MACHINE_ID: $CURRENT_ID

#export NETLOGO=/N/u/chenpeng/BigRed2/netlogo-5.2.0
NETLOGO=/u/chenpeng/netlogo-5.2.0
export CLASSPATH=$CLASSPATH:dist/lib/MonzeABM.jar:lib/opencsv-2.3.jar:$NETLOGO/NetLogo.jar

$JAVA_HOME/bin/java -Xmx30g -cp $CLASSPATH controller.ZambiaMultiThreadSimulator simulator.properties.txt 2 $CURRENT_ID $NUM_NODES
