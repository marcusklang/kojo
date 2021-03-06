#!/usr/bin/env bash
java -version
set -x 
# Build Kojo
./sbt.sh clean test package

# Create staging area
rm -rf installerbuild
mkdir -p installerbuild/lib
cd installer
scala cp-staging-jars.scala
cd ..

cp -var installer/* installerbuild/
cd installerbuild
rm *.*
rm -rf Uninstaller
cp licenses/Kojo-license.txt .
cd ..
rm -rf Kojo-z
mv installerbuild Kojo-z
rm Kojo.zip
zip -r Kojo.zip Kojo-z/*
