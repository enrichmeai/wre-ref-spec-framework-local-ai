# The generator: a JDK, Maven, the framework and the service template, with every dependency fetched at build time,
# so the Job builds generated code offline inside the cluster.
FROM maven:3.10.2-eclipse-temurin-21
WORKDIR /work
COPY framework framework
COPY services services
COPY specs specs
COPY generator generator
# Install the framework, then build the service once with the known-good slot: that fills the Maven cache
# and proves the image can pass before any model output reaches it. The reference slot is removed afterwards.
RUN mvn -q -B -f framework/pom.xml install \
 && cp generator/reference/ShippingQuoteService.java services/shipping-quote/src/main/java/com/enrichmeai/ref/shipping/ \
 && mvn -q -B -f services/shipping-quote/pom.xml clean verify \
 && rm services/shipping-quote/src/main/java/com/enrichmeai/ref/shipping/ShippingQuoteService.java
ENTRYPOINT ["java", "generator/Generator.java", "--mvn-flags", "-q -B -o"]
