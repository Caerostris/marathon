#!/bin/bash -i
set -e

MVM_BASE="$HOME/.mesos"
MVM_SOURCE_DIR="$HOME/.mesos/mesos_src"

# ensure that the MVM_BASE directory exists
if [ ! -d $MVM_BASE ]; then
	mkdir "$MVM_BASE"
fi

function print_help {
	echo "MVM, The Mesos Version Manager."
	echo "Usage: $0 [OPTION|VERSION]"
	echo ""
	echo "VERSION: Any tag or revision hash from the Mesos git repository may be chosen."
	echo ""
	echo "OPTIONS:"
	echo " --delete [VERSION]	Delete an installed version of Mesos"
	echo " --fetch		See --update"
	echo " --help			Display this help screen"
	echo " --latest		Switch to the latest avaiable version (HEAD)"
	echo " --list-tags		List all available version tags"
	echo " --list-installed	List all installed versions"
	echo " --update		Update the list of available versions"
}

function error {
	echo "Error: $1"
	exit 1
}

function check_sources {
	# check if source directory exists
	if [ ! -d $MVM_SOURCE_DIR ]; then
		echo "Mesos sources not found. Cloning sources."
		git clone https://git-wip-us.apache.org/repos/asf/mesos.git "$MVM_SOURCE_DIR"
	fi
}

function print_tags {
	check_sources
	cd $MVM_SOURCE_DIR

	for tag in $(git tag); do
		echo -ne "$tag"

		# check if version is built yet
		if [ -d "$MVM_BASE/$tag" ]; then
			echo -ne "*"
		fi

		echo ""
	done
}

function print_installed {
	for version in $(ls $MVM_BASE); do
		if [ "$version" != "mesos_src" ]; then
			echo "$version"
		fi
	done
}

function compile_mesos_version {
	check_sources
	cd $MVM_SOURCE_DIR

	# checkout requested revision
	if ! git checkout "$1" > /dev/null 2>&1; then
		error "Version '$1' does not match a git tag or revision"
	fi

	# generate configure script if it does not exist
	if [ ! -f "$MVM_SOURCE_DIR/configure" ]; then
		./bootstrap
	fi

	mkdir -p build
	cd build

	../configure CXXFLAGS=-Wno-deprecated-declarations \
		--disable-python \
		--disable-java \
		--with-apr=/usr/local/opt/apr/libexec \
		--with-svn=/usr/local/opt/subversion \
		--prefix="$2"

	make clean
	make

	# prepare installation directory and install Mesos
	mkdir "$2"
	make install
}

function update_sources {
	check_sources
	cd $MVM_SOURCE_DIR
	git fetch
}

function get_head_revision {
	check_sources
	cd $MVM_SOURCE_DIR
	git checkout master > /dev/null 2>&1
	git rev-parse --short=9 HEAD
}

function confirm {
	read -r -p "$1 [y/n] " response
	if [[ ! "$response" =~ [yY](es)* ]]; then
		echo "Abort."
		exit 0
	fi
}

function delete_version {
	if [ ! -d "$MVM_BASE/$1" ]; then
		error "Mesos version '$1' is not currently installed"
	fi

	confirm "rm -rf $MVM_BASE/$1. Proceed?"
	rm -rf $MVM_BASE/$1
}

# check command line arguments
if [ -z ${1+x} ] || [ "$1" == "--help" ]; then
	print_help
	exit 0
elif [ "$1" == "--list-tags" ]; then
	print_tags
	exit 0
elif [ "$1" == "--list-installed" ]; then
	print_installed
	exit 0
elif [ "$1" == "--update" ] || [ "$1" == "--fetch" ]; then
	echo "Fetching updates..."
	update_sources
	echo "Done."
	exit 0
elif [ "$1" == "--delete" ]; then
	if [ -z ${2+x} ]; then
		error "VERSION parameter required for --delete option"
	fi

	delete_version $2
	exit 0
elif [ "$1" == "--latest" ]; then
	# set requested_version to the latest commit
	REQUESTED_VERSION=$(get_head_revision)
	echo "Latest version is '$REQUESTED_VERSION'."
elif [[ $1 == --* ]]; then
	error "Unknown flag: '$1'."
else
	REQUESTED_VERSION="$1"
fi

MESOS_BASE="$MVM_BASE/$REQUESTED_VERSION"
MESOS_BIN="$MESOS_BASE/bin"
MESOS_SBIN="$MESOS_BASE/sbin"
MESOS_LIB="$MESOS_BASE/lib"
MESOS_INCLUDE="$MESOS_BASE/include"

if [ "$MESOS_BASE" == "$MVM_SOURCE_DIR" ]; then
	error "'$REQUESTED_VERSION' is not a valid version name"
elif [ "$REQUESTED_VERSION" == "master" ]; then
	echo "Cowardly refused to check out version 'master'."
	echo "Use the --latest flag in order to switch to the latest version."
	exit 1
fi

# verify that the specified Mesos version actually exists
if [ ! -d $MESOS_BASE ]; then
	confirm "Version '$REQUESTED_VERSION' is not currently installed. Compile?"
	compile_mesos_version "$REQUESTED_VERSION" "$MESOS_BASE"
fi

# spawn new bash shell
PATH="$MESOS_BIN:$MESOS_SBIN:$PATH" \
MESOS_NATIVE_JAVA_LIBRARY="$MESOS_LIB/libmesos.dylib" \
CPATH="$MESOS_INCLUDE:$CPATH" \
LIBRARY_PATH="$MESOS_LIB:$LIBRARY_PATH" \
PS1="(Mesos $REQUESTED_VERSION) $PS1" \
bash
