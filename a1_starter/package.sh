#!/bin/sh

FNAME=a1.tar.gz

tar -czf $FNAME *.java a1.thrift

echo
echo Your tarball file name is: $FNAME
echo
echo It contains the following files:
echo
tar -tf $FNAME

echo
echo Good luck!
echo
