#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --delete --topic greg-test1
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --create --topic greg-test1 --replication-factor 3
/opensource/kafka_2.13-3.1.0/bin/kafka-configs.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --alter --entity-type topics --entity-name greg-test1 --add-config min.insync.replicas=2
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --delete --topic greg-test2
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --create --topic greg-test2 --replication-factor 3
/opensource/kafka_2.13-3.1.0/bin/kafka-configs.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --alter --entity-type topics --entity-name greg-test2 --add-config min.insync.replicas=2
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094 --describe
