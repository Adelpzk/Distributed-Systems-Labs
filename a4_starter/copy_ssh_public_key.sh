#!/bin/bash

ssh-keyscan -H $(cat ecehadoop_hosts) >> ~/.ssh/known_hosts

for host in $(cat ecehadoop_hosts); do 
	echo $host
	ssh-copy-id -i ~/.ssh/id_rsa.pub $host
done
