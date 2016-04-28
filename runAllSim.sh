#!/bin/bash
sort -u $PBS_NODEFILE | (while read line; do
    echo "ssh to host:$line"
    ssh -n $line "cd abm-java/;./runSim.sh" &
  done

  #wait for ssh jobs to finish
  wait
)

