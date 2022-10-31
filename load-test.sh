BOOTSTRAP_SERVER=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094,${KAFKA4}:9095,${KAFKA5}:9096,${KAFKA6}:9097

/opensource/kafka_2.13-3.1.0/bin/kafka-producer-perf-test.sh \
	 --topic greg-test19 \
	 --num-records 100 \
	 --record-size 1024 \
	 --print-metrics \
	 --throughput -1 \
	 --producer-props bootstrap.servers=${BOOTSTRAP_SERVER}
