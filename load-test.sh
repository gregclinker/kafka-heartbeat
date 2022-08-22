/opensource/kafka_2.13-3.1.0/bin/kafka-producer-perf-test.sh \
	 --topic greg-test1 \
	 --num-records 100000 \
	 --record-size 1024 \
	 --print-metrics \
	 --throughput 50 \
	 --producer-props bootstrap.servers=${KAFKA1}:9092,${KAFKA2}:9093,${KAFKA3}:9094
