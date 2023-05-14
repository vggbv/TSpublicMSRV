# TSpublicMSRV
 Project based on microservices
To make this code work you need to add mysql-connector, I used mysql-connector-j-8.0.31, otherwise JDBC will not work.
Also create the database, I put the code needed to create this as "bazadanych.sql".
When it comes to file transfer, I don't chunk it so OOM is likely to happen. I successfully transfered about 1,1GB heavy file with around 16 GB of free RAM space (nothing to be proud of but the requirements are met).
#To do:
#transform it into code that can be dockerized, k8s and then service mesh. Take a brief look on deployment.
Wish you all the best, gabb.