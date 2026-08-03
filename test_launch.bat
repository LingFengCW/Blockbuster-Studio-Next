@echo off
chcp 65001>nul
title bbs-next auto test
cd /D "D:\E\MC\.minecraft\versions\luzhi\"

rem Use wildcard classpath with all library jars
set CP=D:\E\MC\.minecraft\libraries\*;D:\E\MC\.minecraft\versions\luzhi\luzhi.jar;D:\E\MC\PCL\PCL\lwjgl-unsafe-agent.jar

"D:\Program Files\Java\jdk-26.0.1\bin\java.exe" ^
  -Dfile.encoding=COMPAT -Dstderr.encoding=UTF-8 -Dstdout.encoding=UTF-8 ^
  -javaagent:"D:\E\MC\PCL\PCL\lwjgl-unsafe-agent.jar" ^
  -XX:+UseG1GC -XX:-UseAdaptiveSizePolicy -XX:-OmitStackTraceInFastThrow ^
  -Djdk.lang.Process.allowAmbiguousCommands=true ^
  -Dfml.ignoreInvalidMinecraftCertificates=True ^
  -Dfml.ignorePatchDiscrepancies=True ^
  -Dlog4j2.formatMsgNoLookups=true ^
  -XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump ^
  --sun-misc-unsafe-memory-access=allow ^
  --enable-native-access=ALL-UNNAMED ^
  "-Djava.library.path=D:\E\MC\.minecraft\versions\luzhi\luzhi-natives/java" ^
  "-Djna.tmpdir=D:\E\MC\.minecraft\versions\luzhi\luzhi-natives/jna" ^
  "-Dorg.lwjgl.system.SharedLibraryExtractPath=D:\E\MC\.minecraft\versions\luzhi\luzhi-natives/lwjgl" ^
  "-Dio.netty.native.workdir=D:\E\MC\.minecraft\versions\luzhi\luzhi-natives/netty" ^
  -Dminecraft.launcher.brand=PCLCE -Dminecraft.launcher.version=514 ^
  -cp "%CP%" ^
  -DFabricMcEmu=net.minecraft.client.main.Main ^
  -Xmn230m -Xmx1536m ^
  net.fabricmc.loader.impl.launch.knot.KnotClient ^
  --username thewindows11 ^
  --version luzhi ^
  --gameDir "D:\E\MC\.minecraft\versions\luzhi" ^
  --assetsDir "D:\E\MC\.minecraft\assets" ^
  --assetIndex 32 ^
  --uuid 6f60266283164a6a89bde503ca7dc541 ^
  --accessToken dummy ^
  --clientId ${clientid} ^
  --xuid ${auth_xuid} ^
  --versionType PCLCE ^
  --width 854 --height 480

echo Game exited with code %ERRORLEVEL%
pause
