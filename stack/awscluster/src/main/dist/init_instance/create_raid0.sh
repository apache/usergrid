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

# install postfix first and prevent it from prompting the user
debconf-set-selections <<< "postfix postfix/mailname string your.hostname.com"
debconf-set-selections <<< "postfix postfix/main_mailer_type string 'Internet Site'"
apt-get install -y postfix

# install mdadm RAID controller
apt-get -y --force-yes install mdadm

# m1.xlarge has four 414GB disks, but only /dev/xvdb is mounted
# unmount /dev/xvdb so we can use it in our setup
umount /mnt

# create & format partition on each of our four disks
(echo o; echo n; echo p; echo 1; echo ; echo; echo w) | fdisk /dev/xvdb
(echo o; echo n; echo p; echo 1; echo ; echo; echo w) | fdisk /dev/xvdc
(echo o; echo n; echo p; echo 1; echo ; echo; echo w) | fdisk /dev/xvdd
(echo o; echo n; echo p; echo 1; echo ; echo; echo w) | fdisk /dev/xvde

# create striped RAID0 device with our four disks
mdadm --create --verbose /dev/md0 --level=stripe \
--raid-devices=4 /dev/xvdb1 /dev/xvdc1 /dev/xvdd1 /dev/xvde1

# save config
mdadm --detail --scan | tee /etc/mdadm/mdadm.conf

# create, mount and save disk to stab
mkfs.ext4 -b 4096 -E stride=32,stripe-width=128 -L Stripe /dev/md0
mkdir -p /mnt
echo "/dev/md0  /mnt  auto  defaults,nobootwait,noatime 0 2" | tee /etc/fstab
mount /mnt 
