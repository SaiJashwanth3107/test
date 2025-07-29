To start this QA server locally run below command
mvn clean package -DskipTests && java -jar target/first-0.0.1-SNAPSHOT.jar --spring.profiles.active=qa

Packaging command
mvn clean package -DskipTests

To run integration tests
mvn test