# thesis_oracle

## Installation


1. Install

  ```bash
  sudo apt-get install autoconf libsodium-dev libunwind-dev libtool maven # Install libsodium and autoconf
  git clone https://github.com/zeromq/libzmq.git # Get zeromq
  cd libzmq
  git checkout v4.2.0
  ./autogen.sh
  ./configure --with-libsodium
  sudo make -j install
  cd ..
  git clone https://github.com/zeromq/jzmq.git
  cd jzmq/jzmq-jni
  ./autogen.sh
  ./configure
  sudo make -j install
  cd ..
  mvn package
  mvn install -Dgpg.skip=true
  
  cd ..
  git clone git://github.com/cryptobiu/scapi.git
  cd scapi
  git submodule init
  git submodule update
  # The current master of the library is broken, changes at https://github.com/cryptobiu/scapi/pull/86/files
  # must be applied and the src/java/edu/biu/SCProtocols/MaliciousYao folder must be removed in order to compile.
  #Also this repository should be added to pom.xml
  ```
  ```xml
  <repositories>
    <repository>
      <id>repository.jboss.org-public</id>
      <name>JBoss repository</name>
      <url>https://repository.jboss.org/nexus/content/groups/public</url>
    </repository>
  </repositories>
  ```
  ```bash

  mvn package
  mvn install -Dgpg.skip=true
  
  ```
2. Add mavenLocal() to repositories at build.gradle
3. Add "compile group: 'org.zeromq', name: 'jzmq', version: '3.1.1-SNAPSHOT'" to dependencies at build.gradle
4. Add "compile group: 'edu.biu.scapi', name: 'scapi', version: '2.3.0'" to dependencies at build.gradle 
5. Add the path to the native library to the run configuration, probably: /usr/local/lib.

