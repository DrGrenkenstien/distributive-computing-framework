# Steps to run:
- ## ON Master's machine:
  - Compile all the classes.
  - Run the Master: java distributedcomputing.Master 
- ## On each worker machine:
  - Compile all the classes.
  - Run the WorkerNode, specifying the master's IP address and port:
    java distributedcomputing.WorkerNode <master_ip_address> 1099
