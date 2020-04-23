FROM azul/zulu-openjdk:8u192


ARG NODE_NAME=PartyA

RUN echo "Building Yo Cordapp and running as Node: ${NODE_NAME}"


#copy node config
COPY ./build/nodes/PartyA_node.conf /app/node.conf

# copy cordapp jar
COPY ./build/libs/ /app/cordapps

# copy individual workflow and contract jars
COPY ./build/nodes/PartyA /app


# corda 4.5 jar itself
COPY ./corda.jar /app/corda.jar


# working directory set to /app
WORKDIR /app

# run corda on our node
CMD java -jar corda.jar


# expose ports for our node to communicate with the outside world.
EXPOSE 80
EXPOSE 10001
EXPOSE 10002
EXPOSE 10003
EXPOSE 10004
EXPOSE 10005
EXPOSE 10006



EXPOSE 10003 # node
EXPOSE 10043 # admin


EXPOSE 10006 # node
EXPOSE 10049 # admin

