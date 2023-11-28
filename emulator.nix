with import <nixpkgs> {};

androidenv.emulateApp {
  name = "run-test-emulator";
  platformVersion = "33";
  abiVersion = "x86_64";
  systemImageType = "default";
}
