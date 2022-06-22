java \
-DKAFKA_BOOTSTRAP_SERVERS="${KAFKA1}:29092,${KAFKA2}:29093,${KAFKA3}:29094" \
-DKAFKA_SECURITY_PROTOCOL="SSL" \
-DKAFKA_SSL_TRUSTSTORE_LOCATION="/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks" \
-DKAFKA_SSL_TRUSTSTORE_PASSWORD="confluent" \
-DKAFKA_SSL_KEYSTORE_LOCATION="/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks" \
-DKAFKA_SSL_KEYSTORE_PASSWORD="confluent" \
-DKAFKA_SSL_KEY_PASSWORD="confluent" \
-DKAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=" " \
-jar target/kafka-heartbeat-0.1.jar
