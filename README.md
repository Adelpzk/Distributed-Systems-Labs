# Distributed Systems Labs

This repository contains the solutions to various assignments and labs completed for the ECE 454: Distributed Computing course. Below is a summary of each assignment:

## Assignment 1: RPCs and Distributed Password Hashing
In this Lab, we built a distributed system in Java to compute the bcrypt key derivation function, used for securing passwords in web applications. The system includes:
- **Client Layer:** Accepts user requests.
- **Front End (FE) Layer:** Distributes requests to the Back End (BE) nodes and balances load.
- **Back End (BE) Layer:** Handles the actual bcrypt computations.
  
Key tasks included implementing the Apache Thrift interface, handling exceptions, and ensuring scalability and fault tolerance.

## Assignment 2: Big Data Analytics with Apache Spark
This Lab focused on performing data analytics on a movie ratings dataset using Apache Spark. The tasks included:
- Identifying users with the highest ratings for each movie.
- Computing the total number of ratings.
- Counting ratings per user.
- Measuring the similarity between pairs of movies based on user ratings.

The solutions were implemented in Scala and tested in a distributed environment using Hadoop.

## Assignment 3: Apache ZooKeeper and Fault-Tolerant Key-Value Store
In this Lab, we implemented a fault-tolerant key-value store using Apache ZooKeeper for coordination. The primary objectives were:
- Implementing primary-backup replication.
- Using ZooKeeper to determine the primary server and monitor its status.
- Ensuring that the system recovers automatically from failures, with the backup server taking over as the new primary when necessary.

## Assignment 4: Distributed Matrix Multiplication with MPI
This Lab involved implementing a distributed matrix multiplication algorithm using MPI. The key objectives were:
- Distributing the computation of matrix multiplication across multiple processes.
- Ensuring that the root process handles input/output operations with the persistent storage system.
- Testing the solution under various scaling scenarios, including strong and weak scaling.
