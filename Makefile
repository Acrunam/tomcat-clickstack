plugin_name = tomcat8-plugin
publish_bucket = cloudbees-clickstack
publish_repo = testing
publish_url = s3://$(publish_bucket)/$(publish_repo)/

deps = lib/tomcat8.zip lib/cloudbees-jmx-invoker.jar lib/jmxtrans-agent.jar lib/cloudbees-web-container-extras.jar lib/run-javaagent.jar lib/postgresql-jdbc.jar lib/mysql-connector-java.jar lib/mail.jar lib/activation.jar

pkg_files = control server setup lib

include plugin.mk

lib:
	mkdir -p lib

clean:
	rm -rf lib

tomcat8_ver = 8.0.0-RC1
tomcat8_url = http://repo1.maven.org/maven2/org/apache/tomcat/tomcat/$(tomcat8_ver)/tomcat-$(tomcat8_ver).zip
tomcat8_url = http://localhost/maven2/org/apache/tomcat/tomcat/$(tomcat8_ver)/tomcat-$(tomcat8_ver).zip
tomcat8_md5 = 2ad50d694526aa3cdcaf2f0e2852aed9

lib/tomcat8.zip: lib lib/genapp-setup-tomcat8.jar
	curl -fLo lib/tomcat8.zip "$(tomcat8_url)"
	unzip -qd lib lib/tomcat8.zip
	rm -rf lib/apache-tomcat-$(tomcat8_ver)/webapps
	rm lib/tomcat8.zip
	cd lib/apache-tomcat-$(tomcat8_ver); \
	zip -rqy ../tomcat8.zip *
	rm -rf lib/apache-tomcat-$(tomcat8_ver)

JAVA_SOURCES := $(shell find genapp-setup-tomcat8/src -name "*.java")
JAVA_JARS = $(shell find genapp-setup-tomcat8/target -name "*.jar")

lib/genapp-setup-tomcat8.jar: $(JAVA_SOURCES) $(JAVA_JARS) lib
	cd genapp-setup-tomcat8; \
	mvn -q clean test assembly:single; \
	cd target; \
	cp genapp-setup-tomcat8-*-jar-with-dependencies.jar \
	$(CURDIR)/lib/genapp-setup-tomcat8.jar

jmxtrans_agent_ver = 1.0.5
jmxtrans_agent_url = http://repo1.maven.org/maven2/org/jmxtrans/agent/jmxtrans-agent/$(jmxtrans_agent_ver)/jmxtrans-agent-$(jmxtrans_agent_ver).jar
jmxtrans_agent_md5 = 9e143ed7fee5e50cc2049cd8432457da

lib/jmxtrans-agent.jar: lib
	mkdir -p lib
	curl -fLo lib/jmxtrans-agent-$(jmxtrans_agent_ver).jar "$(jmxtrans_agent_url)"
	$(call check-md5,lib/jmxtrans-agent-$(jmxtrans_agent_ver).jar,$(jmxtrans_agent_md5))

jmx_invoker_ver = 1.0.2
jmx_invoker_src = http://repo1.maven.org/maven2/com/cloudbees/cloudbees-jmx-invoker/$(jmx_invoker_ver)/cloudbees-jmx-invoker-$(jmx_invoker_ver)-jar-with-dependencies.jar
jmx_invoker_md5 = c880f7545775529cfce6ea6b67277453

lib/cloudbees-jmx-invoker.jar: lib
	mkdir -p lib
	curl -fLo lib/cloudbees-jmx-invoker-$(jmx_invoker_ver)-jar-with-dependencies.jar "$(jmx_invoker_src)"
	$(call check-md5,lib/cloudbees-jmx-invoker-$(jmx_invoker_ver)-jar-with-dependencies.jar,$(jmx_invoker_md5))

cloudbees_web_container_extras_ver = 1.0.1
cloudbees_web_container_extras_src = http://repo1.maven.org/maven2/com/cloudbees/cloudbees-web-container-extras/$(cloudbees_web_container_extras_ver)/cloudbees-web-container-extras-$(cloudbees_web_container_extras_ver).jar
cloudbees_web_container_extras_md5 = c63a49c5a8071a0616c6696c3e6ed32a

lib/cloudbees-web-container-extras.jar: lib
	mkdir -p lib
	curl -fLo lib/cloudbees-web-container-extras-$(cloudbees_web_container_extras_ver).jar "$(cloudbees_web_container_extras_src)"
	$(call check-md5,lib/cloudbees-web-container-extras-$(cloudbees_web_container_extras_ver).jar,$(cloudbees_web_container_extras_md5))

java_agent_ver = 1.0
java_agent_src = https://s3.amazonaws.com/cloudbees-downloads/appserver/genapp-java-20130313.jar
java_agent_md5 = 44cd511782ea1924449bdb833c036762

lib/run-javaagent.jar: lib
	mkdir -p lib
	curl -fLo lib/run-javaagent-$(java_agent_ver).jar "$(java_agent_src)"
	$(call check-md5,lib/run-javaagent-$(java_agent_ver).jar,$(java_agent_md5))

mysql_connector_ver = 5.1.26
mysql_connector_src = http://repo1.maven.org/maven2/mysql/mysql-connector-java/$(mysql_connector_ver)/mysql-connector-java-$(mysql_connector_ver).jar
mysql_connector_md5 = ac10af580fc6d2e87caf9cf495fb622e

lib/mysql-connector-java.jar: lib
	mkdir -p lib
	curl -fLo lib/mysql-connector-java-$(mysql_connector_ver).jar "$(mysql_connector_src)"
	$(call check-md5,lib/mysql-connector-java-$(mysql_connector_ver).jar,$(mysql_connector_md5))

postgresql_jdbc_ver = 9.1-901.jdbc4
postgresql_jdbc_src = http://repo1.maven.org/maven2/postgresql/postgresql/$(postgresql_jdbc_ver)/postgresql-$(postgresql_jdbc_ver).jar
postgresql_jdbc_md5 = 6e26be40fb8daa96e9327020e035a621

lib/postgresql-jdbc.jar: lib
	mkdir -p lib
	curl -fLo lib/postgresql-$(postgresql_jdbc_ver).jar "$(postgresql_jdbc_src)"
	$(call check-md5,lib/postgresql-$(postgresql_jdbc_ver).jar,$(postgresql_jdbc_md5))

mail_ver = 1.4.7
mail_src = http://repo1.maven.org/maven2/javax/mail/mail/$(mail_ver)/mail-$(mail_ver).jar
mail_md5 = 77f53ff0c78ba43c4812ecc9f53e20f8

lib/mail.jar: lib
	mkdir -p lib
	curl -fLo lib/mail-$(mail_ver).jar "$(mail_src)"
	$(call check-md5,lib/mail-$(mail_ver).jar,$(mail_md5))

activation_ver = 1.1.1
activation_src = http://repo1.maven.org/maven2/javax/activation/activation/$(activation_ver)/activation-$(activation_ver).jar
activation_md5 = 46a37512971d8eca81c3fcf245bf07d2

lib/activation.jar: lib
	mkdir -p lib
	curl -fLo lib/activation-$(activation_ver).jar "$(activation_src)"
	$(call check-md5,lib/activation-$(activation_ver).jar,$(activation_md5))
