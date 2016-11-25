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
  ```
2. Add mavenLocal() to repositories at build.gradle
3. Add "compile group: 'org.zeromq', name: 'jzmq', version: '3.1.1-SNAPSHOT'" to dependencies at build.gradle
4. Add the path to the native library to the run configuration, probably: /usr/local/lib.

