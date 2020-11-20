#!/bin/bash
set -euxo pipefail

# Java 14+ java.lang.Record is ambiguous with hssf.Record :'(
[ $JAVA_VERSION -lt 7 -o $JAVA_VERSION -gt 13 ] && echo "build does not support JAVA_VERSION: $JAVA_VERSION" && exit 0

echo "starting build for JAVA_VERSION: $JAVA_VERSION"

# grab all deps with java 8
[ $JAVA_VERSION -eq 7 ] && run-java 8 mvn dependency:go-offline

# install deps
mvn install -DskipTests=true -Dmaven.javadoc.skip=true -B -V

if [ $JAVA_VERSION -lt 12 ]
then
    # clean and test
    mvn clean test -B
else
    # clean and test
    mvn clean test -B -Djava.version=7 # java12+ minimum target is 7, not 6
fi

# publish only from java 6 and master branch
if [ "$BRANCH_NAME" == "master" -a $JAVA_VERSION -eq 7 ]
then
    echo 'deploying to maven'
    # java 7 cannot do modern SSL, use java 8 to deploy
    run-java 8 mvn deploy -Dmaven.test.skip=true -B

    mkdir -p release
    find -type f -name '*.jar' -print0 | xargs -0n1 -I {} mv '{}' 'release/'
fi

echo 'build success!'
exit 0
