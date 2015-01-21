#!/bin/sh

SCRIPT="$0"

while [ -h "$SCRIPT" ] ; do
  ls=`ls -ld "$SCRIPT"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    SCRIPT="$link"
  else
    SCRIPT=`dirname "$SCRIPT"`/"$link"
  fi
done

if [ ! -d "${APP_DIR}" ]; then
  APP_DIR=`dirname "$SCRIPT"`/..
  APP_DIR=`cd "${APP_DIR}"; pwd`
fi

cd $APP_DIR

# if you've executed sbt assembly previously it will use that instead.
export JAVA_OPTS="${JAVA_OPTS} -DdebugSupportingFiles -DdebugOperations -DdebugModels -DdebugSwagger -XX:MaxPermSize=p256M -Xmx1024M 0 -DloggerPath=conf/log4j.properties"
ags="$@ com.wordnik.swagger.codegen.Codegen -i samples/client/lamudi/spec-files/api.json -l swift -o samples/client/lamudi/output -t src/main/resources/swift"

java -cp $APP_DIR/target/*:$APP_DIR/target/lib/* $ags
