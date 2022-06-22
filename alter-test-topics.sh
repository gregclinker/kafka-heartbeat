#
BOOTSTRAP_SERVER=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094
#
/opensource/kafka_2.13-3.1.0/bin/kafka-configs.sh --bootstrap-server=${BOOTSTRAP_SERVER} --alter --entity-type topics --entity-name greg-test1 --add-config min.insync.replicas=2
#
/opensource/kafka_2.13-3.1.0/bin/kafka-configs.sh --bootstrap-server=${BOOTSTRAP_SERVER} --alter --entity-type topics --entity-name greg-test2 --add-config min.insync.replicas=2
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --describe
