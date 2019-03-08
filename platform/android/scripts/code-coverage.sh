#!/usr/bin/env bash

set -e
set -o pipefail

#
# Get code coverage report, then convert it to JSON.
#
cov_result="";
if [ -f android/MapboxGLAndroidSDK/build/reports/coverage/ ]; then 
    cov_result=android/MapboxGLAndroidSDK/build/reports/coverage/debug/index.html
else
    echo "Coverage file does not exist. Please run tests before executing"
    exit 1
fi

$cov_result --json > output.json

#
# Convert the line coverage for the dynamic target to a percentage. Currently, 
# only CI tests are included when calculated code coverage.
# 
percentage=`node -e "console.log(require('./output.json').lineCoverage)"`
cov=$(printf "%.2f" $(echo "$percentage*100" | bc -l))

# Generate a formatted JSON file and upload it to S3.
./././scripts/code-coverage.sh $cov "Android" "$1"