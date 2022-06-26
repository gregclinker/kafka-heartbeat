export KAFKA_BOOTSTRAP_SERVERS="${KAFKA1}:29092,${KAFKA2}:29093,${KAFKA3}:29094"
export KAFKA_SECURITY_PROTOCOL="SSL"
export KAFKA_SSL_TRUSTSTORE_LOCATION="/home/greg/work/kafka-heartbeat/secrets/kafka_truststore.jks"
export KAFKA_SSL_TRUSTSTORE_PASSWORD="confluent"
export KAFKA_SSL_KEYSTORE_LOCATION="/home/greg/work/kafka-heartbeat/secrets/kafka_keystore.jks"
export KAFKA_SSL_KEYSTORE_PASSWORD="confluent"
export KAFKA_SSL_KEY_PASSWORD="confluent"
export KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM=" "
export HEART_BEAT_CONFIG="{\"numberOfBrokers\":3,\"interval\":10,\"standardIsr\":2,\"reducedIsr\":1,\"countToSwitch\":3,\"kafkaProperties\":null,\"topics\":[\"greg-test1\",\"greg-test2\"]}"

java -jar target/kafka-heartbeat-0.1.jar
