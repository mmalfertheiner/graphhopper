#!/usr/bin/env bash

GH_CLASS=com.graphhopper.util.profiles.Profiles
CONFIG=config.properties
VERSION=$(grep  "<name>" -A 1 pom.xml | grep version | cut -d'>' -f2 | cut -d'<' -f1)
JAR=tools/target/graphhopper-tools-$VERSION-jar-with-dependencies.jar

ACTION=$1
USER=$2

if [ "$ACTION" == "create" ] || [ "$ACTION" == "print" ]; then
  java -cp "$JAR" "$GH_CLASS" "$ACTION" "$USER"
elif [ "$ACTION" == "add" ]; then
  FILE=$3
  OSM=$4
  java -cp "$JAR" "$GH_CLASS" "$ACTION" "$USER" "$FILE" config="$CONFIG" osmreader.osm="$OSM"
fi


