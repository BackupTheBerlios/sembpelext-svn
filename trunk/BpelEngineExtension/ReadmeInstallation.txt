Installation Guide for Semantic BPEL Engine Extensions:
==============================================================

0. Download and install Java 5

1. Download and install ANT 1.6 or later
- add  environment variable ANT_HOME=$ANT_HOME
  e.g. ANT_HOME=C:\Programme\apache-ant-1.7.1
- add %ANT_HOME%\bin to environment variable PATH

2. Download and install SVN client or Eclipse with SVN plugin

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

5. Make sure that you either are connected to the internet 
   or that Protege3.4 build 130 is installed and the 
   environment variable PROTEGE_HOME is set to the installation folder of Protege.
   If Protege is not installed the tools do not perform optimally.
   It is important to use exactly the build version 130!
   This version is available at:
   http://protege.cim3.net/download/old-releases/3.4%20betas/build-130/full/
     
6. Compile and package the Semantic BPEL Engine Extensions via eclipse or via the command "ant deploy-sem-bpel-functions"
   and replace the BpelEngineExtension-1.0-SNAPSHOT.jar with the new JAR
   in folder %ENGINE_HOME%\shared\lib

7. Start Semantic BPEL Engine engine (including Tomcat)
- run %ENGINE_HOME%\start-bpel-engine.bat

The deployed services can be viewed by default in the BPELAdmin console
at http://localhost:8080/BpelAdmin/deployed_services.jsp.