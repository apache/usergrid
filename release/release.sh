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
# This script is used to publish the official release after a successful
# vote of a release-candidate.

set -e
set -o nounset

usergrid_git_url='https://git-wip-us.apache.org/repos/asf/usergrid.git'
usergrid_git_web_url='https://git-wip-us.apache.org/repos/asf?p=usergrid.git'
usergrid_svn_dist_url='https://dist.apache.org/repos/dist/release/usergrid'
usergrid_svn_dev_dist_url='https://dist.apache.org/repos/dist/dev/usergrid'

function print_help_and_exit {
cat <<EOF
Apache Usergrid release tool.

Usage: $0 [-h] [-r #] [-p | publish]

  -h   Print this help message and exit
  -r   Release candidate number (default: 0)
  -p   Publish (default: dry-run (does not publish anything))
EOF
exit 0
}

publish=0
rc_tag_version=0
while getopts ":hl:r:p" opt; do
  case $opt in
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

# Ensure that a signing key is available
if [[ -z "`git config user.signingkey`" ]]; then
  cat <<EOF
Error: No GPG signing key can be found within gitconfig.

To configure one, find your code signing key's ID with

   gpg --list-secret-keys

Then configure it as the signing key for this repository with

   git config user.signingkey YOUR_KEY_ID
EOF
  exit 1
fi

# Set the base dir for the script to be the top level of the repository
base_dir=$(git rev-parse --show-toplevel)
# Verify that this is a clean repository
if [[ -n "`git status --porcelain`" ]]; then
  echo "ERROR: Please run from a clean master."
  exit 1
elif [[ "`git rev-parse --abbrev-ref HEAD`" == "master" ]]; then
  echo "ERROR: This script must be run from the released branch."
  exit 1
fi

if [[ "$base_dir" != "$PWD" ]]; then
  echo "Warrning: This script must be run from the root of the repository ${base_dir}"
  cd $base_dir
fi

# Make sure that this is not on a snapshot release
tagged_version=$(cat .usergridversion | tr '[a-z]' '[A-Z]')
current_version=$tagged_version
if [[ $current_version =~ .*-SNAPSHOT ]]; then
  echo "ERROR: .usergridversion can not be a 'SNAPSHOT', it is ${current_version}"
  exit 1
else
  major=`echo $current_version | cut -d. -f1`
  minor=`echo $current_version | cut -d. -f2`
  patch=`echo $current_version | cut -d. -f3 | cut -d- -f1`

  current_version="${major}.${minor}.${patch}"
fi

# Make sure the tag does not exist
if git rev-parse $current_version >/dev/null 2>&1; then
  echo "ERROR: ${current_version} tag exists."
  exit 1
fi

# All check are now complete, before we start alert if we are in dry-run
if [[ $publish == 0 ]]; then
  echo "Performing dry-run"
fi

# Create a branch for the release and update the .usergridversion and tag it
echo "Creating release branch and tag for ${current_version}"
git checkout -b $current_version
echo $current_version > .usergridversion
git add .usergridversion
git commit -m "Updating .usergridversion to ${current_version}."

git tag -s "${current_version}" -m "usergrid-${current_version} release." $current_version

#if [[ $publish == 1 ]]; then
  #git push origin $current_version
  #git push origin --tags
#fi

dist_name="apache-usergrid-${current_version}"

dist_dir=${base_dir}/dist
release_dir=${dist_dir}/${current_version}
mkdir -p $release_dir
cd $dist_dir

#if [[ $publish == 1 ]]; then
#  echo "Publishing the release"
  # Make and checkout the release dist directory
#  svn mkdir ${usergrid_svn_dist_url}/${current_version} -m "usergrid-${current_version} release"
#  svn co --depth=empty ${usergrid_svn_dist_url}/${current_version} ${release_dir}
#fi

# Now that the .usergridversion has been updated to the release version build the release source dist from it
cd $base_dir
git archive --prefix=${dist_name}/ -o ${release_dir}/${dist_name}.tar.gz HEAD

cd ${release_dir}
# Sign the tarball.
echo "Signing the distribution"
gpg --armor --output ${release_dir}/${dist_name}.tar.gz.asc --detach-sig ${release_dir}/${dist_name}.tar.gz

# Create the checksums
echo "Creating checksums"
# md5
gpg --print-md MD5 ${dist_name}.tar.gz > ${dist_name}.tar.gz.md5
# sha
shasum ${dist_name}.tar.gz > ${dist_name}.tar.gz.sha

#if [[ $publish == 1 ]]; then
  # Commit the release
#  svn add .
#  svn ci -m "usergrid-${current_version} release"

  # Finally delete all release candidate branches
#  for ref in $(git for-each-ref --format='%(refname:short)' 'refs/heads/${current_version}-rc*') do
#    git branch -D ${ref} # line 177: syntax error near unexpected token `git'
#    git push origin --delete ${ref}
#    svn rm ${usergrid_svn_dev_dist_url}/${ref}
#  done
#fi

current_commit_id=`git rev-parse HEAD`

cd ${base_dir}

echo "Done creating the release. The following draft email has been created"
echo "to send to the dev@usergrid.apache.org mailing list."
echo

# Create the email template for the release to be sent to the mailing lists.
MESSAGE=$(cat <<__EOF__
To: dev@usergrid.apache.org
Subject: [RESULT][VOTE] Release Apache Usergrid ${current_version} RC#{rc_tag_version}
as the official Apache Usegrid ${current_version} release has passed.


+1 (Binding)
------------------------------


+1 (Non-binding)
------------------------------


There were no 0 or -1 votes. Thank you to all who helped make this release.


Usegrid ${current_version} includes the following:
---
The CHANGELOG for the release is available at:
${usergrid_git_web_url};a=blob_plain;f=CHANGELOG;hb=${current_version}

The tag used to create the release with is ${current_version}:
${usergrid_git_web_url};a=commit;h=${current_version}

The current Git commit ID is ${current_commit_id}

The release is available at:
${usergrid_svn_dist_url}/${current_version}/${dist_name}.tar.gz

The MD5 checksum of the release can be found at:
${usergrid_svn_dist_url}/${current_version}/${dist_name}.tar.gz.md5

The signature of the release can be found at:
${usergrid_svn_dist_url}/${current_version}/${dist_name}.tar.gz.asc

The GPG key used to sign the release are available at:
${usergrid_svn_dist_url}/KEYS

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
  echo "This is a dry run, nothing has been published."
  echo
  echo "To clean up run: rm -rf ${dist_dir}"
fi

exit 0
