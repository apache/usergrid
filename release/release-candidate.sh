#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#
# This script is used to create a release candidate. It will update the current
# .usergridversion as well as creates a branch for the new release candidate and
# publishes the source distrobution and signatures to be voted on.
#
#   master~1 (0.5.0-snapshot) ----- master (0.6.0-snapshot)
#                             \---- 0.5.0 (0.5.0)
#
# A email template will be generated after successfully generating a release
# candidate which will need to be sent to the dev@ and private@ mailing lists.
#
set -o errexit
set -o nounset

rc_tag_version=0
usergrid_git_web_url='https://git-wip-us.apache.org/repos/asf?p=usergrid.git'
usergrid_svn_dist_url='https://dist.apache.org/repos/dist/dev/usergrid'

function print_help_and_exit {
cat <<EOF
Apache Usergrid release candidate tool.

Usage: $0 [-h] [-l p|m|M] [-r #] [-p | publish]

  -h   Print this help message and exit
  -l   Increment level, must be one of:
       p, patch (default)
  		 m, minor
  		 M, major
  -r   Release candidate number (default: 0)
  -p   Publish the release candidate (default: dry-run, does not publish anything)
EOF
exit 0
}

publish=0
increment_level="patch"
rc_tag_version=0
while getopts ":hl:r:p" opt; do
  case $opt in
    l)
      case ${OPTARG} in
        'p' | 'patch') increment_level='patch' ;;
        'm' | 'minor') increment_level='minor' ;;
        'M' | 'major') increment_level='major' ;;
         *) echo 'Unknown increment level'; exit 1 ;;
      esac
      ;;
    r)
      rc_tag_version=${OPTARG}
      ;;
    p)
      publish=1
      ;;
    h)
      print_help_and_exit
      ;;
    *  )
      echo "Unknown option: -$OPTARG"
      print_help_and_exit
      ;;
  esac
done

shift $(($OPTIND - 1))
if [[ "${1:-dry-run}" == "publish" ]]; then
  publish=1
fi

# Update local repository
git fetch --all -q
git fetch --tags -q

# Set the base dir for the script to be the top level of the repository
base_dir=$(git rev-parse --show-toplevel)

# Verify that this is a clean repository
if [[ -n "`git status --porcelain`" ]]; then
  echo "ERROR: Please run from a clean git repository."
  exit 1
elif [[ "`git rev-parse --abbrev-ref HEAD`" != "master" ]]; then
  echo "ERROR: This script must be run from master."
  exit 1
fi

if [[ "$base_dir" != "$PWD" ]]; then
  echo "Warrning: This script must be run from the root of the repository ${base_dir}"
  cd $base_dir
fi

# Calculate the new version string
current_version=$(cat .usergridversion | tr '[a-z]' '[A-Z]')
if ! [[ $current_version =~ .*-SNAPSHOT ]]; then
  echo "ERROR: .usergridversion is required to contain 'SNAPSHOT', it is ${current_version}"
  exit 1
else
  major=`echo $current_version | cut -d. -f1`
  minor=`echo $current_version | cut -d. -f2`
  patch=`echo $current_version | cut -d. -f3 | cut -d- -f1`

  current_version="${major}.${minor}.${patch}"

  if [[ $increment_level == "patch" ]]; then
    new_master_version="${major}.${minor}.$((patch + 1))"
  elif [[ $increment_level == "minor" ]]; then
    new_master_version="${major}.$((minor + 1)).0"
  elif [[ $increment_level == "major" ]]; then
    new_master_version="$((major + 1)).0.0"
  else
    echo "Unknown release increment ${increment_level}"
    exit 1
  fi

  new_snapshot_version="${new_master_version}-SNAPSHOT"
fi

# Add the rc tag to the current version
current_version_tag="${current_version}-rc${rc_tag_version}"

# Make sure the branch does not exist
if git rev-parse $current_version_tag >/dev/null 2>&1; then
  echo "ERROR: ${current_version_tag} exists."
  exit 1
fi

# Reset instructions
current_git_rev=$(git rev-parse HEAD)
function print_reset_instructions {
cat <<EOF
To roll back your local repo you will need to run:

  git checkout master
  git reset --hard ${current_git_rev}
  git branch -D ${current_version_tag}
EOF
}

# If anything goes wrong from here then print roll back instructions before exiting.
function print_rollback_instructions {
  echo "ERROR: Looks like something has failed while creating the release candidate."
  print_reset_instructions
}
trap print_rollback_instructions EXIT

# All check are now complete, before we start alert if we are in dry-run
if [[ $publish == 0 ]]; then
  echo "Performing dry-run"
fi

# This should be a clean repo we are working against. Run clean just to ensure it is.
git clean -fdxq

echo "Creating ${current_version_tag} branch"
git branch $current_version_tag $(git rev-parse HEAD)

# Build the source distribution from the new branch
echo "Checking out ${current_version_tag} branch and updating .usergridversion"
git checkout $current_version_tag
# Increment the version and create a branch
echo $current_version_tag > .usergridversion
git add .usergridversion
git commit -m "Updating .usergridversion to ${current_version_tag}."

echo "Building the source distribution"
dist_dir=${base_dir}/dist
dist_name="apache-usergrid-${current_version_tag}"

mkdir -p ${dist_dir}
git archive --prefix=${dist_name}/ -o ${dist_dir}/${dist_name}.tar.gz HEAD

cd ${dist_dir}
# Sign the tarball.
echo "Signing the distribution"
gpg --armor --output ${dist_dir}/${dist_name}.tar.gz.asc --detach-sig ${dist_dir}/${dist_name}.tar.gz

# Create the checksums
echo "Creating checksums"
gpg --print-md MD5 ${dist_name}.tar.gz > ${dist_name}.tar.gz.md5
shasum ${dist_name}.tar.gz > ${dist_name}.tar.gz.sha

# Publish release candidate to svn and commit and push the new git branch
if [[ $publish == 1 ]]; then
  echo "Publishing release candidate to ${usergrid_svn_dist_url}/${current_version_tag}"
  svn mkdir ${usergrid_svn_dist_url}/${current_version_tag} -m "usergrid-${current_version} release candidate ${rc_tag_version}"
  svn co --depth=empty ${usergrid_svn_dist_url}/${current_version_tag} ${dist_dir}
  svn add ${dist_name}*
  svn ci -m "usergrid-${current_version} release candidate ${rc_tag_version}"

  echo "Pushing new branch ${current_version_tag} to origin"
  cd ${base_dir}
  git push origin ${current_version_tag}
  echo "Pushing updated .usergridversion to master"
  git checkout master
  git push origin master
fi

cd ${base_dir}

current_commit_id=`git rev-parse HEAD`

echo "Done creating the release candidate. The following draft email has been created"
echo "to send to the dev@usergrid.apache.org mailing list"
echo

# Create the email template for the release candidate to be sent to the mailing lists.
if [[ $(uname) == Darwin ]]; then
  vote_end=$(date -v+3d)
else
  vote_end=$(date -d+3days)
fi

MESSAGE=$(cat <<__EOF__
To: dev@usergrid.apache.org
Subject: [VOTE] Release Apache Usergrid ${current_version} RC${rc_tag_version}

All,

I propose that we accept the following release candidate as the official
Apache Usergrid ${current_version} release.


Usergrid ${current_version_tag} includes the following:
---
The CHANGELOG for the release is available at:
${usergrid_git_web_url}&f=CHANGELOG&hb=${current_version_tag}

The branch used to create the release candidate is:
${usergrid_git_web_url}&hb=${current_version_tag}

The current Git commit ID is ${current_commit_id}

The release candidate is available at:
${usergrid_svn_dist_url}/${current_version_tag}/${dist_name}.tar.gz

The MD5 checksum of the release candidate can be found at:
${usergrid_svn_dist_url}/${current_version_tag}/${dist_name}.tar.gz.md5

The signature of the release candidate can be found at:
${usergrid_svn_dist_url}/${current_version_tag}/${dist_name}.tar.gz.asc

The GPG key used to sign the release are available at:
${usergrid_svn_dist_url}/KEYS

Please download, verify, and test.

The vote will close on ${vote_end}

[ ] +1 Release this as Apache Usergrid ${current_version}
[ ] +0
[ ] -1 Do not release this as Apache Usergrid ${current_version} because...

__EOF__
)

echo "--------------------------------------------------------------------------------"
echo
echo "${MESSAGE}"
echo
echo "--------------------------------------------------------------------------------"
echo

# Print reset instructions if this was a dry-run
if [[ $publish == 0 ]]; then
  echo
  echo "This was a dry run, nothing has been published."
  echo
  print_reset_instructions
fi

# Unset error message handler and exit
trap '' EXIT
exit 0
