# Tomcat 8 ClickStack

To use: 

    bees app:deploy -t tomcat7 -RPLUGIN.SRC.tomcat7=https://community.ci.cloudbees.com/job/tomcat8-clickstack/lastSuccessfulBuild/artifact/build/distributions/tomcat8-clickstack-1.0.0-SNAPSHOT.zip -a APP_ID WAR_FILE

Please don't `-t tomcat7` as long as `-t tomcat8` has not been setup by CloudBees engineering team.

Tomcat 8 ClickStack for CloudBees PaaS.


# Build 

    $  gradlew clean installClickstack distClickstack

After successful build tomcat8-clickstack-1.2.3.zip is created under `build/distributions` and can be uploaded to the CloudBees platform location by the CloudBees team.

# Local development

Note: You should be familiar with developing ClickStacks using the genapp system first. \[[see docs](http://genapp-docs.cloudbees.com/quickstart.html)\]

* Build the plugin project using make to prepare for use in local app deploys
* In plugins\_home, add a symlink to the `tomcat8-clickstack/build/install/tomcat8-clickstack` dir named 'tomcat8'

  $ ln -s tomcat8-clickstack/build/install/tomcat8-clickstack PLUGINS\_HOME/tomcat8

* In your metadata.json, you can now reference the stack using the name 'tomcat8'

    { "app": {  "plugins": ["tomcat8"] } }


Once the plugin is published to a public URL, you can update an app to use it with the CloudBees SDK:

    $ bees app:deploy -a APP_ID -t tomcat7 -RPLUGIN.SRC.tomcat7=URL_TO_YOUR_PLUGIN_ZIP PATH_TO_WARFILE
