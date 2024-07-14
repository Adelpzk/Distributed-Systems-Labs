#!/bin/sh

FNAME=a4.tar.gz

tar -czf $FNAME *.cpp *.hpp

echo Your tarball file name is: $FNAME
echo 
echo It contains the following files:
echo 
tar -tf $FNAME

echo
echo Done.
echo
