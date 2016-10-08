#!/bin/bash

need_make=0

check_para() {
	if [ $1 -ne 3 ]; then
	echo "Illegal number of parameters"
	echo "run.sh <bing account key> <precision> <query>"
	exit 1
	fi
}

check_n_make() {
	for file in adb/*.java; do
		classfile=`echo $file | sed 's/.java/.class/'`
		if [ ! -e "$classfile" ]; then
			need_make=1
			break
		fi
	done
	if [ $need_make == 1 ]; then
		echo "[INFO] Project has not been built yet, build now..."
		make clean
		make
	fi
}

check_para $#
check_n_make
java -cp "lib/*:." adb.QueryCLI "$@"
