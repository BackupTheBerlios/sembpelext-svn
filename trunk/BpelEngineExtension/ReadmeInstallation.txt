Installation Guide for Semantic BPEL Engine Extensions:
==============================================================

1. Download and install Java 5

2. Download and install ANT 1.6 or later
- add  environment variable ANT_HOME=$ANT_HOME
  e.g. ANT_HOME=C:\Programme\apache-ant-1.7.1
- add %ANT_HOME%\bin to environment variable PATH

3. Download and unzip the pre-configured Semantic BPEL Engine (based on Apache Tomcat 5.5.26 and ActiveBPEL-engine 4.1)
   from http://prdownload.berlios.de/sembpelext/SemanticBPELengine.zip 
   into a local folder (further called %ENGINE_HOME%)
   
   a. Get a trial or full-licensed version of Jess7.0p2 from http://www.jessrules.com/jess/download.shtml
      and copy the file jess.jar into %ENGINE_HOME%\common\lib 
      
   b. set the environment variable CATALINA_HOME to point to the directory %ENGINE_HOME%

4. Checkout the project Semantic BPEL Engine Extension (https://developer.berlios.de/projects/sembpelext/)
   from BerliOS (for instructions see https://developer.berlios.de/svn/?group_id=10108)
   into a local folder (further called %ENGINE_EXTENSIONS_HOME%)

   a. Also add the file jess.jar to %ENGINE_EXTENSIONS_HOME%\lib 
  
5. Compile and package the Semantic BPEL Engine Extensions via eclipse or via the command "ant deploy-sem-bpel-functions"
   and replace the BpelEngineExtension-1.0-SNAPSHOT.jar with the new JAR
   in folder %ENGINE_HOME%\shared\lib

5. (Re-)start Semantic BPEL Engine engine (including Tomcat)
- run %ENGINE_HOME%\start-bpel-engine.bat

