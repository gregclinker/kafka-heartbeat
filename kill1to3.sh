docker kill $(docker ps | egrep 'kafka1|kafka2|kafka3' | awk '{print $1}')
