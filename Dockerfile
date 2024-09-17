FROM openjdk:11-jdk
WORKDIR /mp
COPY . .
CMD ["java", "-version"]
