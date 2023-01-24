#
BOOTSTRAP_SERVER=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094,${KAFKA4}:9095,${KAFKA5}:9096,${KAFKA6}:9097
echo $BOOTSTRAP_SERVER
#
for i in {1..50}
do
  /opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --delete --topic greg-test${i}
done
#
for i in {1..10}
do
  /opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test${i} --partitions 2 --replication-factor 4 --config min.insync.replicas=3
done
#
for i in {11..20}
do
  /opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --create --topic greg-test${i} --partitions 2 --replication-factor 2 --config min.insync.replicas=2
done
#
/opensource/kafka_2.13-3.1.0/bin/kafka-topics.sh --bootstrap-server=${BOOTSTRAP_SERVER} --describe
