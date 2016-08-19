#!/bin/bash

#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#   contributor license agreements.  The ASF licenses this file to You
#  under the Apache License, Version 2.0 (the "License"); you may not
#  use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.  For additional information regarding
#  copyright in this work, please see the NOTICE file in the top level
#  directory of this distribution.
#

mkdir -p /var/vcap/sys/log

exec > >(tee -a >(logger -p user.info -t vcap.$(basename $0).stdout) | awk -W interactive '{lineWithDate="echo [`date +\"%Y-%m-%d %H:%M:%S%z\"`] \"" $0 "\""; system(lineWithDate)  }' >>/var/vcap/sys/log/$(basename $0).log)
exec 2> >(tee -a >(logger -p user.error -t vcap.$(basename $0).stderr) | awk -W interactive '{lineWithDate="echo [`date +\"%Y-%m-%d %H:%M:%S%z\"`] \"" $0 "\""; system(lineWithDate)  }' >>/var/vcap/sys/log/$(basename $0).err.log)

pid_guard() {
  echo "------------ STARTING `basename $0` at `date` --------------" | tee /dev/stderr
  pidfile=$1
  name=$2

  if [ -f "$pidfile" ]; then
    pid=$(head -1 "$pidfile")

    if [ -n "$pid" ] && [ -e /proc/$pid ]; then
      echo "$name is already running, please stop it first"
      exit 1
    fi

    echo "Removing stale pidfile..."
    rm $pidfile
  fi
}

wait_pidfile() {
  pidfile=$1
  try_kill=$2
  timeout=${3:-0}
  force=${4:-0}
  countdown=$(( $timeout * 10 ))

  if [ -f "$pidfile" ]; then
    pid=$(head -1 "$pidfile")

    if [ -z "$pid" ]; then
      echo "Unable to get pid from $pidfile"
      exit 1
    fi

    if [ -e /proc/$pid ]; then
      if [ "$try_kill" = "1" ]; then
        echo "Killing $pidfile: $pid "
        kill $pid
      fi
      while [ -e /proc/$pid ]; do
        sleep 0.1
        [ "$countdown" != '0' -a $(( $countdown % 10 )) = '0' ] && echo -n .
        if [ $timeout -gt 0 ]; then
          if [ $countdown -eq 0 ]; then
            if [ "$force" = "1" ]; then
              echo -ne "\nKill timed out, using kill -9 on $pid... "
              kill -9 $pid
              sleep 0.5
            fi
            break
          else
            countdown=$(( $countdown - 1 ))
          fi
        fi
      done
      if [ -e /proc/$pid ]; then
        echo "Timed Out"
      else
        echo "Stopped"
      fi
    else
      echo "Process $pid is not running"
    fi

    rm -f $pidfile
  else
    echo "Pidfile $pidfile doesn't exist"
  fi
}

kill_and_wait() {
  pidfile=$1
  # Monit default timeout for start/stop is 30s
  # Append 'with timeout {n} seconds' to monit start/stop program configs
  timeout=${2:-25}
  force=${3:-1}

  wait_pidfile $pidfile 1 $timeout $force
}

running_in_container() {
  grep -q '/instance' /proc/self/cgroup
}