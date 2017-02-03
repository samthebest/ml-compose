FROM dylanmei/zeppelin:latest

# At the time of writing, and when everything worked just fine, used
# https://github.com/dylanmei/docker-zeppelin/commit/20161bfd1c487afe3e9e0504a393957c99e8a4b2

RUN rm $ZEPPELIN_NOTEBOOK_DIR/2A94M5J1Z/note.json
RUN rmdir $ZEPPELIN_NOTEBOOK_DIR/2A94M5J1Z

ADD log4j.properties /usr/zeppelin/conf/

COPY notebooks/ $ZEPPELIN_NOTEBOOK_DIR/

# We put these notebooks into a directory called docker to simplify scripts
RUN mkdir -p $ZEPPELIN_HOME/docker/
# We also add the template & demo notebooks into /usr/zeppelin/ for easy named access
COPY demo-notebooks/ $ZEPPELIN_HOME/docker/demo-notebooks
COPY template-notebooks/ $ZEPPELIN_HOME/docker/template-notebooks
COPY test-notebooks/ $ZEPPELIN_HOME/docker/test-notebooks

# This has stopped working, so have to load jar using %dep
# I tried REALLY hard to use the following via the API, but the API sucks
# https://zeppelin.apache.org/docs/0.6.0/manual/dependencymanagement.html

ADD ml-compose-assembly.jar /usr/zeppelin/

CMD ["bin/zeppelin.sh"]
