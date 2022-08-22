docker kill $(docker ps | egrep 'kafka4|kafka5|kafka6' | awk '{print $1}')
