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



# WARNING: this does not work for any instances that have more than 2 ephemeral disks



# install postfix first and prevent it from prompting the user
debconf-set-selections <<< "postfix postfix/mailname string your.hostname.com"
debconf-set-selections <<< "postfix postfix/main_mailer_type string 'Internet Site'"
apt-get install -y postfix

# install mdadm RAID controller
apt-get -y --force-yes install mdadm

# m1.xlarge has four 414GB disks, but only /dev/xvdb is mounted
# unmount /dev/xvdb so we can use it in our setup
umount /mnt


#We only support 2 ephemeral disks.  Most c3.x instances only have 2 disks and they're our target
# create striped RAID0 device with our 2 disks
yes | mdadm --create --verbose /dev/md0 --level=0 --raid-devices=2 -c 256  /dev/xvdb /dev/xvdc




# save config
mdadm --detail --scan | tee /etc/mdadm/mdadm.conf

# create, mount and save disk to stab
mkfs.ext4 -b 4096 -E stride=32,stripe-width=128 -L Stripe /dev/md0
mkdir -p /mnt
echo "/dev/md0  /mnt/  ext4    defaults,noatime,nofail 0 0" >> /etc/fstab
mount /dev/md0 /mnt
