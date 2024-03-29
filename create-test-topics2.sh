#
BOOTSTRAP_SERVER=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094,${KAFKA4}:9095,${KAFKA5}:9096,${KAFKA6}:9097
echo $BOOTSTRAP_SERVER
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --delete --topic greg-test1,greg-test2,greg-test3,greg-test4,greg-test5,greg-test6,greg-test7,greg-test8,greg-test9,greg-test10
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test1 --partitions 2 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test2 --partitions 3 --replication-factor 4 --config min.insync.replicas=4
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test3 --partitions 5 --replication-factor 4 --config min.insync.replicas=4
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test4 --partitions 9 --replication-factor 4 --config min.insync.replicas=4
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test5 --partitions 24 --replication-factor 2 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test6 --partitions 2 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test7 --partitions 3 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test8 --partitions 5 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test9 --partitions 9 --replication-factor 4 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test10 --partitions 24 --replication-factor 6 --config min.insync.replicas=3
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --describe
