@echo off
cd /d "C:\Users\smirn\IdeaProjects\RegDozor"
"C:\Users\smirn\.jdks\ms-21.0.9\bin\java.exe" -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar "target\regdozor.jar" >> "data\regdozor.log" 2>&1
