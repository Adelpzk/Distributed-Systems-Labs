#!/bin/bash

GTDIR=ground_truth

if [ ! -d "$GTDIR" ]; then
	echo "First, run the script generate_ground_truth.sh"
	exit
fi


HOSTNAME=$(hostname)
echo Host: $HOSTNAME

MPICXX=mpic++
MPIRUN=mpirun
 
rm *.o
$MPICXX -std=c++17 -O3 -I. matrix_multiplication.cpp -o matrix_multiplication.o
if [ $? -ne 0 ]; then
	exit
fi

echo Compilation successful

MPIHOSTFILE=mpi_ecehadoop_hosts

OUTPUTDIR=output
mkdir $OUTPUTDIR
rm -f $OUTPUTDIR/*.txt

INPUTA=input/matrix_a_2048_x_2048.txt
INPUTB=input/matrix_b_2048_x_2048.txt
#INPUTA=input/matrix_a_3072_x_3072.txt
#INPUTB=input/matrix_b_3072_x_3072.txt

RESULT=$OUTPUTDIR/result_data.txt

STDOUTLOG=$OUTPUTDIR/stdout_log.txt


for MTXORDER in 256 512 1024; do
#for MTXORDER in 512 1024 2048 3072; do
	NUMPROCS=32
	GTC=${GTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt
	OUTPUTC=${OUTPUTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt	
	EXPNAME=t # time-to-solution
	#/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot hostname
	/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot ./matrix_multiplication.o $MTXORDER $INPUTA $INPUTB $OUTPUTC $RESULT $EXPNAME
	diff -b $GTC $OUTPUTC >> $STDOUTLOG 2>&1
	if [ $? -ne 0 ]; then
		echo Output is incorrect
		sed -i '$s/$/0/' $RESULT		
	else
		echo Output is correct
		sed -i '$s/$/1/' $RESULT	
	fi		
done


#for NUMPROCS in 4 8 16; do
for NUMPROCS in 8 16 32; do
	MTXORDER=1024
	#MTXORDER=2048
	#MTXORDER=3072
	GTC=${GTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt
	OUTPUTC=${OUTPUTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt	
	EXPNAME=s # strong scaling
	#/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by node hostname
	#/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot hostname
	/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot ./matrix_multiplication.o $MTXORDER $INPUTA $INPUTB $OUTPUTC $RESULT $EXPNAME
	diff -b $GTC $OUTPUTC >> $STDOUTLOG 2>&1
	if [ $? -ne 0 ]; then
		echo Output is incorrect
		sed -i '$s/$/0/' $RESULT		
	else
		echo Output is correct
		sed -i '$s/$/1/' $RESULT	
	fi		
done


MTXORDER=256
#MTXORDER=512
#for NUMPROCS in 4 8 16; do
for NUMPROCS in 8 16 32; do
	GTC=${GTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt
	OUTPUTC=${OUTPUTDIR}/output_${MTXORDER}_x_${MTXORDER}.txt	
	EXPNAME=w # weak scaling
	#/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by node hostname	
	#/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot hostname
	/usr/bin/time timeout 5m $MPIRUN -np $NUMPROCS --hostfile $MPIHOSTFILE --map-by slot ./matrix_multiplication.o $MTXORDER $INPUTA $INPUTB $OUTPUTC $RESULT $EXPNAME
	MTXORDER=$((MTXORDER*2))
	diff -b $GTC $OUTPUTC >> $STDOUTLOG 2>&1
	if [ $? -ne 0 ]; then
		echo Output is incorrect
		sed -i '$s/$/0/' $RESULT		
	else
		echo Output is correct
		sed -i '$s/$/1/' $RESULT	
	fi
done


echo exptype, numproc, mtxorder, computetime, totaltime, correct > $OUTPUTDIR/result_header.txt
cat $OUTPUTDIR/result_header.txt $OUTPUTDIR/result_data.txt > $OUTPUTDIR/result.txt
rm $OUTPUTDIR/result_header.txt $OUTPUTDIR/result_data.txt $OUTPUTDIR/output_*.txt
cat $OUTPUTDIR/result.txt
