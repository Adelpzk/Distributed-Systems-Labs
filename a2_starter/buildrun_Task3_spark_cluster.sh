#!/bin/bash

hostname | grep ecehadoop
if [ $? -eq 1 ]; then
    echo "This script must be run on ecehadoop :("
    exit -1
fi

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
export SCALA_HOME=/usr
export HADOOP_HOME=/opt/hadoop-latest/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop/
export SPARK_HOME=/opt/spark-latest/
#export SPARK_HOME=/opt/spark-2.4.6-bin-without-hadoop-scala-2.12/
#export SPARK_DIST_CLASSPATH=`$HADOOP_HOME/bin/hadoop classpath`
MAIN_SPARK_JAR=`ls $SPARK_HOME/jars/spark-core*.jar`
export CLASSPATH=".:$MAIN_SPARK_JAR"

export TARGET=in5.txt   # change this to the server file you want to test

echo --- Deleting
rm Task3.jar
rm Task3*.class

echo --- Compiling
$SCALA_HOME/bin/scalac -J-Xmx1g Task3.scala
if [ $? -ne 0 ]; then
    exit
fi

echo --- Jarring
$JAVA_HOME/bin/jar -cf Task3.jar Task3*.class

echo --- Showing input file content
INPUT=/a2_inputs/${TARGET}
$HADOOP_HOME/bin/hdfs dfs -cat $INPUT

echo --- Running
OUTPUT=/user/${USER}/a2_starter_code_output_spark/

# $HADOOP_HOME/bin/hdfs dfs -mkdir $INPUT
$HADOOP_HOME/bin/hdfs dfs -rm -R $OUTPUT
# $HADOOP_HOME/bin/hdfs dfs -cp /a2_inputs/in2.txt $INPUT
time $SPARK_HOME/bin/spark-submit --master yarn --class Task3 --driver-memory 4g --executor-memory 4g Task3.jar $INPUT $OUTPUT

export HADOOP_ROOT_LOGGER="WARN"
# $HADOOP_HOME/bin/hdfs dfs -ls $OUTPUT
# $HADOOP_HOME/bin/hdfs dfs -cat $OUTPUT/*

# change t1 to the correct task
rm out/t3_${TARGET}
$HADOOP_HOME/bin/hdfs dfs -cat $OUTPUT/* | sort > ~/ece454-assignments/a2_starter/out/t3_${TARGET}
diff a2_output/t3_${TARGET} out/t3_${TARGET}
