#!/bin/bash
set -e

MESOS_BASE="$HOME/.mesos/$1"
MESOS_BIN="$MESOS_BASE/bin"
MESOS_SBIN="$MESOS_BASE/sbin"
MESOS_LIB="$MESOS_BASE/lib"

function error {
	echo "Error: $1"
	exit 1
}

# check command line arguments
if [ -z ${1+x} ] || [ "$1" == "--help" ]; then
	echo "Usage: $0 <MESOS_VERSION> [shell]"
	exit 0
fi

# check if a shell was specified as command line parameter
if [ -z ${2+x} ]; then
	# No shell specified - use bash
	SPAWN_SHELL="bash"
else
	# A shell was specified - verify that it is an executable file
	SPAWN_SHELL=$(which $2)
	if [ ! -x $SPAWN_SHELL ]; then
		error "Specified shell is not an executable file"
	fi
fi

# verify that specified Mesos version actually exists
if [ ! -d $MESOS_BASE ]; then
	error "No Mesos version with name '$1' installed."
fi

# spawn shell
PATH="$MESOS_BIN:$MESOS_SBIN:$PATH" MESOS_NATIVE_JAVA_LIBRARY="$MESOS_LIB/libmesos.dylib" $SPAWN_SHELL
