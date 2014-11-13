#!/bin/sh
#******************************************************************************
# This shell script pulls together the pieces that make up the Android SDK
# distribution and builds a single zip file containing those pieces.
#
# The version of the SDK should be passed in as an argument.
# Example: build_sdk_zip.sh 1.4.3
#******************************************************************************

# variables used for javadoc generation
MAVEN_REPO="${HOME}/.m2/repository"
ANDROID_SDK_PATH="/Applications/adt-bundle-mac/sdk/platforms/android-4.2"
JAVADOC_EXE_PATH="${JAVA_HOME}/bin"
JAVA_VERSION="1.6"


# verify that we've been given an argument (the version string)
if [ $# -eq 0 ]
  then
    echo "Error: SDK version string should be passed as argument"
	exit 1
fi

# SDK version string is passed in as an argument to this script
SDK_VERSION="$1"

# the following command extracts the version string from the sdk source
SDK_SOURCE_VERSION=`grep "String SDK_VERSION" src/main/java/org/apache/usergrid/android/sdk/UGClient.java | awk '{print $7}' | cut -d'"' -f2`

if [ "${SDK_VERSION}" != "${SDK_SOURCE_VERSION}" ]; then
	echo "Error: sdk source version (${SDK_SOURCE_VERSION}) does not match specified version (${SDK_VERSION})"
	exit 1
fi


# set up our tools
MVN_COMMAND="mvn"

# set our paths and file names
LIBRARY_BASE_NAME="usergrid-android"
JAR_FILE_NAME="${LIBRARY_BASE_NAME}-${SDK_VERSION}.jar"
ZIP_BASE_NAME="${LIBRARY_BASE_NAME}-sdk"
ZIP_FILE_NAME="${ZIP_BASE_NAME}.zip"
TOPLEVEL_ZIP_DIR="zip"
DEST_ZIP_DIR="${TOPLEVEL_ZIP_DIR}/${LIBRARY_BASE_NAME}-sdk-${SDK_VERSION}"
BUILT_SDK_JAR_FILE="target/${JAR_FILE_NAME}"
ZIP_JAR_DIR="${DEST_ZIP_DIR}/lib"
NEW_PROJECT_TEMPLATE_DIR="new-project-template"
SAMPLES_DIR="samples"
DEST_SAMPLES_DIR="${DEST_ZIP_DIR}/${SAMPLES_DIR}"

# make a clean build
"${MVN_COMMAND}" clean
"${MVN_COMMAND}" install -Dmaven.test.skip=true

# new jar file found?
if [ ! -f "${BUILT_SDK_JAR_FILE}" ] ; then
	echo "Error: unable to find jar file '${BUILT_SDK_JAR_FILE}'"	
	exit 1
fi

# zip directory exists?
if [ -d "${DEST_ZIP_DIR}" ]; then
	# erase all existing files there
	find "${DEST_ZIP_DIR}" -type f -exec rm {} \;
else
	mkdir -p "${DEST_ZIP_DIR}"
fi

# copy everything from repository
for entry in *
do
	if [ -f "$entry" ]; then
		cp "$entry" "${DEST_ZIP_DIR}"
	elif [ -d "$entry" ]; then
		if [ "$entry" != "${TOPLEVEL_ZIP_DIR}" ]; then
			cp -r "$entry" "${DEST_ZIP_DIR}"
		fi
	fi
done


# if we have source/target in zip directory, delete everything under source/target
if [ -d "${DEST_ZIP_DIR}/src/target" ]; then
	rm -rf "${DEST_ZIP_DIR}/src/target"
	rmdir "${DEST_ZIP_DIR}/src/target"
fi

# create directory for jar file
mkdir -p "${ZIP_JAR_DIR}"

# copy jar file to destination directory
cp "${BUILT_SDK_JAR_FILE}" "${ZIP_JAR_DIR}"


# copy new jar file to new-project-template
# missing libs directory?
PROJECT_TEMPLATE_LIBS_DIR="${DEST_ZIP_DIR}/${NEW_PROJECT_TEMPLATE_DIR}/libs"
if [ ! -d "${PROJECT_TEMPLATE_LIBS_DIR}" ]; then
	mkdir -p "${PROJECT_TEMPLATE_LIBS_DIR}"
fi

cp "${BUILT_SDK_JAR_FILE}" "${PROJECT_TEMPLATE_LIBS_DIR}"

# does new-project-template have 'bin' subdirectory?
if [ -d "${DEST_ZIP_DIR}/${NEW_PROJECT_TEMPLATE_DIR}/bin" ]; then
	# delete it
	rm -r "${DEST_ZIP_DIR}/${NEW_PROJECT_TEMPLATE_DIR}/bin"
	rmdir "${DEST_ZIP_DIR}/${NEW_PROJECT_TEMPLATE_DIR}/bin"
fi

# copy new jar file to each sample app lib directory
for sample_entry in ${DEST_SAMPLES_DIR}/*
do
	if [ -d "${sample_entry}" ]; then
		# is it missing a libs subdirectory?
		SAMPLE_LIBS_DIR="${sample_entry}/libs"
		if [ ! -d "${SAMPLE_LIBS_DIR}" ]; then
			mkdir "${SAMPLE_LIBS_DIR}"
		fi
		
		# copy jar file to libs dir for sample app
		cp "${BUILT_SDK_JAR_FILE}" "${SAMPLE_LIBS_DIR}"
		
		# does the sample app directory have a 'bin' subdirectory?
		if [ -d "${sample_entry}/bin" ]; then
			# delete it
			rm -r "${sample_entry}/bin"
			rmdir "${sample_entry}/bin"
		fi
	fi
done

# have sdk-explorer in samples?
if [ -d "${DEST_SAMPLES_DIR}/sdk-explorer" ]; then
	# delete it
	rm -r "${DEST_SAMPLES_DIR}/sdk-explorer"
	rmdir "${DEST_SAMPLES_DIR}/sdk-explorer"
fi

# have build_release_zip.sh?
if [ -f "${DEST_ZIP_DIR}/build_release_zip.sh" ]; then
	# delete it
	rm "${DEST_ZIP_DIR}/build_release_zip.sh"
fi

# generate javadocs
JAVADOC_OUTPUT_DIR="${DEST_ZIP_DIR}/docs"
if [ -d "${JAVADOC_OUTPUT_DIR}" ]; then
  # delete everything that may be there
  rm -r "${JAVADOC_OUTPUT_DIR}/*"
else
  mkdir "${JAVADOC_OUTPUT_DIR}"
fi

CLASSPATH="./src/target/test-classes:${MAVEN_REPO}/com/fasterxml/jackson/core/jackson-core/2.2.3/jackson-core-2.2.3.jar:${MAVEN_REPO}/com/fasterxml/jackson/core/jackson-annotations/2.2.3/jackson-annotations-2.2.3.jar:${MAVEN_REPO}/com/fasterxml/jackson/core/jackson-databind/2.2.3/jackson-databind-2.2.3.jar:${MAVEN_REPO}/commons-codec/commons-codec/1.4/commons-codec-1.4.jar:${MAVEN_REPO}/commons-logging/commons-logging/1.1.1/commons-logging-1.1.1.jar:${MAVEN_REPO}/org/apache/httpcomponents/httpclient/4.1.2/httpclient-4.1.2.jar:${MAVEN_REPO}/org/apache/httpcomponents/httpcore/4.1.2/httpcore-4.1.2.jar"
SOURCEPATH="./src/main/java"
JAVADOC_OPTIONS="-splitindex -use -version -public -author"
PACKAGE_LIST="org.apache.usergrid.android.sdk.exception org.apache.usergrid.android.sdk.response org.apache.usergrid.android.sdk org.apache.usergrid.android.sdk.utils org.apache.usergrid.android.sdk.entities org.apache.usergrid.android.sdk.callbacks"

JDOC_CMD="${JAVADOC_EXE_PATH}/javadoc ${JAVADOC_OPTIONS} -classpath ${CLASSPATH} -d ${JAVADOC_OUTPUT_DIR} -source ${JAVA_VERSION} -sourcepath ${SOURCEPATH} ${PACKAGE_LIST}"
echo ${JDOC_CMD}
${JDOC_CMD}

# create the zip file
cd ${TOPLEVEL_ZIP_DIR} && zip -r -y ${ZIP_FILE_NAME} .

