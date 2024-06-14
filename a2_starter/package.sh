#!/bin/sh

FNAME=a2.tar.gz

tar -czf $FNAME *.scala

echo Your tarball file name is: $FNAME
echo 
echo It contains the following files:
echo 
tar -tf $FNAME

echo
echo Good luck!
echo
