#
BOOTSTRAP_SERVER=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094,${KAFKA4}:9095,${KAFKA5}:9096,${KAFKA6}:9097
echo $BOOTSTRAP_SERVER
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --delete --topic greg-test1
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test1 --partitions 12 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --delete --topic greg-test2
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test2 --partitions 2 --replication-factor 2 --config min.insync.replicas=2
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --describe
