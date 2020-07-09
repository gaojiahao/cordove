@echo off
rem 解决不能联网问题，启动以后，不要重启模拟器。
start %Android_home%\emulator\emulator @flutter_emulator -dns-server 8.8.8.8
::rem E:\soft\android-sdk\emulator\emulator -avd flutter_emulator -prop net.eth0.dns1=8.8.8.8