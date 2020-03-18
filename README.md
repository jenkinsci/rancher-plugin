Jenkins Rancher Plugin
======================

> Note: This plugin only support Rancher1

Enables Jenkins to deploy or upgrade Rancher stack service instance

Rancher is an open source software platform that enables organizations to run containers in production. With Rancher, organizations no longer have to build a container services platform from scratch using a distinct set of open source technologies. Rancher supplies the entire software stack needed to manage containers in production.

You can learn more on the [Rancher Website](http://rancher.com/)

## Features

* Deploy docker image to Rancher
* Upgrade exist service instance in Rancher
* Support options to finish the upgrade automatically
* Support build environment variable as docker image tag, e.q. busybox:${BUILD_NUMBER}

### Potential upcoming features

* provision rancher stack with docker-compose and rancher-compose file

## General information

Jenkins Rancher Plugin Support Deploy or Upgrade Service Instance in Rancher.

## Requirements

### Jenkins

Jenkins version 2.11 or newer is required.

### Rancher

Rancher version 1.2.2 or newer is required.

## Setup

### Install plugin

Install this plugin via the Jenkins plugin manager.
Or Download the latest version of plugin from [releases][https://github.com/jenkinsci/rancher-plugin/releases].

### Create Rancher API Key

1. To enable access to your rancher server, you must create a account api key:
2. Siigin in to rancher server dashboard
3. Select Menu → API
4. Click "Add Account API Key"
5. Give the  api key any name and description you like, e.g "Jenkins"
6. Click the "Create Key"
7. Save the "Access Key" and "Secret Key" any way you like, we will use it later
8. You can now close the dialog

### Add the Rancher API Key to Jenkins:

1. Navigate to your Jenkins instance
2. Select "Credentials" from the Jenkins sidebar
3. Choose a credentials domain and click "Add Credentials"
4. From the "Kind" drop-down, choose "Username with password"
5. Enter a description for the credential — the actual value is not important
8. Click "OK" to create the credential

![](http://7pn5d3.com1.z0.glb.clouddn.com//snapshots/rancher-plugin/secret_config.png)

## Per-job configuration

### Freestyle job configuration

> Deploy or Upgrade Service Instance

1. Create a new free-style project
2. Ensure. via whatever build steps you need, that the Docker image you want to deploy to Rancher will be available in the docker registry
3. Add "Deploy/Upgrade Rancher Service" post-build action
4. Enter the target Rancher Service API endpoint, e.g 'http://rancher-server/v2-beta'
5. Select the credential name from the drop-down list
6. Enter the target rancher environment id, e.g '1a5558'
7. Enter the target service name, e.g 'stack/service'
8. Enter the docker image name, e.g 'busybox' (image name support current build environment variable like 'busybox:${BUILD_NUMBER}' to support dynamic image tag)
9. Optionally choose "Auto Confirm" to finish the upgrade automatically
10. Optionally choose "Public Ports" to export service ports. mutil port shoule be split be ",". e.g 8080:80,9191:91
11. Optionally choose "Environment variables" to config service environment. mutil variable should be split by ",". e.g AAA:aaa,BBB:bbb (Note: The old environment of service will be merge)
12. Optionally choose "Timeout" to config timeout seconds when wait rancher service status change.

![](http://7pn5d3.com1.z0.glb.clouddn.com//snapshots/rancher-plugin/job_config.png)

## Development Testing

For your convenience, this repository includes a docker-compose configuration
for running jenkins, rancher-server, and a rancher agent. It also includes a
bootstrapping script which will configure a Rancher service and Jenkins job
using the plugin from `build/libs/rancher.hpi`. To use this test harness simply
run the following:

```
./gradlew jpi
cd test/
docker-compose up -d --build
```

Rancher, Jenkins, and an Echo Server will be run on ports 8080, 8081, and 8082
respectively. You can simply visit the jenkins web UI and run the "test" job to
test the plugin.

## Version history

### Version 1.0.0 (July 13, 2017)

* Initial release

### Version 1.0.1 (July 17, 2017)

* Fixed dependencies info missing issue.

### Version 1.0.2 (August 04 2017)

* Preserves the log config while upgrading a service.

### Version 1.0.3 (September 03 2017)

* Support export service ports.

### Version 1.0.4 (September 12 2017)

* Support export environment variable.

### Version 1.0.5 (October 10 2017)

* Support health check serialization.

### Version 1.0.7 (December 11 2017)

* Support custom timeout, and build variable in all fields.

### Version 1.0.8 (December 21 2017)

* Fixed issue of timeout logic.

### Version 1.0.10 (April 25 2018)

* Fixed issues.

### Version 1.0.13 (Dec 31 2018 )

* Support start before stop options
* Fixed network config missing
