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


# Install and stop Cassandra
sudo apt-get update
sudo apt-get install -y collectd collectd-utils


. /etc/profile.d/aws-credentials.sh
. /etc/profile.d/usergrid-env.sh

#Wait for graphite to start
cd /usr/share/usergrid/scripts
groovy wait_for_instances.groovy graphite ${GRAPHITE_NUM_SERVERS}
GRAPHITE_HOST=$(groovy get_first_instance.groovy graphite)



cat > /etc/collectd/collectd.conf << EOF
Hostname "${PUBLIC_HOSTNAME}"
LoadPlugin cpu
LoadPlugin df
LoadPlugin entropy
LoadPlugin interface
LoadPlugin load
LoadPlugin logfile
LoadPlugin memory
LoadPlugin processes
LoadPlugin rrdtool
LoadPlugin users
LoadPlugin write_graphite
LoadPlugin java

#ethernet montioring
<Plugin interface>
    Interface "eth0"
    IgnoreSelected false
</Plugin>

#Send to graphite
<Plugin write_graphite>
    <Node "graphing">
        Host "${GRAPHITE_HOST}"
        Port "2003"
        Protocol "tcp"
        LogSendErrors true
        Prefix "collectd."
        StoreRates true
        AlwaysAppendDS false
        EscapeCharacter "_"
    </Node>
</Plugin>

#Raid 0 monitoring
<Plugin df>
    Device "/dev/md0"
    MountPoint "/mnt"
    FSType "ext4"
</Plugin>

<Plugin "logfile">
  LogLevel "info"
  File "/var/log/collectd.log"
  Timestamp true
</Plugin>


<Plugin "java">
  # required JVM argument is the classpath
  # JVMArg "-Djava.class.path=/installpath/collectd/share/collectd/java"
  # Since version 4.8.4 (commit c983405) the API and GenericJMX plugin are
  # provided as .jar files.
  JVMARG "-Djava.class.path=/usr/share/collectd/java/generic-jmx.jar:/usr/share/collectd/java/collectd-api.jar"
  LoadPlugin "org.collectd.java.GenericJMX"

    <Plugin "GenericJMX">
      ################
      # MBean blocks #
      ################
      # Number of classes being loaded.
      <MBean "classes">
        ObjectName "java.lang:type=ClassLoading"
        #InstancePrefix ""
        #InstanceFrom ""

        <Value>
          Type "gauge"
          InstancePrefix "loaded_classes"
          #InstanceFrom ""
          Table false
          Attribute "LoadedClassCount"
        </Value>
      </MBean>

      # Time spent by the JVM compiling or optimizing.
      <MBean "compilation">
        ObjectName "java.lang:type=Compilation"
        #InstancePrefix ""
        #InstanceFrom ""

        <Value>
          Type "total_time_in_ms"
          InstancePrefix "compilation_time"
          #InstanceFrom ""
          Table false
          Attribute "TotalCompilationTime"
        </Value>
      </MBean>

      # Garbage collector information
      <MBean "garbage_collector">
        ObjectName "java.lang:type=GarbageCollector,*"
        InstancePrefix "gc-"
        InstanceFrom "name"

        <Value>
          Type "invocations"
          #InstancePrefix ""
          #InstanceFrom ""
          Table false
          Attribute "CollectionCount"
        </Value>

        <Value>
          Type "total_time_in_ms"
          InstancePrefix "collection_time"
          #InstanceFrom ""
          Table false
          Attribute "CollectionTime"
        </Value>

  #      # Not that useful, therefore commented out.
  #      <Value>
  #        Type "threads"
  #        #InstancePrefix ""
  #        #InstanceFrom ""
  #        Table false
  #        # Demonstration how to access composite types
  #        Attribute "LastGcInfo.GcThreadCount"
  #      </Value>
      </MBean>

      ######################################
      # Define the "jmx_memory" type as:   #
      #   jmx_memory  value:GAUGE:0:U      #
      # See types.db(5) for details.       #
      ######################################

      # Generic heap/nonheap memory usage.
      # Standard Java mbeans
      # Memory usage by memory pool.
      <MBean "memory_pool">
        ObjectName "java.lang:type=MemoryPool,*"
        InstancePrefix "memory_pool-"
        InstanceFrom "name"
        <Value>
          Type "memory"
          #InstancePrefix ""
          #InstanceFrom ""
          Table true
          Attribute "Usage"
        </Value>
      </MBean>

      # Heap memory usage
      <MBean "memory_heap">
        ObjectName "java.lang:type=Memory"
        #InstanceFrom ""
        InstancePrefix "memory-heap"

        # Creates four values: committed, init, max, used
        <Value>
          Type "memory"
          #InstancePrefix ""
          #InstanceFrom ""
          Table true
          Attribute "HeapMemoryUsage"
        </Value>
      </MBean>

      # Non-heap memory usage
      <MBean "memory_nonheap">
        ObjectName "java.lang:type=Memory"
        #InstanceFrom ""
        InstancePrefix "memory-nonheap"

        # Creates four values: committed, init, max, used
        <Value>
          Type "memory"
          #InstancePrefix ""
          #InstanceFrom ""
          Table true
          Attribute "NonHeapMemoryUsage"
        </Value>
      </MBean>

      <MBean "garbage_collector">
        ObjectName "java.lang:type=GarbageCollector,*"
        InstancePrefix "gc-"
        InstanceFrom "name"

        <Value>
          Type "invocations"
          #InstancePrefix ""
          #InstanceFrom ""
          Table false
          Attribute "CollectionCount"
        </Value>

        <Value>
          Type "total_time_in_ms"
          InstancePrefix "collection_time"
          #InstanceFrom ""
          Table false
          Attribute "CollectionTime"
        </Value>
      </MBean>

      ### MBeans by Catalina / Tomcat ###
      # The global request processor (summary for each request processor)
      <MBean "catalina/global_request_processor">
        ObjectName "Catalina:type=GlobalRequestProcessor,*"
        InstancePrefix "request_processor-"
        InstanceFrom "name"

        <Value>
          Type "io_octets"
          InstancePrefix "global"
          #InstanceFrom ""
          Table false
          Attribute "bytesReceived"
          Attribute "bytesSent"
        </Value>

        <Value>
          Type "total_requests"
          InstancePrefix "global"
          #InstanceFrom ""
          Table false
          Attribute "requestCount"
        </Value>

        <Value>
          Type "total_time_in_ms"
          InstancePrefix "global-processing"
          #InstanceFrom ""
          Table false
          Attribute "processingTime"
        </Value>
      </MBean>

      # Details for each  request processor
      <MBean "catalina/detailed_request_processor">
        ObjectName "Catalina:type=RequestProcessor,*"
        InstancePrefix "request_processor-"
        InstanceFrom "worker"

        <Value>
          Type "io_octets"
          #InstancePrefix ""
          InstanceFrom "name"
          Table false
          Attribute "bytesReceived"
          Attribute "bytesSent"
        </Value>

        <Value>
          Type "total_requests"
          #InstancePrefix ""
          InstanceFrom "name"
          Table false
          Attribute "requestCount"
        </Value>

        <Value>
          Type "total_time_in_ms"
          InstancePrefix "processing-"
          InstanceFrom "name"
          Table false
          Attribute "processingTime"
        </Value>
      </MBean>

      # Thread pool
      <MBean "catalina/thread_pool">
        ObjectName "Catalina:type=ThreadPool,*"
        InstancePrefix "request_processor-"
        InstanceFrom "name"

        <Value>
          Type "threads"
          InstancePrefix "total"
          #InstanceFrom ""
          Table false
          Attribute "currentThreadCount"
        </Value>

        <Value>
          Type "threads"
          InstancePrefix "running"
          #InstanceFrom ""
          Table false
          Attribute "currentThreadsBusy"
        </Value>
      </MBean>

      #####################
      # Connection blocks #
      #####################
      <Connection>
        ServiceURL "service:jmx:rmi:///jndi/rmi://localhost:8050/jmxrmi"
        Host "${PUBLIC_HOSTNAME}"
        Collect "classes"
        Collect "compilation"
        Collect "memory_pool"
        Collect "memory_heap"
        Collect "memory_nonheap"
        Collect "garbage_collector"
        Collect "catalina/global_request_processor"
        Collect "catalina/detailed_request_processor"
        Collect "catalina/thread_pool"
      </Connection>
    </Plugin>
</Plugin>
EOF

service collectd stop
service collectd start

#Set the hostname into collectd
sed -i.bak "s/#Hostname \"localhost\"//g" /etc/collectd/collectd.conf

