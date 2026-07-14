@echo off
cd /d "C:\Users\user\IdeaProjects\RegDozor"
"C:\Users\user\.jdks\ms-21.0.9\bin\java.exe" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "target\regdozor.jar" >> "data\regdozor.log" 2>&1
