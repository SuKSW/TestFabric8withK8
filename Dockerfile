FROM java:8
EXPOSE 8080
ADD target/ServiceDiscWithFabric-1-jar-with-dependencies.jar ServiceDiscWithFabric.jar
ENTRYPOINT ["java","-jar","ServiceDiscWithFabric.jar"]
