node {

  stage ('Checkout') {
    git 'https://github.com/jenkinsci/rancher-plugin.git'
  }

  stage('Prepare Env') {
    sh 'docker pull gradle:jdk8'
  }

  stage ('Build') {
    sh 'docker run --rm -v ${WORKSPACE}:/code --workdir /code gradle:jdk8 ./gradlew build'
  }

  stage ('Package') {
    sh 'docker run --rm -v ${WORKSPACE}:/code --workdir /code gradle:jdk8 ./gradlew jpi'
  }

}