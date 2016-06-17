FROM java:openjdk-8u92-jre-alpine
MAINTAINER Robert Medeiros <robert@crimeminister.org>

ADD target/revuecinema-0.0.1-SNAPSHOT-standalone.jar /srv/revuecinema.jar

EXPOSE 8000

CMD ["java", "-cp", "/srv/revuecinema.jar", "clojure.main", "-m", "revuecinema.server"]
