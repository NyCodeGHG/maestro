with import <nixpkgs> {};

androidenv.emulateApp {
  name = "run-test-emulator";
  platformVersion = "21";
  abiVersion = "x86_64";
  systemImageType = "default";
}
