#!/usr/bin/env bash

GH_CLASS=com.graphhopper.util.profiles.Profiles

ACTION=$1
USER=$2
FILE=$3
LOCATION=$4

CONFIG=config.properties
VERSION=$(grep  "<name>" -A 1 pom.xml | grep version | cut -d'>' -f2 | cut -d'<' -f1)
JAR=tools/target/graphhopper-tools-$VERSION-jar-with-dependencies.jar

java -cp "$JAR" "$GH_CLASS" "$ACTION" "$USER" config="$CONFIG" graph.location="$GRAPH" osmreader.osm="$LOCATION"