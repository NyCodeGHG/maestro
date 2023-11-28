with import <nixpkgs> {};
mkShell {
  buildInputs = [
    nodejs
    jdk8
    protobuf
  ];
  JAVA_HOME = "${jdk8}";
  NIX_LD_LIBRARY_PATH = lib.makeLibraryPath [ stdenv.cc.cc ];
  NIX_LD = lib.fileContents "${stdenv.cc}/nix-support/dynamic-linker";
}
