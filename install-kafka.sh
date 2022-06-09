echo -e "\e[1;32mInstalling: Kafka!\e[0m"
kubectl create ns ps-kafka
kubectl label namespace ps-kafka istio-injection=enabled --overwrite
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm upgrade --install kafka bitnami/kafka --set metrics.jmx.enabled=true --set metrics.kafka.enabled=true -n ps-kafka
sleep 2
echo -e "\e[1;33mWaiting for PODs to start...!\e[0m"
kubectl wait pods -n ps-kafka --all  --for condition=Ready --timeout=90s
#Scale Kafka pods..
kubectl scale --replicas=3 sts/kafka  -n ps-kafka
kubectl scale --replicas=1 sts/kafka-zookeeper  -n ps-kafka
echo -e "\e[1;33mWaiting for PODs to start...!\e[0m"
kubectl wait pods  -n ps-kafka  --all  --for condition=Ready --timeout=90s

cat << EOF
###########################################################################################

To test Kafka Producer and Consumer from local you can do following

kubectl run test-kafka-client --restart='Never' --image docker.io/bitnami/kafka:3.1.0-debian-10-r40 --namespace default --command -- sleep infinity  

kubectl exec --tty -i test-kafka-client --namespace default -- bash   

helm status kafka -n ps-kafka

kafka-topics.sh --create --bootstrap-server kafka.ps-kafka.svc.cluster.local:9092 --topic test --partitions 2 --replication-factor 2 --config retention.ms=86400000

kafka-console-producer.sh --broker-list kafka-0.kafka-headless.ps-kafka.svc.cluster.local:9092 --topic test 

kafka-console-consumer.sh --bootstrap-server kafka.ps-kafka.svc.cluster.local:9092 --topic test --from-beginning  

kafka-topics.sh --describe --topic test --bootstrap-server kafka-0.kafka-headless.ps-kafka.svc.cluster.local:9092

#BASIC PERF TEST
kafka-producer-perf-test.sh --topic test --num-records 10000 --record-size 1024 --print-metrics --throughput -1 --producer-props bootstrap.servers=kafka-0.kafka-headless.ps-kafka.svc.cluster.local:9092

kafka-consumer-perf-test.sh --topic test --print-metrics --bootstrap-server kafka.ps-kafka.svc.cluster.local:9092 --messages 50

###########################################################################################
EOF
echo -e "\e[1;32mInstallation complete: Kafka!\e[0m"
