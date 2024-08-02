#!/bin/bash

PYTHON=python3

OUTPUTDIR=ground_truth
mkdir $OUTPUTDIR
rm -f $OUTPUTDIR/*.txt

# INPUTA=input/matrix_a_2048_x_2048.txt
# INPUTB=input/matrix_b_2048_x_2048.txt
INPUTA=input/matrix_a_3072_x_3072.txt
INPUTB=input/matrix_b_3072_x_3072.txt

# for MTXORDER in 128 256 512 1024 2048; do
for MTXORDER in 128 256 512 1024 2048 3072; do  		
	$PYTHON numpy_matrix_multiplication.py $MTXORDER $INPUTA $INPUTB $OUTPUTDIR
done  	  
 	
